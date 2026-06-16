package com.gamer.data.mpcserver.commands.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.DbCommandSupport;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 执行数据库语句（限制允许范围）。
 *
 * <p>
 * 安全白名单：
 * </p>
 * <ul>
 * <li>查询类：SELECT/SHOW/DESC/DESCRIBE</li>
 * <li>DDL 类（表级）：CREATE TABLE / ALTER TABLE / DROP TABLE</li>
 * </ul>
 *
 * <p>
 * 禁止：
 * </p>
 * <ul>
 * <li>DML：INSERT/UPDATE/DELETE/REPLACE 等</li>
 * <li>多语句：SQL 内包含 ';' 时拒绝</li>
 * </ul>
 *
 * <p>
 * 参数约定（params）：
 * </p>
 * <ul>
 * <li>sql（必填）：SQL 文本；会做允许前缀白名单限制（查询类/表级 DDL）</li>
 * <li>maxRows（可选，默认 50）：最多返回行数</li>
 * <li>queryTimeoutSeconds（可选，默认 10）：查询超时</li>
 * <li>maxOutputChars（可选，默认 200000）：最大输出字符数</li>
 * <li>maxCellChars（可选，默认 2000）：单元格最大字符数</li>
 * </ul>
 *
 * <p>
 * 输出格式：首部包含 URL/SQL/MAX_ROWS，然后输出表头（列名）+ 数据行，均以 tab 分隔。
 * </p>
 *
 * <p>
 * 连接复用：内部维护一个单例 Connection（按 URL+user 维度），用前做 {@code isValid()} 检测，
 * 失效后自动重连。这样可避免每次查询都走 TCP 握手 + MySQL 认证，对于频繁查询场景有明显提升。
 * </p>
 *
 * <p>
 * 实现注意：为兼容项目热更新限制，这里不使用 try-with-resources。
 * </p>
 */
@Process("local_sql_query")
public class DbQueryCommand implements CommandHandler {
    private static final int DEFAULT_MAX_ROWS = 50;//最大返回行数
    private static final int MAX_MAX_ROWS = 2000;//最大返回行数上限
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 10;//查询超时时间
    private static final int MAX_QUERY_TIMEOUT_SECONDS = 120;//查询超时时间上限
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 200000;//最大输出字符数
    private static final int MAX_OUTPUT_CHARS_UPPER_BOUND = 1000000;//最大输出字符数上限
    private static final int DEFAULT_MAX_CELL_CHARS = 2000;//单元格最大字符数
    private static final int MAX_CELL_CHARS_UPPER_BOUND = 20000;//单元格最大字符数上限

    /** 连接有效性检测超时（秒）。 */
    private static final int CONN_VALID_TIMEOUT_S = 1;

    /** 缓存的连接及其 URL/user 信息，用于判断是否可以复用。 */
    private String cachedUrl;
    private String cachedUser;
    private Connection cachedConn;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String sql = McpUtils.text(params, "sql");
        int maxRows = normalizePositiveInt(McpUtils.intVal(params, "maxRows"), DEFAULT_MAX_ROWS, MAX_MAX_ROWS);
        int queryTimeoutSeconds = normalizePositiveInt(
            McpUtils.intVal(params, "queryTimeoutSeconds"), DEFAULT_QUERY_TIMEOUT_SECONDS, MAX_QUERY_TIMEOUT_SECONDS);
        int maxOutputChars = normalizePositiveInt(
            McpUtils.intVal(params, "maxOutputChars"), DEFAULT_MAX_OUTPUT_CHARS, MAX_OUTPUT_CHARS_UPPER_BOUND);
        int maxCellChars = normalizePositiveInt(
            McpUtils.intVal(params, "maxCellChars"), DEFAULT_MAX_CELL_CHARS, MAX_CELL_CHARS_UPPER_BOUND);

        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("params.sql不能为空");
        }
        String url = DbCommandSupport.resolveJdbcUrl(ctx, params);
        String user = DbCommandSupport.requireDbDefaults(ctx).user();
        String password = DbCommandSupport.requireDbDefaults(ctx).password();

        String sqlTrim = sql.trim();
        // 归一化换行/制表符，避免前缀白名单匹配因空白差异失效。
        sqlTrim = sqlTrim.replace("\n", " ").replace("\r", " ").replace("\t", " ");
        // 安全约束：只允许查询类 + 指定 DDL（表级），避免写操作被误执行。
        boolean allowQuery =
            sqlTrim.regionMatches(true, 0, "SELECT", 0, 6)
                || sqlTrim.regionMatches(true, 0, "SHOW", 0, 4)
                || sqlTrim.regionMatches(true, 0, "DESCRIBE", 0, 8)
                || sqlTrim.regionMatches(true, 0, "DESC", 0, 4);

        boolean allowDdlTable =
            isCreateTable(sqlTrim)
                || isAlterTable(sqlTrim)
                || isDropTable(sqlTrim);

        if (!allowQuery && !allowDdlTable) {
            throw new IllegalArgumentException(
                "仅允许：查询类（SELECT/SHOW/DESC/DESCRIBE）或表级 DDL（CREATE TABLE/ALTER TABLE/DROP TABLE）");
        }

        Connection conn = acquireConnection(url, user, password);
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sqlTrim);
            // DDL 不会返回 ResultSet；查询类才返回 ResultSet。
            ps.setQueryTimeout(queryTimeoutSeconds);
            if (allowQuery) {
                ps.setMaxRows(maxRows);
                rs = ps.executeQuery();
            } else {
                int updateCount = ps.executeUpdate();
                String ddlMsg = "DDL executed ok; updateCount=" + updateCount;
                McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_DB,
                    "queryDdl sql=" + McpUtils.oneLine(sqlTrim), ddlMsg);
                return CommandResult.of(ddlMsg);
            }

            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            StringBuilder sb = new StringBuilder();
            final String truncMsg = "... (truncated; maxOutputChars reached)";
            if (!appendWithLimit(sb, "URL=" + url, maxOutputChars)) {
                McpServiceLog.cmd(ctx.log(), McpServiceLog.SERVICE_DB,
                    "query status=truncatedEarly sql=" + McpUtils.oneLine(sqlTrim));
                return CommandResult.of(truncMsg);
            }
            if (!appendWithLimit(sb, "SQL=" + McpUtils.oneLine(sqlTrim), maxOutputChars)) {
                McpServiceLog.cmd(ctx.log(), McpServiceLog.SERVICE_DB,
                    "query status=truncatedEarly sql=" + McpUtils.oneLine(sqlTrim));
                return CommandResult.of(truncMsg);
            }
            if (!appendWithLimit(
                sb, "MAX_ROWS=" + maxRows + " QUERY_TIMEOUT_SECONDS=" + queryTimeoutSeconds, maxOutputChars)) {
                McpServiceLog.cmd(ctx.log(), McpServiceLog.SERVICE_DB,
                    "query status=truncatedEarly sql=" + McpUtils.oneLine(sqlTrim));
                return CommandResult.of(truncMsg);
            }
            appendWithLimit(sb, "", maxOutputChars);

            StringBuilder header = new StringBuilder();
            for (int c = 1; c <= colCount; c++) {
                if (c > 1) {
                    header.append("\t");
                }
                header.append(md.getColumnLabel(c));
            }
            if (!appendWithLimit(sb, header.toString(), maxOutputChars)) {
                McpServiceLog.cmd(ctx.log(), McpServiceLog.SERVICE_DB,
                    "query status=truncatedEarly sql=" + McpUtils.oneLine(sqlTrim));
                return CommandResult.of(truncMsg);
            }

            boolean outputLimited = false;
            while (rs.next()) {
                StringBuilder line = new StringBuilder();
                for (int c = 1; c <= colCount; c++) {
                    if (c > 1) {
                        line.append("\t");
                    }
                    Object v = rs.getObject(c);
                    if (v != null) {
                        String s = String.valueOf(v);
                        s = McpUtils.oneLine(s);
                        if (s != null && s.length() > maxCellChars) {
                            s = s.substring(0, maxCellChars) + "...";
                        }
                        line.append(s);
                    }
                }
                if (!appendWithLimit(sb, line.toString(), maxOutputChars)) {
                    outputLimited = true;
                    break;
                }
            }

            if (outputLimited) {
                appendWithLimit(sb, truncMsg, maxOutputChars);
            }
            String queryOut = sb.toString();
            // 返回体含 URL=/大段 TSV，不落 result= 以免日文件重复 JDBC URL 与结果集。
            McpServiceLog.cmd(ctx.log(), McpServiceLog.SERVICE_DB,
                "query sql=" + McpUtils.oneLine(sqlTrim) + " maxRows=" + maxRows);
            return CommandResult.of(queryOut);
        } catch (Exception e) {
            // 查询异常时清除缓存连接，避免将损坏的连接留在缓存中。
            invalidateCachedConn();
            throw e;
        } finally {
            McpUtils.tryClose(rs);
            McpUtils.tryClose(ps);
            // 注意：conn 不在这里关闭，由缓存管理。
        }
    }

    private boolean isCreateTable(String sqlTrim) {
        if (sqlTrim == null) {
            return false;
        }
        // 允许：
        // - CREATE TABLE
        // - CREATE TEMPORARY TABLE
        return sqlTrim.regionMatches(true, 0, "CREATE TABLE", 0, "CREATE TABLE".length())
            || sqlTrim.regionMatches(true, 0, "CREATE TEMPORARY TABLE", 0, "CREATE TEMPORARY TABLE".length());
    }

    private boolean isAlterTable(String sqlTrim) {
        if (sqlTrim == null) {
            return false;
        }
        // 允许：ALTER TABLE
        return sqlTrim.regionMatches(true, 0, "ALTER TABLE", 0, "ALTER TABLE".length());
    }

    private boolean isDropTable(String sqlTrim) {
        if (sqlTrim == null) {
            return false;
        }
        // 允许：DROP TABLE / DROP TABLE IF EXISTS
        return sqlTrim.regionMatches(true, 0, "DROP TABLE", 0, "DROP TABLE".length());
    }

    /**
     * 获取可用连接：优先复用缓存连接，失效时重新建立。
     *
     * <p>
     * 使用 {@code isValid(1)} 检测连接存活，超时 1 秒内无响应视为失效。
     * </p>
     */
    private synchronized Connection acquireConnection(String url, String user, String password) throws Exception {
        if (cachedConn != null) {
            boolean sameTarget = url.equals(cachedUrl) && user.equals(cachedUser);
            if (sameTarget) {
                try {
                    if (cachedConn.isValid(CONN_VALID_TIMEOUT_S)) {
                        return cachedConn;
                    }
                } catch (Exception ignored) {
                    // isValid 抛异常说明连接已不可用，继续重建。
                }
            }
            // 目标变化或连接失效：关闭旧连接。
            McpUtils.tryClose(cachedConn);
            cachedConn = null;
            cachedUrl = null;
            cachedUser = null;
        }

        Connection conn = DriverManager.getConnection(url, user, password);
        cachedConn = conn;
        cachedUrl = url;
        cachedUser = user;
        return conn;
    }

    /**
     * 查询异常时主动清除缓存，确保下次调用重新建连。
     */
    private synchronized void invalidateCachedConn() {
        if (cachedConn != null) {
            McpUtils.tryClose(cachedConn);
            cachedConn = null;
            cachedUrl = null;
            cachedUser = null;
        }
    }

    private int normalizePositiveInt(Integer val, int def, int max) {
        int v = val == null ? def : val;
        if (v <= 0) {
            v = def;
        }
        if (v > max) {
            v = max;
        }
        return v;
    }
}

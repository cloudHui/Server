package com.gamer.data.mpcserver.commands.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.DbCommandSupport;
import com.gamer.data.mpcserver.core.DbDefaults;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 检查当前数据库操作权限（只做安全只读探测）。
 *
 * <p>返回内容：</p>
 * <ul>
 *   <li>CURRENT_USER</li>
 *   <li>HAS_INFORMATION_SCHEMA_TABLES</li>
 *   <li>HAS_MYSQL_GENERAL_LOG_QUERY</li>
 *   <li>HAS_SHOW_DATABASES</li>
 * </ul>
 */
@Process("local_check_permissions")
public class LocalCheckPermissionsCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        DbDefaults d = DbCommandSupport.requireDbDefaults(ctx);
        String url = d.buildJdbcUrl("mysql");
        String user = d.user();
        String password = d.password();

        StringBuilder sb = new StringBuilder();
        sb.append("URL=").append(url).append("\n");

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            st = conn.createStatement();

            // current user
            try {
                rs = st.executeQuery("SELECT CURRENT_USER()");
                if (rs.next()) {
                    sb.append("CURRENT_USER=").append(McpUtils.nvl(rs.getString(1))).append("\n");
                }
            } finally {
                McpUtils.tryClose(rs);
                rs = null;
            }

            // SHOW DATABASES
            sb.append("HAS_SHOW_DATABASES=");
            try {
                rs = st.executeQuery("SHOW DATABASES");
                boolean ok = rs.next();
                sb.append(ok ? "true" : "true(no rows?)").append("\n");
            } catch (Exception e) {
                sb.append("false; err=").append(McpUtils.oneLine(e.getMessage())).append("\n");
            } finally {
                McpUtils.tryClose(rs);
                rs = null;
            }

            // information_schema.tables
            sb.append("HAS_INFORMATION_SCHEMA_TABLES=");
            try {
                rs = st.executeQuery("SELECT table_name FROM information_schema.tables LIMIT 1");
                rs.next();
                sb.append("true\n");
            } catch (Exception e) {
                sb.append("false; err=").append(McpUtils.oneLine(e.getMessage())).append("\n");
            } finally {
                McpUtils.tryClose(rs);
                rs = null;
            }

            // mysql.general_log（用于 DDL/operation logs）
            sb.append("HAS_MYSQL_GENERAL_LOG_QUERY=");
            try {
                rs = st.executeQuery("SELECT event_time FROM mysql.general_log LIMIT 1");
                rs.next();
                sb.append("true\n");
            } catch (Exception e) {
                sb.append("false; err=").append(McpUtils.oneLine(e.getMessage())).append("\n");
            } finally {
                McpUtils.tryClose(rs);
                rs = null;
            }

            return CommandResult.of(sb.toString());
        } finally {
            McpUtils.tryClose(rs);
            McpUtils.tryClose(st);
            McpUtils.tryClose(conn);
        }
    }
}


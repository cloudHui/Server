package com.gamer.data.mpcserver.commands.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.DbCommandSupport;
import com.gamer.data.mpcserver.core.DbDefaults;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 获取库/表列表。
 */
@Process("local_get_database_info")
public class LocalGetDatabaseInfoCommand implements CommandHandler {
    private static final int MAX_DATABASES = 50;
    private static final int MAX_TABLES_PER_DB = 200;

    private static final Set<String> EXCLUDED_SCHEMAS = new HashSet<>();

    static {
        EXCLUDED_SCHEMAS.add("information_schema");
        EXCLUDED_SCHEMAS.add("mysql");
        EXCLUDED_SCHEMAS.add("performance_schema");
        EXCLUDED_SCHEMAS.add("sys");
    }

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        DbDefaults d = DbCommandSupport.requireDbDefaults(ctx);
        String url = d.buildJdbcUrl("mysql");
        String user = d.user();
        String password = d.password();

        StringBuilder sb = new StringBuilder();
        sb.append("URL=").append(url).append("\n");
        sb.append("SHOW DATABASES + TABLES (truncated)\n\n");
        sb.append("DB\tTABLE\n");

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            st = conn.createStatement();
            rs = st.executeQuery("SHOW DATABASES");

            List<String> dbs = new ArrayList<>();
            while (rs.next()) {
                String db = rs.getString(1);
                if (db == null || db.trim().isEmpty()) {
                    continue;
                }
                db = db.trim();
                if (EXCLUDED_SCHEMAS.contains(db)) {
                    continue;
                }
                dbs.add(db);
                if (dbs.size() >= MAX_DATABASES) {
                    break;
                }
            }

            for (String db : dbs) {
                appendTablesForDb(sb, conn, db);
            }
        } finally {
            McpUtils.tryClose(rs);
            McpUtils.tryClose(st);
            McpUtils.tryClose(conn);
        }

        return CommandResult.of(sb.toString());
    }

    private void appendTablesForDb(StringBuilder sb, Connection conn, String db) throws Exception {
        int limitCheck = MAX_TABLES_PER_DB + 1;

        String sqlInfoSchema = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema=? ORDER BY table_name LIMIT ?";

        PreparedStatement ps = null;
        ResultSet trs = null;
        try {
            ps = conn.prepareStatement(sqlInfoSchema);
            ps.setString(1, db);
            ps.setInt(2, limitCheck);
            trs = ps.executeQuery();

            List<String> tables = new ArrayList<>();
            while (trs.next()) {
                String t = trs.getString(1);
                if (t == null) {
                    continue;
                }
                tables.add(t);
            }

            if (tables.isEmpty()) {
                sb.append(db).append("\t").append("(no tables)").append("\n");
                return;
            }

            int real = Math.min(tables.size(), MAX_TABLES_PER_DB);
            for (int i = 0; i < real; i++) {
                sb.append(db).append("\t").append(tables.get(i)).append("\n");
            }
            if (tables.size() > MAX_TABLES_PER_DB) {
                sb.append(db).append("\t")
                    .append("... (more than ").append(MAX_TABLES_PER_DB).append(" tables omitted)")
                    .append("\n");
            }
        } catch (Exception e) {
            // information_schema 权限可能不足，兜底 SHOW TABLES。
            String safeDb = db.replace("`", "``");
            Statement st = null;
            ResultSet rs = null;
            try {
                st = conn.createStatement();
                rs = st.executeQuery("SHOW TABLES FROM `" + safeDb + "`");

                int real = 0;
                while (rs.next()) {
                    String t = rs.getString(1);
                    if (real < MAX_TABLES_PER_DB) {
                        sb.append(db).append("\t").append(t).append("\n");
                    }
                    real++;
                    if (real >= limitCheck) {
                        break;
                    }
                }
                if (real == 0) {
                    sb.append(db).append("\t").append("(no tables)").append("\n");
                } else if (real > MAX_TABLES_PER_DB) {
                    sb.append(db).append("\t")
                        .append("... (more than ").append(MAX_TABLES_PER_DB).append(" tables omitted)")
                        .append("\n");
                }
            } finally {
                McpUtils.tryClose(rs);
                McpUtils.tryClose(st);
            }
        } finally {
            McpUtils.tryClose(trs);
            McpUtils.tryClose(ps);
        }
    }
}


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
 * 获取操作日志（依赖 MySQL general_log）。
 */ 
@Process("local_get_operation_logs")
public class LocalGetOperationLogsCommand implements CommandHandler {
    private static final int MAX_ROWS = 200;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        DbDefaults d = DbCommandSupport.requireDbDefaults(ctx);
        String url = d.buildJdbcUrl("mysql");
        String user = d.user();
        String password = d.password();

        String sql = "SELECT event_time, user_host, thread_id, argument "
            + "FROM mysql.general_log "
            + "WHERE command_type='Query' AND ("
            + "argument LIKE 'INSERT %' OR "
            + "argument LIKE 'UPDATE %' OR "
            + "argument LIKE 'DELETE %' OR "
            + "argument LIKE 'REPLACE %'"
            + ") "
            + "ORDER BY event_time DESC "
            + "LIMIT " + MAX_ROWS;

        StringBuilder sb = new StringBuilder();
        sb.append("URL=").append(url).append("\n");
        sb.append("SQL=").append(sql).append("\n\n");
        sb.append("EVENT_TIME\tUSER_HOST\tTHREAD_ID\tARGUMENT\n");

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            st = conn.createStatement();
            rs = st.executeQuery(sql);

            int row = 0;
            while (rs.next()) {
                row++;
                Object eventTime = rs.getObject(1);
                Object userHost = rs.getObject(2);
                Object threadId = rs.getObject(3);
                String arg = rs.getString(4);
                sb.append(McpUtils.nvl(String.valueOf(eventTime))).append("\t")
                    .append(McpUtils.nvl(String.valueOf(userHost))).append("\t")
                    .append(McpUtils.nvl(String.valueOf(threadId))).append("\t")
                    .append(McpUtils.oneLine(arg)).append("\n");
            }
            sb.append("\nROWS=").append(row).append("\n");
            return CommandResult.of(sb.toString());
        } catch (Exception e) {
            return CommandResult.of("操作日志获取失败/不可用: " + e.getMessage());
        } finally {
            McpUtils.tryClose(rs);
            McpUtils.tryClose(st);
            McpUtils.tryClose(conn);
        }
    }
}


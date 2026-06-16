package com.gamer.data.mpcserver.commands.redis;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.RedisDefaults;
import com.gamer.data.mpcserver.core.Process;
/**
 * Redis SCAN：列出匹配 pattern 的 key（分页、限量）。
 */
@Process("list")
public class ListCommand implements CommandHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_COUNT = 200;
    private static final int MAX_COUNT = 1000;
    private static final int MAX_SCAN_ROUNDS = 200;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String pattern = params == null ? null : McpUtils.text(params, "pattern");
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "*";
        }
        String cursor = params == null ? null : McpUtils.text(params, "cursor");
        if (cursor == null || cursor.trim().isEmpty()) {
            cursor = "0";
        } else {
            cursor = cursor.trim();
        }
        int limit = normalizePositiveInt(McpUtils.intVal(params, "limit"), DEFAULT_LIMIT, MAX_LIMIT);
        int count = normalizePositiveInt(McpUtils.intVal(params, "count"), DEFAULT_COUNT, MAX_COUNT);

        RedisDefaults d = ctx == null ? null : ctx.redisDefaults();
        if (d == null) {
            throw new IllegalStateException("redisDefaults未初始化");
        }

        RedisRespClient client = new RedisRespClient(d.host(), d.port(), d.user(), d.password());
        List<String> keys = new ArrayList<>();
        String nextCursor = cursor;
        boolean truncated = false;
        int rounds = 0;

        // 使用 Session 复用单连接，避免每轮 SCAN 都重新 TCP 连接 + AUTH。
        RedisRespClient.Session session = client.openSession();
        try {
            while (rounds < MAX_SCAN_ROUNDS) {
                rounds++;

                List<String> args = new ArrayList<>();
                args.add("SCAN");
                args.add(nextCursor);
                args.add("MATCH");
                args.add(pattern.trim());
                args.add("COUNT");
                args.add(String.valueOf(count));

                Object resp = session.execute(args);
                ScanResult scanResult = parseScanResult(resp);
                nextCursor = scanResult.nextCursor;

                for (int i = 0; i < scanResult.keys.size(); i++) {
                    if (keys.size() >= limit) {
                        truncated = true;
                        break;
                    }
                    keys.add(scanResult.keys.get(i));
                }

                if (truncated || "0".equals(nextCursor)) {
                    break;
                }
            }
        } finally {
            session.close();
        }

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append("\n");
        }
        if (truncated || !"0".equals(nextCursor)) {
            sb.append("... (truncated; nextCursor=").append(nextCursor).append(")\n");
        }
        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_REDIS,
            "scan pattern=" + pattern.trim() + " cursor=" + cursor + " limit=" + limit + " count=" + count, out);
        return CommandResult.of(out);
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

    private ScanResult parseScanResult(Object resp) {
        if (!(resp instanceof List)) {
            throw new IllegalStateException("SCAN响应格式错误");
        }
        List<Object> arr = (List<Object>) resp;
        if (arr.size() < 2) {
            throw new IllegalStateException("SCAN响应字段不足");
        }

        String nextCursor = String.valueOf(arr.get(0));
        List<String> keys = new ArrayList<>();
        Object keysObj = arr.get(1);
        if (keysObj instanceof List) {
            List<Object> keyArr = (List<Object>) keysObj;
            for (Object it : keyArr) {
                if (it == null) {
                    continue;
                }
                keys.add(String.valueOf(it));
            }
        }

        ScanResult r = new ScanResult();
        r.nextCursor = nextCursor == null ? "0" : nextCursor.trim();
        if (r.nextCursor.isEmpty()) {
            r.nextCursor = "0";
        }
        r.keys = keys;
        return r;
    }

    private static class ScanResult {
        private String nextCursor;
        private List<String> keys;
    }
}


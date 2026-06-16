package com.gamer.data.mpcserver.commands.redis;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.RedisDefaults;
import com.gamer.data.mpcserver.core.Process;
/**
 * Redis DEL：删除 key（支持单个或多个 key）。
 */
@Process("delete")
public class DeleteCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        if (params == null) {
            throw new IllegalArgumentException("params不能为空");
        }
        JsonNode keyNode = params.get("key");
        if (keyNode == null || keyNode.isNull()) {
            throw new IllegalArgumentException("params.key不能为空");
        }

        List<String> keys = new ArrayList<>();
        if (keyNode.isArray()) {
            for (int i = 0; i < keyNode.size(); i++) {
                JsonNode it = keyNode.get(i);
                String k = it == null || it.isNull() ? null : it.asText();
                if (k != null && !k.trim().isEmpty()) {
                    keys.add(k.trim());
                }
            }
        } else {
            String k = keyNode.asText();
            if (k != null && !k.trim().isEmpty()) {
                keys.add(k.trim());
            }
        }

        if (keys.isEmpty()) {
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_REDIS, "del keys=(empty)", "0");
            return CommandResult.of("0");
        }

        RedisDefaults d = ctx == null ? null : ctx.redisDefaults();
        if (d == null) {
            throw new IllegalStateException("redisDefaults未初始化");
        }

        List<String> args = new ArrayList<>();
        args.add("DEL");
        args.addAll(keys);

        RedisRespClient client = new RedisRespClient(d.host(), d.port(), d.user(), d.password());
        Object resp = client.execute(args);
        String out = resp == null ? "0" : String.valueOf(resp);
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_REDIS, "del keyCount=" + keys.size(), out);
        return CommandResult.of(out);
    }
}


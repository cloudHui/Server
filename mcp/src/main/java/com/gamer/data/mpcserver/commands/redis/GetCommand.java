package com.gamer.data.mpcserver.commands.redis;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.RedisDefaults;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * Redis GET：获取 key 的字符串值。
 */
@Process("get")
public class GetCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String key = McpUtils.text(params, "key");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("params.key不能为空");
        }

        RedisDefaults d = ctx == null ? null : ctx.redisDefaults();
        if (d == null) {
            throw new IllegalStateException("redisDefaults未初始化");
        }

        List<String> args = new ArrayList<>();
        args.add("GET");
        args.add(key.trim());

        RedisRespClient client = new RedisRespClient(d.host(), d.port(), d.user(), d.password());
        Object resp = client.execute(args);
        String out = resp == null ? "null" : String.valueOf(resp);
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_REDIS, "get key=" + key.trim(), out);
        return CommandResult.of(out);
    }
}


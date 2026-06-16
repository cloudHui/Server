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
 * Redis SET：设置 key 的字符串值（可选 EX 过期秒数）。
 */ 
@Process("set")
public class SetCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String key = McpUtils.text(params, "key");
        String value = McpUtils.text(params, "value");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("params.key不能为空");
        }
        if (value == null) {
            value = "";
        }

        Integer expireSeconds = McpUtils.intVal(params, "expireSeconds");

        RedisDefaults d = ctx == null ? null : ctx.redisDefaults();
        if (d == null) {
            throw new IllegalStateException("redisDefaults未初始化");
        }

        List<String> args = new ArrayList<>();
        args.add("SET");
        args.add(key.trim());
        args.add(value);

        if (expireSeconds != null && expireSeconds > 0) {
            args.add("EX");
            args.add(String.valueOf(expireSeconds.intValue()));
        }

        RedisRespClient client = new RedisRespClient(d.host(), d.port(), d.user(), d.password());
        Object resp = client.execute(args);
        String out = resp == null ? "null" : String.valueOf(resp);
        String cmd = "set key=" + key.trim();
        if (expireSeconds != null && expireSeconds > 0) {
            cmd = cmd + " EX=" + expireSeconds;
        }
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_REDIS, cmd, out);
        return CommandResult.of(out);
    }
}


package com.gamer.data.mpcserver.commands;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.Process;

/**
 * MCP 心跳命令：用于连通性检查与保活。
 *
 * <p>
 * 返回固定前缀 pong，并附带服务端时间戳，便于调用端快速判断服务可用性。
 * </p>
 *
 * @author liuyunhui
 * @date 2026-04-13
 */
@Process("ping")
public class PingCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) {
        return CommandResult.of("pong " + new Date().getTime());
    }
}

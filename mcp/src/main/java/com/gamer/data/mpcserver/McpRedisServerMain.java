package com.gamer.data.mpcserver;

/**
 * Redis MCP 服务入口。
 */
public class McpRedisServerMain {
    public static void main(String[] args) throws Exception {
        McpServer.run(McpServer.ServerProfile.REDIS, args, ".cursor/mcpsever/redis");
    }
}


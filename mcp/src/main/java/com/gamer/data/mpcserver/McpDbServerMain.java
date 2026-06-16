package com.gamer.data.mpcserver;

/**
 * 数据库 MCP 服务入口：暴露 db.*（describe/query/create/alter）。
 *
 * @author liuyunhui
 * @date 2026-03-19
 * @version 1.0
 */
public class McpDbServerMain {
    public static void main(String[] args) throws Exception {
        McpServer.run(McpServer.ServerProfile.DB, args,".cursor/mcpsever/db");
    }
}


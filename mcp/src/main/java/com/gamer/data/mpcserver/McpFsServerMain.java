package com.gamer.data.mpcserver;

/**
 * 文件系统 MCP 服务入口：仅暴露 fs.*。
 *
 * @author liuyunhui
 * @date 2026-03-19
 * @version 1.0
 */
public class McpFsServerMain {
    public static void main(String[] args) throws Exception {
        McpServer.run(McpServer.ServerProfile.FS, args, ".cursor/mcpsever/fs");
    }
}


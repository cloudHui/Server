package com.gamer.data.mpcserver;

/**
 * Git 只读查询 MCP 服务入口。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
public class McpGitServerMain {
    public static void main(String[] args) throws Exception {
        McpServer.run(McpServer.ServerProfile.GIT, args, ".cursor/mcpsever/git");
    }
}

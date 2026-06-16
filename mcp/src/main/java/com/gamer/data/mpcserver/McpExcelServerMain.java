package com.gamer.data.mpcserver;

/**
 * Excel MCP 服务入口：仅暴露 excel.read。
 *
 * @author liuyunhui
 * @date 2026-03-19
 * @version 1.0
 */
public class McpExcelServerMain {
    public static void main(String[] args) throws Exception {
        McpServer.run(McpServer.ServerProfile.EXCEL, args,".cursor/mcpsever/excel");
    }
}


package com.gamer.data.mpcserver;

import com.gamer.data.mpcserver.commands.image.ImageConfig;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandDispatcher;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.DbDefaults;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.GitDefaults;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.RedisDefaults;
import com.gamer.data.mpcserver.log.DailyFileLog;
import com.gamer.data.mpcserver.protocol.McpResponse;

/**
 * Image MCP 服务入口。
 *
 * @author liuyunhui
 * @date 2026-06-12
 */
public class McpImageServerMain {
    public static void main(String[] args) throws Exception {
        parseImageArgs(args);
        McpServer.run(McpServer.ServerProfile.IMAGE, args, ".cursor/mcpsever/image");
    }

    private static void parseImageArgs(String[] args) {
        if (args == null) return;
        for (String a : args) {
            if (a == null) continue;
            String s = a.trim();
            if (s.startsWith("--mimoApiKey=")) {
                ImageConfig.setMimoApiKey(s.substring("--mimoApiKey=".length()));
            } else if (s.startsWith("--mimoBaseUrl=")) {
                ImageConfig.setMimoBaseUrl(s.substring("--mimoBaseUrl=".length()));
            } else if (s.startsWith("--mimoModel=")) {
                ImageConfig.setMimoModel(s.substring("--mimoModel=".length()));
            }
        }
    }
}

package com.gamer.data.mpcserver.commands.fs;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.Process;
/**
 * 列出允许访问的根目录（沙箱范围）。
 */
@Process("list_allowed_directories")
public class ListAllowedDirectoriesCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        List<String> roots = ctx.fileSandbox() == null ? null : ctx.fileSandbox().allowedRootPaths();
        StringBuilder sb = new StringBuilder();
        sb.append("Allowed directories:\n");
        if (roots == null || roots.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (String root : roots) {
                sb.append(root).append("\n");
            }
        }
        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "listAllowedDirectories", out);
        return CommandResult.of(out);
    }
}


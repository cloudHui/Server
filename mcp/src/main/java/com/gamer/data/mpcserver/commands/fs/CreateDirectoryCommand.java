package com.gamer.data.mpcserver.commands.fs;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 创建目录。
 */
@Process("create_directory")
public class CreateDirectoryCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        File dir = sbx.requireAllowedPath(path);
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException("目标已存在且不是目录: " + dir.getPath());
        }
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) {
                throw new IllegalStateException("创建目录失败: " + dir.getPath());
            }
        }

        String out = "Successfully created directory: " + dir.getPath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "createDirectory path=" + path, out);
        return CommandResult.of(out);
    }
}

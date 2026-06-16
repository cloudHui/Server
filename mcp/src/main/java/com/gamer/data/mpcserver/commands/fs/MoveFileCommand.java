package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 移动/重命名文件。
 */ 
@Process("move_file")
public class MoveFileCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String source = McpUtils.text(params, "source");
        String destination = McpUtils.text(params, "destination");
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("params.source不能为空");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("params.destination不能为空");
        }

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        File src = sbx.requireAllowedPath(source);
        if (!src.exists()) {
            throw new IllegalArgumentException("源路径不存在: " + src.getPath());
        }

        File dst = sbx.requireAllowedPath(destination);

        try {
            Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // Files.move 对跨分区/权限等更严格；兜底 renameTo。
            boolean ok = src.renameTo(dst);
            if (!ok) {
                throw e;
            }
        }

        String out = "Successfully moved to: " + dst.getPath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "moveFile source=" + source + " destination=" + destination, out);
        return CommandResult.of(out);
    }
}


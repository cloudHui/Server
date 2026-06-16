package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 读取媒体文件（图片/音频等）并返回 base64（避免协议里传二进制）。
 *
 * <p>限制：</p>
 * <ul>
 *   <li>默认 maxBytes=1MB（可通过 params.maxBytes 调整）</li>
 * </ul>
 */ 
@Process("read_media_file")
public class ReadMediaFileCommand implements CommandHandler {
    private static final int DEFAULT_MAX_BYTES = 1024 * 1024;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("params.path不能为空");
        }
        Integer maxBytesNode = McpUtils.intVal(params, "maxBytes");
        int maxBytes = maxBytesNode == null || maxBytesNode <= 0 ? DEFAULT_MAX_BYTES : maxBytesNode;

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File f = sbx.requireAllowedFile(path);

        long fileSize = f.length();
        if (fileSize > maxBytes) {
            String err = "ERROR: media file too large, fileSize=" + fileSize + " > maxBytes=" + maxBytes;
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "readMediaFile path=" + path + " maxBytes=" + maxBytes, err);
            return CommandResult.of(err);
        }

        byte[] bytes = Files.readAllBytes(f.toPath());
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String out = "PATH=" + f.getAbsolutePath() + "\nFILE_SIZE_BYTES=" + fileSize + "\nBASE64=" + base64;
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "readMediaFile path=" + path + " maxBytes=" + maxBytes, out);
        return CommandResult.of(out);
    }
}


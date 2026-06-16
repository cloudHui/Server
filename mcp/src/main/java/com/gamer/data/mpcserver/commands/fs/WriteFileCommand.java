package com.gamer.data.mpcserver.commands.fs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 创建或覆盖写入文本文件（UTF-8）。
 */ 
@Process("write_file")
public class WriteFileCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        String content = McpUtils.text(params, "content");
        if (content == null) {
            content = "";
        }

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File f = sbx.requireAllowedPath(path);

        BufferedWriter bw = null;
        try {
            // 覆盖写
            bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8));
            bw.write(content);
            bw.flush();
        } finally {
            McpUtils.tryClose(bw);
        }

        String out = "Successfully wrote to " + f.getPath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "writeFile path=" + path, out);
        return CommandResult.of(out);
    }
}


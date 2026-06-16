package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 获取文件或目录信息。
 */
@Process("get_file_info")
public class GetFileInfoCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File f = sbx.requireAllowedPath(path);
        if (!f.exists()) {
            throw new IllegalArgumentException("路径不存在: " + f.getPath());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("path: ").append(f.getPath()).append("\n");
        sb.append("absolutePath: ").append(f.getAbsolutePath()).append("\n");
        try {
            sb.append("canonicalPath: ").append(f.getCanonicalPath()).append("\n");
        } catch (Exception ignored) {
        }
        sb.append("type: ").append(f.isDirectory() ? "directory" : (f.isFile() ? "file" : "other")).append("\n");
        if (f.isFile()) {
            sb.append("size: ").append(f.length()).append("\n");
        } else {
            sb.append("size: ").append(0).append("\n");
        }
        sb.append("lastModified: ").append(sdf.format(new Date(f.lastModified()))).append("\n");
        sb.append("readable: ").append(f.canRead()).append("\n");
        sb.append("writable: ").append(f.canWrite()).append("\n");
        sb.append("executable: ").append(f.canExecute()).append("\n");
        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "getFileInfo path=" + path, out);
        return CommandResult.of(out);
    }
}


package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 列出目录内容（支持 offset/limit 分页）。
 *
 * <p>使用 {@link DirectoryStream} 按需迭代，不再用 {@code listFiles()} 把所有条目加载进内存，
 * 对包含大量文件的目录可提前退出，避免全量内存分配。</p>
 */
@Process("list_directory")
public class ListDirectoryCommand implements CommandHandler {
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_MAX_CHARS = 200000;
    private static final int MAX_MAX_CHARS = 1000000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        int offset = normalizeNonNegativeInt(McpUtils.intVal(params, "offset"));
        int limit = normalizePositiveInt(McpUtils.intVal(params, "limit"), DEFAULT_LIMIT, MAX_LIMIT);
        int maxChars = normalizePositiveInt(McpUtils.intVal(params, "maxOutputChars"), DEFAULT_MAX_CHARS, MAX_MAX_CHARS);
        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File dir = sbx.requireAllowedDirectory(path);

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        int collected = 0;
        boolean hasMore = false;
        boolean charTruncated = false;

        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(dir.toPath());
            for (Path entry : stream) {
                if (entry == null) {
                    continue;
                }
                if (idx < offset) {
                    idx++;
                    continue;
                }
                if (collected >= limit) {
                    hasMore = true;
                    break;
                }
                idx++;
                collected++;
                File e = entry.toFile();
                String line = (e.isDirectory() ? "[DIR] " : "[FILE] ") + e.getName();
                if (!appendWithLimit(sb, line, maxChars)) {
                    charTruncated = true;
                    break;
                }
            }
        } finally {
            McpUtils.tryClose(stream);
        }

        if (charTruncated) {
            appendWithLimit(sb, "... (truncated; maxOutputChars reached)", maxChars);
        } else if (hasMore) {
            appendWithLimit(sb, "... (truncated; more entries available)", maxChars);
        }
        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "listDirectory path=" + path + " offset=" + offset + " limit=" + limit, out);
        return CommandResult.of(out);
    }

    private int normalizePositiveInt(Integer val, int def, int max) {
        int v = val == null ? def : val;
        if (v <= 0) {
            v = def;
        }
        if (v > max) {
            v = max;
        }
        return v;
    }

    private int normalizeNonNegativeInt(Integer val) {
        int v = val == null ? 0 : val;
        if (v < 0) {
            v = 0;
        }
        return v;
    }

}

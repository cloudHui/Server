package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 列出目录内容，并附带 size。
 */
@Process("list_directory_with_sizes")
public class ListDirectoryWithSizesCommand implements CommandHandler {
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_MAX_CHARS = 200000;
    private static final int MAX_MAX_CHARS = 1000000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        String sortBy = McpUtils.text(params, "sortBy");
        int offset = normalizeNonNegativeInt(McpUtils.intVal(params, "offset"));
        int limit = normalizePositiveInt(McpUtils.intVal(params, "limit"), DEFAULT_LIMIT, MAX_LIMIT);
        int maxChars = normalizePositiveInt(McpUtils.intVal(params, "maxOutputChars"), DEFAULT_MAX_CHARS, MAX_MAX_CHARS);

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        File dir = sbx.requireAllowedDirectory(path);
        File[] entries = dir.listFiles();
        if (entries == null || entries.length == 0) {
            String empty = "";
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "listDirectoryWithSizes path=" + path + " sortBy=" + sortBy, empty);
            return CommandResult.of(empty);
        }

        sortEntries(entries, sortBy);

        StringBuilder sb = new StringBuilder();
        if (offset >= entries.length) {
            String empty2 = "";
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "listDirectoryWithSizes path=" + path + " offset=" + offset + " limit=" + limit, empty2);
            return CommandResult.of(empty2);
        }
        int end = offset + limit;
        if (end > entries.length) {
            end = entries.length;
        }
        boolean charTruncated = false;
        for (int i = offset; i < end; i++) {
            File e = entries[i];
            if (e == null) {
                continue;
            }
            long size = e.isFile() ? e.length() : 0;
            String line = (e.isDirectory() ? "[DIR] " : "[FILE] ") + e.getName() + " size=" + size;
            if (!appendWithLimit(sb, line, maxChars)) {
                charTruncated = true;
                break;
            }
        }
        if (!charTruncated && end < entries.length) {
            appendWithLimit(sb, "... (truncated; more entries available)", maxChars);
        } else if (charTruncated) {
            appendWithLimit(sb, "... (truncated; maxOutputChars reached)", maxChars);
        }

        String listOut = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "listDirectoryWithSizes path=" + path + " offset=" + offset + " limit=" + limit + " sortBy=" + sortBy,
            listOut);
        return CommandResult.of(listOut);
    }

    private void sortEntries(File[] entries, String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            Arrays.sort(entries, (o1, o2) -> {
                String a = o1 == null ? "" : o1.getName();
                String b = o2 == null ? "" : o2.getName();
                return a.compareTo(b);
            });
            return;
        }

        String s = sortBy.trim().toLowerCase();
        boolean size = s.startsWith("size");
        boolean desc = s.endsWith("desc") || s.endsWith("_desc") || s.endsWith("d");

        if (size) {
            Arrays.sort(entries, (o1, o2) -> {
                long a = (o1 != null && o1.isFile()) ? o1.length() : 0;
                long b = (o2 != null && o2.isFile()) ? o2.length() : 0;
                if (a == b) {
                    String an = o1 == null ? "" : o1.getName();
                    String bn = o2 == null ? "" : o2.getName();
                    return an.compareTo(bn);
                }
                if (desc) {
                    return a < b ? 1 : -1;
                }
                return a < b ? -1 : 1;
            });
            return;
        }

        Arrays.sort(entries, (o1, o2) -> {
            String a = o1 == null ? "" : o1.getName();
            String b = o2 == null ? "" : o2.getName();
            return a.compareTo(b);
        });
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


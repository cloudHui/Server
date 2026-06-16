package com.gamer.data.mpcserver.commands.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 批量读取多个文本文件（UTF-8）。
 *
 * <p>paths 参数兼容：</p>
 * <ul>
 *   <li>paths 为 JSON array：["a.txt","b.txt"]</li>
 *   <li>paths 为字符串：可以是 JSON array 字符串 或逗号/换行分隔列表</li>
 * </ul>
 */ 
@Process("read_multiple_files")
public class ReadMultipleFilesCommand implements CommandHandler {
    private static final int DEFAULT_MAX_CHARS = 200000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        if (ctx == null) {
            throw new IllegalArgumentException("CommandContext不能为空");
        }
        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        JsonNode node = params == null ? null : params.get("paths");
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("params.paths不能为空");
        }

        int maxChars = DEFAULT_MAX_CHARS;
        Integer maxCharsNode = McpUtils.intVal(params, "maxChars");
        if (maxCharsNode != null && maxCharsNode > 0) {
            maxChars = maxCharsNode;
        }

        List<String> paths = parsePaths(ctx, node);
        if (paths.isEmpty()) {
            String empty = "FILE_COUNT=0";
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "readMultipleFiles paths=(empty)", empty);
            return CommandResult.of(empty);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FILE_COUNT=").append(paths.size()).append("\n");

        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            sb.append("\n-- FILE[").append(i).append("] ").append(p).append(" --\n");
            try {
                File f = sbx.requireAllowedFile(p);
                String content = readTextLimited(f, maxChars);
                sb.append(content);
            } catch (Exception e) {
                sb.append("ERROR: ").append(McpUtils.oneLine(e.getMessage())).append("\n");
            }
        }
        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "readMultipleFiles fileCount=" + paths.size() + " maxChars=" + maxChars, out);
        return CommandResult.of(out);
    }

    private List<String> parsePaths(CommandContext ctx, JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || node.isNull()) {
            return out;
        }

        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode it = node.get(i);
                if (it == null || it.isNull()) {
                    continue;
                }
                String s = it.asText();
                if (s != null && !s.trim().isEmpty()) {
                    out.add(s.trim());
                }
            }
            return out;
        }

        String s = node.asText();
        if (s == null) {
            return out;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return out;
        }

        // JSON array 字符串
        if (t.startsWith("[") || t.startsWith("{")) {
            try {
                JsonNode root = ctx.mapper().readTree(t);
                if (root != null && root.isArray()) {
                    for (int i = 0; i < root.size(); i++) {
                        JsonNode it = root.get(i);
                        if (it == null || it.isNull()) {
                            continue;
                        }
                        String v = it.asText();
                        if (v != null && !v.trim().isEmpty()) {
                            out.add(v.trim());
                        }
                    }
                    return out;
                }
            } catch (Exception ignored) {
            }
        }

        // 逗号/换行分隔
        String[] arr = t.split("[,\\n\\r\\t;]+");
        for (String v : arr) {
            if (v != null && !v.trim().isEmpty()) {
                out.add(v.trim());
            }
        }
        return out;
    }

    private String readTextLimited(File f, int maxChars) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                int remaining = maxChars - sb.length();
                if (remaining <= 0) {
                    sb.append("... (truncated; maxChars=").append(maxChars).append(")");
                    break;
                }
                if (line.length() > remaining) {
                    sb.append(line, 0, remaining);
                    sb.append("... (truncated; maxChars=").append(maxChars).append(")");
                    break;
                }
                sb.append(line);
            }
            return sb.toString();
        } finally {
            McpUtils.tryClose(br);
        }
    }
}


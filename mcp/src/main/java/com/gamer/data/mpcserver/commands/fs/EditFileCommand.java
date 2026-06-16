package com.gamer.data.mpcserver.commands.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * 行级编辑（以 JSON 编辑描述为主，失败时退化为“整文件替换”）。
 *
 * <p>
 * 推荐 edits 格式：
 * </p>
 * 
 * <pre>
 * [
 *   {"startLine": 1, "endLine": 1, "content": "new line 1\nnew line 2"},
 *   {"startLine": 10, "content": "only one line replace"}
 * ]
 * </pre>
 */
@Process("edit_file")
public class EditFileCommand implements CommandHandler {
    private static class EditOp {
        int startLine;
        Integer endLine;
        List<String> contentLines;
    }

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        String edits = McpUtils.text(params, "edits");
        if (edits == null) {
            edits = "";
        }

        String dryRunStr = McpUtils.text(params, "dryRun");
        boolean dryRun = dryRunStr != null && ("true".equalsIgnoreCase(dryRunStr) || "1".equals(dryRunStr.trim()));

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        File f = sbx.requireAllowedPath(path);
        List<String> lines = readAllLinesIfExists(f);

        String trimmed = edits.trim();
        boolean isJson = trimmed.startsWith("[") || trimmed.startsWith("{");
        boolean applied = false;

        if (isJson) {
            try {
                JsonNode root = ctx.mapper().readTree(edits);
                List<EditOp> ops = parseEditOps(root);
                if (!ops.isEmpty()) {
                    applyOps(lines, ops);
                    applied = true;
                }
            } catch (Exception ignored) {
                // JSON 格式不满足时不直接报错，统一退化整文件替换。
            }
        }

        if (!applied) {
            lines = splitLines(edits);
        }

        if (dryRun) {
            String dry = "dryRun=true；编辑完成但未写入文件。target=" + f.getPath() + ", newLineCount=" + lines.size();
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "editFile path=" + path + " dryRun=true", dry);
            return CommandResult.of(dry);
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(f.toPath(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8));
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    bw.write("\n");
                }
                bw.write(lines.get(i));
            }
            bw.flush();
        } finally {
            McpUtils.tryClose(bw);
        }

        String ok = "Successfully edited file: " + f.getPath() + ", newLineCount=" + lines.size();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS, "editFile path=" + path + " dryRun=false", ok);
        return CommandResult.of(ok);
    }

    private List<String> readAllLinesIfExists(File f) throws Exception {
        List<String> out = new ArrayList<>();
        if (f == null || !f.exists()) {
            return out;
        }
        if (!f.isFile()) {
            throw new IllegalArgumentException("目标不是文件: " + f.getPath());
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                out.add(line);
            }
        } finally {
            McpUtils.tryClose(br);
        }
        return out;
    }

    private List<String> splitLines(String text) {
        String s = text == null ? "" : text;
        // -1 保留末尾空行（如需要）
        String[] arr = s.split("\\r?\\n", -1);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<EditOp> parseEditOps(JsonNode root) {
        if (root == null) {
            return Collections.emptyList();
        }
        if (!root.isArray()) {
            return Collections.emptyList();
        }
        List<EditOp> out = new ArrayList<>();
        for (int i = 0; i < root.size(); i++) {
            JsonNode it = root.get(i);
            if (it == null || it.isNull() || !it.isObject()) {
                continue;
            }
            Integer startLine = McpUtils.intVal(it, "startLine");
            if (startLine == null) {
                continue;
            }
            EditOp op = new EditOp();
            op.startLine = startLine;
            op.endLine = null;

            Integer endLine = McpUtils.intVal(it, "endLine");
            if (endLine != null) {
                op.endLine = endLine;
            }

            String content = McpUtils.text(it, "content");
            if (content == null) {
                content = McpUtils.text(it, "newContent");
            }
            if (content == null) {
                content = "";
            }
            op.contentLines = splitLines(content);
            out.add(op);
        }
        return out;
    }

    private void applyOps(List<String> lines, List<EditOp> ops) {
        if (lines == null || ops == null || ops.isEmpty()) {
            return;
        }
        ops.sort((o1, o2) -> {
            // 从后往前，避免行号移动影响
            return o2.startLine - o1.startLine;
        });

        for (EditOp op : ops) {
            if (op == null) {
                continue;
            }
            int startIdx = op.startLine - 1;
            if (startIdx < 0) {
                startIdx = 0;
            }
            if (startIdx > lines.size()) {
                startIdx = lines.size();
            }

            int endIdx;
            if (op.endLine == null) {
                endIdx = startIdx;
            } else {
                endIdx = op.endLine - 1;
                if (endIdx < startIdx) {
                    endIdx = startIdx;
                }
            }
            if (endIdx >= lines.size()) {
                endIdx = lines.size() - 1;
            }

            // 删除 [startIdx, endIdx]
            if (!lines.isEmpty() && startIdx <= endIdx) {
                lines.subList(startIdx, endIdx + 1).clear();
            }

            // 插入新内容
            if (op.contentLines != null && !op.contentLines.isEmpty()) {
                lines.addAll(startIdx, op.contentLines);
            }
        }
    }
}

package com.gamer.data.mpcserver.commands.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
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
 * 读取文本文件（UTF-8）。支持 head/tail（按行截取），两者互斥。
 *
 * <p>tail 模式优化：使用 {@link RandomAccessFile} 从文件末尾向前扫描，
 * 只读取需要的字节，避免读取整个文件（O(fileSize) → O(tail * avgLineLen)）。</p>
 */
@Process("read_text_file")
public class ReadTextFileCommand implements CommandHandler {
    private static final int DEFAULT_MAX_LINES = 1000;
    private static final int MAX_LINES_UPPER_BOUND = 10000;
    private static final int DEFAULT_MAX_CHARS = 200000;
    private static final int MAX_CHARS_UPPER_BOUND = 1000000;

    /**
     * tail 倒读时每行预估字节数（宽松估算，实际会再补足）。
     */
    private static final int TAIL_AVG_LINE_BYTES = 150;
    /**
     * 单次倒读块大小（8KB），分块向前扫描。
     */
    private static final int TAIL_CHUNK_SIZE = 8192;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        Integer head = McpUtils.intVal(params, "head");
        Integer tail = McpUtils.intVal(params, "tail");
        int maxLines = normalizePositiveInt(McpUtils.intVal(params, "maxLines"), DEFAULT_MAX_LINES, MAX_LINES_UPPER_BOUND);
        int maxChars = normalizePositiveInt(McpUtils.intVal(params, "maxChars"), DEFAULT_MAX_CHARS, MAX_CHARS_UPPER_BOUND);
        if (head != null && head < 0) {
            head = null;
        }
        if (tail != null && tail < 0) {
            tail = null;
        }
        if (head != null && tail != null) {
            throw new IllegalArgumentException("head 与 tail 不能同时指定");
        }

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File f = sbx.requireAllowedFile(path);

        if (head != null) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8));
                String outHead = readHead(br, head, maxChars);
                McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                    "readTextFile path=" + path + " head=" + head, outHead);
                return CommandResult.of(outHead);
            } finally {
                McpUtils.tryClose(br);
            }
        }

        if (tail != null) {
            String outTail = readTailFast(f, tail, maxChars);
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "readTextFile path=" + path + " tail=" + tail, outTail);
            return CommandResult.of(outTail);
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8));
            // 不再默认全量读取；未指定 head/tail 时按 maxLines + maxChars 保护返回体。
            String outAll = readAllLimited(br, maxLines, maxChars);
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "readTextFile path=" + path + " maxLines=" + maxLines + " maxChars=" + maxChars, outAll);
            return CommandResult.of(outAll);
        } finally {
            McpUtils.tryClose(br);
        }
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

    private String readAllLimited(BufferedReader br, int maxLines, int maxChars) throws Exception {
        StringBuilder out = new StringBuilder();
        String line;
        int lineCount = 0;
        boolean lineLimited = false;
        boolean charLimited = false;
        while ((line = br.readLine()) != null) {
            if (lineCount >= maxLines) {
                lineLimited = true;
                break;
            }
            if (!appendWithLimit(out, line, maxChars)) {
                charLimited = true;
                break;
            }
            lineCount++;
        }
        if (lineLimited || charLimited) {
            out.append("... (truncated");
            if (lineLimited) {
                out.append("; maxLines=").append(maxLines);
            }
            if (charLimited) {
                out.append("; maxChars=").append(maxChars);
            }
            out.append(")\n");
        }
        return out.toString();
    }

    private String readHead(BufferedReader br, int n, int maxChars) throws Exception {
        if (n <= 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        String line;
        int i = 0;
        boolean charLimited = false;
        while (i < n && (line = br.readLine()) != null) {
            if (!appendWithLimit(out, line, maxChars)) {
                charLimited = true;
                break;
            }
            i++;
        }
        if (charLimited) {
            out.append("... (truncated; maxChars=").append(maxChars).append(")\n");
        }
        return out.toString();
    }

    /**
     * 从文件末尾倒读最后 n 行，使用 RandomAccessFile 避免读取整个文件。
     *
     * <p>算法：先估算偏移量（n * TAIL_AVG_LINE_BYTES），从该偏移向后读取并收集行；
     * 若行数不足则向前再扩大块继续读，直到行数满足或读到文件头。
     * 最终只返回末尾 n 行。</p>
     */
    private String readTailFast(File f, int n, int maxChars) throws Exception {
        if (n <= 0) {
            return "";
        }
        if (n > MAX_LINES_UPPER_BOUND) {
            n = MAX_LINES_UPPER_BOUND;
        }

        long fileLen = f.length();
        if (fileLen == 0) {
            return "";
        }

        // 估算起始读取位置，先按 n * avgLineBytes 倒推，最小读 1 个 chunk。
        long estimatedStart = fileLen - (long) n * TAIL_AVG_LINE_BYTES;
        if (estimatedStart < 0) {
            estimatedStart = 0;
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");

            // 分块向前扩展，直到收集到足够的行或读到文件头。
            long readStart = estimatedStart;
            List<String> lines = null;
            while (true) {
                raf.seek(readStart);
                byte[] chunk = new byte[(int) Math.min(fileLen - readStart, fileLen)];
                int read = raf.read(chunk);
                if (read <= 0) {
                    break;
                }
                // 若不是文件头，跳过第一个不完整行（因为 readStart 可能在行中间）。
                String raw = new String(chunk, 0, read, StandardCharsets.UTF_8);
                List<String> parsed = splitLines(raw);
                if (readStart > 0 && parsed.size() > 1) {
                    // 非文件头时首行可能是残缺行，丢弃。
                    parsed.remove(0);
                }
                if (parsed.size() >= n || readStart == 0) {
                    lines = parsed;
                    break;
                }
                // 行数还不够，向前多读一个 chunk。
                readStart -= TAIL_CHUNK_SIZE;
                if (readStart < 0) {
                    readStart = 0;
                }
            }

            if (lines == null || lines.isEmpty()) {
                return "";
            }

            // 只取末尾 n 行。
            int fromIdx = lines.size() > n ? lines.size() - n : 0;
            StringBuilder out = new StringBuilder();
            boolean charLimited = false;
            for (int i = fromIdx; i < lines.size(); i++) {
                if (!appendWithLimit(out, lines.get(i), maxChars)) {
                    charLimited = true;
                    break;
                }
            }
            if (charLimited) {
                out.append("... (truncated; maxChars=").append(maxChars).append(")\n");
            }
            return out.toString();
        } finally {
            McpUtils.tryClose(raf);
        }
    }

    /**
     * 按 \n 或 \r\n 分割字符串为行列表，保留空行但去掉末尾多余空行。
     */
    private List<String> splitLines(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        int len = text.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                int end = i;
                if (end > start && text.charAt(end - 1) == '\r') {
                    end--;
                }
                result.add(text.substring(start, end));
                start = i + 1;
            }
        }
        // 最后一段（无换行结尾）
        if (start < len) {
            result.add(text.substring(start));
        }
        return result;
    }
}

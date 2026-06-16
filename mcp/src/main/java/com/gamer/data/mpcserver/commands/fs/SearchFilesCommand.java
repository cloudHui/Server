package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
/**
 * 递归搜索文件/目录。
 *
 * <p>pattern 支持通配：</p>
 * <ul>
 *   <li>* 匹配任意长度字符串（不含路径分隔的限制不做区分）</li>
 *   <li>? 匹配单个字符</li>
 * </ul>
 *
 * <p>匹配对象为“相对 path 的相对路径”，统一用 '/' 作为分隔符。</p>
 */ 
@Process("search_files")
public class SearchFilesCommand implements CommandHandler {
    private static final int DEFAULT_MAX_RESULTS = 2000;
    private static final int MAX_MAX_RESULTS = 50000;
    private static final int DEFAULT_MAX_DEPTH = 20;
    private static final int MAX_MAX_DEPTH = 100;
    private static final int DEFAULT_MAX_NODES = 20000;
    private static final int MAX_MAX_NODES = 200000;
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 200000;
    private static final int MAX_OUTPUT_CHARS_UPPER_BOUND = 1000000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        String pattern = McpUtils.text(params, "pattern");
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("params.pattern不能为空");
        }
        String excludePatterns = McpUtils.text(params, "excludePatterns");
        int maxResults = normalizePositiveInt(McpUtils.intVal(params, "maxResults"), DEFAULT_MAX_RESULTS, MAX_MAX_RESULTS);
        int maxDepth = normalizePositiveInt(McpUtils.intVal(params, "maxDepth"), DEFAULT_MAX_DEPTH, MAX_MAX_DEPTH);
        int maxNodes = normalizePositiveInt(McpUtils.intVal(params, "maxNodes"), DEFAULT_MAX_NODES, MAX_MAX_NODES);
        int maxOutputChars = normalizePositiveInt(
            McpUtils.intVal(params, "maxOutputChars"), DEFAULT_MAX_OUTPUT_CHARS, MAX_OUTPUT_CHARS_UPPER_BOUND);

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }
        File baseDir = sbx.requireAllowedDirectory(path);

        List<String> excludes = parseExcludePatterns(excludePatterns);
        List<String> matches = new ArrayList<>();
        SearchState state = new SearchState(maxResults, maxDepth, maxNodes);
        state.visitedDirs.add(safeCanonical(baseDir));
        long startNano = System.nanoTime();
        walk(baseDir, baseDir, pattern.trim(), excludes, sbx, 0, matches, state);
        long elapsedMs = (System.nanoTime() - startNano) / 1000000L;

        if (matches.isEmpty()) {
            String noMatch = buildNoMatchOutput(elapsedMs, state, maxOutputChars);
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "searchFiles path=" + path + " pattern=" + pattern, noMatch);
            return CommandResult.of(noMatch);
        }
        StringBuilder sb = new StringBuilder();
        appendSearchHeader(sb, maxOutputChars, elapsedMs, state);
        boolean charTruncated = false;
        for (String match : matches) {
            if (!appendWithLimit(sb, match, maxOutputChars)) {
                charTruncated = true;
                break;
            }
        }
        if (charTruncated || state.reachedAnyLimit()) {
            appendWithLimit(sb, buildTruncatedHint(state, charTruncated), maxOutputChars);
        }
        String searchOut = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "searchFiles path=" + path + " pattern=" + pattern, searchOut);
        return CommandResult.of(searchOut);
    }

    private String buildNoMatchOutput(long elapsedMs, SearchState state, int maxOutputChars) {
        StringBuilder sb = new StringBuilder();
        appendSearchHeader(sb, maxOutputChars, elapsedMs, state);
        if (state.reachedAnyLimit()) {
            appendWithLimit(sb, "No matches found (search truncated by limits)", maxOutputChars);
        } else {
            appendWithLimit(sb, "No matches found", maxOutputChars);
        }
        return sb.toString();
    }

    private void appendSearchHeader(StringBuilder sb, int maxOutputChars, long elapsedMs, SearchState state) {
        String h = "SEARCH_MS=" + elapsedMs +
                " nodesVisited=" + state.nodesVisited +
                " maxResults=" + state.maxResults +
                " maxDepth=" + state.maxDepth +
                " maxNodes=" + state.maxNodes +
                " depthLimited=" + state.depthLimited +
                " resultLimited=" + state.resultLimited +
                " nodeLimited=" + state.nodeLimited;
        appendWithLimit(sb, h, maxOutputChars);
    }

    private void walk(File root, File cur, String pattern, List<String> excludes, FileSandbox sbx, int depth,
        List<String> out, SearchState state) {
        if (state.shouldStop()) {
            return;
        }
        if (depth > state.maxDepth) {
            state.depthLimited = true;
            return;
        }
        if (cur == null) {
            return;
        }
        File[] children = cur.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c == null) {
                continue;
            }
            if (state.nodesVisited >= state.maxNodes) {
                state.nodeLimited = true;
                return;
            }
            state.nodesVisited++;

            if (c.isDirectory()) {
                // 目录：使用 canonical 校验，防止 symlink/junction 指向沙箱外部。
                if (!sbx.isPathAllowed(c)) {
                    continue;
                }
                String rel = relativize(root, c);
                if (matchesAny(rel, excludes)) {
                    continue;
                }
                if (wildcardMatch(pattern, rel)) {
                    if (out.size() >= state.maxResults) {
                        state.resultLimited = true;
                        return;
                    }
                    out.add(c.getPath());
                }
                String canon = safeCanonical(c);
                if (state.visitedDirs.contains(canon)) {
                    continue;
                }
                state.visitedDirs.add(canon);
                walk(root, c, pattern, excludes, sbx, depth + 1, out, state);
            } else {
                // 普通文件：使用快速绝对路径校验，跳过 canonical 系统调用。
                if (!sbx.isPathAllowedFast(c)) {
                    continue;
                }
                String rel = relativize(root, c);
                if (matchesAny(rel, excludes)) {
                    continue;
                }
                if (wildcardMatch(pattern, rel)) {
                    if (out.size() >= state.maxResults) {
                        state.resultLimited = true;
                        return;
                    }
                    out.add(c.getPath());
                }
            }
        }
    }

    private List<String> parseExcludePatterns(String excludePatterns) {
        List<String> out = new ArrayList<>();
        if (excludePatterns == null || excludePatterns.trim().isEmpty()) {
            return out;
        }
        String[] arr = excludePatterns.split(",");
        for (String s : arr) {
            String p = s == null ? null : s.trim();
            if (p == null || p.isEmpty()) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    private boolean matchesAny(String rel, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        String text = rel == null ? "" : rel;
        for (String p : patterns) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            if (wildcardMatch(p, text)) {
                return true;
            }
        }
        return false;
    }

    private String relativize(File root, File child) {
        String r = root.getAbsolutePath().replace('\\', '/');
        String c = child.getAbsolutePath().replace('\\', '/');
        if (c.startsWith(r)) {
            String s = c.substring(r.length());
            if (s.startsWith("/")) {
                s = s.substring(1);
            }
            return s;
        }
        return c;
    }

    private String safeCanonical(File f) {
        if (f == null) {
            return "";
        }
        try {
            return f.getCanonicalPath();
        } catch (Exception e) {
            return f.getAbsolutePath();
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

    private String buildTruncatedHint(SearchState state, boolean charTruncated) {
        StringBuilder sb = new StringBuilder();
        sb.append("... (truncated");
        if (charTruncated) {
            sb.append("; maxOutputChars reached");
        }
        if (state.resultLimited) {
            sb.append("; maxResults=").append(state.maxResults);
        }
        if (state.depthLimited) {
            sb.append("; maxDepth=").append(state.maxDepth);
        }
        if (state.nodeLimited) {
            sb.append("; maxNodes=").append(state.maxNodes);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 简单通配符匹配：* 和 ?。
     */
    private boolean wildcardMatch(String pattern, String text) {
        if (pattern == null) {
            return false;
        }
        if (text == null) {
            text = "";
        }
        // 统一分隔符
        text = text.replace('\\', '/');
        pattern = pattern.replace('\\', '/');

        int p = 0;
        int t = 0;
        int star = -1;
        int mark = -1;

        while (t < text.length()) {
            if (p < pattern.length() && (pattern.charAt(p) == '?' || pattern.charAt(p) == text.charAt(t))) {
                p++;
                t++;
                continue;
            }
            if (p < pattern.length() && pattern.charAt(p) == '*') {
                star = p;
                p++;
                mark = t;
                continue;
            }
            if (star != -1) {
                p = star + 1;
                mark++;
                t = mark;
                continue;
            }
            return false;
        }

        while (p < pattern.length() && pattern.charAt(p) == '*') {
            p++;
        }
        return p == pattern.length();
    }

    private static class SearchState {
        private final int maxResults;
        private final int maxDepth;
        private final int maxNodes;
        private int nodesVisited;
        private boolean resultLimited;
        private boolean depthLimited;
        private boolean nodeLimited;
        private final Set<String> visitedDirs = new HashSet<>();

        private SearchState(int maxResults, int maxDepth, int maxNodes) {
            this.maxResults = maxResults;
            this.maxDepth = maxDepth;
            this.maxNodes = maxNodes;
        }

        private boolean shouldStop() {
            return resultLimited || nodeLimited;
        }

        private boolean reachedAnyLimit() {
            return resultLimited || depthLimited || nodeLimited;
        }
    }
}


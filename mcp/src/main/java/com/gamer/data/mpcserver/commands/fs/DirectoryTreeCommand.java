package com.gamer.data.mpcserver.commands.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
 * 递归目录树。
 */
@Process("directory_tree")
public class DirectoryTreeCommand implements CommandHandler {
    private static final int DEFAULT_MAX_DEPTH = 20;
    private static final int MAX_MAX_DEPTH = 100;
    private static final int DEFAULT_MAX_NODES = 20000;
    private static final int MAX_MAX_NODES = 200000;
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 200000;
    private static final int MAX_OUTPUT_CHARS_UPPER_BOUND = 1000000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String path = McpUtils.text(params, "path");
        String excludePatterns = McpUtils.text(params, "excludePatterns");
        int maxDepth = normalizePositiveInt(McpUtils.intVal(params, "maxDepth"), DEFAULT_MAX_DEPTH, MAX_MAX_DEPTH);
        int maxNodes = normalizePositiveInt(McpUtils.intVal(params, "maxNodes"), DEFAULT_MAX_NODES, MAX_MAX_NODES);
        int maxOutputChars = normalizePositiveInt(
            McpUtils.intVal(params, "maxOutputChars"), DEFAULT_MAX_OUTPUT_CHARS, MAX_OUTPUT_CHARS_UPPER_BOUND);

        FileSandbox sbx = ctx.fileSandbox();
        if (sbx == null) {
            throw new IllegalStateException("FileSandbox未初始化");
        }

        File root = sbx.requireAllowedDirectory(path);

        List<String> patterns = new ArrayList<>();
        if (excludePatterns != null && !excludePatterns.trim().isEmpty()) {
            String[] arr = excludePatterns.split(",");
            for (String s : arr) {
                String p = s == null ? null : s.trim();
                if (p != null && !p.isEmpty()) {
                    patterns.add(p);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean rootOk = appendWithLimit(sb, root.getPath(), maxOutputChars);
        if (!rootOk) {
            String trunc = "... (truncated; maxOutputChars reached)";
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
                "directoryTree path=" + path + " maxDepth=" + maxDepth + " maxNodes=" + maxNodes, trunc);
            return CommandResult.of(trunc);
        }
        TreeState state = new TreeState(maxDepth, maxNodes);
        state.visitedDirs.add(safeCanonical(root));
        walk(root, root, 0, patterns, sbx, sb, state, maxOutputChars);
        if (state.reachedAnyLimit()) {
            appendWithLimit(sb, buildTruncatedHint(state), maxOutputChars);
        }

        String treeOut = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_FS,
            "directoryTree path=" + path + " maxDepth=" + maxDepth + " maxNodes=" + maxNodes, treeOut);
        return CommandResult.of(treeOut);
    }

    private void walk(File root, File cur, int depth, List<String> patterns, FileSandbox sbx, StringBuilder out,
        TreeState state, int maxOutputChars) {
        if (state.shouldStop) {
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
        if (children == null || children.length == 0) {
            return;
        }
        Arrays.sort(children, (o1, o2) -> {
            String a = o1 == null ? "" : o1.getName();
            String b = o2 == null ? "" : o2.getName();
            return a.compareTo(b);
        });

        for (File c : children) {
            if (c == null) {
                continue;
            }
            if (state.nodeCount >= state.maxNodes) {
                state.nodeLimited = true;
                state.shouldStop = true;
                return;
            }
            state.nodeCount++;

            // 目录：用 canonical 校验防 symlink 越界；普通文件：用快速绝对路径校验。
            if (c.isDirectory()) {
                if (!sbx.isPathAllowed(c)) {
                    continue;
                }
            } else {
                if (!sbx.isPathAllowedFast(c)) {
                    continue;
                }
            }

            String rel = relativize(root, c);
            if (matchesAny(rel, patterns)) {
                continue;
            }
            for (int d = 0; d < depth + 1; d++) {
                if (!appendRawWithLimit(out, maxOutputChars)) {
                    state.outputLimited = true;
                    state.shouldStop = true;
                    return;
                }
            }
            String line = (c.isDirectory() ? "[DIR] " : "[FILE] ") + c.getName();
            if (!appendWithLimit(out, line, maxOutputChars)) {
                state.outputLimited = true;
                state.shouldStop = true;
                return;
            }
            if (c.isDirectory()) {
                String canon = safeCanonical(c);
                if (state.visitedDirs.contains(canon)) {
                    continue;
                }
                state.visitedDirs.add(canon);
                walk(root, c, depth + 1, patterns, sbx, out, state, maxOutputChars);
            }
        }
    }

    private boolean matchesAny(String rel, List<String> patterns) {
        if (rel == null) {
            rel = "";
        }
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String p : patterns) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            String pp = p.replace('\\', '/');
            String rr = rel.replace('\\', '/');
            if (wildcardMatch(pp, rr)) {
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

    private boolean appendRawWithLimit(StringBuilder out, int maxChars) {
        if (out.length() >= maxChars) {
            return false;
        }
        String s = "  ";
        int remain = maxChars - out.length();
        if (remain <= 0) {
            return false;
        }
        if (s.length() > remain) {
            out.append(s, 0, remain);
            return false;
        }
        out.append(s);
        return true;
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

    private String buildTruncatedHint(TreeState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("... (truncated");
        if (state.outputLimited) {
            sb.append("; maxOutputChars reached");
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

    private static class TreeState {
        private final int maxDepth;
        private final int maxNodes;
        private int nodeCount;
        private boolean depthLimited;
        private boolean nodeLimited;
        private boolean outputLimited;
        private boolean shouldStop;
        private final Set<String> visitedDirs = new HashSet<>();

        private TreeState(int maxDepth, int maxNodes) {
            this.maxDepth = maxDepth;
            this.maxNodes = maxNodes;
        }

        private boolean reachedAnyLimit() {
            return depthLimited || nodeLimited || outputLimited;
        }
    }
}

package com.gamer.data.mpcserver.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文件系统沙箱：所有文件/目录访问必须限制在 allowedRoots 内。
 *
 * <p>设计目标：</p>
 * <ul>
 *   <li>防止 ../ 路径穿越</li>
 *   <li>尽可能处理符号链接/junction 导致的路径逃逸</li>
 *   <li>允许对“尚不存在的目标文件”进行写入校验（通过父目录校验）</li>
 * </ul>
 *
 * <p>注意：为兼容项目热更新限制，不使用 lambda/stream/try-with-resources。</p>
 */
public class FileSandbox {
    private final File workDir;
    private final List<File> allowedRootsCanonical;
    private final List<File> allowedRootsAbsolute;

    public FileSandbox(File workDir, List<File> allowedRoots) {
        if (workDir == null) {
            workDir = new File(System.getProperty("user.dir"));
        }
        this.workDir = workDir.getAbsoluteFile();
        if (allowedRoots == null || allowedRoots.isEmpty()) {
            allowedRoots = new ArrayList<>();
            allowedRoots.add(this.workDir);
        }
        this.allowedRootsCanonical = new ArrayList<>();
        this.allowedRootsAbsolute = new ArrayList<>();
        for (File r : allowedRoots) {
            if (r == null) {
                continue;
            }
            File abs = resolveAgainstWorkDir(r);
            if (abs == null) {
                continue;
            }
            allowedRootsAbsolute.add(abs);
            try {
                allowedRootsCanonical.add(abs.getCanonicalFile());
            } catch (Exception ignored) {
                allowedRootsCanonical.add(abs.getAbsoluteFile());
            }
        }
    }

    /**
     * 以“原始入参文本”形式返回 allowedRoots，便于 tools 输出。
     */
    public List<String> allowedRootPaths() {
        List<String> out = new ArrayList<>();
        for (File file : allowedRootsAbsolute) {
            out.add(file.getPath());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 将入参 path 解析为受限路径（File）。对不存在的文件也可校验：
     * - 如果目标不存在：校验其父目录（必须存在且在 allowedRoots 内），并返回规范化的绝对路径。
     */
    public File requireAllowedPath(String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("params.path不能为空");
        }
        File f = resolveAgainstWorkDir(new File(inputPath.trim()));

        // 1) 如果存在，则用 canonical 校验（可解析符号链接/junction）。
        if (f.exists()) {
            File canon = toCanonicalQuiet(f);
            ensureUnderAllowedRoots(canon);
            return canon;
        }

        // 2) 不存在：校验父目录（必须存在），避免写到沙箱外。
        File absNorm = f.getAbsoluteFile();
        File parent = absNorm.getParentFile();
        if (parent == null) {
            throw new IllegalArgumentException("非法路径（无父目录）: " + absNorm.getPath());
        }
        if (!parent.exists() || !parent.isDirectory()) {
            throw new IllegalArgumentException("父目录不存在: " + parent.getPath());
        }
        File parentCanon = toCanonicalQuiet(parent);
        ensureUnderAllowedRoots(parentCanon);
        return absNorm;
    }

    /**
     * 获取一个“必须是存在目录”的受限路径。
     */
    public File requireAllowedDirectory(String inputPath) {
        File f = requireAllowedPath(inputPath);
        if (!f.exists()) {
            throw new IllegalArgumentException("目录不存在: " + f.getPath());
        }
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("不是目录: " + f.getPath());
        }
        return f;
    }

    /**
     * 获取一个“必须是存在文件”的受限路径。
     */
    public File requireAllowedFile(String inputPath) {
        File f = requireAllowedPath(inputPath);
        if (!f.exists()) {
            throw new IllegalArgumentException("文件不存在: " + f.getPath());
        }
        if (!f.isFile()) {
            throw new IllegalArgumentException("不是文件: " + f.getPath());
        }
        return f;
    }

    /**
     * 判断给定路径是否位于允许根目录内（完整版，含 canonical 解析）。
     *
     * <p>与 requireAllowedPath 的区别：</p>
     * <ul>
     *   <li>本方法返回 boolean，不抛业务异常，便于遍历时快速过滤</li>
     *   <li>会优先使用 canonical 路径，尽量避免符号链接/junction 越界</li>
     * </ul>
     */
    public boolean isPathAllowed(File inputPath) {
        if (inputPath == null) {
            return false;
        }
        try {
            File abs = resolveAgainstWorkDir(inputPath);
            if (abs == null) {
                return false;
            }
            File target = abs.exists() ? toCanonicalQuiet(abs) : abs.getAbsoluteFile();
            ensureUnderAllowedRoots(target);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 轻量校验：仅用绝对路径（不做 canonical/系统调用）快速判断路径是否在允许范围内。
     *
     * <p>适用于遍历时对大量普通文件节点的快速过滤：</p>
     * <ul>
     *   <li>普通文件不含符号链接/junction，绝对路径校验足够安全。</li>
     *   <li>目录节点建议继续使用 {@link #isPathAllowed(File)}（含 canonical）以防 symlink 越界。</li>
     * </ul>
     */
    public boolean isPathAllowedFast(File inputPath) {
        if (inputPath == null) {
            return false;
        }
        try {
            File abs = resolveAgainstWorkDir(inputPath);
            if (abs == null) {
                return false;
            }
            File target = abs.getAbsoluteFile();
            ensureUnderAllowedRoots(target);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private File resolveAgainstWorkDir(File f) {
        if (f == null) {
            return null;
        }
        if (f.isAbsolute()) {
            return f.getAbsoluteFile();
        }
        return new File(workDir, f.getPath()).getAbsoluteFile();
    }

    private File toCanonicalQuiet(File f) {
        if (f == null) {
            return null;
        }
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
            return f.getAbsoluteFile();
        }
    }

    private void ensureUnderAllowedRoots(File target) {
        if (target == null) {
            throw new IllegalArgumentException("非法路径: null");
        }

        File targetCanon = toCanonicalQuiet(target);

        // 先用 canonical roots 校验，减少 symlink/junction 逃逸风险。
        for (File root : allowedRootsCanonical) {
            if (isUnderRoot(targetCanon, root)) {
                return;
            }
        }
        // 再用 absolute roots 兜底（root 本身可能不存在导致 canonical 不稳定）。
        File targetAbs = target.getAbsoluteFile();
        for (File root : allowedRootsAbsolute) {
            if (isUnderRoot(targetAbs, root)) {
                return;
            }
        }
        throw new IllegalArgumentException("路径不在允许范围内: " + target.getPath());
    }

    private boolean isUnderRoot(File target, File root) {
        if (target == null || root == null) {
            return false;
        }
        String t = normalizeForPrefix(target);
        String r = normalizeForPrefix(root);
        if (t.equals(r)) {
            return true;
        }
        // 必须以 root + separator 开头，避免 /a/b2 误匹配 /a/b
        if (!r.endsWith("/")) {
            r = r + "/";
        }
        return t.startsWith(r);
    }

    private String normalizeForPrefix(File f) {
        String p = f.getAbsolutePath();
        // Windows/跨平台：统一 separator，便于 startsWith 校验
        p = p.replace('\\', '/');
        // 简单去掉尾部 /
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}


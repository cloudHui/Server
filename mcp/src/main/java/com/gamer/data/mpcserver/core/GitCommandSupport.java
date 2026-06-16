package com.gamer.data.mpcserver.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 在指定仓库目录执行只读 git 命令，并截断输出。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
public final class GitCommandSupport {

    /** git_run 允许的子命令（只读） */
    public static final Set<String> READ_ONLY_SUBCOMMANDS = new HashSet<String>(Arrays.asList(
        "log", "show", "diff", "diff-tree", "rev-parse", "status", "branch", "config"));

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private GitCommandSupport() {}

    /**
     * 解析并校验仓库目录。
     *
     * @param defaults
     *            Git 默认配置
     * @return 仓库 File
     */
    public static File requireRepo(GitDefaults defaults) {
        if (defaults == null || defaults.getRepoDir() == null) {
            throw new IllegalStateException("Git 仓库未配置，请在 mcp.json 中设置 --gitRepo= 路径");
        }
        File repo = defaults.getRepoDir();
        if (!repo.isDirectory()) {
            throw new IllegalArgumentException("gitRepo 不是有效目录: " + repo.getPath());
        }
        File gitDir = new File(repo, ".git");
        if (!gitDir.exists()) {
            throw new IllegalArgumentException("目录不是 Git 仓库（缺少 .git）: " + repo.getPath());
        }
        return repo;
    }

    /**
     * 执行 git 参数列表（不含可执行文件名 git 本身）。
     *
     * @param repo
     *            仓库根目录
     * @param gitArgs
     *            git 子参数，如 log、show
     * @param maxOutputChars
     *            最大输出字符
     * @return 标准输出+标准错误合并文本
     */
    public static String runGit(File repo, List<String> gitArgs, int maxOutputChars) throws Exception {
        return runGit(repo, gitArgs, maxOutputChars, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行 git 并限制超时。
     */
    public static String runGit(File repo, List<String> gitArgs, int maxOutputChars, int timeoutSeconds)
        throws Exception {
        if (gitArgs == null || gitArgs.isEmpty()) {
            throw new IllegalArgumentException("git 参数不能为空");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(gitArgs);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(repo);
        pb.redirectErrorStream(true);
        java.lang.Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() >= maxOutputChars) {
                    out.append("\n... [输出已截断，超过 maxOutputChars=").append(maxOutputChars).append("]\n");
                    break;
                }
                if (out.length() > 0) {
                    out.append('\n');
                }
                int remain = maxOutputChars - out.length();
                if (line.length() > remain) {
                    out.append(line, 0, remain);
                    out.append("\n... [输出已截断]\n");
                    break;
                }
                out.append(line);
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("git 命令超时（" + timeoutSeconds + "s）: " + joinArgs(gitArgs));
        }
        int exit = process.exitValue();
        String text = out.toString();
        if (exit != 0) {
            throw new IllegalStateException("git 退出码 " + exit + "，命令: git " + joinArgs(gitArgs) + "\n输出:\n" + text);
        }
        return text;
    }

    /**
     * 校验 git_run 的首个参数为只读子命令。
     */
    public static void assertReadOnlySubCommand(String subCommand) {
        if (subCommand == null || subCommand.trim().isEmpty()) {
            throw new IllegalArgumentException("git 子命令不能为空");
        }
        String sc = subCommand.trim().toLowerCase();
        if (!READ_ONLY_SUBCOMMANDS.contains(sc)) {
            throw new IllegalArgumentException("不允许的 git 子命令: " + subCommand + "，允许: " + READ_ONLY_SUBCOMMANDS);
        }
    }

    /**
     * 拒绝明显危险的参数片段（双保险）。
     */
    public static void assertSafeArgs(List<String> gitArgs) {
        if (gitArgs == null) {
            return;
        }
        int i;
        for (i = 0; i < gitArgs.size(); i++) {
            String a = gitArgs.get(i);
            if (a == null) {
                continue;
            }
            String lower = a.toLowerCase();
            if (lower.contains("push") || lower.contains("reset") || lower.contains("checkout")
                || lower.contains("merge") || lower.contains("rebase") || lower.contains("commit")
                || lower.contains("clean") || lower.contains("stash")) {
                throw new IllegalArgumentException("参数含禁止片段: " + a);
            }
        }
        assertConfigReadOnly(gitArgs);
    }

    /**
     * git config 仅允许读取（user.name / user.email / --get / --list 等），禁止 set/unset。
     */
    public static void assertConfigReadOnly(List<String> gitArgs) {
        if (gitArgs == null || gitArgs.isEmpty()) {
            return;
        }
        if (!"config".equalsIgnoreCase(gitArgs.get(0).trim())) {
            return;
        }
        int i;
        for (i = 1; i < gitArgs.size(); i++) {
            String a = gitArgs.get(i);
            if (a == null) {
                continue;
            }
            String lower = a.trim().toLowerCase();
            if ("set".equals(lower) || "unset".equals(lower) || "remove".equals(lower)
                || "replace-all".equals(lower) || lower.contains("--replace")) {
                throw new IllegalArgumentException("git config 只读，禁止写入: " + a);
            }
        }
    }

    private static String joinArgs(List<String> args) {
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(args.get(i));
        }
        return sb.toString();
    }
}

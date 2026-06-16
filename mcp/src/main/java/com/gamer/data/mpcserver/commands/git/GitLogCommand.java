package com.gamer.data.mpcserver.commands.git;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.GitCommandSupport;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 按日期区间列出提交（只读 git log）。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
@Process("git_log")
public class GitLogCommand implements CommandHandler {

    private static final int DEFAULT_MAX_COUNT = 500;
    private static final int MAX_COUNT_UPPER = 2000;

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        File repo = GitCommandSupport.requireRepo(ctx.gitDefaults());
        String since = McpUtils.text(params, "since");
        String until = McpUtils.text(params, "until");
        if (since == null || since.trim().isEmpty()) {
            throw new IllegalArgumentException("since 不能为空，格式 yyyy-MM-dd");
        }
        String author = McpUtils.text(params, "author");
        boolean noMerges = !"false".equalsIgnoreCase(McpUtils.text(params, "noMerges"));
        int maxCount = normalizeMaxCount(McpUtils.intVal(params, "maxCount"));
        int maxChars = normalizeMaxChars(McpUtils.intVal(params, "maxOutputChars"));

        List<String> args = new ArrayList<>();
        args.add("log");
        args.add("--date=short");
        args.add("--pretty=format:%h|%ad|%an|%s");
        args.add("--since=" + since.trim());
        if (until != null && !until.trim().isEmpty()) {
            args.add("--until=" + until.trim());
        }
        if (author != null && !author.trim().isEmpty()) {
            args.add("--author=" + author.trim());
        }
        if (noMerges) {
            args.add("--no-merges");
        }
        args.add("-n");
        args.add(String.valueOf(maxCount));

        GitCommandSupport.assertSafeArgs(args);
        String out = GitCommandSupport.runGit(repo, args, maxChars);
        return CommandResult.of(out);
    }

    private static int normalizeMaxCount(Integer v) {
        if (v == null || v <= 0) {
            return DEFAULT_MAX_COUNT;
        }
        if (v > MAX_COUNT_UPPER) {
            return MAX_COUNT_UPPER;
        }
        return v;
    }

    private static int normalizeMaxChars(Integer v) {
        if (v == null || v <= 0) {
            return 200000;
        }
        if (v > 500000) {
            return 500000;
        }
        return v;
    }
}

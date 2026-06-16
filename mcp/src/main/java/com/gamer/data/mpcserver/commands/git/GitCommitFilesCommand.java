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
 * 列出单条提交变更文件（git diff-tree --name-status）。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
@Process("git_commit_files")
public class GitCommitFilesCommand implements CommandHandler {

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        File repo = GitCommandSupport.requireRepo(ctx.gitDefaults());
        String commit = McpUtils.text(params, "commit");
        if (commit == null || commit.trim().isEmpty()) {
            throw new IllegalArgumentException("commit 不能为空（hash）");
        }
        int maxChars = McpUtils.intOrDefault(params, "maxOutputChars", 200000);

        List<String> args = new ArrayList<>();
        args.add("diff-tree");
        args.add("--no-commit-id");
        args.add("--name-status");
        args.add("-r");
        args.add(commit.trim());

        GitCommandSupport.assertSafeArgs(args);
        String out = GitCommandSupport.runGit(repo, args, maxChars);
        return CommandResult.of(out);
    }
}

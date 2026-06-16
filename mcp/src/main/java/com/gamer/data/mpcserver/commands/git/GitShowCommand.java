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
 * 查看单条提交：--stat 或 patch diff（只读 git show）。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
@Process("git_show")
public class GitShowCommand implements CommandHandler {

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        File repo = GitCommandSupport.requireRepo(ctx.gitDefaults());
        String commit = McpUtils.text(params, "commit");
        if (commit == null || commit.trim().isEmpty()) {
            throw new IllegalArgumentException("commit 不能为空（hash）");
        }
        boolean statOnly = "true".equalsIgnoreCase(McpUtils.text(params, "statOnly"));
        int maxChars = McpUtils.intOrDefault(params, "maxOutputChars", 500000);
        int contextLines = McpUtils.intOrDefault(params, "contextLines", 3);

        List<String> args = new ArrayList<>();
        args.add("show");
        args.add(commit.trim());
        args.add("--no-color");
        if (statOnly) {
            args.add("--stat");
        } else {
            args.add("-U");
            args.add(String.valueOf(contextLines));
        }

        GitCommandSupport.assertSafeArgs(args);
        String out = GitCommandSupport.runGit(repo, args, maxChars);
        return CommandResult.of(out);
    }
}

package com.gamer.data.mpcserver.commands.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.GitCommandSupport;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 执行只读 git 子命令（白名单：log/show/diff/diff-tree/rev-parse/status/branch）。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
@Process("git_run")
public class GitRunCommand implements CommandHandler {

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        File repo = GitCommandSupport.requireRepo(ctx.gitDefaults());
        List<String> gitArgs = parseArgsArray(params);
        if (gitArgs.isEmpty()) {
            throw new IllegalArgumentException("args 不能为空（JSON 字符串数组，首项为 git 子命令）");
        }
        GitCommandSupport.assertReadOnlySubCommand(gitArgs.get(0));
        GitCommandSupport.assertSafeArgs(gitArgs);

        int maxChars = McpUtils.intOrDefault(params, "maxOutputChars", 500000);
        int timeoutSec = McpUtils.intOrDefault(params, "timeoutSeconds", 60);

        String out = GitCommandSupport.runGit(repo, gitArgs, maxChars, timeoutSec);
        return CommandResult.of(out);
    }

    /**
     * 从 params.args 解析字符串数组。
     */
    private static List<String> parseArgsArray(JsonNode params) {
        List<String> list = new ArrayList<>();
        if (params == null) {
            return list;
        }
        JsonNode argsNode = params.get("args");
        if (argsNode == null || !argsNode.isArray()) {
            return list;
        }
        Iterator<JsonNode> it = argsNode.elements();
        while (it.hasNext()) {
            JsonNode n = it.next();
            if (n != null && !n.isNull()) {
                list.add(n.asText());
            }
        }
        return list;
    }
}

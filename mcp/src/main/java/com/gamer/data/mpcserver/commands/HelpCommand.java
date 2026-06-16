package com.gamer.data.mpcserver.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.Process;

/**
 * 帮助命令:输出当前支持的 method 列表与参数提示。
 *
 * <p>
 * 注意:这里的输出是给人看的帮助文本，并非严格的机器协议。
 * </p>
 * 
 * @author liuyunhui
 * @date 2026-04-13
 */
@Process("help")
public class HelpCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) {
        // 这里用 List + sort 保持输出稳定(即使未来增加条目也容易对比 diff)。
        List<String> lines = new ArrayList<>();
        lines.add("可用命令(method)示例:");
        lines.add("- help");
        lines.add("- ping");
        lines.add("- excel_describe_sheets {fileAbsolutePath}");
        lines.add("- excel_read_sheet {fileAbsolutePath, sheetName?, sheetIndex?, maxRows?, maxCols?}");
        lines.add("- excel_write_to_sheet {fileAbsolutePath, sheetName, newSheet, range, values}");
        lines.add("- local_get_database_info {}");
        lines.add("- local_sql_query {sql, maxRows?, queryTimeoutSeconds?}");
        lines.add("- local_check_permissions {}");
        lines.add("- get {key}");
        lines.add("- set {key, value, expireSeconds?}");
        lines.add("- delete {key}");
        lines.add("- list {pattern?}");
        lines.add("- read_text_file {path, head?, tail?}");
        lines.add("- write_file {path, content}");
        lines.add("- list_directory {path}");
        lines.add("- search_files {path, pattern}");
        Collections.sort(lines);

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return CommandResult.of(sb.toString());
    }
}

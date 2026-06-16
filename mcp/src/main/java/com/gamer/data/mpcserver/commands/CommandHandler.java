package com.gamer.data.mpcserver.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;

/**
 * 单个命令处理器。
 *
 * <p>约定：参数校验失败请抛 {@link IllegalArgumentException}，由入口统一转换为
 * {@code INVALID_PARAMS} 响应；其它异常将被视为内部错误。</p>
 */
public interface CommandHandler {
    /**
     * 执行命令并返回文本结果。
     *
     * @param ctx 共享上下文（ObjectMapper、日志等）
     * @param params 请求参数（可能为 null）
     * @return 命令结果（允许返回 null，入口会按空字符串处理）
     * @throws Exception 执行过程中的任意异常
     */
    CommandResult handle(CommandContext ctx, JsonNode params) throws Exception;

    /**
     * 追加字符串到 StringBuilder，并限制最大长度。
     * 
     * @param out
     *            StringBuilder
     * @param line
     *            要追加的字符串
     * @param maxChars
     *            最大长度
     * @return 是否追加成功
     */
    default boolean appendWithLimit(StringBuilder out, String line, int maxChars) {
        if (out.length() >= maxChars) {
            return false;
        }
        String s = line == null ? "" : line;
        int remain = maxChars - out.length();
        if (remain <= 1) {
            return false;
        }
        int canWrite = remain - 1;
        if (s.length() > canWrite) {
            out.append(s, 0, canWrite).append("\n");
            return false;
        }
        out.append(s).append("\n");
        return true;
    }
}


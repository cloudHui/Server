package com.gamer.data.mpcserver.core;

/**
 * 命令执行结果（当前仅包含一段文本）。
 *
 * <p>保持轻量：协议层最终会把该文本塞到 {@code {"result":{"text":...}}} 中。</p>
 */
public class CommandResult {
    public final String text;

    /**
     * @param text 输出文本（允许为空/空串；由调用方决定含义）
     */
    public CommandResult(String text) {
        this.text = text;
    }

    /**
     * 静态工厂，便于调用侧写法简洁。
     *
     * @param text 输出文本
     * @return CommandResult
     */
    public static CommandResult of(String text) {
        return new CommandResult(text);
    }
}


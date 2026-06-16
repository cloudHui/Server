package com.gamer.data.mpcserver.log;

/**
 * 简单日志接口。
 *
 * <p>该项目的 mcpserver 是一个 stdin/stdout 交互程序，日志主要用于：
 * 1) 记录关键事件与错误，便于排查；
 * 2) 保持输出协议（stdout 的 JSON 响应）不受日志影响。</p>
 */
public interface Log {
    /**
     * 记录一条日志。
     *
     * @param message 日志内容
     */
    void logMessage(String message);

    /**
     * 记录一条日志（可选的展示风格参数）。
     *
     * <p>redShow 当前实现未区分颜色，仅保留接口兼容性。</p>
     *
     * @param message 日志内容
     * @param redShow 是否希望高亮展示（实现可忽略）
     */
    void logMessage(String message, boolean redShow);
}


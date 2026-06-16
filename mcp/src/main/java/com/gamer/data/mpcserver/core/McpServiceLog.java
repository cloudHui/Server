package com.gamer.data.mpcserver.core;

import com.gamer.data.mpcserver.log.Log;

/**
 * MCP 各服务命令日志：一行命令摘要 + 一行处理结果（单行化并截断）。
 */
public final class McpServiceLog {
    /** 文件服务。 */
    public static final String SERVICE_FS = "fs";
    /** 表格(Excel) 服务。 */
    public static final String SERVICE_EXCEL = "excel";
    /** Redis 服务。 */
    public static final String SERVICE_REDIS = "redis";
    /** 数据库服务。 */
    public static final String SERVICE_DB = "db";

    public static final int MAX_CMD_CHARS = 4000;
    public static final int MAX_RESULT_CHARS = 32000;

    private McpServiceLog() {
    }

    /**
     * 记录命令说明（不含结果正文）。
     */
    public static void cmd(Log log, String service, String commandSummary) {
        String s = clip(McpUtils.nvl(McpUtils.oneLine(commandSummary)), MAX_CMD_CHARS);
        log.logMessage("[" + service + "] cmd=" + s);
    }

    /**
     * 记录返回给调用方的结果文本（与 {@link CommandResult} 一致，可截断）。
     *
     * <p>若以 {@code FILE=} 开头（Excel 读表等首行），不打印 {@code result=}，避免路径与大段表格进日文件。</p>
     */
    public static void result(Log log, String service, String resultText) {
        // Redis 命令结果可能很大或含敏感业务数据：只保留 cmd=，不写 result=。
        if (SERVICE_REDIS.equals(service)) {
            return;
        }
        String oneLine = McpUtils.nvl(McpUtils.oneLine(resultText));
        if (oneLine.startsWith("FILE=") || oneLine.startsWith("READ_MODE=")) {
            return;
        }
        String s = clip(oneLine, MAX_RESULT_CHARS);
        log.logMessage("[" + service + "] result=" + s);
    }

    /**
     * 先 {@link #cmd} 再 {@link #result}。
     */
    public static void cmdAndResult(Log log, String service, String commandSummary, String resultText) {
        cmd(log, service, commandSummary);
        result(log, service, resultText);
    }

    private static String clip(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "... (truncated; maxChars=" + maxChars + ")";
    }
}

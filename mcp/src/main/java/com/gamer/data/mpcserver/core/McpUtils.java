package com.gamer.data.mpcserver.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * mcpserver 内部通用小工具集合（保持轻量、无外部依赖）。
 *
 * <p>目的：收敛 commands/ 与入口中的重复代码（参数读取、字符串单行化、静默关闭资源等）。
 * 这些方法的实现刻意保持“宽容”：例如数字解析失败返回 null，而不是抛异常，便于调用侧决定默认值。</p>
 */
public final class McpUtils {
    private McpUtils() {
    }

    /**
     * 从 params 中读取一个文本字段。
     *
     * @param params 参数对象（可为 null）
     * @param key 字段名
     * @return 字符串值；不存在/为 null 时返回 null
     */
    public static String text(JsonNode params, String key) {
        if (params == null) {
            return null;
        }
        JsonNode n = params.get(key);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.asText();
    }

    /**
     * 从 params 中读取一个整数字段。
     *
     * <p>兼容两种形态：</p>
     * <ul>
     *   <li>JSON number：直接取 asInt()</li>
     *   <li>JSON string：trim 后尝试 parseInt</li>
     * </ul>
     *
     * @param params 参数对象（可为 null）
     * @param key 字段名
     * @return Integer；不存在/空/解析失败时返回 null
     */
    public static Integer intVal(JsonNode params, String key) {
        if (params == null) {
            return null;
        }
        JsonNode n = params.get(key);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isInt()) {
            return n.asInt();
        }
        String s = n.asText();
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取整数，若为空则返回默认值。
     */
    public static int intOrDefault(JsonNode params, String key, int def) {
        Integer v = intVal(params, key);
        return v == null ? def : v;
    }

    /**
     * 将字符串压成单行（把 CR/LF 变成空格）。
     *
     * <p>用于日志/TSV 输出，避免换行破坏“一行一个记录”的格式。</p>
     */
    public static String oneLine(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\r", " ").replace("\n", " ");
    }

    /**
     * null → 空串。
     */
    public static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * 安静关闭资源（忽略 close 异常）。
     */
    public static void tryClose(AutoCloseable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception ignored) {
        }
    }
}


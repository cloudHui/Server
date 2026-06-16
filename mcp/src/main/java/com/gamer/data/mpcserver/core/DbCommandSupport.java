package com.gamer.data.mpcserver.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * DB 相关命令的公共支持方法（不做任何 I/O）。
 *
 * <p>注意：为保持行为一致，这里刻意沿用现有命令的约束：</p>
 * <ul>
 *   <li>强制使用 {@link CommandContext#dbDefaults()} 作为连接来源</li>
 *   <li>params.url/user/password 由命令读取但最终会被忽略</li>
 *   <li>params.database 若为空则回退到 defaults.database()</li>
 * </ul>
 */
public final class DbCommandSupport {
    private DbCommandSupport() {
    }

    /**
     * DB 工具的前置条件：必须提供默认连接信息。
     *
     * <p>这里不允许命令侧自由传 url/user/password，原因是该 mcpserver 通常以“工具进程”的形式
     * 运行，连接信息由启动脚本或运行环境统一注入，避免调用方（尤其是远端/自动化）随意指向其它库。</p>
     *
     * <p>因此命令仍然保留 params.url/user/password 字段（协议兼容），但最终会被忽略。</p>
     */
    public static DbDefaults requireDbDefaults(CommandContext ctx) {
        if (ctx == null || ctx.dbDefaults() == null) {
            throw new IllegalArgumentException(
                "未配置默认数据库连接，请在启动命令中传入 --dbHost/--dbPort/--dbUser/--dbPassword（可选 --dbDatabase）");
        }
        return ctx.dbDefaults();
    }

    /**
     * 解析目标数据库名（可选）。
     *
     * <p>优先使用 params.database；为空则回退 defaults.database()；两者都为空时返回 null，
     * 由 {@link DbDefaults#buildJdbcUrl(String)} 继续做兜底（当前兜底为 mysql）。</p>
     */
    public static String resolveDatabase(JsonNode params, DbDefaults defaults) {
        String db = McpUtils.text(params, "database");
        if (db == null || db.trim().isEmpty()) {
            db = defaults.database();
        }
        return db;
    }

    /**
     * 解析最终 JDBC URL。
     *
     * <p>重要：该 URL 完全由启动参数（defaults）生成，params.url 仅为兼容保留。</p>
     */
    public static String resolveJdbcUrl(CommandContext ctx, JsonNode params) {
        DbDefaults d = requireDbDefaults(ctx);
        String db = resolveDatabase(params, d);
        return d.buildJdbcUrl(db);
    }
}


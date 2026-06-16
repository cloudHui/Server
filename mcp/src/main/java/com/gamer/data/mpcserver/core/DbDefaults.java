package com.gamer.data.mpcserver.core;

/**
 * 默认数据库连接信息（从 mcp.json env 或启动参数解析）。
 */
public class DbDefaults {
    private final String host;
    private final Integer port;
    private final String user;
    private final String password;
    private final String database;

    public DbDefaults(String host, Integer port, String user, String password, String database) {
        this.host = (host == null || host.trim().isEmpty()) ? "127.0.0.1" : host.trim();
        this.port = port == null ? Integer.valueOf(3306) : port;
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.database = (database == null || database.trim().isEmpty()) ? null : database.trim();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port == null ? 3306 : port;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    public String database() {
        return database;
    }

    public String buildJdbcUrl(String db) {
        String d = (db == null || db.trim().isEmpty()) ? database : db.trim();
        if (d == null || d.trim().isEmpty()) {
            d = "kingdom_game";
        }
        // 保持简单可靠：不引入额外依赖，仅拼接常用参数
        return "jdbc:mysql://" + host + ":" + port() + "/" + d
            + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    }
}


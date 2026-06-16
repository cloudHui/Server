package com.gamer.data.mpcserver.core;

/**
 * 默认 Redis 连接信息（从 mcp.json env 或启动参数解析）。
 */
public class RedisDefaults {
    private final String host;
    private final Integer port;
    private final String user;
    private final String password;

    public RedisDefaults(String host, Integer port, String user, String password) {
        this.host = (host == null || host.trim().isEmpty()) ? "127.0.0.1" : host.trim();
        this.port = port == null ? Integer.valueOf(6379) : port;
        this.user = user == null ? "" : user.trim();
        this.password = password == null ? "" : password;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port == null ? 6379 : port;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }
}


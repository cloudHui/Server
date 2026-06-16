package com.gamer.data.mpcserver.commands.redis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个极简 Redis RESP 客户端（仅用于调试工具：GET/SET/DEL/SCAN）。
 *
 * <p>避免引入 Jedis/lettuce 依赖：servertool/lib 当前不包含 redis 客户端 jar。</p>
 *
 * <p>支持两种使用模式：</p>
 * <ul>
 *   <li>一次性模式：{@link #execute(List)}，内部新建/销毁连接（保留原有行为，用于单次操作）</li>
 *   <li>会话模式：{@link #openSession()}，返回 Session 对象可复用同一个连接执行多条命令；
 *       Session 实现 AutoCloseable，但为兼容热更新约束不使用 try-with-resources，
 *       需在 finally 中调用 {@link Session#close()}。</li>
 * </ul>
 */
public class RedisRespClient {
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final String user;
    private final String password;

    public RedisRespClient(String host, int port) {
        this(host, port, "", "", DEFAULT_TIMEOUT_MS);
    }

    public RedisRespClient(String host, int port, String user, String password) {
        this(host, port, user, password, DEFAULT_TIMEOUT_MS);
    }

    public RedisRespClient(String host, int port, String user, String password, int timeoutMs) {
        this.host = host == null ? "127.0.0.1" : host.trim();
        this.port = port <= 0 ? 6379 : port;
        this.user = user == null ? "" : user.trim();
        this.password = password == null ? "" : password;
        this.timeoutMs = timeoutMs <= 0 ? DEFAULT_TIMEOUT_MS : timeoutMs;
    }

    /**
     * 一次性执行：内部新建连接、执行命令、关闭连接。
     * 适用于单次 GET/SET/DEL 操作。
     */
    public Object execute(List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("redis args 不能为空");
        }

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            doAuthIfNeeded(in, out);

            byte[] req = buildCommand(args);
            out.write(req);
            out.flush();

            return readResponse(in);
        } finally {
            tryCloseSocket(socket);
        }
    }

    /**
     * 打开一个可复用的连接会话。调用方负责在 finally 中调用 {@link Session#close()}。
     *
     * <p>典型用法：</p>
     * <pre>
     * RedisRespClient.Session session = client.openSession();
     * try {
     *     Object r1 = session.execute(args1);
     *     Object r2 = session.execute(args2);
     * } finally {
     *     session.close();
     * }
     * </pre>
     */
    public Session openSession() throws Exception {
        Socket socket = new Socket();
        boolean success = false;
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            doAuthIfNeeded(in, out);
            success = true;
            return new Session(socket, in, out);
        } finally {
            if (!success) {
                tryCloseSocket(socket);
            }
        }
    }

    /**
     * 可复用的连接会话，持有一个打开的 TCP 连接，AUTH 只做一次。
     */
    public static final class Session {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private boolean closed;

        private Session(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        /**
         * 在本会话持有的连接上执行一条命令。
         */
        public Object execute(List<String> args) throws Exception {
            if (closed) {
                throw new IllegalStateException("Session已关闭");
            }
            if (args == null || args.isEmpty()) {
                throw new IllegalArgumentException("redis args 不能为空");
            }
            byte[] req = buildCommand(args);
            out.write(req);
            out.flush();
            return readResponse(in);
        }

        /**
         * 关闭本会话持有的连接。多次调用安全。
         */
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            tryCloseSocket(socket);
        }
    }

    private void doAuthIfNeeded(InputStream in, OutputStream out) throws Exception {
        if (password == null || password.isEmpty()) {
            return;
        }
        List<String> authArgs = new ArrayList<>();
        authArgs.add("AUTH");
        if (user != null && !user.trim().isEmpty()) {
            authArgs.add(user.trim());
        }
        authArgs.add(password);

        byte[] req = buildCommand(authArgs);
        out.write(req);
        out.flush();
        Object resp = readResponse(in);
        if (resp == null) {
            throw new IOException("redis auth failed: empty response");
        }
        String text = String.valueOf(resp);
        if (!"OK".equalsIgnoreCase(text.trim())) {
            throw new IOException("redis auth failed: " + text);
        }
    }

    private static byte[] buildCommand(List<String> args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            String head = "*" + args.size() + "\r\n";
            bos.write(head.getBytes(StandardCharsets.UTF_8));
            for (String a : args) {
                if (a == null) {
                    a = "";
                }
                byte[] data = a.getBytes(StandardCharsets.UTF_8);
                String bulkHead = "$" + data.length + "\r\n";
                bos.write(bulkHead.getBytes(StandardCharsets.UTF_8));
                bos.write(data);
                bos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
        return bos.toByteArray();
    }

    private static Object readResponse(InputStream in) throws Exception {
        int type = in.read();
        if (type == -1) {
            throw new IOException("redis connection closed");
        }

        char t = (char) type;
        if (t == '+') {
            return readLine(in);
        }
        if (t == '-') {
            String line = readLine(in);
            throw new IOException("redis error: " + line);
        }
        if (t == ':') {
            String line = readLine(in);
            return Long.valueOf(line.trim());
        }
        if (t == '$') {
            int len = Integer.parseInt(readLine(in).trim());
            if (len < 0) {
                return null;
            }
            byte[] data = readBytes(in, len);
            readBytes(in, 2);
            return new String(data, StandardCharsets.UTF_8);
        }
        if (t == '*') {
            int len = Integer.parseInt(readLine(in).trim());
            if (len < 0) {
                return null;
            }
            List<Object> arr = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                arr.add(readResponse(in));
            }
            return arr;
        }

        throw new IOException("Unknown RESP type: " + t);
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("unexpected EOF");
            }
            if (b == '\r') {
                int n = in.read();
                if (n != '\n') {
                    throw new IOException("invalid CRLF");
                }
                break;
            }
            bos.write(b);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream in, int len) throws Exception {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(buf, off, len - off);
            if (r == -1) {
                throw new IOException("unexpected EOF");
            }
            off += r;
        }
        return buf;
    }

    private static void tryCloseSocket(Socket s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch (Exception ignored) {
        }
    }
}

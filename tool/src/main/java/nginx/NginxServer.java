import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;

public class NginxServer {

    // 保存当前运行的 JAR 包名称
    private static String CURRENT_JAR_NAME = "";

    public static void main(String[] args) throws Exception {
        // 【新增 1】：动态获取当前正在运行的 JAR 包名称
        try {
            String path = NginxServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            CURRENT_JAR_NAME = new File(path).getName();
        } catch (Exception e) {
            CURRENT_JAR_NAME = "NginxServer.jar"; // 兜底默认值
        }

        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("警告: 端口格式错误，将使用默认端口 8080");
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", NginxServer::handleRequest);
        server.start();

        System.out.println("内网文件服务已启动！");
        System.out.println("当前监听端口: " + port);
        System.out.println("已自动隐藏自身程序文件: " + CURRENT_JAR_NAME);
        System.out.println("浏览器访问: http://127.0.0.1:" + port);
    }

    private static void handleRequest(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();

            if (path.contains("..")) {
                sendError(exchange, 403, "403 Forbidden");
                return;
            }

            File file = new File("." + path);
            if (!file.exists()) {
                sendError(exchange, 404, "404 Not Found");
                return;
            }

            // 【新增 2】：防止别人直接通过输入 URL 下载这个 JAR 包
            if (file.isFile() && file.getName().equalsIgnoreCase(CURRENT_JAR_NAME)) {
                sendError(exchange, 403, "403 Forbidden - Access Denied");
                return;
            }

            if (file.isDirectory()) {
                handleDirectory(exchange, file, path);
            } else {
                handleFile(exchange, file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleDirectory(HttpExchange exchange, File dir, String path) throws Exception {
        if (!path.endsWith("/")) {
            exchange.getResponseHeaders().set("Location", path + "/");
            exchange.sendResponseHeaders(301, -1);
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='utf-8'><title>Index of ").append(path).append("</title></head>");
        html.append("<body><h1>Index of ").append(path).append("</h1><hr><pre>\n");
        if (!"/".equals(path)) html.append("<a href=\"../\">../</a>\n");

        // 【修改】：获取文件列表时，直接过滤掉当前的 JAR 包
        File[] files = dir.listFiles((d, name) -> !name.equalsIgnoreCase(CURRENT_JAR_NAME));

        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
            });

            for (File f : files) {
                String name = f.getName();
                String slash = f.isDirectory() ? "/" : "";
                html.append("<a href=\"").append(name).append(slash).append("\">").append(name).append(slash).append("</a>\n");
            }
        }

        html.append("</pre><hr></body></html>");
        sendResponse(exchange, 200, "text/html; charset=utf-8", html.toString().getBytes("UTF-8"));
    }

    private static void handleFile(HttpExchange exchange, File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String contentType = getContentType(file.getName());
        sendResponse(exchange, 200, contentType, bytes);
    }

    private static void sendResponse(HttpExchange exchange, int code, String contentType, byte[] data) throws Exception {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static void sendError(HttpExchange exchange, int code, String msg) throws Exception {
        sendResponse(exchange, code, "text/plain; charset=utf-8", msg.getBytes("UTF-8"));
    }

    private static String getContentType(String fileName) {
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".svg")) return "image/svg+xml";
        if (ext.endsWith(".html") || ext.endsWith(".htm")) return "text/html; charset=utf-8";
        if (ext.endsWith(".txt") || ext.endsWith(".java") || ext.endsWith(".xml")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }
}
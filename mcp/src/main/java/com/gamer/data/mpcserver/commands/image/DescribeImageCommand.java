package com.gamer.data.mpcserver.commands.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 图片识别命令：读取图片文件 → 调 MiMo v2.5 视觉模型 → 返回文字描述。
 *
 * <p>支持两种输入：</p>
 * <ul>
 *   <li>image_path：图片文件绝对路径</li>
 *   <li>image_data + media_type：base64 编码的图片数据</li>
 * </ul>
 *
 * @author liuyunhui
 * @date 2026-06-12
 */
@Process("describe_image")
public class DescribeImageCommand implements CommandHandler {

    private static final String DEFAULT_PROMPT = "请详细描述这张图片的内容，包括文字、布局、颜色等所有可见信息。";
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp"};
    private static final Map<String, String> EXT_TO_MIME = new HashMap<>();

    static {
        EXT_TO_MIME.put(".png", "image/png");
        EXT_TO_MIME.put(".jpg", "image/jpeg");
        EXT_TO_MIME.put(".jpeg", "image/jpeg");
        EXT_TO_MIME.put(".gif", "image/gif");
        EXT_TO_MIME.put(".webp", "image/webp");
        EXT_TO_MIME.put(".bmp", "image/bmp");
    }

    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        String imagePath = McpUtils.text(params, "image_path");
        String imageData = McpUtils.text(params, "image_data");
        String mediaType = McpUtils.text(params, "media_type");
        String prompt = McpUtils.text(params, "prompt");

        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = DEFAULT_PROMPT;
        }

        String b64;
        String mime;

        if (imageData != null && !imageData.trim().isEmpty()) {
            b64 = imageData;
            mime = (mediaType != null && !mediaType.trim().isEmpty()) ? mediaType : "image/png";
        } else if (imagePath != null && !imagePath.trim().isEmpty()) {
            File file = new File(imagePath.trim());
            if (!file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("文件不存在: " + imagePath);
            }
            byte[] bytes = readFile(file);
            b64 = Base64.getEncoder().encodeToString(bytes);
            mime = detectMimeType(file.getName(), bytes);
        } else {
            throw new IllegalArgumentException("缺少必要参数: image_path 或 image_data");
        }

        String apiKey = ImageConfig.getMimoApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置 MiMo API Key，请在启动参数中传入 --mimoApiKey=xxx");
        }

        String description = callMimoApi(apiKey, b64, mime, prompt);
        if (description == null) {
            throw new IllegalStateException("图片识别失败，API 未返回有效结果");
        }
        return CommandResult.of(description);
    }

    private static byte[] readFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            return buf;
        }
    }

    private static String detectMimeType(String fileName, byte[] data) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            ext = fileName.substring(dot).toLowerCase();
        }
        String mime = EXT_TO_MIME.get(ext);
        if (mime != null) return mime;

        if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == (byte) 0x50) return "image/png";
        if (data.length >= 3 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) return "image/jpeg";
        if (data.length >= 4 && data[0] == (byte) 0x47 && data[1] == (byte) 0x49) return "image/gif";
        if (data.length >= 12 && data[0] == (byte) 0x52 && data[1] == (byte) 0x49
            && data[8] == (byte) 0x57 && data[9] == (byte) 0x45) return "image/webp";
        return "image/png";
    }

    private static String callMimoApi(String apiKey, String b64Data, String mimeType, String prompt) throws Exception {
        String baseUrl = ImageConfig.getMimoBaseUrl();
        String model = ImageConfig.getMimoModel();
        String urlStr = baseUrl + "/v1/messages";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image");
        Map<String, Object> source = new HashMap<>();
        source.put("type", "base64");
        source.put("media_type", mimeType);
        source.put("data", b64Data);
        imageContent.put("source", source);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", new Object[]{imageContent, textContent});
        body.put("messages", new Object[]{userMsg});

        ObjectMapper mapper = new ObjectMapper();
        byte[] bodyBytes = mapper.writeValueAsBytes(body);

        SSLContext sc = createTrustAllSSL();
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
        }
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        byte[] respBytes = readAll(is);

        if (code >= 400) {
            throw new IOException("MiMo API 错误 " + code + ": " + new String(respBytes, StandardCharsets.UTF_8));
        }

        JsonNode root = mapper.readTree(respBytes);
        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }
        }
        return null;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        while ((n = is.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    private static SSLContext createTrustAllSSL() throws Exception {
        TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, tm, new java.security.SecureRandom());
        return sc;
    }
}

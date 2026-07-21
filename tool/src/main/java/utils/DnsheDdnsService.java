package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DnsheDdnsService {

    // ================== 配置区域 ==================
    private static final String API_KEY = "你的_X-API-Key";
    private static final String API_SECRET = "你的_X-API-Secret";

    private static final String SUB_DOMAIN = "home";       
    private static final String ROOT_DOMAIN = "bbroot.com"; 

    // 检查公网IP的间隔时间（5分钟）
    private static final long CHECK_INTERVAL = 5 * 60 * 1000;
    // =============================================

    //https://my.dnshe.com/index.php?m=domain_hub&view=domains 谷歌账号获取
    private static final String API_URL = "https://api005.dnshe.com/index.php?m=domain_hub&endpoint=subdomains&action=update";
    
    // 公共 IP 查询接口，按稳定性排序
    private static final String[] LOOKUP_SERVICES = {
        "https://api.icanhazip.com",
        "https://ifconfig.me/ip",
        "https://ident.me"
    };

    private static String lastIp = "";

    public static void main(String[] args) {
        System.out.println("[DDNS] DNSHE 动态域名解析守护进程已启动...");
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 1. 获取当前外网 IP
                String currentIp = getPublicIp();
                
                if (currentIp != null) {
                    // 2. 检查 IP 是否有变动
                    if (!currentIp.equals(lastIp)) {
                        System.out.printf("[DDNS] 检测到公网 IP 发生变化: %s -> %s%n", lastIp, currentIp);
                        
                        // 3. 调用 DNSHE 接口更新域名解析
                        if (updateDnsheRecord(currentIp)) {
                            lastIp = currentIp; // 更新成功，记录当前 IP
                            System.out.printf("[DDNS] 域名解析同步成功，当前绑定 IP: %s%n", currentIp);
                        } else {
                            System.err.println("[DDNS] 域名解析同步失败，将在下个周期重试。");
                        }
                    } else {
                        System.out.printf("[DDNS] IP 未发生改变，依然是: %s%n", currentIp);
                    }
                }
            } catch (Exception e) {
                System.err.println("[DDNS] 运行中捕获到未知异常: " + e.getMessage());
            }

            // 4. 定时休眠
            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                System.err.println("[DDNS] 守护进程收到中断信号，准备退出...");
                Thread.currentThread().interrupt(); // 保持中断状态
                break;
            }
        }
    }

    /**
     * 获取当前局域网的外网公网IP（带高可用容错）
     */
    private static String getPublicIp() {
        for (String service : LOOKUP_SERVICES) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(service);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                // 显式指定 UTF_8 编码防止老旧系统环境乱码
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String ip = rd.readLine();
                    if (ip != null && !ip.trim().isEmpty()) {
                        return ip.trim();
                    }
                }
            } catch (Exception e) {
                System.err.printf("[IP检查] 从接口 %s 获取 IP 失败，尝试下一个...%n", service);
            } finally {
                if (conn != null) {
                    conn.disconnect(); // 显式释放连接资源
                }
            }
        }
        System.err.println("[IP检查] 错误：所有公共 IP 接口均无法连接，请检查本地网络状况！");
        return null;
    }

    /**
     * 调用 DNSHE V2.0 接口更新解析记录
     */
    private static boolean updateDnsheRecord(String newIp) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST"); 
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // 设置统一鉴权 Header
            conn.setRequestProperty("X-API-Key", API_KEY);
            conn.setRequestProperty("X-API-Secret", API_SECRET);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            // 构建 JSON 请求体
            String jsonBody = String.format(
                "{\"domain\":\"%s\",\"subdomain\":\"%s\",\"type\":\"A\",\"value\":\"%s\"}",
                ROOT_DOMAIN, SUB_DOMAIN, newIp
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush(); // 显式刷新缓冲区
            }

            int responseCode = conn.getResponseCode();
            
            // 采用更优雅的方式兼容读取正确流或错误流
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(), 
                    StandardCharsets.UTF_8))) {
                
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                String responseStr = response.toString();
                System.out.printf("[DNSHE API 响应] 状态码: %d, 返回内容: %s%n", responseCode, responseStr);
                
                // 健壮性判断：状态码必须是 200 且返回 JSON 包含 success
                return responseCode == HttpURLConnection.HTTP_OK && responseStr.contains("success");
            }

        } catch (Exception e) {
            System.err.println("[DNSHE API 错误] 请求发送失败: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect(); // 无论成功失败，确保连接关闭
            }
        }
    }
}
package web.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 调用 lobby admin HTTP
 */
@Component
public class LobbyAdminClient {
	private static final Logger logger = LoggerFactory.getLogger(LobbyAdminClient.class);
	private final ObjectMapper mapper = new ObjectMapper();

	@Value("${lobby.admin-http:http://127.0.0.1:5701}")
	private String adminBase;

	@SuppressWarnings("unchecked")
	public Map<String, Object> getRegistration() {
		return get("/registration", null);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> listInvites(String token) {
		return get("/invites", token);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> createInvite(String token, Map<String, Object> body) {
		return post("/invites", token, body);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> revokeInvite(String token, Map<String, Object> body) {
		return post("/invites/revoke", token, body);
	}

	private Map<String, Object> get(String path, String token) {
		try {
			HttpURLConnection conn = open(path, "GET", token);
			return read(conn);
		} catch (Exception e) {
			logger.warn("lobby admin GET {} 失败: {}", path, e.getMessage());
			return null;
		}
	}

	private Map<String, Object> post(String path, String token, Map<String, Object> body) {
		try {
			HttpURLConnection conn = open(path, "POST", token);
			byte[] bytes = mapper.writeValueAsBytes(body == null ? new HashMap<>() : body);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			try (OutputStream os = conn.getOutputStream()) {
				os.write(bytes);
			}
			return read(conn);
		} catch (Exception e) {
			logger.warn("lobby admin POST {} 失败: {}", path, e.getMessage());
			return null;
		}
	}

	private HttpURLConnection open(String path, String method, String token) throws Exception {
		URL url = new URL(adminBase + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setConnectTimeout(3000);
		conn.setReadTimeout(5000);
		if (token != null && !token.isEmpty()) {
			conn.setRequestProperty("Authorization", "Bearer " + token);
		}
		return conn;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> read(HttpURLConnection conn) throws Exception {
		int code = conn.getResponseCode();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
				StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		if (sb.length() == 0) {
			Map<String, Object> err = new HashMap<>();
			err.put("code", code);
			err.put("msg", "empty response");
			return err;
		}
		return mapper.readValue(sb.toString(), Map.class);
	}
}

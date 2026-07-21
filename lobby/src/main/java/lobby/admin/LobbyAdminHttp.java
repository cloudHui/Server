package lobby.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lobby.Lobby;
import lobby.db.InviteEntity;
import lobby.db.InviteRepository;
import lobby.db.UserEntity;
import lobby.db.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邀请管理轻量 HTTP（仅 admin token 可访问；供 web 反代）
 */
public class LobbyAdminHttp {
	private static final Logger logger = LoggerFactory.getLogger(LobbyAdminHttp.class);

	private HttpServer server;

	public void start(int port) throws IOException {
		if (port <= 0) {
			logger.info("Lobby admin HTTP 未启用 (port={})", port);
			return;
		}
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
		server.createContext("/invites", this::handleInvites);
		server.createContext("/registration", this::handleRegistration);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		logger.info("Lobby admin HTTP 已启动: 127.0.0.1:{}", port);
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	private void handleRegistration(HttpExchange ex) throws IOException {
		if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
			writeJson(ex, 405, jsonError(405, "method not allowed"));
			return;
		}
		boolean open = Lobby.getInstance().isOpenRegister();
		writeJson(ex, 200, "{\"code\":0,\"openRegister\":" + open
				+ ",\"inviteRequired\":" + (!open) + "}");
	}

	private void handleInvites(HttpExchange ex) throws IOException {
		Optional<UserEntity> admin = authenticateAdmin(ex);
		if (!admin.isPresent()) {
			writeJson(ex, 401, jsonError(401, "需要 admin 登录"));
			return;
		}

		String method = ex.getRequestMethod();
		if ("GET".equalsIgnoreCase(method)) {
			listInvites(ex);
			return;
		}
		if ("POST".equalsIgnoreCase(method)) {
			String path = ex.getRequestURI().getPath();
			if (path.endsWith("/revoke")) {
				revokeInvite(ex);
			} else {
				createInvite(ex, admin.get());
			}
			return;
		}
		writeJson(ex, 405, jsonError(405, "method not allowed"));
	}

	private void listInvites(HttpExchange ex) throws IOException {
		InviteRepository repo = Lobby.getInstance().getInviteRepository();
		List<InviteEntity> list = repo.listAll();
		StringBuilder sb = new StringBuilder();
		sb.append("{\"code\":0,\"invites\":[");
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(toJson(list.get(i)));
		}
		sb.append("]}");
		writeJson(ex, 200, sb.toString());
	}

	private void createInvite(HttpExchange ex, UserEntity admin) throws IOException {
		Map<String, String> body = parseJsonObject(readBody(ex));
		String note = body.getOrDefault("note", "");
		int maxUses = parseInt(body.get("maxUses"), 1);
		int expiresDays = parseInt(body.get("expiresDays"), 7);
		Long expiresAt = null;
		if (expiresDays > 0) {
			expiresAt = System.currentTimeMillis() + expiresDays * 24L * 60 * 60 * 1000;
		}
		InviteEntity entity = Lobby.getInstance().getInviteRepository()
				.create(note, admin.getUsername(), expiresAt, maxUses);
		if (entity == null) {
			writeJson(ex, 500, jsonError(500, "创建失败"));
			return;
		}
		writeJson(ex, 200, "{\"code\":0,\"invite\":" + toJson(entity) + "}");
	}

	private void revokeInvite(HttpExchange ex) throws IOException {
		Map<String, String> body = parseJsonObject(readBody(ex));
		String token = body.get("token");
		if (token == null || token.isEmpty()) {
			writeJson(ex, 400, jsonError(400, "缺少 token"));
			return;
		}
		boolean ok = Lobby.getInstance().getInviteRepository().revoke(token);
		writeJson(ex, 200, ok ? "{\"code\":0,\"msg\":\"ok\"}" : jsonError(404, "邀请码不存在"));
	}

	private Optional<UserEntity> authenticateAdmin(HttpExchange ex) {
		Headers headers = ex.getRequestHeaders();
		String auth = headers.getFirst("Authorization");
		if (auth == null || !auth.startsWith("Bearer ")) {
			return Optional.empty();
		}
		String token = auth.substring(7).trim();
		if (token.isEmpty()) {
			return Optional.empty();
		}
		UserRepository users = Lobby.getInstance().getUserRepository();
		Optional<UserEntity> user = users.findByToken(token);
		if (!user.isPresent() || !user.get().isEnabled()) {
			return Optional.empty();
		}
		if (!"admin".equals(user.get().getUsername())) {
			return Optional.empty();
		}
		return user;
	}

	private static String toJson(InviteEntity e) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append("\"id\":").append(e.getId()).append(',');
		sb.append("\"token\":\"").append(escape(e.getToken())).append("\",");
		sb.append("\"note\":\"").append(escape(nullToEmpty(e.getNote()))).append("\",");
		sb.append("\"createdBy\":\"").append(escape(nullToEmpty(e.getCreatedBy()))).append("\",");
		sb.append("\"createdAt\":").append(e.getCreatedAt()).append(',');
		sb.append("\"expiresAt\":").append(e.getExpiresAt() == null ? "null" : e.getExpiresAt()).append(',');
		sb.append("\"maxUses\":").append(e.getMaxUses()).append(',');
		sb.append("\"usedCount\":").append(e.getUsedCount()).append(',');
		sb.append("\"enabled\":").append(e.isEnabled()).append(',');
		sb.append("\"valid\":").append(e.isValidNow());
		sb.append('}');
		return sb.toString();
	}

	private static String jsonError(int code, String msg) {
		return "{\"code\":" + code + ",\"msg\":\"" + escape(msg) + "\"}";
	}

	private static void writeJson(HttpExchange ex, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		ex.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String readBody(HttpExchange ex) throws IOException {
		try (InputStream in = ex.getRequestBody()) {
			byte[] buf = new byte[8192];
			StringBuilder sb = new StringBuilder();
			int n;
			while ((n = in.read(buf)) >= 0) {
				sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
			}
			return sb.toString();
		}
	}

	/** 极简 JSON object 解析（仅 string/number 字段） */
	private static Map<String, String> parseJsonObject(String raw) {
		Map<String, String> map = new LinkedHashMap<>();
		if (raw == null || raw.isEmpty()) {
			return map;
		}
		String s = raw.trim();
		if (s.startsWith("{")) {
			s = s.substring(1);
		}
		if (s.endsWith("}")) {
			s = s.substring(0, s.length() - 1);
		}
		List<String> parts = splitTopLevel(s);
		for (String part : parts) {
			int idx = part.indexOf(':');
			if (idx <= 0) {
				continue;
			}
			String key = unquote(part.substring(0, idx).trim());
			String val = unquote(part.substring(idx + 1).trim());
			map.put(key, val);
		}
		return map;
	}

	private static List<String> splitTopLevel(String s) {
		List<String> parts = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean inStr = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
				inStr = !inStr;
			}
			if (c == ',' && !inStr) {
				parts.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(c);
			}
		}
		if (cur.length() > 0) {
			parts.add(cur.toString());
		}
		return parts;
	}

	private static String unquote(String v) {
		if (v == null) {
			return "";
		}
		v = v.trim();
		if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
			return v.substring(1, v.length() - 1);
		}
		return v;
	}

	private static int parseInt(String v, int def) {
		if (v == null || v.isEmpty() || "null".equals(v)) {
			return def;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}

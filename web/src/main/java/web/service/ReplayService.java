package web.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 读取 game 落盘的回放文本（replay/日期/桌号_局.txt）
 */
@Service
public class ReplayService {
	private static final Logger logger = LoggerFactory.getLogger(ReplayService.class);

	@Value("${game.replay-dir:../build/game/replay}")
	private String replayDir;

	public List<Map<String, Object>> listReplays(int limit) {
		Path root = resolveRoot();
		List<Map<String, Object>> items = new ArrayList<>();
		if (!Files.isDirectory(root)) {
			return items;
		}
		int lim = limit <= 0 ? 100 : Math.min(limit, 300);
		try (Stream<Path> days = Files.list(root)) {
			List<Path> dayDirs = days.filter(Files::isDirectory)
					.sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
			for (Path day : dayDirs) {
				try (Stream<Path> files = Files.list(day)) {
					List<Path> txts = files.filter(p -> p.getFileName().toString().endsWith(".txt"))
							.sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
							.collect(Collectors.toList());
					for (Path f : txts) {
						Map<String, Object> m = summarize(day.getFileName().toString(), f);
						items.add(m);
						if (items.size() >= lim) {
							return items;
						}
					}
				}
			}
		} catch (IOException e) {
			logger.warn("列出回放失败: {}", e.getMessage());
		}
		return items;
	}

	public Map<String, Object> getReplay(String date, String name) {
		Map<String, Object> result = new HashMap<>();
		if (date == null || name == null || date.contains("..") || name.contains("..")
				|| date.contains("/") || name.contains("/")) {
			result.put("code", 400);
			result.put("msg", "非法路径");
			return result;
		}
		if (!name.endsWith(".txt")) {
			name = name + ".txt";
		}
		Path root = resolveRoot().toAbsolutePath().normalize();
		Path file = root.resolve(date).resolve(name).normalize();
		if (!file.startsWith(root)) {
			result.put("code", 400);
			result.put("msg", "非法路径");
			return result;
		}
		if (!Files.isRegularFile(file)) {
			result.put("code", 404);
			result.put("msg", "回放不存在");
			return result;
		}
		try {
			String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			result.put("code", 0);
			result.put("date", date);
			result.put("name", name);
			result.put("content", content);
			result.putAll(summarize(date, file));
			return result;
		} catch (IOException e) {
			result.put("code", 500);
			result.put("msg", "读取失败");
			return result;
		}
	}

	private Map<String, Object> summarize(String date, Path file) {
		Map<String, Object> m = new HashMap<>();
		String name = file.getFileName().toString();
		m.put("id", date + "/" + name);
		m.put("date", date);
		m.put("name", name);
		m.put("mtime", file.toFile().lastModified());
		m.put("size", file.toFile().length());
		String tableId = "";
		String round = "";
		String gameType = "";
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			for (String line : lines) {
				if (line.startsWith("桌号:")) tableId = line.substring(3).trim();
				else if (line.startsWith("玩法:")) gameType = line.substring(3).trim();
				else if (line.startsWith("当前局:")) round = line.substring(4).trim();
				if (!tableId.isEmpty() && !gameType.isEmpty() && !round.isEmpty()) break;
			}
		} catch (IOException ignored) {
		}
		m.put("tableId", tableId);
		m.put("round", round);
		m.put("gameType", gameType);
		return m;
	}

	private Path resolveRoot() {
		Path p = Paths.get(replayDir);
		if (!p.isAbsolute()) {
			p = Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
		}
		return p.toAbsolutePath().normalize();
	}
}

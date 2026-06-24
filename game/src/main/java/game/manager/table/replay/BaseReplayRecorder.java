package game.manager.table.replay;

import game.manager.table.mj.MjExposedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class BaseReplayRecorder implements ReplayRecorder {

	private static final Logger logger = LoggerFactory.getLogger(BaseReplayRecorder.class);

	/** 上次清理回放文件的日期，每天只清理一次 */
	private static volatile String lastCleanupDate = "";

	protected final long tableId;
	protected final int round;
	protected final StringBuilder sb = new StringBuilder();
	protected int actionIndex = 0;
	protected boolean finalized = false;

	protected BaseReplayRecorder(long tableId, int round) {
		this.tableId = tableId;
		this.round = round;
	}

	@Override
	public void writeHeader(String gameType, int totalRounds, int seatNum, Map<Integer, Integer> userIds, Map<Integer, String> nicknames) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sb.append("=== 回放文件 ===\n");
		sb.append("桌号: ").append(tableId).append("\n");
		sb.append("玩法: ").append(gameType).append("\n");
		sb.append("总局数: ").append(totalRounds).append("\n");
		sb.append("当前局: ").append(round).append("\n");
		sb.append("时间: ").append(sdf.format(new Date())).append("\n");
		sb.append("\n=== 玩家 ===\n");
		for (int i = 0; i < seatNum; i++) {
			sb.append("座").append(i).append(": userId=").append(userIds.getOrDefault(i, 0))
					.append(", nick=").append(nicknames.getOrDefault(i, "未知")).append("\n");
		}
	}

	@Override
	public void writeConfig(String configLine) {
		sb.append("\n=== 配置 ===\n").append(configLine).append("\n");
	}

	@Override
	public void writeDealerAndLaiZi(int dealerSeat, int laiZiTileId, int flipTileId) {
		// 默认空实现，MJ覆写
	}

	@Override
	public void writeInitHands(Map<Integer, List<Integer>> hands) {
		sb.append("\n=== 初始发牌 ===\n");
		for (Map.Entry<Integer, List<Integer>> entry : hands.entrySet()) {
			sb.append("座").append(entry.getKey()).append(": ").append(formatList(entry.getValue())).append("\n");
		}
	}

	@Override
	public void writeSettlement(int winnerSeat, int fan, String winType, int[] scores) {
		sb.append("\n=== 结算 ===\n");
		sb.append("番数: ").append(fan).append("\n");
		sb.append("胡牌方式: ").append(winType).append("\n");
		for (int i = 0; i < scores.length; i++) {
			sb.append("座").append(i).append(": ").append(scores[i] >= 0 ? "+" : "").append(scores[i]).append("\n");
		}
	}

	/** 保存回放文件到磁盘，每天清理一次7天前的旧文件 */
	@Override
	public void save() {
		if (finalized) return;
		finalized = true;
		try {
			String jarDir = getJarDir();
			SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
			String today = dateFmt.format(new Date());
			String dirPath = jarDir + File.separator + "replay" + File.separator + today;
			File dir = new File(dirPath);
			if (!dir.exists()) dir.mkdirs();
			File file = new File(dir, tableId + "_" + round + ".txt");
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
				writer.write(sb.toString());
			}
			logger.info("回放文件已保存: {}", file.getAbsolutePath());
			if (!today.equals(lastCleanupDate)) {
				lastCleanupDate = today;
				cleanOldReplays(jarDir);
			}
		} catch (Exception e) {
			logger.error("保存回放文件失败, tableId: {}, round: {}", tableId, round, e);
		}
	}

	/** 清理replay目录下超过7天的子目录 */
	private void cleanOldReplays(String jarDir) {
		File replayDir = new File(jarDir, "replay");
		if (!replayDir.exists() || !replayDir.isDirectory()) return;
		long threshold = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
		File[] subDirs = replayDir.listFiles(File::isDirectory);
		if (subDirs == null) return;
		for (File sub : subDirs) {
			if (sub.lastModified() < threshold) {
				deleteDir(sub);
				logger.info("清理过期回放目录: {}", sub.getName());
			}
		}
	}

	/** 递归删除目录 */
	private void deleteDir(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) deleteDir(f);
				else f.delete();
			}
		}
		dir.delete();
	}

	protected String getJarDir() {
		try {
			return new File(BaseReplayRecorder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
		} catch (Exception e) { return "."; }
	}

	protected String formatList(List<Integer> list) {
		if (list == null || list.isEmpty()) return "[]";
		StringBuilder b = new StringBuilder("[");
		for (int i = 0; i < list.size(); i++) { if (i > 0) b.append(","); b.append(list.get(i)); }
		return b.append("]").toString();
	}

	protected void appendAction(String action) {
		sb.append("[").append(++actionIndex).append("] ").append(action).append("\n");
	}
}

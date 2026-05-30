package game.manager.table.replay;

import game.manager.table.mj.MjExposedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 麻将回放记录器
 * 记录牌局全过程到txt文件
 */
public class MjReplayRecorder implements ReplayRecorder {

	private static final Logger logger = LoggerFactory.getLogger(MjReplayRecorder.class);

	private final long tableId;
	private final int round;
	private final StringBuilder sb = new StringBuilder();
	private int actionIndex = 0;
	private boolean finalized = false;

	public MjReplayRecorder(long tableId, int round) {
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
		sb.append("庄家: 座").append(dealerSeat).append("\n");
		if (laiZiTileId > 0) {
			sb.append("翻牌: ").append(flipTileId).append(" → 赖子: ").append(laiZiTileId).append("\n");
		}
	}

	@Override
	public void writeInitHands(Map<Integer, List<Integer>> hands) {
		sb.append("\n=== 初始发牌 ===\n");
		for (Map.Entry<Integer, List<Integer>> entry : hands.entrySet()) {
			sb.append("座").append(entry.getKey()).append(": ").append(formatList(entry.getValue())).append("\n");
		}
	}

	@Override public void recordDraw(int seat, int tileId) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 摸牌 ").append(tileId).append("\n"); }
	@Override public void recordDiscard(int seat, int tileId) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 出牌 ").append(tileId).append("\n"); }
	@Override public void recordAutoDiscard(int seat, int tileId) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 超时出牌 ").append(tileId).append("\n"); }
	@Override public void recordChi(int seat, int fromSeat, List<Integer> chiTiles, List<Integer> handTiles) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 吃 ").append(formatList(chiTiles)).append(" (座").append(fromSeat).append("出)\n"); }
	@Override public void recordPeng(int seat, int fromSeat, int tileId) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 碰 ").append(tileId).append(" (座").append(fromSeat).append("出)\n"); }
	@Override public void recordMingGang(int seat, int fromSeat, int tileId) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 明杠 ").append(tileId).append(" (座").append(fromSeat).append("出)\n"); }
	@Override public void recordAnGang(int seat, int tileId, int drawnTile) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 暗杠 ").append(tileId).append(" → 补牌 ").append(drawnTile).append("\n"); }
	@Override public void recordBuGang(int seat, int tileId, int drawnTile) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 补杠 ").append(tileId).append(" → 补牌 ").append(drawnTile).append("\n"); }
	@Override public void recordBuGangRobbed(int seat, int tileId, int robberSeat) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 补杠 ").append(tileId).append(" 被座").append(robberSeat).append(" 抢杠胡\n"); }
	@Override public void recordHu(int seat, int tileId, boolean ziMo) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 胡 ").append(tileId).append(ziMo ? " (自摸)" : " (点炮)").append("\n"); }
	@Override public void recordAutoPass(int seat) { sb.append("[").append(++actionIndex).append("] 座").append(seat).append(" 超时过\n"); }
	@Override public void recordDrawGame() { sb.append("[").append(++actionIndex).append("] 流局\n"); }

	@Override
	public void writeFinalState(Map<Integer, List<Integer>> finalHands, Map<Integer, List<MjExposedSet>> exposedSetsMap, int wallRemaining) {
		sb.append("\n=== 最终状态 ===\n");
		for (Map.Entry<Integer, List<Integer>> entry : finalHands.entrySet()) {
			int seat = entry.getKey();
			sb.append("座").append(seat).append(" 手牌: ").append(formatList(entry.getValue()));
			List<MjExposedSet> sets = exposedSetsMap.get(seat);
			if (sets != null && !sets.isEmpty()) {
				sb.append(" 副露:");
				for (MjExposedSet set : sets) {
					sb.append("[").append(set.getType().name().toLowerCase()).append(" ").append(formatList(set.getTileIds())).append("]");
				}
			}
			sb.append("\n");
		}
		sb.append("牌墙剩余: ").append(wallRemaining).append("张\n");
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

	@Override
	public void save() {
		if (finalized) return;
		finalized = true;
		try {
			String jarDir = getJarDir();
			SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
			String dirPath = jarDir + File.separator + "replay" + File.separator + dateFmt.format(new Date());
			File dir = new File(dirPath);
			if (!dir.exists()) dir.mkdirs();
			File file = new File(dir, tableId + "_" + round + ".txt");
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
				writer.write(sb.toString());
			}
			logger.info("回放文件已保存: {}", file.getAbsolutePath());
		} catch (Exception e) {
			logger.error("保存回放文件失败, tableId: {}, round: {}", tableId, round, e);
		}
	}

	private String getJarDir() {
		try {
			return new File(MjReplayRecorder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
		} catch (Exception e) { return "."; }
	}

	private String formatList(List<Integer> list) {
		if (list == null || list.isEmpty()) return "[]";
		StringBuilder b = new StringBuilder("[");
		for (int i = 0; i < list.size(); i++) { if (i > 0) b.append(","); b.append(list.get(i)); }
		return b.append("]").toString();
	}
}

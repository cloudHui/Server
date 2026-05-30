package game.manager.table.mj;

import java.util.*;

/**
 * 麻将单局结算结果
 */
public class MjRoundResult {

	/** 第几局 */
	private int round;

	/** 胡牌玩家座位(-1=流局) */
	private int winnerSeat;

	/** 番数 */
	private int fan;

	/** 每家得分(正=赢, 负=输) */
	private int[] scores;

	/** 胡牌方式 */
	private String winType; // "ziMo"/"dianPao"/"gangShangHua"/"qiangGangHu"/"liuJu"

	/** 每家副露统计: seat -> 副露列表 */
	private Map<Integer, List<ExposedStat>> exposedStats = new HashMap<>();

	/**
	 * 单条副露统计
	 */
	public static class ExposedStat {
		private final String type; // "peng"/"mingGang"/"anGang"/"buGang"/"chi"
		private final List<Integer> tileIds;

		public ExposedStat(String type, List<Integer> tileIds) {
			this.type = type;
			this.tileIds = Collections.unmodifiableList(new ArrayList<>(tileIds));
		}

		public String getType() { return type; }
		public List<Integer> getTileIds() { return tileIds; }
	}

	// --- Getters & Setters ---

	public int getRound() { return round; }
	public void setRound(int round) { this.round = round; }

	public int getWinnerSeat() { return winnerSeat; }
	public void setWinnerSeat(int winnerSeat) { this.winnerSeat = winnerSeat; }

	public int getFan() { return fan; }
	public void setFan(int fan) { this.fan = fan; }

	public int[] getScores() { return scores; }
	public void setScores(int[] scores) { this.scores = scores; }

	public String getWinType() { return winType; }
	public void setWinType(String winType) { this.winType = winType; }

	public Map<Integer, List<ExposedStat>> getExposedStats() { return exposedStats; }
	public void setExposedStats(Map<Integer, List<ExposedStat>> exposedStats) { this.exposedStats = exposedStats; }

	/**
	 * 从MjTableContext提取每家的副露统计
	 */
	public static Map<Integer, List<ExposedStat>> extractExposedStats(MjTableContext ctx, int seatNum) {
		Map<Integer, List<ExposedStat>> stats = new HashMap<>();
		for (int i = 0; i < seatNum; i++) {
			List<MjExposedSet> sets = ctx.getExposedSets(i);
			List<ExposedStat> list = new ArrayList<>();
			for (MjExposedSet set : sets) {
				String type;
				switch (set.getType()) {
					case PENG: type = "peng"; break;
					case MING_GANG: type = "mingGang"; break;
					case AN_GANG: type = "anGang"; break;
					case BU_GANG: type = "buGang"; break;
					case CHI: type = "chi"; break;
					default: type = "unknown"; break;
				}
				list.add(new ExposedStat(type, set.getTileIds()));
			}
			stats.put(i, list);
		}
		return stats;
	}
}

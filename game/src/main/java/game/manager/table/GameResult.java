package game.manager.table;

import java.util.*;

/**
 * 整场游戏结算基类(多局汇总)
 * MJ/DDZ各自继承扩展
 */
public class GameResult {

	/** 总局数 */
	protected int totalRounds;

	/** 已完成局数 */
	protected int completedRounds;

	/** 每局结果 */
	protected final List<RoundEntry> roundEntries = new ArrayList<>();

	/** 每家累计总分: seat -> totalScore */
	protected final Map<Integer, Integer> totalScores = new HashMap<>();

	/**
	 * 单局记录(通用)
	 */
	public static class RoundEntry {
		private final int round;
		private final int winnerSeat; // -1=流局
		private final int score; // 番数 or settleFactor
		private final int[] scores;
		private final String winType;

		public RoundEntry(int round, int winnerSeat, int score, int[] scores, String winType) {
			this.round = round;
			this.winnerSeat = winnerSeat;
			this.score = score;
			this.scores = scores;
			this.winType = winType;
		}

		public int getRound() { return round; }
		public int getWinnerSeat() { return winnerSeat; }
		public int getScore() { return score; }
		public int[] getScores() { return scores; }
		public String getWinType() { return winType; }
	}

	/**
	 * 添加一局结果
	 */
	public void addRound(int round, int winnerSeat, int score, int[] scores, String winType) {
		roundEntries.add(new RoundEntry(round, winnerSeat, score, scores, winType));
		completedRounds++;
		for (int i = 0; i < scores.length; i++) {
			totalScores.merge(i, scores[i], Integer::sum);
		}
	}

	/** 是否所有局已完成 */
	public boolean isComplete() { return completedRounds >= totalRounds; }

	/** 按总分降序返回排名 */
	public List<Map.Entry<Integer, Integer>> getRanking() {
		List<Map.Entry<Integer, Integer>> ranking = new ArrayList<>(totalScores.entrySet());
		ranking.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		return ranking;
	}

	// --- Getters & Setters ---

	public int getTotalRounds() { return totalRounds; }
	public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
	public int getCompletedRounds() { return completedRounds; }
	public List<RoundEntry> getRoundEntries() { return roundEntries; }
	public Map<Integer, Integer> getTotalScores() { return totalScores; }
	public int getTotalScore(int seat) { return totalScores.getOrDefault(seat, 0); }
}

package game.manager.table;

import java.util.*;

/**
 * 麻将整场结算
 */
public class MjGameResult extends GameResult {

	/** 每局的番数 */
	private final List<Integer> fanList = new ArrayList<>();

	/** 每局的胡牌方式 */
	private final List<String> winTypeList = new ArrayList<>();

	@Override
	public void addRound(int round, int winnerSeat, int score, int[] scores, String winType) {
		super.addRound(round, winnerSeat, score, scores, winType);
		fanList.add(score); // score = fan for MJ
		winTypeList.add(winType);
	}

	public List<Integer> getFanList() { return fanList; }
	public List<String> getWinTypeList() { return winTypeList; }
}

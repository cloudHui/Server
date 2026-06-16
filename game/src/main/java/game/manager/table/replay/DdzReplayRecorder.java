package game.manager.table.replay;

import java.util.List;

public class DdzReplayRecorder extends BaseReplayRecorder {

	public DdzReplayRecorder(long tableId, int round) {
		super(tableId, round);
	}

	public void recordBid(int seat, int score) {
		appendAction("座" + seat + " 叫分 " + score);
	}

	public void recordNotCall(int seat) {
		appendAction("座" + seat + " 不叫");
	}

	public void recordRob(int seat) {
		appendAction("座" + seat + " 抢地主");
	}

	public void recordNotRob(int seat) {
		appendAction("座" + seat + " 不抢");
	}

	public void recordBottomCards(int landlordSeat, List<Integer> bottomCards) {
		sb.append("\n=== 底牌 ===\n");
		sb.append("地主: 座").append(landlordSeat).append("\n");
		sb.append("底牌: ").append(formatList(bottomCards)).append("\n");
	}

	public void recordPlay(int seat, List<Integer> cardIds) {
		appendAction("座" + seat + " 出牌 " + formatList(cardIds));
	}

	public void recordPass(int seat) {
		appendAction("座" + seat + " 过");
	}

	public void recordAutoPlay(int seat, List<Integer> cardIds) {
		appendAction("座" + seat + " 超时出牌 " + formatList(cardIds));
	}

	public void recordAutoPass(int seat) {
		appendAction("座" + seat + " 超时过");
	}

	@Override
	public void writeSettlement(int winnerSeat, int fan, String winType, int[] scores) {
		sb.append("\n=== 结算 ===\n");
		sb.append("赢家: 座").append(winnerSeat).append("\n");
		sb.append("倍数: ").append(fan).append("\n");
		sb.append("类型: ").append(winType).append("\n");
		for (int i = 0; i < scores.length; i++) {
			sb.append("座").append(i).append(": ").append(scores[i] >= 0 ? "+" : "").append(scores[i]).append("\n");
		}
	}
}

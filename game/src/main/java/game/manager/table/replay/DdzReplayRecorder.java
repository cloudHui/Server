package game.manager.table.replay;

import java.util.List;

/**
 * 斗地主回放记录器
 * 记录叫分、抢地主、出牌、过牌等操作
 */
public class DdzReplayRecorder extends BaseReplayRecorder {

	public DdzReplayRecorder(long tableId, int round) {
		super(tableId, round);
	}

	/** 记录叫分 */
	public void recordBid(int seat, int score) { appendAction("座" + seat + " 叫分 " + score); }
	/** 记录不叫 */
	public void recordNotCall(int seat) { appendAction("座" + seat + " 不叫"); }
	/** 记录抢地主 */
	public void recordRob(int seat) { appendAction("座" + seat + " 抢地主"); }
	/** 记录不抢 */
	public void recordNotRob(int seat) { appendAction("座" + seat + " 不抢"); }
	/** 记录底牌 */
	public void recordBottomCards(int landlordSeat, List<Integer> bottomCards) {
		sb.append("\n=== 底牌 ===\n");
		sb.append("地主: 座").append(landlordSeat).append("\n");
		sb.append("底牌: ").append(formatList(bottomCards)).append("\n");
	}

	/** 记录出牌 */
	public void recordPlay(int seat, List<Integer> cardIds) { appendAction("座" + seat + " 出牌 " + formatList(cardIds)); }
	/** 记录过牌 */
	public void recordPass(int seat) { appendAction("座" + seat + " 过"); }
	/** 记录超时自动出牌 */
	public void recordAutoPlay(int seat, List<Integer> cardIds) { appendAction("座" + seat + " 超时出牌 " + formatList(cardIds)); }
	/** 记录超时自动过 */
	public void recordAutoPass(int seat) { appendAction("座" + seat + " 超时过"); }

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

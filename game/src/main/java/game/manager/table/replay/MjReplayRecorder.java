package game.manager.table.replay;

import game.manager.table.mj.MjExposedSet;

import java.util.List;
import java.util.Map;

/**
 * 麻将回放记录器
 * 记录摸牌、出牌、碰杠吃胡等操作，最终写入文本文件
 */
public class MjReplayRecorder extends BaseReplayRecorder {

	public MjReplayRecorder(long tableId, int round) {
		super(tableId, round);
	}

	@Override
	public void writeDealerAndLaiZi(int dealerSeat, int laiZiTileId, int flipTileId) {
		sb.append("庄家: 座").append(dealerSeat).append("\n");
		if (laiZiTileId > 0) {
			sb.append("翻牌: ").append(flipTileId).append(" → 赖子: ").append(laiZiTileId).append("\n");
		}
	}

	/** 记录摸牌 */
	public void recordDraw(int seat, int tileId) { appendAction("座" + seat + " 摸牌 " + tileId); }
	/** 记录出牌 */
	public void recordDiscard(int seat, int tileId) { appendAction("座" + seat + " 出牌 " + tileId); }
	/** 记录超时自动出牌 */
	public void recordAutoDiscard(int seat, int tileId) { appendAction("座" + seat + " 超时出牌 " + tileId); }
	/** 记录吃 */
	public void recordChi(int seat, int fromSeat, List<Integer> chiTiles, List<Integer> handTiles) { appendAction("座" + seat + " 吃 " + formatList(chiTiles) + " (座" + fromSeat + "出)"); }
	/** 记录碰 */
	public void recordPeng(int seat, int fromSeat, int tileId) { appendAction("座" + seat + " 碰 " + tileId + " (座" + fromSeat + "出)"); }
	/** 记录明杠 */
	public void recordMingGang(int seat, int fromSeat, int tileId) { appendAction("座" + seat + " 明杠 " + tileId + " (座" + fromSeat + "出)"); }
	/** 记录暗杠 */
	public void recordAnGang(int seat, int tileId, int drawnTile) { appendAction("座" + seat + " 暗杠 " + tileId + " → 补牌 " + drawnTile); }
	/** 记录补杠 */
	public void recordBuGang(int seat, int tileId, int drawnTile) { appendAction("座" + seat + " 补杠 " + tileId + " → 补牌 " + drawnTile); }
	/** 记录补杠被抢 */
	public void recordBuGangRobbed(int seat, int tileId, int robberSeat) { appendAction("座" + seat + " 补杠 " + tileId + " 被座" + robberSeat + " 抢杠胡"); }
	/** 记录胡牌 */
	public void recordHu(int seat, int tileId, boolean ziMo) { appendAction("座" + seat + " 胡 " + tileId + (ziMo ? " (自摸)" : " (点炮)")); }
	/** 记录超时pass */
	public void recordAutoPass(int seat) { appendAction("座" + seat + " 超时过"); }
	/** 记录流局 */
	public void recordDrawGame() { appendAction("流局"); }

	@Override
	public void writeSettlement(int winnerSeat, int fan, String winType, int[] scores) {
		sb.append("\n=== 结算 ===\n");
		sb.append("番数: ").append(fan).append("\n");
		sb.append("胡牌方式: ").append(winType).append("\n");
		for (int i = 0; i < scores.length; i++) {
			sb.append("座").append(i).append(": ").append(scores[i] >= 0 ? "+" : "").append(scores[i]).append("\n");
		}
	}

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
}

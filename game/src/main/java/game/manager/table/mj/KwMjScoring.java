package game.manager.table.mj;

import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;
import model.tablemodel.TableModel;

import java.util.List;

/**
 * 卡五星计番
 */
public class KwMjScoring implements MjScoring {

	private static final int FAN_PING_HU = 1;          // 平胡
	private static final int FAN_KA_WU_XING = 2;       // 卡五星(额外)
	private static final int FAN_PENG_PENG_HU = 2;      // 碰碰胡
	private static final int FAN_QING_YI_SE = 3;        // 清一色
	private static final int FAN_QI_DUI = 2;            // 七对
	private static final int FAN_LONG_QI_DUI = 4;       // 龙七对
	private static final int FAN_GANG_SHANG_HUA = 2;    // 杠上开花(额外)
	private static final int FAN_QIANG_GANG_HU = 2;     // 抢杠胡(额外)
	private static final int FAN_HAI_DI = 2;            // 海底捞月(额外)

	private final KwWinChecker kwWinChecker;

	public KwMjScoring(KwWinChecker kwWinChecker) {
		this.kwWinChecker = kwWinChecker;
	}

	@Override
	public int calcFan(MjWinResult winResult, MjTableContext ctx, TableModel model) {
		int fan = FAN_PING_HU;

		List<MjExposedSet> exposedSets = winResult.getExposedSets();
		List<Card> handTiles = winResult.getHandTiles();

		// 卡五星
		if (kwWinChecker.isKaWuXing(handTiles, winResult.getWinTile())) {
			fan += FAN_KA_WU_XING;
		}

		// 碰碰胡
		if (isPengPengHu(exposedSets, handTiles)) {
			fan = Math.max(fan, FAN_PENG_PENG_HU);
		}

		// 清一色
		if (isQingYiSe(handTiles, exposedSets)) {
			fan = Math.max(fan, FAN_QING_YI_SE);
		}

		// 七对/龙七对
		if (exposedSets.isEmpty() && handTiles.size() == 14) {
			if (isLongQiDui(handTiles)) {
				fan = Math.max(fan, FAN_LONG_QI_DUI);
			} else {
				fan = Math.max(fan, FAN_QI_DUI);
			}
		}

		// 额外番
		if (winResult.isGangShangKaiHua()) {
			fan += FAN_GANG_SHANG_HUA;
		}
		if (winResult.isQiangGangHu()) {
			fan += FAN_QIANG_GANG_HU;
		}
		if (winResult.isHaiDi()) {
			fan += FAN_HAI_DI;
		}

		int maxFan = model.getMaxFan();
		if (maxFan > 0 && fan > maxFan) {
			fan = maxFan;
		}

		return fan;
	}

	@Override
	public int[] settle(MjWinResult winResult, int fan, MjTableContext ctx, TableModel model, int seatNum) {
		int[] scores = new int[seatNum];
		int baseScore = model.getBaseScore();
		int winScore = baseScore * fan;

		if (winResult.isZiMo()) {
			// 自摸: 其他每家都输
			for (int i = 0; i < seatNum; i++) {
				if (i == winResult.getWinnerId()) {
					scores[i] = winScore * (seatNum - 1);
				} else {
					scores[i] = -winScore;
				}
			}
		}
		// 卡五星通常只允许自摸, 点炮不处理

		return scores;
	}

	private boolean isPengPengHu(List<MjExposedSet> exposedSets, List<Card> handTiles) {
		for (MjExposedSet set : exposedSets) {
			if (set.getType() == MjExposedSet.Type.CHI) {
				return false;
			}
		}
		return handTiles.size() == 2 && handTiles.get(0).getId() == handTiles.get(1).getId();
	}

	private boolean isQingYiSe(List<Card> handTiles, List<MjExposedSet> exposedSets) {
		int suit = -1;
		for (Card c : handTiles) {
			int s = MjConst.suitOf(c.getId());
			if (suit == -1) {
				suit = s;
			} else if (s != suit) {
				return false;
			}
		}
		for (MjExposedSet set : exposedSets) {
			for (int tileId : set.getTileIds()) {
				if (MjConst.suitOf(tileId) != suit) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isLongQiDui(List<Card> handTiles) {
		if (handTiles.size() != 14) {
			return false;
		}
		int[] sorted = handTiles.stream().mapToInt(Card::getId).sorted().toArray();
		for (int i = 0; i < 14; i += 2) {
			if (sorted[i] != sorted[i + 1]) {
				return false;
			}
		}
		for (int i = 0; i < 14; i += 2) {
			if (i + 3 < 14 && sorted[i] == sorted[i + 3]) {
				return true;
			}
		}
		return false;
	}
}

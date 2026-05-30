package game.manager.table.mj;

import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;

import java.util.List;

/**
 * 卡五星胡牌检测
 * 扩展: 只用两种花色、卡五胡(3-5-7卡中张5)
 */
public class KwWinChecker extends MjWinChecker {

	private final int[] allowedSuits;
	/** 是否必须卡五才能胡(true=必须卡五, false=卡五只是加番) */
	private final boolean mustKaWu;

	public KwWinChecker(int[] allowedSuits, boolean allowSevenPairs, boolean mustKaWu) {
		super(allowSevenPairs);
		this.allowedSuits = allowedSuits;
		this.mustKaWu = mustKaWu;
	}

	@Override
	public boolean canWin(List<Card> handTiles, List<MjExposedSet> exposedSets, int winTile) {
		if (!super.canWin(handTiles, exposedSets, winTile)) {
			return false;
		}
		if (!checkSuits(handTiles)) {
			return false;
		}
		// 必须卡五才能胡
		if (mustKaWu && !isKaWuXing(handTiles, winTile)) {
			return false;
		}
		return true;
	}

	/**
	 * 卡五检测: 手牌中有 3-5 或 5-7 (同花色), 且胡的牌是4或6
	 */
	public boolean isKaWuXing(List<Card> handTiles, int winTile) {
		int winSuit = MjConst.suitOf(winTile);
		int winValue = MjConst.valueOf(winTile);

		if (winSuit > MjConst.SUIT_TONG) {
			return false;
		}

		boolean has3 = false, has5 = false, has7 = false;
		for (Card c : handTiles) {
			if (MjConst.suitOf(c.getId()) == winSuit) {
				int v = MjConst.valueOf(c.getId());
				if (v == 3) has3 = true;
				if (v == 5) has5 = true;
				if (v == 7) has7 = true;
			}
		}

		if (winValue == 4 && has3 && has5) return true;
		if (winValue == 6 && has5 && has7) return true;

		return false;
	}

	private boolean checkSuits(List<Card> handTiles) {
		for (Card c : handTiles) {
			int suit = MjConst.suitOf(c.getId());
			boolean allowed = false;
			for (int s : allowedSuits) {
				if (s == suit) {
					allowed = true;
					break;
				}
			}
			if (!allowed) return false;
		}
		return true;
	}

	public int[] getAllowedSuits() {
		return allowedSuits;
	}
}

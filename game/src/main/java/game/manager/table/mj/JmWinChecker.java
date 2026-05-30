package game.manager.table.mj;

import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;

import java.util.List;

/**
 * 荆门麻将胡牌检测
 * 扩展: 赖子(万能牌)替代、开口笑(必须有副露才能胡)
 */
public class JmWinChecker extends MjWinChecker {

	private int laiZiTileId;

	public JmWinChecker(int laiZiTileId, boolean allowSevenPairs) {
		super(allowSevenPairs);
		this.laiZiTileId = laiZiTileId;
	}

	@Override
	public boolean canWin(List<Card> handTiles, List<MjExposedSet> exposedSets, int winTile) {
		if (laiZiTileId == 0) {
			return super.canWin(handTiles, exposedSets, winTile);
		}
		return canWinWithLaiZi(handTiles, exposedSets, winTile);
	}

	private boolean canWinWithLaiZi(List<Card> handTiles, List<MjExposedSet> exposedSets, int winTile) {
		int laiZiCount = 0;
		int[] normalTiles = new int[handTiles.size()];
		int normalCount = 0;

		for (Card c : handTiles) {
			if (c.getId() == laiZiTileId) {
				laiZiCount++;
			} else {
				normalTiles[normalCount++] = c.getId();
			}
		}

		if (laiZiCount == 0) {
			return super.canWin(handTiles, exposedSets, winTile);
		}

		return tryWinWithLaiZi(normalTiles, normalCount, laiZiCount, exposedSets);
	}

	private boolean tryWinWithLaiZi(int[] normalTiles, int normalCount, int laiZiCount, List<MjExposedSet> exposedSets) {
		int meldCount = exposedSets.size();

		// 七对(带赖子)
		if (allowSevenPairs && meldCount == 0 && normalCount + laiZiCount == 14) {
			if (checkSevenPairsWithLaiZi(normalTiles, normalCount, laiZiCount)) {
				return true;
			}
		}

		// 标准胡(带赖子)
		return checkStandardWinWithLaiZi(normalTiles, normalCount, laiZiCount);
	}

	private boolean checkSevenPairsWithLaiZi(int[] tiles, int count, int laiZiCount) {
		java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
		for (int i = 0; i < count; i++) {
			freq.merge(tiles[i], 1, Integer::sum);
		}
		int needLaiZi = 0;
		for (int c : freq.values()) {
			if (c % 2 != 0) needLaiZi++;
		}
		return needLaiZi <= laiZiCount;
	}

	private boolean checkStandardWinWithLaiZi(int[] normalTiles, int normalCount, int laiZiCount) {
		int[] allTiles = new int[9 * 3 + 4 + 3];
		int idx = 0;
		for (int suit = 1; suit <= 3; suit++) {
			for (int v = 1; v <= 9; v++) {
				allTiles[idx++] = MjConst.encode(suit, v);
			}
		}
		for (int v = 1; v <= 4; v++) {
			allTiles[idx++] = MjConst.encode(MjConst.SUIT_FENG, v);
		}
		for (int v = 1; v <= 3; v++) {
			allTiles[idx++] = MjConst.encode(MjConst.SUIT_JIAN, v);
		}

		return backtrackLaiZi(normalTiles, normalCount, laiZiCount, new int[laiZiCount], 0, allTiles);
	}

	private boolean backtrackLaiZi(int[] normalTiles, int normalCount, int totalLaiZi,
								 int[] laiZiValues, int depth, int[] candidates) {
		if (depth == totalLaiZi) {
			int[] combined = new int[normalCount + totalLaiZi];
			System.arraycopy(normalTiles, 0, combined, 0, normalCount);
			System.arraycopy(laiZiValues, 0, combined, normalCount, totalLaiZi);
			return checkStandardWin(combined);
		}

		for (int tileId : candidates) {
			laiZiValues[depth] = tileId;
			if (backtrackLaiZi(normalTiles, normalCount, totalLaiZi, laiZiValues, depth + 1, candidates)) {
				return true;
			}
		}
		return false;
	}

	public int getLaiZiTileId() {
		return laiZiTileId;
	}

	public void setLaiZiTileId(int laiZiTileId) {
		this.laiZiTileId = laiZiTileId;
	}
}

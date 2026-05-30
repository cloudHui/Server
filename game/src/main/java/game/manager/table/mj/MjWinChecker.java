package game.manager.table.mj;

import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;

import java.util.*;

/**
 * 通用麻将胡牌检测
 * 基础算法: 4面子+1将、七对、龙七对
 * 特殊玩法通过子类覆写扩展
 */
public class MjWinChecker {

	protected final boolean allowSevenPairs;

	public MjWinChecker() {
		this.allowSevenPairs = true;
	}

	public MjWinChecker(boolean allowSevenPairs) {
		this.allowSevenPairs = allowSevenPairs;
	}

	/**
	 * 检查手牌+副露是否能胡
	 */
	public boolean canWin(List<Card> handTiles, List<MjExposedSet> exposedSets, int winTile) {
		if (handTiles == null || handTiles.isEmpty()) {
			return false;
		}
		int meldCount = exposedSets != null ? exposedSets.size() : 0;
		int handSize = handTiles.size();
		if (handSize % 3 != 2) {
			return false;
		}

		int[] values = new int[handSize];
		for (int i = 0; i < handSize; i++) {
			values[i] = handTiles.get(i).getId();
		}

		// 检查标准胡牌: 4面子+1将
		if (checkStandardWin(values)) {
			return true;
		}

		// 检查七对(需配置允许)
		if (allowSevenPairs && meldCount == 0 && checkSevenPairs(values)) {
			return true;
		}

		return false;
	}

	/**
	 * 标准胡牌: 4面子+1将
	 */
	protected boolean checkStandardWin(int[] tiles) {
		if (tiles.length % 3 != 2) {
			return false;
		}
		int[] sorted = tiles.clone();
		Arrays.sort(sorted);

		for (int i = 0; i < sorted.length - 1; i++) {
			if (i > 0 && sorted[i] == sorted[i - 1]) {
				continue;
			}
			if (sorted[i] == sorted[i + 1]) {
				int[] remaining = removePair(sorted, i, i + 1);
				if (checkAllMelds(remaining)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkAllMelds(int[] tiles) {
		if (tiles.length == 0) {
			return true;
		}
		if (tiles.length % 3 != 0) {
			return false;
		}

		Arrays.sort(tiles);
		int first = tiles[0];

		// 尝试刻子
		if (tiles.length >= 3 && tiles[0] == tiles[1] && tiles[1] == tiles[2]) {
			int[] remaining = removeTriplet(tiles, 0);
			if (checkAllMelds(remaining)) {
				return true;
			}
		}

		// 尝试顺子
		int suit = MjConst.suitOf(first);
		int value = MjConst.valueOf(first);
		if (suit <= MjConst.SUIT_TONG && value <= 7) {
			int t1 = MjConst.encode(suit, value + 1);
			int t2 = MjConst.encode(suit, value + 2);
			int idx1 = indexOf(tiles, t1, 1);
			int idx2 = indexOf(tiles, t2, idx1 >= 0 ? idx1 + 1 : -1);
			if (idx1 >= 0 && idx2 >= 0) {
				int[] remaining = removeThree(tiles, 0, idx1, idx2);
				if (checkAllMelds(remaining)) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean checkSevenPairs(int[] tiles) {
		if (tiles.length != 14) {
			return false;
		}
		int[] sorted = tiles.clone();
		Arrays.sort(sorted);
		for (int i = 0; i < 14; i += 2) {
			if (sorted[i] != sorted[i + 1]) {
				return false;
			}
		}
		return true;
	}

	// --- 工具方法 ---

	private int[] removePair(int[] sorted, int i1, int i2) {
		int[] result = new int[sorted.length - 2];
		int idx = 0;
		for (int i = 0; i < sorted.length; i++) {
			if (i != i1 && i != i2) {
				result[idx++] = sorted[i];
			}
		}
		return result;
	}

	private int[] removeTriplet(int[] sorted, int start) {
		int[] result = new int[sorted.length - 3];
		int idx = 0;
		for (int i = 0; i < sorted.length; i++) {
			if (i < start || i > start + 2) {
				result[idx++] = sorted[i];
			}
		}
		return result;
	}

	private int[] removeThree(int[] sorted, int i1, int i2, int i3) {
		int[] result = new int[sorted.length - 3];
		int idx = 0;
		for (int i = 0; i < sorted.length; i++) {
			if (i != i1 && i != i2 && i != i3) {
				result[idx++] = sorted[i];
			}
		}
		return result;
	}

	private int indexOf(int[] arr, int val, int from) {
		for (int i = (from >= 0 ? from : 0); i < arr.length; i++) {
			if (arr[i] == val) {
				return i;
			}
		}
		return -1;
	}

	// --- 碰/杠/吃检测 ---

	public boolean canPeng(List<Card> handTiles, int tileId) {
		int count = 0;
		for (Card c : handTiles) {
			if (c.getId() == tileId) count++;
		}
		return count >= 2;
	}

	public boolean canMingGang(List<Card> handTiles, int tileId) {
		int count = 0;
		for (Card c : handTiles) {
			if (c.getId() == tileId) count++;
		}
		return count >= 3;
	}

	public boolean canAnGang(List<Card> handTiles) {
		Map<Integer, Integer> countMap = new HashMap<>();
		for (Card c : handTiles) {
			countMap.merge(c.getId(), 1, Integer::sum);
		}
		for (int count : countMap.values()) {
			if (count >= 4) return true;
		}
		return false;
	}

	public boolean canBuGang(List<Card> handTiles, List<MjExposedSet> exposedSets, int tileId) {
		boolean hasPeng = false;
		for (MjExposedSet set : exposedSets) {
			if (set.getType() == MjExposedSet.Type.PENG && set.getTileIds().get(0) == tileId) {
				hasPeng = true;
				break;
			}
		}
		if (!hasPeng) return false;
		for (Card c : handTiles) {
			if (c.getId() == tileId) return true;
		}
		return false;
	}

	public boolean canChi(List<Card> handTiles, int tileId) {
		return !getChiCombos(handTiles, tileId).isEmpty();
	}

	public List<int[]> getChiCombos(List<Card> handTiles, int tileId) {
		List<int[]> combos = new ArrayList<>();
		int suit = MjConst.suitOf(tileId);
		int value = MjConst.valueOf(tileId);
		if (suit > MjConst.SUIT_TONG) return combos;

		boolean[] has = new boolean[10];
		for (Card c : handTiles) {
			if (MjConst.suitOf(c.getId()) == suit) {
				has[MjConst.valueOf(c.getId())] = true;
			}
		}

		if (value >= 3 && has[value - 2] && has[value - 1]) {
			combos.add(new int[]{MjConst.encode(suit, value - 2), MjConst.encode(suit, value - 1)});
		}
		if (value >= 2 && value <= 8 && has[value - 1] && has[value + 1]) {
			combos.add(new int[]{MjConst.encode(suit, value - 1), MjConst.encode(suit, value + 1)});
		}
		if (value <= 7 && has[value + 1] && has[value + 2]) {
			combos.add(new int[]{MjConst.encode(suit, value + 1), MjConst.encode(suit, value + 2)});
		}

		return combos;
	}

	public List<Integer> getAnGangTiles(List<Card> handTiles) {
		Map<Integer, Integer> countMap = new HashMap<>();
		for (Card c : handTiles) {
			countMap.merge(c.getId(), 1, Integer::sum);
		}
		List<Integer> result = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
			if (entry.getValue() >= 4) result.add(entry.getKey());
		}
		return result;
	}
}

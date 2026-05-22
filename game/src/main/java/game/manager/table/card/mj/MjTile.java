package game.manager.table.card.mj;

/**
 * 麻将牌
 * 牌ID编码: suit*100 + value
 */
public class MjTile implements Comparable<MjTile> {

	private final int id;

	public MjTile(int id) {
		this.id = id;
	}

	public MjTile(int suit, int value) {
		this.id = MjConst.encode(suit, value);
	}

	public int getId() {
		return id;
	}

	public int getSuit() {
		return MjConst.suitOf(id);
	}

	public int getValue() {
		return MjConst.valueOf(id);
	}

	@Override
	public int compareTo(MjTile o) {
		if (this.id == o.id) {
			return 0;
		}
		return this.id - o.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MjTile)) return false;
		return this.id == ((MjTile) obj).id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		int suit = getSuit();
		int val = getValue();
		String[] suitNames = {"", "万", "条", "筒", "", ""};
		String[] fengNames = {"", "东", "南", "西", "北"};
		String[] jianNames = {"", "中", "发", "白"};
		if (suit == MjConst.SUIT_FENG) {
			return fengNames[val];
		} else if (suit == MjConst.SUIT_JIAN) {
			return jianNames[val];
		} else {
			return val + suitNames[suit];
		}
	}
}

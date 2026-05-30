package game.manager.table.mj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 麻将副露牌组(碰/杠/吃)
 */
public class MjExposedSet {

	public enum Type {
		/** 明杠(别人出牌, 手里3张) */
		MING_GANG,
		/** 暗杠(手里4张) */
		AN_GANG,
		/** 补杠(碰后摸到第4张) */
		BU_GANG,
		/** 碰 */
		PENG,
		/** 吃 */
		CHI
	}

	private final Type type;
	private final List<Integer> tileIds;
	/** 来源座位(碰/杠来自谁的牌, -1表示暗杠) */
	private final int fromSeat;

	public MjExposedSet(Type type, List<Integer> tileIds, int fromSeat) {
		this.type = type;
		this.tileIds = Collections.unmodifiableList(new ArrayList<>(tileIds));
		this.fromSeat = fromSeat;
	}

	public Type getType() {
		return type;
	}

	public List<Integer> getTileIds() {
		return tileIds;
	}

	public int getFromSeat() {
		return fromSeat;
	}

	/**
	 * 是否是杠(任何类型)
	 */
	public boolean isGang() {
		return type == Type.MING_GANG || type == Type.AN_GANG || type == Type.BU_GANG;
	}

	/**
	 * 获取杠的牌ID(杠的基准牌)
	 */
	public int getGangTileId() {
		return tileIds.get(0);
	}
}

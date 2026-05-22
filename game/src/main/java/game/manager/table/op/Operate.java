package game.manager.table.op;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import game.manager.table.Table;
import proto.GameProto;

/**
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 * @className Operate
 * @description 操作管理器，负责游戏桌子的操作记录和处理
 * @createDate 2026-05-03
 */
public class Operate {

	/**
	 * 当前操作位置
	 */
	private int currOpSeat = -1;

	/**
	 * 上一个操作位置
	 */
	private int lastOpSeat;

	/**
	 * 位置操作信息 是不是麻将才用
	 */
	private final Map<Integer, Set<GameProto.OpInfo>> posOp = new HashMap<>();

	private final Table table;

	public Operate(Table table) {
		this.table = table;
	}

	public int getCurrOpSeat() {
		return currOpSeat;
	}

	public void setCurrOpSeat(int currOpSeat) {
		this.currOpSeat = currOpSeat;
	}

	public int getLastOpSeat() {
		return lastOpSeat;
	}

	public void setLastOpSeat(int lastOpSeat) {
		this.lastOpSeat = lastOpSeat;
	}

	public void addPosOpInfo(int pos, GameProto.OpInfo op) {
		posOp.computeIfAbsent(pos, k -> new HashSet<>()).add(op);
	}

	public Set<GameProto.OpInfo> getSeatOps(int seat) {
		return posOp.get(seat);
	}

	public Set<GameProto.OpInfo> getCurrChoice() {
		return posOp.get(currOpSeat);
	}

	/**
	 * 移动到下一个玩家操作
	 */
	public void moveToNextOp() {
		lastOpSeat = currOpSeat;
		if (++currOpSeat >= table.getTableModel().getSeatNum()) {
			currOpSeat = 0;
		}
		posOp.clear();
	}

	/**
	 * 重置
	 */
	public void reset() {
		currOpSeat = -1;
		lastOpSeat = -1;
		posOp.clear();
	}

	public void clearChoiceMap() {
		posOp.clear();
	}
}

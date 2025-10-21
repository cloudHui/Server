package game.manager.table.op;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.manager.table.Table;
import proto.GameProto;

/**
 * @author admin
 * @className Operate
 * @description
 * @createDate 2025/10/21 14:31
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
	 * 位置操作信息
	 */
	private final Map<Integer, List<GameProto.OpInfo>> posOp = new HashMap<>();

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

	/**
	 * 移动到下一个玩家操作
	 */
	public void moveToNextOp() {

	}

	/**
	 * 重置
	 */
	public void reset() {
		currOpSeat = -1;
		lastOpSeat = -1;
		posOp.clear();
	}
}

package msg.registor.enums;

/**
 * 桌子运行状态
 */
public enum TableState {
	IDLE_ROB(4, "等玩家抢地主开始", 8),
	ROB(3, "玩家抢地主通知", IDLE_ROB),
	START_ANI(2, "开始动画(或者发牌)", 3, ROB),
	WAITING(1, "等人开始", START_ANI),
	IDLE_CARD(8, "等玩家出牌操作开始", 20),
	CARD(7, "玩家出牌通知", IDLE_CARD),
	IDLE_SHOW_CARD(6, "等地主明牌", 5),
	SHOW_CARD(5, "地主明牌通知", IDLE_SHOW_CARD),
	TABLE_DIS(9, "牌局解散"),
	TABLE_OVER(10, "牌局结束", 1, TABLE_DIS),
	ROUND_OVER(11, "等人准备下一局", START_ANI),
	;

	private final int id;

	private final String des;

	private final int overTime;

	private final TableState next;

	TableState(int id, String des) {
		this.id = id;
		this.des = des;
		overTime = -1;
		next = null;
	}

	TableState(int id, String des, int overTime) {
		this.id = id;
		this.des = des;
		this.overTime = overTime;
		next = null;
	}

	TableState(int id, String des, TableState next) {
		this.id = id;
		this.des = des;
		this.next = next;
		overTime = -1;
	}

	TableState(int id, String des, int overTime, TableState next) {
		this.id = id;
		this.des = des;
		this.overTime = overTime;
		this.next = next;
	}

	public int getId() {
		return id;
	}

	public String getDes() {
		return des;
	}

	public int getOverTime() {
		return overTime;
	}

	public TableState getNext() {
		return next;
	}

	@Override
	public String toString() {
		return "{" + name() +
				",overTime=" + overTime +
				", next=" + (next == null ? null : next.name()) +
				'}';
	}
}

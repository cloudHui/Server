package msg.registor.enums;

public enum TableState {
	WAITE(0, "等待"),
	START(1, "开始"),
	;

	TableState(int state, String desc) {
		this.state = state;
		this.desc = desc;
	}

	private final int state;

	private final String desc;

	public int getState() {
		return state;
	}

	public String getDesc() {
		return desc;
	}


	@Override
	public String toString() {
		return "TableState{" +
				"state=" + state +
				", desc='" + desc + '\'' +
				'}';
	}
}

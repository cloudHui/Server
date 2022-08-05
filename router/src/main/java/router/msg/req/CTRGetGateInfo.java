package router.msg.req;

public class CTRGetGateInfo {
	private String uniqCode;

	public CTRGetGateInfo() {
	}

	public String getUniqCode() {
		return uniqCode;
	}

	public void setUniqCode(String uniqCode) {
		this.uniqCode = uniqCode;
	}

	@Override
	public String toString() {
		return "CTRGetGateInfo{" +
				"uniqCode='" + uniqCode + '\'' +
				'}';
	}
}

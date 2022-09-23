package msg.http.req;

public class GetGateInfoRequest {
	private String uniqCode;

	public GetGateInfoRequest() {
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

package msg.http.req;

import java.util.List;

/**
 * 注册 gate 信息 请求
 */
public class RegisterGateInfoRequest {

	private List<String> innerIpPort;

	private List<String> ipPort;

	public RegisterGateInfoRequest() {
	}

	public List<String> getIpPort() {
		return ipPort;
	}

	public List<String> getInnerIpPort() {
		return innerIpPort;
	}

	public void setInnerIpPort(List<String> innerIpPort) {
		this.innerIpPort = innerIpPort;
	}

	public void setIpPort(List<String> ipPort) {
		this.ipPort = ipPort;
	}

	@Override
	public String toString() {
		return "RegisterGateInfoRequest{" +
				"innerIpPort=" + innerIpPort +
				", ipPort=" + ipPort +
				'}';
	}
}

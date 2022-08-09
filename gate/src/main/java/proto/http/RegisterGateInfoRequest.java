package proto.http;

import java.util.ArrayList;
import java.util.List;

/**
 * 注册 gate 信息 请求
 */
public class RegisterGateInfoRequest {

	private List<String> innerIpPort = new ArrayList<>();

	private List<String> ipPort = new ArrayList<>();

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

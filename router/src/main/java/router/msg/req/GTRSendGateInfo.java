package router.msg.req;

import java.util.List;

/**
 *  gate  to router 发送 gate ip 和端口 信息
 */
public class GTRSendGateInfo {
	private List<String> gateIpPortList;

	public GTRSendGateInfo() {
	}

	public List<String> getGateIpPortList() {
		return gateIpPortList;
	}

	public void setGateIpPortList(List<String> gateIpPortList) {
		this.gateIpPortList = gateIpPortList;
	}
}

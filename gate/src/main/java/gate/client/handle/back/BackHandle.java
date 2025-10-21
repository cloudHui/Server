package gate.client.handle.back;

import gate.client.GateTcpClient;
import net.message.TCPMessage;

public interface BackHandle {

	void handle(TCPMessage response, GateTcpClient client);
}

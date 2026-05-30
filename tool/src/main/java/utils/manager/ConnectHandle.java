package utils.manager;

import com.google.protobuf.Message;
import net.client.Sender;

public interface ConnectHandle {

	void handle(Message message, Sender handler, int sequence, int transId);
}
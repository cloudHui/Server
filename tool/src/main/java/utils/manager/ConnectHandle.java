package utils.manager;

import com.google.protobuf.Message;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ConnectHandle {
	Logger LOGGER = LoggerFactory.getLogger(ConnectHandle.class);

	void handle(Message message, Sender handler, int sequence, int transId);
}
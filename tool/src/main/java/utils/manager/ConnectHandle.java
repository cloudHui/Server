package utils.manager;

import com.google.protobuf.Message;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ConnectHandle {
	Logger LOGGER = LoggerFactory.getLogger(ConnectHandle.class);

	void handle(Message message, ConnectHandler serverClient, int sequence);
}

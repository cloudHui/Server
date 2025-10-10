package robot.connect.handle;

import com.google.protobuf.Message;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RobotHandle {
	Logger LOGGER = LoggerFactory.getLogger(RobotHandle.class);

	void handle(Message message, ConnectHandler serverClient);
}

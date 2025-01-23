package utils.handel;

import com.google.protobuf.Message;
import net.client.Sender;
import net.handler.Handler;

/**
 * 注册回复处理
 */
public class RegisterAckHandler implements Handler {

	private static final RegisterAckHandler instance = new RegisterAckHandler();

	public static RegisterAckHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		return true;
	}
}

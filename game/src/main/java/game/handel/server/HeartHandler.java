package game.handel.server;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 心跳请求
 */
public class HeartHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(HeartHandler.class);

	private static final HeartHandler instance = new HeartHandler();

	public static HeartHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long role, Message reqHeart, int mapId) {
		ModelProto.ReqHeart req = (ModelProto.ReqHeart) reqHeart;
		long now = System.currentTimeMillis();
		int serverType = req.getServerType();
		ModelProto.AckHeart.Builder ack = ModelProto.AckHeart.newBuilder();
		ack.setReqTime(now);
		ack.setServerType(ServerType.Game.getServerType());
		sender.sendMessage(MessageId.HEART_ACK, ack.build(), null);
		logger.error("server:{}, heart req", ServerType.get(serverType));
		return true;
	}
}

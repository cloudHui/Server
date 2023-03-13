package game.handel;

import msg.MessageHandel;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 心跳请求
 */
public class HeartHandler implements Handler<ModelProto.ReqHeart> {

	private final static Logger logger = LoggerFactory.getLogger(HeartHandler.class);

	private static HeartHandler instance = new HeartHandler();

	public static HeartHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqHeart req) {
		long now = System.currentTimeMillis();
		long cost = now - req.getReqTime();
		int serverType = req.getServerType();
		ModelProto.AckHeart.Builder ack = ModelProto.AckHeart.newBuilder();
		ack.setReqTime(now);
		ack.setServerType(ServerType.Game.getServerType());
		sender.sendMessage(MessageHandel.HEART_ACK, ack.build(), null);
		logger.info("server:{}, heart req cost:{}ms", ServerType.get(serverType), cost);
		if (cost > 50) {
			//超过50毫秒 打印错误日志
			logger.error("server:{}, heart toolong cost:{}ms ", ServerType.get(serverType), cost);
		}
		return true;
	}
}

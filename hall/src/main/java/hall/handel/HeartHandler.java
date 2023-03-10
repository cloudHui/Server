package hall.handel;

import msg.MessageHandel;
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
		ModelProto.AckHeart.Builder ackHeart = ModelProto.AckHeart.newBuilder();
		ackHeart.setReqTime(req.getReqTime());
		ackHeart.setAckTime(System.currentTimeMillis());
		sender.sendMessage(MessageHandel.HEART, ackHeart.build(), null);
		long cost = ackHeart.getAckTime() - ackHeart.getReqTime();
		logger.info("server:{}, heart cost:{}ms", req.getServerType(), cost);
		if (cost > 50) {
			//超过50毫秒 打印错误日志
			logger.error("server:{}, heart toolong cost:{}ms ", req.getServerType(), cost);
		}
		return true;
	}
}

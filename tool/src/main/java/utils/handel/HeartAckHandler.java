package utils.handel;

import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 心跳请求
 */
public class HeartAckHandler implements Handler<ModelProto.AckHeart> {

	private final static Logger logger = LoggerFactory.getLogger(HeartAckHandler.class);

	private static HeartAckHandler instance = new HeartAckHandler();

	public static HeartAckHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.AckHeart ack) {
		long now = System.currentTimeMillis();
		long cost = now - ack.getReqTime();
		int serverType = ack.getServerType();
		logger.info("server:{}, heart ack cost:{}ms", ServerType.get(serverType), cost);
		if (cost > 50) {
			//超过50毫秒 打印错误日志
			logger.error("server:{}, heart ack toolong cost:{}ms ", ServerType.get(serverType), cost);
		}
		return true;
	}
}

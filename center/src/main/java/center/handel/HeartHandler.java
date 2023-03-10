package center.handel;

import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 心跳请求
 */
public class HeartHandler implements Handler<ModelProto.Heart> {

	private final static Logger logger = LoggerFactory.getLogger(HeartHandler.class);

	private static HeartHandler instance = new HeartHandler();

	public static HeartHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.Heart req) {
		long cost = System.currentTimeMillis() - req.getReqTime();
		int serverType = req.getServerType();
		logger.info("server:{}, heart req cost:{}ms", ServerType.get(serverType), cost);
		if (cost > 50) {
			//超过50毫秒 打印错误日志
			logger.error("server:{}, heart toolong cost:{}ms ", ServerType.get(serverType), cost);
		}
		return true;
	}
}

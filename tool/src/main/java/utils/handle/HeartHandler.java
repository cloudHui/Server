package utils.handle;

import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 心跳请求
 */
@ProcessType(CMsg.HEART)
public class HeartHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(HeartHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message reqHeart, int mapId, long sequence) {
		ModelProto.ReqHeart req = (ModelProto.ReqHeart) reqHeart;
		long now = System.currentTimeMillis();
		int serverType = req.getServerType();
		ModelProto.AckHeart.Builder ack = ModelProto.AckHeart.newBuilder();
		ack.setReqTime(now);
		ack.setRetryTime(req.getRetryTime());
		sender.sendMessage(clientId, CMsg.HEART_ACK, mapId, 0, ack.build(), sequence);
		logger.debug("[server:{}, heart req cost:{}ms]", ServerType.get(serverType), now - req.getReqTime());
		return true;
	}
}

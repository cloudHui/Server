package gate.connect;

import java.util.HashMap;
import java.util.Map;

import gate.client.GateTcpClient;
import gate.handel.AckServerInfoHandel;
import gate.handel.RegisterNoticeHandler;
import gate.handel.ServerBreakNoticeHandler;
import msg.MessageId;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

public class ConnectProcessor {

	private final static Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);


	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageId.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case MessageId.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageId.ACK_SERVER:
				return ModelProto.AckServerInfo.parseFrom(bytes);
			case MessageId.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			case MessageId.REGISTER_NOTICE:
				return ModelProto.NotRegisterInfo.parseFrom(bytes);
			case MessageId.BREAK_NOTICE:
				return ModelProto.NotServerBreak.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(MessageId.HEART_ACK, HeartAckHandler.getInstance());
		handlers.put(MessageId.ACK_SERVER, AckServerInfoHandel.getInstance());
		handlers.put(MessageId.BREAK_NOTICE, ServerBreakNoticeHandler.getInstance());

	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > MessageId.BASE_ID_INDEX) {
			int userId = 0;
			if (msgId == MessageId.HallMsg.ACK_LOGIN.getId()) {
				HallProto.AckLogin ack = HallProto.AckLogin.parseFrom(tcpMessage.getMessage());
				userId = ack.getUserId();
			}
			//直接转发给客户端的
			return transferMsg(tcpMessage.getMapId(), tcpMessage, userId);
		}
		return false;
	};

	/**
	 * 找到客户端链接 并转发消息
	 */
	public static boolean transferMsg(long connectId, TCPMessage msg, int userId) {
		GateTcpClient gateClient = (GateTcpClient) ClientHandler.getClient(connectId);
		if (null != gateClient) {
			if (userId != 0) {
				gateClient.setRoleId(userId);
			}
			gateClient.sendMessage(msg);
			return true;
		}
		logger.error("ERROR! failed for transfer message(connect:{} message id:{})", connectId, msg.getMessageId());
		return false;
	}
}

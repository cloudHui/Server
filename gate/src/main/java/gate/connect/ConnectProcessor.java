package gate.connect;

import java.util.HashMap;
import java.util.Map;

import gate.client.ClientProto;
import gate.client.GateTcpClient;
import gate.handel.server.AckServerInfoHandel;
import gate.handel.server.RegisterNoticeHandler;
import gate.handel.server.ServerBreakNoticeHandler;
import msg.Message;
import net.client.handler.ClientHandler;
import net.connect.TCPConnect;
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

	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);


	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case Message.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case Message.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case Message.ACK_SERVER:
				return ModelProto.AckServerInfo.parseFrom(bytes);
			case Message.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			case Message.REGISTER_NOTICE:
				return ModelProto.NotRegisterInfo.parseFrom(bytes);
			case Message.BREAK_NOTICE:
				return ModelProto.NotServerBreak.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(Message.HEART_ACK, HeartAckHandler.getInstance());
		handlers.put(Message.ACK_SERVER, AckServerInfoHandel.getInstance());
		handlers.put(Message.BREAK_NOTICE, ServerBreakNoticeHandler.getInstance());

	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > Message.BASE_ID_INDEX) {
			int userId = 0;
			if (msgId == Message.HallMsg.ACK_LOGIN.getId()) {
				HallProto.AckLogin ack = HallProto.AckLogin.parseFrom(tcpMessage.getMessage());
				userId = ack.getUserId();
			}
			//直接转发给客户端的
			return transferMsg(tcpMessage.getMapId(), tcpMessage, null, userId);
		}
		return false;
	};

	/**
	 * 找到客户端链接 并转发消息
	 */
	public static boolean transferMsg(long connectId, TCPMessage msg, com.google.protobuf.Message innerMsg, int userId) {
		GateTcpClient gateClient = (GateTcpClient) ClientHandler.getClient(connectId);
		if (null != gateClient) {
			if (userId != 0) {
				gateClient.setUserId(userId);
			}
			if (null != innerMsg) {
				gateClient.sendMessage(replace(msg, innerMsg));
			} else {
				gateClient.sendMessage(msg);
			}
			return true;
		}
		logger.error("ERROR! failed for transfer message(connect:{} message id:{})", connectId, msg.getMessageId());
		return false;
	}

	public static TCPMessage replace(TCPMessage tcpMessage, com.google.protobuf.Message msg) {
		tcpMessage.setMessage(msg.toByteArray());
		return tcpMessage;
	}
}

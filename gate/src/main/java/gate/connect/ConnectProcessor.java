package gate.connect;

import java.util.HashMap;
import java.util.Map;

import gate.client.GateTcpClient;
import msg.HallMessageId;
import msg.MessageId;
import msg.MessageTrans;
import msg.registor.HandleTypeRegister;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

public class ConnectProcessor {

	private final static Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public static void init() {
		HandleTypeRegister.bindClassProcess(ConnectProcessor.class, handlers);
		HandleTypeRegister.bindTransMap(MessageId.class, TRANS_MAP, MessageTrans.GateClient);
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > MessageId.BASE_ID_INDEX && (msgId & 1) == 0) {
			GateTcpClient gateClient = (GateTcpClient) ClientHandler.getClient(tcpMessage.getClientId());
			if (null != gateClient) {
				if (msgId == HallMessageId.ACK_LOGIN_MSG) {
					HallProto.AckLogin ack = HallProto.AckLogin.parseFrom(tcpMessage.getMessage());
					gateClient.setRoleId(ack.getUserId());
					gateClient.setClubId(ack.getClub());
					gateClient.setChannel(ack.getChannel());
				}
				//直接转发给客户端的
				gateClient.sendMessage(tcpMessage);
				return true;
			}
			logger.error("[ERROR! failed for transfer message(clientId:{} message id:{})]", tcpMessage.getClientId(), tcpMessage.getMessageId());
		}
		return false;
	};
}

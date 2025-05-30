package gate.connect.handle;

import java.util.Map;

import com.google.protobuf.Message;
import gate.client.GateTcpClient;
import msg.registor.message.CMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GateProto;

/**
 * 广播
 */
@ProcessType(CMsg.BROAD)
public class BroadCastHandle implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(BroadCastHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		GateProto.BroadCast broadCast = (GateProto.BroadCast) message;
		Broad broad = Broad.get(broadCast.getType());
		LOGGER.error("[BroadCast:{}]", broadCast.toString());
		if (broad != null) {
			doBroad(broad, broadCast);
		} else {
			LOGGER.error("[BroadCast:{} can not find broadType]", broadCast.toString());
		}
		return true;
	}

	/**
	 * 处理广播消息
	 */
	private void doBroad(Broad broad, GateProto.BroadCast broadCast) {
		Map<Integer, Sender> allClient;
		GateTcpClient tcpClient;
		switch (broad) {
			case PERSON:
				ClientHandler client = ClientHandler.getClient(broadCast.getClientId());
				if (client != null) {
					client.sendMessage(CMsg.BROAD, broadCast);
				} else {
					LOGGER.error("[BroadCast:{} error no client]", broadCast.toString());
				}
				break;
			case ALL:
				allClient = ClientHandler.getAllClient();
				for (Map.Entry<Integer, Sender> entry : allClient.entrySet()) {
					tcpClient = (GateTcpClient) entry.getValue();
					tcpClient.sendMessage(CMsg.BROAD, broadCast);
				}
				break;
			case CHANNEL:
				allClient = ClientHandler.getAllClient();
				for (Map.Entry<Integer, Sender> entry : allClient.entrySet()) {
					try {
						tcpClient = (GateTcpClient) entry.getValue();
						if (tcpClient.getChannel() == broadCast.getChannel()) {
							tcpClient.sendMessage(CMsg.BROAD, broadCast);
						}
					} catch (Exception e) {
						LOGGER.error("[BroadCast:{} Channel error:{} ]", broadCast.toString(), e.getMessage());
					}
				}
				break;
			case CLUB:
				allClient = ClientHandler.getAllClient();
				for (Map.Entry<Integer, Sender> entry : allClient.entrySet()) {
					try {
						tcpClient = (GateTcpClient) entry.getValue();
						if (tcpClient.getClubId() == broadCast.getClub()) {
							tcpClient.sendMessage(CMsg.BROAD, broadCast);
						}
					} catch (Exception e) {
						LOGGER.error("[BroadCast:{} Club  error:{} ]", broadCast.toString(), e.getMessage());
					}
				}
				break;
			default:
				break;
		}
	}

	enum Broad {
		PERSON(0, "个人"),
		ALL(1, "全服"),
		CHANNEL(2, "渠道"),
		CLUB(3, "工会"),
		;
		int id;
		String des;


		Broad(int id, String des) {
			this.id = id;
			this.des = des;
		}

		public static Broad get(int id) {
			for (Broad broad : values()) {
				if (broad.id == id) {
					return broad;
				}
			}
			return null;
		}
	}
}

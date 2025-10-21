package gate.connect.handle.not;

import java.util.Map;

import com.google.protobuf.Message;
import gate.client.GateTcpClient;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GateProto;

/**
 * 广播消息处理器
 * 处理不同类型的广播消息分发
 */
@ProcessType(CMsg.BROAD)
public class BroadCastHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(BroadCastHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GateProto.BroadCast broadCast = (GateProto.BroadCast) message;
			BroadcastType broadcastType = BroadcastType.get(broadCast.getType());

			logger.info("处理广播消息, 类型: {}, 发送者: {}",
					broadcastType != null ? broadcastType.description : "未知", clientId);

			if (broadcastType != null) {
				executeBroadcast(broadcastType, broadCast);
			} else {
				logger.error("未知的广播类型: {}, 广播内容: {}", broadCast.getType(), broadCast.toString());
			}
			return true;
		} catch (Exception e) {
			logger.error("处理广播消息失败, clientId: {}, error: {}", clientId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 执行广播分发
	 */
	private void executeBroadcast(BroadcastType broadcastType, GateProto.BroadCast broadCast) {
		switch (broadcastType) {
			case PERSON:
				broadcastToPerson(broadCast);
				break;
			case ALL:
				broadcastToAll(broadCast);
				break;
			case CHANNEL:
				broadcastToChannel(broadCast);
				break;
			case CLUB:
				broadcastToClub(broadCast);
				break;
			default:
				logger.warn("未处理的广播类型: {}", broadcastType);
				break;
		}
	}

	/**
	 * 广播给指定个人
	 */
	private void broadcastToPerson(GateProto.BroadCast broadCast) {
		ClientHandler client = ClientHandler.getClient(broadCast.getClientId());
		if (client != null) {
			client.sendMessage(CMsg.BROAD, broadCast);
			logger.debug("个人广播成功, targetClientId: {}", broadCast.getClientId());
		} else {
			logger.warn("目标客户端不存在, 无法发送个人广播, targetClientId: {}", broadCast.getClientId());
		}
	}

	/**
	 * 广播给所有客户端
	 */
	private void broadcastToAll(GateProto.BroadCast broadCast) {
		Map<Integer, Sender> allClients = ClientHandler.getAllClient();
		int successCount = 0;

		for (Map.Entry<Integer, Sender> entry : allClients.entrySet()) {
			try {
				GateTcpClient client = (GateTcpClient) entry.getValue();
				client.sendMessage(CMsg.BROAD, broadCast);
				successCount++;
			} catch (Exception e) {
				logger.error("全服广播发送失败, clientId: {}, error: {}", entry.getKey(), e.getMessage());
			}
		}

		logger.info("全服广播完成, 总数: {}, 成功: {}", allClients.size(), successCount);
	}

	/**
	 * 广播给指定渠道
	 */
	private void broadcastToChannel(GateProto.BroadCast broadCast) {
		Map<Integer, Sender> allClients = ClientHandler.getAllClient();
		int targetChannel = broadCast.getChannel();
		int successCount = 0;

		for (Map.Entry<Integer, Sender> entry : allClients.entrySet()) {
			try {
				GateTcpClient client = (GateTcpClient) entry.getValue();
				if (client.getChannel() == targetChannel) {
					client.sendMessage(CMsg.BROAD, broadCast);
					successCount++;
				}
			} catch (Exception e) {
				logger.error("渠道广播发送失败, clientId: {}, channel: {}, error: {}",
						entry.getKey(), targetChannel, e.getMessage());
			}
		}

		logger.info("渠道广播完成, channel: {}, 匹配客户端: {}", targetChannel, successCount);
	}

	/**
	 * 广播给指定俱乐部
	 */
	private void broadcastToClub(GateProto.BroadCast broadCast) {
		Map<Integer, Sender> allClients = ClientHandler.getAllClient();
		int targetClub = broadCast.getClub();
		int successCount = 0;

		for (Map.Entry<Integer, Sender> entry : allClients.entrySet()) {
			try {
				GateTcpClient client = (GateTcpClient) entry.getValue();
				if (client.getClubId() == targetClub) {
					client.sendMessage(CMsg.BROAD, broadCast);
					successCount++;
				}
			} catch (Exception e) {
				logger.error("俱乐部广播发送失败, clientId: {}, club: {}, error: {}",
						entry.getKey(), targetClub, e.getMessage());
			}
		}

		logger.info("俱乐部广播完成, club: {}, 匹配客户端: {}", targetClub, successCount);
	}

	/**
	 * 广播类型枚举
	 */
	enum BroadcastType {
		PERSON(0, "个人"),
		ALL(1, "全服"),
		CHANNEL(2, "渠道"),
		CLUB(3, "工会");

		private final int id;
		private final String description;

		BroadcastType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		public static BroadcastType get(int id) {
			for (BroadcastType type : values()) {
				if (type.id == id) {
					return type;
				}
			}
			return null;
		}
	}
}
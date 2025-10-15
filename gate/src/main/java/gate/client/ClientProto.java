package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import gate.Gate;
import io.netty.channel.ChannelHandler;
import msg.registor.HandleTypeRegister;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.client.handler.WsClientHandler;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ModelProto;
import proto.ResultProto;
import utils.ServerManager;

public class ClientProto {
	private final static Logger LOGGER = LoggerFactory.getLogger(ClientProto.class);
	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (gateClient, tcpMessage) -> transferMessage((GateTcpClient) gateClient, tcpMessage);
	public final static Parser PARSER = HandleTypeRegister::parseMessage;
	private final static Map<Integer, Handler> HANDLER_MAP = new HashMap<>();
	public final static Handlers HANDLERS = HANDLER_MAP::get;

	public static void init() {
		//绑定专用服务器消息处理
		HandleTypeRegister.initFactory(ClientProto.class, HANDLER_MAP);
		//绑定通用服务器消息处理
		HandleTypeRegister.initFactory(HANDLER_MAP);
	}

	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateTcpClient client, TCPMessage tcpMessage) {
		int msgId = tcpMessage.getMessageId();
		if (msgId < CMsg.BASE_ID_INDEX) {
			return false;
		}
		long sequence = tcpMessage.getSequence();
		tcpMessage.setMapId(client.getId());
		setClientId(tcpMessage, client);
		ConnectHandler connect = getTransServerClient(msgId);
		if (connect != null) {
			long start = System.currentTimeMillis();
			connect.sendTcpMessage(tcpMessage, 3).whenComplete((message, throwable) -> {
				if (null != throwable) {
					LOGGER.error("[ERROR! failed for send {} to:{}]", Integer.toHexString(msgId), connect.getConnectServer(), throwable);
					client.sendMessage(TCPMessage.newInstance(ResultProto.Result.TIME_OUT_VALUE));
				} else {
					try {
						message.setSequence(sequence);
						transResToClient(message, start, client);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			return true;
		}
		LOGGER.error("[transferMessage msgId:{} error]", Integer.toHexString(msgId));
		return true;
	}

	/**
	 * 设置链接id 如果是登录使用链接id 否则是玩家id
	 */
	private static void setClientId(TCPMessage tcpMessage, GateTcpClient client) {
		tcpMessage.setClientId((tcpMessage.getMessageId() & HMsg.REQ_LOGIN_MSG) != 0 ? client.getId() : client.getRoleId());
	}

	/**
	 * 服务器返回消息到客户端
	 */
	private static void transResToClient(TCPMessage tcpMessage, long start, GateTcpClient client) {
		int msgId = tcpMessage.getMessageId();
		if (msgId > CMsg.BASE_ID_INDEX && (msgId & 2) != 0) {
			if (msgId == HMsg.ACK_LOGIN_MSG) {
				try {
					HallProto.AckLogin ack = HallProto.AckLogin.parseFrom(tcpMessage.getMessage());
					client.setRoleId(ack.getUserId());
					client.setClubId(ack.getClub());
					client.setChannel(ack.getChannel());
					notCenterLink(ClientHandler.getRemoteIP(client).getHostString());
				} catch (Exception e) {
					LOGGER.error("AckLogin parse error msgId:{} userId:{}", Integer.toHexString(msgId), client.getRoleId());
				}
			}
			//直接转发给客户端的
			client.sendMessage(tcpMessage);
			LOGGER.error("transResToClient success msgId:{} userId:{} cost:{}ms", Integer.toHexString(msgId), client.getRoleId(),
					System.currentTimeMillis() - start);
			return;
		}
		LOGGER.error("[transResToClient! failed clientId:{} msgId:{}]", client.getId(), Integer.toHexString(msgId));
	}

	/**
	 * 获取转发服务的链接
	 */
	private static ConnectHandler getTransServerClient(int msgId) {
		ServerType serverType = getServerTypeByMessageId(msgId);
		if (serverType == null) {
			LOGGER.error("[getTransServerClient error no serverType msgId:{}]", Integer.toHexString(msgId));
			return null;
		}
		return Gate.getInstance().getServerManager().getServerClient(serverType);
	}

	/**
	 * 通过消息id获取要转发的服务类型
	 */
	private static ServerType getServerTypeByMessageId(int msgId) {
		if ((msgId & CMsg.GAME_TYPE) != 0) {
			return ServerType.Game;
		} else if ((msgId & CMsg.HALL_TYPE) != 0) {
			return ServerType.Hall;
		} else if ((msgId & CMsg.ROOM_TYPE) != 0) {
			return ServerType.Room;
		}
		return null;
	}

	/**
	 * 通知服务器玩家离线
	 */
	protected static void notServerBreak(int userId, ChannelHandler handler) {
		ModelProto.NotBreak.Builder not = ModelProto.NotBreak.newBuilder();
		not.setUserId(userId);
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}

		sendNotBreak(ServerType.Game, not.build());
		sendNotBreak(ServerType.Hall, not.build());
		sendNotBreak(ServerType.Room, not.build());
		ConnectHandler serverClient = Gate.getInstance().getServerManager().getServerClient(ServerType.Center);
		if (serverClient != null) {
			if (handler instanceof ClientHandler) {
				not.setCert(ByteString.copyFromUtf8(ClientHandler.getRemoteIP((ClientHandler) handler).getHostName()));
			} else if (handler instanceof WsClientHandler) {
				not.setCert(ByteString.copyFromUtf8(WsClientHandler.getRemoteIP((WsClientHandler) handler).getHostName()));
			}
			serverClient.sendMessage(CMsg.NOT_BREAK, not.build());
		}
	}

	/**
	 * 通知服务器玩家掉线掉线
	 */
	private static void sendNotBreak(ServerType serverType, ModelProto.NotBreak not) {
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}
		ConnectHandler serverClient = serverManager.getServerClient(serverType);
		if (serverClient != null) {
			serverClient.sendMessage(CMsg.NOT_BREAK, not);
		}
	}

	/**
	 * 通知中心玩家登录成功
	 */
	private static void notCenterLink(String cert) {
		ModelProto.NotRegisterClient.Builder not = ModelProto.NotRegisterClient.newBuilder();
		not.setCert(ByteString.copyFromUtf8(cert));
		ConnectHandler serverClient = Gate.getInstance().getServerManager().getServerClient(ServerType.Center);
		if (serverClient != null) {
			serverClient.sendMessage(CMsg.NOT_LINK, not.build());
		}
	}
}
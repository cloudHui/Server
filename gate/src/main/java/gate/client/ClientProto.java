package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import gate.Gate;
import io.netty.channel.ChannelHandler;
import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
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
import utils.ServerManager;

public class ClientProto {
	private final static Logger LOGGER = LoggerFactory.getLogger(ClientProto.class);
	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();
	private final static Map<Integer, Handler> HANDLER_MAP = new HashMap<>();
	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (gateClient, tcpMessage) -> transferMessage((GateTcpClient) gateClient, tcpMessage);

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	public final static Handlers HANDLERS = HANDLER_MAP::get;

	public static void init() {
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.GateServer);

		//绑定专用服务器消息处理
		HandleTypeRegister.bindClassPackageProcess(ClientProto.class, HANDLER_MAP);
		//绑定通用服务器消息处理
		HandleTypeRegister.bindDefaultPackageProcess(HANDLER_MAP);
	}

	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateTcpClient gateClient, TCPMessage tcpMessage) {
		if (tcpMessage.getMessageId() < CMsg.BASE_ID_INDEX) {
			return false;
		}
		int msgId = tcpMessage.getMessageId();

		tcpMessage.setMapId(gateClient.getId());
		tcpMessage.setClientId(gateClient.getId());

		ConnectHandler connect = getTransServerClient(msgId, gateClient);
		if (connect != null) {
			long send = System.currentTimeMillis();
			connect.sendMessage(tcpMessage, 3).whenComplete((message, throwable) -> {
				LOGGER.info("[send transferMessage message: {} to {} back res success:{} cost:{}ms]",
						Integer.toHexString(msgId), connect.getConnectServer(),
						throwable == null ? true : throwable.getMessage(), System.currentTimeMillis() - send);
				transResToClient((TCPMessage) message);
			});
		}
		LOGGER.error("[error msg transferMessage to server msgId:{}]", Integer.toHexString(msgId));
		return false;
	}

	/**
	 * 服务器返回消息到客户端
	 */
	private static void transResToClient(TCPMessage tcpMessage) {
		int msgId = tcpMessage.getMessageId();
		int clientId = tcpMessage.getClientId();
		if (msgId > CMsg.BASE_ID_INDEX && (msgId & 1) == 0) {
			GateTcpClient gateClient = (GateTcpClient) ClientHandler.getClient(clientId);
			if (null != gateClient) {
				if (msgId == HMsg.ACK_LOGIN_MSG) {
					try {
						HallProto.AckLogin ack = HallProto.AckLogin.parseFrom(tcpMessage.getMessage());
						gateClient.setRoleId(ack.getUserId());
						gateClient.setClubId(ack.getClub());
						gateClient.setChannel(ack.getChannel());
						notCenterLink(ClientHandler.getRemoteIP(gateClient).getHostString());
					} catch (Exception e) {
						LOGGER.error("AckLogin parse error msgId:{} userId:{}", Integer.toHexString(msgId), gateClient.getRoleId());
					}
				}
				//直接转发给客户端的
				gateClient.sendMessage(tcpMessage);
				LOGGER.debug("transResToClient success msgId:{} userId:{}", Integer.toHexString(msgId), gateClient.getRoleId());
				return;
			}
			LOGGER.error("[ERROR! failed transResToClient gateClient null (clientId:{} msgId:{})]", clientId, Integer.toHexString(msgId));
			return;
		}
		LOGGER.error("[ERROR! failed transResToClient  (clientId:{} msgId id:{} error )]", clientId, Integer.toHexString(msgId));
	}

	/**
	 * 获取转发服务的链接
	 */
	private static ConnectHandler getTransServerClient(int msgId, GateTcpClient gateClient) {
		ServerType serverType = getServerTypeByMessageId(msgId);
		if (serverType == null) {
			LOGGER.error("[getTransServerClient error no serverType msgId:{}]", Integer.toHexString(msgId));
			return null;
		}
		ServerManager manager = Gate.getInstance().getServerManager();
		return getConnectHandler(getClientId(serverType, gateClient), serverType, manager, gateClient);
	}

	/**
	 * 获取服务id
	 */
	private static int getClientId(ServerType serverType, GateTcpClient gateClient) {
		int clientId = 0;
		switch (serverType) {
			case Game:
				clientId = gateClient.getGameId();
				break;
			case Hall:
				clientId = gateClient.getHallId();
				break;
			case Room:
				clientId = gateClient.getRoomId();
				break;
		}
		return clientId;
	}

	/**
	 * 获取服务器链接
	 */
	private static ConnectHandler getConnectHandler(int clientId, ServerType serverType, ServerManager manager, GateTcpClient gateClient) {
		if (clientId != 0) {
			return manager.getServerClient(serverType, clientId);
		} else {
			ConnectHandler connectHandler = manager.getServerClient(serverType);
			switch (serverType) {
				case Game:
					gateClient.setGameId(connectHandler.getConnectServer().getServerId());
					break;
				case Hall:
					gateClient.setHallId(connectHandler.getConnectServer().getServerId());
					break;
				case Room:
					gateClient.setRoomId(connectHandler.getConnectServer().getServerId());
					break;
			}
			return connectHandler;
		}
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
	protected static void notServerBreak(int userId, int gameId, int hallId, int roomId, ChannelHandler handler) {
		ModelProto.NotBreak.Builder not = ModelProto.NotBreak.newBuilder();
		not.setUserId(userId);
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}

		sendNotBreak(ServerType.Game, gameId, not.build());
		sendNotBreak(ServerType.Hall, hallId, not.build());
		sendNotBreak(ServerType.Room, roomId, not.build());
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
	private static void sendNotBreak(ServerType serverType, int clientId, ModelProto.NotBreak not) {
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}
		ConnectHandler serverClient = serverManager.getServerClient(serverType, clientId);
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
package room.client;

import io.netty.channel.ChannelHandler;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.Room;

/**
 * 房间客户端连接处理器
 * 专门处理网关服务器到房间服务器的连接
 */
public class RoomClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(RoomClient.class);

	private ModelProto.ServerInfo serverInfo;

	public RoomClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(this::handleConnectionClose);

		logger.debug("创建新的房间客户端连接");
	}

	/**
	 * 处理连接关闭事件
	 */
	private void handleConnectionClose(ChannelHandler client) {
		logger.info("房间客户端连接关闭, serverInfo: {}",
				serverInfo != null ? ServerType.get(serverInfo.getServerType()) : "unknown");

		if (serverInfo != null) {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Room.getInstance().getServerClientManager().removeServerClient(serverType, serverInfo.getServerId());
				logger.info("已从服务器客户端管理器中移除, serverType: {}, serverId: {}",
						serverType, serverInfo.getServerId());
			}
		}
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
		logger.debug("设置服务器信息, serverId: {}, type: {}",
				serverInfo.getServerId(), serverInfo.getServerType());
	}
}
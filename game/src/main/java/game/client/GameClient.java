package game.client;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import game.Game;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;

/**
 * 游戏客户端连接处理器
 * 处理网关服务器到游戏服务器的连接
 */
public class GameClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

	private ModelProto.ServerInfo serverInfo;

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(this::handleConnectionClose);

		logger.debug("创建新的游戏客户端连接");
	}

	/**
	 * 处理连接关闭事件
	 */
	private void handleConnectionClose(ChannelHandler client) {
		logger.info("游戏客户端连接关闭, serverInfo: {}", serverInfo != null ? serverInfo.getServerId() : "unknown");

		if (serverInfo != null) {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Game.getInstance().getServerClientManager().removeServerClient(serverType, serverInfo.getServerId());
				logger.info("已从服务器客户端管理器中移除, serverType: {}, serverId: {}",
						serverType, serverInfo.getServerId());
			}
		}
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
		logger.debug("设置服务器信息, serverType: {}, serverId: {}",
				ServerType.get(serverInfo.getServerType()), serverInfo.getServerId());
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}
}
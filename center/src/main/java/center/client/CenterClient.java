package center.client;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.Center;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 中心服务器客户端连接处理器
 * 处理其他服务器到中心服务器的连接
 */
public class CenterClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(CenterClient.class);

	private ModelProto.ServerInfo serverInfo;

	public CenterClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(this::handleConnectionClose);

		// 所有消息都认为是安全的（中心服务器信任所有连接）
		setSafe((msgId) -> true);

		logger.debug("创建新的中心服务器客户端连接");
	}

	/**
	 * 处理连接关闭事件
	 */
	private void handleConnectionClose(ChannelHandler client) {
		if (serverInfo == null) {
			logger.warn("连接关闭，但服务器信息为空");
			return;
		}

		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			logger.warn("连接关闭，未知的服务器类型: {}", serverInfo.getServerType());
			return;
		}

		ServerClientManager manager = Center.getInstance().getServerManager();
		manager.removeServerClient(serverType, serverInfo.getServerId());

		logger.info("服务器连接关闭, serverType: {}, serverId: {}, address: {}",
				serverType, serverInfo.getServerId(), serverInfo.getIpConfig().toStringUtf8());
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
		logger.debug("设置服务器信息, serverType: {}, serverId: {}",
				ServerType.get(serverInfo.getServerType()), serverInfo.getServerId());
	}
}
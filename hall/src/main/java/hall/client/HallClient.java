package hall.client;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hall.Hall;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;

/**
 * 大厅客户端连接处理器
 * 处理其他服务器到大堂服务器的连接
 */
public class HallClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(HallClient.class);

	private ModelProto.ServerInfo serverInfo;

	public HallClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(this::handleConnectionClose);

		logger.debug("创建新的大厅客户端连接");
	}

	/**
	 * 处理连接关闭事件
	 */
	private void handleConnectionClose(ChannelHandler client) {
		logger.info("大厅客户端连接关闭, serverInfo: {}",
				serverInfo != null ? serverInfo.getServerId() : "unknown");

		if (serverInfo != null) {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Hall.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
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
}
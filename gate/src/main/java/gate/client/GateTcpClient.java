package gate.client;

import gate.Gate;
import msg.Message;
import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.connect.TCPConnect;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;
import utils.ServerManager;


public class GateTcpClient extends ClientHandler<GateTcpClient, TCPMessage> {

	private int userId = 0;
	private int gameId = 0;
	private int hallId = 0;

	public GateTcpClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GateTcpClient>) client -> notServerBreak());

		setSafe((Safe<GateTcpClient, TCPMessage>) (gateClient, msg) -> msg.getMessageId() == Message.HallMsg.REQ_LOGIN.getId() || userId != 0);
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getHallId() {
		return hallId;
	}

	public void setHallId(int hallId) {
		this.hallId = hallId;
	}

	/**
	 * 通知服务器玩家离线
	 */
	public void notServerBreak() {
		ModelProto.NotBreak.Builder not = ModelProto.NotBreak.newBuilder();
		not.setUserId(getUserId());
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}
		TCPConnect serverClient = serverManager.getServerClient(ServerType.Game, getGameId());
		if (serverClient != null) {
			serverClient.sendMessage(Message.NOT_BREAK, not.build(), null);
		}
		serverClient = serverManager.getServerClient(ServerType.Hall, getHallId());
		if (serverClient != null) {
			serverClient.sendMessage(Message.NOT_BREAK, not.build(), null);
		}
	}
}

package gate.client;

import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.client.handler.WsClientHandler;
import net.connect.TCPConnect;
import net.message.TCPMaker;
import proto.ModelProto;
import utils.ServerManager;


public class GateWsClient extends WsClientHandler {

	private int userId = 0;
	private int gameId = 0;
	private int hallId = 0;

	public GateWsClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {
			notServerBreak();
		});

		setSafe((msgId) -> msgId == MessageId.HallMsg.REQ_LOGIN.getId() || userId != 0);
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
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build());
		}
		serverClient = serverManager.getServerClient(ServerType.Hall, getHallId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build());
		}
	}
}

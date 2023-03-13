package gate.client;

import gate.Gate;
import msg.MessageHandel;
import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.connect.TCPConnect;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;
import utils.ServerManager;


public class GateClient extends ClientHandler<GateClient, TCPMessage> {

	private int userId = 0;
	private int gameId = 0;
	private int hallId = 0;

	public GateClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GateClient>) client -> {
			//Todo
			//发送这个玩家下线到其他服务
			//删除这个玩家的链接和数据
			notServerBreak();
		});

		setSafe((Safe<GateClient, TCPMessage>) (gateClient, msg) -> msg.getMessageId() == MessageHandel.HallMsg.REQ_LOGIN.getId() || userId != 0);
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
		TCPConnect serverClient = serverManager.getServerClient(ServerType.Game, getGameId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageHandel.NOT_BREAK, not.build(), null);
		}
		serverClient = serverManager.getServerClient(ServerType.Hall, getHallId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageHandel.NOT_BREAK, not.build(), null);
		}
	}
}

package hall.handel.server;


import hall.Hall;
import hall.client.HallClient;
import msg.Message;
import msg.ServerType;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

public class ServerHandel {

	private final static Logger logger = LoggerFactory.getLogger(ServerHandel.class);

	/**
	 * 心跳
	 */
	public final static Handler<ModelProto.ReqHeart> HEART_HANDLER = (sender, sequence, req, mapId) -> {
		long now = System.currentTimeMillis();
		int serverType = req.getServerType();
		ModelProto.AckHeart.Builder ack = ModelProto.AckHeart.newBuilder();
		ack.setReqTime(now);
		ack.setServerType(ServerType.Game.getServerType());
		sender.sendMessage(Message.HEART_ACK, ack.build(), null);
		logger.error("server:{}, heart req", ServerType.get(serverType));
		return true;
	};

	/**
	 * 注册请求
	 */
	public final static Handler<ModelProto.ReqRegister> REGISTER_HANDLER = (sender, sequence, req, mapId) -> {
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}

		Hall.getInstance().setGateClient((HallClient) sender);

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(Math.toIntExact(sequence), Message.ACK_REGISTER, ackRegister.build(), null);
		return true;
	};

	/**
	 * 通知玩家断线
	 */
	public final static Handler<ModelProto.NotBreak> NOT_BREAK_HANDLER = (sender, sequence, req, mapId) -> true;
}

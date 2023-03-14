package test.handel;


import msg.Message;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import proto.HallProto;
import test.Test;

public class ServerHandel {

	private final static Logger logger = LoggerFactory.getLogger(ServerHandel.class);

	/**
	 * 登录
	 */
	public final static Handler<HallProto.AckLogin> ACK_LOGIN_HANDLER = (sender, sequence, ack, mapId) -> {
		logger.info("AckLoginHandler:{}", ack.toString());
		Test.getInstance().execute(() -> {
			GameProto.ReqEnterTable.Builder req = GameProto.ReqEnterTable.newBuilder();
			req.setTableId(1);
			sender.sendMessage(Message.GameMsg.REQ_ENTER_TABLE.getId(), req.build(), null);
		});
		return true;
	};

	/**
	 * 入桌
	 */
	public final static Handler<GameProto.AckEnterTable> ACK_ENTER_TABLE_HANDLER = (sender, sequence, ack, mapId) -> {
		logger.info("AckEnterTable:{}", ack.toString());
		return true;
	};
}

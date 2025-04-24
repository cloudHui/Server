package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.HallMessageId;
import msg.RoomMessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.HallProto;
import proto.RoomProto;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {

	private final static Map<Integer, Handler> MAP = new HashMap<>();

	public static void init() {
		HandleTypeRegister.bindClassProcess(ConnectProcessor.class, MAP);
	}


	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case HallMessageId.ACK_LOGIN_MSG:
				return HallProto.AckLogin.parseFrom(bytes);
			case RoomMessageId.ACK_ROOM_LIST_MSG:
				return RoomProto.AckGetRoomList.parseFrom(bytes);
			//case RoomMessageId.ACK_ROOM_LIST_MSG:
			//	return RoomProto.AckGetRoomList.parseFrom(bytes);
		}
		return null;
	};

	public final static Handlers HANDLERS = MAP::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}

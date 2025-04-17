package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.HallMessageId;
import msg.RoomMessageId;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.HallProto;
import proto.RoomProto;
import robot.connect.handel.AckGetRoomListHandler;
import robot.connect.handel.AckLoginHandler;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {

	public final static Parser PARSER = (id, bytes) -> {
		if (id == HallMessageId.ACK_LOGIN_MSG) {
			return HallProto.AckLogin.parseFrom(bytes);
		} else if (id == RoomMessageId.ACK_ROOM_LIST_MSG) {
			return RoomProto.AckGetRoomList.parseFrom(bytes);
		}
		return null;
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(HallMessageId.ACK_LOGIN_MSG, AckLoginHandler.getInstance());
		handlers.put(RoomMessageId.ACK_ROOM_LIST_MSG, AckGetRoomListHandler.getInstance());

	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;

}

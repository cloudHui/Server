package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.HallProto;
import proto.RoomProto;
import robot.handel.AckGetRoomListHandler;
import robot.handel.AckLoginHandler;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		if (id == MessageId.HallMsg.ACK_LOGIN.getId()) {
			return HallProto.AckLogin.parseFrom(bytes);
		} else if (id == MessageId.RoomMsg.ACK_ROOM_LIST.getId()) {
			return RoomProto.AckGetRoomList.parseFrom(bytes);
		}
		return null;
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.HallMsg.ACK_LOGIN.getId(), AckLoginHandler.getInstance());
		handlers.put(MessageId.RoomMsg.ACK_ROOM_LIST.getId(), AckGetRoomListHandler.getInstance());

	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;

}

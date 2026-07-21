package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.LobbyProto;

/**
 * Lobby 服务消息（原 HMsg + RMsg）
 */
@ClassType
public class LMsg {

	@ClassField(value = LobbyProto.ReqLogin.class, des = "请求登录")
	public static final int REQ_LOGIN_MSG = CMsg.LOBBY_TYPE | 1;

	@ClassField(value = LobbyProto.AckLogin.class, des = "登录回复")
	public static final int ACK_LOGIN_MSG = CMsg.LOBBY_TYPE | 2;

	@ClassField(value = LobbyProto.ReqUserRegister.class, des = "请求注册")
	public static final int REQ_REGISTER_MSG = CMsg.LOBBY_TYPE | 3;

	@ClassField(value = LobbyProto.AckUserRegister.class, des = "注册回复")
	public static final int ACK_REGISTER_MSG = CMsg.LOBBY_TYPE | 4;

	@ClassField(value = LobbyProto.ReqJoinRoomTable.class, des = "请求加入桌子")
	public static final int REQ_JOIN_ROOM_TABLE_MSG = CMsg.LOBBY_TYPE | 5;

	@ClassField(value = LobbyProto.AckJoinRoomTable.class, des = "加入桌子回复")
	public static final int ACK_JOIN_ROOM_TABLE_MSG = CMsg.LOBBY_TYPE | 6;

	@ClassField(value = LobbyProto.ReqRoomList.class, des = "请求房间列表")
	public static final int REQ_ROOM_LIST_MSG = CMsg.LOBBY_TYPE | 7;

	@ClassField(value = LobbyProto.AckRoomList.class, des = "房间列表回复")
	public static final int ACK_ROOM_LIST_MSG = CMsg.LOBBY_TYPE | 8;
}

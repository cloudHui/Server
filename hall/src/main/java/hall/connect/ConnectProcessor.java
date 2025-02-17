package hall.connect;

import java.util.HashMap;
import java.util.Map;

import hall.handle.server.AckServerInfoHandel;
import hall.handle.server.RegisterNoticeHandler;
import hall.handle.server.ServerBreakNoticeHandler;
import msg.MessageId;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.ModelProto;

public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageId.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case MessageId.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			case MessageId.ACK_SERVER:
				return ModelProto.AckServerInfo.parseFrom(bytes);
			case MessageId.REGISTER_NOTICE:
				return ModelProto.NotRegisterInfo.parseFrom(bytes);
			case MessageId.BREAK_NOTICE:
				return ModelProto.NotServerBreak.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.ACK_SERVER, AckServerInfoHandel.getInstance());
		handlers.put(MessageId.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(MessageId.BREAK_NOTICE, ServerBreakNoticeHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;

}

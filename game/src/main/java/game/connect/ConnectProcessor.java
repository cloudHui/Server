package game.connect;

import net.message.Parser;
import net.message.Transfer;
import proto.ModelProto;

import static msg.MessageId.ACK_REGISTER;
import static msg.MessageId.HEART_ACK;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
		}
		return null;
	};

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}

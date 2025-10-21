package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import utils.manager.ConnectHandle;

/**
 * 登录回复
 */
@ProcessClass(HallProto.AckLogin.class)
public class AckLoginHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		if (message instanceof HallProto.AckLogin) {
			HallProto.AckLogin ack = (HallProto.AckLogin) message;
			LOGGER.error("AckLogin:{}", ack.toString());
			//Todo 添加区分处理有房间重连 没房间获取房间列表 或者获取房间列表登录完成后直接请求 不管有没有房间 等获取房间列表回来后再看 没有房间就创建房间

		}
	}
}

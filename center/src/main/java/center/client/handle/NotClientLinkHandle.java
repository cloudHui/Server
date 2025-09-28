package center.client.handle;

import java.util.concurrent.ConcurrentHashMap;

import annotation.ProcessType;
import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * gate通知玩家链接
 */
@ProcessType(value = CMsg.NOT_BREAK)
public class NotClientLinkHandle implements Handler {

	private static final ConcurrentHashMap<String, String> clientToGate = new ConcurrentHashMap<>();

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.NotRegisterClient req = (ModelProto.NotRegisterClient) msg;
		addClientLink(req.getCert().toStringUtf8(), req.getGate().toStringUtf8());
		return true;
	}


	/**
	 * 客户端断线
	 */
	public static void clientDisconnect(String ip) {
		clientToGate.remove(ip);
	}

	/**
	 * 客户端链接
	 */
	public static void addClientLink(String ip, String gate) {
		clientToGate.put(ip, gate);
	}

	/**
	 * 获取gate链接
	 */
	public static String getLinkGate(String cert) {
		return clientToGate.get(cert);
	}
}
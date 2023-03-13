package hall.handel;

import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 登录请求
 */
public class ReqLoginHandler implements Handler<HallProto.ReqLogin> {

	private static ReqLoginHandler instance = new ReqLoginHandler();

	//游客登录
	private final static int VISIT = 1;

	//手机号登录
	private final static int PHONE = 2;

	//token登录
	private final static int TOKEN = 3;

	public static ReqLoginHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, HallProto.ReqLogin req) {
		String avatar = req.getAvatar().toStringUtf8();
		String cert = req.getCert().toStringUtf8();
		int certType = req.getCertType();
		switch (certType) {
			case VISIT:
				break;
			case PHONE:
				break;
			case TOKEN:
				break;
			default:
				break;
		}
		return true;
	}
}

package test.handel;

import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

/**
 * 登录回复
 */
public class AckLoginHandler implements Handler<HallProto.AckLogin> {

	private final static Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);

	private static AckLoginHandler instance = new AckLoginHandler();

	public static AckLoginHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, HallProto.AckLogin req, int mapId) {
		return true;
	}
}

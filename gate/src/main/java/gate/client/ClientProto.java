package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import net.client.Sender;
import net.connect.ConnectHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientProto {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientProto.class);

    public final static Parser PARSER = (id, msg) -> {
        switch (id) {

            default: {
                return null;
            }
        }
    };


    private final static Map<Integer, Handler> handlers;
    static {
        handlers = new HashMap<>();

    }
    public final static Handlers HANDLERS = handlers::get;


    public final static Transfer<GateClient, TCPMessage> TRANSFER = (gateClient, tcpMessage) -> {

        switch (tcpMessage.getMessageId()){
            default:
                break;
        }

        return false;
    };

    public static boolean transferMsg(long connectId, TCPMessage msg) {
        return transferMsg(connectId, msg, null);
    }

    public static boolean transferMsg(long connectId, TCPMessage msg, Message innerMsg) {
        Sender sender = ConnectHandler.getSender(connectId);
        if (null != sender) {
            if (null != innerMsg) {
                sender.sendMessage(replace(msg, innerMsg));
            } else {
                sender.sendMessage(msg);
            }

            return true;
        }

        LOGGER.error("ERROR! failed for transfer message(connect:{} message id:{})", connectId, msg.getMessageId());
        return false;
    }

    public static TCPMessage replace(TCPMessage tcpMessage, Message msg) {
        tcpMessage.setMessage(msg.toByteArray());
        return tcpMessage;
    }
}

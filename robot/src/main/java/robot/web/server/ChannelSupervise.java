package robot.web.server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ChannelSupervise {
	private static final ChannelGroup GlobalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private static final ConcurrentMap<String, ChannelId> ChannelMap = new ConcurrentHashMap<>();

	public static void addChannel(String apiToken, Channel channel) {
		GlobalGroup.add(channel);
		if (null != apiToken) {
			ChannelMap.put(apiToken, channel.id());
		}
	}

	public static void updateChannel(String apiToken, Channel channel) {
		Channel c = GlobalGroup.find(channel.id());
		if (null == c) {
			addChannel(apiToken, channel);
		} else {
			ChannelMap.put(apiToken, channel.id());
		}
	}

	public static void removeChannel(Channel channel) {
		GlobalGroup.remove(channel);
		Collection values = ChannelMap.values();
		values.remove(channel.id());
	}

	public static Channel findChannel(String apiToken) {
		ChannelId channelId = ChannelMap.get(apiToken);
		if (null == channelId) {
			return null;
		}

		return GlobalGroup.find(ChannelMap.get(apiToken));
	}

	public static void sendToAll(TextWebSocketFrame tws) {
		GlobalGroup.writeAndFlush(tws);
	}

	public static void sendToSimple(String apiToken, TextWebSocketFrame tws) {
		GlobalGroup.find(ChannelMap.get(apiToken)).writeAndFlush(tws);
	}
}
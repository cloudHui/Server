package lobby.client.handle.role;

import java.util.concurrent.ConcurrentHashMap;

import net.client.Sender;

/**
 * 创桌异步回包时，需要沿用「收到 REQ_JOIN 的那条 Gate 连接」回 ACK，
 * 否则 ACK 走另一条 Lobby→Gate 连接，Gate 侧找不到 completer，前端就会加入超时。
 */
public final class PendingCreateJoin {
	private static final ConcurrentHashMap<Integer, PendingCreateJoin> BY_USER = new ConcurrentHashMap<>();

	public final int sequence;
	public final Sender replyTo;

	private PendingCreateJoin(int sequence, Sender replyTo) {
		this.sequence = sequence;
		this.replyTo = replyTo;
	}

	public static void put(int userId, int sequence, Sender replyTo) {
		if (userId > 0 && replyTo != null) {
			BY_USER.put(userId, new PendingCreateJoin(sequence, replyTo));
		}
	}

	public static PendingCreateJoin take(int userId) {
		return BY_USER.remove(userId);
	}
}

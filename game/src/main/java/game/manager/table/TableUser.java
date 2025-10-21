package game.manager.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.cards.Card;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏用户模型
 * 代表一个游戏玩家的状态和信息
 */
public class TableUser {
	private static final Logger logger = LoggerFactory.getLogger(TableUser.class);

	private final Set<Long> tableIds = new HashSet<>();
	private int userId;
	private boolean online;
	private String head;
	private String nick;
	private final int gateId;
	private int seated = -1;
	private final List<Card> cards;
	//private long diamond;

	public TableUser(int userId, String head, String nick, int gateId) {
		this.userId = userId;
		this.head = head;
		this.nick = nick;
		this.gateId = gateId;
		online = true;
		cards = new ArrayList<>();
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
		logger.debug("设置用户ID: {}", userId);
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnLine(boolean online) {
		boolean changed = this.online != online;
		this.online = online;
		if (changed) {
			logger.debug("更新用户在线状态, userId: {}, online: {}", userId, online);
		}
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public int getGateId() {
		return gateId;
	}

	public int getSeated() {
		return seated;
	}

	public void setSeated(int seated) {
		this.seated = seated;
		logger.info("更新用户入座状态, userId: {}, old:{} seated: {}", userId, this.seated, seated);
	}

	/**
	 * 摸牌
	 */
	public void addCards(Card card) {
		cards.add(card);
	}

	public List<Card> getCards() {
		return cards;
	}

	/**
	 * 出牌
	 */
	public boolean outCards(Card card) {
		return cards.remove(card);
	}
	//public long getDiamond() {
	//	return diamond;
	//}
	//
	//public void setDiamond(long diamond) {
	//	long oldValue = this.diamond;
	//	this.diamond = diamond;
	//	if (oldValue != diamond) {
	//		logger.debug("更新用户钻石数量, userId: {}, old: {}, new: {}", userId, oldValue, diamond);
	//	}
	//}

	public Set<Long> getTableIds() {
		return new HashSet<>(tableIds); // 返回副本避免外部修改
	}

	/**
	 * 添加桌子ID到用户
	 */
	public void addTable(long tableId) {
		if (tableIds.add(tableId)) {
			logger.info("用户添加桌子, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 从用户移除桌子ID
	 */
	public void removeTable(long tableId) {
		if (tableIds.remove(tableId)) {
			logger.debug("用户移除桌子, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 检查用户是否在指定桌子中
	 */
	public boolean isInTable(String tableId) {
		return tableIds.contains(tableId);
	}

	/**
	 * 获取用户所在的桌子数量
	 */
	public int getTableCount() {
		return tableIds.size();
	}

	/**
	 * 清理用户所有桌子关联
	 */
	public void clearTables() {
		int count = tableIds.size();
		tableIds.clear();
		logger.debug("清理用户所有桌子关联, userId: {}, 数量: {}", userId, count);
	}

	/**
	 * 发送玩家消息
	 *
	 * @param message 消息
	 */
	public void sendRoleMessage(Message message, int messageId, long tableId) {
		ClientHandler serverClient = Game.getInstance().getServerClientManager().getServerClient(ServerType.Gate, gateId);

		if (serverClient == null) {
			logger.error("sendRole:{} Message error gate:{} null table:{}", userId, gateId, tableId);
			return;
		}
		serverClient.sendMessage(TCPMessage.newInstance(messageId, message.toByteArray()));
		logger.info("sendRole:{} Message success gate:{} null table:{}", userId, gateId, tableId);
	}

	@Override
	public String toString() {
		return "GameUser{" +
				"tableIds=" + tableIds +
				", userId=" + userId +
				", online=" + online +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TableUser tableUser = (TableUser) o;
		return Objects.equals(tableIds, tableUser.tableIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tableIds);
	}
}
package game.manager.table;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.banner.Banner;
import game.manager.table.card.poll.CardPool;
import game.manager.table.op.Operate;
import game.manager.table.state.TableStateHandleManager;
import model.TableModel;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import proto.ModelProto;

/**
 * 游戏桌子模型
 * 代表一个游戏桌子的状态和行为
 */
public class Table {
	private static final Logger logger = LoggerFactory.getLogger(Table.class);

	private final long tableId;

	private final TableModel tableModel;

	private final ModelProto.RoomRole creator;

	/**
	 * 桌子上的玩家(玩家id 和玩家数据)
	 */
	private final Map<Integer, TableUser> users = new HashMap<>();

	/**
	 * 桌子上的玩家(座位号和玩家id)
	 */
	private final Map<Integer, TableUser> seatUsers = new HashMap<>();

	/**
	 * 牌桌运行状态
	 */
	private TableState tableState = TableState.WAITING;

	/**
	 * 状态开始时间
	 */
	private long stateStartTime;

	private int errorTimes;
	/**
	 * 牌桌牌数据
	 */
	private final CardPool cardPool;

	/**
	 * 抢地主数据
	 */
	private final Banner banner;

	/**
	 * 操作数据
	 */
	private final Operate op;

	/**
	 * 最大错误次数
	 */
	private static final int MAX_ERROR = 100;

	public Table(long tableId, TableModel model, ModelProto.RoomRole creator) {
		this.tableId = tableId;
		this.creator = creator;
		this.tableModel = model;
		cardPool = new CardPool(this);
		op = new Operate(this);
		banner = new Banner();
		logger.info("创建桌子实例, tableId: {}", tableId);
	}

	public long getTableId() {
		return tableId;
	}

	public TableModel getTableModel() {
		return tableModel;
	}

	public ModelProto.RoomRole getCreator() {
		return creator;
	}

	public int getRoomId() {
		return tableModel.getId();
	}

	public Map<Integer, TableUser> getUsers() {
		return users;
	}

	public TableState getTableState() {
		return tableState;
	}

	public void upNextState(TableState next) {
		upNextStateWithTime(next, System.currentTimeMillis());
	}

	public void upNextState() {
		upNextStateWithTime(tableState.getNext(), System.currentTimeMillis());
	}

	public void upNextStateWithTime(TableState next, long now) {
		logger.info("table:{} change state old:{} new:{}", tableId, this.tableState, tableState);
		if (next == null) {
			next = tableState.getNext();
		}
		if (next == null) {
			logger.error("table:{} stat:{} update to nextState:null error ", tableId, tableState);
			return;
		}
		tableState = tableState.getNext();
		stateStartTime = now;

	}

	public long getStateStartTime() {
		return stateStartTime;
	}

	public void addErrorTime() {
		if (++errorTimes >= MAX_ERROR) {
			upNextState(TableState.TABLE_OVER);
		}
	}

	public TableUser getUser(int userId, int gateId, GameProto.ReqEnterTable req) {
		TableUser tableUser = users.get(userId);
		if (tableUser == null) {
			tableUser = new TableUser(userId, req.getHead().toStringUtf8(), req.getNick().toStringUtf8(), gateId);
			users.put(userId, tableUser);
		}
		return tableUser;
	}

	public Map<Integer, TableUser> getSeatUsers() {
		return seatUsers;
	}

	public TableUser getSeatUser(int seat){
		return seatUsers.get(seat);
	}

	public Banner getBanner() {
		return banner;
	}

	public Operate getOp() {
		return op;
	}

	/**
	 * 桌子是否坐满了
	 *
	 * @return 是否坐满
	 */
	public boolean sitFull() {
		return users.size() >= tableModel.getSeatNum();
	}

	/**
	 * 桌子空了
	 */
	public boolean isEmpty() {
		return users.isEmpty();
	}

	/**
	 * 开始游戏了
	 *
	 * @return 是否开始游戏
	 */
	public boolean gaming() {
		return tableState != TableState.WAITING && tableState != TableState.TABLE_OVER;
	}

	/**
	 * 启动桌子逻辑循环
	 */
	public void start() {
		try {
			int groupIndex = getGroupIndex();
			Game.getInstance().registerSerialTimer(groupIndex, 1000, 500, -1, this::tableLoop, this);
			logger.info("启动桌子逻辑循环, tableId: {}, groupIndex: {}", tableId, groupIndex);
		} catch (Exception e) {
			logger.error("启动桌子逻辑循环失败, tableId: {}", tableId, e);
		}
	}

	/**
	 * 获取线程组处理ID
	 * 根据桌子ID的最后一位数字确定线程组,实现负载均衡
	 */
	public int getGroupIndex() {
		try {
			int groupIndex = (int) (tableId % 10);
			logger.debug("计算线程组索引, tableId: {}, groupIndex: {}", tableId, groupIndex);
			return groupIndex;
		} catch (Exception e) {
			logger.error("计算线程组索引失败, tableId: {}", tableId, e);
			return 0;
		}
	}

	/**
	 * 桌子主循环
	 * 处理桌子的游戏逻辑更新
	 *
	 * @param table 桌子实例
	 * @return 是否需要结束任务循环 true-结束 false-继续
	 */
	public boolean tableLoop(Table table) {
		try {
			return TableStateHandleManager.handle(this);
		} catch (Exception e) {
			logger.error("桌子循环执行异常, tableId: {}", tableId, e);
			return false;
		}
	}

	/**
	 * 给玩家发牌
	 */
	public void sendCard() {
		cardPool.initCards();
		cardPool.dealInitCard();
	}

	/**
	 * 添加玩家到桌子
	 *
	 * @param user 玩家对象
	 * @return 是否入桌成功
	 */
	public int addUser(TableUser user) {
		try {
			if (user == null) {
				logger.warn("尝试添加空玩家到桌子, tableId: {}", tableId);
				return ConstProto.Result.ROLE_NULL_VALUE;
			}

			if (sitFull()) {
				logger.warn("桌子满了, tableId: {}", tableId);
				return ConstProto.Result.TABLE_FULL_VALUE;
			}
			if (isEmpty()) {
				start();
			}
			int seat = occupySeat(user);
			logger.info("玩家加入桌子, userId: {}, tableId: {} seat:{}", user.getUserId(), tableId, seat);
			return seat == -1 ? ConstProto.Result.TABLE_FULL_VALUE : ConstProto.Result.SUCCESS_VALUE;
		} catch (Exception e) {
			logger.error("添加玩家到桌子失败, userId: {}, tableId: {}", user != null ? user.getUserId() : "null", tableId, e);
			return ConstProto.Result.ROLE_ERROR_VALUE;
		}
	}

	/**
	 * 占座
	 *
	 * @param user 玩家
	 * @return 坐下的位置
	 */
	private int occupySeat(TableUser user) {
		for (int index = 0; index < tableModel.getSeatNum(); index++) {
			if (!seatUsers.containsKey(index)) {
				seatUsers.put(index, user);
				user.setSeated(index);
				return index;
			}
		}
		return -1;
	}

	/**
	 * 从桌子移除玩家
	 */
	public boolean removeUser(TableUser user) {
		try {
			if (user == null) {
				logger.warn("尝试从桌子移除空玩家, tableId: {}", tableId);
				return false;
			}

			// TODO: 实现具体的移除逻辑
			user.removeTable(tableId);

			logger.info("玩家离开桌子, userId: {}, tableId: {}", user.getUserId(), tableId);
			return true;
		} catch (Exception e) {
			logger.error("从桌子移除玩家失败, userId: {}, tableId: {}",
					user != null ? user.getUserId() : "null", tableId, e);
			return false;
		}
	}

	private GameProto.NotTableState builderTableState() {
		return GameProto.NotTableState.newBuilder()
				.setState(tableState.getId())
				.setStateStart(stateStartTime)
				.setStateDuration(tableState.getOverTime())
				.build();
	}

	/**
	 * 发送桌子消息
	 *
	 * @param message 消息
	 */
	public void sendTableMessage(Message message, int messageId) {
		for (Map.Entry<Integer, TableUser> entry : seatUsers.entrySet()) {
			entry.getValue().sendRoleMessage(message, messageId, tableId);
		}
		logger.info("sendTableMessage:{} message:{}", tableId, message.toString());
	}

	@Override
	public String toString() {
		return String.format("Table{tableId='%d'}", tableId);
	}
}
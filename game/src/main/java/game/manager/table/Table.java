package game.manager.table;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.banner.Banner;
import game.manager.table.card.mj.MjTilePool;
import game.manager.table.card.poll.CardPool;
import game.manager.table.mj.MjTableContext;
import game.manager.table.op.Operate;
import game.manager.table.state.TableStateHandleManager;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import proto.ModelProto;
import utils.metrics.MetricsCollector;
import utils.trace.TraceContext;

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
	 * 斗地主牌局上下文（叫地主、出牌）
	 */
	private final game.manager.table.ddz.DdzTableContext ddz = new game.manager.table.ddz.DdzTableContext();

	/**
	 * 麻将牌墙
	 */
	private final MjTilePool mjTilePool;

	/**
	 * 麻将牌局上下文
	 */
	private final MjTableContext mjContext = new MjTableContext();

	/**
	 * 最大错误次数
	 */
	private static final int MAX_ERROR = 100;

	/**
	 * 游戏中循环间隔(毫秒)
	 */
	private static final long LOOP_INTERVAL = 500;

	/**
	 * 空闲循环间隔(毫秒) - 等待玩家/结束时使用，减少无用tick
	 */
	private static final long IDLE_LOOP_INTERVAL = 2000;

	/**
	 * 当前定时器节点ID（用于动态替换间隔）
	 */
	private int timerNodeId = -1;

	/**
	 * 当前循环间隔
	 */
	private long currentLoopInterval = IDLE_LOOP_INTERVAL;

	public Table(long tableId, TableModel model, ModelProto.RoomRole creator) {
		this.tableId = tableId;
		this.creator = creator;
		this.tableModel = model;
		cardPool = new CardPool(this);
		mjTilePool = new MjTilePool(this);
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
		if (next == null) {
			next = tableState.getNext();
		}
		if (next == null) {
			logger.error("table:{} stat:{} update to nextState:null error ", tableId, tableState);
			return;
		}
		logger.info("table:{} change state old:{} new:{}", tableId, this.tableState, next);
		tableState = next;
		stateStartTime = now;

		// 根据状态自动调整循环间隔
		adjustLoopInterval(next);
	}

	/**
	 * 根据桌子状态调整循环间隔
	 * 游戏进行中 → 500ms（快速响应玩家操作）
	 * 等待/结束 → 2000ms（减少无用tick）
	 */
	private void adjustLoopInterval(TableState state) {
		if (state == TableState.TABLE_DIS) {
			// 解散状态不需要定时器
			return;
		}
		if (state == TableState.WAITING || state == TableState.ROUND_OVER
				|| state == TableState.TABLE_OVER) {
			setLoopInterval(IDLE_LOOP_INTERVAL);
		} else {
			setLoopInterval(LOOP_INTERVAL);
		}
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

	public TableUser getSeatUser(int seat) {
		return seatUsers.get(seat);
	}

	public int nextSeat(int seat) {
		return ++seat >= tableModel.getSeatNum() ? 0 : seat;
	}

	public TableUser getNextSeatUser(int seat) {
		return seatUsers.get(nextSeat(seat));
	}

	public Banner getBanner() {
		return banner;
	}

	public Operate getOp() {
		return op;
	}

	public game.manager.table.ddz.DdzTableContext getDdz() {
		return ddz;
	}

	public MjTilePool getMjTilePool() {
		return mjTilePool;
	}

	public MjTableContext getMjContext() {
		return mjContext;
	}

	public CardPool getCardPool() {
		return cardPool;
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
		return tableState != TableState.WAITING
				&& tableState != TableState.TABLE_OVER
				&& tableState != TableState.TABLE_DIS;
	}

	/**
	 * 启动桌子逻辑循环
	 */
	public void start() {
		try {
			int groupIndex = getGroupIndex();
			timerNodeId = Game.getInstance().registerSerialTimerWithId(
					groupIndex, 1000, IDLE_LOOP_INTERVAL, -1, this::tableLoop, this);
			currentLoopInterval = IDLE_LOOP_INTERVAL;
			logger.info("启动桌子逻辑循环, tableId: {}, groupIndex: {}, interval: {}ms",
					tableId, groupIndex, IDLE_LOOP_INTERVAL);
		} catch (Exception e) {
			logger.error("启动桌子逻辑循环失败, tableId: {}", tableId, e);
		}
	}

	/**
	 * 动态调整循环间隔
	 * 游戏中用500ms快速响应，空闲时用200ms减少tick量
	 */
	public void setLoopInterval(long intervalMs) {
		if (currentLoopInterval == intervalMs) {
			return;
		}
		try {
			int groupIndex = getGroupIndex();
			// 注销旧定时器
			if (timerNodeId > 0) {
				Game.getInstance().unregisterTimer(timerNodeId);
			}
			// 注册新定时器
			timerNodeId = Game.getInstance().registerSerialTimerWithId(
					groupIndex, 0, intervalMs, -1, this::tableLoop, this);
			currentLoopInterval = intervalMs;
			logger.debug("调整循环间隔, tableId: {}, interval: {}ms", tableId, intervalMs);
		} catch (Exception e) {
			logger.error("调整循环间隔失败, tableId: {}", tableId, e);
		}
	}

	/**
	 * 获取线程组处理ID
	 * 使用桌子ID作为分组Key，由ExecutorPool自动分配到对应线程
	 * 同一张桌子的所有操作（定时器、玩家进入、玩家操作）始终在同一线程串行执行
	 */
	public int getGroupIndex() {
		return (int) tableId;
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
			TraceContext.setTableId(tableId);
			MetricsCollector.getInstance().incrementCounter("game.table_loops");
			return TableStateHandleManager.handle(this);
		} catch (Exception e) {
			logger.error("桌子循环执行异常, tableId: {}", tableId, e);
			return false;
		}
	}

	/**
	 * 给玩家发牌(斗地主)
	 */
	public void sendCard() {
		cardPool.dealInitCard();
	}

	/**
	 * 给玩家发麻将牌
	 */
	public void sendMjCard() {
		mjTilePool.dealInitTiles();
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

			int prevSeat = user.getSeated();
			if (prevSeat >= 0 && seatUsers.get(prevSeat) == user) {
				return ConstProto.Result.SUCCESS_VALUE;
			}
			if (prevSeat >= 0) {
				user.setSeated(-1);
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

			users.remove(user.getUserId());
			int seat = user.getSeated();
			if (seat >= 0) {
				seatUsers.remove(seat);
			}
			user.removeTable(tableId);
			user.setSeated(-1);

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

	public void sendTableMessageRaw(int messageId, byte[] payload) {
		for (Map.Entry<Integer, TableUser> entry : seatUsers.entrySet()) {
			entry.getValue().sendRoleMessageBytes(messageId, payload, tableId);
		}
		logger.info("sendTableMessageRaw:{} bytes:{}", tableId, payload != null ? payload.length : 0);
	}

	@Override
	public String toString() {
		return String.format("Table{tableId='%d'}", tableId);
	}
}
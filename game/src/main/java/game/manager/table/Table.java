package game.manager.table;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.op.Operate;
import game.manager.table.replay.ReplayRecorder;
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
 * 游戏桌子基类
 * 包含所有玩法共用的状态、玩家管理、循环控制
 * 具体玩法通过子类(MjTable/DdzTable)实现
 */
public abstract class Table {
	private static final Logger logger = LoggerFactory.getLogger(Table.class);

	private final long tableId;
	private final TableModel tableModel;
	private final ModelProto.RoomRole creator;

	private final Map<Integer, TableUser> users = new ConcurrentHashMap<>();
	private final Map<Integer, TableUser> seatUsers = new ConcurrentHashMap<>();

	private TableState tableState = TableState.WAITING;
	private long stateStartTime;
	private int errorTimes;

	private final Operate op;
	private final Set<Integer> readySet = ConcurrentHashMap.newKeySet();
	private int currentRound = 1;
	private GameResult gameResult;
	private ReplayRecorder replayRecorder;

	private static final int MAX_ERROR = 100;
	private static final long LOOP_INTERVAL = 500;
	private static final long IDLE_LOOP_INTERVAL = 2000;
	private static final AtomicInteger ROBOT_ID_SEQ = new AtomicInteger(-100000);
	private int timerNodeId = -1;
	private long currentLoopInterval = IDLE_LOOP_INTERVAL;
	/** 本桌运行时托管（补机器人后开启，勿改共享 TableModel） */
	private boolean runtimeAutoPlay;

	protected Table(long tableId, TableModel model, ModelProto.RoomRole creator) {
		this.tableId = tableId;
		this.creator = creator;
		this.tableModel = model;
		this.op = new Operate(this);
		this.gameResult = createGameResult();
		this.stateStartTime = System.currentTimeMillis();
		logger.info("创建桌子实例, tableId: {}, type: {}", tableId, model.getType());
	}

	public int getOwnerId() { return creator != null ? creator.getRoleId() : 0; }

	// ======================== 抽象方法(子类实现) ========================

	/** 玩法类型: 1=麻将, 2=斗地主 */
	public abstract int getGameType();

	/** 发初始牌 */
	public abstract void dealCards();

	/** 重置游戏上下文(下一局时调用) */
	public abstract void resetGameContext();

	/** 处理玩家操作(多态分发) */
	public abstract int processOp(int userId, GameProto.OpInfo op, net.client.Sender sender, long mapId, int sequence);

	/** 创建玩法专属的GameResult */
	public abstract GameResult createGameResult();

	/** 同步游戏状态给重连玩家 */
	public abstract void syncGameState(TableUser user);

	/** 初始化游戏配置(发牌前调用) */
	public abstract void initGameConfig();

	// ======================== 通用方法 ========================

	public long getTableId() { return tableId; }
	public TableModel getTableModel() { return tableModel; }
	public ModelProto.RoomRole getCreator() { return creator; }
	public int getRoomId() { return tableModel.getId(); }
	public Map<Integer, TableUser> getUsers() { return users; }
	public TableState getTableState() { return tableState; }
	public long getStateStartTime() { return stateStartTime; }
	public Operate getOp() { return op; }
	public Map<Integer, TableUser> getSeatUsers() { return seatUsers; }

	public TableUser getSeatUser(int seat) { return seatUsers.get(seat); }
	public int nextSeat(int seat) { return ++seat >= tableModel.getSeatNum() ? 0 : seat; }
	public TableUser getNextSeatUser(int seat) { return seatUsers.get(nextSeat(seat)); }

	public boolean sitFull() { return seatUsers.size() >= tableModel.getSeatNum(); }
	public boolean isEmpty() { return users.isEmpty(); }
	public boolean gaming() {
		return tableState != TableState.WAITING && tableState != TableState.TABLE_OVER
				&& tableState != TableState.TABLE_DIS;
	}

	public boolean hasHumanPlayer() {
		for (TableUser u : users.values()) {
			if (!u.isRobot()) return true;
		}
		return false;
	}

	public boolean isAllRobot() {
		if (users.isEmpty()) return false;
		for (TableUser u : users.values()) {
			if (!u.isRobot()) return false;
		}
		return true;
	}

	public boolean isAutoPlayEnabled() {
		return runtimeAutoPlay || tableModel.getAutoPlay() != 0;
	}

	/** 补齐空位为机器人；成功补位后打开本桌托管以便超时代打 */
	public int fillRobotSeats() {
		int added = 0;
		while (!sitFull()) {
			int botId = ROBOT_ID_SEQ.decrementAndGet();
			if (botId >= 0) {
				ROBOT_ID_SEQ.set(-100000);
				botId = ROBOT_ID_SEQ.decrementAndGet();
			}
			TableUser bot = new TableUser(botId, "", "机器人" + Math.abs(botId % 1000), 0);
			bot.setRobot(true);
			int result = addUser(bot);
			if (result != ConstProto.Result.SUCCESS_VALUE) {
				logger.warn("补机器人失败, tableId: {}, result: {}", tableId, result);
				break;
			}
			added++;
		}
		if (added > 0) {
			runtimeAutoPlay = true;
			logger.info("桌子补机器人完成, tableId: {}, added: {}, seats: {}/{}",
					tableId, added, seatUsers.size(), tableModel.getSeatNum());
			notifySeatPlayers();
		}
		return added;
	}

	/** 推送当前座位名单（补机器人后刷新前端显示） */
	public void notifySeatPlayers() {
		GameProto.AckEnterTable.Builder response = GameProto.AckEnterTable.newBuilder();
		response.setTableInfo(GameProto.TableInfo.newBuilder()
				.setTableId(tableId).setRoomId(getRoomId()).build());
		for (TableUser tableUser : users.values()) {
			response.addPlayers(GameProto.Player.newBuilder()
					.setPosition(tableUser.getSeated())
					.setRoleId(tableUser.getUserId())
					.setNickName(com.google.protobuf.ByteString.copyFromUtf8(
							tableUser.getNick() == null ? "" : tableUser.getNick()))
					.setAvatar(com.google.protobuf.ByteString.copyFromUtf8(
							tableUser.getHead() == null ? "" : tableUser.getHead()))
					.build());
		}
		sendTableMessage(response.build(), msg.registor.message.GMsg.ACK_ENTER_TABLE_MSG);
	}

	// ======================== 状态转换 ========================

	public void upNextState(TableState next) { upNextStateWithTime(next, System.currentTimeMillis()); }
	public void upNextState() { upNextStateWithTime(tableState.getNext(), System.currentTimeMillis()); }

	public void upNextStateWithTime(TableState next, long now) {
		if (next == null) next = tableState.getNext();
		if (next == null) {
			logger.error("table:{} stat:{} update to nextState:null error", tableId, tableState);
			return;
		}
		logger.info("table:{} change state old:{} new:{}", tableId, this.tableState, next);
		tableState = next;
		stateStartTime = now;
		adjustLoopInterval(next);
	}

	private void adjustLoopInterval(TableState state) {
		if (state == TableState.TABLE_DIS) return;
		if (state == TableState.WAITING || state == TableState.ROUND_OVER || state == TableState.TABLE_OVER) {
			setLoopInterval(IDLE_LOOP_INTERVAL);
		} else {
			setLoopInterval(LOOP_INTERVAL);
		}
	}

	public void addErrorTime() {
		if (++errorTimes >= MAX_ERROR) upNextState(TableState.TABLE_OVER);
	}

	// ======================== 定时器 ========================

	public void start() {
		try {
			if (timerNodeId > 0) return;
			int groupIndex = getGroupIndex();
			timerNodeId = Game.getInstance().registerSerialTimerWithId(
					groupIndex, 1000, IDLE_LOOP_INTERVAL, -1, this::tableLoop, this);
			currentLoopInterval = IDLE_LOOP_INTERVAL;
			logger.info("启动桌子逻辑循环, tableId: {}, groupIndex: {}", tableId, groupIndex);
		} catch (Exception e) {
			logger.error("启动桌子逻辑循环失败, tableId: {}", tableId, e);
		}
	}

	/** 停止桌子循环，避免删桌后仍触发 Waiting */
	public void stop() {
		try {
			if (timerNodeId > 0) {
				Game.getInstance().unregisterTimer(timerNodeId);
				timerNodeId = -1;
				logger.info("停止桌子逻辑循环, tableId: {}", tableId);
			}
		} catch (Exception e) {
			logger.error("停止桌子逻辑循环失败, tableId: {}", tableId, e);
		}
	}

	public void setLoopInterval(long intervalMs) {
		if (currentLoopInterval == intervalMs) return;
		try {
			int groupIndex = getGroupIndex();
			if (timerNodeId > 0) Game.getInstance().unregisterTimer(timerNodeId);
			timerNodeId = Game.getInstance().registerSerialTimerWithId(
					groupIndex, 0, intervalMs, -1, this::tableLoop, this);
			currentLoopInterval = intervalMs;
		} catch (Exception e) {
			logger.error("调整循环间隔失败, tableId: {}", tableId, e);
		}
	}

	/** 获取定时器分组索引，防溢出 */
	public int getGroupIndex() { return (int) (tableId % Integer.MAX_VALUE); }

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

	// ======================== 玩家管理 ========================

	public TableUser getUser(int userId, int gateId, GameProto.ReqEnterTable req) {
		TableUser tableUser = users.get(userId);
		if (tableUser == null) {
			// 先不写入 users：避免 addUser 前 sitFull 被预占导致第三人 TABLE_FULL
			tableUser = new TableUser(userId, req.getHead().toStringUtf8(), req.getNick().toStringUtf8(), gateId);
		}
		return tableUser;
	}

	public int addUser(TableUser user) {
		try {
			if (user == null) return ConstProto.Result.ROLE_NULL_VALUE;
			int prevSeat = user.getSeated();
			if (prevSeat >= 0 && seatUsers.get(prevSeat) == user) {
				users.put(user.getUserId(), user);
				return ConstProto.Result.SUCCESS_VALUE;
			}
			if (prevSeat >= 0) user.setSeated(-1);
			if (sitFull()) return ConstProto.Result.TABLE_FULL_VALUE;
			if (isEmpty()) start();
			int seat = occupySeat(user);
			if (seat == -1) return ConstProto.Result.TABLE_FULL_VALUE;
			users.put(user.getUserId(), user);
			logger.info("玩家加入桌子, userId: {}, tableId: {} seat:{}", user.getUserId(), tableId, seat);
			return ConstProto.Result.SUCCESS_VALUE;
		} catch (Exception e) {
			logger.error("添加玩家到桌子失败, userId: {}, tableId: {}", user != null ? user.getUserId() : "null", tableId, e);
			return ConstProto.Result.ROLE_ERROR_VALUE;
		}
	}

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

	public boolean removeUser(TableUser user) {
		try {
			if (user == null) return false;
			users.remove(user.getUserId());
			int seat = user.getSeated();
			if (seat >= 0) seatUsers.remove(seat);
			user.removeTable(tableId);
			user.setSeated(-1);
			logger.info("玩家离开桌子, userId: {}, tableId: {}", user.getUserId(), tableId);
			return true;
		} catch (Exception e) {
			logger.error("从桌子移除玩家失败, userId: {}, tableId: {}", user != null ? user.getUserId() : "null", tableId, e);
			return false;
		}
	}

	// ======================== 消息发送 ========================

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
	}

	// ======================== 多局 ========================

	public void clearReadySet() { readySet.clear(); }
	public void addReady(int userId) { readySet.add(userId); }
	public boolean allReady() { return readySet.size() >= tableModel.getSeatNum(); }
	public int getReadyCount() { return readySet.size(); }

	public void resetForNextRound() {
		readySet.clear();
		currentRound++;
		op.reset();
		for (TableUser user : seatUsers.values()) user.getCards().clear();
		resetGameContext(); // 子类实现: 重置MJ/DDZ上下文
		tableState = TableState.WAITING;
		stateStartTime = System.currentTimeMillis();
		logger.info("牌桌重置准备下一局, tableId: {}, round: {}", tableId, currentRound);
	}

	public int getCurrentRound() { return currentRound; }
	public GameResult getGameResult() { return gameResult; }
	public boolean isLastRound() { return currentRound >= tableModel.getTotalRounds(); }
	public boolean isMultiRound() { return tableModel.getTotalRounds() > 1; }

	public ReplayRecorder getReplayRecorder() { return replayRecorder; }
	public void setReplayRecorder(ReplayRecorder replayRecorder) { this.replayRecorder = replayRecorder; }
	public abstract ReplayRecorder createReplayRecorder();

	@Override
	public String toString() { return String.format("Table{tableId='%d', type=%d}", tableId, getGameType()); }
}

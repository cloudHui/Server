package game.manager.table;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.op.Operate;
import game.manager.table.replay.ReplayRecorder;
import game.manager.table.state.TableStateHandleManager;
import model.tablemodel.TableModel;
import model.tablemodel.RobotRoomTemplates;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import proto.ModelProto;
import utils.metrics.MetricsCollector;
import utils.trace.TraceContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

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
    private final GameResult gameResult;
    private ReplayRecorder replayRecorder;

    private static final int MAX_ERROR = 100;
    private static final long LOOP_INTERVAL = 500;
    private static final long IDLE_LOOP_INTERVAL = 2000;
    private static final AtomicInteger ROBOT_ID_SEQ = new AtomicInteger(-100000);
    private ScheduledFuture<?> loopFuture;
    private long currentLoopInterval = IDLE_LOOP_INTERVAL;
    /**
     * 本桌运行时托管（补机器人后开启，勿改共享 TableModel）
     */
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

    public int getOwnerId() {
        return creator != null ? creator.getRoleId() : 0;
    }

    // ======================== 抽象方法(子类实现) ========================

    /**
     * 玩法类型: 1=麻将, 2=斗地主
     */
    public abstract int getGameType();

    /**
     * 发初始牌
     */
    public abstract void dealCards();

    /**
     * 重置游戏上下文(下一局时调用)
     */
    public abstract void resetGameContext();

    /**
     * 处理玩家操作(多态分发)
     */
    public abstract int processOp(int userId, GameProto.OpInfo op, net.client.Sender sender, long mapId, int sequence);

    /**
     * 创建玩法专属的GameResult
     */
    public abstract GameResult createGameResult();

    /**
     * 同步游戏状态给重连玩家
     */
    public abstract void syncGameState(TableUser user);

    /**
     * 初始化游戏配置(发牌前调用)
     */
    public abstract void initGameConfig();

    // ======================== 通用方法 ========================

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

    public long getStateStartTime() {
        return stateStartTime;
    }

    public Operate getOp() {
        return op;
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

    public boolean sitFull() {
        return seatUsers.size() >= tableModel.getSeatNum();
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

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

    /**
     * 补齐空位为机器人；成功补位后打开本桌托管以便超时代打
     */
    public int fillRobotSeats() {
        int added = 0;
        while (!sitFull()) {
            int botId = ROBOT_ID_SEQ.decrementAndGet();
            if (botId >= 0) {
                ROBOT_ID_SEQ.set(-100000);
                botId = ROBOT_ID_SEQ.decrementAndGet();
            }
			TableUser bot = new TableUser(botId, "", randomBotName(), 0);
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

	/** 机器人昵称只使用随机英文字母，固定十位且不暴露机器人身份。 */
	private static String randomBotName() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		StringBuilder name = new StringBuilder(10);
		for (int i = 0; i < 10; i++) {
			name.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
		}
		return name.toString();
	}

	/** 机器人模板允许全机器人桌开局；普通桌仍保持原有保护逻辑。 */
	public boolean isRobotRoom() {
		return RobotRoomTemplates.isRobotRoom(getRoomId());
	}

    /**
     * 推送当前座位名单（补机器人后刷新前端显示）
     */
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

    public void upNextState(TableState next) {
        upNextStateWithTime(next, System.currentTimeMillis());
    }

    public void upNextState() {
        upNextStateWithTime(tableState.getNext(), System.currentTimeMillis());
    }

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

    /** 将桌子内状态任务投递到桌串行队列（物理线程可与其他桌共享）。 */
    public CompletableFuture<Void> execute(Runnable task) {
        return Game.getInstance().getTableExecutorManager().submit(tableId, task);
    }

    public void start() {
        try {
            if (loopFuture != null && !loopFuture.isCancelled()) return;
            loopFuture = Game.getInstance().getTableExecutorManager().schedule(
                    tableId, () -> tableLoop(this), 1000, IDLE_LOOP_INTERVAL);
            currentLoopInterval = IDLE_LOOP_INTERVAL;
            logger.info("启动桌子逻辑循环, tableId: {}", tableId);
        } catch (Exception e) {
            logger.error("启动桌子逻辑循环失败, tableId: {}", tableId, e);
        }
    }

    /**
     * 停止桌子循环，避免删桌后仍触发 Waiting
     */
    public void stop() {
        try {
            if (loopFuture != null) {
                Game.getInstance().getTableExecutorManager().cancel(loopFuture);
                loopFuture = null;
                logger.info("停止桌子逻辑循环, tableId: {}", tableId);
            }
        } catch (Exception e) {
            logger.error("停止桌子逻辑循环失败, tableId: {}", tableId, e);
        }
    }

    public void setLoopInterval(long intervalMs) {
        if (currentLoopInterval == intervalMs) return;
        try {
            if (loopFuture != null) Game.getInstance().getTableExecutorManager().cancel(loopFuture);
            loopFuture = Game.getInstance().getTableExecutorManager().schedule(
                    tableId, () -> tableLoop(this), 0, intervalMs);
            currentLoopInterval = intervalMs;
        } catch (Exception e) {
            logger.error("调整循环间隔失败, tableId: {}", tableId, e);
        }
    }

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

    /** 在本桌线程生成大厅需要的只读快照，避免并发读取座位状态。 */
    public CompletableFuture<ModelProto.RoomTableInfo> getRoomTableInfoAsync() {
        CompletableFuture<ModelProto.RoomTableInfo> result = new CompletableFuture<>();
        execute(() -> result.complete(buildRoomTableInfo())).exceptionally(error -> {
            result.completeExceptionally(error);
            return null;
        });
        return result;
    }

    private ModelProto.RoomTableInfo buildRoomTableInfo() {
        ModelProto.RoomTableInfo.Builder builder = ModelProto.RoomTableInfo.newBuilder()
                .setTableId(tableId).setRoomId(getRoomId()).setOwnerId(getOwnerId())
                .setCreatorId(getOwnerId()).setGameType(getGameType());
        for (TableUser user : seatUsers.values()) {
            if (user == null) continue;
            builder.addTableRoles(ModelProto.RoomRole.newBuilder().setRoleId(user.getUserId())
                    .setNickName(com.google.protobuf.ByteString.copyFromUtf8(user.getNick())).build());
        }
        return builder.build();
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
			// 机器人模板的体验是进入即开局：第一个真人入座后立即补齐其余席位。
			// 递归补位时跳过机器人自身，避免重复触发。
			if (!user.isRobot() && isRobotRoom()) fillRobotSeats();
			return ConstProto.Result.SUCCESS_VALUE;
        } catch (Exception e) {
            logger.error("添加玩家到桌子失败, userId: {}, tableId: {}", user.getUserId(), tableId, e);
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

    public void removeUser(TableUser user) {
        try {
            if (user == null) return;
            users.remove(user.getUserId());
            int seat = user.getSeated();
            if (seat >= 0) seatUsers.remove(seat);
            user.removeTable(tableId);
            user.setSeated(-1);
            logger.info("玩家离开桌子, userId: {}, tableId: {}", user.getUserId(), tableId);
        } catch (Exception e) {
            logger.error("从桌子移除玩家失败, userId: {}, tableId: {}", user.getUserId(), tableId, e);
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

    public void clearReadySet() {
        readySet.clear();
    }

    public void addReady(int userId) {
        readySet.add(userId);
    }

    public boolean allReady() {
        return readySet.size() >= tableModel.getSeatNum();
    }

    public int getReadyCount() {
        return readySet.size();
    }

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

    public int getCurrentRound() {
        return currentRound;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public boolean isLastRound() {
        return currentRound >= tableModel.getTotalRounds();
    }

    public boolean isMultiRound() {
        return tableModel.getTotalRounds() > 1;
    }

    public ReplayRecorder getReplayRecorder() {
        return replayRecorder;
    }

    public void setReplayRecorder(ReplayRecorder replayRecorder) {
        this.replayRecorder = replayRecorder;
    }

    public abstract ReplayRecorder createReplayRecorder();

    @Override
    public String toString() {
        return String.format("Table{tableId='%d', type=%d}", tableId, getGameType());
    }
}

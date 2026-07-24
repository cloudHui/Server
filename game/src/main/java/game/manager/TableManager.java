package game.manager;

import game.Game;
import game.manager.table.DdzTable;
import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.thread.TableManagerExecutor;
import model.tablemodel.TableModel;
import model.tablemodel.TableModelJson;
import model.tablemodel.RobotRoomTemplates;
import msg.registor.enums.ServerType;
import msg.registor.message.SMsg;
import net.client.handler.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import proto.GameProto;
import tool.config.TableConfigManager;
import msg.registor.message.GMsg;
import utils.metrics.MetricsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;

/**
 * 桌子管理器
 * 负责游戏桌子的创建、管理和生命周期控制
 */
public class TableManager {
    private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

    private final Map<Long, Table> tableMap;
    /**
     * 桌号：时间戳毫秒 + 序号，避免短时间撞号
     */
    private final AtomicLong tableIdSeq = new AtomicLong(System.currentTimeMillis());

    private final TableConfigManager configManager;
    private final TableManagerExecutor managerExecutor;

    public TableManager() {
        managerExecutor = new TableManagerExecutor(Game.getInstance().getThreadPoolManager().tableManagerPool().getExecutorService());
        tableMap = new ConcurrentHashMap<>();
        configManager = new TableConfigManager();
        if (configManager.loadFail()) {
            throw new RuntimeException("加载配置文件失败");
        }
		// 与大厅注册同一组固定模板，机器人桌无需依赖外部配置文件。
		RobotRoomTemplates.register(configManager::putRuntimeModel);
        configManager.startWatch();
        logger.info("桌子管理器初始化完成");
    }

    /**
     * 添加桌子
     */
    private void addTable(Table table) {
        if (table == null) {
            logger.warn("尝试添加空桌子");
            return;
        }

        long tableId = table.getTableId();
        Table existingTable = tableMap.get(tableId);

        if (existingTable != null) {
            logger.warn("桌子已存在,添加失败, tableId: {}", tableId);
        } else {
            tableMap.put(tableId, table);
            Game.getInstance().getTableExecutorManager().register(tableId);
            MetricsCollector.getInstance().setGauge("game.active_tables", tableMap.size());
            MetricsCollector.getInstance().incrementCounter("game.tables_created");
            logger.debug("添加新桌子, tableId: {}", tableId);
        }
    }

    /**
     * 获取桌子
     */
    public Table getTable(long tableId) {
        Table table = tableMap.get(tableId);
        if (table == null) {
            logger.debug("桌子不存在, tableId: {}", tableId);
        }
        return table;
    }

    /**
     * 删除桌子
     */
    private void removeTable(long tableId) {
        Table removedTable = tableMap.remove(tableId);
        if (removedTable != null) {
            notifyPlayersTableDestroyed(removedTable);
            removedTable.stop();
            Game.getInstance().getTableExecutorManager().remove(tableId);
            MetricsCollector.getInstance().setGauge("game.active_tables", tableMap.size());
            MetricsCollector.getInstance().incrementCounter("game.tables_destroyed");
            logger.info("删除桌子, tableId: {}", tableId);
            notifyRoomTableDestroyed(tableId);
        } else {
            logger.warn("桌子不存在,无法删除, tableId: {}", tableId);
        }
    }

    /** 通知仍在桌内的客户端桌子已解散，客户端收到后应清理桌面并返回大厅。 */
    private void notifyPlayersTableDestroyed(Table table) {
        GameProto.NotTableState notification = GameProto.NotTableState.newBuilder()
                .setState(msg.registor.enums.TableState.TABLE_DIS.getId())
                .setStateStart(System.currentTimeMillis())
                .setStateDuration(0)
                .build();
        for (TableUser user : table.getSeatUsers().values()) {
            try {
                user.sendRoleMessage(notification, GMsg.NOT_TABLE_STATE, table.getTableId());
            } catch (Exception e) {
                logger.warn("通知玩家桌子解散失败, tableId: {}, userId: {}", table.getTableId(), user.getUserId(), e);
            }
        }
    }

    /** 在桌子管理线程创建桌子，网络线程只接收 Future。 */
    public CompletableFuture<Table> createTableAsync(int roomId, ModelProto.RoomRole role) {
        return managerExecutor.submit(() -> createTable(roomId, role));
    }

    /**
     * 在桌子管理线程删除桌子，桌子线程不直接修改全局索引。
     */
    public void removeTableAsync(long tableId) {
        managerExecutor.submit(() -> {
            removeTable(tableId);
            return null;
        });
    }

    public CompletableFuture<List<Table>> findTablesByUserIdAsync(int userId) {
        return managerExecutor.submit(() -> findTablesByUserId(userId));
    }

    public CompletableFuture<List<ModelProto.RoomTableInfo>> getAllTableInfoAsync() {
        return managerExecutor.submit(this::snapshotTables).thenCompose(this::collectTableInfo);
    }

    /** 关闭桌子管理线程，供服务停止时释放资源。 */
    public void shutdown() {
        managerExecutor.shutdown();
    }

    private List<Table> snapshotTables() {
        return new ArrayList<>(tableMap.values());
    }

    private CompletableFuture<List<ModelProto.RoomTableInfo>> collectTableInfo(List<Table> tables) {
        List<CompletableFuture<ModelProto.RoomTableInfo>> futures = new ArrayList<>();
        for (Table table : tables) futures.add(table.getRoomTableInfoAsync());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> collectResults(futures));
    }

    private List<ModelProto.RoomTableInfo> collectResults(List<CompletableFuture<ModelProto.RoomTableInfo>> futures) {
        List<ModelProto.RoomTableInfo> result = new ArrayList<>();
        for (CompletableFuture<ModelProto.RoomTableInfo> future : futures) result.add(future.join());
        return result;
    }

    /**
     * Lobby 连入 game 后注册在 ServerClientManager，不能用 ServerManager（那是 game 主动外连）
     */
    private ClientHandler lobbyClient() {
        return Game.getInstance().getServerClientManager().getServerClient(ServerType.Lobby);
    }

    /**
     * 通知Lobby桌子已销毁
     */
    private void notifyRoomTableDestroyed(long tableId) {
        try {
            ClientHandler lobbyServer = lobbyClient();
            if (lobbyServer == null) {
                logger.warn("通知Lobby桌子销毁失败：无 Lobby 连接, tableId: {}", tableId);
                return;
            }

            ServerProto.NotTableDestroyed not = ServerProto.NotTableDestroyed.newBuilder()
                    .setTableId(tableId)
                    .build();
            lobbyServer.sendMessage(SMsg.NOT_TABLE_DESTROYED_MSG, not);
            logger.info("已通知Lobby桌子销毁, tableId: {}", tableId);
        } catch (Exception e) {
            logger.error("通知Lobby桌子销毁失败, tableId: {}", tableId, e);
        }
    }

    /**
     * 通知Lobby玩家离桌（桌子仍保留）
     */
    public void notifyRoomPlayerLeft(long tableId, int roleId) {
        try {
            ClientHandler lobbyServer = lobbyClient();
            if (lobbyServer == null) {
                logger.warn("通知Lobby玩家离桌失败：无 Lobby 连接, tableId: {}, roleId: {}", tableId, roleId);
                return;
            }

            ServerProto.NotTablePlayerLeft not = ServerProto.NotTablePlayerLeft.newBuilder()
                    .setTableId(tableId)
                    .setRoleId(roleId)
                    .build();
            lobbyServer.sendMessage(SMsg.NOT_TABLE_PLAYER_LEFT_MSG, not);
            logger.info("已通知Lobby玩家离桌, tableId: {}, roleId: {}", tableId, roleId);
        } catch (Exception e) {
            logger.error("通知Lobby玩家离桌失败, tableId: {}, roleId: {}", tableId, roleId, e);
        }
    }

    /**
     * 获取新的桌子ID（单调递增，防撞号）
     */
    private long getTableId() {
        long id = tableIdSeq.incrementAndGet();
        logger.info("创建新桌子ID: {}", id);
        return id;
    }

    /**
     * 创建桌子
     *
     * @param roomId 桌子类型
     * @param role   创建的玩家（avatar 若以 TMJSON: 开头则为自定义模板覆盖）
     * @return 桌子实例
     */
    public Table createTable(int roomId, ModelProto.RoomRole role) {
        synchronized (TableManager.class) {
            TableModel model = resolveModel(roomId, role);
            if (model == null) {
                throw new IllegalArgumentException("未知房间模板 roomId=" + roomId);
            }
            Table table;
            if (model.getType() == 1) {
                table = new MjTable(getTableId(), model, role);
            } else {
                table = new DdzTable(getTableId(), model, role);
            }
            addTable(table);
            return table;
        }
    }

    private TableModel resolveModel(int roomId, ModelProto.RoomRole role) {
        if (role != null && !role.getAvatar().isEmpty()) {
            String avatar = role.getAvatar().toStringUtf8();
            if (avatar.startsWith("TMJSON:")) {
                TableModel custom = TableModelJson.parse(avatar.substring("TMJSON:".length()));
                if (custom != null) {
                    if (custom.getId() <= 0) {
                        custom.setId(roomId > 0 ? roomId : (10000 + (int) (System.currentTimeMillis() % 100000)));
                    }
                    configManager.putRuntimeModel(custom);
                    return custom;
                }
            }
        }
        return configManager.getTableModel(roomId);
    }

    /**
     * 查找用户所在的桌子
     */
    public List<Table> findTablesByUserId(int userId) {
        List<Table> result = new ArrayList<>();
        for (Table table : tableMap.values()) {
            if (table.getUsers().containsKey(userId)) {
                result.add(table);
            }
        }
        return result;
    }

    /**
     * 获取所有桌子的RoomTableInfo（用于Room重启恢复）
     */
    public List<ModelProto.RoomTableInfo> getAllTableInfo() {
        List<ModelProto.RoomTableInfo> list = new ArrayList<>();
        for (Table table : tableMap.values()) {
            ModelProto.RoomTableInfo.Builder builder = ModelProto.RoomTableInfo.newBuilder()
                    .setTableId(table.getTableId())
                    .setRoomId(table.getTableModel().getId())
                    .setOwnerId(table.getOwnerId())
                    .setCreatorId(table.getOwnerId())
                    .setGameType(table.getTableModel().getType());

            for (TableUser user : table.getSeatUsers().values()) {
                if (user != null) {
                    builder.addTableRoles(ModelProto.RoomRole.newBuilder()
                            .setRoleId(user.getUserId())
                            .setNickName(com.google.protobuf.ByteString.copyFromUtf8(user.getNick()))
                            .build());
                }
            }
            list.add(builder.build());
        }
        return list;
    }
}

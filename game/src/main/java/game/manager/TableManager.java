package game.manager;

import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.MjTable;
import game.manager.table.DdzTable;
import model.tablemodel.TableModel;
import msg.registor.enums.ServerType;
import msg.registor.message.SMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import net.connect.handle.ConnectHandler;
import utils.metrics.MetricsCollector;
import utils.other.excel.ExcelUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 桌子管理器
 * 负责游戏桌子的创建、管理和生命周期控制
 */
public class TableManager {
    private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

    // 初始桌子序号
    private static final int BASE_INDEX = 100000;
    private final Map<Long, Table> tableMap;

    /**
     * 当前初始化桌子号
     */
    private int currentIndex = BASE_INDEX;

    private final Map<Integer, TableModel> tableModelMap = new HashMap<>();

    public TableManager() {
        tableMap = new ConcurrentHashMap<>();
        init();
        logger.info("桌子管理器初始化完成");
    }


    /**
     * 初始化房间管理器
     * 从Excel文件加载房间配置
     */
    public synchronized void init() {
        try {
            List<Object> properties = new ArrayList<>();

            // 读取Excel配置
            ExcelUtil.readExcelJavaValue("TableModel.xlsx", properties, TableModel.class);

            synchronized (tableModelMap) {
                //Todo 重新load 以后之前的房间尽量打完删除
                tableModelMap.clear();

                for (Object object : properties) {
                    TableModel model = (TableModel) object;
                    tableModelMap.put(model.getId(), model);
                    logger.debug("加载房间模板, id: {}", model.getId());
                }
            }

            logger.info("房间管理器初始化完成,加载模板数量: {}", tableModelMap.size());
        } catch (Exception e) {
            logger.error("房间管理器初始化失败", e);
            throw new RuntimeException("房间管理器初始化失败", e);
        }
    }

    /**
     * 添加桌子
     */
    public void addTable(Table table) {
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
    public Table removeTable(long tableId) {
        Table removedTable = tableMap.remove(tableId);
        if (removedTable != null) {
            MetricsCollector.getInstance().setGauge("game.active_tables", tableMap.size());
            MetricsCollector.getInstance().incrementCounter("game.tables_destroyed");
            logger.info("删除桌子, tableId: {}", tableId);
            notifyRoomTableDestroyed(tableId);
        } else {
            logger.warn("桌子不存在,无法删除, tableId: {}", tableId);
        }
        return removedTable;
    }

    /**
     * 通知Room桌子已销毁
     */
    private void notifyRoomTableDestroyed(long tableId) {
        try {
            ConnectHandler roomServer = Game.getInstance().getServerManager().getServerClient(ServerType.Room);
            if (roomServer == null) return;

            ServerProto.NotTableDestroyed not = ServerProto.NotTableDestroyed.newBuilder()
                    .setTableId(tableId)
                    .build();
            roomServer.sendMessage(SMsg.NOT_TABLE_DESTROYED_MSG, not);
            logger.debug("已通知Room桌子销毁, tableId: {}", tableId);
        } catch (Exception e) {
            logger.error("通知Room桌子销毁失败, tableId: {}", tableId, e);
        }
    }

    /**
     * 获取新的桌子ID
     */
    private long getTableId() {
        logger.info("创建新桌子ID: {}", ++currentIndex);
        return currentIndex;

    }

    /**
     * 创建桌子
     *
     * @param roomId 桌子类型
     * @param role   创建的玩家
     * @return 桌子实例
     */
    public Table createTable(int roomId, ModelProto.RoomRole role) {
        synchronized (TableManager.class) {
            TableModel model = tableModelMap.get(roomId);
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

    /**
     * 获取当前桌子数量
     */
    public int getTableCount() {
        return tableMap.size();
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
     * 清理所有桌子（用于服务器关闭时）
     */
    public void clearAllTables() {
        int count = tableMap.size();
        tableMap.clear();
        logger.info("清理所有桌子,数量: {}", count);
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
                            .setRoleId((int) user.getUserId())
                            .setNickName(com.google.protobuf.ByteString.copyFromUtf8(user.getNick()))
                            .build());
                }
            }
            list.add(builder.build());
        }
        return list;
    }
}
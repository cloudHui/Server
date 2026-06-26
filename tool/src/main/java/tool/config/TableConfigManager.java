package tool.config;

import model.tablemodel.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 房间模板配置管理器
 * 负责加载、缓存和热更新房间模板配置
 */
public class TableConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(TableConfigManager.class);

    private static final String CONFIG_DIR = "../config";

    private final ConfigLoader<TableModel> configLoader;
    private final Map<Integer, TableModel> tableModelMap = new ConcurrentHashMap<>();
    private Consumer<Map<Integer, TableModel>> onChangeCallback;

    public TableConfigManager() {
        this.configLoader = new ConfigLoader<>(CONFIG_DIR, TableModel.class);
        this.configLoader.onChange(this::onConfigChange);
    }

    /**
     * 设置配置变更回调
     */
    public void onChange(Consumer<Map<Integer, TableModel>> callback) {
        this.onChangeCallback = callback;
    }

    /**
     * 加载配置
     */
    public boolean loadFail() {
        List<TableModel> configs = configLoader.load();
        if (configs == null) {
            return true;
        }

        tableModelMap.clear();
        for (TableModel model : configs) {
            tableModelMap.put(model.getId(), model);
        }

        logger.info("房间模板加载完成, 数量: {}", tableModelMap.size());
        return false;
    }

    /**
     * 启动配置监控
     */
    public void startWatch() {
        configLoader.startWatch();
    }

    /**
     * 停止配置监控
     */
    public void stopWatch() {
        configLoader.stopWatch();
    }

    /**
     * 获取房间模板
     */
    public TableModel getTableModel(int modelId) {
        return tableModelMap.get(modelId);
    }

    /**
     * 获取所有房间模板
     */
    public Map<Integer, TableModel> getAllTableModels() {
        return tableModelMap;
    }

    /**
     * 配置变更回调
     */
    private void onConfigChange(List<TableModel> newConfigs) {
        logger.info("检测到配置变更，重新加载房间模板...");
        tableModelMap.clear();
        for (TableModel model : newConfigs) {
            tableModelMap.put(model.getId(), model);
        }
        logger.info("房间模板更新完成, 数量: {}", tableModelMap.size());

        if (onChangeCallback != null) {
            onChangeCallback.accept(tableModelMap);
        }
    }
}

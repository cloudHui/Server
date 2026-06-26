package tool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 配置加载器
 * 支持启动时加载和定时热更新
 * 自动扫描目录找到对应的配置文件
 */
public class ConfigLoader<T> {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private final String configDir;
    private final Class<T> configType;
    private final AtomicReference<List<T>> configHolder = new AtomicReference<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ConfigWatcher");
        t.setDaemon(true);
        return t;
    });

    private Path configPath;
    private FileTime lastModified;
    private long lastSize;
    private Consumer<List<T>> onChangeCallback;

    public ConfigLoader(String configDir, Class<T> configType) {
        this.configDir = configDir;
        this.configType = configType;
    }

    /**
     * 设置配置变更回调
     */
    public void onChange(Consumer<List<T>> callback) {
        this.onChangeCallback = callback;
    }

    /**
     * 启动时加载配置
     * 自动扫描目录找到对应的配置文件
     */
    public List<T> load() {
        try {
            // 扫描目录找到对应的配置文件
            configPath = findConfigFile();
            if (configPath == null) {
                logger.error("未找到配置文件: {}, 类型: {}", configDir, configType.getSimpleName());
                return null;
            }

            List<T> configs = readConfig(configPath);
            configHolder.set(configs);

            // 记录文件信息
            lastModified = Files.getLastModifiedTime(configPath);
            lastSize = Files.size(configPath);
            logger.info("配置加载成功: {}, 文件大小: {} bytes, 修改时间: {}, 配置数量: {}",
                    configPath, lastSize, lastModified, configs.size());

            return configs;
        } catch (Exception e) {
            logger.error("加载配置失败: {}", configDir, e);
            return null;
        }
    }

    /**
     * 扫描目录找到对应的配置文件
     * 文件名格式: <classname>_models.dat
     */
    private Path findConfigFile() {
        Path dir = Paths.get(configDir);
        if (!Files.isDirectory(dir)) {
            logger.error("配置目录不存在: {}", configDir);
            return null;
        }

        String expectedFileName = configType.getSimpleName().toLowerCase() + "_models.dat";

        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().equals(expectedFileName))
                         .findFirst()
                         .orElse(null);
        } catch (Exception e) {
            logger.error("扫描配置目录失败: {}", configDir, e);
            return null;
        }
    }

    /**
     * 启动定时检测（每分钟检测一次）
     */
    public void startWatch() {
        scheduler.scheduleAtFixedRate(this::checkAndUpdate, 1, 1, TimeUnit.MINUTES);
        logger.info("启动配置监控: {}, 检测间隔: 1分钟", configPath);
    }

    /**
     * 停止定时检测
     */
    public void stopWatch() {
        scheduler.shutdown();
        logger.info("停止配置监控: {}", configPath);
    }

    /**
     * 获取当前配置
     */
    public List<T> getConfig() {
        return configHolder.get();
    }

    /**
     * 检测并更新配置
     */
    private void checkAndUpdate() {
        try {
            if (configPath == null || !Files.exists(configPath)) {
                // 重新扫描目录
                configPath = findConfigFile();
                if (configPath == null) {
                    return;
                }
            }

            FileTime currentModified = Files.getLastModifiedTime(configPath);
            long currentSize = Files.size(configPath);

            // 检测变化
            if (!currentModified.equals(lastModified) || currentSize != lastSize) {
                logger.info("检测到配置文件变化: {}, 原大小: {} -> 新大小: {}, 原时间: {} -> 新时间: {}",
                        configPath, lastSize, currentSize, lastModified, currentModified);

                List<T> newConfigs = readConfig(configPath);
                configHolder.set(newConfigs);

                lastModified = currentModified;
                lastSize = currentSize;

                logger.info("配置更新成功: {}, 新配置数量: {}", configPath, newConfigs.size());

                if (onChangeCallback != null) {
                    onChangeCallback.accept(newConfigs);
                }
            }
        } catch (Exception e) {
            logger.error("检测配置更新失败: {}", configPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> readConfig(Path path) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            return (List<T>) ois.readObject();
        }
    }
}

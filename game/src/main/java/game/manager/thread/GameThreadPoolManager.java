package game.manager.thread;

import threadtutil.thread.ExecutorPool;
import threadtutil.utils.TimeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Game 进程的线程资源唯一入口。
 * 四类任务必须通过对应的池提交，避免桌内逻辑、玩家请求、桌管理和数据库互相阻塞。
 */
public final class GameThreadPoolManager {
    private final ExecutorPool tablePool;
    private final ExecutorPool playerPool;
    private final ExecutorPool tableManagerPool;
    private final ExecutorService databasePool;

    public GameThreadPoolManager(int workerSize, int queueCapacity, int databaseSize) {
        int size = workerSize > 0 ? workerSize : Math.max(32, TimeUtils.PROCESS_NUMBER);
        tablePool = new ExecutorPool("Game-Table", size, queueCapacity);
        playerPool = new ExecutorPool("Game-Player", size, queueCapacity);
        tableManagerPool = new ExecutorPool("Game-TableManager", 1, queueCapacity);
        databasePool = Executors.newFixedThreadPool(Math.max(1, databaseSize), r -> {
            Thread t = new Thread(r, "Game-Database");
            t.setDaemon(false);
            return t;
        });
    }

    public ExecutorPool tablePool() { return tablePool; }
    public ExecutorPool playerPool() { return playerPool; }
    public ExecutorPool tableManagerPool() { return tableManagerPool; }
    public ExecutorService databasePool() { return databasePool; }

    public void shutdown() {
        tablePool.getExecutorService().shutdownNow();
        playerPool.getExecutorService().shutdownNow();
        tableManagerPool.getExecutorService().shutdownNow();
        databasePool.shutdownNow();
    }
}

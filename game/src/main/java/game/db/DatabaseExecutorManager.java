package game.db;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 数据库专用线程池，避免 SQLite 写入阻塞桌子逻辑线程或网络线程。 */
public class DatabaseExecutorManager {
    private final ExecutorService executor;

    public DatabaseExecutorManager(int poolSize) {
        executor = Executors.newFixedThreadPool(Math.max(1, poolSize), runnable ->
                new Thread(runnable, "Game-Database"));
    }

    public DatabaseExecutorManager(ExecutorService executor) {
        this.executor = executor;
    }

    public CompletableFuture<Void> submit(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.execute(() -> runTask(task, future));
        return future;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void runTask(Runnable task, CompletableFuture<Void> future) {
        try {
            task.run();
            future.complete(null);
        } catch (Throwable error) {
            future.completeExceptionally(error);
        }
    }
}

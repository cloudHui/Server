package game.manager.thread;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** 每桌独立的单线程执行器，保证同一桌所有状态变更按顺序执行。 */
public class TableExecutorManager {
    private final Map<Long, ScheduledThreadPoolExecutor> executors = new ConcurrentHashMap<>();

    public void register(long tableId) {
        executors.computeIfAbsent(tableId, this::newExecutor);
    }

    public CompletableFuture<Void> submit(long tableId, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduledThreadPoolExecutor executor = executors.get(tableId);
        if (executor == null) {
            future.completeExceptionally(new IllegalStateException("桌子执行器不存在: " + tableId));
            return future;
        }
        executor.execute(() -> runTask(task, future));
        return future;
    }

    public ScheduledFuture<?> schedule(long tableId, Runnable task, long delayMs, long intervalMs) {
        register(tableId);
        return executors.get(tableId).scheduleAtFixedRate(task, delayMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    public void remove(long tableId) {
        ScheduledThreadPoolExecutor executor = executors.remove(tableId);
        if (executor != null) executor.shutdownNow();
    }

    public void shutdown() {
        for (Long tableId : executors.keySet()) remove(tableId);
    }

    private ScheduledThreadPoolExecutor newExecutor(long tableId) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, namedFactory(tableId));
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private ThreadFactory namedFactory(long tableId) {
        return runnable -> new Thread(runnable, "Table-" + tableId);
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

package game.manager.thread;

import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.utils.TimeUtils;
import utils.trace.TraceContext;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 固定桌工作线程池：同桌任务串行，多桌可共享物理线程。
 * 调度线程只负责触发，真正改桌状态仍投回桌串行队列。
 */
public class TableExecutorManager {
    private final ExecutorPool workers;
    private final ScheduledExecutorService scheduler;
    private final Set<Long> activeTables = ConcurrentHashMap.newKeySet();
    /** 防止调度堆积：同一桌上一拍未结束则跳过本拍。 */
    private final Set<Long> tickBusy = ConcurrentHashMap.newKeySet();

    public TableExecutorManager(int poolSize, int queueCapacity) {
        int size = Math.max(32, poolSize > 0 ? poolSize : TimeUtils.PROCESS_NUMBER);
        this.workers = new ExecutorPool("TableWorker", size, queueCapacity);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TableScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public TableExecutorManager(ExecutorPool workers, int queueCapacity) {
        this.workers = workers;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TableScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /** 建桌时登记，允许后续 submit/schedule。 */
    public void register(long tableId) {
        activeTables.add(tableId);
    }

    /** 将任务按 tableId 亲和投递到固定 worker，保证同桌串行。 */
    public CompletableFuture<Void> submit(long tableId, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!activeTables.contains(tableId)) {
            future.completeExceptionally(new IllegalStateException("桌子执行器不存在: " + tableId));
            return future;
        }
        workers.serialExecute(new TableTask(tableId, () -> runTask(tableId, task, future)));
        return future;
    }

    /**
     * 周期调度：在调度线程触发后投回桌队列执行。
     * 上一拍仍在队列/执行中时跳过，避免同桌任务堆积。
     */
    public ScheduledFuture<?> schedule(long tableId, Runnable task, long delayMs, long intervalMs) {
        register(tableId);
        return scheduler.scheduleAtFixedRate(
                () -> dispatchTick(tableId, task), delayMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    /** 删桌：拒绝新任务，并清掉忙碌标记。 */
    public void remove(long tableId) {
        activeTables.remove(tableId);
        tickBusy.remove(tableId);
    }

    public void shutdown() {
        activeTables.clear();
        tickBusy.clear();
        scheduler.shutdownNow();
        workers.getExecutorService().shutdownNow();
    }

    private void dispatchTick(long tableId, Runnable task) {
        if (!activeTables.contains(tableId)) return;
        if (!tickBusy.add(tableId)) return;
        submit(tableId, () -> {
            try {
                task.run();
            } finally {
                tickBusy.remove(tableId);
            }
        }).exceptionally(error -> {
            tickBusy.remove(tableId);
            return null;
        });
    }

    private void runTask(long tableId, Runnable task, CompletableFuture<Void> future) {
        try {
            TraceContext.setTableId(tableId);
            task.run();
            future.complete(null);
        } catch (Throwable error) {
            future.completeExceptionally(error);
        } finally {
            TraceContext.endTrace();
        }
    }

    /** 以 tableId 哈希为 groupId，供 ExecutorPool 做同桌串行亲和。 */
    private static final class TableTask implements Task {
        private final int groupId;
        private final Runnable runnable;

        private TableTask(long tableId, Runnable runnable) {
            this.groupId = Long.hashCode(tableId);
            this.runnable = runnable;
        }

        @Override
        public int groupId() {
            return groupId;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}

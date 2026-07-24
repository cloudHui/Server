package game.manager.thread;

import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.utils.TimeUtils;
import utils.trace.TraceContext;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Game 进程线程资源唯一入口。
 * <p>
 * 整合原先分散的桌执行器与桌管理执行器，四类任务通过对应能力提交，
 * 避免桌内逻辑、玩家请求、桌管理和数据库互相阻塞。
 */
public final class GameThreadPoolManager {
	private final ExecutorPool tablePool;
	private final ExecutorPool playerPool;
	private final ExecutorPool tableManagerPool;
	private final ExecutorService databasePool;
	private final ScheduledExecutorService tableScheduler;
	private final Set<Long> activeTables = ConcurrentHashMap.newKeySet();
	/** 防止调度堆积：同一桌上一拍未结束则跳过本拍。 */
	private final Set<Long> tickBusy = ConcurrentHashMap.newKeySet();

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
		tableScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "Game-TableScheduler");
			t.setDaemon(true);
			return t;
		});
	}

	public ExecutorPool tablePool() {
		return tablePool;
	}

	public ExecutorPool playerPool() {
		return playerPool;
	}

	public ExecutorPool tableManagerPool() {
		return tableManagerPool;
	}

	public ExecutorService databasePool() {
		return databasePool;
	}

	/** 建桌时登记，允许后续 submit/schedule。 */
	public void registerTable(long tableId) {
		activeTables.add(tableId);
	}

	/** 将任务按 tableId 亲和投递，保证同桌串行。 */
	public CompletableFuture<Void> submitTable(long tableId, Runnable task) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		if (!activeTables.contains(tableId)) {
			future.completeExceptionally(new IllegalStateException("桌子执行器不存在: " + tableId));
			return future;
		}
		tablePool.serialExecute(new TableTask(tableId, () -> runTableTask(tableId, task, future)));
		return future;
	}

	/**
	 * 周期调度：调度线程只负责触发，真正改桌状态仍投回桌串行队列。
	 * 上一拍仍在队列/执行中时跳过，避免同桌任务堆积。
	 */
	public ScheduledFuture<?> scheduleTable(long tableId, Runnable task, long delayMs, long intervalMs) {
		registerTable(tableId);
		return tableScheduler.scheduleAtFixedRate(
				() -> dispatchTick(tableId, task), delayMs, intervalMs, TimeUnit.MILLISECONDS);
	}

	public void cancelTableSchedule(ScheduledFuture<?> future) {
		if (future != null) {
			future.cancel(false);
		}
	}

	/** 删桌：拒绝新任务，并清掉忙碌标记。 */
	public void removeTable(long tableId) {
		activeTables.remove(tableId);
		tickBusy.remove(tableId);
	}

	/**
	 * 桌子生命周期与全局索引操作走单线程池，避免网络线程直接改索引。
	 */
	public <T> CompletableFuture<T> submitTableManager(Callable<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		tableManagerPool.execute(() -> runManagerTask(task, future));
		return future;
	}

	public void shutdown() {
		activeTables.clear();
		tickBusy.clear();
		tableScheduler.shutdownNow();
		tablePool.getExecutorService().shutdownNow();
		playerPool.getExecutorService().shutdownNow();
		tableManagerPool.getExecutorService().shutdownNow();
		databasePool.shutdownNow();
	}

	private void dispatchTick(long tableId, Runnable task) {
		if (!activeTables.contains(tableId)) {
			return;
		}
		if (!tickBusy.add(tableId)) {
			return;
		}
		submitTable(tableId, () -> {
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

	private void runTableTask(long tableId, Runnable task, CompletableFuture<Void> future) {
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

	private <T> void runManagerTask(Callable<T> task, CompletableFuture<T> future) {
		try {
			future.complete(task.call());
		} catch (Throwable error) {
			future.completeExceptionally(error);
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

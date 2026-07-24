package threadtutil.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.model.TaskList;
import threadtutil.utils.TimeUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 执行器池：普通并行执行 + 按 groupId 串行亲和。
 * 同 groupId 任务进入同一 TaskList，保证顺序；不同 group 可并行。
 */
public class ExecutorPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorPool.class);

	private final ThreadPool threadPool;
	private final TaskList[] taskLists;

	private static final long PROCESS_TIMEOUT_MS = 5000L;
	private static final long STACK_TRACE_TIMEOUT_MS = 60000L;
	private static final int INIT_SIZE = 1000;

	public ExecutorPool(String executorName) {
		this(executorName, 0);
	}

	public ExecutorPool(String executorName, int size) {
		this(executorName, size, INIT_SIZE);
	}

	public ExecutorPool(String executorName, int size, int queueCapacity) {
		this.threadPool = new ThreadPool(executorName,
				(size < 1) ? TimeUtils.PROCESS_NUMBER : size, queueCapacity);
		this.taskLists = new TaskList[this.threadPool.size()];
		for (int i = 0; i < this.threadPool.size(); ++i) {
			this.taskLists[i] = new TaskList();
		}
		LOGGER.info("[init] thread pool size:{}, queue capacity:{}", this.threadPool.size(), queueCapacity);
	}

	public int size() {
		return this.threadPool.size();
	}

	public void execute(Runnable runnable) {
		this.threadPool.execute(runnable);
	}

	/**
	 * 串行化执行：相同 groupId 的任务按入队顺序执行。
	 * 使用 floorMod 避免 Integer.MIN_VALUE 时 Math.abs 仍为负。
	 */
	public CompletableFuture<Task> serialExecute(Task task) {
		TaskNode taskNode = new TaskNode(task);
		int index = Math.floorMod(taskNode.groupId(), this.size());
		if (this.taskLists[index].offerAndSchedule(taskNode)) {
			this.threadPool.execute(() -> processTasks(index));
		}
		return taskNode.completableFuture;
	}

	/**
	 * 优先处理目标队列，再尝试窃取其他空闲队列，提高线程利用率。
	 */
	private void processTasks(int startIndex) {
		try {
			long threadId = Thread.currentThread().getId();
			drainOne(startIndex, threadId);
			int size = this.size();
			for (int step = 1; step < size; ++step) {
				int idx = startIndex + step;
				if (idx >= size) {
					idx -= size;
				}
				if (this.taskLists[idx].isNotEmpty() && !this.taskLists[idx].isBusy()) {
					drainOne(idx, threadId);
				} else if (this.taskLists[idx].isBusy() && !this.taskLists[idx].isSelf(threadId)) {
					checkAndLogTimeout(idx);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error processing tasks", e);
		}
	}

	private void drainOne(int idx, long threadId) {
		TaskList list = this.taskLists[idx];
		if (list.isBusy() && !list.isSelf(threadId)) {
			checkAndLogTimeout(idx);
			return;
		}
		if (!list.getProcessingAuthority(threadId)) {
			return;
		}
		try {
			processTaskList(idx);
		} finally {
			list.releaseProcessingAuthority(threadId);
			// 仅持有处理权的线程负责解除/重投调度，避免与在途 worker 互相清标记。
			if (list.finishAndRescheduleIfNeeded()) {
				this.threadPool.execute(() -> processTasks(idx));
			}
		}
	}

	private void checkAndLogTimeout(int idx) {
		long busySince = this.taskLists[idx].getBusySinceMs();
		if (busySince <= 0L) {
			return;
		}
		long elapsed = System.currentTimeMillis() - busySince;
		if (elapsed <= PROCESS_TIMEOUT_MS) {
			return;
		}
		long processorId = this.taskLists[idx].getProcessorId();
		String processorName = this.threadPool.getThreadName(processorId);
		LOGGER.error("[THREAD POOL] process is too long(idx:{}, processor:{}:{}, elapsedMs:{})",
				idx, processorId, processorName, elapsed);
		if (elapsed > STACK_TRACE_TIMEOUT_MS) {
			logStackTrace(processorId);
		}
	}

	private void logStackTrace(long processorId) {
		Thread thread = ThreadPool.getThread(processorId);
		if (thread == null) {
			return;
		}
		StringBuilder sb = new StringBuilder(256);
		sb.append("Stack for ").append(thread.getId()).append(':').append(thread.getName()).append(':');
		for (StackTraceElement element : thread.getStackTrace()) {
			sb.append("\n ").append(element.getClassName())
					.append('.').append(element.getMethodName())
					.append('(').append(element.getFileName())
					.append(':').append(element.getLineNumber()).append(')');
		}
		LOGGER.error(sb.toString());
	}

	private void processTaskList(int idx) {
		TaskNode task;
		while ((task = (TaskNode) this.taskLists[idx].poll()) != null) {
			this.taskLists[idx].updateTime();
			try {
				task.run();
				task.completableFuture.complete(task.t);
			} catch (Throwable e) {
				LOGGER.error("RUN1:{}", task, e);
				try {
					task.completableFuture.completeExceptionally(e);
				} catch (Throwable e2) {
					LOGGER.error("RUN2:{}", task, e2);
				}
			}
		}
	}

	public CompletableFuture<Void> run(Runnable runnable) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		this.threadPool.execute(() -> {
			try {
				runnable.run();
				future.complete(null);
			} catch (Throwable throwable) {
				future.completeExceptionally(throwable);
			}
		});
		return future;
	}

	public ExecutorService getExecutorService() {
		return this.threadPool.getPools();
	}

	/** 优雅关闭底层线程池。 */
	public void shutdown() {
		this.threadPool.close();
	}

	private static class TaskNode implements Task {
		public final Task t;
		public final CompletableFuture<Task> completableFuture;

		public TaskNode(Task task) {
			this.t = task;
			this.completableFuture = new CompletableFuture<>();
		}

		@Override
		public int groupId() {
			return this.t.groupId();
		}

		@Override
		public void run() {
			this.t.run();
		}

		@Override
		public String toString() {
			return String.format("TaskNode %s", this.t.toString());
		}
	}
}

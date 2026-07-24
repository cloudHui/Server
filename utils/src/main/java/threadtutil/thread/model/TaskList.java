package threadtutil.thread.model;

import threadtutil.thread.Task;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 串行任务队列：同组任务 FIFO，同一时刻仅一个 worker 持有处理权。
 * 使用 ConcurrentLinkedQueue 避免 CopyOnWriteArrayList 在高频入出队时的复制开销。
 */
public class TaskList {
	private final AtomicLong processor = new AtomicLong(0L);
	/** 是否已向线程池投递过 drain 任务，防止每入队一次就提交一次 Runnable。 */
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private volatile long busySinceMs;
	private final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();

	public boolean isBusy() {
		return processor.get() != 0L;
	}

	public boolean isSelf(long processorId) {
		return getProcessorId() == processorId;
	}

	public boolean getProcessingAuthority(long processorId) {
		if (processor.compareAndSet(0L, processorId)) {
			busySinceMs = System.currentTimeMillis();
			return true;
		}
		return processor.get() == processorId;
	}

	public void releaseProcessingAuthority(long processorId) {
		if (processor.compareAndSet(processorId, 0L)) {
			busySinceMs = 0L;
		}
	}

	public long getProcessorId() {
		return processor.get();
	}

	public void updateTime() {
		busySinceMs = System.currentTimeMillis();
	}

	/** 开始处理的时间戳（毫秒），空闲时为 0。 */
	public long getBusySinceMs() {
		return busySinceMs;
	}

	/**
	 * 入队；若此前未调度 drain，返回 true，由调用方提交 worker。
	 */
	public boolean offerAndSchedule(Task task) {
		tasks.offer(task);
		return scheduled.compareAndSet(false, true);
	}

	public Task poll() {
		Task task = tasks.poll();
		if (task != null) {
			busySinceMs = System.currentTimeMillis();
		}
		return task;
	}

	public boolean isNotEmpty() {
		return !tasks.isEmpty();
	}

	/**
	 * drain 结束后解除调度标记；若期间又有新任务入队，重新置位并返回 true。
	 */
	public boolean finishAndRescheduleIfNeeded() {
		scheduled.set(false);
		return !tasks.isEmpty() && scheduled.compareAndSet(false, true);
	}
}

package threadtutil.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.utils.TimeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定大小线程池：统一命名、有界队列、队列满时 CallerRuns 反压。
 */
public class ThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);
	private static final int INIT_SIZE = 1000;

	private final ExecutorService pools;
	private final ThreadGroup group;
	private final String threadPrefix;
	private final AtomicInteger threadNumber;
	private final Map<Long, String> threadName;
	private final int size;

	public ThreadPool(String prefix) {
		this(prefix, 0);
	}

	public ThreadPool(String prefix, int size) {
		this(prefix, size, INIT_SIZE);
	}

	public ThreadPool(String prefix, int size, int queueCapacity) {
		this.threadName = new ConcurrentHashMap<>();
		SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		this.threadPrefix = prefix;
		this.threadNumber = new AtomicInteger(0);
		this.size = (size > 0) ? size : TimeUtils.PROCESS_NUMBER;
		int capacity = queueCapacity > 0 ? queueCapacity : INIT_SIZE;

		// 有界队列 + CallerRuns：满载时由提交方执行，形成自然反压，避免直接拒绝任务。
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				this.size,
				this.size,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(capacity),
				this::createThread,
				new ThreadPoolExecutor.CallerRunsPolicy());
		executor.prestartAllCoreThreads();
		this.pools = executor;
		LOGGER.info("[init] ThreadPool name:{}, size:{}, queue:{}", prefix, this.size, capacity);
	}

	private Thread createThread(Runnable runnable) {
		Thread t = new Thread(this.group, runnable,
				this.threadPrefix + '_' + this.threadNumber.incrementAndGet(), 0L);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		this.threadName.put(t.getId(), t.getName());
		return t;
	}

	public final int size() {
		return this.size;
	}

	public ExecutorService getPools() {
		return this.pools;
	}

	public Future<?> execute(Runnable runnable) {
		return this.pools.submit(runnable);
	}

	public void close() {
		this.pools.shutdown();
	}

	public void closeNow() {
		this.pools.shutdownNow();
	}

	protected String getThreadName(long threadId) {
		return this.threadName.get(threadId);
	}

	/** 按线程 ID 查找（仅用于超时诊断，热路径勿调用）。 */
	public static Thread getThread(long threadId) {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		while (root.getParent() != null) {
			root = root.getParent();
		}
		Thread[] threads = new Thread[Math.max(8, root.activeCount() * 2)];
		int count = root.enumerate(threads, true);
		for (int i = 0; i < count; ++i) {
			if (threads[i] != null && threadId == threads[i].getId()) {
				return threads[i];
			}
		}
		return null;
	}
}

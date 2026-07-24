package threadtutil.timer;

import threadtutil.thread.ExecutorPool;
import threadtutil.timer.model.SerialTimeNode;
import threadtutil.timer.model.TimeNode;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时器：在独立调度线程上检查到期节点，实际业务投递到 ExecutorPool。
 */
public class Timer extends AbstractTimer<ExecutorPool> {
	private static final AtomicInteger TIMER_SEQ = new AtomicInteger();
	private volatile Thread schedulerThread;

	public Timer() {
		super(new ArrayList<>());
	}

	@Override
	public Timer setRunners(ExecutorPool runners) {
		exit();
		timeSignal.notifySignal();
		this.runners = runners;
		Thread thread = new Thread(this, "Threadtutil-Timer-" + TIMER_SEQ.incrementAndGet());
		thread.setDaemon(true);
		this.schedulerThread = thread;
		thread.start();
		return this;
	}

	@Override
	protected CompletableFuture<?> executeTimeNode(TimeNode<?> timeNode) {
		if (timeNode instanceof SerialTimeNode) {
			return runners.serialExecute((SerialTimeNode<?>) timeNode);
		}
		return runners.run(timeNode);
	}

	@Override
	protected void rescheduleNode(TimeNode<?> node) {
		runners.run(() -> addNode(node));
	}

	/** 停止调度线程（通过 exit 打断循环）。 */
	public void stop() {
		exit();
		timeSignal.notifySignal();
		Thread thread = schedulerThread;
		if (thread != null) {
			thread.interrupt();
		}
	}
}

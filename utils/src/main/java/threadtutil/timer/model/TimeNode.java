package threadtutil.timer.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.timer.Runner;

/**
 * 普通定时节点。count=-1 表示无限次；runner 返回 true 时提前结束。
 */
public class TimeNode<T> implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeNode.class);
	private final int id;
	private final Runner<T> runner;
	private final T param;
	private final long interval;
	private int count;
	private long triggerTime;

	public TimeNode(int id, Runner<T> runner, T param, long delay, long interval) {
		this(id, runner, param, delay, interval, -1);
	}

	public TimeNode(int id, Runner<T> runner, T param, long delay, long interval, int count) {
		this.id = id;
		this.runner = runner;
		this.param = param;
		this.interval = interval;
		this.count = count;
		this.triggerTime = System.currentTimeMillis() + delay;
	}

	public int getId() {
		return this.id;
	}

	public long timeDifference(long time) {
		return this.triggerTime - time;
	}

	public boolean onTime(long time) {
		return time >= this.triggerTime;
	}

	public void refreshTriggerTime() {
		this.triggerTime += this.interval;
	}

	public boolean unFinished() {
		return this.count != 0;
	}

	@Override
	public void run() {
		if (this.count > 0) {
			--this.count;
		}
		try {
			if (this.runner.run(this.param)) {
				this.count = 0;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute time node id: {}", this.id, e);
		}
	}
}

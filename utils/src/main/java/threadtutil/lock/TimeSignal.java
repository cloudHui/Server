package threadtutil.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时器等待/唤醒信号。
 * 先置 notified 再 notify，避免丢失唤醒；wait 返回后清除标记。
 */
public class TimeSignal {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeSignal.class);
	private boolean notified;

	public void notifySignal() {
		synchronized (this) {
			notified = true;
			notify();
		}
	}

	public void waitSignal(long waitTime) {
		if (waitTime <= 0L) {
			return;
		}
		synchronized (this) {
			if (!notified) {
				try {
					wait(waitTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					LOGGER.warn("TimeSignal wait interrupted", e);
				}
			}
			notified = false;
		}
	}
}

package threadtutil.timer.model;

import threadtutil.thread.Task;
import threadtutil.timer.Runner;

/**
 * 串行定时节点：实现 Task，按 groupId 投递到 ExecutorPool 串行队列。
 */
public class SerialTimeNode<T> extends TimeNode<T> implements Task {
	private final int groupId;

	public SerialTimeNode(int groupId, int id, Runner<T> runner, T param, long delay, long interval) {
		this(groupId, id, runner, param, delay, interval, -1);
	}

	public SerialTimeNode(int groupId, int id, Runner<T> runner, T param, long delay, long interval, int count) {
		super(id, runner, param, delay, interval, count);
		this.groupId = groupId;
	}

	@Override
	public int groupId() {
		return this.groupId;
	}
}

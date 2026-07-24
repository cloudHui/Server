package threadtutil.thread;

/**
 * 可串行调度的任务：groupId 相同则进入同一队列顺序执行。
 */
public interface Task extends Runnable {

	int groupId();
}

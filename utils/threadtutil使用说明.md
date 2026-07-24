# threadtutil 使用说明

`threadtutil` 是 utils 模块内的通用线程与定时工具包，供 gate / lobby / game / robot 等进程复用。

## 包结构

| 包路径 | 作用 |
|--------|------|
| `threadtutil.thread` | 固定线程池、按 groupId 串行执行 |
| `threadtutil.timer` | 延迟 / 周期 / 有限次定时器 |
| `threadtutil.lock` | 定时器等待唤醒信号 |
| `threadtutil.utils` | 时间与默认并发度工具 |

## 1. ExecutorPool：业务线程池

```java
// 名称、线程数、有界队列容量
ExecutorPool pool = new ExecutorPool("Game-Player", 32, 100000);

// 普通并行任务
pool.execute(() -> handlePlayerRequest(msg));

// 返回 CompletableFuture 的并行任务
pool.run(() -> doWork()).whenComplete((v, e) -> { /* ... */ });

// 同 groupId 串行（例如同一玩家、同一桌子）
pool.serialExecute(new Task() {
    @Override public int groupId() { return userId; }
    @Override public void run() { updateUserState(); }
});
```

要点：

- 线程数 `<=0` 时使用 `TimeUtils.PROCESS_NUMBER`（默认 CPU*2）。
- 队列满时采用 `CallerRunsPolicy`，由提交线程执行，形成反压，避免直接丢弃。
- `serialExecute` 用 `Math.floorMod(groupId, poolSize)` 选队列，保证同组顺序。

## 2. Timer：定时调度

```java
ExecutorPool pool = new ExecutorPool("Lobby");
Timer timer = new Timer().setRunners(pool);

// delay / interval 单位：毫秒；count=-1 表示无限次
// runner 返回 true 表示提前结束
timer.register(1000, 5000, -1, param -> {
    heartbeat(param);
    return false;
}, ctx);

// 串行定时：同一 groupId 的回调进入串行队列
timer.registerSerial(tableIdHash, 0, 200, -1, table -> {
    table.tick();
    return false;
}, table);

int nodeId = timer.registerSerialWithId(groupId, 0, 1000, 10, runner, param);
timer.unregister(nodeId);

// 进程退出
timer.stop();
pool.shutdown();
```

要点：

- 调度线程只负责到期检测，业务在 `ExecutorPool` 中执行，避免阻塞调度循环。
- `SerialTimeNode` 会走 `serialExecute`，适合“同桌 / 同玩家”周期逻辑。

## 3. DisorderTimer：自定义执行器

当执行器不是 `ExecutorPool` 时使用：

```java
DisorderTimer timer = new DisorderTimer();
timer.setRunners(task -> CompletableFuture.runAsync(task));
timer.register(1, 5, -1, param -> false, null); // 参数单位：秒
```

## 4. 在 Game 中的推荐用法

Game 进程请通过 `GameThreadPoolManager` 获取池，不要再各自 `new ExecutorPool`：

```java
GameThreadPoolManager pools = Game.getInstance().getThreadPoolManager();

// 玩家请求
pools.playerPool().execute(runnable);

// 同桌串行 + 周期调度
pools.registerTable(tableId);
pools.submitTable(tableId, () -> table.onAction(msg));
ScheduledFuture<?> loop = pools.scheduleTable(tableId, table::tick, 1000, 200);

// 桌生命周期 / 全局索引（单线程）
pools.submitTableManager(() -> tableManager.createTable(roomId, role));

// 数据库
pools.databasePool().execute(() -> scoreRepository.save(row));
```

## 5. 性能与使用约束

1. **网络线程不跑重逻辑**：只做解码与投递，状态修改进对应池。
2. **同资源同 groupId**：需要互斥的状态变更必须串行，避免锁竞争。
3. **勿在串行任务里阻塞 IO**：数据库走 database 池；远程调用注意超时。
4. **定时回调要短**：长任务应再投递到业务池，防止拖慢同组后续任务。
5. **关闭顺序**：先 `timer.stop()`，再 `pool.shutdown()` / `GameThreadPoolManager.shutdown()`。

## 6. 常见问题

**Q: 为什么同桌任务偶尔乱序？**  
A: 检查是否混用了 `execute` 与 `submitTable`/`serialExecute`。只有串行 API 保证顺序。

**Q: 队列满了会怎样？**  
A: 提交线程自己执行任务（CallerRuns），表现为调用方变慢，而不是静默丢任务。

**Q: `groupId` 用负数可以吗？**  
A: 可以。内部使用 `floorMod`，不会因 `Integer.MIN_VALUE` 产生负索引。

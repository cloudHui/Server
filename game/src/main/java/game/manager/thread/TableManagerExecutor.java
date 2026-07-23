package game.manager.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 桌子生命周期和桌子索引使用的独立单线程执行器。 */
public class TableManagerExecutor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r ->
            new Thread(r, "TableManager"));

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> runTask(task, future));
        return future;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private <T> void runTask(Callable<T> task, CompletableFuture<T> future) {
        try {
            future.complete(task.call());
        } catch (Throwable error) {
            future.completeExceptionally(error);
        }
    }
}

package me.sunmc.vw.task;

import me.sunmc.vw.VaultWeaponsPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Manages async and scheduled tasks for VaultWeapons
 */
public class AsyncTaskManager {

    private final VaultWeaponsPlugin plugin;
    private final ScheduledExecutorService scheduledExecutor;

    public AsyncTaskManager(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.scheduledExecutor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread thread = new Thread(r, "VaultWeapons-Scheduler");
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * Run a task asynchronously
     *
     * @param runnable The task to run
     * @return A CompletableFuture
     */
    @NotNull
    public CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, plugin.getExecutorService());
    }

    /**
     * Run a task asynchronously and return a result
     *
     * @param supplier The supplier to run
     * @param <T>      The return type
     * @return A CompletableFuture with the result
     */
    @NotNull
    public <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, plugin.getExecutorService());
    }

    /**
     * Run a task on the main server thread
     *
     * @param runnable The task to run
     */
    public void runSync(@NotNull Runnable runnable) {
        plugin.runSync(runnable);
    }

    /**
     * Run a task asynchronously after a delay
     *
     * @param runnable The task to run
     * @param delay    The delay in milliseconds
     * @return A ScheduledFuture
     */
    @NotNull
    public ScheduledFuture<?> runAsyncLater(@NotNull Runnable runnable, long delay) {
        return scheduledExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a task asynchronously repeatedly
     *
     * @param runnable     The task to run
     * @param initialDelay The initial delay in milliseconds
     * @param period       The period between executions in milliseconds
     * @return A ScheduledFuture
     */
    @NotNull
    public ScheduledFuture<?> runAsyncTimer(@NotNull Runnable runnable, long initialDelay, long period) {
        return scheduledExecutor.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a task on the main thread after a delay
     *
     * @param runnable The task to run
     * @param delay    The delay in ticks (20 ticks = 1 second)
     */
    public void runSyncLater(@NotNull Runnable runnable, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
    }

    /**
     * Run a task on the main thread repeatedly
     *
     * @param runnable     The task to run
     * @param initialDelay The initial delay in ticks
     * @param period       The period between executions in ticks
     */
    public void runSyncTimer(@NotNull Runnable runnable, long initialDelay, long period) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, initialDelay, period);
    }

    /**
     * Shutdown the scheduled executor
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

package me.sunmc.vw;

import me.sunmc.vw.command.MainCommand;
import me.sunmc.vw.command.RecipeCommand;
import me.sunmc.vw.config.ConfigManager;
import me.sunmc.vw.gui.GUIManager;
import me.sunmc.vw.listener.CraftListener;
import me.sunmc.vw.listener.WeaponAbilityListener;
import me.sunmc.vw.task.AsyncTaskManager;
import me.sunmc.vw.weapon.WeaponManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * VaultWeapons - A powerful, async, fully configurable weapons plugin for Paper 1.21+
 *
 * @author SunMC
 */
public class VaultWeaponsPlugin extends JavaPlugin {

    private static VaultWeaponsPlugin instance;

    private ConfigManager configManager;
    private WeaponManager weaponManager;
    private GUIManager guiManager;
    private AsyncTaskManager asyncTaskManager;
    private ExecutorService executorService;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize executor service for multi-threaded operations
        executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread thread = new Thread(r, "VaultWeapons-Worker");
                    thread.setDaemon(true);
                    return thread;
                }
        );

        this.configManager = new ConfigManager(this);
        this.weaponManager = new WeaponManager(this);
        this.guiManager = new GUIManager(this);
        this.asyncTaskManager = new AsyncTaskManager(this);

        CompletableFuture.runAsync(() -> {
            configManager.loadConfigs();
            weaponManager.loadWeapons();
        }, executorService).thenRunAsync(() -> {
            getServer().getPluginManager().registerEvents(new CraftListener(this), this);
            getServer().getPluginManager().registerEvents(new WeaponAbilityListener(this), this);
            getServer().getPluginManager().registerEvents(guiManager, this);

            MainCommand mainCommand = new MainCommand(this);
            RecipeCommand recipeCommand = new RecipeCommand(this);

            Objects.requireNonNull(getCommand("vaultweapons")).setExecutor(mainCommand);
            Objects.requireNonNull(getCommand("vaultweapons")).setTabCompleter(mainCommand);
            Objects.requireNonNull(getCommand("recipe")).setExecutor(recipeCommand);
            Objects.requireNonNull(getCommand("recipe")).setTabCompleter(recipeCommand);

            getLogger().info("VaultWeapons has been enabled successfully!");
        }, runnable -> getServer().getScheduler().runTask(this, runnable)).exceptionally(throwable -> {
            getLogger().log(Level.SEVERE, "Failed to initialize VaultWeapons", throwable);
            getServer().getPluginManager().disablePlugin(this);
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        getServer().getScheduler().cancelTasks(this);

        getLogger().info("VaultWeapons has been disabled!");
    }

    /**
     * Run a task asynchronously using the plugin's executor service
     *
     * @param runnable The task to run
     * @return A CompletableFuture for the async operation
     */
    public CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executorService);
    }

    /**
     * Run a task on the main server thread
     *
     * @param runnable The task to run
     */
    public void runSync(@NotNull Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    /**
     * Run a task asynchronously after a delay
     *
     * @param runnable The task to run
     * @param delay    The delay in ticks
     * @return The BukkitTask
     */
    public BukkitTask runAsyncLater(@NotNull Runnable runnable, long delay) {
        return getServer().getScheduler().runTaskLaterAsynchronously(this, runnable, delay);
    }

    /**
     * Run a task asynchronously repeatedly
     *
     * @param runnable The task to run
     * @param delay    The initial delay in ticks
     * @param period   The period between executions in ticks
     * @return The BukkitTask
     */
    public BukkitTask runAsyncTimer(@NotNull Runnable runnable, long delay, long period) {
        return getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, delay, period);
    }

    @NotNull
    public static VaultWeaponsPlugin getInstance() {
        return instance;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public WeaponManager getWeaponManager() {
        return weaponManager;
    }

    @NotNull
    public GUIManager getGuiManager() {
        return guiManager;
    }

    @NotNull
    public AsyncTaskManager getAsyncTaskManager() {
        return asyncTaskManager;
    }

    @NotNull
    public ExecutorService getExecutorService() {
        return executorService;
    }
}
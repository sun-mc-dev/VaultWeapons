package me.sunmc.vw;

import me.sunmc.vw.command.VWCommand;
import me.sunmc.vw.config.WeaponConfigLoader;
import me.sunmc.vw.listener.CraftListener;
import me.sunmc.vw.listener.GUIListener;
import me.sunmc.vw.listener.HitListener;
import me.sunmc.vw.listener.KillListener;
import me.sunmc.vw.manager.CraftTracker;
import me.sunmc.vw.manager.RecipeManager;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class VaultWeapons extends JavaPlugin {

    private static VaultWeapons instance;

    private WeaponConfigLoader configLoader;
    private WeaponManager weaponManager;
    private RecipeManager recipeManager;
    private CraftTracker craftTracker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        CompletableFuture.runAsync(this::loadAsync)
                .thenRun(() -> getServer().getScheduler().runTask(this, this::initSync))
                .exceptionally(ex -> {
                    getLogger().log(Level.SEVERE, "[VaultWeapons] Fatal startup error", ex);
                    getServer().getPluginManager().disablePlugin(this);
                    return null;
                });
    }

    private void loadAsync() {
        configLoader = new WeaponConfigLoader(this);
        configLoader.load();
        craftTracker = new CraftTracker(this);
    }

    private void initSync() {
        weaponManager = new WeaponManager(this, configLoader);
        recipeManager = new RecipeManager(this, configLoader, weaponManager);
        recipeManager.registerAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new CraftListener(this, weaponManager, craftTracker), this);
        pm.registerEvents(new KillListener(this, weaponManager), this);
        pm.registerEvents(new HitListener(this, weaponManager), this);
        pm.registerEvents(new GUIListener(weaponManager), this);

        var vwCmd = getCommand("vaultweapons");
        if (vwCmd != null) {
            var cmd = new VWCommand(this, weaponManager, configLoader, craftTracker);
            vwCmd.setExecutor(cmd);
            vwCmd.setTabCompleter(cmd);
        }

        startLogo();
    }

    private void startLogo() {
        String[] logo = {
                " ",
                "  &6 ____   ____            .__   __   __      __                                        ",
                "  &6 \\   \\ /   /____   __ __|  | /  |_/  \\    /  \\ ____ _____  ______   ____   ____  ______",
                "  &6  \\   Y   /\\__  \\ |  |  \\  | \\   __\\   \\/\\/   // __ \\\\__  \\ \\____ \\ /  _ \\ /    \\ /  ___/",
                "  &6   \\     /  / __ \\|  |  /  |_ |  |  \\        /\\  ___/ / __ \\|  |_> >  <_> )   |  \\\\___ \\ ",
                "  &6    \\___/  (____  /____/|____/|__|   \\__/\\  /  \\___  >____  /   __/ \\____/|___|  /____  >",
                "  &6               \\/                        \\/       \\/     \\/ |__|               \\/     \\/ ",
                " ",
                "  &7Version: &f" + getPluginMeta().getVersion() + "  &7Author: &f" + String.join(", ", getPluginMeta().getAuthors()),
                " "
        };

        for (String line : logo) {
            getComponentLogger().info(TextUtil.parse(line));
        }

        getComponentLogger().info(Component.text("  ")
                .append(Component.text(configLoader.getWeapons().size() + " weapon(s) loaded and ready.",
                        NamedTextColor.GREEN)));
    }

    @Override
    public void onDisable() {
        if (craftTracker != null) craftTracker.save();
        getComponentLogger().info(Component.text("VaultWeapons disabled.", NamedTextColor.RED));
    }

    public void reloadPlugin() {
        reloadConfig();
        CompletableFuture.runAsync(() -> {
                    configLoader.load();
                    craftTracker.load();
                })
                .thenRun(() -> getServer().getScheduler().runTask(this, () -> {
                    recipeManager.unregisterAll();
                    recipeManager.registerAll();
                    getLogger().info("VaultWeapons reloaded — "
                            + configLoader.getWeapons().size() + " weapon(s) loaded.");
                }))
                .exceptionally(ex -> {
                    getLogger().log(Level.SEVERE, "[VaultWeapons] Reload error", ex);
                    return null;
                });
    }

    public WeaponConfigLoader getConfigLoader() {
        return configLoader;
    }
}
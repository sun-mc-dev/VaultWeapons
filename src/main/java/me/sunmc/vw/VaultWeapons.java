package me.sunmc.vw;

import me.sunmc.vw.command.VWCommand;
import me.sunmc.vw.config.WeaponConfigLoader;
import me.sunmc.vw.listener.CraftListener;
import me.sunmc.vw.listener.GUIListener;
import me.sunmc.vw.listener.KillListener;
import me.sunmc.vw.manager.RecipeManager;
import me.sunmc.vw.manager.WeaponManager;
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
    }

    private void initSync() {
        weaponManager = new WeaponManager(this, configLoader);
        recipeManager = new RecipeManager(this, configLoader, weaponManager);
        recipeManager.registerAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new CraftListener(this, weaponManager), this);
        pm.registerEvents(new KillListener(this, weaponManager), this);
        pm.registerEvents(new GUIListener(weaponManager), this);

        var vwCmd = getCommand("vaultweapons");
        if (vwCmd != null) {
            var cmd = new VWCommand(this, weaponManager, configLoader);
            vwCmd.setExecutor(cmd);
            vwCmd.setTabCompleter(cmd);
        }

        getComponentLogger().info(Component.text("VaultWeapons enabled — "
                + configLoader.getWeapons().size() + " weapon(s) loaded.", NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        getComponentLogger().info(Component.text("VaultWeapons disabled.", NamedTextColor.RED));
    }

    public void reloadPlugin() {
        reloadConfig();
        CompletableFuture.runAsync(() -> configLoader.load())
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
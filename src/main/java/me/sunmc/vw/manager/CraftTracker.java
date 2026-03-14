package me.sunmc.vw.manager;

import me.sunmc.vw.VaultWeapons;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CraftTracker {

    private final VaultWeapons plugin;
    private final File dataFile;
    // UUID -> set of weapon IDs that player has crafted
    private final Map<UUID, Set<String>> craftedMap = new ConcurrentHashMap<>();

    public CraftTracker(@NotNull VaultWeapons plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "craft_data.yml");
        load();
    }

    public boolean hasCrafted(UUID playerId, String weaponId) {
        Set<String> set = craftedMap.get(playerId);
        return set != null && set.contains(weaponId);
    }

    public void markCrafted(UUID playerId, String weaponId) {
        craftedMap.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(weaponId);
        saveAsync();
    }

    /**
     * Remove craft record for ONE weapon across all players.
     */
    public void resetWeapon(String weaponId) {
        for (Set<String> set : craftedMap.values()) set.remove(weaponId);
        saveAsync();
    }

    /**
     * Remove ALL craft records.
     */
    public void resetAll() {
        craftedMap.clear();
        saveAsync();
    }

    public void load() {
        craftedMap.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidStr : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Set<String> weapons = ConcurrentHashMap.newKeySet();
                weapons.addAll(cfg.getStringList(uuidStr));
                craftedMap.put(uuid, weapons);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : craftedMap.entrySet()) {
            cfg.set(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save craft_data.yml", e);
        }
    }
}
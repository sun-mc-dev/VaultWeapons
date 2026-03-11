package me.sunmc.vw.config;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.model.AbilityType;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.model.WeaponRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class WeaponConfigLoader {

    private final VaultWeapons plugin;
    private final Map<String, WeaponDefinition> weapons = new ConcurrentHashMap<>();

    public WeaponConfigLoader(VaultWeapons plugin) {
        this.plugin = plugin;
    }

    /**
     * Thread-safe; may be called from any thread.
     */
    public void load() {
        weapons.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("weapons");
        if (root == null) {
            plugin.getLogger().warning("No 'weapons' section found in config.yml!");
            return;
        }

        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection ws = root.getConfigurationSection(key);
                if (ws == null) continue;

                String material = ws.getString("type", "DIAMOND_SWORD");
                String name = ws.getString("name", key);
                List<String> lore = ws.getStringList("lore");
                int mdCfg = ws.getInt("custom_model_data", 0);


                Map<String, Integer> enchants = new HashMap<>();
                ConfigurationSection enchSec = ws.getConfigurationSection("enchantments");
                if (enchSec != null) {
                    for (String ek : enchSec.getKeys(false)) {
                        enchants.put(ek.toLowerCase(Locale.ROOT), enchSec.getInt(ek));
                    }
                }


                AbilityType abilityType = AbilityType.NONE;
                Map<String, Object> abilityCfg = new HashMap<>();
                ConfigurationSection abSec = ws.getConfigurationSection("ability");
                if (abSec != null) {
                    String typeStr = abSec.getString("type", "NONE");
                    try {
                        abilityType = AbilityType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning(
                                "Unknown ability type '" + typeStr + "' for weapon " + key);
                    }
                    for (String ak : abSec.getKeys(false)) {
                        abilityCfg.put(ak, abSec.get(ak));
                    }
                }


                WeaponRecipe recipe = null;
                ConfigurationSection recSec = ws.getConfigurationSection("recipe");
                if (recSec != null) {
                    String rType = recSec.getString("type", "SHAPED");
                    List<String> shape = recSec.getStringList("shape");
                    Map<Character, String> ings = new HashMap<>();
                    ConfigurationSection ingsSec = recSec.getConfigurationSection("ingredients");
                    if (ingsSec != null) {
                        for (String ik : ingsSec.getKeys(false)) {
                            if (!ik.isEmpty()) {
                                ings.put(ik.charAt(0), ingsSec.getString(ik, "AIR"));
                            }
                        }
                    }
                    recipe = new WeaponRecipe(rType, shape, ings);
                }

                weapons.put(key, new WeaponDefinition(
                        key, material, name, lore, mdCfg,
                        enchants, abilityType, abilityCfg, recipe
                ));
                plugin.getLogger().info("Loaded weapon: " + key);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load weapon: " + key, e);
            }
        }
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView Map<String, WeaponDefinition> getWeapons() {
        return Collections.unmodifiableMap(weapons);
    }

    public WeaponDefinition getWeapon(String id) {
        return weapons.get(id);
    }
}
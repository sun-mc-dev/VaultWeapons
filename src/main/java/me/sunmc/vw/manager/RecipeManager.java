package me.sunmc.vw.manager;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.config.WeaponConfigLoader;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.model.WeaponRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RecipeManager {

    private final VaultWeapons plugin;
    private final WeaponConfigLoader configLoader;
    private final WeaponManager weaponManager;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public RecipeManager(VaultWeapons plugin, WeaponConfigLoader configLoader,
                         WeaponManager weaponManager) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.weaponManager = weaponManager;
    }

    /**
     * Must be called on the main thread.
     */
    public void registerAll() {
        for (WeaponDefinition def : configLoader.getWeapons().values()) {
            try {
                registerWeaponRecipe(def);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to register recipe for: " + def.getId(), e);
            }
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    private void registerWeaponRecipe(@NotNull WeaponDefinition def) {
        WeaponRecipe recipe = def.getRecipe();
        if (recipe == null) return;

        ItemStack result = weaponManager.createWeapon(def.getId());
        if (result == null) {
            plugin.getLogger().warning("Could not create weapon item for recipe: " + def.getId());
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, def.getId());

        if ("SHAPED".equalsIgnoreCase(recipe.type())) {
            List<String> shape = recipe.shape();
            if (shape.isEmpty()) return;

            ShapedRecipe shaped = new ShapedRecipe(key, result);
            shaped.shape(shape.toArray(new String[0]));

            for (Map.Entry<Character, String> entry : recipe.ingredients().entrySet()) {
                char c = entry.getKey();
                String mat = entry.getValue();
                boolean inShape = shape.stream().anyMatch(row -> row.indexOf(c) >= 0);
                if (!inShape) continue;
                try {
                    shaped.setIngredient(c, Material.valueOf(mat.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(
                            "Invalid ingredient '" + mat + "' in recipe " + def.getId());
                }
            }

            Bukkit.addRecipe(shaped);
            registered.add(key);
            plugin.getLogger().info("Registered recipe for: " + def.getId());
        } else {
            plugin.getLogger().warning(
                    "Unsupported recipe type '" + recipe.type() + "' for: " + def.getId());
        }
    }
}
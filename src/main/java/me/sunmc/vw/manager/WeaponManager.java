package me.sunmc.vw.manager;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.config.WeaponConfigLoader;
import me.sunmc.vw.model.AbilityType;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WeaponManager {
    public final NamespacedKey KEY_WEAPON_ID;
    public final NamespacedKey KEY_KILLS;
    public final NamespacedKey KEY_SHARPNESS;

    private final VaultWeapons plugin;
    private final WeaponConfigLoader configLoader;

    public WeaponManager(VaultWeapons plugin, WeaponConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.KEY_WEAPON_ID = new NamespacedKey(plugin, "weapon_id");
        this.KEY_KILLS = new NamespacedKey(plugin, "kills");
        this.KEY_SHARPNESS = new NamespacedKey(plugin, "sharpness_level");
    }

    /**
     * Creates a fully configured ItemStack for the given weapon ID, or null on error.
     */
    public @Nullable ItemStack createWeapon(String weaponId) {
        WeaponDefinition def = configLoader.getWeapon(weaponId);
        if (def == null) return null;

        Material material;
        try {
            material = Material.valueOf(def.getMaterialType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(
                    "Invalid material '" + def.getMaterialType() + "' for weapon " + weaponId);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;


        meta.displayName(TextUtil.parse(def.getDisplayName()));


        List<Component> loreComponents = new ArrayList<>();
        for (String line : def.getLore()) {
            loreComponents.add(TextUtil.parse(line));
        }
        meta.lore(loreComponents);

        // Custom model data (resource-pack texture support)
        if (def.getCustomModelData() > 0) {
            meta.setCustomModelData(def.getCustomModelData());
        }


        for (Map.Entry<String, Integer> entry : def.getEnchantments().entrySet()) {
            Enchantment ench = resolveEnchantment(entry.getKey());
            if (ench != null) {
                meta.addEnchant(ench, entry.getValue(), true);
            } else {
                plugin.getLogger().warning(
                        "Unknown enchantment '" + entry.getKey() + "' for weapon " + weaponId);
            }
        }

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_WEAPON_ID, PersistentDataType.STRING, weaponId);
        pdc.set(KEY_KILLS, PersistentDataType.INTEGER, 0);


        if (def.getAbilityType() == AbilityType.SWORD_SHARPNESS) {
            int initial = def.getEnchantments().getOrDefault("sharpness", 1);
            pdc.set(KEY_SHARPNESS, PersistentDataType.INTEGER, initial);
        }

        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> getWeaponId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(KEY_WEAPON_ID, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    public boolean isVaultWeapon(ItemStack item) {
        return getWeaponId(item).isPresent();
    }

    public int getKills(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer v = meta.getPersistentDataContainer().get(KEY_KILLS, PersistentDataType.INTEGER);
        return v != null ? v : 0;
    }

    /**
     * Increments kill count stored in the item's PDC. Caller must set the item back in inventory.
     */
    public void addKill(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int kills = meta.getPersistentDataContainer()
                .getOrDefault(KEY_KILLS, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(KEY_KILLS, PersistentDataType.INTEGER, kills + 1);
        item.setItemMeta(meta);
    }

    public int getSharpnessLevel(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return 1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 1;
        Integer v = meta.getPersistentDataContainer().get(KEY_SHARPNESS, PersistentDataType.INTEGER);
        return v != null ? v : 1;
    }

    /**
     * Updates both the PDC sharpness value and the live enchantment on the item.
     */
    public void setSharpnessLevel(@NotNull ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_SHARPNESS, PersistentDataType.INTEGER, level);
        Enchantment sharp = resolveEnchantment("sharpness");
        if (sharp != null) meta.addEnchant(sharp, level, true);
        item.setItemMeta(meta);
    }

    public static Enchantment resolveEnchantment(@NotNull String name) {
        String key = name.toLowerCase().replace(" ", "_");
        key = switch (key) {
            case "damage_all" -> "sharpness";
            case "sweeping", "sweeping_edge" -> "sweeping_edge";
            default -> key;
        };
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }
}
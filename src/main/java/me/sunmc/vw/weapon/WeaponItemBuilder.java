package me.sunmc.vw.weapon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for building weapon ItemStacks
 */
public class WeaponItemBuilder {
    public static final NamespacedKey WEAPON_ID_KEY = new NamespacedKey("vaultweapons", "weapon_id");
    public static final NamespacedKey KILLS_KEY = new NamespacedKey("vaultweapons", "kills");

    /**
     * Create an ItemStack for a weapon
     *
     * @param weaponData The weapon data
     * @param kills      The number of kills
     * @return The created ItemStack
     */
    @NotNull
    public static ItemStack create(@NotNull WeaponData weaponData, int kills) {
        ItemStack item = new ItemStack(weaponData.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Component displayName = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(weaponData.getName());
            meta.displayName(displayName);


            List<Component> lore = new ArrayList<>();
            for (String line : weaponData.getLore()) {
                String processedLine = processPlaceholders(line, weaponData, kills);
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(processedLine));
            }
            meta.lore(lore);

            if (weaponData.getCustomModelData() > 0) {
                meta.setCustomModelData(weaponData.getCustomModelData());
            }

            meta.setUnbreakable(weaponData.isUnbreakable());

            for (Map.Entry<Enchantment, Integer> entry : weaponData.getEnchants().entrySet()) {

                if (meta.getEnchants().isEmpty() || canEnchant(meta, entry.getKey())) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(WEAPON_ID_KEY, PersistentDataType.STRING, weaponData.getId());
            pdc.set(KILLS_KEY, PersistentDataType.INTEGER, kills);

            applyAbilityEnchants(meta, weaponData, kills);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Process placeholders in lore
     *
     * @param line       The line to process
     * @param weaponData The weapon data
     * @param kills      The number of kills
     * @return The processed line
     */
    @NotNull
    private static String processPlaceholders(@NotNull String line, @NotNull WeaponData weaponData, int kills) {
        String result = line;

        result = result.replace("%kills%", String.valueOf(kills));

        if (weaponData.hasAbility()) {
            WeaponAbility ability = weaponData.getAbility();

            switch (Objects.requireNonNull(ability).getEffectType()) {
                case RELOAD_DECREASE:
                    int reloadBonus = Math.min(kills * ability.getValue(), ability.getMaxValue());
                    double bonusPercent = (reloadBonus / 20.0) * 100; // Convert ticks to percentage
                    result = result.replace("%bonus%", String.format("%.0f", bonusPercent));
                    result = result.replace("%sharpness%", "0");
                    break;

                case SHARPNESS_INCREASE:
                    int sharpness = Math.min(kills * ability.getValue(), ability.getMaxValue());
                    result = result.replace("%sharpness%", String.valueOf(sharpness + 1)); // +1 for base sharpness
                    result = result.replace("%bonus%", "0");
                    break;

                default:
                    result = result.replace("%bonus%", "0");
                    result = result.replace("%sharpness%", "0");
            }
        } else {
            result = result.replace("%bonus%", "0");
            result = result.replace("%sharpness%", "0");
        }

        return result;
    }

    /**
     * Apply ability-based enchantments
     *
     * @param meta       The item meta
     * @param weaponData The weapon data
     * @param kills      The number of kills
     */
    private static void applyAbilityEnchants(@NotNull ItemMeta meta, @NotNull WeaponData weaponData, int kills) {
        if (!weaponData.hasAbility()) {
            return;
        }

        WeaponAbility ability = weaponData.getAbility();

        try {
            assert ability != null;
            if (ability.getEffectType() == WeaponAbility.EffectType.SHARPNESS_INCREASE) {
                int extraSharpness = ability.calculateEffectiveValue(kills);
                int baseSharpness = weaponData.getEnchants().getOrDefault(Enchantment.SHARPNESS, 0);
                int totalSharpness = baseSharpness + extraSharpness;

                meta.removeEnchant(Enchantment.SHARPNESS);
                meta.addEnchant(Enchantment.SHARPNESS, totalSharpness, true);
            }
        } catch (Exception e) {
            throw new NullPointerException();
        }
    }

    /**
     * Check if an enchantment can be applied
     *
     * @param meta    The item meta
     * @param enchant The enchantment to check
     * @return true if the enchantment can be applied
     */
    private static boolean canEnchant(@NotNull ItemMeta meta, @NotNull Enchantment enchant) {
        for (Enchantment existing : meta.getEnchants().keySet()) {
            if (existing.conflictsWith(enchant)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if an ItemStack is a VaultWeapon
     *
     * @param item The item to check
     * @return true if the item is a VaultWeapon
     */
    public static boolean isVaultWeapon(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(WEAPON_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Get the weapon ID from an ItemStack
     *
     * @param item The item to check
     * @return The weapon ID, or null if not a VaultWeapon
     */
    public static @Nullable String getWeaponId(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(WEAPON_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Get the kill count from an ItemStack
     *
     * @param item The item to check
     * @return The kill count, or 0 if not a VaultWeapon
     */
    public static int getKills(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer kills = pdc.get(KILLS_KEY, PersistentDataType.INTEGER);
        return kills != null ? kills : 0;
    }

    /**
     * Set the kill count on an ItemStack
     *
     * @param item  The item to modify
     * @param kills The new kill count
     * @return The modified ItemStack
     */
    @NotNull
    public static ItemStack setKills(@NotNull ItemStack item, int kills) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KILLS_KEY, PersistentDataType.INTEGER, kills);


        String weaponId = pdc.get(WEAPON_ID_KEY, PersistentDataType.STRING);
        if (weaponId != null) {
            // Update lore with new kill count (handled by weapon manager when needed)
            updateLoreWithKills(meta, weaponId, kills);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Update the lore with new kill count
     *
     * @param meta     The item meta
     * @param weaponId The weapon ID
     * @param kills    The kill count
     */
    private static void updateLoreWithKills(@NotNull ItemMeta meta, @NotNull String weaponId, int kills) {
        // This will be handled by the weapon manager to get fresh lore from config
        // For now, just update the persistent data
    }

    /**
     * Create a display item for GUI
     *
     * @param material The material
     * @param name     The display name
     * @param lore     The lore
     * @return The created ItemStack
     */
    @NotNull
    public static ItemStack createDisplayItem(@NotNull Material material, @NotNull String name, @NotNull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(loreComponents);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a filler item for GUI backgrounds
     *
     * @return The filler ItemStack
     */
    @NotNull
    public static ItemStack createFiller() {
        return createDisplayItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }
}

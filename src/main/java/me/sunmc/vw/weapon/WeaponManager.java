package me.sunmc.vw.weapon;

import me.sunmc.vw.VaultWeaponsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all weapons in the plugin
 */
public class WeaponManager {

    private final VaultWeaponsPlugin plugin;
    private final Map<String, WeaponData> weapons;
    private final Map<String, NamespacedKey> recipeKeys;

    public WeaponManager(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.weapons = new ConcurrentHashMap<>();
        this.recipeKeys = new ConcurrentHashMap<>();
    }

    /**
     * Load all weapons from configuration
     */
    public void loadWeapons() {
        weapons.clear();

        for (NamespacedKey key : recipeKeys.values()) {
            plugin.getServer().removeRecipe(key);
        }
        recipeKeys.clear();

        FileConfiguration weaponsConfig = plugin.getConfigManager().getWeaponsConfig();
        ConfigurationSection weaponsSection = weaponsConfig.getConfigurationSection("weapons");

        if (weaponsSection == null) {
            plugin.getLogger().warning("No weapons found in configuration!");
            return;
        }

        for (String weaponId : weaponsSection.getKeys(false)) {
            try {
                WeaponData weaponData = loadWeapon(weaponId, weaponsSection.getConfigurationSection(weaponId));
                if (weaponData != null) {
                    weapons.put(weaponId, weaponData);
                    registerRecipe(weaponData);
                    plugin.getLogger().info("Loaded weapon: " + weaponId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load weapon: " + weaponId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + weapons.size() + " weapons!");
    }

    /**
     * Load a single weapon from configuration
     *
     * @param id      The weapon ID
     * @param section The configuration section
     * @return The loaded WeaponData, or null if invalid
     */
    @Nullable
    private WeaponData loadWeapon(@NotNull String id, @Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String name = section.getString("name", "&f" + id);
        String description = section.getString("description", "");
        String materialName = section.getString("material", "DIAMOND_SWORD").toUpperCase();
        Material material;

        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for weapon " + id + ": " + materialName);
            return null;
        }

        int customModelData = section.getInt("custom-model-data", 0);
        boolean unbreakable = section.getBoolean("unbreakable", false);
        List<String> lore = section.getStringList("lore");

        Map<Enchantment, Integer> enchants = new HashMap<>();
        ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
        if (enchantsSection != null) {
            for (String enchantName : enchantsSection.getKeys(false)) {
                Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantName.toLowerCase()));

                if (enchant != null) {
                    int level = enchantsSection.getInt(enchantName, 1);
                    enchants.put(enchant, level);
                } else {
                    plugin.getLogger().warning("Invalid enchantment for weapon " + id + ": " + enchantName);
                }
            }
        }

        // Load ability
        WeaponAbility ability = null;
        ConfigurationSection abilitySection = section.getConfigurationSection("abilities");
        if (abilitySection != null) {
            ability = loadAbility(abilitySection);
        }

        // Load recipe
        WeaponRecipe recipe = null;
        ConfigurationSection recipeSection = section.getConfigurationSection("recipe");
        if (recipeSection != null) {
            recipe = loadRecipe(recipeSection);
        }

        // Load NBT data
        Map<String, Object> nbtData = new HashMap<>();
        ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
        if (nbtSection != null) {
            for (String key : nbtSection.getKeys(false)) {
                nbtData.put(key, nbtSection.get(key));
            }
        }

        return new WeaponData.Builder()
                .id(id)
                .name(name)
                .description(description)
                .material(material)
                .customModelData(customModelData)
                .unbreakable(unbreakable)
                .lore(lore)
                .enchants(enchants)
                .ability(ability)
                .recipe(recipe)
                .nbtData(nbtData)
                .build();
    }

    /**
     * Load ability from configuration
     *
     * @param section The configuration section
     * @return The loaded WeaponAbility
     */
    @NotNull
    private WeaponAbility loadAbility(@NotNull ConfigurationSection section) {
        String typeName = section.getString("type", "KILL_EFFECT").toUpperCase();
        String effectName = section.getString("effect", "CUSTOM").toUpperCase();

        WeaponAbility.TriggerType triggerType;
        WeaponAbility.EffectType effectType;

        try {
            triggerType = WeaponAbility.TriggerType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            triggerType = WeaponAbility.TriggerType.KILL_EFFECT;
        }

        try {
            effectType = WeaponAbility.EffectType.valueOf(effectName);
        } catch (IllegalArgumentException e) {
            effectType = WeaponAbility.EffectType.CUSTOM;
        }

        return new WeaponAbility.Builder()
                .triggerType(triggerType)
                .effectType(effectType)
                .value(section.getInt("value", 1))
                .maxValue(section.getInt("max-value", 10))
                .minValue(section.getInt("min-value", 0))
                .minReload(section.getInt("min-reload", 10))
                .build();
    }

    /**
     * Load recipe from configuration
     *
     * @param section The configuration section
     * @return The loaded WeaponRecipe
     */
    @NotNull
    private WeaponRecipe loadRecipe(@NotNull ConfigurationSection section) {
        List<String> shape = section.getStringList("shape");
        if (shape.isEmpty()) {
            shape = List.of("   ", "   ", "   ");
        }

        while (shape.size() < 3) {
            shape = new ArrayList<>(shape);
            shape.add("   ");
        }

        Map<Character, Material> ingredients = new HashMap<>();
        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                char c = key.charAt(0);
                String materialName = ingredientsSection.getString(key, "AIR").toUpperCase();
                try {
                    Material material = Material.valueOf(materialName);
                    ingredients.put(c, material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid ingredient material: " + materialName);
                }
            }
        }

        return new WeaponRecipe.Builder()
                .shape(shape)
                .ingredients(ingredients)
                .build();
    }

    /**
     * Register a recipe for a weapon
     *
     * @param weaponData The weapon data
     */
    private void registerRecipe(@NotNull WeaponData weaponData) {
        if (!weaponData.hasRecipe()) {
            return;
        }

        WeaponRecipe recipe = weaponData.getRecipe();
        NamespacedKey recipeKey = new NamespacedKey(plugin, "vaultweapon_" + weaponData.getId());
        recipeKeys.put(weaponData.getId(), recipeKey);

        ShapedRecipe shapedRecipe = new ShapedRecipe(recipeKey, weaponData.createItemStack(0));

        List<String> shape = null;
        if (recipe != null) {
            shape = recipe.getShape();
        }
        if (shape != null) {
            shapedRecipe.shape(shape.toArray(new String[0]));
        }

        if (recipe != null) {
            for (Map.Entry<Character, Material> entry : recipe.getIngredients().entrySet()) {
                shapedRecipe.setIngredient(entry.getKey(), entry.getValue());
            }
        }

        plugin.getServer().addRecipe(shapedRecipe);
    }

    /**
     * Get a weapon by ID
     *
     * @param id The weapon ID
     * @return The WeaponData, or null if not found
     */
    @Nullable
    public WeaponData getWeapon(@NotNull String id) {
        return weapons.get(id);
    }

    /**
     * Get all weapons
     *
     * @return A collection of all weapons
     */
    @NotNull
    public Collection<WeaponData> getAllWeapons() {
        return weapons.values();
    }

    /**
     * Get all weapon IDs
     *
     * @return A set of all weapon IDs
     */
    @NotNull
    public Set<String> getWeaponIds() {
        return weapons.keySet();
    }

    /**
     * Check if a weapon exists
     *
     * @param id The weapon ID
     * @return true if the weapon exists
     */
    public boolean hasWeapon(@NotNull String id) {
        return weapons.containsKey(id);
    }

    /**
     * Give a weapon to a player
     *
     * @param player   The player
     * @param weaponId The weapon ID
     * @param kills    The initial kill count
     * @param amount   The amount to give
     * @return true if successful
     */
    public boolean giveWeapon(@NotNull Player player, @NotNull String weaponId, int kills, int amount) {
        WeaponData weaponData = weapons.get(weaponId);
        if (weaponData == null) {
            return false;
        }

        ItemStack item = weaponData.createItemStack(kills);
        item.setAmount(amount);

        // Add to player inventory, drop if full
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        return true;
    }

    /**
     * Update weapon kills in an ItemStack
     *
     * @param item  The ItemStack
     * @param kills The new kill count
     * @return The updated ItemStack
     */
    @NotNull
    public ItemStack updateWeaponKills(@NotNull ItemStack item, int kills) {
        String weaponId = WeaponItemBuilder.getWeaponId(item);
        if (weaponId == null) {
            return item;
        }

        WeaponData weaponData = weapons.get(weaponId);
        if (weaponData == null) {
            return item;
        }

        return weaponData.createItemStack(kills);
    }
}

package me.sunmc.vw.weapon;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a weapon's configuration data
 */
public class WeaponData {

    private final String id;
    private final String name;
    private final String description;
    private final Material material;
    private final int customModelData;
    private final boolean unbreakable;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchants;
    private final WeaponAbility ability;
    private final WeaponRecipe recipe;
    private final Map<String, Object> nbtData;

    public WeaponData(@NotNull String id, @NotNull String name, @NotNull String description,
                      @NotNull Material material, int customModelData, boolean unbreakable,
                      @NotNull List<String> lore, @NotNull Map<Enchantment, Integer> enchants,
                      @Nullable WeaponAbility ability, @Nullable WeaponRecipe recipe,
                      @NotNull Map<String, Object> nbtData) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.material = material;
        this.customModelData = customModelData;
        this.unbreakable = unbreakable;
        this.lore = lore;
        this.enchants = enchants;
        this.ability = ability;
        this.recipe = recipe;
        this.nbtData = nbtData;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    @NotNull
    public List<String> getLore() {
        return lore;
    }

    @NotNull
    public Map<Enchantment, Integer> getEnchants() {
        return enchants;
    }

    @Nullable
    public WeaponAbility getAbility() {
        return ability;
    }

    @Nullable
    public WeaponRecipe getRecipe() {
        return recipe;
    }

    @NotNull
    public Map<String, Object> getNbtData() {
        return nbtData;
    }

    /**
     * Check if this weapon has an ability
     *
     * @return true if the weapon has an ability
     */
    public boolean hasAbility() {
        return ability != null;
    }

    /**
     * Check if this weapon has a recipe
     *
     * @return true if the weapon has a recipe
     */
    public boolean hasRecipe() {
        return recipe != null;
    }

    /**
     * Create an ItemStack builder for this weapon
     *
     * @param kills The number of kills for this weapon instance
     * @return The ItemStack
     */
    @NotNull
    public ItemStack createItemStack(int kills) {
        return WeaponItemBuilder.create(this, kills);
    }

    /**
     * Builder class for WeaponData
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private Material material;
        private int customModelData = 0;
        private boolean unbreakable = false;
        private List<String> lore = List.of();
        private Map<Enchantment, Integer> enchants = new ConcurrentHashMap<>();
        private WeaponAbility ability;
        private WeaponRecipe recipe;
        private Map<String, Object> nbtData = new ConcurrentHashMap<>();

        public Builder id(@NotNull String id) {
            this.id = id;
            return this;
        }

        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        public Builder description(@NotNull String description) {
            this.description = description;
            return this;
        }

        public Builder material(@NotNull Material material) {
            this.material = material;
            return this;
        }

        public Builder customModelData(int customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public Builder unbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public Builder lore(@NotNull List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder enchants(@NotNull Map<Enchantment, Integer> enchants) {
            this.enchants = enchants;
            return this;
        }

        public Builder addEnchant(@NotNull Enchantment enchant, int level) {
            this.enchants.put(enchant, level);
            return this;
        }

        public Builder ability(@Nullable WeaponAbility ability) {
            this.ability = ability;
            return this;
        }

        public Builder recipe(@Nullable WeaponRecipe recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder nbtData(@NotNull Map<String, Object> nbtData) {
            this.nbtData = nbtData;
            return this;
        }

        public Builder addNbtData(@NotNull String key, @NotNull Object value) {
            this.nbtData.put(key, value);
            return this;
        }

        @NotNull
        public WeaponData build() {
            return new WeaponData(id, name, description, material, customModelData,
                    unbreakable, lore, enchants, ability, recipe, nbtData);
        }
    }
}
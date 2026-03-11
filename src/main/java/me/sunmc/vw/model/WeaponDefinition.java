package me.sunmc.vw.model;

import java.util.List;
import java.util.Map;

public final class WeaponDefinition {

    private final String id;
    private final String materialType;
    private final String displayName;
    private final List<String> lore;
    private final int customModelData;
    private final Map<String, Integer> enchantments;
    private final AbilityType abilityType;
    private final Map<String, Object> abilityConfig;
    private final WeaponRecipe recipe;

    public WeaponDefinition(
            String id, String materialType, String displayName,
            List<String> lore, int customModelData,
            Map<String, Integer> enchantments,
            AbilityType abilityType, Map<String, Object> abilityConfig,
            WeaponRecipe recipe) {
        this.id = id;
        this.materialType = materialType;
        this.displayName = displayName;
        this.lore = List.copyOf(lore);
        this.customModelData = customModelData;
        this.enchantments = Map.copyOf(enchantments);
        this.abilityType = abilityType;
        this.abilityConfig = Map.copyOf(abilityConfig);
        this.recipe = recipe;
    }

    public String getId() {
        return id;
    }

    public String getMaterialType() {
        return materialType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public AbilityType getAbilityType() {
        return abilityType;
    }

    public WeaponRecipe getRecipe() {
        return recipe;
    }

    /**
     * Safely reads an int from the ability-config map (handles Integer / Double from YAML).
     */
    public int abilityInt(String key, int def) {
        Object v = abilityConfig.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    /**
     * Safely reads a double from the ability-config map (handles Integer / Double from YAML).
     */
    public double abilityDouble(String key, double def) {
        Object v = abilityConfig.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return def;
    }
}
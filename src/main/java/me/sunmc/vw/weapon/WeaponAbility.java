package me.sunmc.vw.weapon;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a weapon's ability configuration
 */
public class WeaponAbility {

    /**
     * Types of ability triggers
     */
    public enum TriggerType {
        /**
         * Triggered when a player kills an entity
         */
        KILL_EFFECT,
        /**
         * Triggered when hitting an entity
         */
        ON_HIT,
        /**
         * Triggered while holding the weapon
         */
        ON_HOLD,
        /**
         * Always active passive ability
         */
        PASSIVE
    }

    /**
     * Types of ability effects
     */
    public enum EffectType {
        /**
         * Decreases reload time for crossbows
         */
        RELOAD_DECREASE,
        /**
         * Increases sharpness level
         */
        SHARPNESS_INCREASE,
        /**
         * Custom effect (requires custom handler)
         */
        CUSTOM
    }

    private final TriggerType triggerType;
    private final EffectType effectType;
    private final int value;
    private final int maxValue;
    private final int minValue;
    private final int minReload;

    public WeaponAbility(@NotNull TriggerType triggerType, @NotNull EffectType effectType,
                         int value, int maxValue, int minValue, int minReload) {
        this.triggerType = triggerType;
        this.effectType = effectType;
        this.value = value;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.minReload = minReload;
    }

    @NotNull
    public TriggerType getTriggerType() {
        return triggerType;
    }

    @NotNull
    public EffectType getEffectType() {
        return effectType;
    }

    public int getValue() {
        return value;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMinReload() {
        return minReload;
    }

    /**
     * Calculate the effective value based on kills
     *
     * @param kills The number of kills
     * @return The effective value
     */
    public int calculateEffectiveValue(int kills) {
        int effectiveValue = kills * value;
        return Math.min(effectiveValue, maxValue);
    }

    /**
     * Check if the ability has reached its maximum value
     *
     * @param kills The number of kills
     * @return true if the maximum has been reached
     */
    public boolean isMaxed(int kills) {
        return calculateEffectiveValue(kills) >= maxValue;
    }

    /**
     * Builder class for WeaponAbility
     */
    public static class Builder {
        private TriggerType triggerType = TriggerType.KILL_EFFECT;
        private EffectType effectType = EffectType.CUSTOM;
        private int value = 1;
        private int maxValue = 10;
        private int minValue = 0;
        private int minReload = 10;

        public Builder triggerType(@NotNull TriggerType triggerType) {
            this.triggerType = triggerType;
            return this;
        }

        public Builder effectType(@NotNull EffectType effectType) {
            this.effectType = effectType;
            return this;
        }

        public Builder value(int value) {
            this.value = value;
            return this;
        }

        public Builder maxValue(int maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder minValue(int minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder minReload(int minReload) {
            this.minReload = minReload;
            return this;
        }

        @NotNull
        public WeaponAbility build() {
            return new WeaponAbility(triggerType, effectType, value, maxValue, minValue, minReload);
        }
    }
}

package me.sunmc.vw.model;

public enum AbilityType {
    /**
     * Per kill (PvP): crossbow reload time decreases.
     */
    CROSSBOW_RELOAD,
    /**
     * Per kill (PvP): sword Sharpness level increases by 1.
     */
    SWORD_SHARPNESS,
    /**
     * Per kill (PvP): heals the player by a configurable amount.
     */
    LIFESTEAL,
    /**
     * Per kill (PvP): grants the player a Speed potion effect.
     */
    SPEED_BOOST,
    /**
     * Per kill (PvP): strikes visual lightning at the victim's location.
     */
    LIGHTNING_STRIKE,
    /**
     * Per hit (PvP): applies Poison to the target.
     */
    POISON_BLADE,
    NONE
}
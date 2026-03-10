package me.sunmc.vw.gui;

import me.sunmc.vw.weapon.WeaponData;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an open recipe GUI session
 */
public class RecipeGUI {

    private final WeaponData weapon;
    private final boolean isDetail;

    public RecipeGUI(@NotNull WeaponData weapon, boolean isDetail) {
        this.weapon = weapon;
        this.isDetail = isDetail;
    }

    @NotNull
    public WeaponData getWeapon() {
        return weapon;
    }

    public boolean isDetail() {
        return isDetail;
    }
}

package me.sunmc.vw.gui;

import me.sunmc.vw.weapon.WeaponData;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Inventory holder for the recipe detail GUI
 */
public class RecipeDetailHolder implements InventoryHolder {

    private final WeaponData weapon;

    public RecipeDetailHolder(@NotNull WeaponData weapon) {
        this.weapon = weapon;
    }

    @NotNull
    public WeaponData getWeapon() {
        return weapon;
    }

    @Override
    @NotNull
    public org.bukkit.inventory.Inventory getInventory() {
        return null;
    }
}

package me.sunmc.vw.listener;

import me.sunmc.vw.gui.RecipeGUI;
import me.sunmc.vw.manager.WeaponManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

public final class GUIListener implements Listener {
    @SuppressWarnings("unused")
    private final WeaponManager weaponManager;

    public GUIListener(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeGUI) {
            event.setCancelled(true);
        }
    }
}
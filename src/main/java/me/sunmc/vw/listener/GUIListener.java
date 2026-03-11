package me.sunmc.vw.listener;

import me.sunmc.vw.gui.RecipeGUI;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.util.FireworkUtil;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

    /**
     * Cancels any damage dealt by a VaultWeapons celebration firework.
     * FireworkUtil.isVaultFirework() returns true only for fireworks we spawned
     * and automatically removes the UUID so the set stays clean.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onFireworkDamage(@NotNull EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework fw
                && FireworkUtil.isVaultFirework(fw.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
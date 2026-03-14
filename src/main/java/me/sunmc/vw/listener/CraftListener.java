package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.manager.CraftTracker;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.util.FireworkUtil;
import me.sunmc.vw.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

public final class CraftListener implements Listener {

    private final VaultWeapons plugin;
    private final WeaponManager weaponManager;
    private final CraftTracker craftTracker;

    /**
     * Guards celebrate() so it runs at most once per crafting action.
     */
    private final Set<UUID> celebrating =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Guards scheduleCloseWithMessage() so only one close is scheduled
     * per player at a time — PrepareItemCraftEvent fires rapidly.
     */
    private final Set<UUID> closingInventory =
            Collections.synchronizedSet(new HashSet<>());

    public CraftListener(VaultWeapons plugin, WeaponManager weaponManager,
                         CraftTracker craftTracker) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        this.craftTracker = craftTracker;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        // Guard 1 — vault weapon used as an ingredient
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            if (weaponManager.isVaultWeapon(ingredient)) {
                event.getInventory().setResult(null);
                scheduleCloseWithMessage(player,
                        Component.text("✖ Vault Weapons cannot be used as crafting ingredients.",
                                NamedTextColor.RED));
                return;
            }
        }

        // Guard 2 — player already crafted this weapon (one-time limit)
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;
        Optional<String> idOpt = weaponManager.getWeaponId(result);
        if (idOpt.isEmpty()) return;

        if (craftTracker.hasCrafted(player.getUniqueId(), idOpt.get())) {
            event.getInventory().setResult(null);
            WeaponDefinition def = plugin.getConfigLoader().getWeapon(idOpt.get());
            Component msg = Component.text("✖ You have already crafted ", NamedTextColor.RED)
                    .append(def != null
                            ? TextUtil.parse(def.getDisplayName())
                            : Component.text(idOpt.get(), NamedTextColor.WHITE))
                    .append(Component.text("! An admin can run /vw weapon reset to allow it again.",
                            NamedTextColor.RED));
            scheduleCloseWithMessage(player, msg);
        }
    }

    /**
     * Closes the player's inventory on the next tick (inventory changes cannot
     * be made mid-event) and then shows a 5-second action bar message.
     * Debounced per player, so it triggers at most once per placement action.
     */
    private void scheduleCloseWithMessage(@NotNull Player player, Component message) {
        if (!closingInventory.add(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            closingInventory.remove(player.getUniqueId());
            showTimedActionBar(player, message, 5);
        });
    }

    /**
     * Repeats the action-bar message every second for {@code seconds} seconds.
     */
    private void showTimedActionBar(Player player, Component message, int seconds) {
        final int[] tick = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            player.sendActionBar(message);
            if (++tick[0] >= seconds) task.cancel();
        }, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(@NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getInventory().getResult();
        if (result == null) return;

        Optional<String> idOpt = weaponManager.getWeaponId(result);
        if (idOpt.isEmpty()) return;

        WeaponDefinition def = plugin.getConfigLoader().getWeapon(idOpt.get());
        if (def == null) return;

        // Persist the craft so this player can never craft this weapon again
        craftTracker.markCrafted(player.getUniqueId(), idOpt.get());

        // Server-wide broadcast
        plugin.getServer().broadcast(
                Component.text("⚒ ", NamedTextColor.GOLD)
                        .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" has forged ", NamedTextColor.GREEN))
                        .append(TextUtil.parse(def.getDisplayName()))
                        .append(Component.text("!", NamedTextColor.GREEN))
        );

        // Personal celebration (debounced)
        if (!celebrating.add(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            celebrate(player, def);
            celebrating.remove(player.getUniqueId());
        }, 1L);
    }

    private void celebrate(@NotNull Player player, @NotNull WeaponDefinition def) {
        player.showTitle(Title.title(
                Component.text("✦ VAULT WEAPON CRAFTED ✦", NamedTextColor.GOLD),
                TextUtil.parse(def.getDisplayName()),
                Title.Times.times(
                        Duration.ofMillis(300),
                        Duration.ofSeconds(3),
                        Duration.ofMillis(600)
                )
        ));

        player.playSound(player.getLocation(),
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        FireworkUtil.launch(player.getLocation());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> FireworkUtil.launch(player.getLocation()), 8L);

        player.sendMessage(
                Component.text("⚒ You forged ", NamedTextColor.GREEN)
                        .append(TextUtil.parse(def.getDisplayName()))
                        .append(Component.text("! Its power is now yours.", NamedTextColor.GREEN))
        );
    }
}
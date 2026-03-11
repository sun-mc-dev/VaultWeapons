package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeapons;
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

    private final Set<UUID> celebrating =
            Collections.synchronizedSet(new HashSet<>());

    public CraftListener(VaultWeapons plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
    }

    /**
     * ── FIX: If any ingredient in the crafting grid is a Vault Weapon,
     * wipe the result so the craft cannot proceed at all.
     * PrepareItemCraftEvent fires every time the grid changes, before the
     * player can click the result slot — so this is the correct place to block it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            if (weaponManager.isVaultWeapon(ingredient)) {
                event.getInventory().setResult(null);

                if (event.getView().getPlayer() instanceof Player player) {
                    player.sendActionBar(Component.text(
                            "✖ Vault Weapons cannot be used as crafting ingredients.",
                            NamedTextColor.RED));
                }
                return;
            }
        }
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
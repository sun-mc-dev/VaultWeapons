package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.model.WeaponDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class KillListener implements Listener {

    private final VaultWeapons plugin;
    private final WeaponManager weaponManager;

    public KillListener(VaultWeapons plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR) return;

        Optional<String> idOpt = weaponManager.getWeaponId(weapon);
        if (idOpt.isEmpty()) return;

        WeaponDefinition def = plugin.getConfigLoader().getWeapon(idOpt.get());
        if (def == null) return;

        switch (def.getAbilityType()) {
            case CROSSBOW_RELOAD -> applyCrossbowReload(killer, weapon, def);
            case SWORD_SHARPNESS -> applySwordSharpness(killer, weapon, def);
            case LIFESTEAL -> applyLifesteal(killer, weapon, def);
            case SPEED_BOOST -> applySpeedBoost(killer, weapon, def);
            case LIGHTNING_STRIKE -> applyLightningStrike(killer, weapon, event, def);
            default -> { /* NONE / POISON_BLADE handled in HitListener */ }
        }
    }

    private void applyCrossbowReload(@NotNull Player player, ItemStack weapon,
                                     @NotNull WeaponDefinition def) {
        int base = def.abilityInt("base_reload_ticks", 25);
        int reduction = def.abilityInt("reduction_per_kill", 2);
        int minTicks = def.abilityInt("min_reload_ticks", 1);

        weaponManager.addKill(weapon);
        player.getInventory().setItemInMainHand(weapon);

        int kills = weaponManager.getKills(weapon);
        int reloadTicks = Math.max(minTicks, base - kills * reduction);

        player.sendActionBar(
                Component.text("⚡ Auto-reload in ", NamedTextColor.AQUA)
                        .append(Component.text(reloadTicks + " ticks", NamedTextColor.WHITE))
                        .append(Component.text("  [kills: " + kills + "]", NamedTextColor.DARK_AQUA))
        );
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> autoLoadCrossbow(player), reloadTicks);
    }

    private void autoLoadCrossbow(@NotNull Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!weaponManager.isVaultWeapon(held)) return;
        if (held.getType() != Material.CROSSBOW) return;
        if (!(held.getItemMeta() instanceof CrossbowMeta meta)) return;
        if (meta.hasChargedProjectiles()) return;

        meta.setChargedProjectiles(List.of(new ItemStack(Material.ARROW)));
        held.setItemMeta(meta);
        player.getInventory().setItemInMainHand(held);

        player.sendActionBar(Component.text("✦ Crossbow auto-reloaded!", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.2f);
    }

    private void applySwordSharpness(Player player, ItemStack weapon,
                                     @NotNull WeaponDefinition def) {
        int maxSharp = def.abilityInt("max_sharpness", 10);
        int current = weaponManager.getSharpnessLevel(weapon);

        weaponManager.addKill(weapon);

        if (current >= maxSharp) {
            player.getInventory().setItemInMainHand(weapon);
            player.sendActionBar(
                    Component.text("⚔ Max Sharpness ", NamedTextColor.DARK_RED)
                            .append(Component.text("(" + maxSharp + ") reached!", NamedTextColor.RED))
            );
            return;
        }

        int next = current + 1;
        weaponManager.setSharpnessLevel(weapon, next);
        player.getInventory().setItemInMainHand(weapon);

        int kills = weaponManager.getKills(weapon);
        player.sendActionBar(
                Component.text("⚔ Sharpness ", NamedTextColor.RED)
                        .append(Component.text(String.valueOf(next), NamedTextColor.DARK_RED))
                        .append(Component.text("  [kills: " + kills + "]", NamedTextColor.GRAY))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    private void applyLifesteal(@NotNull Player player, ItemStack weapon,
                                @NotNull WeaponDefinition def) {
        double healAmount = def.abilityDouble("heal_amount", 4.0);

        weaponManager.addKill(weapon);
        player.getInventory().setItemInMainHand(weapon);

        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) return;

        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth.getValue());
        player.setHealth(newHealth);

        int kills = weaponManager.getKills(weapon);
        player.sendActionBar(
                Component.text("❤ Lifesteal +", NamedTextColor.RED)
                        .append(Component.text((healAmount / 2) + " hearts", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("  [kills: " + kills + "]", NamedTextColor.GRAY))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void applySpeedBoost(@NotNull Player player, ItemStack weapon,
                                 @NotNull WeaponDefinition def) {
        int durationTicks = def.abilityInt("duration_ticks", 100); // 5 seconds
        int amplifier = def.abilityInt("amplifier", 1);         // Speed II

        weaponManager.addKill(weapon);
        player.getInventory().setItemInMainHand(weapon);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                durationTicks,
                amplifier - 1,
                false,
                true,
                true
        ));

        int kills = weaponManager.getKills(weapon);
        player.sendActionBar(
                Component.text("⚡ Speed Boost ", NamedTextColor.YELLOW)
                        .append(Component.text("(" + durationTicks / 20 + "s)", NamedTextColor.WHITE))
                        .append(Component.text("  [kills: " + kills + "]", NamedTextColor.GRAY))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 2.0f);
    }

    private void applyLightningStrike(@NotNull Player player, ItemStack weapon,
                                      @NotNull EntityDeathEvent event,
                                      @NotNull WeaponDefinition def) {
        weaponManager.addKill(weapon);
        player.getInventory().setItemInMainHand(weapon);

        event.getEntity().getWorld()
                .strikeLightningEffect(event.getEntity().getLocation());

        int kills = weaponManager.getKills(weapon);
        player.sendActionBar(
                Component.text("⚡ Lightning Strike!", NamedTextColor.YELLOW)
                        .append(Component.text("  [kills: " + kills + "]", NamedTextColor.GRAY))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
    }
}
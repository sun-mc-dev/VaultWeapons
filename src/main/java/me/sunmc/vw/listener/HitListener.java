package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.model.AbilityType;
import me.sunmc.vw.model.WeaponDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class HitListener implements Listener {

    private final VaultWeapons plugin;
    private final WeaponManager weaponManager;

    public HitListener(VaultWeapons plugin, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR) return;

        Optional<String> idOpt = weaponManager.getWeaponId(weapon);
        if (idOpt.isEmpty()) return;

        WeaponDefinition def = plugin.getConfigLoader().getWeapon(idOpt.get());
        if (def == null) return;

        if (def.getAbilityType() == AbilityType.POISON_BLADE) {
            applyPoisonBlade(attacker, victim, def);
        }
    }

    private void applyPoisonBlade(@NotNull Player attacker, @NotNull LivingEntity victim,
                                  @NotNull WeaponDefinition def) {
        int durationTicks = def.abilityInt("duration_ticks", 100); // 5 seconds default
        int amplifier = def.abilityInt("amplifier", 0);         // 0 = Poison I

        victim.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON,
                durationTicks,
                amplifier,
                false,
                true,
                true
        ));

        attacker.sendActionBar(
                Component.text("☠ Poison applied ", NamedTextColor.DARK_GREEN)
                        .append(Component.text("(" + durationTicks / 20 + "s)", NamedTextColor.GREEN))
        );
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.6f, 1.5f);
    }
}
package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.weapon.WeaponAbility;
import me.sunmc.vw.weapon.WeaponData;
import me.sunmc.vw.weapon.WeaponItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Listener for weapon ability activation events
 */
public class WeaponAbilityListener implements Listener {

    private final VaultWeaponsPlugin plugin;

    // Track crossbow reload times per weapon
    private final Map<UUID, Long> crossbowReloadTracker = new HashMap<>();

    public WeaponAbilityListener(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();

        if (!WeaponItemBuilder.isVaultWeapon(weapon)) {
            return;
        }

        String weaponId = WeaponItemBuilder.getWeaponId(weapon);
        if (weaponId == null) {
            return;
        }

        WeaponData weaponData = plugin.getWeaponManager().getWeapon(weaponId);
        if (weaponData == null || !weaponData.hasAbility()) {
            return;
        }

        WeaponAbility ability = weaponData.getAbility();

        if (ability != null && ability.getTriggerType() != WeaponAbility.TriggerType.KILL_EFFECT) {
            return;
        }


        if (!killer.hasPermission("vaultweapons.use")) {
            return;
        }

        int currentKills = WeaponItemBuilder.getKills(weapon);
        int newKills = currentKills + 1;


        CompletableFuture.runAsync(() -> {
            if (ability != null) {
                processKillAbility(killer, weapon, weaponData, ability, newKills);
            }
        }, plugin.getExecutorService());
    }

    /**
     * Process kill-based ability effects
     *
     * @param killer     The killer
     * @param weapon     The weapon ItemStack
     * @param weaponData The weapon data
     * @param ability    The ability
     * @param newKills   The new kill count
     */
    private void processKillAbility(@NotNull Player killer, @NotNull ItemStack weapon,
                                    @NotNull WeaponData weaponData, @NotNull WeaponAbility ability,
                                    int newKills) {

        plugin.runSync(() -> {

            ItemStack updatedWeapon = plugin.getWeaponManager().updateWeaponKills(weapon, newKills);

            killer.getInventory().setItemInMainHand(updatedWeapon);

            switch (ability.getEffectType()) {
                case RELOAD_DECREASE:
                    handleReloadDecrease(killer, weaponData, ability, newKills);
                    break;

                case SHARPNESS_INCREASE:
                    handleSharpnessIncrease(killer, weaponData, ability, newKills, updatedWeapon);
                    break;

                default:
                    break;
            }

            playAbilitySound(killer);
        });
    }

    /**
     * Handle crossbow reload decrease ability
     *
     * @param player     The player
     * @param weaponData The weapon data
     * @param ability    The ability
     * @param kills      The kill count
     */
    private void handleReloadDecrease(@NotNull Player player, @NotNull WeaponData weaponData,
                                      @NotNull WeaponAbility ability, int kills) {
        int bonus = ability.calculateEffectiveValue(kills);

        String message;
        if (ability.isMaxed(kills)) {
            message = plugin.getConfigManager().getMessage("abilities.crossbow-max-reload");
        } else {
            message = plugin.getConfigManager().getMessage("abilities.crossbow-reload-decrease",
                    "kills", String.valueOf(kills));
        }

        player.sendMessage(message);
    }

    /**
     * Handle sword sharpness increase ability
     *
     * @param player     The player
     * @param weaponData The weapon data
     * @param ability    The ability
     * @param kills      The kill count
     * @param weapon     The updated weapon
     */
    private void handleSharpnessIncrease(@NotNull Player player, @NotNull WeaponData weaponData,
                                         @NotNull WeaponAbility ability, int kills,
                                         @NotNull ItemStack weapon) {
        int bonus = ability.calculateEffectiveValue(kills);
        int baseSharpness = weaponData.getEnchants().getOrDefault(Enchantment.SHARPNESS, 0);
        int totalSharpness = baseSharpness + bonus;

        String message;
        if (ability.isMaxed(kills)) {
            message = plugin.getConfigManager().getMessage("abilities.sword-max-sharpness");
        } else {
            message = plugin.getConfigManager().getMessage("abilities.sword-sharpness-increase",
                    "level", String.valueOf(totalSharpness));
        }

        player.sendMessage(message);
    }

    /**
     * Play ability activation sound
     *
     * @param player The player
     */
    private void playAbilitySound(@NotNull Player player) {
        String soundName = plugin.getConfigManager().getMainConfig()
                .getString("effects.sound.ability-activate", "entity.experience_orb.pickup");

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_").replace("-", "_"));
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, 0.5f, 1.2f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName, SoundCategory.PLAYERS, 0.5f, 1.2f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrossbowShoot(@NotNull EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack weapon = event.getBow();

        if (weapon == null || weapon.getType() != Material.CROSSBOW) {
            return;
        }

        if (!WeaponItemBuilder.isVaultWeapon(weapon)) {
            return;
        }

        String weaponId = WeaponItemBuilder.getWeaponId(weapon);
        if (weaponId == null) {
            return;
        }

        WeaponData weaponData = plugin.getWeaponManager().getWeapon(weaponId);
        if (weaponData == null || !weaponData.hasAbility()) {
            return;
        }

        WeaponAbility ability = weaponData.getAbility();

        // Only handle RELOAD_DECREASE ability
        if (ability != null && ability.getEffectType() != WeaponAbility.EffectType.RELOAD_DECREASE) {
            return;
        }

        int kills = WeaponItemBuilder.getKills(weapon);
        if (ability != null) {
            int reloadDecrease = ability.calculateEffectiveValue(kills);
        }
        if (ability != null) {
            int minReload = ability.getMinReload();
        }


        crossbowReloadTracker.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Get the reload time for a crossbow weapon
     *
     * @param weapon The weapon ItemStack
     * @return The reload time in ticks, or -1 if not applicable
     */
    public int getCrossbowReloadTime(@NotNull ItemStack weapon) {
        if (!WeaponItemBuilder.isVaultWeapon(weapon)) {
            return -1;
        }

        String weaponId = WeaponItemBuilder.getWeaponId(weapon);
        if (weaponId == null) {
            return -1;
        }

        WeaponData weaponData = plugin.getWeaponManager().getWeapon(weaponId);
        if (weaponData == null || !weaponData.hasAbility()) {
            return -1;
        }

        WeaponAbility ability = weaponData.getAbility();
        if (ability != null && ability.getEffectType() != WeaponAbility.EffectType.RELOAD_DECREASE) {
            return -1;
        }

        int kills = WeaponItemBuilder.getKills(weapon);
        int reloadDecrease = 0;
        if (ability != null) {
            reloadDecrease = ability.calculateEffectiveValue(kills);
        }
        int baseReload = 25; // Default crossbow reload time in ticks
        int minReload = 0;
        if (ability != null) {
            minReload = ability.getMinReload();
        }

        return Math.max(baseReload - reloadDecrease, minReload);
    }
}

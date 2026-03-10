package me.sunmc.vw.listener;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.config.ConfigManager;
import me.sunmc.vw.weapon.WeaponData;
import me.sunmc.vw.weapon.WeaponItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Listener for crafting events to handle weapon crafting effects
 */
public class CraftListener implements Listener {

    private final VaultWeaponsPlugin plugin;

    public CraftListener(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(@NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        // Check if this is a VaultWeapon
        if (!WeaponItemBuilder.isVaultWeapon(result)) {
            return;
        }

        String weaponId = WeaponItemBuilder.getWeaponId(result);
        if (weaponId == null) {
            return;
        }

        WeaponData weaponData = plugin.getWeaponManager().getWeapon(weaponId);
        if (weaponData == null) {
            return;
        }

        // Check permission
        if (!player.hasPermission("vaultweapons.craft")) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        // Run crafting effects asynchronously
        CompletableFuture.runAsync(() -> {
            // Wait a tick for the craft to complete
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Run effects on main thread
            plugin.runSync(() -> {
                playCraftingEffects(player, weaponData);
                sendCraftingMessage(player, weaponData);
            });
        }, plugin.getExecutorService());
    }

    /**
     * Play visual effects when crafting a weapon
     *
     * @param player     The player
     * @param weaponData The crafted weapon
     */
    private void playCraftingEffects(@NotNull Player player, @NotNull WeaponData weaponData) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (!config.getBoolean("effects.enabled", true)) {
            return;
        }

        // Play title effect
        if (config.getBoolean("effects.title.enabled", true)) {
            playTitleEffect(player, weaponData);
        }

        // Play firework effect
        if (config.getBoolean("effects.firework.enabled", true)) {
            playFireworkEffect(player);
        }

        // Play sound effect
        if (config.getBoolean("effects.sound.enabled", true)) {
            playSoundEffect(player);
        }
    }

    /**
     * Play the title effect
     *
     * @param player     The player
     * @param weaponData The weapon data
     */
    private void playTitleEffect(@NotNull Player player, @NotNull WeaponData weaponData) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        int fadeIn = config.getInt("effects.title.fade-in", 10);
        int stay = config.getInt("effects.title.stay", 40);
        int fadeOut = config.getInt("effects.title.fade-out", 10);

        String title = plugin.getConfigManager().getMessage("titles.craft-success.title");
        String subtitle = plugin.getConfigManager().getMessage("titles.craft-success.subtitle",
                "weapon", weaponData.getName());


        player.showTitle(
                Title.title(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(title),
                        LegacyComponentSerializer.legacyAmpersand().deserialize(subtitle),
                        Title.Times.times(
                                Duration.ofMillis(fadeIn * 50L),
                                Duration.ofMillis(stay * 50L),
                                Duration.ofMillis(fadeOut * 50L)
                        )
                )
        );
    }

    /**
     * Play the firework effect
     *
     * @param player The player
     */
    private void playFireworkEffect(@NotNull Player player) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        ConfigurationSection fireworkSection = config.getConfigurationSection("effects.firework");

        if (fireworkSection == null) {
            return;
        }

        // Create firework
        Firework firework = (Firework) player.getWorld().spawnEntity(
                player.getLocation().add(0, 1, 0),
                EntityType.FIREWORK_ROCKET
        );

        FireworkMeta meta = firework.getFireworkMeta();

        // Set power
        int power = fireworkSection.getInt("power", 1);
        meta.setPower(Math.max(0, Math.min(power, 3)));

        // Create firework effect
        FireworkEffect.Builder builder = FireworkEffect.builder();

        // Set type
        String typeName = fireworkSection.getString("type", "BALL_LARGE");
        try {
            FireworkEffect.Type type = FireworkEffect.Type.valueOf(typeName);
            builder.with(type);
        } catch (IllegalArgumentException e) {
            builder.with(FireworkEffect.Type.BALL_LARGE);
        }

        // Set colors
        List<String> colorStrings = fireworkSection.getStringList("colors");
        for (String colorStr : colorStrings) {
            Color color = parseColor(colorStr);
            if (color != null) {
                builder.withColor(color);
            }
        }

        // Set fade colors
        List<String> fadeColorStrings = fireworkSection.getStringList("fade-colors");
        for (String colorStr : fadeColorStrings) {
            Color color = parseColor(colorStr);
            if (color != null) {
                builder.withFade(color);
            }
        }

        // Set trail and flicker
        if (fireworkSection.getBoolean("trail", true)) {
            builder.withTrail();
        }
        if (fireworkSection.getBoolean("flicker", true)) {
            builder.withFlicker();
        }

        meta.addEffect(builder.build());
        firework.setFireworkMeta(meta);
    }

    /**
     * Parse a color string to a Bukkit Color
     *
     * @param colorStr The color string (hex or name)
     * @return The Bukkit Color, or null if invalid
     */
    private @Nullable Color parseColor(@NotNull String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                int rgb = Integer.parseInt(colorStr.substring(1), 16);
                return Color.fromRGB(rgb);
            } else {

                return (Color) Color.class.getField(colorStr.toUpperCase()).get(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Play the sound effect
     *
     * @param player The player
     */
    private void playSoundEffect(@NotNull Player player) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        String soundName = config.getString("effects.sound.craft-success", "entity.player.levelup");

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_").replace("-", "_"));
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    /**
     * Send crafting message to the player
     *
     * @param player     The player
     * @param weaponData The weapon data
     */
    private void sendCraftingMessage(@NotNull Player player, @NotNull WeaponData weaponData) {
        String message = plugin.getConfigManager().getMessage("weapons.crafted",
                "weapon", weaponData.getName());
        player.sendMessage(message);
    }

}


package me.sunmc.vw.task;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.weapon.WeaponData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Task for playing visual effects when crafting weapons
 */
public class CraftEffectTask extends BukkitRunnable {

    private final VaultWeaponsPlugin plugin;
    private final Player player;
    private final WeaponData weaponData;
    private int step = 0;

    public CraftEffectTask(@NotNull VaultWeaponsPlugin plugin, @NotNull Player player,
                           @NotNull WeaponData weaponData) {
        this.plugin = plugin;
        this.player = player;
        this.weaponData = weaponData;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        switch (step) {
            case 0 -> {
                if (config.getBoolean("effects.title.enabled", true)) {
                    playTitleEffect();
                }
            }
            case 1 -> {
                if (config.getBoolean("effects.firework.enabled", true)) {
                    playFireworkEffect();
                }
            }
            case 2 -> {
                if (config.getBoolean("effects.sound.enabled", true)) {
                    playSoundEffect();
                }
                // Task complete
                cancel();
            }
        }

        step++;
    }

    /**
     * Play the title effect
     */
    private void playTitleEffect() {
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
     */
    private void playFireworkEffect() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        org.bukkit.configuration.ConfigurationSection fireworkSection =
                config.getConfigurationSection("effects.firework");

        if (fireworkSection == null) {
            return;
        }

        Location location = player.getLocation().add(0, 1, 0);

        // Spawn firework
        Firework firework = (Firework) player.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        int power = fireworkSection.getInt("power", 1);
        meta.setPower(Math.max(0, Math.min(power, 3)));

        // Build firework effect
        FireworkEffect.Builder builder = FireworkEffect.builder();

        String typeName = fireworkSection.getString("type", "BALL_LARGE");
        try {
            FireworkEffect.Type type = FireworkEffect.Type.valueOf(typeName);
            builder.with(type);
        } catch (IllegalArgumentException e) {
            builder.with(FireworkEffect.Type.BALL_LARGE);
        }

        // Parse colors
        List<String> colorStrings = fireworkSection.getStringList("colors");
        for (String colorStr : colorStrings) {
            Color color = parseColor(colorStr);
            if (color != null) {
                builder.withColor(color);
            }
        }

        // Parse fade colors
        List<String> fadeColorStrings = fireworkSection.getStringList("fade-colors");
        for (String colorStr : fadeColorStrings) {
            Color color = parseColor(colorStr);
            if (color != null) {
                builder.withFade(color);
            }
        }

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
     * @param colorStr The color string
     * @return The Bukkit Color, or null if invalid
     */
    private @Nullable Color parseColor(@NotNull String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                int rgb = Integer.parseInt(colorStr.substring(1), 16);
                return Color.fromRGB(rgb);
            } else {
                // Try named colors
                return switch (colorStr.toUpperCase()) {
                    case "RED" -> Color.RED;
                    case "GREEN" -> Color.GREEN;
                    case "BLUE" -> Color.BLUE;
                    case "YELLOW" -> Color.YELLOW;
                    case "PURPLE" -> Color.PURPLE;
                    case "ORANGE" -> Color.ORANGE;
                    case "WHITE" -> Color.WHITE;
                    case "BLACK" -> Color.BLACK;
                    case "AQUA" -> Color.AQUA;
                    case "FUCHSIA" -> Color.FUCHSIA;
                    case "LIME" -> Color.LIME;
                    case "MAROON" -> Color.MAROON;
                    case "NAVY" -> Color.NAVY;
                    case "OLIVE" -> Color.OLIVE;
                    case "SILVER" -> Color.SILVER;
                    case "TEAL" -> Color.TEAL;
                    default -> {
                        // Try to parse as RGB components
                        String[] parts = colorStr.split(",");
                        if (parts.length == 3) {
                            yield Color.fromRGB(
                                    Integer.parseInt(parts[0].trim()),
                                    Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim())
                            );
                        }
                        yield null;
                    }
                };
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Play the sound effect
     */
    private void playSoundEffect() {
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
     * Start the craft effect task
     *
     * @param plugin     The plugin instance
     * @param player     The player
     * @param weaponData The weapon data
     */
    public static void start(@NotNull VaultWeaponsPlugin plugin, @NotNull Player player,
                             @NotNull WeaponData weaponData) {
        CraftEffectTask task = new CraftEffectTask(plugin, player, weaponData);
        task.runTaskTimer(plugin, 0L, 10L); // Run every 10 ticks (0.5 seconds)
    }
}

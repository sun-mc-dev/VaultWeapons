package me.sunmc.vw.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

public final class FireworkUtil {

    private FireworkUtil() {
    }

    /**
     * Spawns a firework and immediately detonates it via {@link Firework#detonate()}.
     * This causes zero entity damage.
     */
    public static void launch(@NotNull Location location) {
        location.getWorld().spawn(location, Firework.class, fw -> {
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .flicker(true)
                    .trail(true)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.YELLOW, Color.ORANGE, Color.YELLOW)
                    .withFade(Color.WHITE)
                    .build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
        }).detonate();
    }
}
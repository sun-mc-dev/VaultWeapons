package me.sunmc.vw.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FireworkUtil {

    private FireworkUtil() {
    }

    /**
     * Set of UUIDs of fireworks spawned by VaultWeapons.
     * Checked (and cleared) by the damage listener to cancel firework damage.
     */
    private static final Set<UUID> vaultFireworks =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Spawns a firework and immediately detonates it.
     * The UUID is registered so that {@link #isVaultFirework(UUID)} returns true
     * during the damage event, allowing the listener to cancel any damage dealt.
     */
    public static void launch(@NotNull Location location) {
        Firework fw = location.getWorld().spawn(location, Firework.class, f -> {
            FireworkMeta meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .flicker(true)
                    .trail(true)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.YELLOW, Color.ORANGE, Color.YELLOW)
                    .withFade(Color.WHITE)
                    .build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
        });

        // Register before detonating so the UUID is present when damage events fire
        vaultFireworks.add(fw.getUniqueId());
        fw.detonate();
    }

    /**
     * Returns true (and removes the UUID) if this firework was spawned by VaultWeapons.
     * Call this from {@code EntityDamageByEntityEvent} to cancel firework damage.
     */
    public static boolean isVaultFirework(@NotNull UUID uuid) {
        return vaultFireworks.remove(uuid);
    }
}
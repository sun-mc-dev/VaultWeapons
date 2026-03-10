package me.sunmc.vw.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for item manipulation
 */
public final class ItemUtils {

    private ItemUtils() {
        // Prevent instantiation
    }

    /**
     * Check if an ItemStack is null or air
     *
     * @param item The item to check
     * @return true if the item is null or air
     */
    public static boolean isNullOrAir(@Nullable ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Get a display name for a material
     *
     * @param material The material
     * @return A formatted display name
     */
    @NotNull
    public static String getMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Compare two ItemStacks ignoring amount
     *
     * @param item1 The first item
     * @param item2 The second item
     * @return true if the items are similar
     */
    public static boolean isSimilar(@Nullable ItemStack item1, @Nullable ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        return item1.isSimilar(item2);
    }

    /**
     * Get the amount of a specific material in an ItemStack array
     *
     * @param items    The items to search
     * @param material The material to count
     * @return The total amount
     */
    public static int countMaterial(@NotNull ItemStack @NotNull [] items, @NotNull Material material) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
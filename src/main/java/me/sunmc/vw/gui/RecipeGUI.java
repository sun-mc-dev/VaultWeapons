package me.sunmc.vw.gui;

import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.model.WeaponRecipe;
import me.sunmc.vw.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 9×3 Read-only Recipe Viewer.
 * <p>
 * Slot layout:
 * Row 0 (slots  0- 8): [ bg ][ R0C0 ][ R0C1 ][ R0C2 ][ bg ][ bg ][ bg ][ bg ][ bg ]
 * Row 1 (slots  9-17): [ bg ][ R1C0 ][ R1C1 ][ R1C2 ][ bg ][ ➜  ][ bg ][ res ][ bg ]
 * Row 2 (slots 18-26): [ bg ][ R2C0 ][ R2C1 ][ R2C2 ][ bg ][ bg ][ bg ][ bg ][ bg ]
 * <p>
 * Crafting grid row anchor slots: row 0 → 1, row 1 → 10, row 2 → 19
 * Arrow  → slot 14
 * Result → slot 16
 */
public final class RecipeGUI implements InventoryHolder {

    private static final int ARROW_SLOT = 14;
    private static final int RESULT_SLOT = 16;
    private static final int[] ROW_ANCHORS = {1, 10, 19};

    private final Inventory inventory;

    public RecipeGUI(@NotNull WeaponDefinition def, WeaponManager weaponManager) {
        inventory = Bukkit.createInventory(this, 27,
                Component.text("Recipe: ").append(TextUtil.parse(def.getDisplayName())));
        populate(def, weaponManager);
    }

    private void populate(WeaponDefinition def, WeaponManager weaponManager) {
        // Background
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.displayName(Component.empty());
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 27; i++) inventory.setItem(i, bg);

        // Crafting grid
        WeaponRecipe recipe = def.getRecipe();
        if (recipe != null && "SHAPED".equalsIgnoreCase(recipe.type())) {
            List<String> shape = recipe.shape();
            for (int row = 0; row < Math.min(shape.size(), 3); row++) {
                String rowStr = shape.get(row);
                for (int col = 0; col < Math.min(rowStr.length(), 3); col++) {
                    char c = rowStr.charAt(col);
                    if (c == ' ') continue;
                    String matName = recipe.ingredients().get(c);
                    if (matName == null) continue;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase());
                        ItemStack ing = new ItemStack(mat);
                        ItemMeta im = ing.getItemMeta();
                        if (im != null) {
                            im.displayName(Component.text(formatMaterial(mat.name()),
                                    NamedTextColor.YELLOW));
                            ing.setItemMeta(im);
                        }
                        inventory.setItem(ROW_ANCHORS[row] + col, ing);
                    } catch (IllegalArgumentException ignored) { /* skip bad material */ }
                }
            }
        }

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta am = arrow.getItemMeta();
        am.displayName(Component.text("➜", NamedTextColor.YELLOW));
        arrow.setItemMeta(am);
        inventory.setItem(ARROW_SLOT, arrow);

        ItemStack result = weaponManager.createWeapon(def.getId());
        if (result != null) inventory.setItem(RESULT_SLOT, result);
    }

    /**
     * "DIAMOND_SWORD" → "Diamond Sword"
     */
    private static @NotNull String formatMaterial(@NotNull String matName) {
        String[] parts = matName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
package me.sunmc.vw.gui;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.config.ConfigManager;
import me.sunmc.vw.weapon.WeaponData;
import me.sunmc.vw.weapon.WeaponItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages all GUI interactions for VaultWeapons
 */
public class GUIManager implements Listener {

    private final VaultWeaponsPlugin plugin;
    private final Map<UUID, RecipeGUI> openGuis;
    private final Map<UUID, Integer> currentPage;
    private final List<Integer> craftingSlots;
    private final int resultSlot;

    public GUIManager(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
        this.currentPage = new HashMap<>();

        List<Integer> slots = plugin.getConfigManager().getMainConfig()
                .getIntegerList("recipe-display.crafting-slots");
        this.craftingSlots = slots.isEmpty() ?
                List.of(10, 11, 12, 19, 20, 21, 28, 29, 30) : slots;
        this.resultSlot = plugin.getConfigManager().getMainConfig()
                .getInt("recipe-display.result-slot", 23);
    }

    /**
     * Open the recipe list GUI for a player
     *
     * @param player The player
     * @param page   The page number (0-indexed)
     */
    public void openRecipeListGUI(@NotNull Player player, int page) {
        List<WeaponData> weapons = new ArrayList<>(plugin.getWeaponManager().getAllWeapons());

        if (weapons.isEmpty()) {
            player.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("weapons.weapon-not-found",
                            "weapon", "any")));
            return;
        }


        currentPage.put(player.getUniqueId(), page);

        String title = ConfigManager.translateColors(
                plugin.getConfigManager().getMainConfig().getString("recipe-gui.title", "&8&lWeapon Recipes"));
        Inventory inventory = Bukkit.createInventory(new RecipeListHolder(), 27, Component.text(title));


        ItemStack filler = WeaponItemBuilder.createFiller();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Add weapon items (up to 7 per page)
        int startIndex = page * 7;
        int[] displaySlots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < displaySlots.length; i++) {
            int weaponIndex = startIndex + i;
            if (weaponIndex < weapons.size()) {
                WeaponData weapon = weapons.get(weaponIndex);
                ItemStack displayItem = createWeaponDisplayItem(weapon);
                inventory.setItem(displaySlots[i], displayItem);
            }
        }

        // Navigation
        if (page > 0) {
            ItemStack prevItem = createNavItem(Material.ARROW, "&aPrevious Page");
            inventory.setItem(18, prevItem);
        }

        if ((page + 1) * 7 < weapons.size()) {
            ItemStack nextItem = createNavItem(Material.ARROW, "&cNext Page");
            inventory.setItem(26, nextItem);
        }


        ItemStack closeItem = createNavItem(Material.BARRIER, "&cClose");
        inventory.setItem(22, closeItem);

        player.openInventory(inventory);
    }

    /**
     * Open the recipe detail GUI for a specific weapon
     *
     * @param player The player
     * @param weapon The weapon data
     */
    public void openRecipeDetailGUI(@NotNull Player player, @NotNull WeaponData weapon) {
        String title = ConfigManager.translateColors(
                plugin.getConfigManager().getMessage("gui.recipe-title",
                        "weapon", weapon.getName()));
        Inventory inventory = Bukkit.createInventory(new RecipeDetailHolder(weapon), 27, Component.text(title));


        ItemStack filler = WeaponItemBuilder.createFiller();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        if (weapon.hasRecipe()) {
            Material[] slots = new Material[0];
            if (weapon.getRecipe() != null) {
                slots = weapon.getRecipe().toSlotArray();
            }
            for (int i = 0; i < 9 && i < craftingSlots.size(); i++) {
                Material material = slots[i];
                if (material != null && material != Material.AIR) {
                    ItemStack ingredient = new ItemStack(material);
                    inventory.setItem(craftingSlots.get(i), ingredient);
                }
            }
        }

        // Display result
        ItemStack result = weapon.createItemStack(0);
        inventory.setItem(resultSlot, result);

        // Add back button
        ItemStack backItem = createNavItem(Material.SPECTRAL_ARROW, "&eBack to List");
        inventory.setItem(18, backItem);

        // Add close button
        ItemStack closeItem = createNavItem(Material.BARRIER, "&cClose");
        inventory.setItem(26, closeItem);

        // Store open GUI
        openGuis.put(player.getUniqueId(), new RecipeGUI(weapon, true));

        player.openInventory(inventory);
    }

    /**
     * Create a weapon display item for the recipe list
     *
     * @param weapon The weapon data
     * @return The display ItemStack
     */
    @NotNull
    private ItemStack createWeaponDisplayItem(@NotNull WeaponData weapon) {
        return weapon.createItemStack(0);
    }

    /**
     * Create a navigation item
     *
     * @param material The material
     * @param name     The display name
     * @return The created ItemStack
     */
    @NotNull
    private ItemStack createNavItem(@NotNull Material material, @NotNull String name) {
        return WeaponItemBuilder.createDisplayItem(material, name, List.of());
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof RecipeListHolder) {
            event.setCancelled(true);
            handleRecipeListClick(player, event.getSlot());
        } else if (holder instanceof RecipeDetailHolder detailHolder) {
            event.setCancelled(true);
            handleRecipeDetailClick(player, event.getSlot(), detailHolder.getWeapon());
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openGuis.remove(player.getUniqueId());
        }
    }

    /**
     * Handle clicks in the recipe list GUI
     *
     * @param player The player
     * @param slot   The clicked slot
     */
    private void handleRecipeListClick(@NotNull Player player, int slot) {
        List<WeaponData> weapons = new ArrayList<>(plugin.getWeaponManager().getAllWeapons());
        int page = currentPage.getOrDefault(player.getUniqueId(), 0);

        // Display slots
        int[] displaySlots = {10, 11, 12, 13, 14, 15, 16};
        int slotIndex = -1;
        for (int i = 0; i < displaySlots.length; i++) {
            if (displaySlots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            int weaponIndex = page * 7 + slotIndex;
            if (weaponIndex < weapons.size()) {
                openRecipeDetailGUI(player, weapons.get(weaponIndex));
            }
        } else if (slot == 18 && page > 0) {
            // Previous page
            openRecipeListGUI(player, page - 1);
        } else if (slot == 26 && (page + 1) * 7 < weapons.size()) {
            // Next page
            openRecipeListGUI(player, page + 1);
        } else if (slot == 22) {
            // Close
            player.closeInventory();
        }
    }

    /**
     * Handle clicks in the recipe detail GUI
     *
     * @param player The player
     * @param slot   The clicked slot
     * @param weapon The weapon being viewed
     */
    private void handleRecipeDetailClick(@NotNull Player player, int slot, @NotNull WeaponData weapon) {
        if (slot == 18) {
            // Back to list
            int page = currentPage.getOrDefault(player.getUniqueId(), 0);
            openRecipeListGUI(player, page);
        } else if (slot == 26) {
            // Close
            player.closeInventory();
        }
    }

    /**
     * Get the current page for a player
     *
     * @param player The player
     * @return The current page
     */
    public int getCurrentPage(@NotNull Player player) {
        return currentPage.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Clear GUI data for a player
     *
     * @param player The player
     */
    public void clearPlayerData(@NotNull Player player) {
        openGuis.remove(player.getUniqueId());
        currentPage.remove(player.getUniqueId());
    }
}

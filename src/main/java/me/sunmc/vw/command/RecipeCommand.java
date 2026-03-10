package me.sunmc.vw.command;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.config.ConfigManager;
import me.sunmc.vw.weapon.WeaponData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for viewing weapon recipes
 */
public class RecipeCommand implements CommandExecutor, TabCompleter {

    private final VaultWeaponsPlugin plugin;

    public RecipeCommand(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("vaultweapons.recipe")) {
            player.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return true;
        }

        if (args.length == 0) {
            // Open recipe list GUI
            plugin.getGuiManager().openRecipeListGUI(player, 0);
            return true;
        }

        // Try to find weapon by ID or name
        String searchTerm = String.join("_", args).toLowerCase();
        WeaponData weapon = findWeapon(searchTerm);

        if (weapon == null) {
            player.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("weapons.weapon-not-found",
                            "weapon", searchTerm)));
            return true;
        }

        // Open recipe detail GUI
        plugin.getGuiManager().openRecipeDetailGUI(player, weapon);
        return true;
    }

    /**
     * Find a weapon by ID or partial name match
     *
     * @param searchTerm The search term
     * @return The found WeaponData, or null if not found
     */
    @Nullable
    private WeaponData findWeapon(@NotNull String searchTerm) {
        // Try exact ID match first
        WeaponData weapon = plugin.getWeaponManager().getWeapon(searchTerm);
        if (weapon != null) {
            return weapon;
        }

        // Try partial matches
        for (WeaponData w : plugin.getWeaponManager().getAllWeapons()) {
            // Check if ID contains the search term
            if (w.getId().toLowerCase().contains(searchTerm)) {
                return w;
            }

            String cleanName = ConfigManager.translateColors(w.getName())
                    .replaceAll("[\\u00A7&][0-9a-fk-or]", "")
                    .toLowerCase();
            if (cleanName.contains(searchTerm)) {
                return w;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();
        for (WeaponData weapon : plugin.getWeaponManager().getAllWeapons()) {
            completions.add(weapon.getId());
        }

        String input = String.join("_", args).toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));

        return completions;
    }
}
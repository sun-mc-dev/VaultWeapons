package me.sunmc.vw.command;

import me.sunmc.vw.VaultWeaponsPlugin;
import me.sunmc.vw.config.ConfigManager;
import me.sunmc.vw.weapon.WeaponData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main command handler for VaultWeapons
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final VaultWeaponsPlugin plugin;

    public MainCommand(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "recipes" -> handleRecipes(sender);
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.unknown-command")));
        }

        return true;
    }

    /**
     * Handle the reload subcommand
     *
     * @param sender The command sender
     */
    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("vaultweapons.admin")) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        CompletableFuture.runAsync(() -> {
            plugin.getConfigManager().reloadConfigs();
            plugin.getWeaponManager().loadWeapons();
        }, plugin.getExecutorService()).thenRun(() -> {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.reload")));
        });
    }

    /**
     * Handle the recipes subcommand
     *
     * @param sender The command sender
     */
    private void handleRecipes(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("vaultweapons.recipe")) {
            player.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        plugin.getGuiManager().openRecipeListGUI(player, 0);
    }

    /**
     * Handle the give subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     */
    private void handleGive(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("vaultweapons.admin")) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.invalid-args",
                            "usage", "/vw give <player> <weapon> [amount]")));
            return;
        }

        String playerName = args[1];
        String weaponId = args[2].toLowerCase();
        int amount = 1;

        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) {
                    amount = 1;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ConfigManager.translateColors(
                        plugin.getConfigManager().getMessage("admin.invalid-amount",
                                "amount", args[3])));
                return;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("admin.player-not-found",
                            "player", playerName)));
            return;
        }

        WeaponData weaponData = plugin.getWeaponManager().getWeapon(weaponId);
        if (weaponData == null) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("weapons.weapon-not-found",
                            "weapon", weaponId)));
            return;
        }

        int finalAmount = amount;
        plugin.runSync(() -> {
            plugin.getWeaponManager().giveWeapon(target, weaponId, 0, finalAmount);
        });

        sender.sendMessage(ConfigManager.translateColors(
                plugin.getConfigManager().getMessage("admin.weapon-given",
                        "weapon", weaponData.getName(),
                        "amount", String.valueOf(amount),
                        "player", target.getName())));
    }

    /**
     * Handle the list subcommand
     *
     * @param sender The command sender
     */
    private void handleList(@NotNull CommandSender sender) {
        if (!sender.hasPermission("vaultweapons.recipe")) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        sender.sendMessage(ConfigManager.translateColors(
                plugin.getConfigManager().getMessage("help.weapons-header")));

        for (WeaponData weapon : plugin.getWeaponManager().getAllWeapons()) {
            sender.sendMessage(ConfigManager.translateColors(
                    plugin.getConfigManager().getMessage("help.weapon-list",
                            "weapon", weapon.getName(),
                            "description", weapon.getDescription())));
        }
    }

    /**
     * Send help message to sender
     *
     * @param sender The command sender
     */
    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(ConfigManager.translateColors(
                plugin.getConfigManager().getMessage("help.header")));

        for (String cmd : plugin.getConfigManager().getMessagesConfig().getStringList("help.commands")) {
            sender.sendMessage(ConfigManager.translateColors(cmd));
        }

        sender.sendMessage(ConfigManager.translateColors(
                plugin.getConfigManager().getMessage("help.footer")));
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("recipes");
            completions.add("give");
            completions.add("list");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getWeaponManager().getWeaponIds());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            completions.add("1");
            completions.add("10");
            completions.add("64");
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));

        return completions;
    }
}
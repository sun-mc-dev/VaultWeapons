package me.sunmc.vw.command;

import me.sunmc.vw.VaultWeapons;
import me.sunmc.vw.config.WeaponConfigLoader;
import me.sunmc.vw.gui.RecipeGUI;
import me.sunmc.vw.manager.WeaponManager;
import me.sunmc.vw.model.WeaponDefinition;
import me.sunmc.vw.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VWCommand implements TabExecutor {

    private final VaultWeapons plugin;
    private final WeaponManager weaponManager;
    private final WeaponConfigLoader configLoader;

    public VWCommand(VaultWeapons plugin, WeaponManager weaponManager,
                     WeaponConfigLoader configLoader) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        this.configLoader = configLoader;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> cmdList(sender);
            case "recipe" -> cmdRecipe(sender, args);
            case "give" -> cmdGive(sender, args);
            case "reload" -> cmdReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void cmdList(CommandSender sender) {
        if (configLoader.getWeapons().isEmpty()) {
            sender.sendMessage(Component.text("No weapons loaded.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("── VaultWeapons ─────────────────", NamedTextColor.GOLD));
        for (WeaponDefinition def : configLoader.getWeapons().values()) {
            sender.sendMessage(
                    Component.text("  • ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(def.getId(), NamedTextColor.AQUA))
                            .append(Component.text(" → ", NamedTextColor.GRAY))
                            .append(TextUtil.parse(def.getDisplayName()))
            );
        }
    }

    private void cmdRecipe(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can open GUIs.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("vaultweapons.use")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /vw recipe <weapon_id>", NamedTextColor.RED));
            return;
        }
        WeaponDefinition def = configLoader.getWeapon(args[1]);
        if (def == null) {
            player.sendMessage(Component.text("Unknown weapon: " + args[1], NamedTextColor.RED));
            return;
        }
        new RecipeGUI(def, weaponManager).open(player);
    }

    private void cmdGive(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultweapons.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /vw give <player> <weapon_id>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return;
        }
        WeaponDefinition def = configLoader.getWeapon(args[2]);
        if (def == null) {
            sender.sendMessage(Component.text("Unknown weapon: " + args[2], NamedTextColor.RED));
            return;
        }
        ItemStack item = weaponManager.createWeapon(def.getId());
        if (item == null) {
            sender.sendMessage(Component.text("Failed to create item.", NamedTextColor.RED));
            return;
        }
        target.getInventory().addItem(item);
        sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                .append(TextUtil.parse(def.getDisplayName()))
                .append(Component.text(" to " + target.getName() + ".", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                .append(TextUtil.parse(def.getDisplayName()))
                .append(Component.text("!", NamedTextColor.GREEN)));
    }

    private void cmdReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("vaultweapons.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Reloading VaultWeapons…", NamedTextColor.YELLOW));
        plugin.reloadPlugin();
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("── VaultWeapons Help ───────────────", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/vw list", NamedTextColor.AQUA)
                .append(Component.text(" — List all weapons", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/vw recipe <id>", NamedTextColor.AQUA)
                .append(Component.text(" — View crafting recipe", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/vw give <player> <id>", NamedTextColor.AQUA)
                .append(Component.text(" — Give a weapon [admin]", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/vw reload", NamedTextColor.AQUA)
                .append(Component.text(" — Reload config [admin]", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String @NotNull [] args) {
        if (args.length == 1)
            return filter(List.of("list", "recipe", "give", "reload"), args[0]);
        if (args.length == 2)
            return switch (args[0].toLowerCase()) {
                case "recipe", "give" -> filter(new ArrayList<>(configLoader.getWeapons().keySet()), args[1]);
                default -> Collections.emptyList();
            };
        if (args.length == 3 && "give".equalsIgnoreCase(args[0]))
            return filter(new ArrayList<>(configLoader.getWeapons().keySet()), args[2]);
        return Collections.emptyList();
    }

    private static @NotNull List<String> filter(@NotNull List<String> opts, String prefix) {
        List<String> out = new ArrayList<>();
        for (String o : opts)
            if (o.toLowerCase().startsWith(prefix.toLowerCase())) out.add(o);
        return out;
    }
}
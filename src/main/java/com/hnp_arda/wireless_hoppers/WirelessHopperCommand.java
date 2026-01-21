package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

final class WirelessHopperCommand implements CommandExecutor, TabCompleter {
    private final ResourcePackManager resourcePackManager;

    WirelessHopperCommand(ResourcePackManager resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("pack")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return true;
            }
            if (!sender.hasPermission("wirelesshopper.pack")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            resourcePackManager.prompt(player);
            return true;
        }
        if (!sender.hasPermission("wirelesshopper.give")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " give <player> <item>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Items: hopper, tool, upgrade_iron, upgrade_gold, upgrade_diamond, upgrade_netherite, all", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Or: /" + label + " pack", NamedTextColor.YELLOW));
            return true;
        }
        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /" + label + " give <player> <item>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /" + label + " give <player> <item>", NamedTextColor.YELLOW));
            return true;
        }
        String itemKey = args[2].toLowerCase();
        List<ItemStack> items = new ArrayList<>();
        switch (itemKey) {
            case "hopper" -> items.add(WirelessItems.createHopperItem());
            case "tool" -> items.add(WirelessItems.createTargetTool());
            case "upgrade_iron" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.IRON));
            case "upgrade_gold" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.GOLD));
            case "upgrade_diamond" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND));
            case "upgrade_netherite" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE));
            case "all" -> {
                items.add(WirelessItems.createHopperItem());
                items.add(WirelessItems.createTargetTool());
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.IRON));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.GOLD));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE));
            }
            default -> {
                sender.sendMessage(Component.text("Unknown item. Use: hopper, tool, upgrade_iron, upgrade_gold, upgrade_diamond, upgrade_netherite, all", NamedTextColor.RED));
                return true;
            }
        }
        for (ItemStack stack : items) {
            target.getInventory().addItem(stack);
        }
        sender.sendMessage(Component.text("Gave items to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NonNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("give");
            suggestions.add("pack");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            suggestions.add("hopper");
            suggestions.add("tool");
            suggestions.add("upgrade_iron");
            suggestions.add("upgrade_gold");
            suggestions.add("upgrade_diamond");
            suggestions.add("upgrade_netherite");
            suggestions.add("all");
        }
        return suggestions;
    }
}

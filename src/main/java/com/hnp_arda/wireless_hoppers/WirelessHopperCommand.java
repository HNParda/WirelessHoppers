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
        if (args.length >= 1 && (args[0].equalsIgnoreCase("lang") || args[0].equalsIgnoreCase("language"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text(Lang.tr(Lang.defaultLocale(), "command.only_players"), NamedTextColor.RED));
                return true;
            }
            String locale = Lang.localeFromPlayer(player);
            if (args.length == 1) {
                player.sendMessage(Component.text(
                    Lang.tr(player, "command.lang.current", java.util.Map.of("lang", locale)),
                    NamedTextColor.YELLOW
                ));
                player.sendMessage(Component.text(
                    Lang.tr(player, "command.lang.usage", java.util.Map.of("label", label)),
                    NamedTextColor.YELLOW
                ));
                return true;
            }
            String requested = args[1].toLowerCase();
            if ("auto".equalsIgnoreCase(requested)) {
                Lang.setPlayerLocale(player.getUniqueId(), "auto");
                player.sendMessage(Component.text(
                    Lang.tr(player, "command.lang.set_auto"),
                    NamedTextColor.GREEN
                ));
                return true;
            }
            if (!Lang.availableLocales().contains(requested)) {
                player.sendMessage(Component.text(
                    Lang.tr(player, "command.lang.invalid"),
                    NamedTextColor.RED
                ));
                return true;
            }
            Lang.setPlayerLocale(player.getUniqueId(), requested);
            player.sendMessage(Component.text(
                Lang.tr(player, "command.lang.set", java.util.Map.of("lang", requested)),
                NamedTextColor.GREEN
            ));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("pack")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text(Lang.tr(Lang.defaultLocale(), "command.only_players"), NamedTextColor.RED));
                return true;
            }
            if (!sender.hasPermission("wirelesshopper.pack")) {
                sender.sendMessage(Component.text(Lang.tr(player, "command.no_permission"), NamedTextColor.RED));
                return true;
            }
            resourcePackManager.prompt(player);
            return true;
        }
        if (!sender.hasPermission("wirelesshopper.give")) {
            String locale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
            sender.sendMessage(Component.text(Lang.tr(locale, "command.no_permission"), NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            String locale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
            sender.sendMessage(Component.text(
                Lang.tr(locale, "command.usage_give", java.util.Map.of("label", label)),
                NamedTextColor.YELLOW
            ));
            sender.sendMessage(Component.text(Lang.tr(locale, "command.items"), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                Lang.tr(locale, "command.pack", java.util.Map.of("label", label)),
                NamedTextColor.YELLOW
            ));
            return true;
        }
        if (!args[0].equalsIgnoreCase("give")) {
            String locale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
            sender.sendMessage(Component.text(
                Lang.tr(locale, "command.usage_give", java.util.Map.of("label", label)),
                NamedTextColor.YELLOW
            ));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            String locale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
            sender.sendMessage(Component.text(Lang.tr(locale, "command.player_not_found"), NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            String locale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
            sender.sendMessage(Component.text(
                Lang.tr(locale, "command.usage_give", java.util.Map.of("label", label)),
                NamedTextColor.YELLOW
            ));
            return true;
        }
        String itemKey = args[2].toLowerCase();
        String locale = Lang.localeFromPlayer(target);
        List<ItemStack> items = new ArrayList<>();
        switch (itemKey) {
            case "hopper" -> items.add(WirelessItems.createHopperItem(locale));
            case "tool" -> items.add(WirelessItems.createTargetTool(locale));
            case "upgrade_iron" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.IRON, locale));
            case "upgrade_gold" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.GOLD, locale));
            case "upgrade_diamond" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND, locale));
            case "upgrade_netherite" -> items.add(WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE, locale));
            case "all" -> {
                items.add(WirelessItems.createHopperItem(locale));
                items.add(WirelessItems.createTargetTool(locale));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.IRON, locale));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.GOLD, locale));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND, locale));
                items.add(WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE, locale));
            }
            default -> {
                String senderLocale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
                sender.sendMessage(Component.text(Lang.tr(senderLocale, "command.unknown_item"), NamedTextColor.RED));
                return true;
            }
        }
        for (ItemStack stack : items) {
            target.getInventory().addItem(stack);
        }
        String senderLocale = sender instanceof Player player ? Lang.localeFromPlayer(player) : Lang.defaultLocale();
        sender.sendMessage(Component.text(
            Lang.tr(senderLocale, "command.gave_items", java.util.Map.of("player", target.getName())),
            NamedTextColor.GREEN
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NonNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("give");
            suggestions.add("pack");
            suggestions.add("lang");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("lang") || args[0].equalsIgnoreCase("language"))) {
            suggestions.add("auto");
            suggestions.addAll(Lang.availableLocales());
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

package com.hnp_arda.wireless_hoppers;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class WirelessItems {
    static final String TYPE_HOPPER = "wireless_hopper";
    static final String TYPE_UPGRADE = "upgrade";
    static final String TYPE_TOOL = "target_tool";
    private static final String MODEL_HOPPER = "wireless_hopper_item";
    private static final String MODEL_TOOL = "wireless_target_tool";
    private static final String MODEL_UPGRADE_IRON = "wireless_upgrade_iron";
    private static final String MODEL_UPGRADE_GOLD = "wireless_upgrade_gold";
    private static final String MODEL_UPGRADE_DIAMOND = "wireless_upgrade_diamond";
    private static final String MODEL_UPGRADE_NETHERITE = "wireless_upgrade_netherite";

    private WirelessItems() {
    }

    static ItemStack createHopperItem() {
        return createHopperItem(Lang.defaultLocale());
    }

    static ItemStack createHopperItem(String locale) {
        ItemStack item = new ItemStack(Material.BAMBOO_MOSAIC_SLAB);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Lang.tr(locale, "items.hopper.name"), NamedTextColor.GOLD));
        meta.setItemModel(new org.bukkit.NamespacedKey(Keys.BLOCK_MARKER.getNamespace(), MODEL_HOPPER));
        meta.lore(List.of(
            Component.text(Lang.tr(locale, "items.hopper.lore.row1"), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "items.hopper.lore.row234"), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "items.hopper.lore.row5"), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "items.hopper.lore.configure"), NamedTextColor.DARK_GRAY)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.ITEM_TYPE, PersistentDataType.STRING, TYPE_HOPPER);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack createUpgradeItem(UpgradeTier tier) {
        return createUpgradeItem(tier, Lang.defaultLocale());
    }

    static ItemStack createUpgradeItem(UpgradeTier tier, String locale) {
        Material material = switch (tier) {
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case DIAMOND -> Material.DIAMOND;
            case NETHERITE -> Material.NETHERITE_INGOT;
        };
        ItemStack item = new ItemStack(material);
        applyUpgradeLore(item, tier, locale);
        return item;
    }

    static void applyUpgradeLore(ItemStack item, UpgradeTier tier, String locale) {
        if (item == null || item.getType().isAir() || tier == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(Component.text(Lang.tr(locale, tier.langKey()), NamedTextColor.AQUA));
        meta.setItemModel(new org.bukkit.NamespacedKey(Keys.BLOCK_MARKER.getNamespace(), switch (tier) {
            case IRON -> MODEL_UPGRADE_IRON;
            case GOLD -> MODEL_UPGRADE_GOLD;
            case DIAMOND -> MODEL_UPGRADE_DIAMOND;
            case NETHERITE -> MODEL_UPGRADE_NETHERITE;
        }));
        meta.lore(List.of(
            Component.text(Lang.tr(locale, "gui.upgrade.cooldown", java.util.Map.of("ticks", String.valueOf(tier.cooldownTicks()))), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "gui.upgrade.items_per", java.util.Map.of("count", String.valueOf(tier.itemsPerTransfer()))), NamedTextColor.GRAY)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.ITEM_TYPE, PersistentDataType.STRING, TYPE_UPGRADE);
        pdc.set(Keys.UPGRADE, PersistentDataType.STRING, tier.name());
        item.setItemMeta(meta);
    }

    static ItemStack createTargetTool() {
        return createTargetTool(Lang.defaultLocale());
    }

    static ItemStack createTargetTool(String locale) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Lang.tr(locale, "items.tool.name"), NamedTextColor.GREEN));
        meta.setItemModel(new org.bukkit.NamespacedKey(Keys.BLOCK_MARKER.getNamespace(), MODEL_TOOL));
        meta.lore(List.of(
            Component.text(Lang.tr(locale, "items.tool.lore.bind"), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "items.tool.lore.insert"), NamedTextColor.DARK_GRAY)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.ITEM_TYPE, PersistentDataType.STRING, TYPE_TOOL);
        item.setItemMeta(meta);
        return item;
    }

    static boolean isWirelessHopper(ItemStack item) {
        return hasType(item, TYPE_HOPPER);
    }

    static boolean isUpgrade(ItemStack item) {
        return hasType(item, TYPE_UPGRADE);
    }

    static boolean isTargetTool(ItemStack item) {
        return hasType(item, TYPE_TOOL);
    }

    static UpgradeTier getUpgradeTier(ItemStack item) {
        if (!isUpgrade(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String tierName = meta.getPersistentDataContainer().get(Keys.UPGRADE, PersistentDataType.STRING);
        if (tierName == null) {
            return null;
        }
        try {
            return UpgradeTier.valueOf(tierName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static boolean hasType(ItemStack item, String type) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String stored = meta.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING);
        return type.equals(stored);
    }
}

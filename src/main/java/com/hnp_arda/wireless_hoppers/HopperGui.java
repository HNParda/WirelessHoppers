package com.hnp_arda.wireless_hoppers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

final class HopperGui {
    static final int SIZE = 54;
    static final int BUFFER_START = 0;
    static final int BUFFER_END = 8;
    static final int FILTER_START = 9;
    static final int FILTER_END = 35;
    static final int UPGRADE_SLOT = 36;
    static final int TARGET_SLOT = 40;
    static final int TOGGLE_SLOT = 44;
    static final int STATUS_START = 45;
    static final int STATUS_END = 53;

    private HopperGui() {
    }

    static Inventory create(HopperRegistry.HopperPos pos, HopperData data, String locale) {
        Inventory inventory = Bukkit.createInventory(new HopperGuiHolder(pos, locale), SIZE,
            Component.text(Lang.tr(locale, "gui.title"), NamedTextColor.DARK_AQUA));
        for (int i = FILTER_START; i <= FILTER_END; i++) {
            inventory.setItem(i, filterDisplay(i - FILTER_START, data.filters()[i - FILTER_START], locale));
        }
        for (int i = BUFFER_START; i <= BUFFER_END; i++) {
            inventory.setItem(i, cloneSingle(data.buffer()[i]));
        }
        ItemStack upgrade = cloneSingle(data.upgradeItem());
        if (upgrade != null) {
            UpgradeTier tier = WirelessItems.getUpgradeTier(upgrade);
            WirelessItems.applyUpgradeLore(upgrade, tier, locale);
        }
        inventory.setItem(UPGRADE_SLOT, upgrade);
        inventory.setItem(TARGET_SLOT, cloneSingle(data.targetItem()));
        inventory.setItem(TOGGLE_SLOT, toggleItem(data, locale));

        ItemStack filler = fillerItem(Component.text(" ", NamedTextColor.DARK_GRAY));
        for (int i = BUFFER_END + 1; i < STATUS_START; i++) {
            if (i == UPGRADE_SLOT || i == TARGET_SLOT || i == TOGGLE_SLOT) {
                continue;
            }
            inventory.setItem(i, filler);
        }
        writeStatus(inventory, data);
        return inventory;
    }

    static boolean isBufferSlot(int slot) {
        return slot >= BUFFER_START && slot <= BUFFER_END;
    }

    static boolean isFilterSlot(int slot) {
        return slot >= FILTER_START && slot <= FILTER_END;
    }

    static boolean isStatusSlot(int slot) {
        return slot >= STATUS_START && slot <= STATUS_END;
    }

    static ItemStack toggleItem(HopperData data, String locale) {
        boolean whitelist = data.isWhitelist();
        int count = countItems(data.filters());
        Material material = whitelist ? Material.LIME_DYE : Material.RED_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(
            Lang.tr(locale, whitelist ? "gui.toggle.whitelist" : "gui.toggle.blacklist"),
            whitelist ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        meta.lore(List.of(
                Component.text(Lang.tr(locale, "gui.toggle.click"), NamedTextColor.GRAY),
                Component.text(Lang.tr(locale,
                        whitelist ? "gui.toggle.whitelist_summary" : "gui.toggle.blacklist_summary",
                        java.util.Map.of("count", String.valueOf(count))),
                        whitelist ? NamedTextColor.GREEN : NamedTextColor.RED)
        ));
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack fillerItem(Component name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack filterDisplay(int index, ItemStack filter, String locale) {
        if (filter == null || filter.getType().isAir()) {
            ItemStack pane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
            ItemMeta meta = pane.getItemMeta();
            meta.displayName(Component.text(Lang.tr(locale, "gui.filter.slot",
                java.util.Map.of("index", String.valueOf(index + 1))), NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text(Lang.tr(locale, "gui.filter.empty"), NamedTextColor.GRAY),
                Component.text(Lang.tr(locale, "gui.filter.set"), NamedTextColor.DARK_GRAY)
            ));
            meta.getPersistentDataContainer().set(Keys.FILTER_PANE, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            pane.setItemMeta(meta);
            return pane;
        }
        ItemStack display = cloneSingle(filter);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            meta.displayName(Component.text(Lang.tr(locale, "gui.filter.slot",
                java.util.Map.of("index", String.valueOf(index + 1))), NamedTextColor.AQUA));
            lore.add(Component.text(Lang.tr(locale, "gui.filter.item",
                java.util.Map.of("item", displayName(filter))), NamedTextColor.GRAY));
            lore.add(Component.text(Lang.tr(locale, "gui.filter.replace"), NamedTextColor.DARK_GRAY));
            lore.add(Component.text(Lang.tr(locale, "gui.filter.clear"), NamedTextColor.DARK_GRAY));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(Keys.FILTER_PANE, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            display.setItemMeta(meta);
        }
        return display;
    }

    static ItemStack statusItem(String name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    static List<ItemStack> statusItems(HopperData data, String locale) {
        List<ItemStack> items = new ArrayList<>();
        items.add(statusItem(Lang.tr(locale, "gui.status.upgrade"),
            upgradeLore(data, locale)));
        items.add(statusItem(Lang.tr(locale, "gui.status.target"),
            targetLore(data, locale)));
        items.add(statusItem(Lang.tr(locale, "gui.status.mode"),
            List.of(filterSummary(data, locale))));
        return items;
    }

    static ItemStack cloneSingle(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack clone = item.clone();
        if (clone.getAmount() > 1) {
            clone.setAmount(1);
        }
        return clone;
    }

    static ItemStack[] readBuffer(Inventory inventory) {
        ItemStack[] buffer = new ItemStack[HopperData.BUFFER_SLOTS];
        for (int i = BUFFER_START; i <= BUFFER_END; i++) {
            buffer[i] = inventory.getItem(i);
        }
        return buffer;
    }

    static void writeBuffer(Inventory inventory, ItemStack[] buffer) {
        for (int i = BUFFER_START; i <= BUFFER_END; i++) {
            inventory.setItem(i, buffer[i]);
        }
    }

    static void writeFilters(Inventory inventory, ItemStack[] filters, String locale) {
        for (int i = FILTER_START; i <= FILTER_END; i++) {
            int index = i - FILTER_START;
            inventory.setItem(i, filterDisplay(index, filters[index], locale));
        }
    }

    static void writeStatus(Inventory inventory, HopperData data) {
        String locale = localeFromInventory(inventory);
        ItemStack filler = fillerItem(Component.text(" ", NamedTextColor.DARK_GRAY));
        for (int i = STATUS_START; i <= STATUS_END; i++) {
            inventory.setItem(i, filler);
        }
        List<ItemStack> statusItems = statusItems(data, locale);
        if (!statusItems.isEmpty()) {
            inventory.setItem(45, statusItems.getFirst());
        }
        if (statusItems.size() > 1) {
            inventory.setItem(49, statusItems.get(1));
        }
        if (statusItems.size() > 2) {
            inventory.setItem(53, statusItems.get(2));
        }
        inventory.setItem(TOGGLE_SLOT, toggleItem(data, locale));
    }

    private static Component filterSummary(HopperData data, String locale) {
        int count = countItems(data.filters());
        if (data.isWhitelist()) {
            return Component.text(Lang.tr(locale, "gui.toggle.whitelist_summary",
                java.util.Map.of("count", String.valueOf(count))), NamedTextColor.GREEN);
        }
        return Component.text(Lang.tr(locale, "gui.toggle.blacklist_summary",
            java.util.Map.of("count", String.valueOf(count))), NamedTextColor.RED);
    }

    private static List<Component> upgradeLore(HopperData data, String locale) {
        UpgradeTier tier = data.upgradeTier();
        if (tier == null) {
            return List.of(
                Component.text(Lang.tr(locale, "gui.upgrade.none"), NamedTextColor.GRAY),
                Component.text(Lang.tr(locale, "gui.upgrade.cooldown",
                    java.util.Map.of("ticks", String.valueOf(TransferConfig.defaultCooldownTicks()))), NamedTextColor.GRAY),
                Component.text(Lang.tr(locale, "gui.upgrade.items_per",
                    java.util.Map.of("count", String.valueOf(TransferConfig.defaultItemsPerTransfer()))), NamedTextColor.GRAY)
            );
        }
        return List.of(
                Component.text(Lang.tr(locale, tier.langKey()), NamedTextColor.AQUA),
                Component.text(Lang.tr(locale, "gui.upgrade.cooldown",
                    java.util.Map.of("ticks", String.valueOf(tier.cooldownTicks()))), NamedTextColor.GRAY),
                Component.text(Lang.tr(locale, "gui.upgrade.items_per",
                    java.util.Map.of("count", String.valueOf(tier.itemsPerTransfer()))), NamedTextColor.GRAY)
        );
    }

    private static List<Component> targetLore(HopperData data, String locale) {
        HopperData.TargetInfo target = data.targetInfo();
        if (target == null) {
            return List.of(Component.text(Lang.tr(locale, "gui.target.not_linked"), NamedTextColor.GRAY));
        }
        return List.of(
                Component.text(target.inventoryType(), NamedTextColor.YELLOW),
                Component.text(Lang.tr(locale, "gui.target.coords", java.util.Map.of(
                    "X", String.valueOf(target.x()),
                    "Y", String.valueOf(target.y()),
                    "Z", String.valueOf(target.z())
                )), NamedTextColor.GRAY)
        );
    }

    private static int countItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    private static String displayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) {
                return PlainTextComponentSerializer.plainText().serialize(name);
            }
        }
        return item.getType().name();
    }

    static String localeFromInventory(Inventory inventory) {
        if (inventory.getHolder() instanceof HopperGuiHolder holder) {
            return holder.locale();
        }
        return Lang.defaultLocale();
    }

    record HopperGuiHolder(HopperRegistry.HopperPos pos, String locale) implements InventoryHolder {

        @Override
        public @NonNull Inventory getInventory() {
            return Bukkit.createInventory(this, SIZE);
        }
    }
}

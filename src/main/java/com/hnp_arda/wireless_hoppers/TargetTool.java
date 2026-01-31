package com.hnp_arda.wireless_hoppers;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class TargetTool {
    private TargetTool() {
    }

    static void writeTarget(ItemStack tool, BlockState state, String locale) {
        if (tool == null || state == null) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return;
        }
        Location loc = state.getLocation();
        String inventoryType = state instanceof org.bukkit.inventory.InventoryHolder holder
            ? holder.getInventory().getType().name()
            : state.getType().name();
        String payload = loc.getWorld().getUID() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ()
            + ";" + inventoryType;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.TOOL_TARGET, PersistentDataType.STRING, payload);
        meta.lore(List.of(
            Component.text(Lang.tr(locale, "tool.linked", java.util.Map.of("type", inventoryType)), NamedTextColor.GRAY),
            Component.text(Lang.tr(locale, "tool.coords", java.util.Map.of(
                "X", String.valueOf(loc.getBlockX()),
                "Y", String.valueOf(loc.getBlockY()),
                "Z", String.valueOf(loc.getBlockZ())
            )), NamedTextColor.GRAY)
        ));
        tool.setItemMeta(meta);
    }

    static HopperData.TargetInfo readTarget(ItemStack tool) {
        if (tool == null) {
            return null;
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return null;
        }
        String payload = meta.getPersistentDataContainer().get(Keys.TOOL_TARGET, PersistentDataType.STRING);
        if (payload == null) {
            return null;
        }
        return HopperData.TargetInfo.fromString(payload);
    }
}

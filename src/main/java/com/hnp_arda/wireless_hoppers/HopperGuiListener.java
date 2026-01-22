package com.hnp_arda.wireless_hoppers;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

final class HopperGuiListener implements Listener {
    private final HopperRegistry registry;
    private final org.bukkit.plugin.Plugin plugin;

    HopperGuiListener(HopperRegistry registry, org.bukkit.plugin.Plugin plugin) {
        this.registry = registry;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HopperGui.HopperGuiHolder(HopperRegistry.HopperPos pos))) {
            return;
        }
        HopperData data = registry.get(pos);
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (rawSlot >= topSize) {
            if (event.isShiftClick()) {
                handleShiftFromPlayer(event, pos);
            }
            return;
        }

        if (HopperGui.isStatusSlot(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        if (HopperGui.isFilterSlot(rawSlot)) {
            event.setCancelled(true);
            handleFilterClick(event, data, rawSlot);
            return;
        }

        if (rawSlot == HopperGui.TOGGLE_SLOT) {
            event.setCancelled(true);
            toggleMode(event.getInventory(), pos);
            return;
        }

        if (rawSlot == HopperGui.UPGRADE_SLOT) {
            handleUpgradeSlot(event, pos, data);
            return;
        }

        if (rawSlot == HopperGui.TARGET_SLOT) {
            handleTargetSlot(event, pos, data);
            return;
        }

        if (HopperGui.isBufferSlot(rawSlot)) {
            scheduleFullSync(event.getInventory(), pos);
            return;
        }

        event.setCancelled(true);
    }

    private void handleShiftFromPlayer(InventoryClickEvent event, HopperRegistry.HopperPos pos) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }
        Inventory top = event.getView().getTopInventory();

        ItemStack upgradeSlot = top.getItem(HopperGui.UPGRADE_SLOT);
        if (WirelessItems.isUpgrade(current) && (upgradeSlot == null || upgradeSlot.getType().isAir())) {
            ItemStack moved = HopperGui.cloneSingle(current);
            WirelessItems.applyUpgradeLore(moved, WirelessItems.getUpgradeTier(moved));
            top.setItem(HopperGui.UPGRADE_SLOT, moved);
            decrementCurrent(event, current);
            scheduleFullSync(top, pos);
            event.setCancelled(true);
            return;
        }

        ItemStack targetSlot = top.getItem(HopperGui.TARGET_SLOT);
        if (WirelessItems.isTargetTool(current) && (targetSlot == null || targetSlot.getType().isAir())) {
            top.setItem(HopperGui.TARGET_SLOT, HopperGui.cloneSingle(current));
            decrementCurrent(event, current);
            scheduleFullSync(top, pos);
            event.setCancelled(true);
            return;
        }

        ItemStack remaining = moveIntoBuffer(top, current);
        if (remaining == null || remaining.getAmount() == 0) {
            event.setCurrentItem(null);
        } else {
            event.setCurrentItem(remaining);
        }
        scheduleFullSync(top, pos);
        event.setCancelled(true);
    }

    private void decrementCurrent(InventoryClickEvent event, ItemStack current) {
        int remaining = current.getAmount() - 1;
        if (remaining <= 0) {
            event.setCurrentItem(null);
        } else {
            current.setAmount(remaining);
            event.setCurrentItem(current);
        }
    }

    private void handleFilterClick(InventoryClickEvent event, HopperData data, int rawSlot) {
        if (data == null) {
            return;
        }
        int index = rawSlot - HopperGui.FILTER_START;
        ItemStack cursor = event.getCursor();
        if (cursor.getType().isAir()) {
            data.filters()[index] = null;
            data.save(data.location().getBlock());
        } else if (event.isLeftClick() || event.isRightClick()) {
            data.filters()[index] = HopperGui.cloneSingle(cursor);
            data.save(data.location().getBlock());
        }
        event.getInventory().setItem(rawSlot, HopperGui.filterDisplay(index, data.filters()[index]));
        HopperGui.writeStatus(event.getInventory(), data);
    }

    private void handleUpgradeSlot(InventoryClickEvent event, HopperRegistry.HopperPos pos, HopperData data) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (!cursor.getType().isAir() && !WirelessItems.isUpgrade(cursor)) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick() && current != null && !current.getType().isAir()) {
            ItemStack moving = HopperGui.cloneSingle(current);
            WirelessItems.applyUpgradeLore(moving, WirelessItems.getUpgradeTier(moving));
            Player player = (Player) event.getWhoClicked();
            if (player.getInventory().addItem(moving).isEmpty()) {
                event.setCurrentItem(null);
                if (data != null) {
                    data.setUpgradeItem(null);
                    data.save(data.location().getBlock());
                    HopperGui.writeStatus(event.getInventory(), data);
                }
            }
            event.setCancelled(true);
        }
        scheduleFullSync(event.getInventory(), pos);
    }

    private void handleTargetSlot(InventoryClickEvent event, HopperRegistry.HopperPos pos, HopperData data) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (!cursor.getType().isAir() && !WirelessItems.isTargetTool(cursor)) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick() && current != null && !current.getType().isAir()) {
            Player player = (Player) event.getWhoClicked();
            if (player.getInventory().addItem(HopperGui.cloneSingle(current)).isEmpty()) {
                event.setCurrentItem(null);
                if (data != null) {
                    data.setTargetItem(null);
                    data.setTargetInfo(null);
                    data.save(data.location().getBlock());
                    HopperGui.writeStatus(event.getInventory(), data);
                }
            }
            event.setCancelled(true);
        }
        scheduleFullSync(event.getInventory(), pos);
    }

    private ItemStack moveIntoBuffer(Inventory top, ItemStack stack) {
        ItemStack remaining = stack.clone();
        for (int slot = HopperGui.BUFFER_START; slot <= HopperGui.BUFFER_END; slot++) {
            ItemStack existing = top.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                top.setItem(slot, remaining);
                return null;
            }
            if (existing.isSimilar(remaining) && existing.getAmount() < existing.getMaxStackSize()) {
                int transferable = Math.min(remaining.getAmount(), existing.getMaxStackSize() - existing.getAmount());
                existing.setAmount(existing.getAmount() + transferable);
                remaining.setAmount(remaining.getAmount() - transferable);
                top.setItem(slot, existing);
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }
        return remaining;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof HopperGui.HopperGuiHolder(HopperRegistry.HopperPos pos))) {
            return;
        }
        boolean touchesBuffer = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= event.getView().getTopInventory().getSize()) {
                continue;
            }
            if (HopperGui.isFilterSlot(slot)) {
                event.setCancelled(true);
                return;
            }
            if (!HopperGui.isBufferSlot(slot)) {
                event.setCancelled(true);
                return;
            }
            touchesBuffer = true;
        }
        if (touchesBuffer) {
            scheduleFullSync(event.getInventory(), pos);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HopperGui.HopperGuiHolder(HopperRegistry.HopperPos pos))) {
            return;
        }
        HopperData data = registry.get(pos);
        if (data == null) {
            registry.closeInventory(pos);
            return;
        }
        Inventory top = event.getInventory();
        if (!top.getViewers().isEmpty()) {
            return;
        }
        Player player = (Player) event.getPlayer();

        ItemStack[] buffer = new ItemStack[HopperData.BUFFER_SLOTS];
        for (int i = HopperGui.BUFFER_START; i <= HopperGui.BUFFER_END; i++) {
            buffer[i] = top.getItem(i);
        }
        data.setBuffer(buffer);

        ItemStack upgrade = top.getItem(HopperGui.UPGRADE_SLOT);
        if (upgrade != null && !upgrade.getType().isAir() && !WirelessItems.isUpgrade(upgrade)) {
            upgrade = null;
        }
        if (upgrade != null) {
            WirelessItems.applyUpgradeLore(upgrade, WirelessItems.getUpgradeTier(upgrade));
        }
        if (upgrade != null && upgrade.getAmount() > 1) {
            ItemStack extra = upgrade.clone();
            extra.setAmount(upgrade.getAmount() - 1);
            upgrade.setAmount(1);
            player.getInventory().addItem(extra).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        data.setUpgradeItem(HopperGui.cloneSingle(upgrade));

        ItemStack targetItem = top.getItem(HopperGui.TARGET_SLOT);
        if (targetItem != null && !targetItem.getType().isAir() && !WirelessItems.isTargetTool(targetItem)) {
            targetItem = null;
        }
        if (targetItem != null && targetItem.getAmount() > 1) {
            ItemStack extra = targetItem.clone();
            extra.setAmount(targetItem.getAmount() - 1);
            targetItem.setAmount(1);
            player.getInventory().addItem(extra).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        data.setTargetItem(HopperGui.cloneSingle(targetItem));
        if (targetItem != null) {
            data.setTargetInfo(TargetTool.readTarget(targetItem));
        } else {
            data.setTargetInfo(null);
        }
        Block block = data.location().getBlock();
        data.save(block);
        HopperGui.writeStatus(top, data);
        registry.closeInventory(pos);
    }

    private void toggleMode(Inventory inventory, HopperRegistry.HopperPos pos) {
        HopperData data = registry.get(pos);
        if (data == null) {
            return;
        }
        data.setWhitelist(!data.isWhitelist());
        inventory.setItem(HopperGui.TOGGLE_SLOT, HopperGui.toggleItem(data));
        HopperGui.writeStatus(inventory, data);
        data.save(data.location().getBlock());
    }

    private void scheduleFullSync(Inventory inventory, HopperRegistry.HopperPos pos) {
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            HopperData data = registry.get(pos);
            if (data == null) {
                return;
            }
            data.setBuffer(HopperGui.readBuffer(inventory));

            ItemStack upgrade = inventory.getItem(HopperGui.UPGRADE_SLOT);
            if (upgrade != null && !upgrade.getType().isAir() && !WirelessItems.isUpgrade(upgrade)) {
                upgrade = null;
            }
            if (upgrade != null) {
                WirelessItems.applyUpgradeLore(upgrade, WirelessItems.getUpgradeTier(upgrade));
            }
            data.setUpgradeItem(HopperGui.cloneSingle(upgrade));

            ItemStack target = inventory.getItem(HopperGui.TARGET_SLOT);
            if (target != null && !target.getType().isAir() && !WirelessItems.isTargetTool(target)) {
                target = null;
            }
            data.setTargetItem(HopperGui.cloneSingle(target));
            if (target != null) {
                data.setTargetInfo(TargetTool.readTarget(target));
            } else {
                data.setTargetInfo(null);
            }
            data.save(data.location().getBlock());
            HopperGui.writeStatus(inventory, data);
        });
    }
}

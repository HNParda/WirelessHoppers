package com.hnp_arda.wireless_hoppers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class WirelessHopperListener implements Listener {
    private final HopperRegistry registry;

    WirelessHopperListener(HopperRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();
        if (block.getType() != Material.WARPED_SLAB) {
            return;
        }
        if (WirelessItems.isWirelessHopper(item)) {
            if (!event.getPlayer().hasPermission("wirelesshopper.place")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("You don't have permission to place Wireless Hoppers.", NamedTextColor.RED));
                return;
            }
            WirelessHopperBlock.markWirelessHopper(block);
            HopperData data = new HopperData(block.getLocation());
            data.save(block);
            registry.register(block);
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("Warped slabs are reserved for Wireless Hoppers.", NamedTextColor.RED));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            return;
        }
        if (!event.getPlayer().hasPermission("wirelesshopper.use")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You don't have permission to break Wireless Hoppers.", NamedTextColor.RED));
            return;
        }
        event.setDropItems(false);
        HopperData data = HopperData.load(block);
        HopperRegistry.HopperPos pos = HopperRegistry.HopperPos.fromBlock(block);
        org.bukkit.inventory.Inventory open = registry.getOpenInventory(pos);
        if (data != null && open != null) {
            data.setBuffer(HopperGui.readBuffer(open));
            ItemStack upgrade = open.getItem(HopperGui.UPGRADE_SLOT);
            if (upgrade != null && !upgrade.getType().isAir() && !WirelessItems.isUpgrade(upgrade)) {
                upgrade = null;
            }
            if (upgrade != null) {
                WirelessItems.applyUpgradeLore(upgrade, WirelessItems.getUpgradeTier(upgrade));
            }
            data.setUpgradeItem(HopperGui.cloneSingle(upgrade));
            ItemStack targetItem = open.getItem(HopperGui.TARGET_SLOT);
            if (targetItem != null && !targetItem.getType().isAir() && !WirelessItems.isTargetTool(targetItem)) {
                targetItem = null;
            }
            data.setTargetItem(HopperGui.cloneSingle(targetItem));
            if (targetItem != null) {
                data.setTargetInfo(TargetTool.readTarget(targetItem));
            } else {
                data.setTargetInfo(null);
            }
            data.save(block);
        }
        if (data != null) {
            data.save(block);
            for (ItemStack item : data.buffer()) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            for (ItemStack item : data.filters()) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            if (data.upgradeItem() != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), data.upgradeItem());
            }
            if (data.targetItem() != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), data.targetItem());
            }
        }
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(block.getLocation(), WirelessItems.createHopperItem());
        }
        WirelessHopperBlock.clear(block);
        registry.unregister(block);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (event.getPlayer().isSneaking() && item != null && item.getType().isBlock() && block.getType() == Material.WARPED_SLAB) {
            return;
        }
        if (WirelessItems.isTargetTool(item) && block.getState() instanceof org.bukkit.inventory.InventoryHolder) {
            TargetTool.writeTarget(item, block.getState());
            event.getPlayer().sendMessage(Component.text("Linked target: " + block.getType().name(), NamedTextColor.GREEN));
            event.setCancelled(true);
            return;
        }
        if (block.getType() != Material.WARPED_SLAB) {
            return;
        }
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            return;
        }
        if (!event.getPlayer().hasPermission("wirelesshopper.use")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You don't have permission to use Wireless Hoppers.", NamedTextColor.RED));
            return;
        }
        HopperData data = registry.get(HopperRegistry.HopperPos.fromBlock(block));
        if (data == null) {
            data = HopperData.load(block);
            if (data == null) {
                return;
            }
        }
        HopperRegistry.HopperPos pos = HopperRegistry.HopperPos.fromBlock(block);
        event.getPlayer().openInventory(registry.openInventory(pos, data));
        event.setCancelled(true);
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (WirelessHopperBlock.isWirelessHopper(block)) {
            event.setNewCurrent(0);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        registry.registerChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        registry.unregisterChunk(event.getChunk());
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getResult() == null || event.getInventory().getResult().getType() != Material.WARPED_SLAB) {
            return;
        }
        event.getInventory().setResult(null);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getType() != Material.WARPED_SLAB) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onStonecutterClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.STONECUTTER) {
            return;
        }
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() == Material.WARPED_SLAB) {
            event.setCancelled(true);
        }
    }
}

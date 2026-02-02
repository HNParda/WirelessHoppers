package com.hnp_arda.wireless_hoppers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
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
import org.bukkit.permissions.PermissionAttachmentInfo;

final class WirelessHopperListener implements Listener {
    private static final String LIMIT_PREFIX = "wirelesshopper.limit.";
    private final HopperRegistry registry;

    WirelessHopperListener(HopperRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();
        if (block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        if (WirelessItems.isWirelessHopper(item)) {
            if (!event.getPlayer().hasPermission("wirelesshopper.place")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text(
                    Lang.tr(event.getPlayer(), "listener.place_no_permission"),
                    NamedTextColor.RED
                ));
                return;
            }
            int limit = resolveLimit(event.getPlayer());
            if (limit != -1) {
                int count = registry.countOwnedBy(event.getPlayer().getUniqueId());
                if (count >= limit) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text(
                        Lang.tr(event.getPlayer(), "listener.place_limit_reached",
                            java.util.Map.of("limit", String.valueOf(limit))),
                        NamedTextColor.RED
                    ));
                    return;
                }
            }
            WirelessHopperBlock.markWirelessHopper(block);
            HopperData data = new HopperData(block.getLocation());
            data.setOwnerId(event.getPlayer().getUniqueId());
            data.save(block);
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(
            Lang.tr(event.getPlayer(), "listener.place_reserved"),
            NamedTextColor.RED
        ));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            return;
        }
        if (!event.getPlayer().hasPermission("wirelesshopper.use")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                Lang.tr(event.getPlayer(), "listener.break_no_permission"),
                NamedTextColor.RED
            ));
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
                WirelessItems.applyUpgradeLore(upgrade, WirelessItems.getUpgradeTier(upgrade),
                    Lang.localeFromPlayer(event.getPlayer()));
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

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
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
        if (event.getPlayer().isSneaking() && item != null && item.getType().isBlock() && block.getType() == Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        if (WirelessItems.isTargetTool(item) && block.getState() instanceof org.bukkit.inventory.InventoryHolder) {
            TargetTool.writeTarget(item, block.getState(), Lang.localeFromPlayer(event.getPlayer()));
            event.getPlayer().sendMessage(Component.text(
                Lang.tr(event.getPlayer(), "listener.target_linked",
                    java.util.Map.of("type", block.getType().name())),
                NamedTextColor.GREEN
            ));
            event.setCancelled(true);
            return;
        }
        if (block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            return;
        }
        if (!event.getPlayer().hasPermission("wirelesshopper.use")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                Lang.tr(event.getPlayer(), "listener.use_no_permission"),
                NamedTextColor.RED
            ));
            return;
        }
        if (!SimpleClaimSystemHook.canInteract(event.getPlayer(), block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                Lang.tr(event.getPlayer(), "listener.claim_no_access"),
                NamedTextColor.RED
            ));
            return;
        }
        HopperData data = registry.get(HopperRegistry.HopperPos.fromBlock(block));
        if (data == null) {
            data = HopperData.load(block);
            if (data == null) {
                data = new HopperData(block.getLocation());
                data.save(block);
            }
        }
        HopperRegistry.HopperPos pos = HopperRegistry.HopperPos.fromBlock(block);
        if (registry.get(pos) == null) {
            registry.register(block);
        }
        event.getPlayer().openInventory(registry.openInventory(event.getPlayer(), pos, data));
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.getType().name().endsWith("_PICKAXE")) {
            event.setCancelled(true);
        }
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
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        if (!WirelessItems.isWirelessHopper(result)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        if (!WirelessItems.isWirelessHopper(current)) {
            event.setCancelled(true);
        }
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
        if (current != null && current.getType() == Material.BAMBOO_MOSAIC_SLAB && !WirelessItems.isWirelessHopper(current)) {
            event.setCancelled(true);
        }
    }

    private int resolveLimit(org.bukkit.entity.Player player) {
        int configLimit = PlacementConfig.maxHoppersPerPlayer();
        int permissionLimit = permissionLimit(player);
        if (permissionLimit != Integer.MIN_VALUE) {
            return permissionLimit;
        }
        return configLimit;
    }

    private int permissionLimit(org.bukkit.entity.Player player) {
        if (player.hasPermission(LIMIT_PREFIX + "unlimited")) {
            return -1;
        }
        int best = Integer.MIN_VALUE;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String permission = info.getPermission();
            if (!permission.startsWith(LIMIT_PREFIX)) {
                continue;
            }
            String suffix = permission.substring(LIMIT_PREFIX.length());
            if (suffix.equalsIgnoreCase("unlimited")) {
                return -1;
            }
            try {
                int value = Integer.parseInt(suffix);
                if (value == -1) {
                    return -1;
                }
                if (value >= 0 && value > best) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
                continue;
            }
        }
        return best;
    }
}

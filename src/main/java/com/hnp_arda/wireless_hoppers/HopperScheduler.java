package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HopperScheduler implements Runnable {
    private static final int MAX_HOPPERS_PER_TICK = 200;
    private static final int ITEM_INDEX_REFRESH_TICKS = 10;
    private static final BlockFace[] HOPPER_PULL_FACES = new BlockFace[]{
        BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private final HopperRegistry registry;
    private final ItemIndex itemIndex;
    private final Map<HopperRegistry.HopperPos, Long> nextTransferTick = new HashMap<>();
    private long tickCounter = 0L;
    private int hopperCursor = 0;
    private List<HopperRegistry.HopperPos> hopperList = List.of();

    HopperScheduler(HopperRegistry registry, ItemIndex itemIndex) {
        this.registry = registry;
        this.itemIndex = itemIndex;
    }

    @Override
    public void run() {
        tickCounter++;
        if (tickCounter == 1 || tickCounter % ITEM_INDEX_REFRESH_TICKS == 0) {
            refreshItemIndex();
        }
        if (registry.allHoppers().isEmpty()) {
            return;
        }
        if (hopperCursor >= hopperList.size()) {
            hopperList = registry.allHoppers().stream()
                .map(data -> HopperRegistry.HopperPos.fromBlock(data.location().getBlock()))
                .toList();
            hopperCursor = 0;
        }
        int processed = 0;
        while (processed < MAX_HOPPERS_PER_TICK && hopperCursor < hopperList.size()) {
            HopperRegistry.HopperPos pos = hopperList.get(hopperCursor++);
            long nextTick = nextTransferTick.getOrDefault(pos, 0L);
            if (tickCounter < nextTick) {
                processed++;
                continue;
            }
            HopperData data = registry.get(pos);
            if (data == null) {
                processed++;
                continue;
            }
            processHopper(data);
            syncToOpenGui(pos, data);
            UpgradeTier tier = data.upgradeTier();
            int cooldown = tier == null ? TransferConfig.defaultCooldownTicks() : tier.cooldownTicks();
            nextTransferTick.put(pos, tickCounter + cooldown);
            processed++;
        }
    }

    private void refreshItemIndex() {
        Set<HopperRegistry.ChunkKey> activeChunks = registry.activeChunks();
        if (activeChunks.isEmpty()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            itemIndex.update(world, activeChunks);
        }
    }

    private void processHopper(HopperData data) {
        Block block = data.location().getBlock();
        if (!WirelessHopperBlock.isWirelessHopper(block)) {
            WirelessHopperBlock.clear(block);
            registry.unregister(block);
            return;
        }
        if (isRedstoneBlocked(data, block)) {
            return;
        }
        boolean changed = pullFromAboveContainer(data);
        changed |= pullFromMinecartInventory(data);
        changed |= pullFromAdjacentHoppers(data);
        changed |= pickupGroundItems(data);
        changed |= transferBuffer(data);
        if (changed) {
            data.save(block);
        }
    }

    private void syncToOpenGui(HopperRegistry.HopperPos pos, HopperData data) {
        for (Inventory inventory : registry.getOpenInventories(pos)) {
            HopperGui.writeBuffer(inventory, data.buffer());
            String locale = HopperGui.localeFromInventory(inventory);
            HopperGui.writeFilters(inventory, data.filters(), locale);
            HopperGui.writeStatus(inventory, data);
        }
    }

    private boolean pickupGroundItems(HopperData data) {
        if (isBufferFull(data.buffer())) {
            return false;
        }
        Block block = data.location().getBlock();
        BoundingBox box = new BoundingBox(
            block.getX(), block.getY() + 1.0, block.getZ(),
            block.getX() + 1.0, block.getY() + 2.0, block.getZ() + 1.0
        );
        HopperRegistry.ChunkKey key = HopperRegistry.ChunkKey.fromBlock(block);
        List<Item> items = itemIndex.getItemsInChunk(key);
        boolean changed = false;
        for (Item itemEntity : items) {
            if (itemEntity.isDead() || !itemEntity.isValid()) {
                continue;
            }
            if (!box.overlaps(itemEntity.getBoundingBox())) {
                continue;
            }
            ItemStack stack = itemEntity.getItemStack();
            if (blockedByFilter(data, stack)) {
                continue;
            }
            ItemStack remaining = insertIntoBuffer(data.buffer(), stack);
            changed = true;
            if (remaining == null || remaining.getAmount() == 0) {
                itemEntity.remove();
            } else {
                itemEntity.setItemStack(remaining);
            }
            if (isBufferFull(data.buffer())) {
                return true;
            }
        }
        return changed;
    }

    private boolean pullFromAdjacentHoppers(HopperData data) {
        if (isBufferFull(data.buffer())) {
            return false;
        }
        Block targetBlock = data.location().getBlock();
        int limit = data.upgradeTier() == null ? TransferConfig.defaultItemsPerTransfer() : data.upgradeTier().itemsPerTransfer();
        int moved = 0;
        boolean changed = false;
        for (BlockFace face : HOPPER_PULL_FACES) {
            if (moved >= limit || isBufferFull(data.buffer())) {
                break;
            }
            Block hopperBlock = targetBlock.getRelative(face);
            if (hopperBlock.getType() != org.bukkit.Material.HOPPER) {
                continue;
            }
            BlockState state = hopperBlock.getState();
            if (!(state instanceof InventoryHolder holder)) {
                continue;
            }
            org.bukkit.block.data.BlockData blockData = hopperBlock.getBlockData();
            if (!(blockData instanceof Hopper hopperData)) {
                continue;
            }
            BlockFace output = hopperData.getFacing();
            Block expected = hopperBlock.getRelative(output);
            if (!expected.getLocation().equals(targetBlock.getLocation())) {
                continue;
            }
            Inventory inventory = holder.getInventory();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                if (blockedByFilter(data, item)) {
                    continue;
                }
                int remaining = limit - moved;
                ItemStack toMove = item.clone();
                if (toMove.getAmount() > remaining) {
                    toMove.setAmount(remaining);
                }
                ItemStack leftover = insertIntoBuffer(data.buffer(), toMove);
                int inserted = toMove.getAmount() - (leftover == null ? 0 : leftover.getAmount());
                if (inserted <= 0) {
                    break;
                }
                moved += inserted;
                int newAmount = item.getAmount() - inserted;
                if (newAmount <= 0) {
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(newAmount);
                    inventory.setItem(i, item);
                }
                changed = true;
                if (moved >= limit || isBufferFull(data.buffer())) {
                    break;
                }
            }
        }
        return changed;
    }

    private boolean pullFromAboveContainer(HopperData data) {
        if (isBufferFull(data.buffer())) {
            return false;
        }
        Block block = data.location().getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (!(above.getState() instanceof InventoryHolder holder)) {
            return false;
        }
        Inventory inventory = holder.getInventory();
        return pullFromInventory(data, inventory);
    }

    private boolean pullFromMinecartInventory(HopperData data) {
        if (isBufferFull(data.buffer())) {
            return false;
        }
        Block block = data.location().getBlock();
        BoundingBox box = new BoundingBox(
            block.getX() - 0.25, block.getY(), block.getZ() - 0.25,
            block.getX() + 1.25, block.getY() + 2.5, block.getZ() + 1.25
        );
        for (org.bukkit.entity.Entity entity : block.getWorld().getNearbyEntities(box)) {
            if (!(entity instanceof org.bukkit.entity.Minecart)) {
                continue;
            }
            if (!(entity instanceof InventoryHolder holder)) {
                continue;
            }
            if (entity.getLocation().getBlockX() != block.getX()
                || entity.getLocation().getBlockZ() != block.getZ()) {
                continue;
            }
            if (pullFromInventory(data, holder.getInventory())) {
                return true;
            }
        }
        return false;
    }

    private boolean pullFromInventory(HopperData data, Inventory inventory) {
        int limit = data.upgradeTier() == null ? TransferConfig.defaultItemsPerTransfer() : data.upgradeTier().itemsPerTransfer();
        int moved = 0;
        boolean changed = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (blockedByFilter(data, item)) {
                continue;
            }
            int remaining = limit - moved;
            if (remaining <= 0) {
                break;
            }
            ItemStack toMove = item.clone();
            if (toMove.getAmount() > remaining) {
                toMove.setAmount(remaining);
            }
            ItemStack leftover = insertIntoBuffer(data.buffer(), toMove);
            int inserted = toMove.getAmount() - (leftover == null ? 0 : leftover.getAmount());
            if (inserted <= 0) {
                break;
            }
            moved += inserted;
            int newAmount = item.getAmount() - inserted;
            if (newAmount <= 0) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(newAmount);
                inventory.setItem(i, item);
            }
            changed = true;
            if (moved >= limit || isBufferFull(data.buffer())) {
                break;
            }
        }
        return changed;
    }

    private boolean transferBuffer(HopperData data) {
        HopperData.TargetInfo targetInfo = data.targetInfo();
        if (targetInfo == null) {
            return false;
        }
        World world = Bukkit.getWorld(targetInfo.worldId());
        if (world == null || !world.isChunkLoaded(targetInfo.x() >> 4, targetInfo.z() >> 4)) {
            return false;
        }
        Location targetLocation = targetInfo.toLocation(world);
        BlockState state = targetLocation.getBlock().getState();
        if (!(state instanceof InventoryHolder holder)) {
            return false;
        }
        Inventory targetInventory = holder.getInventory();
        if (!targetInventory.getType().name().equalsIgnoreCase(targetInfo.inventoryType())) {
            return false;
        }
        UpgradeTier tier = data.upgradeTier();
        int limit = tier == null ? TransferConfig.defaultItemsPerTransfer() : tier.itemsPerTransfer();
        int moved = 0;
        ItemStack[] buffer = data.buffer();
        boolean changed = false;
        for (int i = 0; i < buffer.length; i++) {
            ItemStack item = buffer[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (blockedByFilter(data, item)) {
                continue;
            }
            int remainingCapacity = limit - moved;
            if (remainingCapacity <= 0) {
                break;
            }
            ItemStack toMove = item.clone();
            if (toMove.getAmount() > remainingCapacity) {
                toMove.setAmount(remainingCapacity);
            }
            int attempted = toMove.getAmount();
            Map<Integer, ItemStack> leftovers = targetInventory.addItem(toMove);
            int inserted = attempted;
            if (!leftovers.isEmpty()) {
                inserted -= leftovers.values().iterator().next().getAmount();
            }
            if (inserted <= 0) {
                break;
            }
            moved += inserted;
            int newAmount = item.getAmount() - inserted;
            if (newAmount <= 0) {
                buffer[i] = null;
            } else {
                item.setAmount(newAmount);
                buffer[i] = item;
            }
            changed = true;
            if (moved >= limit) {
                break;
            }
        }
        return changed;
    }

    private boolean isBufferFull(ItemStack[] buffer) {
        for (ItemStack item : buffer) {
            if (item == null || item.getType().isAir() || item.getAmount() < item.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private ItemStack insertIntoBuffer(ItemStack[] buffer, ItemStack stack) {
        ItemStack remaining = stack.clone();
        for (int i = 0; i < buffer.length; i++) {
            ItemStack existing = buffer[i];
            if (existing == null || existing.getType().isAir()) {
                buffer[i] = remaining.clone();
                return null;
            }
            if (existing.isSimilar(remaining) && existing.getAmount() < existing.getMaxStackSize()) {
                int transferable = Math.min(remaining.getAmount(), existing.getMaxStackSize() - existing.getAmount());
                existing.setAmount(existing.getAmount() + transferable);
                remaining.setAmount(remaining.getAmount() - transferable);
                buffer[i] = existing;
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }
        return remaining;
    }

    private boolean blockedByFilter(HopperData data, ItemStack stack) {
        if (!data.isWhitelist() && HopperData.isEmpty(data.filters())) {
            return false;
        }
        boolean strict = data.hasStrictMatch();
        boolean match = false;
        for (ItemStack filter : data.filters()) {
            if (filter == null || filter.getType().isAir()) {
                continue;
            }
            if (strict ? filter.isSimilar(stack) : filter.getType() == stack.getType()) {
                match = true;
                break;
            }
        }
        return data.isWhitelist() != match;
    }

    private boolean isRedstoneBlocked(HopperData data, Block block) {
        RedstoneMode mode = data.redstoneMode();
        if (mode == null || mode == RedstoneMode.IGNORED) {
            return false;
        }
        boolean powered = block.getBlockPower() > 0;
        return (mode == RedstoneMode.LOW) == powered;
    }
}

package com.hnp_arda.wireless_hoppers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

final class HopperRegistry {
    private final HopperStorage storage;
    private final Map<HopperPos, HopperData> hoppers = new HashMap<>();
    private final Map<ChunkKey, Set<HopperPos>> hoppersByChunk = new HashMap<>();
    private final Map<HopperPos, Map<UUID, Inventory>> openInventories = new HashMap<>();

    HopperRegistry(HopperStorage storage) {
        this.storage = storage;
    }

    Collection<HopperData> allHoppers() {
        return hoppers.values();
    }

    Set<ChunkKey> activeChunks() {
        return new HashSet<>(hoppersByChunk.keySet());
    }

    HopperData get(HopperPos pos) {
        return hoppers.get(pos);
    }

    int countOwnedBy(UUID ownerId) {
        return storage.countOwnedBy(ownerId);
    }

    Inventory getOpenInventory(HopperPos pos) {
        Map<UUID, Inventory> byPlayer = openInventories.get(pos);
        if (byPlayer == null || byPlayer.isEmpty()) {
            return null;
        }
        return byPlayer.values().iterator().next();
    }

    Collection<Inventory> getOpenInventories(HopperPos pos) {
        Map<UUID, Inventory> byPlayer = openInventories.get(pos);
        if (byPlayer == null || byPlayer.isEmpty()) {
            return List.of();
        }
        return List.copyOf(byPlayer.values());
    }

    Inventory openInventory(Player player, HopperPos pos, HopperData data) {
        if (!hoppers.containsKey(pos)) {
            hoppers.put(pos, data);
            Block block = data.location().getBlock();
            hoppersByChunk.computeIfAbsent(ChunkKey.fromBlock(block), ignored -> new HashSet<>()).add(pos);
        }
        String locale = Lang.localeFromPlayer(player);
        Map<UUID, Inventory> byPlayer = openInventories.computeIfAbsent(pos, ignored -> new HashMap<>());
        Inventory existing = byPlayer.get(player.getUniqueId());
        if (existing != null) {
            if (!HopperGui.localeFromInventory(existing).equals(locale)) {
                Inventory created = HopperGui.create(pos, data, locale);
                byPlayer.put(player.getUniqueId(), created);
                return created;
            }
            HopperGui.writeBuffer(existing, data.buffer());
            HopperGui.writeFilters(existing, data.filters(), locale);
            ItemStack upgrade = HopperGui.cloneSingle(data.upgradeItem());
            if (upgrade != null) {
                UpgradeTier tier = WirelessItems.getUpgradeTier(upgrade);
                WirelessItems.applyUpgradeLore(upgrade, tier, locale);
            }
            existing.setItem(HopperGui.UPGRADE_SLOT, upgrade);
            existing.setItem(HopperGui.TARGET_SLOT, HopperGui.cloneSingle(data.targetItem()));
            existing.setItem(HopperGui.TOGGLE_SLOT, HopperGui.toggleItem(data, locale));
            existing.setItem(HopperGui.REDSTONE_SLOT, HopperGui.redstoneToggleItem(data, locale));
            HopperGui.writeStatus(existing, data);
            return existing;
        }
        Inventory created = HopperGui.create(pos, data, locale);
        byPlayer.put(player.getUniqueId(), created);
        return created;
    }

    void closeInventory(HopperPos pos, UUID playerId) {
        Map<UUID, Inventory> byPlayer = openInventories.get(pos);
        if (byPlayer == null) {
            return;
        }
        byPlayer.remove(playerId);
        if (byPlayer.isEmpty()) {
            openInventories.remove(pos);
        }
    }

    void register(Block block) {
        HopperData data = HopperData.load(block);
        if (data == null) {
            return;
        }
        HopperPos pos = HopperPos.fromBlock(block);
        hoppers.put(pos, data);
        hoppersByChunk.computeIfAbsent(ChunkKey.fromBlock(block), ignored -> new HashSet<>()).add(pos);
    }

    void unregister(Block block) {
        HopperPos pos = HopperPos.fromBlock(block);
        hoppers.remove(pos);
        Set<HopperPos> inChunk = hoppersByChunk.get(ChunkKey.fromBlock(block));
        if (inChunk != null) {
            inChunk.remove(pos);
            if (inChunk.isEmpty()) {
                hoppersByChunk.remove(ChunkKey.fromBlock(block));
            }
        }
        openInventories.remove(pos);
    }

    void registerChunk(Chunk chunk) {
        for (HopperPos pos : storage.positionsInChunk(chunk)) {
            if (hoppers.containsKey(pos)) {
                continue;
            }
            Block block = chunk.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
            if (!WirelessHopperBlock.isWirelessHopper(block)) {
                storage.removeInvalid(block);
                continue;
            }
            register(block);
        }
    }

    void unregisterChunk(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        Set<HopperPos> positions = hoppersByChunk.remove(key);
        if (positions == null) {
            return;
        }
        for (HopperPos pos : positions) {
            HopperData data = hoppers.remove(pos);
            if (data != null) {
                Block block = data.location().getBlock();
                data.save(block);
            }
            openInventories.remove(pos);
        }
    }

    record HopperPos(UUID worldId, int x, int y, int z) {
        static HopperPos fromBlock(Block block) {
            Location loc = block.getLocation();
            return new HopperPos(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

    }

    record ChunkKey(UUID worldId, int x, int z) {
        static ChunkKey fromBlock(Block block) {
            Chunk chunk = block.getChunk();
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }
}

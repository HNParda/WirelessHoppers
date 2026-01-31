package com.hnp_arda.wireless_hoppers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;

final class WirelessHopperBlock {
    private static HopperStorage storage;

    private WirelessHopperBlock() {
    }

    static void init(HopperStorage hopperStorage) {
        storage = hopperStorage;
    }

    static boolean isWirelessHopper(Block block) {
        if (storage == null || block == null || block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return false;
        }
        return storage.contains(block);
    }

    static void markWirelessHopper(Block block) {
        if (block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            block.setType(Material.BAMBOO_MOSAIC_SLAB, false);
        }
        BlockData data = block.getBlockData();
        if (data instanceof Slab slab) {
            slab.setType(Slab.Type.TOP);
            slab.setWaterlogged(false);
            block.setBlockData(slab, false);
        }
        if (storage != null) {
            storage.put(block, new byte[0]);
        }
    }

    static void clear(Block block) {
        if (storage == null || block == null || block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        storage.remove(block);
        storage.save();
    }

    static void writeData(Block block, byte[] data) {
        if (storage == null || block == null || block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return;
        }
        storage.put(block, data);
        storage.save();
    }

    static byte[] readData(Block block) {
        if (storage == null || block == null || block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return null;
        }
        return storage.get(block);
    }
}

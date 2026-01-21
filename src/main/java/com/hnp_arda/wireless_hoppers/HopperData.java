package com.hnp_arda.wireless_hoppers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

final class HopperData {
    static final int FILTER_SLOTS = 27;
    static final int BUFFER_SLOTS = 9;

    private final Location location;
    private ItemStack[] filters;
    private ItemStack[] buffer;
    private ItemStack upgradeItem;
    private ItemStack targetItem;
    private boolean whitelist;
    private TargetInfo targetInfo;

    HopperData(Location location) {
        this.location = location;
        this.filters = new ItemStack[FILTER_SLOTS];
        this.buffer = new ItemStack[BUFFER_SLOTS];
        this.whitelist = false;
    }

    static HopperData load(Block block) {
        if (block.getType() != Material.WARPED_SLAB) {
            return null;
        }
        byte[] payload = WirelessHopperBlock.readData(block);
        if (payload == null || payload.length == 0) {
            return null;
        }
        return deserialize(block.getLocation(), payload);
    }

    static boolean isEmpty(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    static ItemStack[] normalizeSize(ItemStack[] items, int size) {
        ItemStack[] normalized = new ItemStack[size];
        if (items != null) {
            System.arraycopy(items, 0, normalized, 0, Math.min(items.length, size));
        }
        return normalized;
    }

    static ItemStack firstItem(ItemStack[] items) {
        if (items == null || items.length == 0) {
            return null;
        }
        ItemStack first = items[0];
        return first != null && first.getType() != Material.AIR ? first : null;
    }

    private static HopperData deserialize(Location location, byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            HopperData hopperData = new HopperData(location);
            hopperData.filters = normalizeSize((ItemStack[]) dataInput.readObject(), FILTER_SLOTS);
            hopperData.buffer = normalizeSize((ItemStack[]) dataInput.readObject(), BUFFER_SLOTS);
            hopperData.upgradeItem = (ItemStack) dataInput.readObject();
            hopperData.targetItem = (ItemStack) dataInput.readObject();
            hopperData.whitelist = dataInput.readBoolean();
            String target = dataInput.readUTF();
            hopperData.targetInfo = target.isEmpty() ? null : TargetInfo.fromString(target);
            return hopperData;
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to deserialize hopper data", ex);
        }
    }

    Location location() {
        return location;
    }

    ItemStack[] filters() {
        return filters;
    }

    ItemStack[] buffer() {
        return buffer;
    }

    ItemStack upgradeItem() {
        return upgradeItem;
    }

    ItemStack targetItem() {
        return targetItem;
    }

    boolean isWhitelist() {
        return whitelist;
    }

    void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    TargetInfo targetInfo() {
        return targetInfo;
    }

    void setFilters(ItemStack[] filters) {
        this.filters = filters;
    }

    void setBuffer(ItemStack[] buffer) {
        this.buffer = buffer;
    }

    void setUpgradeItem(ItemStack upgradeItem) {
        this.upgradeItem = upgradeItem;
    }

    void setTargetItem(ItemStack targetItem) {
        this.targetItem = targetItem;
    }

    void setTargetInfo(TargetInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    UpgradeTier upgradeTier() {
        return WirelessItems.getUpgradeTier(upgradeItem);
    }

    boolean hasStrictMatch() {
        return upgradeTier() == UpgradeTier.NETHERITE;
    }

    void save(Block block) {
        if (block.getType() != Material.WARPED_SLAB) {
            WirelessHopperBlock.clear(block);
            return;
        }
        WirelessHopperBlock.writeData(block, serialize());
    }

    private byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(filters);
            dataOutput.writeObject(buffer);
            dataOutput.writeObject(upgradeItem);
            dataOutput.writeObject(targetItem);
            dataOutput.writeBoolean(whitelist);
            dataOutput.writeUTF(targetInfo == null ? "" : targetInfo.toString());
            dataOutput.flush();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize hopper data", ex);
        }
    }

    record TargetInfo(UUID worldId, int x, int y, int z, String inventoryType) {
        static TargetInfo fromString(String value) {
            String[] parts = value.split(";");
            if (parts.length != 5) {
                return null;
            }
            try {
                UUID world = UUID.fromString(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                return new TargetInfo(world, x, y, z, parts[4]);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        @Override
        public @NonNull String toString() {
            return worldId + ";" + x + ";" + y + ";" + z + ";" + inventoryType;
        }

        Location toLocation(World world) {
            return new Location(world, x, y, z);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z, inventoryType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TargetInfo(UUID id, int x1, int y1, int z1, String type))) {
                return false;
            }
            return Objects.equals(worldId, id)
                    && x == x1
                    && y == y1
                    && z == z1
                    && Objects.equals(inventoryType, type);
        }
    }
}

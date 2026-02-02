package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private UUID ownerId;

    HopperData(Location location) {
        this.location = location;
        this.filters = new ItemStack[FILTER_SLOTS];
        this.buffer = new ItemStack[BUFFER_SLOTS];
        this.whitelist = false;
    }

    @SuppressWarnings("UnstableApiUsage")
    static HopperData load(Block block) {
        if (block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            return null;
        }
        byte[] payload = WirelessHopperBlock.readData(block);
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            return deserialize(block.getLocation(), payload);
        } catch (RuntimeException ex) {
            Bukkit.getLogger().warning("[WirelessHopper] Failed to load data at "
                + block.getWorld().getName() + " " + block.getX() + " " + block.getY() + " " + block.getZ()
                + " payloadSize=" + payload.length + " error=" + ex.getMessage());
            return null;
        }
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

    private static final int MAGIC = 0x574831;

    private static HopperData deserialize(Location location, byte[] data) {
        if (data.length < 8) {
            throw new IllegalStateException("Invalid hopper data format");
        }
        return newDeserialize(location, data);
    }

    private static HopperData newDeserialize(Location location, byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(inputStream)) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalStateException("Invalid hopper data format");
            }
            int versionOrFilterCount = in.readInt();
            int version = 0;
            int filterCount;
            if (versionOrFilterCount == 1 || versionOrFilterCount == 2) {
                version = versionOrFilterCount;
                filterCount = in.readInt();
            } else {
                filterCount = versionOrFilterCount;
            }
            HopperData hopperData = new HopperData(location);
            if (filterCount < 0 || filterCount > FILTER_SLOTS) {
                throw new IllegalStateException("Invalid filter count: " + filterCount);
            }
            ItemStack[] filters = new ItemStack[filterCount];
            for (int i = 0; i < filterCount; i++) {
                filters[i] = readItemStack(in);
            }
            int bufferCount = in.readInt();
            if (bufferCount < 0 || bufferCount > BUFFER_SLOTS) {
                throw new IllegalStateException("Invalid buffer count: " + bufferCount);
            }
            ItemStack[] buffer = new ItemStack[bufferCount];
            for (int i = 0; i < bufferCount; i++) {
                buffer[i] = readItemStack(in);
            }
            hopperData.filters = normalizeSize(filters, FILTER_SLOTS);
            hopperData.buffer = normalizeSize(buffer, BUFFER_SLOTS);
            hopperData.upgradeItem = readItemStack(in);
            hopperData.targetItem = readItemStack(in);
            hopperData.whitelist = in.readBoolean();
            String target = in.readUTF();
            hopperData.targetInfo = target.isEmpty() ? null : TargetInfo.fromString(target);
            if (version >= 2) {
                String owner = in.readUTF();
                if (!owner.isEmpty()) {
                    try {
                        hopperData.ownerId = UUID.fromString(owner);
                    } catch (IllegalArgumentException ignored) {
                        hopperData.ownerId = null;
                    }
                }
            }
            return hopperData;
        } catch (IOException ex) {
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

    UUID ownerId() {
        return ownerId;
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

    void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    UpgradeTier upgradeTier() {
        return WirelessItems.getUpgradeTier(upgradeItem);
    }

    boolean hasStrictMatch() {
        return upgradeTier() == UpgradeTier.NETHERITE;
    }

    void save(Block block) {
        if (block.getType() != Material.BAMBOO_MOSAIC_SLAB) {
            WirelessHopperBlock.clear(block);
            return;
        }
        WirelessHopperBlock.writeData(block, serialize());
    }

    private byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(outputStream)) {
            out.writeInt(MAGIC);
            out.writeInt(2);
            out.writeInt(filters.length);
            for (ItemStack item : filters) {
                writeItemStack(out, item);
            }
            out.writeInt(buffer.length);
            for (ItemStack item : buffer) {
                writeItemStack(out, item);
            }
            writeItemStack(out, upgradeItem);
            writeItemStack(out, targetItem);
            out.writeBoolean(whitelist);
            out.writeUTF(targetInfo == null ? "" : targetInfo.toString());
            out.writeUTF(ownerId == null ? "" : ownerId.toString());
            out.flush();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize hopper data", ex);
        }
    }

    private static void writeItemStack(DataOutputStream out, ItemStack item) throws IOException {
        if (item == null || item.getType().isAir()) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = item.serializeAsBytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static ItemStack readItemStack(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }
        if (length == 0) {
            return null;
        }
        if (length > 2_000_000) {
            return null;
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        if (!isValidItemStackBytes(bytes)) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void skipItemStack(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0) {
            return;
        }
        if (length > 2_000_000) {
            in.skipBytes(length);
            return;
        }
        in.skipBytes(length);
    }

    static UUID readOwnerId(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(inputStream)) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                return null;
            }
            int versionOrFilterCount = in.readInt();
            int version = 0;
            int filterCount;
            if (versionOrFilterCount == 1 || versionOrFilterCount == 2) {
                version = versionOrFilterCount;
                filterCount = in.readInt();
            } else {
                filterCount = versionOrFilterCount;
            }
            if (filterCount < 0 || filterCount > FILTER_SLOTS) {
                return null;
            }
            for (int i = 0; i < filterCount; i++) {
                skipItemStack(in);
            }
            int bufferCount = in.readInt();
            if (bufferCount < 0 || bufferCount > BUFFER_SLOTS) {
                return null;
            }
            for (int i = 0; i < bufferCount; i++) {
                skipItemStack(in);
            }
            skipItemStack(in);
            skipItemStack(in);
            in.readBoolean();
            in.readUTF();
            if (version >= 2) {
                String owner = in.readUTF();
                if (owner.isEmpty()) {
                    return null;
                }
                return UUID.fromString(owner);
            }
            return null;
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isValidItemStackBytes(byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }
        return (bytes[0] == (byte) 0x1F && bytes[1] == (byte) 0x8B)
            || (bytes[0] == (byte) 0x28 && bytes[1] == (byte) 0xB5);
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

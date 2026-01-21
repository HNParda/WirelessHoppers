package com.hnp_arda.wireless_hoppers;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class HopperStorage {
    private final File file;
    private final Map<HopperRegistry.HopperPos, byte[]> data = new HashMap<>();
    private final Map<HopperRegistry.ChunkKey, Set<HopperRegistry.HopperPos>> index = new HashMap<>();

    HopperStorage(File dataFolder) {
        this.file = new File(dataFolder, "hoppers.dat");
    }

    void load() {
        if (!file.exists()) {
            return;
        }
        data.clear();
        index.clear();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                UUID worldId = UUID.fromString(in.readUTF());
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                int length = in.readInt();
                byte[] payload = new byte[length];
                in.readFully(payload);
                HopperRegistry.HopperPos pos = new HopperRegistry.HopperPos(worldId, x, y, z);
                data.put(pos, payload);
                index(pos);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load hopper storage", ex);
        }
    }

    void save() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create data folder: " + parent.getAbsolutePath());
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(data.size());
            for (Map.Entry<HopperRegistry.HopperPos, byte[]> entry : data.entrySet()) {
                HopperRegistry.HopperPos pos = entry.getKey();
                out.writeUTF(pos.worldId().toString());
                out.writeInt(pos.x());
                out.writeInt(pos.y());
                out.writeInt(pos.z());
                byte[] payload = entry.getValue() == null ? new byte[0] : entry.getValue();
                out.writeInt(payload.length);
                out.write(payload);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save hopper storage", ex);
        }
    }

    boolean contains(Block block) {
        return data.containsKey(HopperRegistry.HopperPos.fromBlock(block));
    }

    byte[] get(Block block) {
        return data.get(HopperRegistry.HopperPos.fromBlock(block));
    }

    void put(Block block, byte[] payload) {
        HopperRegistry.HopperPos pos = HopperRegistry.HopperPos.fromBlock(block);
        data.put(pos, payload == null ? new byte[0] : payload);
        index(pos);
    }

    void remove(Block block) {
        HopperRegistry.HopperPos pos = HopperRegistry.HopperPos.fromBlock(block);
        data.remove(pos);
        HopperRegistry.ChunkKey key = new HopperRegistry.ChunkKey(pos.worldId(), pos.x() >> 4, pos.z() >> 4);
        Set<HopperRegistry.HopperPos> positions = index.get(key);
        if (positions != null) {
            positions.remove(pos);
            if (positions.isEmpty()) {
                index.remove(key);
            }
        }
    }

    Set<HopperRegistry.HopperPos> positionsInChunk(Chunk chunk) {
        return new HashSet<>(index.getOrDefault(new HopperRegistry.ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()), Set.of()));
    }

    void removeInvalid(Block block) {
        remove(block);
    }

    private void index(HopperRegistry.HopperPos pos) {
        HopperRegistry.ChunkKey key = new HopperRegistry.ChunkKey(pos.worldId(), pos.x() >> 4, pos.z() >> 4);
        index.computeIfAbsent(key, ignored -> new HashSet<>()).add(pos);
    }
}

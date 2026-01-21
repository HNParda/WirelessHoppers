package com.hnp_arda.wireless_hoppers;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ItemIndex {
    private final Map<HopperRegistry.ChunkKey, List<Item>> itemsByChunk = new HashMap<>();

    void update(World world, Set<HopperRegistry.ChunkKey> activeChunks) {
        itemsByChunk.keySet().removeIf(key -> key.worldId().equals(world.getUID()));
        for (HopperRegistry.ChunkKey key : activeChunks) {
            if (!key.worldId().equals(world.getUID())) {
                continue;
            }
            Chunk chunk = world.getChunkAt(key.x(), key.z());
            List<Item> items = new ArrayList<>();
            for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                if (entity instanceof Item item) {
                    items.add(item);
                }
            }
            itemsByChunk.put(key, items);
        }
    }

    List<Item> getItemsInChunk(HopperRegistry.ChunkKey key) {
        return itemsByChunk.getOrDefault(key, List.of());
    }
}

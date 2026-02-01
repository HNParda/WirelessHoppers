package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

final class SimpleClaimSystemHook {
    private static boolean initialized;
    private static boolean available;
    private static Method getClaimAtChunkMethod;
    private static Method getPermissionForPlayerMethod;
    private static Object apiInstance;

    private SimpleClaimSystemHook() {
    }

    static boolean canInteract(Player player, Block block) {
        if (!init()) {
            return true;
        }
        try {
            Object claim = getClaimAtChunkMethod.invoke(apiInstance, block.getChunk());
            if (claim == null) {
                return true;
            }
            Object allowed = getPermissionForPlayerMethod.invoke(claim, "InteractBlocks", player);
            return allowed instanceof Boolean b && b;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static boolean init() {
        if (initialized) {
            return available;
        }
        initialized = true;
        if (Bukkit.getPluginManager().getPlugin("SimpleClaimSystem") == null) {
            available = false;
            return false;
        }
        try {
            Class<?> provider = Class.forName("fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider");
            Method getApiMethod = provider.getMethod("getAPI");
            apiInstance = getApiMethod.invoke(null);
            if (apiInstance == null) {
                available = false;
                return false;
            }
            Class<?> apiClass = Class.forName("fr.xyness.SCS.API.SimpleClaimSystemAPI");
            getClaimAtChunkMethod = apiClass.getMethod("getClaimAtChunk", org.bukkit.Chunk.class);
            Class<?> claimClass = Class.forName("fr.xyness.SCS.Types.Claim");
            getPermissionForPlayerMethod = claimClass.getMethod("getPermissionForPlayer", String.class, Player.class);
            available = true;
            return true;
        } catch (Exception ex) {
            available = false;
            return false;
        }
    }
}

package com.hnp_arda.wireless_hoppers;

import org.bukkit.configuration.file.FileConfiguration;

final class PlacementConfig {
    private static final String PATH_MAX_PER_PLAYER = "limits.max-hoppers-per-player";
    private static int maxHoppersPerPlayer = -1;

    private PlacementConfig() {
    }

    static void load(Main plugin) {
        FileConfiguration config = plugin.getConfig();
        maxHoppersPerPlayer = config.getInt(PATH_MAX_PER_PLAYER, -1);
    }

    static int maxHoppersPerPlayer() {
        return maxHoppersPerPlayer;
    }
}

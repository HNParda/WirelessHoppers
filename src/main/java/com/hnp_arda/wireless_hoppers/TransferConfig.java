package com.hnp_arda.wireless_hoppers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;

final class TransferConfig {
    private static final String PATH_DEFAULT_COOLDOWN = "transfer.default.cooldown-ticks";
    private static final String PATH_DEFAULT_ITEMS = "transfer.default.items-per-transfer";
    private static final String PATH_UPGRADES = "transfer.upgrades.";

    private static int defaultCooldownTicks = 8;
    private static int defaultItemsPerTransfer = 1;
    private static final EnumMap<UpgradeTier, TierSettings> tierSettings = new EnumMap<>(UpgradeTier.class);

    private TransferConfig() {
    }

    static void load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        defaultCooldownTicks = clamp(config.getInt(PATH_DEFAULT_COOLDOWN, defaultCooldownTicks), 1);
        defaultItemsPerTransfer = clamp(config.getInt(PATH_DEFAULT_ITEMS, defaultItemsPerTransfer), 1);
        for (UpgradeTier tier : UpgradeTier.values()) {
            String key = PATH_UPGRADES + tier.name().toLowerCase();
            int cooldown = clamp(config.getInt(key + ".cooldown-ticks", tier.defaultCooldownTicks()), 1);
            int items = clamp(config.getInt(key + ".items-per-transfer", tier.defaultItemsPerTransfer()), 1);
            tierSettings.put(tier, new TierSettings(cooldown, items));
        }
    }

    static int defaultCooldownTicks() {
        return defaultCooldownTicks;
    }

    static int defaultItemsPerTransfer() {
        return defaultItemsPerTransfer;
    }

    static int cooldownFor(UpgradeTier tier, int fallback) {
        TierSettings settings = tierSettings.get(tier);
        return settings == null ? fallback : settings.cooldownTicks();
    }

    static int itemsFor(UpgradeTier tier, int fallback) {
        TierSettings settings = tierSettings.get(tier);
        return settings == null ? fallback : settings.itemsPerTransfer();
    }

    private static int clamp(int value, int min) {
        return Math.max(value, min);
    }

    private record TierSettings(int cooldownTicks, int itemsPerTransfer) {
    }
}

package com.hnp_arda.wireless_hoppers;

enum UpgradeTier {
    IRON(16, 4, "upgrade.tier.iron"),
    GOLD(12, 8, "upgrade.tier.gold"),
    DIAMOND(8, 16, "upgrade.tier.diamond"),
    NETHERITE(4, 32, "upgrade.tier.netherite");

    private final int cooldownTicks;
    private final int itemsPerTransfer;
    private final String langKey;

    UpgradeTier(int cooldownTicks, int itemsPerTransfer, String langKey) {
        this.cooldownTicks = cooldownTicks;
        this.itemsPerTransfer = itemsPerTransfer;
        this.langKey = langKey;
    }

    int cooldownTicks() {
        return cooldownTicks;
    }

    int itemsPerTransfer() {
        return itemsPerTransfer;
    }

    String langKey() {
        return langKey;
    }
}

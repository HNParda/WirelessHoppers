package com.hnp_arda.wireless_hoppers;

enum UpgradeTier {
    IRON(16, 4, "Iron Upgrade"),
    GOLD(12, 8, "Gold Upgrade"),
    DIAMOND(8, 16, "Diamond Upgrade"),
    NETHERITE(4, 32, "Netherite Upgrade");

    private final int cooldownTicks;
    private final int itemsPerTransfer;
    private final String displayName;

    UpgradeTier(int cooldownTicks, int itemsPerTransfer, String displayName) {
        this.cooldownTicks = cooldownTicks;
        this.itemsPerTransfer = itemsPerTransfer;
        this.displayName = displayName;
    }

    int cooldownTicks() {
        return cooldownTicks;
    }

    int itemsPerTransfer() {
        return itemsPerTransfer;
    }

    String displayName() {
        return displayName;
    }
}

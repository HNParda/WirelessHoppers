package com.hnp_arda.wireless_hoppers;

enum UpgradeTier {
    IRON(6, 2, "upgrade.tier.iron"),
    GOLD(4, 4, "upgrade.tier.gold"),
    DIAMOND(2, 8, "upgrade.tier.diamond"),
    NETHERITE(1, 16, "upgrade.tier.netherite");

    private final int defaultCooldownTicks;
    private final int defaultItemsPerTransfer;
    private final String langKey;

    UpgradeTier(int cooldownTicks, int itemsPerTransfer, String langKey) {
        this.defaultCooldownTicks = cooldownTicks;
        this.defaultItemsPerTransfer = itemsPerTransfer;
        this.langKey = langKey;
    }

    int cooldownTicks() {
        return TransferConfig.cooldownFor(this, defaultCooldownTicks);
    }

    int itemsPerTransfer() {
        return TransferConfig.itemsFor(this, defaultItemsPerTransfer);
    }

    String langKey() {
        return langKey;
    }

    int defaultCooldownTicks() {
        return defaultCooldownTicks;
    }

    int defaultItemsPerTransfer() {
        return defaultItemsPerTransfer;
    }
}

package com.hnp_arda.wireless_hoppers;

enum RedstoneMode {
    LOW("gui.redstone.low", "gui.redstone.low_summary"),
    HIGH("gui.redstone.high", "gui.redstone.high_summary"),
    IGNORED("gui.redstone.ignored", "gui.redstone.ignored_summary");

    private final String labelKey;
    private final String summaryKey;

    RedstoneMode(String labelKey, String summaryKey) {
        this.labelKey = labelKey;
        this.summaryKey = summaryKey;
    }

    String labelKey() {
        return labelKey;
    }

    String summaryKey() {
        return summaryKey;
    }

    RedstoneMode next() {
        return switch (this) {
            case LOW -> HIGH;
            case HIGH -> IGNORED;
            case IGNORED -> LOW;
        };
    }
}

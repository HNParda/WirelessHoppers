package com.hnp_arda.wireless_hoppers;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

final class Keys {
    static NamespacedKey BLOCK_MARKER;
    static NamespacedKey DATA;
    static NamespacedKey UPGRADE;
    static NamespacedKey ITEM_TYPE;
    static NamespacedKey TOOL_TARGET;
    static NamespacedKey FILTER_PANE;

    private Keys() {
    }

    static void init(JavaPlugin plugin) {
        BLOCK_MARKER = new NamespacedKey(plugin, "wireless_hopper");
        DATA = new NamespacedKey(plugin, "data");
        UPGRADE = new NamespacedKey(plugin, "upgrade");
        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
        TOOL_TARGET = new NamespacedKey(plugin, "tool_target");
        FILTER_PANE = new NamespacedKey(plugin, "filter_pane");
    }
}

package com.hnp_arda.wireless_hoppers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HexFormat;

final class ResourcePackManager {

    final String URL = "https://download.mc-packs.net/pack/c31012a18e00071920d36576cd7bb4bd2da8d2aa.zip";
    final String HASH = "c31012a18e00071920d36576cd7bb4bd2da8d2aa";

    byte[] hash;

    ResourcePackManager() {
        hash = HexFormat.of().parseHex(HASH);
    }

    void prompt(Player player) {
        player.setResourcePack(URL, hash, Component.text("WirelessHopper Resource Pack"), true);
    }

}

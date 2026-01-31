package com.hnp_arda.wireless_hoppers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HexFormat;

final class ResourcePackManager {
    private final JavaPlugin plugin;

    ResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    void prompt(Player player) {
        String url = plugin.getConfig().getString("resource-pack-url", "");
        if (url.isBlank()) {
            player.sendMessage(Component.text(
                    Lang.tr(player, "resourcepack.missing_url"),
                    net.kyori.adventure.text.format.NamedTextColor.RED
            ));
            return;
        }
        String hash = plugin.getConfig().getString("resource-pack-sha1", "");
        boolean required = plugin.getConfig().getBoolean("resource-pack-required", false);
        boolean useSha1 = plugin.getConfig().getBoolean("resource-pack-use-sha1", true);
        String prompt = plugin.getConfig().getString("resource-pack-prompt", "WirelessHopper Resource Pack");
        if (!useSha1 || hash.isBlank()) {
            player.setResourcePack(url, null, Component.text(prompt), required);
        } else {
            player.setResourcePack(url, hexToBytes(hash), Component.text(prompt), required);
        }
    }

}

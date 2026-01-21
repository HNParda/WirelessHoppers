package com.hnp_arda.wireless_hoppers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ResourcePackManager {
    private static final List<String> RESOURCE_FILES = List.of(
            "resourcepack/pack.mcmeta",
            "resourcepack/assets/minecraft/blockstates/warped_slab.json",
            "resourcepack/assets/wirelesshopper/models/block/wireless_hopper.json",
            "resourcepack/assets/wirelesshopper/textures/block/wireless_hopper.png",
            "resourcepack/assets/wirelesshopper/textures/block/wireless_hopper_top.png",
            "resourcepack/assets/wirelesshopper/textures/block/wireless_hopper_inside.png",
            "resourcepack/assets/wirelesshopper/items/wireless_hopper_item.json",
            "resourcepack/assets/wirelesshopper/items/wireless_upgrade_iron.json",
            "resourcepack/assets/wirelesshopper/items/wireless_upgrade_gold.json",
            "resourcepack/assets/wirelesshopper/items/wireless_upgrade_diamond.json",
            "resourcepack/assets/wirelesshopper/items/wireless_upgrade_netherite.json",
            "resourcepack/assets/wirelesshopper/items/wireless_target_tool.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_hopper_item.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_upgrade_iron.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_upgrade_gold.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_upgrade_diamond.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_upgrade_netherite.json",
            "resourcepack/assets/wirelesshopper/models/item/wireless_target_tool.json",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_hopper_item.png",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_upgrade_iron.png",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_upgrade_gold.png",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_upgrade_diamond.png",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_upgrade_netherite.png",
            "resourcepack/assets/wirelesshopper/textures/item/wireless_target_tool.png"
    );

    private final JavaPlugin plugin;
    private final File packFile;
    private String sha1;
    private HttpServer server;
    private boolean packReady;

    ResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.packFile = new File(plugin.getDataFolder(), "resourcepack.zip");
    }

    private static String sha1(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    void ensurePackReady() {
        plugin.getDataFolder().mkdirs();
        if (!packReady) {
            try {
                buildZip();
                sha1 = sha1(packFile);
                packReady = true;
                if (plugin.getConfig().getString("resource-pack-sha1", "").isEmpty()) {
                    plugin.getConfig().set("resource-pack-sha1", sha1);
                    plugin.saveConfig();
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to build resource pack zip: " + ex.getMessage());
                return;
            }
        }
        try {
            startHttpServer();
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to start resource pack server: " + ex.getMessage());
        }
    }

    void prompt(Player player) {
        ensurePackReady();
        String url = resolvedUrl();
        if (url == null || url.isBlank()) {
            player.sendMessage(Component.text("Resource pack URL is not configured.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        String hash = sha1 == null ? "" : sha1;
        boolean required = plugin.getConfig().getBoolean("resource-pack-required", false);
        boolean useSha1 = plugin.getConfig().getBoolean("resource-pack-use-sha1", true);
        String prompt = plugin.getConfig().getString("resource-pack-prompt", "WirelessHopper Resource Pack");
        if (!useSha1 || hash == null || hash.isBlank()) {
            player.setResourcePack(url, null, Component.text(prompt), required);
        } else {
            player.setResourcePack(url, hexToBytes(hash), Component.text(prompt), required);
        }
    }

    void shutdown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private String resolvedUrl() {
        String url = plugin.getConfig().getString("resource-pack-url", "");
        if (url != null && !url.isBlank()) {
            return url;
        }
        String host = plugin.getConfig().getString("resource-pack-host", "localhost");
        int port = plugin.getConfig().getInt("resource-pack-port", 8123);
        String version = sha1 == null ? "0" : sha1;
        return "http://" + host + ":" + port + "/resourcepack.zip?v=" + version;
    }

    private void buildZip() throws IOException {
        if (packFile.exists()) {
            packFile.delete();
        }
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(packFile))) {
            for (String path : RESOURCE_FILES) {
                try (InputStream in = plugin.getResource(path)) {
                    if (in == null) {
                        plugin.getLogger().warning("Missing resource: " + path);
                        continue;
                    }
                    ZipEntry entry = new ZipEntry(path.replace("resourcepack/", ""));
                    entry.setTime(0);
                    out.putNextEntry(entry);
                    in.transferTo(out);
                    out.closeEntry();
                }
            }
        }
    }

    private void startHttpServer() throws IOException {
        if (server != null) {
            return;
        }
        int port = plugin.getConfig().getInt("resource-pack-port", 8123);
        String bind = plugin.getConfig().getString("resource-pack-bind", "0.0.0.0");
        InetSocketAddress address = new InetSocketAddress(bind, port);
        server = HttpServer.create(address, 0);
        server.createContext("/resourcepack.zip", new PackHandler(packFile));
        server.start();
        plugin.getLogger().info("WirelessHopper resource pack server running at " + resolvedUrl());
    }

    private record PackHandler(File packFile) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                if (!packFile.exists()) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.getResponseHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
                exchange.getResponseHeaders().add("Pragma", "no-cache");
                exchange.getResponseHeaders().add("Expires", "0");
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(packFile.length()));
                exchange.sendResponseHeaders(200, packFile.length());
                try (var out = exchange.getResponseBody()) {
                    Files.copy(packFile.toPath(), out);
                }
                exchange.close();
            }
        }
}

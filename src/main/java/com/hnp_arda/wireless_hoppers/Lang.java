package com.hnp_arda.wireless_hoppers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Lang {
    private static final Map<String, Map<String, String>> LOCALES = new HashMap<>();
    private static final Map<UUID, String> PLAYER_LOCALES = new HashMap<>();
    private static final Set<String> AVAILABLE_LOCALES = new HashSet<>();
    private static String defaultLocale = "de";
    private static boolean defaultAuto = false;
    private static JavaPlugin plugin;

    private Lang() {
    }

    static void init(JavaPlugin plugin) {
        Lang.plugin = plugin;
        String configLocale = plugin.getConfig().getString("lang-default", "de");
        defaultAuto = "auto".equalsIgnoreCase(configLocale);
        defaultLocale = normalizeLocale(configLocale);
        loadLocale(plugin, "en");
        loadLocale(plugin, "de");
        loadPlayerLocales(plugin);
    }

    static String defaultLocale() {
        return defaultLocale;
    }

    static Set<String> availableLocales() {
        return Set.copyOf(AVAILABLE_LOCALES);
    }

    static String localeFromPlayer(Player player) {
        if (player == null) {
            return defaultLocale;
        }
        String stored = PLAYER_LOCALES.get(player.getUniqueId());
        if (stored != null && !stored.isBlank()) {
            if ("auto".equalsIgnoreCase(stored)) {
                return normalizeLocale(player.getLocale());
            }
            return stored;
        }
        if (defaultAuto) {
            return normalizeLocale(player.getLocale());
        }
        return defaultLocale;
    }

    static String tr(Player player, String key) {
        return tr(localeFromPlayer(player), key, Map.of());
    }

    static String tr(Player player, String key, Map<String, String> args) {
        return tr(localeFromPlayer(player), key, args);
    }

    static String tr(String locale, String key) {
        return tr(locale, key, Map.of());
    }

    static String tr(String locale, String key, Map<String, String> args) {
        String normalized = normalizeLocale(locale);
        String value = getValue(normalized, key);
        if (value == null && !normalized.equals(defaultLocale)) {
            value = getValue(defaultLocale, key);
        }
        if (value == null) {
            value = key;
        }
        for (Map.Entry<String, String> entry : args.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    private static void loadLocale(JavaPlugin plugin, String locale) {
        try (InputStream in = plugin.getResource("lang/" + locale + ".yml")) {
            if (in == null) {
                plugin.getLogger().warning("Missing language file: lang/" + locale + ".yml");
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(in, StandardCharsets.UTF_8)
            );
            Map<String, String> map = new HashMap<>();
            for (String key : config.getKeys(true)) {
                if (config.isConfigurationSection(key)) {
                    continue;
                }
                Object value = config.get(key);
                if (value != null) {
                    map.put(key, String.valueOf(value));
                }
            }
            LOCALES.put(locale, map);
            AVAILABLE_LOCALES.add(locale);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load language file: lang/" + locale + ".yml (" + ex.getMessage() + ")");
        }
    }

    private static void loadPlayerLocales(JavaPlugin plugin) {
        PLAYER_LOCALES.clear();
        var section = plugin.getConfig().getConfigurationSection("lang-players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String locale = normalizeLocale(section.getString(key, ""));
                if (!locale.isBlank()) {
                    PLAYER_LOCALES.put(uuid, locale);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    static void setPlayerLocale(UUID playerId, String locale) {
        if (plugin == null || playerId == null) {
            return;
        }
        String normalized = normalizeLocale(locale);
        if (locale == null || locale.isBlank()) {
            PLAYER_LOCALES.remove(playerId);
            plugin.getConfig().set("lang-players." + playerId, null);
            plugin.saveConfig();
            return;
        }
        if ("auto".equalsIgnoreCase(locale) || "auto".equalsIgnoreCase(normalized)) {
            PLAYER_LOCALES.put(playerId, "auto");
            plugin.getConfig().set("lang-players." + playerId, "auto");
            plugin.saveConfig();
            return;
        }
        if (!AVAILABLE_LOCALES.contains(normalized)) {
            return;
        }
        PLAYER_LOCALES.put(playerId, normalized);
        plugin.getConfig().set("lang-players." + playerId, normalized);
        plugin.saveConfig();
    }

    private static String getValue(String locale, String key) {
        Map<String, String> map = LOCALES.get(locale);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return defaultLocale;
        }
        String normalized = locale.toLowerCase();
        int dash = normalized.indexOf('-');
        if (dash >= 0) {
            normalized = normalized.substring(0, dash);
        }
        int underscore = normalized.indexOf('_');
        if (underscore >= 0) {
            normalized = normalized.substring(0, underscore);
        }
        return normalized.isBlank() ? defaultLocale : normalized;
    }
}

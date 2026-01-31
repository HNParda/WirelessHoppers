package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class Main extends JavaPlugin implements Listener {
    private HopperRegistry registry;
    private ResourcePackManager resourcePackManager;
    private HopperStorage storage;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Lang.init(this);
        Keys.init(this);
        storage = new HopperStorage(getDataFolder());
        storage.load();
        WirelessHopperBlock.init(storage);
        registry = new HopperRegistry(storage);
        ItemIndex itemIndex = new ItemIndex();
        HopperScheduler scheduler = new HopperScheduler(registry, itemIndex);
        resourcePackManager = new ResourcePackManager(this);

        Bukkit.getPluginManager().registerEvents(new WirelessHopperListener(registry), this);
        Bukkit.getPluginManager().registerEvents(new HopperGuiListener(registry, this), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        WirelessHopperCommand command = new WirelessHopperCommand(resourcePackManager);
        PluginCommand cmd = getCommand("wirelesshopper");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        recipeManager = new RecipeManager(this);
        recipeManager.registerAll();

        Bukkit.getWorlds().forEach(world -> {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                registry.registerChunk(chunk);
            }
        });

        Bukkit.getScheduler().runTaskTimer(this, scheduler, 1L, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("recipes-auto-unlock-on-join", true) && recipeManager != null) {
            event.getPlayer().discoverRecipes(recipeManager.recipeKeys());
        }
        if (!getConfig().getBoolean("resource-pack-auto-on-join", true)) {
            return;
        }
        resourcePackManager.prompt(event.getPlayer());
    }

    @Override
    public void onDisable() {
        if (registry == null) {
            return;
        }
        for (HopperData data : registry.allHoppers()) {
            data.save(data.location().getBlock());
        }
        if (storage != null) {
            storage.save();
        }
    }
}

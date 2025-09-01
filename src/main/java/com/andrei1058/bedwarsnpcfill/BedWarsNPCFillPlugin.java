package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BedWarsNPCFillPlugin extends JavaPlugin {

    private static BedWarsNPCFillPlugin instance;
    private ConfigurationHandler configHandler;

    @Override
    public void onEnable() {
        instance = this;
        configHandler = new ConfigurationHandler(this);
        Bukkit.getPluginManager().registerEvents(new BedWarsEventListener(), this);
        
        // Register command executor
        getCommand("bedwarsnpcfill").setExecutor(new BedWarsNPCFillCommand());
        
        getLogger().info("[BedWarsNPCFill] Plugin enabled successfully!");
    }

    public ConfigurationHandler getConfigHandler() {
        return configHandler;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BedWarsNPCFillPlugin getInstance() {
        return instance;
    }
}

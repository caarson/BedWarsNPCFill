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

        /*
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Sentinel") && Class.forName("org.mcmonkey.sentinel.SentinelPlugin") != null) {
                Bukkit.getPluginManager().registerEvents(new SentinelBridgeListener(), this);
                getLogger().info("[BedWarsNPCFill] Registered Sentinel bridge listener (events)");

                Object instance = Class.forName("org.mcmonkey.sentinel.SentinelPlugin").getField("instance").get(null);
                if (instance != null) {
                    Class.forName("org.mcmonkey.sentinel.SentinelPlugin")
                        .getMethod("registerIntegration", Class.forName("org.mcmonkey.sentinel.SentinelIntegration"))
                        .invoke(instance, new BedWarsSentinelIntegration());
                    getLogger().info("[BedWarsNPCFill] Registered Sentinel tryAttack hook (integration)");
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Sentinel not installed, integration skipped.
        } catch (Exception ex) {
            getLogger().warning("[BedWarsNPCFill] Failed to register Sentinel integration: " + ex.getMessage());
        }
        */
        
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

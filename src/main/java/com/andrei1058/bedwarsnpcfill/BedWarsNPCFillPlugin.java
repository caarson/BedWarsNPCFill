package com.andrei1058.bedwarsnpcfill;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.andrei1058.bedwars.api.BedWars;

public class BedWarsNPCFillPlugin extends JavaPlugin {
    private static BedWarsNPCFillPlugin instance;
    
    public void onEnable() {
        if (BedWars.getInstance() == null) {
            getLogger().severe("BedWars 1058 API must be installed!");
            return;
        }
        
        saveDefaultConfig();
        // Initialize config handler
        new ConfigurationHandler(this);
        
        // Register listeners
        PluginManager pm = getServer().getPluginManager();
        //pm.registerEvents(new GameListener(), this); // To be implemented
        
        BedWars.getInstance().addFill NPC logic here...
    }
    
    public void onDisable() {
        // Cleanup NPCs and resources
    }
    
    public static BedWarsNPCFillPlugin getInstance() {
        return instance;
    }
}

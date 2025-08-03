package com.andrei1058.bedwarsnpcfill;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BedWarsNPCFillPlugin extends JavaPlugin {
    
    public void onEnable() {
        saveDefaultConfig();
        
        PluginManager pm = getServer().getPluginManager();
        
        // Register listeners for BedWars events
        pm.registerEvents(new GameEventListener(this), this);
        
        // Initialize NPC manager
        NPCHandler npcHandler = new NPCHandler(this);
    }

    public void onDisable() {
        // Cleanup logic here
    }
}

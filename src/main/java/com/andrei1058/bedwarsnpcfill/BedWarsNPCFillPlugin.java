package com.andrei1058.bedwarsnpcfill;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.events.BedWarsGameStartedEvent;
import com.andrei1058.bedwars.api.events.BedWarsGameEndedEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.Bukkit;

public class BedWarsNPCFillPlugin extends JavaPlugin implements Listener {
    private static BedWarsNPCFillPlugin instance;
    private ConfigurationHandler configHandler;
    private NPCManager npcManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        if (BedWars.getInstance() == null) {
            getLogger().severe("BedWars 1058 API must be installed!");
            return;
        }
        
        saveDefaultConfig();
        // Initialize config handler and NPC manager
        configHandler = new ConfigurationHandler(this);
        npcManager = new NPCManager(this);
        
        // Register listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new GameListener(), this);
        
        getLogger().info("BedWars NPC Fill plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cleanup NPCs and resources
        getLogger().info("Cleaning up NPCs...");
        // In a real implementation, we would clean up all NPCs here
    }
    
    @EventHandler
    public void onGameStart(BedWarsGameStartedEvent event) {
        // Place bed defenses for all teams with NPCs
        for (com.andrei1058.bedwars.api.arena.team.ITeam team : event.getArena().getTeams()) {
            npcManager.placeBedDefense(
                team.getBed(), 
                configHandler.getBedDefenseTemplate()
            );
        }
    }
    
    @EventHandler
    public void onGameEnd(BedWarsGameEndedEvent event) {
        // Cleanup NPCs for all teams
        for (com.andrei1058.bedwars.api.arena.team.ITeam team : event.getArena().getTeams()) {
            npcManager.removeTeamNPCs(team.getName());
        }
    }
    
    public static BedWarsNPCFillPlugin getInstance() {
        return instance;
    }
    
    public ConfigurationHandler getConfigurationHandler() {
        return configHandler;
    }
    
    public NPCManager getNpcManager() {
        return npcManager;
    }
}

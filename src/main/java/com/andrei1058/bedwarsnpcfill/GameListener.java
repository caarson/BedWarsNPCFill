package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.events.BedWarsTeamChangeEvent;
import com.andrei1058.bedwars.api.events.BedWarsPlayerLeaveEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import java.util.logging.Logger;

public class GameListener implements Listener {
    private final Logger logger = Logger.getLogger(GameListener.class.getName());
    
    @EventHandler
    public void onBedWarsTeamChange(BedWarsTeamChangeEvent event) {
        // Check if team is not full and needs NPC filling
        if (event.getNewSize() < BedWars.MAX_PLAYERS_PER_TEAM) {
            // Get NPC manager and spawn NPCs for this team
            NPCManager npcManager = BedWarsNPCFillPlugin.getInstance().getNpcManager();
            
            // Get team spawn location (in real implementation, this would come from BedWars API)
            // For now, we'll use a placeholder location
            Location spawnLocation = new Location(event.getPlayer().getWorld(), 0, 0, 0);
            
            npcManager.spawnNPCsForTeam(
                event.getTeamName(), 
                spawnLocation, 
                event.getNewSize()
            );
        }
    }
    
    @EventHandler
    public void onPlayerLeave(BedWarsPlayerLeaveEvent event) {
        // When a player leaves, check if team needs NPCs
        String teamName = event.getTeam().getName();
        int currentPlayers = event.getTeam().getSize();
        
        NPCManager npcManager = BedWarsNPCFillPlugin.getInstance().getNpcManager();
        Location spawnLocation = new Location(event.getPlayer().getWorld(), 0, 0, 0);
        
        npcManager.spawnNPCsForTeam(teamName, spawnLocation, currentPlayers);
    }
}

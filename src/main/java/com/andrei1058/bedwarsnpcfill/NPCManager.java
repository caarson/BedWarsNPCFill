package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCManager {
    private final BedWarsNPCFillPlugin plugin;
    private final ConfigurationHandler config;
    
    // Map to track NPCs by team
    private final Map<String, List<UUID>> teamNPCs = new HashMap<>();
    
    // Map to track bed break cooldowns (team -> timestamp)
    private final Map<String, Long> bedBreakCooldowns = new HashMap<>();
    
    public NPCManager(BedWarsNPCFillPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigurationHandler();
        
        // Start the NPC task scheduler
        startNPCTask();
    }
    
    /**
     * Spawns NPCs for a specific team if needed
     */
    public void spawnNPCsForTeam(String teamName, Location spawnLocation, int currentPlayers) {
        // Check if we should spawn NPCs for this team
        int maxNPCs = config.getMaxNPCsPerTeam();
        int minPlayers = config.getMinPlayersToSpawn();
        
        if (currentPlayers < minPlayers && maxNPCs > 0) {
            int neededNPCs = Math.min(maxNPCs, minPlayers - currentPlayers);
            
            // Place bed defense for this team
            placeBedDefense(spawnLocation, config.getBedDefenseTemplate());
            
            // In a real implementation, we would spawn NPCs here
            // This is where Citizens/Sentinel integration would happen
            
            Bukkit.getLogger().info("Spawning " + neededNPCs + " NPCs for team: " + teamName + " at " + spawnLocation);
        }
    }
    
    /**
     * Remove all NPCs from a team when it's eliminated or game ends
     */
    public void removeTeamNPCs(String teamName) {
        List<UUID> npcList = teamNPCs.get(teamName);
        if (npcList != null) {
            // In a real implementation, we would remove the actual NPCs here
            
            Bukkit.getLogger().info("Removing " + npcList.size() + " NPCs for team: " + teamName);
            
            // Clear from map
            npcList.clear();
        }
    }
    
    /**
     * Pre-place bed defense structure around a team's bed
     */
    public void placeBedDefense(Location bedLocation, List<ConfigurationHandler.BlockOffset> template) {
        if (bedLocation == null || template == null) return;
        
        World world = bedLocation.getWorld();
        if (world == null) return;
        
        for (ConfigurationHandler.BlockOffset block : template) {
            Location blockLoc = bedLocation.clone().add(block.getX(), block.getY(), block.getZ());
            try {
                Material material = Material.valueOf(block.getType());
                if (blockLoc.getBlock().getType() == Material.AIR) {
                    blockLoc.getBlock().setType(material);
                }
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid material in bed defense template: " + block.getType());
            }
        }
    }
    
    /**
     * Start the NPC behavior task
     */
    private void startNPCTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // This would iterate through all active NPCs and perform:
                // 1. Targeting nearest player within combat radius
                // 2. Bridging toward players within bridging range
                // 3. Checking for bed break proximity
                
                // Placeholder logic - in real implementation this would use Citizens API
                for (Map.Entry<String, List<UUID>> entry : teamNPCs.entrySet()) {
                    for (UUID npcId : entry.getValue()) {
                        // Simulated NPC behavior
                        handleNPCBehavior(npcId, entry.getKey());
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    /**
     * Handle NPC behavior (targeting, bridging, bed breaking)
     */
    private void handleNPCBehavior(UUID npcId, String teamName) {
        // This is placeholder logic - in a real implementation we would:
        // 1. Get nearest enemy player within combatRadius
        // 2. If player found on another island within bridgingRange, place blocks toward them
        // 3. If within bedBreakProximity of an enemy bed, trigger bed break
        
        // Simulated bed break check (with cooldown)
        long currentTime = System.currentTimeMillis();
        if (currentTime - bedBreakCooldowns.getOrDefault(teamName, 0L) > 30000) { // 30 sec cooldown
            // Check if near enemy bed
            if (Math.random() < 0.1) { // 10% chance to simulate bed break
                triggerBedBreak(teamName);
                bedBreakCooldowns.put(teamName, currentTime);
            }
        }
    }
    
    /**
     * Trigger bed break event for a team
     */
    private void triggerBedBreak(String victimTeam) {
        // Display title to all players on victim team
        String title = config.getBedBrokenTitle();
        String subtitle = config.getBedBrokenSubtitle();
        
        // In a real implementation, we would:
        // 1. Find all players on the victim team
        // 2. Send them the title
        // 3. Trigger the bed break event in BedWars 1058 API
        
        Bukkit.getLogger().info("NPC broke bed for team: " + victimTeam);
    }
    
    /**
     * Simple fallback targeting logic (when Citizens is not available)
     */
    public void simpleTargeting(Player npc, Player target) {
        // Simple attack logic: move toward player and attack
        npc.teleport(target.getLocation());
        npc.attack(target);
    }
}

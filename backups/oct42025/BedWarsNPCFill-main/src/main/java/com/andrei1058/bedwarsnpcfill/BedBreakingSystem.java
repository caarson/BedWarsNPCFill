package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * System to handle NPC bed breaking when they get within proximity of enemy beds
 */
public class BedBreakingSystem {
    
    private static BukkitRunnable bedBreakingTask;
    private static final Set<String> brokenBeds = new HashSet<>();
    
    /**
     * Start the bed breaking system
     */
    public static void startBedBreakingSystem() {
        if (bedBreakingTask != null) {
            bedBreakingTask.cancel();
        }
        
        bedBreakingTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAllNPCsForBedBreaking();
            }
        };
        
        bedBreakingTask.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 10L); // Check every 10 ticks (0.5 seconds)
        Bukkit.getLogger().info("[BedWarsNPCFill] Started NPC bed breaking system");
    }
    
    /**
     * Stop the bed breaking system
     */
    public static void stopBedBreakingSystem() {
        if (bedBreakingTask != null) {
            bedBreakingTask.cancel();
            bedBreakingTask = null;
        }
        brokenBeds.clear();
        Bukkit.getLogger().info("[BedWarsNPCFill] Stopped NPC bed breaking system");
    }
    
    /**
     * Check all NPCs for bed breaking proximity
     */
    private static void checkAllNPCsForBedBreaking() {
        try {
            // Get all NPCs from Citizens registry that are created by our plugin
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.getName().startsWith("NPC_") && npc.isSpawned() && npc.getEntity() != null) {
                    checkNPCBedBreaking(npc);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error in bed breaking check: " + e.getMessage());
        }
    }
    
    /**
     * Check if an NPC is close enough to break an enemy bed
     */
    private static void checkNPCBedBreaking(NPC npc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;
            
            Location npcLoc = entity.getLocation();
            String npcName = npc.getName();
            
            // Extract arena and team from NPC name (format: NPC_arenaName_teamName)
            String[] parts = npcName.split("_");
            if (parts.length < 3) return;
            
            String arenaName = parts[1];
            String npcTeamName = parts[2];
            
            // Get arena instance
            IArena arena = Arena.getArenaByName(arenaName);
            if (arena == null) return;
            
            // Get NPC's team
            ITeam npcTeam = arena.getTeam(npcTeamName);
            if (npcTeam == null) return;
            
            // Check all enemy teams for beds
            for (ITeam enemyTeam : arena.getTeams()) {
                if (enemyTeam.equals(npcTeam)) continue; // Skip own team
                
                // Check if enemy bed still exists
                if (enemyTeam.isBedDestroyed()) continue;
                
                // Get enemy bed location
                Location bedLocation = enemyTeam.getBed();
                if (bedLocation == null) continue;
                
                // Check distance between NPC and enemy bed
                double distance = npcLoc.distance(bedLocation);
                double breakProximity = BedWarsNPCFillPlugin.getInstance().getConfigHandler().getBedBreakProximity();
                
                if (distance <= breakProximity) {
                    // NPC is within breaking proximity - break the bed
                    breakBed(npc, enemyTeam, bedLocation, arena);
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error checking bed breaking for NPC " + npc.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Break an enemy bed and notify players
     */
    private static void breakBed(NPC npc, ITeam enemyTeam, Location bedLocation, IArena arena) {
        try {
            String bedKey = enemyTeam.getName() + "_" + arena.getArenaName();
            
            // Check if we already broke this bed to avoid duplicate breaks
            if (brokenBeds.contains(bedKey)) {
                return;
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " is breaking bed of team " + enemyTeam.getName() + " at " + bedLocation);
            
            // Mark bed as broken in BedWars system
            enemyTeam.setBedDestroyed(true);
            
            // Break the bed blocks
            breakBedBlocks(bedLocation);
            
            // Notify all players in the arena
            String bedBrokenTitle = BedWarsNPCFillPlugin.getInstance().getConfigHandler().getBedBrokenTitle();
            String bedBrokenSubtitle = BedWarsNPCFillPlugin.getInstance().getConfigHandler().getBedBrokenSubtitle();
            
            for (Player player : arena.getPlayers()) {
                if (enemyTeam.getMembers().contains(player)) {
                    // Send title to the team whose bed was broken (using simpler method for older Bukkit versions)
                    player.sendTitle(bedBrokenTitle, bedBrokenSubtitle);
                    player.sendMessage("§cYour bed was broken by an NPC!");
                } else {
                    player.sendMessage("§aNPC " + npc.getName() + " broke " + enemyTeam.getName() + "'s bed!");
                }
            }
            
            // Add to broken beds set to prevent duplicate breaks
            brokenBeds.add(bedKey);
            
            Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " successfully broke bed of team " + enemyTeam.getName());
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error breaking bed for NPC " + npc.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Break the actual bed blocks in the world
     */
    private static void breakBedBlocks(Location bedLocation) {
        try {
            // Bed blocks are typically two blocks - check both parts
            Block bedBlock = bedLocation.getBlock();
            
            // Check if this is a bed block
            if (isBedBlock(bedBlock.getType())) {
                bedBlock.setType(Material.AIR);
                Bukkit.getLogger().info("[BedWarsNPCFill] Broke bed block at " + bedLocation);
            }
            
            // Check adjacent blocks for the other part of the bed
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    
                    Location adjacentLoc = bedLocation.clone().add(x, 0, z);
                    Block adjacentBlock = adjacentLoc.getBlock();
                    
                    if (isBedBlock(adjacentBlock.getType())) {
                        adjacentBlock.setType(Material.AIR);
                        Bukkit.getLogger().info("[BedWarsNPCFill] Broke adjacent bed block at " + adjacentLoc);
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error breaking bed blocks: " + e.getMessage());
        }
    }
    
    /**
     * Check if a material is a bed block
     */
    private static boolean isBedBlock(Material material) {
        return material == Material.BED_BLOCK || 
               material.name().contains("BED") || 
               material.name().endsWith("_BED");
    }
    
    /**
     * Clear broken beds cache (useful when restarting games)
     */
    public static void clearBrokenBeds() {
        brokenBeds.clear();
        Bukkit.getLogger().info("[BedWarsNPCFill] Cleared broken beds cache");
    }
}

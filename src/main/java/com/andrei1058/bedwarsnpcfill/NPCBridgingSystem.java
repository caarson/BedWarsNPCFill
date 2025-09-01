package com.andrei1058.bedwarsnpcfill;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Custom bridging system for NPCs since Sentinel doesn't support bridging
 */
public class NPCBridgingSystem {
    
    private static final Set<NPC> activeNPCs = new HashSet<>();
    private static BukkitRunnable bridgingTask;
    
    /**
     * Start the bridging system for all NPCs
     */
    public static void startBridgingSystem() {
        if (bridgingTask != null) {
            bridgingTask.cancel();
        }
        
        bridgingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (NPC npc : activeNPCs) {
                    if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
                        checkAndBridge(npc);
                    }
                }
            }
        };
        
        bridgingTask.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 5L); // Check every 5 ticks (0.25 seconds)
        Bukkit.getLogger().info("[BedWarsNPCFill] Started custom NPC bridging system");
    }
    
    /**
     * Stop the bridging system
     */
    public static void stopBridgingSystem() {
        if (bridgingTask != null) {
            bridgingTask.cancel();
            bridgingTask = null;
        }
        activeNPCs.clear();
        Bukkit.getLogger().info("[BedWarsNPCFill] Stopped custom NPC bridging system");
    }
    
    /**
     * Add an NPC to the bridging system
     */
    public static void addNPCToBridging(NPC npc) {
        if (npc != null) {
            activeNPCs.add(npc);
        }
    }
    
    /**
     * Remove an NPC from the bridging system
     */
    public static void removeNPCFromBridging(NPC npc) {
        if (npc != null) {
            activeNPCs.remove(npc);
        }
    }
    
    /**
     * Check if an NPC needs bridging and place wool blocks
     */
    private static void checkAndBridge(NPC npc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;
            
            Location npcLoc = entity.getLocation();
            
            // Check if NPC is moving (has velocity) to avoid unnecessary bridging when stationary
            if (entity.getVelocity().lengthSquared() < 0.01) {
                return;
            }
            
            // Check the block directly below the NPC
            Location checkLoc = npcLoc.clone().subtract(0, 1, 0);
            Block blockBelow = checkLoc.getBlock();
            
            // If the block below is air or a replaceable block, place wool
            if (blockBelow.getType() == Material.AIR || isReplaceable(blockBelow.getType())) {
                // Also check if there's a block 2 blocks below to ensure we're not bridging into void
                Location checkLoc2 = npcLoc.clone().subtract(0, 2, 0);
                Block block2Below = checkLoc2.getBlock();
                
                if (block2Below.getType() != Material.AIR) {
                    // Place white wool at the block below position
                    blockBelow.setType(Material.WOOL);
                    blockBelow.setData((byte) 0); // White wool
                    
                    Bukkit.getLogger().info("[BedWarsNPCFill] Placed bridge block for NPC " + npc.getName());
                }
            }
            
            // Optional: Check ahead in the direction the NPC is moving to pre-place blocks
            tryPreemptiveBridging(npc, npcLoc);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error in bridging check for NPC " + npc.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Try to place blocks ahead of the NPC to create a smoother bridge
     */
    private static void tryPreemptiveBridging(NPC npc, Location npcLoc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;
            
            // Get the direction the NPC is facing/moving
            float yaw = npcLoc.getYaw();
            double radianYaw = Math.toRadians(yaw);
            
            // Calculate positions 2 and 3 blocks ahead
            double xOffset2 = -Math.sin(radianYaw) * 2;
            double zOffset2 = Math.cos(radianYaw) * 2;
            double xOffset3 = -Math.sin(radianYaw) * 3;
            double zOffset3 = Math.cos(radianYaw) * 3;
            
            Location aheadLoc2 = npcLoc.clone().add(xOffset2, -1, zOffset2);
            Location aheadLoc3 = npcLoc.clone().add(xOffset3, -1, zOffset3);
            
            Block blockAhead2 = aheadLoc2.getBlock();
            Block blockAhead3 = aheadLoc3.getBlock();
            
            // Place wool if the blocks ahead are air
            if (blockAhead2.getType() == Material.AIR || isReplaceable(blockAhead2.getType())) {
                blockAhead2.setType(Material.WOOL);
                blockAhead2.setData((byte) 0);
            }
            
            if (blockAhead3.getType() == Material.AIR || isReplaceable(blockAhead3.getType())) {
                blockAhead3.setType(Material.WOOL);
                blockAhead3.setData((byte) 0);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error in preemptive bridging for NPC " + npc.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if a block type is replaceable for bridging
     */
    private static boolean isReplaceable(Material material) {
        return material == Material.LONG_GRASS || 
               material == Material.DEAD_BUSH || 
               material == Material.YELLOW_FLOWER ||
               material == Material.RED_ROSE ||
               material == Material.BROWN_MUSHROOM ||
               material == Material.RED_MUSHROOM ||
               material == Material.SNOW ||
               material == Material.WATER ||
               material == Material.STATIONARY_WATER ||
               material == Material.LAVA ||
               material == Material.STATIONARY_LAVA;
    }
}

package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BedWarsRealCommands {
    
    /**
     * Uses the actual BedWars1058 commands that exist on the server
     * Based on the discovery output, we know these commands exist
     */
    public static void useRealBedWarsCommands(String arenaName, Player player) {
        player.sendMessage("§a[BedWarsNPCFill] §eUsing real BedWars1058 commands...");
        
        // The key insight: BedWars1058 doesn't have force start, but we can modify arena settings
        
        // Step 1: Try to check current arena settings
        tryGetArenaInfo(arenaName, player);
        
        // Step 2: Try to enable the arena (in case it's disabled)
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            tryEnableArena(arenaName, player);
        }, 20L);
        
        // Step 3: Try to modify arena configuration if possible
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            tryModifyArenaConfig(arenaName, player);
        }, 40L);
        
        // Step 4: Final attempt - try to join more players or simulate activity
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            trySimulateMorePlayers(arenaName, player);
        }, 60L);
    }
    
    private static void tryGetArenaInfo(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eGetting arena information...");
        
        // Try to get arena info using available commands
        String[] infoCommands = {
            "bw arenaList",  // Check if arena is in the list
            "bw cmds",       // Check available commands
        };
        
        for (String cmd : infoCommands) {
            try {
                player.sendMessage("§e[BedWarsNPCFill] §eTrying: /" + cmd);
                Bukkit.dispatchCommand(player, cmd);
                Thread.sleep(500);
            } catch (Exception e) {
                // Continue
            }
        }
    }
    
    private static void tryEnableArena(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eAttempting to enable arena...");
        
        // Try to enable the arena (it might be disabled)
        String[] enableCommands = {
            "bw enableArena " + arenaName,
            "bw enableArena " + arenaName.toLowerCase(),
            "bw enableArena " + arenaName.toUpperCase(),
        };
        
        for (String cmd : enableCommands) {
            try {
                player.sendMessage("§e[BedWarsNPCFill] §eTrying: /" + cmd);
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                
                if (result) {
                    player.sendMessage("§a[BedWarsNPCFill] §eArena enable command executed: /" + cmd);
                }
                
                Thread.sleep(500);
            } catch (Exception e) {
                // Continue
            }
        }
    }
    
    private static void tryModifyArenaConfig(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eAttempting to modify arena configuration...");
        
        // Since there's no direct "set minimum players" command visible,
        // let's try some configuration approaches
        String[] configCommands = {
            "bw setupArena " + arenaName,  // Might open setup mode
            "bw arenaList 1 set",          // Try to set something to 1
            "bw arenaList 2 set",          // Different values
        };
        
        for (String cmd : configCommands) {
            try {
                player.sendMessage("§e[BedWarsNPCFill] §eTrying config: /" + cmd);
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                
                if (result) {
                    player.sendMessage("§a[BedWarsNPCFill] §eConfig command executed: /" + cmd);
                }
                
                Thread.sleep(1000); // Longer delay for config commands
            } catch (Exception e) {
                // Continue
            }
        }
    }
    
    private static void trySimulateMorePlayers(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eAttempting to simulate additional players...");
        
        // Since we can't force start, let's try to make the arena think there are more players
        try {
            // Try to have the same player join multiple times rapidly
            for (int i = 0; i < 5; i++) {
                String[] joinCommands = {
                    "bw join " + arenaName,
                    "bw join Solo",
                    "bw join solo",
                };
                
                for (String joinCmd : joinCommands) {
                    try {
                        Bukkit.dispatchCommand(player, joinCmd);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        // Continue
                    }
                }
            }
            
            player.sendMessage("§e[BedWarsNPCFill] §eSimulated multiple join attempts");
            
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] §eSimulation failed: " + e.getMessage());
        }
        
        // Final message
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            player.sendMessage("§a[BedWarsNPCFill] §eReal command attempts completed");
            player.sendMessage("§e[BedWarsNPCFill] §eNote: BedWars1058 doesn't have force start - arena needs real players");
            player.sendMessage("§e[BedWarsNPCFill] §eConsider asking server admin to lower minimum players for testing");
        }, 40L);
    }
}

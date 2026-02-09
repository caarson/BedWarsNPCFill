package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DirectArenaManipulator {
    
    /**
     * Alternative approach: Instead of trying force start commands,
     * let's try to manipulate the arena in different ways
     */
    public static void tryDirectApproach(String arenaName, Player triggeringPlayer) {
        triggeringPlayer.sendMessage("§a[BedWarsNPCFill] §eTrying direct arena manipulation approach...");
        
        // Approach 1: Try to execute player commands as if from the player
        tryPlayerCommands(arenaName, triggeringPlayer);
        
        // Approach 2: Try alternative command formats
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            tryAlternativeCommands(arenaName, triggeringPlayer);
        }, 40L);
        
        // Approach 3: Try to create fake joins
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            trySimulatePlayerJoins(arenaName, triggeringPlayer);
        }, 80L);
    }
    
    private static void tryPlayerCommands(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eTrying commands executed as player...");
        
        String[] playerCommands = {
            // Removed "bw leave" - we want to STAY in the arena to start it!
            "bw start " + arenaName,    // Try as player command
            "bw forcestart " + arenaName, // Try as player command
        };
        
        for (String command : playerCommands) {
            try {
                player.sendMessage("§e[BedWarsNPCFill] §eExecuting as player: /" + command);
                boolean result = Bukkit.dispatchCommand(player, command);
                
                if (result) {
                    player.sendMessage("§a[BedWarsNPCFill] §ePlayer command succeeded: /" + command);
                } else {
                    player.sendMessage("§c[BedWarsNPCFill] §ePlayer command failed: /" + command);
                }
                
                Thread.sleep(500);
            } catch (Exception e) {
                player.sendMessage("§c[BedWarsNPCFill] §eError with player command /" + command + ": " + e.getMessage());
            }
        }
    }
    
    private static void tryAlternativeCommands(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eTrying alternative command formats...");
        
        // Try different command variations that might exist
        String[] alternativeCommands = {
            "bw " + arenaName + " start",
            "bw " + arenaName + " forcestart", 
            "bedwars start " + arenaName,
            "bedwars " + arenaName + " start",
            arenaName + " start",
            "arena start " + arenaName,
            "game start " + arenaName,
            "match start " + arenaName
        };
        
        for (String command : alternativeCommands) {
            try {
                player.sendMessage("§e[BedWarsNPCFill] §eTrying: /" + command);
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                
                if (result) {
                    player.sendMessage("§a[BedWarsNPCFill] §eAlternative command worked: /" + command);
                    break; // Stop if we found one that works
                }
                
                Thread.sleep(300);
            } catch (Exception e) {
                // Continue with next command
            }
        }
    }
    
    private static void trySimulatePlayerJoins(String arenaName, Player player) {
        player.sendMessage("§e[BedWarsNPCFill] §eTrying to simulate additional player joins...");
        
        try {
            // Try to make the same player "join" multiple times to trigger game start
            for (int i = 0; i < 7; i++) {
                try {
                    // Try different join commands
                    String[] joinCommands = {
                        "bw join " + arenaName,
                        "bw play " + arenaName,
                        "bedwars join " + arenaName,
                        "join " + arenaName
                    };
                    
                    for (String joinCmd : joinCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), joinCmd);
                        Thread.sleep(100);
                    }
                    
                } catch (Exception e) {
                    // Continue
                }
            }
            
            player.sendMessage("§e[BedWarsNPCFill] §eSimulated multiple join attempts");
            
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] §eSimulation approach failed: " + e.getMessage());
        }
        
        // Final check
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            player.sendMessage("§a[BedWarsNPCFill] §eDirect manipulation attempts completed");
            player.sendMessage("§e[BedWarsNPCFill] §eIf game didn't start, the arena may require multiple real players");
            player.sendMessage("§e[BedWarsNPCFill] §eTry using /bedwarsnpcfill discover to see available commands");
        }, 60L);
    }
}

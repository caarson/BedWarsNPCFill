package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BedWarsArenaForcer {
    
    /**
     * Attempts to force start a BedWars arena by manipulating the player count
     * and arena state through various methods
     */
    public static void forceStartArena(String arenaName, Player triggeringPlayer) {
        Bukkit.getLogger().info("[BedWarsNPCFill] Attempting to force start arena: " + arenaName);
        
        // Try multiple approaches to force start the arena
        
        // Approach 1: Try to use BedWars commands
        tryCommandApproach(arenaName, triggeringPlayer);
        
        // Approach 2: Try to manipulate via reflection (after a delay)
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            tryReflectionApproach(arenaName, triggeringPlayer);
        }, 20L); // 1 second delay
        
        // Approach 3: Try to add fake players (after another delay)
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            tryFakePlayerApproach(arenaName, triggeringPlayer);
        }, 40L); // 2 second delay
    }
    
    private static void tryCommandApproach(String arenaName, Player triggeringPlayer) {
        try {
            // Use the ACTUAL BedWars1058 commands that exist
            String[] possibleCommands = {
                "bw arena setMinPlayers " + arenaName + " 1",  // Set minimum players to 1 (correct syntax)
                "bw arena setMaxPlayers " + arenaName + " 8",  // Ensure max players allows game
                "bw arena enable " + arenaName,               // Enable the arena
                "bw list",                                    // List arenas to see status
            };
            
            for (String command : possibleCommands) {
                try {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Trying command: /" + command);
                    triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eTrying: /" + command);
                    
                    // Execute command as console (more permissions)
                    boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    
                    if (result) {
                        triggeringPlayer.sendMessage("§a[BedWarsNPCFill] §eCommand executed successfully: /" + command);
                    } else {
                        triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eCommand failed: /" + command);
                    }
                    
                    // Add delay between commands
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Command failed: " + command + " - " + e.getMessage());
                    triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eError with /" + command + ": " + e.getMessage());
                }
            }
            
            // After configuring arena, try to use BedWars API to force start
            Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
                try {
                    triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eNow trying to force start via API...");
                    boolean apiResult = tryBedWarsAPIStart(arenaName, triggeringPlayer);
                    if (apiResult) {
                        triggeringPlayer.sendMessage("§a[BedWarsNPCFill] §eAPI force start successful!");
                    } else {
                        triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eAPI force start failed");
                    }
                } catch (Exception e) {
                    triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eAPI error: " + e.getMessage());
                }
            }, 60L); // 3 seconds delay
            
            // Check if arena state changed
            Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
                checkArenaStatus(arenaName, triggeringPlayer, "Command approach");
            }, 100L); // 5 seconds delay
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Command approach failed: " + e.getMessage());
            triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eCommand approach failed: " + e.getMessage());
        }
    }
    
    /**
     * Try to use BedWars1058 API to force start the arena
     */
    private static boolean tryBedWarsAPIStart(String arenaName, Player triggeringPlayer) {
        try {
            // Get BedWars API
            Class<?> bedWarsClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
            Object bedWarsAPI = Bukkit.getServicesManager().getRegistration(bedWarsClass).getProvider();
            
            if (bedWarsAPI == null) {
                triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eBedWars API not available");
                return false;
            }
            
            // Get arena util
            Object arenaUtil = bedWarsAPI.getClass().getMethod("getArenaUtil").invoke(bedWarsAPI);
            Object arena = arenaUtil.getClass().getMethod("getArenaByName", String.class).invoke(arenaUtil, arenaName);
            
            if (arena == null) {
                triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eArena not found: " + arenaName);
                return false;
            }
            
            // Get current status
            Object status = arena.getClass().getMethod("getStatus").invoke(arena);
            triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eCurrent arena status: " + status);
            
            // Try to set status to STARTING to trigger countdown
            try {
                // Get the enum class for GameState
                Class<?> gameStateClass = Class.forName("com.andrei1058.bedwars.api.arena.GameState");
                Object startingState = null;
                
                // Find the STARTING enum value
                for (Object enumConstant : gameStateClass.getEnumConstants()) {
                    if (enumConstant.toString().equals("STARTING")) {
                        startingState = enumConstant;
                        break;
                    }
                }
                
                if (startingState != null) {
                    // Try to set the arena status to STARTING
                    arena.getClass().getMethod("setStatus", gameStateClass).invoke(arena, startingState);
                    triggeringPlayer.sendMessage("§a[BedWarsNPCFill] §eSet arena status to STARTING!");
                    
                    // Also try to set the countdown timer
                    try {
                        arena.getClass().getMethod("setStartCountdown", int.class).invoke(arena, 10);
                        triggeringPlayer.sendMessage("§a[BedWarsNPCFill] §eSet countdown to 10 seconds!");
                    } catch (Exception e) {
                        // Method might not exist, continue
                    }
                    
                    return true;
                } else {
                    triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eCould not find STARTING state");
                }
            } catch (Exception e) {
                triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eError setting arena status: " + e.getMessage());
            }
            
            return false;
            
        } catch (Exception e) {
            triggeringPlayer.sendMessage("§c[BedWarsNPCFill] §eBedWars API error: " + e.getMessage());
            Bukkit.getLogger().warning("[BedWarsNPCFill] BedWars API error: " + e.getMessage());
            return false;
        }
    }
    
    private static void tryReflectionApproach(String arenaName, Player triggeringPlayer) {
        triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eSkipping reflection approach due to access restrictions");
        triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eFocusing on command-based solutions instead");
        
        // Skip reflection entirely since it's causing "public" modifier access issues
        Bukkit.getLogger().info("[BedWarsNPCFill] Skipping reflection approach - access restrictions detected");
    }
    
    private static void tryFakePlayerApproach(String arenaName, Player triggeringPlayer) {
        triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eSkipping fake player approach - focusing on configuration changes");
        
        // Instead of trying to add fake players, let's try to change arena configuration
        try {
            // Try a different set of configuration commands
            String[] configCommands = {
                "bw arena " + arenaName + " setminplayers 1",
                "bw arena " + arenaName + " setmaxplayers 8", 
                "bw config " + arenaName + " min-players 1"
            };
            
            for (String command : configCommands) {
                try {
                    triggeringPlayer.sendMessage("§e[BedWarsNPCFill] §eTrying config: /" + command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    Thread.sleep(300);
                } catch (Exception e) {
                    // Continue with next command
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Configuration approach failed: " + e.getMessage());
        }
        
        // Final status check
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            checkArenaStatus(arenaName, triggeringPlayer, "Configuration approach");
        }, 80L);
    }
    
    private static void checkArenaStatus(String arenaName, Player player, String method) {
        try {
            Class<?> bedWarsClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
            Object bedWarsAPI = Bukkit.getServicesManager().getRegistration(bedWarsClass).getProvider();
            
            if (bedWarsAPI != null) {
                Object arenaUtil = bedWarsAPI.getClass().getMethod("getArenaUtil").invoke(bedWarsAPI);
                Object arena = arenaUtil.getClass().getMethod("getArenaByName", String.class).invoke(arenaUtil, arenaName);
                
                if (arena != null) {
                    Object status = arena.getClass().getMethod("getStatus").invoke(arena);
                    @SuppressWarnings("unchecked")
                    java.util.List<Player> players = (java.util.List<Player>) arena.getClass().getMethod("getPlayers").invoke(arena);
                    int minPlayers = (Integer) arena.getClass().getMethod("getMinPlayers").invoke(arena);
                    
                    player.sendMessage("§a[BedWarsNPCFill] §eStatus after " + method + ":");
                    player.sendMessage("§e  Arena: " + arenaName);
                    player.sendMessage("§e  Status: " + status);
                    player.sendMessage("§e  Players: " + players.size() + "/" + minPlayers + " minimum");
                    
                    Bukkit.getLogger().info("[BedWarsNPCFill] Arena " + arenaName + " status: " + status + ", players: " + players.size());
                }
            }
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] §eCould not check arena status: " + e.getMessage());
        }
    }
}

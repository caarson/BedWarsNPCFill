package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * DirectBedWarsAPI - ULTIMATE FIX v3.0.0 - AGGRESSIVE EDITION
 * 
 * Enhanced with maximum debugging and multiple approaches to force game start
 */
public class DirectBedWarsAPI {
    
    private static final String BEDWARS_PLUGIN_NAME = "BedWars1058";
    private static boolean initialized = false;
    private static Plugin bedwarsPlugin = null;
    
    /**
     * Initialize connection to BedWars1058 plugin
     */
    public static boolean initialize() {
        if (initialized) return true;
        
        try {
            bedwarsPlugin = Bukkit.getPluginManager().getPlugin(BEDWARS_PLUGIN_NAME);
            if (bedwarsPlugin == null) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] BedWars1058 plugin not found!");
                return false;
            }
            
            initialized = true;
            Bukkit.getLogger().info("[BedWarsNPCFill] DirectBedWarsAPI initialized - ULTIMATE FIX ready!");
            return true;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to initialize DirectBedWarsAPI: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * AGGRESSIVE ULTIMATE FIX - Try everything possible to start the arena
     */
    public static void attemptArenaStartAggressive(String arenaName, Player player) {
        if (!initialize()) return;
        
        try {
            Bukkit.getLogger().info("[BedWarsNPCFill] AGGRESSIVE ULTIMATE FIX: Starting for " + arenaName);
            
            if (player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §l⚡ AGGRESSIVE MODE ACTIVATED ⚡");
                player.sendMessage("§e[BedWarsNPCFill] §eTrying EVERY possible method to start the game...");
            }
            
            // Method 1: Direct arena configuration file modification
            modifyArenaConfigForSoloPlay(arenaName, player);
            
            // Method 2: Command-based manipulation (try more commands)
            executeArenaCommandsAggressive(arenaName, player);
            
            // Method 3: Try to find and modify ALL arena configs
            modifyAllArenasAggressive(player);
            
            // Method 4: Force reload multiple times
            forceReloadBedWars(player);
            
            // Method 5: Try alternative arena naming patterns
            tryAlternativeArenaNames(arenaName, player);
            
            if (player != null) {
                player.sendMessage("§a[BedWarsNPCFill] §eAGGRESSIVE FIX complete! Check if game starts now...");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] AGGRESSIVE FIX failed: " + e.getMessage());
            if (player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §eAGGRESSIVE FIX error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Try to modify arena config files with maximum debugging
     */
    private static void modifyArenaConfigForSoloPlay(String arenaName, Player player) {
        try {
            File bedwarsDataFolder = bedwarsPlugin.getDataFolder();
            File arenasFolder = new File(bedwarsDataFolder, "Arenas");
            
            if (player != null) {
                player.sendMessage("§e[BedWarsNPCFill] §7Looking for arena configs in: " + arenasFolder.getAbsolutePath());
            }
            
            if (!arenasFolder.exists()) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Arenas folder not found: " + arenasFolder.getAbsolutePath());
                if (player != null) {
                    player.sendMessage("§c[BedWarsNPCFill] §eArenas folder not found!");
                    player.sendMessage("§7Expected: " + arenasFolder.getAbsolutePath());
                }
                return;
            }
            
            // List all files in the arenas folder for debugging
            File[] allFiles = arenasFolder.listFiles();
            if (allFiles != null) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Found " + allFiles.length + " files in Arenas folder:");
                if (player != null) {
                    player.sendMessage("§e[BedWarsNPCFill] §7Found " + allFiles.length + " arena files:");
                }
                
                for (File file : allFiles) {
                    Bukkit.getLogger().info("[BedWarsNPCFill] - " + file.getName());
                    if (player != null && allFiles.length < 10) { // Don't spam if too many files
                        player.sendMessage("§7- " + file.getName());
                    }
                }
            }
            
            // Try multiple naming patterns for the arena
            String[] possibleNames = {
                arenaName + ".yml",
                arenaName.toLowerCase() + ".yml",
                arenaName.toUpperCase() + ".yml",
                arenaName + "_config.yml",
                arenaName + "-config.yml"
            };
            
            boolean foundAny = false;
            for (String possibleName : possibleNames) {
                File arenaFile = new File(arenasFolder, possibleName);
                if (arenaFile.exists()) {
                    foundAny = true;
                    if (player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Found: " + possibleName);
                    }
                    
                    // Try to modify this file
                    boolean modified = modifySpecificArenaFile(arenaFile, player);
                    if (modified && player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Modified: " + possibleName);
                    }
                }
            }
            
            if (!foundAny && player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §eNo arena config found for: " + arenaName);
                player.sendMessage("§7Try checking §6/bw arenas §7to see available arena names");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error in config modification: " + e.getMessage());
            if (player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §eConfig error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Modify a specific arena file
     */
    private static boolean modifySpecificArenaFile(File arenaFile, Player player) {
        try {
            // Read file content
            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(arenaFile)) {
                int ch;
                while ((ch = reader.read()) != -1) {
                    content.append((char) ch);
                }
            }
            
            String originalContent = content.toString();
            String modifiedContent = originalContent;
            boolean wasModified = false;
            
            // Show some of the file content for debugging
            if (player != null) {
                String[] lines = originalContent.split("\n");
                player.sendMessage("§e[BedWarsNPCFill] §7File content preview:");
                for (int i = 0; i < Math.min(5, lines.length); i++) {
                    player.sendMessage("§7  " + lines[i]);
                }
                if (lines.length > 5) {
                    player.sendMessage("§7  ... (" + (lines.length - 5) + " more lines)");
                }
            }
            
            // Modify minPlayers
            if (modifiedContent.contains("minPlayers:")) {
                String before = modifiedContent;
                modifiedContent = modifiedContent.replaceAll("minPlayers:\\s*\\d+", "minPlayers: 1");
                if (!before.equals(modifiedContent)) {
                    wasModified = true;
                    Bukkit.getLogger().info("[BedWarsNPCFill] Modified minPlayers to 1 in " + arenaFile.getName());
                    if (player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Set minPlayers: 1");
                    }
                }
            } else {
                // Add minPlayers if not present
                modifiedContent += "\nminPlayers: 1\n";
                wasModified = true;
                Bukkit.getLogger().info("[BedWarsNPCFill] Added minPlayers: 1 to " + arenaFile.getName());
                if (player != null) {
                    player.sendMessage("§a[BedWarsNPCFill] §7+ Added minPlayers: 1");
                }
            }
            
            // Also ensure maxInTeam is set for solo
            if (modifiedContent.contains("maxInTeam:")) {
                String before = modifiedContent;
                modifiedContent = modifiedContent.replaceAll("maxInTeam:\\s*\\d+", "maxInTeam: 1");
                if (!before.equals(modifiedContent)) {
                    wasModified = true;
                    if (player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Set maxInTeam: 1");
                    }
                }
            }
            
            // Write back if anything was changed
            if (wasModified) {
                try (FileWriter writer = new FileWriter(arenaFile)) {
                    writer.write(modifiedContent);
                }
                Bukkit.getLogger().info("[BedWarsNPCFill] Successfully wrote modified config to " + arenaFile.getName());
                return true;
            } else {
                if (player != null) {
                    player.sendMessage("§e[BedWarsNPCFill] §7File already had correct settings");
                }
                return false;
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to modify " + arenaFile.getName() + ": " + e.getMessage());
            if (player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §7✗ Error modifying " + arenaFile.getName());
            }
            return false;
        }
    }
    
    /**
     * Execute MANY arena commands aggressively
     */
    private static void executeArenaCommandsAggressive(String arenaName, Player player) {
        if (player != null) {
            player.sendMessage("§e[BedWarsNPCFill] §7Executing aggressive command sequence...");
        }
        
        // Try many different command formats and variations
        String[] commands = {
            // Basic arena modification commands
            "bw arena setMinPlayers " + arenaName + " 1",
            "bw arena setMaxPlayers " + arenaName + " 8",
            "bw arena enable " + arenaName,
            "bedwars arena setMinPlayers " + arenaName + " 1",
            "bedwars arena enable " + arenaName,
            
            // Force start commands
            "bw forcestart " + arenaName,
            "bw start " + arenaName,
            "bedwars forcestart " + arenaName,
            "bedwars start " + arenaName,
            
            // Alternative formats
            "bw " + arenaName + " setMinPlayers 1",
            "bw " + arenaName + " start",
            "bw " + arenaName + " forcestart",
            
            // Generic force commands
            "bw forcestart",
            "bw start",
            "bedwars forcestart",
            "bedwars start"
        };
        
        int successCount = 0;
        for (String cmd : commands) {
            try {
                if (player != null) {
                    player.sendMessage("§7[BedWarsNPCFill] Trying: /" + cmd);
                }
                
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                if (result) {
                    successCount++;
                    Bukkit.getLogger().info("[BedWarsNPCFill] Command successful: /" + cmd);
                    if (player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Success: /" + cmd);
                    }
                } else {
                    if (player != null) {
                        player.sendMessage("§c[BedWarsNPCFill] §7✗ Failed: /" + cmd);
                    }
                }
                
                // Small delay between commands
                Thread.sleep(200);
                
            } catch (Exception e) {
                if (player != null) {
                    player.sendMessage("§c[BedWarsNPCFill] §7✗ Error: /" + cmd + " - " + e.getMessage());
                }
            }
        }
        
        if (player != null) {
            player.sendMessage("§e[BedWarsNPCFill] §7Command results: " + successCount + "/" + commands.length + " successful");
        }
    }
    
    /**
     * Try to modify ALL arena files
     */
    private static void modifyAllArenasAggressive(Player player) {
        try {
            File bedwarsDataFolder = bedwarsPlugin.getDataFolder();
            File arenasFolder = new File(bedwarsDataFolder, "Arenas");
            
            if (!arenasFolder.exists()) return;
            
            File[] arenaFiles = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (arenaFiles == null || arenaFiles.length == 0) {
                if (player != null) {
                    player.sendMessage("§c[BedWarsNPCFill] §7No .yml files found in arenas folder");
                }
                return;
            }
            
            if (player != null) {
                player.sendMessage("§e[BedWarsNPCFill] §7Modifying ALL " + arenaFiles.length + " arena configs...");
            }
            
            int modifiedCount = 0;
            for (File arenaFile : arenaFiles) {
                try {
                    boolean modified = modifySpecificArenaFile(arenaFile, null); // Don't spam player with all files
                    if (modified) {
                        modifiedCount++;
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to modify " + arenaFile.getName());
                }
            }
            
            if (player != null) {
                player.sendMessage("§a[BedWarsNPCFill] §7Modified " + modifiedCount + "/" + arenaFiles.length + " arena configs");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error in modifyAllArenasAggressive: " + e.getMessage());
        }
    }
    
    /**
     * Force reload BedWars multiple times
     */
    private static void forceReloadBedWars(Player player) {
        if (player != null) {
            player.sendMessage("§e[BedWarsNPCFill] §7Force reloading BedWars configuration...");
        }
        
        String[] reloadCommands = {
            "bw reload",
            "bedwars reload",
            "bw reloadConfig",
            "bedwars reloadConfig",
            "bw arena reload",
            "bedwars arena reload"
        };
        
        for (String cmd : reloadCommands) {
            try {
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                if (result) {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Reload successful: /" + cmd);
                    if (player != null) {
                        player.sendMessage("§a[BedWarsNPCFill] §7✓ Reloaded with: /" + cmd);
                    }
                    Thread.sleep(1000); // Wait 1 second after successful reload
                    break;
                }
            } catch (Exception e) {
                // Continue
            }
        }
    }
    
    /**
     * Try alternative arena names
     */
    private static void tryAlternativeArenaNames(String arenaName, Player player) {
        if (player != null) {
            player.sendMessage("§e[BedWarsNPCFill] §7Trying alternative arena name patterns...");
        }
        
        // Generate alternative names
        String[] alternatives = {
            arenaName.toLowerCase(),
            arenaName.toUpperCase(),
            arenaName.replace("1v1", ""),
            arenaName.replace("solo", ""),
            arenaName + "1v1",
            arenaName + "solo",
            "solo_" + arenaName,
            "1v1_" + arenaName
        };
        
        for (String altName : alternatives) {
            if (!altName.equals(arenaName)) {
                if (player != null) {
                    player.sendMessage("§7[BedWarsNPCFill] Trying alternative: " + altName);
                }
                modifyArenaConfigForSoloPlay(altName, null); // Don't spam output
                executeArenaCommandsAggressive(altName, null);
            }
        }
    }
    
    /**
     * Attempt to start a specific arena (called by ArenaScheduler)
     * @param arenaName The name of the arena to start
     * @param player The player who triggered the start
     */
    public static void attemptArenaStart(String arenaName, Player player) {
        if (!initialize()) return;
        
        try {
            // Use the existing aggressive method to start the arena
            Bukkit.getLogger().info("[BedWarsNPCFill] Starting arena via DirectBedWarsAPI: " + arenaName);
            if (player != null) {
                player.sendMessage("§a[BedWarsNPCFill] §eStarting arena using BedWars1058 API...");
            }
            
            // Execute the start command using the player (who likely has permissions)
            boolean result;
            if (player != null) {
                result = Bukkit.dispatchCommand(player, "bw start " + arenaName);
            } else {
                result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bw start " + arenaName);
            }
            
            if (result) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Arena start command successful for: " + arenaName);
                if (player != null) {
                    player.sendMessage("§a[BedWarsNPCFill] §eArena started successfully!");
                }
            } else {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Arena start command failed for: " + arenaName);
                if (player != null) {
                    player.sendMessage("§c[BedWarsNPCFill] §eFailed to start arena! Trying fallback...");
                }
                // Fallback to the aggressive method
                attemptArenaStartAggressive(arenaName, player);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error starting arena: " + e.getMessage());
            if (player != null) {
                player.sendMessage("§c[BedWarsNPCFill] §eError: " + e.getMessage());
            }
        }
    }

    /**
     * Check if BedWars1058 plugin is available and active
     */
    public static boolean isBedWarsAvailable() {
        return initialize() && bedwarsPlugin != null && bedwarsPlugin.isEnabled();
    }
    
    /**
     * Modify ALL arenas (simple version for compatibility)
     */
    public static void modifyAllArenas() {
        modifyAllArenasAggressive(null);
    }
}

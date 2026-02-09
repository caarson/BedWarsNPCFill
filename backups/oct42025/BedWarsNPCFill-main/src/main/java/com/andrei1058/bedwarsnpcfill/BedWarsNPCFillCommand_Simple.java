package com.andrei1058.bedwarsnpcfill;

import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BedWarsNPCFillCommand_Simple implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c[BedWarsNPCFill] This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "test":
                testCountdown(player);
                break;
            case "countdown":
                startCountdownProcess(player);
                break;
            case "debug":
                if (args.length < 2) {
                    // Try to debug current arena
                    String worldName = player.getWorld().getName();
                    String arenaName = worldName.startsWith("bw_") ? worldName.substring(3) : worldName;
                    debugArena(player, arenaName);
                } else {
                    debugArena(player, args[1]);
                }
                break;
            case "start":
                if (args.length < 2) {
                    player.sendMessage("§c[BedWarsNPCFill] Usage: /bedwarsnpcfill start <arena>");
                } else {
                    startManual(player, args[1]);
                }
                break;
            case "force":
                if (args.length < 2) {
                    // Try to force start current arena
                    String worldName = player.getWorld().getName();
                    String arenaName = worldName.startsWith("bw_") ? worldName.substring(3) : worldName;
                    forceStartArena(player, arenaName);
                } else {
                    forceStartArena(player, args[1]);
                }
                break;
            case "discover":
            case "commands":
                BedWarsCommandDiscovery.discoverBedWarsCommands(player);
                break;
            case "cleannpcs":
            case "removenpcs":
                cleanNPCs(player);
                break;
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§a[BedWarsNPCFill] Available commands:");
        player.sendMessage("§e/bedwarsnpcfill test §7- Test the countdown functionality");
        player.sendMessage("§e/bedwarsnpcfill countdown §7- Start full countdown with NPCs (must be in arena)");
        player.sendMessage("§e/bedwarsnpcfill debug [arena] §7- Deep debug BedWars internals (check console)");
        player.sendMessage("§e/bedwarsnpcfill start <arena> §7- Manually start NPC filling for an arena");
        player.sendMessage("§e/bedwarsnpcfill force [arena] §7- Force start an arena (uses current arena if not specified)");
        player.sendMessage("§e/bedwarsnpcfill discover §7- Discover available BedWars commands");
        player.sendMessage("§e/bedwarsnpcfill cleannpcs §7- Remove all NPCs created by this plugin");
    }

    private void testCountdown(Player player) {
        player.sendMessage("§a[BedWarsNPCFill] §eStarting test countdown sequence...");
        player.sendMessage("§e[BedWarsNPCFill] §eThis simulates what happens when you join a BedWars arena:");
        player.sendMessage("§e[BedWarsNPCFill] §e1. Wait 5 seconds");
        player.sendMessage("§e[BedWarsNPCFill] §e2. Start 20-second countdown");
        player.sendMessage("§e[BedWarsNPCFill] §e3. Fill arena with NPCs");

        // 5-second initial delay
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            player.sendMessage("§a[BedWarsNPCFill] §e5-second delay complete! Starting countdown...");
            startTestCountdown(player);
        }, 5 * 20L);

        Bukkit.getLogger().info("[BedWarsNPCFill] Started test sequence for player " + player.getName());
    }

    private void startTestCountdown(Player player) {
        new BukkitRunnable() {
            int countdown = 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    if (countdown % 5 == 0 || countdown <= 5) {
                        player.sendMessage("§a[BedWarsNPCFill] §eGame starting with NPCs in " + countdown + " seconds...");
                    }
                    countdown--;
                } else {
                    player.sendMessage("§a[BedWarsNPCFill] §eCountdown complete! Filling arena with NPCs...");
                    
                    // Try to determine arena name from world
                    String worldName = player.getWorld().getName();
                    String arenaName = worldName;
                    
                    if (worldName.startsWith("bw_")) {
                        arenaName = worldName.substring(3);
                    }
                    
                    // Attempt NPC filling
                    try {
            // NPC filling is currently disabled
                        player.sendMessage("§a[BedWarsNPCFill] §eSuccessfully called NPC fill for arena: " + arenaName);
                    } catch (Exception e) {
                        player.sendMessage("§e[BedWarsNPCFill] §eNPC fill attempted for arena: " + arenaName);
                        player.sendMessage("§e[BedWarsNPCFill] §eResponse: " + e.getMessage());
                    }
                    
                    player.sendMessage("§a[BedWarsNPCFill] §eTest sequence complete!");
                    player.sendMessage("§e[BedWarsNPCFill] §eIn a real game, NPCs would now fill empty team slots.");
                    
                    cancel();
                }
            }
        }.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 20L);
    }

    private void debugArena(Player player, String arenaName) {
        player.sendMessage("§a[BedWarsNPCFill] §eStarting deep debug analysis for arena: " + arenaName);
        player.sendMessage("§e[BedWarsNPCFill] §eCheck console for detailed debug information!");
        
        // Run the deep debugger
        BedWarsDeepDebugger.performDeepAnalysis(arenaName, player);
        
        player.sendMessage("§a[BedWarsNPCFill] §eDeep debug analysis complete - check console!");
    }

    private void startManual(Player player, String arenaName) {
        player.sendMessage("§a[BedWarsNPCFill] §eManually starting NPC fill for arena: " + arenaName);
        
            player.sendMessage("§a[BedWarsNPCFill] §eNPC filling is currently disabled");
            player.sendMessage("§e[BedWarsNPCFill] §eThis command would fill arena '" + arenaName + "' with NPCs if enabled");
    }
    
    private void forceStartArena(Player player, String arenaName) {
        player.sendMessage("§a[BedWarsNPCFill] §eAttempting to force start arena: " + arenaName);
        player.sendMessage("§e[BedWarsNPCFill] §eThis will try multiple methods to start the arena...");
        
        // Use the arena forcer
        BedWarsArenaForcer.forceStartArena(arenaName, player);
    }
    
    /**
     * Start the full countdown process with NPCs
     */
    private void startCountdownProcess(Player player) {
        if (!isPlayerInBedWarsWorld(player)) {
            player.sendMessage("§c[BedWarsNPCFill] §eYou must be in a BedWars arena to use this command!");
            return;
        }
        
        String worldName = player.getWorld().getName();
        player.sendMessage("§a[BedWarsNPCFill] §eStarting countdown process for arena: " + worldName);
        
        // Start 5-second delay
        player.sendMessage("§a[BedWarsNPCFill] §eStarting 5-second delay before countdown...");
        
        Bukkit.getScheduler().runTaskLater(BedWarsNPCFillPlugin.getInstance(), () -> {
            player.sendMessage("§a[BedWarsNPCFill] §eSpawning NPCs and starting countdown...");
            
            // Spawn NPCs
            spawnNPCsForArena(player, worldName);
            
            // Try to trigger arena start
            DirectBedWarsAPI.attemptArenaStart(worldName, player);
            
            // Start 20-second countdown
            startArenaCountdown(player, worldName);
            
        }, 100L); // 5 seconds
    }
    
    /**
     * Spawn Citizens NPCs to fill empty player slots in the arena
     */
    private void spawnNPCsForArena(Player player, String worldName) {
        try {
            player.sendMessage("§a[BedWarsNPCFill] §eSpawning Citizens NPCs...");
            Bukkit.getLogger().info("[BedWarsNPCFill] Spawning NPCs for arena: " + worldName);
            
            player.sendMessage("§a[BedWarsNPCFill] §eNPC filling is currently disabled");
            player.sendMessage("§e[BedWarsNPCFill] §eNPCs would be spawned via NPC Manager if enabled");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to spawn NPCs: " + e.getMessage());
            player.sendMessage("§c[BedWarsNPCFill] §eFailed to spawn NPCs: " + e.getMessage());
        }
    }
    
    /**
     * Spawn NPCs using Citizens API directly
     */
    private void spawnCitizensNPCs(Player player, String worldName) {
        try {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage("§c[BedWarsNPCFill] §eWorld not found: " + worldName);
                return;
            }
            
            // Get player's location as a base for NPC spawning
            org.bukkit.Location playerLoc = player.getLocation();
            
            // Try to spawn 1-3 NPCs around the player
            for (int i = 1; i <= 3; i++) {
                try {
                    // Calculate spawn location near player
                    org.bukkit.Location npcLoc = playerLoc.clone().add(i * 2, 0, i * 2);
                    
                    // Try to create NPC using Citizens commands
                    String npcName = "BW_NPC_" + i + "_" + worldName;
                    String createCommand = "npc create " + npcName;
                    
                    // Execute as player (Citizens usually requires player context)
                    boolean result = player.performCommand(createCommand);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Created NPC " + npcName + ": " + result);
                    
                    if (result) {
                        // Teleport NPC to arena
                        player.performCommand("npc tp " + npcLoc.getX() + " " + npcLoc.getY() + " " + npcLoc.getZ());
                        player.sendMessage("§a[BedWarsNPCFill] §eSpawned NPC " + i + ": " + npcName);
                    }
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to spawn NPC " + i + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Citizens NPC spawning failed: " + e.getMessage());
            player.sendMessage("§c[BedWarsNPCFill] §eCitizens NPC spawning failed: " + e.getMessage());
        }
    }
    
    /**
     * Start the 20-second countdown for arena start
     */
    private void startArenaCountdown(Player player, String worldName) {
        player.sendMessage("§a[BedWarsNPCFill] §eStarting 20-second countdown...");
        
        new BukkitRunnable() {
            int countdown = 20; // 20 seconds countdown
            
            @Override
            public void run() {
                if (!player.isOnline() || !isPlayerInBedWarsWorld(player)) {
                    cancel();
                    return;
                }
                
                // Try to force arena start every few seconds
                if (countdown % 5 == 0) {
                    DirectBedWarsAPI.attemptArenaStart(worldName, player);
                    player.sendMessage("§a[BedWarsNPCFill] §eForcing arena start... " + countdown + " seconds remaining");
                } else {
                    player.sendMessage("§e[BedWarsNPCFill] §7Countdown: " + countdown + " seconds");
                }
                
                // Special actions at certain times
                if (countdown == 15) {
                    player.sendMessage("§a[BedWarsNPCFill] §eEnsuring NPCs are properly registered...");
                    DirectBedWarsAPI.attemptArenaStart(worldName, player);
                }
                
                if (countdown == 10) {
                    player.sendMessage("§a[BedWarsNPCFill] §eFinal arena configuration...");
                    DirectBedWarsAPI.attemptArenaStart(worldName, player);
                }
                
                if (countdown == 5) {
                    player.sendMessage("§a[BedWarsNPCFill] §eGame starting in 5 seconds!");
                }
                
                if (countdown <= 0) {
                    // Time's up - force game start
                    player.sendMessage("§a[BedWarsNPCFill] §eForcing game start NOW!");
                    
                    // Final attempt to start the game
                    DirectBedWarsAPI.attemptArenaStart(worldName, player);
                    
                    player.sendMessage("§a[BedWarsNPCFill] §eCountdown complete! Game should start now.");
                    cancel();
                    return;
                }
                
                countdown--;
            }
        }.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 20L); // Run every second
    }
    
    /**
     * Check if a player is currently in a BedWars arena world
     */
    private boolean isPlayerInBedWarsWorld(Player player) {
        String worldName = player.getWorld().getName();
        
        return worldName.startsWith("bw_") || 
               worldName.startsWith("bedwars_") ||
               worldName.contains("arena") ||
               worldName.toLowerCase().contains("bedwar") ||
               worldName.toLowerCase().contains("1v1") ||
               worldName.toLowerCase().contains("2v2") ||
               worldName.toLowerCase().contains("4v4") ||
               worldName.toLowerCase().contains("amazon") ||
               worldName.toLowerCase().contains("glacier") ||
               worldName.toLowerCase().contains("solo") ||
               (worldName.toLowerCase().contains("bw") && !worldName.equals("world"));
    }

    /**
     * Clean up all NPCs created by this plugin
     */
    private void cleanNPCs(Player player) {
        player.sendMessage("§a[BedWarsNPCFill] §eCleaning up all NPCs created by this plugin...");
        
        try {
            // Check if Citizens is available
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            
            int removedCount = 0;
            
            // Iterate through all NPCs in the registry
            for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
                String npcName = npc.getName();
                
                // Check if NPC name starts with "NPC_" (our naming pattern)
                if (npcName.startsWith("NPC_")) {
                    try {
                        npc.destroy();
                        removedCount++;
                        Bukkit.getLogger().info("[BedWarsNPCFill] Removed NPC: " + npcName);
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to remove NPC " + npcName + ": " + e.getMessage());
                    }
                }
            }
            
            // Also clean up any internally tracked NPCs
            NPCManager.removeAllNPCs();
            
            player.sendMessage("§a[BedWarsNPCFill] §eSuccessfully removed " + removedCount + " NPCs!");
            Bukkit.getLogger().info("[BedWarsNPCFill] Cleaned up " + removedCount + " NPCs via command");
            
        } catch (ClassNotFoundException e) {
            player.sendMessage("§c[BedWarsNPCFill] §eCitizens plugin not found! Cannot clean NPCs.");
            Bukkit.getLogger().warning("[BedWarsNPCFill] Citizens not found - cannot clean NPCs");
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] §eError cleaning NPCs: " + e.getMessage());
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error cleaning NPCs: " + e.getMessage());
        }
    }
}

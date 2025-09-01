package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * GameStarter - Properly starts BedWars1058 games using direct API calls
 * instead of relying on commands
 */
public class GameStarter {
    
    private static final int START_DELAY = 40; // 40 seconds delay before starting
    
    /**
     * Start a game in the specified arena after a delay
     */
    public static void startGameWithDelay(String arenaName, Player player) {
        if (!DirectBedWarsAPI.isBedWarsAvailable()) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] BedWars1058 not available for game start");
            if (player != null) {
                player.sendMessage("§cBedWars1058 plugin not found or not enabled!");
            }
            return;
        }
        
        IArena arena = getArenaByName(arenaName);
        if (arena == null) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Arena not found: " + arenaName);
            if (player != null) {
                player.sendMessage("§cArena not found: " + arenaName);
            }
            return;
        }
        
        if (player != null) {
            player.sendMessage("§a[BedWarsNPCFill] §eGame will start in " + START_DELAY + " seconds...");
        }
        
        // Schedule the game start
        new BukkitRunnable() {
            @Override
            public void run() {
                startGameImmediately(arena, player);
            }
        }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), START_DELAY * 20L);
    }
    
    /**
     * Start the game immediately in the specified arena by name
     */
    public static void startGameImmediately(String arenaName, Player player) {
        IArena arena = getArenaByName(arenaName);
        if (arena == null) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Arena not found: " + arenaName);
            if (player != null) {
                player.sendMessage("§cArena not found: " + arenaName);
            }
            return;
        }
        startGameImmediately(arena, player);
    }
    
    /**
     * Start the game immediately in the specified arena
     */
    public static void startGameImmediately(IArena arena, Player player) {
        try {
            if (arena == null) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Cannot start game - arena is null");
                return;
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Attempting to start game in arena: " + arena.getArenaName() + " (current status: " + arena.getStatus() + ")");
            
            // Check if arena is in a state that can be started
            if (arena.getStatus() != GameState.waiting && arena.getStatus() != GameState.starting) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Arena " + arena.getArenaName() + " is not in waiting/starting state: " + arena.getStatus());
                if (player != null) {
                    player.sendMessage("§cArena is not ready to start (status: " + arena.getStatus() + ")");
                }
                return;
            }
            
            // Ensure minimum players condition is met
            if (arena.getPlayers().size() < 1) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Not enough players to start game");
                if (player != null) {
                    player.sendMessage("§cNot enough players to start game");
                }
                return;
            }
            
            // Use direct API call to change status to starting first
            if (arena.getStatus() == GameState.waiting) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Changing arena status from WAITING to STARTING");
                if (arena instanceof Arena) {
                    ((Arena) arena).changeStatus(GameState.starting);
                } else {
                    // Fallback for IArena implementations
                    try {
                        Method changeStatusMethod = arena.getClass().getMethod("changeStatus", GameState.class);
                        changeStatusMethod.invoke(arena, GameState.starting);
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to change status via reflection: " + e.getMessage());
                    }
                }
            }
            
            // Now force immediate start if still in starting state
            if (arena.getStatus() == GameState.starting) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Arena is in STARTING state, forcing immediate start");
                if (arena instanceof Arena) {
                    forceImmediateStart((Arena) arena);
                } else {
                    // Fallback: try to use the changeStatus method to playing directly
                    try {
                        Method changeStatusMethod = arena.getClass().getMethod("changeStatus", GameState.class);
                        changeStatusMethod.invoke(arena, GameState.playing);
                        Bukkit.getLogger().info("[BedWarsNPCFill] Used reflection to set status to PLAYING");
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to set playing status via reflection: " + e.getMessage());
                    }
                }
            }
            
            // Verify the status changed
            Bukkit.getLogger().info("[BedWarsNPCFill] Final arena status: " + arena.getStatus());
            
            if (player != null) {
                player.sendMessage("§a[BedWarsNPCFill] §eGame started successfully! Status: " + arena.getStatus());
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BedWarsNPCFill] Error starting game", e);
            if (player != null) {
                player.sendMessage("§cError starting game: " + e.getMessage());
            }
        }
    }
    
    /**
     * Force immediate start by directly setting the arena status to playing and teleporting players
     */
    private static void forceImmediateStart(Arena arena) {
        try {
            // First, try to cancel any existing starting task
            Field startingTaskField = Arena.class.getDeclaredField("startingTask");
            startingTaskField.setAccessible(true);
            Object startingTask = startingTaskField.get(arena);
            
            if (startingTask != null) {
                // Cancel the starting task if it's running
                try {
                    Method cancelMethod = startingTask.getClass().getMethod("cancel");
                    cancelMethod.invoke(startingTask);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Cancelled existing starting task");
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Could not cancel starting task: " + e.getMessage());
                }
            }
            
            // Directly change status to playing to force game start
            Bukkit.getLogger().info("[BedWarsNPCFill] Force changing arena status to PLAYING");
            arena.changeStatus(GameState.playing);
            
            // Now teleport all players and NPCs to their team islands
            Bukkit.getLogger().info("[BedWarsNPCFill] Teleporting players and NPCs to their islands");
            teleportPlayersToIslands(arena);
            
            // Start NPC combat behavior with a delay to ensure teleportation is complete
            Bukkit.getLogger().info("[BedWarsNPCFill] Scheduling NPC combat behavior with 3-second delay");
            new BukkitRunnable() {
                @Override
                public void run() {
                    NPCManager.startNPCCombat(arena);
                }
            }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 60L); // 60 ticks = 3 seconds delay
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Could not force immediate start: " + e.getMessage());
            // Fallback: try to use the changeStatus method directly
            try {
                arena.changeStatus(GameState.playing);
                // Attempt teleportation even if other parts failed
                teleportPlayersToIslands(arena);
            } catch (Exception ex) {
                Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to set playing status: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Teleport all players and NPCs to their respective team islands
     */
    private static void teleportPlayersToIslands(Arena arena) {
        try {
            Bukkit.getLogger().info("[BedWarsNPCFill] Starting player teleportation for arena: " + arena.getArenaName());
            Bukkit.getLogger().info("[BedWarsNPCFill] Players in arena: " + arena.getPlayers().size());
            for (Player p : arena.getPlayers()) {
                Bukkit.getLogger().info("[BedWarsNPCFill] - Player: " + p.getName());
            }
            
            // Teleport real players
            for (Player player : arena.getPlayers()) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Processing player: " + player.getName());
                ITeam team = arena.getTeam(player);
                
                if (team != null) {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Player " + player.getName() + " is in team: " + team.getName());
                    
                    // Use the team's firstSpawn method to properly teleport player
                    try {
                        Method firstSpawnMethod = team.getClass().getMethod("firstSpawn", Player.class);
                        firstSpawnMethod.invoke(team, player);
                        Bukkit.getLogger().info("[BedWarsNPCFill] Successfully teleported player " + player.getName() + " to team " + team.getName() + " using firstSpawn");
                        continue; // Success, move to next player
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to use firstSpawn for player " + player.getName() + ": " + e.getMessage());
                    }
                    
                    // Fallback: teleport to team spawn location
                    try {
                        Field spawnLocationField = team.getClass().getDeclaredField("spawn");
                        spawnLocationField.setAccessible(true);
                        Location spawnLoc = (Location) spawnLocationField.get(team);
                        if (spawnLoc != null) {
                            player.teleport(spawnLoc);
                            Bukkit.getLogger().info("[BedWarsNPCFill] Fallback teleport for player " + player.getName() + " to location: " + spawnLoc);
                        } else {
                            Bukkit.getLogger().warning("[BedWarsNPCFill] Team spawn location is null for player " + player.getName());
                            // Try to get spawn from team's getSpawn method
                            try {
                                Method getSpawnMethod = team.getClass().getMethod("getSpawn");
                                Location spawn = (Location) getSpawnMethod.invoke(team);
                                if (spawn != null) {
                                    player.teleport(spawn);
                                    Bukkit.getLogger().info("[BedWarsNPCFill] Teleported player " + player.getName() + " using getSpawn method");
                                }
                            } catch (Exception ex) {
                                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not get spawn via getSpawn method: " + ex.getMessage());
                            }
                        }
                    } catch (Exception ex) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Could not teleport player " + player.getName() + " via spawn field: " + ex.getMessage());
                    }
                } else {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Player " + player.getName() + " is not in any team! Attempting to find team...");
                    
                    // Try to find the player's team by iterating through all teams
                    boolean foundTeam = false;
                    for (ITeam possibleTeam : arena.getTeams()) {
                        if (possibleTeam.getMembers().contains(player)) {
                            Bukkit.getLogger().info("[BedWarsNPCFill] Found player " + player.getName() + " in team " + possibleTeam.getName() + " via members list");
                            foundTeam = true;
                            // Teleport using the team's spawn location
                            try {
                                Field spawnLocationField = possibleTeam.getClass().getDeclaredField("spawn");
                                spawnLocationField.setAccessible(true);
                                Location spawnLoc = (Location) spawnLocationField.get(possibleTeam);
                                if (spawnLoc != null) {
                                    player.teleport(spawnLoc);
                                    Bukkit.getLogger().info("[BedWarsNPCFill] Teleported player " + player.getName() + " to team " + possibleTeam.getName() + " spawn");
                                } else {
                                    // Try getSpawn method
                                    try {
                                        Method getSpawnMethod = possibleTeam.getClass().getMethod("getSpawn");
                                        Location spawn = (Location) getSpawnMethod.invoke(possibleTeam);
                                        if (spawn != null) {
                                            player.teleport(spawn);
                                            Bukkit.getLogger().info("[BedWarsNPCFill] Teleported player " + player.getName() + " using getSpawn method");
                                        }
                                    } catch (Exception ex) {
                                        Bukkit.getLogger().warning("[BedWarsNPCFill] Could not get spawn via getSpawn method: " + ex.getMessage());
                                    }
                                }
                            } catch (Exception ex) {
                                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not teleport player " + player.getName() + " to team " + possibleTeam.getName() + ": " + ex.getMessage());
                            }
                            break;
                        }
                    }
                    
                    if (!foundTeam) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find team for player " + player.getName() + ". Attempting direct teleport to first available team spawn.");
                        // Last resort: teleport to first team's spawn
                        if (!arena.getTeams().isEmpty()) {
                            ITeam firstTeam = arena.getTeams().get(0);
                            try {
                                Method getSpawnMethod = firstTeam.getClass().getMethod("getSpawn");
                                Location spawn = (Location) getSpawnMethod.invoke(firstTeam);
                                if (spawn != null) {
                                    player.teleport(spawn);
                                    Bukkit.getLogger().info("[BedWarsNPCFill] Teleported player " + player.getName() + " to first team's spawn as fallback");
                                }
                            } catch (Exception ex) {
                                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not teleport player to first team: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
            
            // Teleport NPCs using NPCManager
            NPCManager.teleportNPCsToIslands(arena);
            
            // Give BedWars starting inventory to all players
            Bukkit.getLogger().info("[BedWarsNPCFill] Giving BedWars starting inventory to players");
            giveBedWarsStartingInventory(arena);
            
            // Set players to survival mode so they can place blocks
            Bukkit.getLogger().info("[BedWarsNPCFill] Setting players to survival mode");
            setPlayersToSurvivalMode(arena);
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Player teleportation, inventory setup, and game mode change complete");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error teleporting players to islands: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Give BedWars starting inventory to all players in the arena
     */
    private static void giveBedWarsStartingInventory(Arena arena) {
        try {
            Bukkit.getLogger().info("[BedWarsNPCFill] Attempting to give BedWars starting inventory");
            
            for (Player player : arena.getPlayers()) {
                try {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Giving inventory to player: " + player.getName());
                    
                    // Clear player's inventory first
                    player.getInventory().clear();
                    
                    // Try to use BedWars1058's inventory giving mechanism
                    try {
                        // Look for a method in Arena that gives starting items
                        Method giveStartingItemsMethod = Arena.class.getDeclaredMethod("giveStartingItems", Player.class);
                        giveStartingItemsMethod.setAccessible(true);
                        giveStartingItemsMethod.invoke(arena, player);
                        Bukkit.getLogger().info("[BedWarsNPCFill] Successfully gave starting items to " + player.getName() + " via Arena method");
                        continue;
                    } catch (NoSuchMethodException e) {
                        Bukkit.getLogger().info("[BedWarsNPCFill] Arena.giveStartingItems method not found, trying team method");
                    }
                    
                    // Try team-based inventory giving
                    ITeam team = arena.getTeam(player);
                    if (team != null) {
                        try {
                            Method giveStartingItemsMethod = team.getClass().getDeclaredMethod("giveStartingItems", Player.class);
                            giveStartingItemsMethod.setAccessible(true);
                            giveStartingItemsMethod.invoke(team, player);
                            Bukkit.getLogger().info("[BedWarsNPCFill] Successfully gave starting items to " + player.getName() + " via Team method");
                            continue;
                        } catch (NoSuchMethodException e) {
                            Bukkit.getLogger().info("[BedWarsNPCFill] Team.giveStartingItems method not found");
                        }
                    }
                    
                    // Fallback: Give basic BedWars starting items manually
                    giveBasicBedWarsItems(player);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Gave basic BedWars items to " + player.getName() + " as fallback");
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Error giving inventory to " + player.getName() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error giving starting inventory: " + e.getMessage());
        }
    }
    
    /**
     * Give basic BedWars starting items as fallback
     */
    private static void giveBasicBedWarsItems(Player player) {
        try {
            // Give basic BedWars items (wool, pickaxe, sword)
            // This is a fallback if BedWars1058 API methods are not available
            org.bukkit.inventory.ItemStack wool = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOOL, 64);
            org.bukkit.inventory.ItemStack woodPickaxe = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOOD_PICKAXE, 1);
            org.bukkit.inventory.ItemStack woodSword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOOD_SWORD, 1);
            
            player.getInventory().addItem(wool);
            player.getInventory().addItem(woodPickaxe);
            player.getInventory().addItem(woodSword);
            
            // Set armor (leather)
            org.bukkit.inventory.ItemStack leatherHelmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET, 1);
            org.bukkit.inventory.ItemStack leatherChestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE, 1);
            org.bukkit.inventory.ItemStack leatherLeggings = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS, 1);
            org.bukkit.inventory.ItemStack leatherBoots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS, 1);
            
            player.getInventory().setHelmet(leatherHelmet);
            player.getInventory().setChestplate(leatherChestplate);
            player.getInventory().setLeggings(leatherLeggings);
            player.getInventory().setBoots(leatherBoots);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error giving basic BedWars items: " + e.getMessage());
        }
    }
    
    /**
     * Get arena by name using BedWars1058 API
     */
    private static IArena getArenaByName(String arenaName) {
        try {
            // Try to use BedWars API if available
            if (BedWars.getAPI() != null) {
                return BedWars.getAPI().getArenaUtil().getArenaByName(arenaName);
            }
            
            // Fallback to reflection
            try {
                Method getArenaByNameMethod = Arena.class.getMethod("getArenaByName", String.class);
                return (IArena) getArenaByNameMethod.invoke(null, arenaName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to get arena by name via reflection: " + e.getMessage());
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error getting arena: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if an arena can be started (has at least one player)
     */
    public static boolean canStartGame(String arenaName) {
        IArena arena = getArenaByName(arenaName);
        if (arena == null) return false;
        
        return arena.getPlayers().size() >= 1 && 
               (arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting);
    }
    
    /**
     * Get all available arenas
     */
    public static List<IArena> getAllArenas() {
        try {
            // Try to use BedWars API if available
            if (BedWars.getAPI() != null) {
                return BedWars.getAPI().getArenaUtil().getArenas();
            }
            
            // Fallback to reflection
            try {
                Method getArenasMethod = Arena.class.getMethod("getArenas");
                return (List<IArena>) getArenasMethod.invoke(null);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to get arenas via reflection: " + e.getMessage());
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error getting arenas: " + e.getMessage());
        }
        return null;
    }

    /**
     * Set all players in the arena to survival mode so they can place blocks
     */
    private static void setPlayersToSurvivalMode(Arena arena) {
        try {
            Bukkit.getLogger().info("[BedWarsNPCFill] Setting players to survival mode for arena: " + arena.getArenaName());
            
            for (Player player : arena.getPlayers()) {
                try {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Setting " + player.getName() + " to survival mode");
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Successfully set " + player.getName() + " to survival mode");
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Could not set " + player.getName() + " to survival mode: " + e.getMessage());
                }
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Survival mode setup complete");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error setting players to survival mode: " + e.getMessage());
        }
    }
}

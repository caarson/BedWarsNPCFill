package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.event.SpawnReason;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

// Sentinel imports for combat AI
import org.mcmonkey.sentinel.SentinelTrait;
import org.mcmonkey.sentinel.SentinelUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPCManager {
    private static final Map<String, List<NPC>> arenaNPCs = new HashMap<>();
    private static final Map<String, Player> arenaExcludePlayers = new HashMap<>();
    
    /**
     * Spawn NPCs for an arena and assign them to all teams that don't have real players
     * @param arenaName The name of the arena
     * @param excludePlayer This parameter is no longer used but kept for backward compatibility
     */
    public static void spawnNPCs(String arenaName, Player excludePlayer) {
        try {
            // Get arena instance
            IArena arena = Arena.getArenaByName(arenaName);
            if (arena == null) {
                Bukkit.getLogger().severe("[BedWarsNPCFill] Could not find arena: " + arenaName);
                return;
            }
            
            // Only add NPCs when arena is in waiting state
            if (arena.getStatus() != com.andrei1058.bedwars.api.arena.GameState.waiting) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Cannot add NPCs to arena in state: " + arena.getStatus());
                return;
            }
            
            // Clear existing NPCs for this arena
            removeNPCs(arenaName);
            
            // Store exclude player for this arena
            if (excludePlayer != null) {
                arenaExcludePlayers.put(arenaName, excludePlayer);
                Bukkit.getLogger().info("[BedWarsNPCFill] Stored exclude player for arena " + arenaName + ": " + excludePlayer.getName());
            } else {
                arenaExcludePlayers.remove(arenaName);
            }
            
            // Ensure excludePlayer is assigned to a team if not already
            if (excludePlayer != null) {
                ITeam playerTeam = arena.getTeam(excludePlayer);
                if (playerTeam == null) {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Player " + excludePlayer.getName() + " is not in any team. Assigning to a team...");
                    // Find the first empty team and assign the player to it
                    for (ITeam team : arena.getTeams()) {
                        if (team.getMembers().isEmpty()) {
                            team.addPlayers(excludePlayer);
                            Bukkit.getLogger().info("[BedWarsNPCFill] Assigned player " + excludePlayer.getName() + " to team " + team.getName());
                            break;
                        }
                    }
                    // Re-check if player is now in a team
                    playerTeam = arena.getTeam(excludePlayer);
                    if (playerTeam == null) {
                        Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to assign player " + excludePlayer.getName() + " to any team! NPC spawning may not work correctly.");
                    }
                }
            }
            
            List<NPC> npcs = new ArrayList<>();
            arenaNPCs.put(arenaName, npcs);
            
            int npcCount = 0;
            int teamCount = arena.getTeams().size();
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Checking " + teamCount + " teams for NPC spawning in arena: " + arenaName);
            
            // Get the team to exclude (if excludePlayer is provided and in a team)
            ITeam excludeTeam = null;
            if (excludePlayer != null) {
                excludeTeam = arena.getTeam(excludePlayer);
                if (excludeTeam != null) {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Excluding player's team: " + excludeTeam.getName() + " for player: " + excludePlayer.getName());
                    // Debug: list all members of the excludeTeam
                    List<Player> excludeTeamMembers = excludeTeam.getMembers();
                    StringBuilder excludeMembersInfo = new StringBuilder();
                    for (Player member : excludeTeamMembers) {
                        boolean isNPC = CitizensAPI.getNPCRegistry().isNPC(member);
                        excludeMembersInfo.append(member.getName()).append(" (NPC: ").append(isNPC).append("), ");
                    }
                    Bukkit.getLogger().info("[BedWarsNPCFill] Exclude team members: " + excludeMembersInfo.toString());
                } else {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Player " + excludePlayer.getName() + " is not in any team! Cannot exclude team.");
                    // Debug: check all teams to see where player might be
                    for (ITeam team : arena.getTeams()) {
                        if (team.getMembers().contains(excludePlayer)) {
                            Bukkit.getLogger().info("[BedWarsNPCFill] Found player " + excludePlayer.getName() + " in team " + team.getName() + " via members list, but getTeam() returned null!");
                        }
                    }
                }
            }
            
            // Create NPCs for each team using actual team names from the arena
            for (ITeam team : arena.getTeams()) {
                String teamName = team.getName();
                List<Player> members = team.getMembers();
                
                Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Checking team: " + teamName + " for NPC spawning. Team members: " + members.size());
                
                // Log all members for debugging
                StringBuilder debugMemberInfo = new StringBuilder();
                for (Player member : members) {
                    boolean isNPC = CitizensAPI.getNPCRegistry().isNPC(member);
                    debugMemberInfo.append(member.getName()).append(" (NPC: ").append(isNPC).append("), ");
                }
                Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Team " + teamName + " members: " + debugMemberInfo.toString());
                
                // Check if this team contains the excludePlayer
                boolean containsPlayer = excludePlayer != null && members.contains(excludePlayer);
                Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Team " + teamName + " contains player " + (excludePlayer != null ? excludePlayer.getName() : "null") + ": " + containsPlayer);
                
                // For the player's team, we now allow NPCs to spawn as teammates
                // Only skip if the team contains other real players besides the excludePlayer
                if (containsPlayer) {
                    // Check if this is the player's team and if it has other real players
                    if (excludeTeam != null && team.equals(excludeTeam)) {
                        boolean hasOtherRealPlayers = false;
                        for (Player member : members) {
                            if (!CitizensAPI.getNPCRegistry().isNPC(member) && !member.equals(excludePlayer)) {
                                hasOtherRealPlayers = true;
                                break;
                            }
                        }
                        if (hasOtherRealPlayers) {
                            Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Skipping NPC spawn for player's team " + teamName + " - has other real players");
                            continue;
                        } else {
                            Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Allowing NPC spawn for player's team " + teamName + " - only real player is excludePlayer");
                        }
                    } else {
                        // For other teams, skip if they contain the excludePlayer (shouldn't happen, but safety)
                        Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Skipping NPC spawn for team " + teamName + " because it contains player " + excludePlayer.getName());
                        continue;
                    }
                }
                
                // This section has been removed as it's redundant with the previous check
                // The previous check already handles team membership correctly
                
                Location teamSpawn = team.getSpawn();
                
                Bukkit.getLogger().info("[BedWarsNPCFill] Team " + teamName + " has no real players. Creating NPC at spawn: " + teamSpawn);
                
                // Create NPC
                NPC npc = CitizensAPI.getNPCRegistry().createNPC(
                    org.bukkit.entity.EntityType.PLAYER, 
                    "NPC_" + arenaName + "_" + teamName
                );
                
                // Spawn NPC at team's spawn location
                npc.spawn(teamSpawn);
                npcs.add(npc);
                
                // Add NPC to scoreboard team
                addNPCToScoreboard(npc, teamName);
                
                // Configure Sentinel AI in PASSIVE mode initially to prevent early attacks
                configureSentinelAI(npc, teamName, arena, excludePlayer, false);
                
                // Simulate player join for BedWars, passing excludePlayer for safety check
                simulatePlayerJoin(npc, arenaName, teamName, excludePlayer);
                
                // Add NPC to custom bridging system
                NPCBridgingSystem.addNPCToBridging(npc);
                
                Bukkit.getLogger().info("[BedWarsNPCFill] Spawned NPC " + npc.getName() + " for team " + teamName + " at location: " + teamSpawn);
                npcCount++;
            }
            
        Bukkit.getLogger().info("[BedWarsNPCFill] Successfully spawned " + npcCount + " NPC opponents across " + teamCount + " teams!");
        
        // Force a scoreboard update after all NPCs are spawned
        forceScoreboardUpdate();
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error spawning NPCs: " + e.getMessage());
            Bukkit.getLogger().severe("[BedWarsNPCFill] NPC spawning error: " + e.getMessage());
        }
    }
    
    /**
     * Spawn NPCs for an arena and assign them to teams (backward compatibility)
     * @param arenaName The name of the arena
     */
    public static void spawnNPCs(String arenaName) {
        spawnNPCs(arenaName, null);
    }
    
    /**
     * Add an NPC to the scoreboard team
     * @param npc The NPC to add
     * @param teamName The team name
     */
    private static void addNPCToScoreboard(NPC npc, String teamName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.addEntry(npc.getUniqueId().toString());
    }
    
    /**
     * Simulate player join event for BedWars using official API with safety check for excludePlayer
     */
    private static void simulatePlayerJoin(NPC npc, String arenaName, String teamName, Player excludePlayer) {
        Bukkit.getLogger().info("[BedWarsNPCFill] Starting NPC join simulation for " + npc.getName() + " in " + arenaName + " with excludePlayer: " + (excludePlayer != null ? excludePlayer.getName() : "null"));
        
        try {
            // Ensure NPC entity is a Player
            if (!(npc.getEntity() instanceof Player)) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] NPC entity is not a Player!");
                return;
            }
            Player npcPlayer = (Player) npc.getEntity();
            Bukkit.getLogger().info("[BedWarsNPCFill] NPC entity is valid Player: " + npcPlayer.getName());
            
            // Get arena instance
            IArena arena = Arena.getArenaByName(arenaName);
            if (arena == null) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find arena: " + arenaName);
                return;
            }
            Bukkit.getLogger().info("[BedWarsNPCFill] Arena found: " + arenaName + " with status: " + arena.getStatus());
            
            // Get team instance
            ITeam team = arena.getTeam(teamName);
            if (team == null) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find team: " + teamName);
                return;
            }
            Bukkit.getLogger().info("[BedWarsNPCFill] Team found: " + teamName);
            
            // Note: We now allow NPCs on the player's team as teammates
            // The spawning logic above ensures only appropriate teams get NPCs
            
            // Create fake player connection using Citizens API
            Bukkit.getLogger().info("[BedWarsNPCFill] Creating fake player connection...");
            npc.addTrait(net.citizensnpcs.trait.GameModeTrait.class);
            net.citizensnpcs.api.event.NPCSpawnEvent spawnEvent = new net.citizensnpcs.api.event.NPCSpawnEvent(
                npc, 
                team.getSpawn(), 
                SpawnReason.PLUGIN
            );
            Bukkit.getPluginManager().callEvent(spawnEvent);
            
            if (!spawnEvent.isCancelled()) {
                // Properly add NPC to arena using BedWars API
                Bukkit.getLogger().info("[BedWarsNPCFill] Adding NPC to arena...");
                arena.addPlayer(npcPlayer, true); // Force add as spectator first
                Bukkit.getLogger().info("[BedWarsNPCFill] NPC added to arena successfully");
                
                // Assign NPC to team using BedWars API
                Bukkit.getLogger().info("[BedWarsNPCFill] Assigning NPC to team...");
                team.addPlayers(npcPlayer);
                Bukkit.getLogger().info("[BedWarsNPCFill] NPC assigned to team successfully");
                
                // Update team status
                team.setBedDestroyed(false);
                Bukkit.getLogger().info("[BedWarsNPCFill] Team bed status updated");
                
                // Make NPC appear as online player
                Bukkit.getLogger().info("[BedWarsNPCFill] Making NPC appear online...");
                npc.getOrAddTrait(net.citizensnpcs.trait.ScoreboardTrait.class);
                npc.getOrAddTrait(net.citizensnpcs.trait.SkinTrait.class);
                
                // Force update player list
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.hidePlayer(npcPlayer);
                    p.showPlayer(npcPlayer);
                }
                
                Bukkit.getLogger().info("[BedWarsNPCFill] NPC successfully joined arena: " + arenaName + " in team " + teamName);
            } else {
                Bukkit.getLogger().warning("[BedWarsNPCFill] NPC spawn event was cancelled!");
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] CRITICAL ERROR in NPC join simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simulate player join event for BedWars using official API (backward compatibility)
     */
    private static void simulatePlayerJoin(NPC npc, String arenaName, String teamName) {
        simulatePlayerJoin(npc, arenaName, teamName, null);
    }
    
    /**
     * Remove all NPCs for an arena
     * @param arenaName The name of the arena
     */
    public static void removeNPCs(String arenaName) {
        if (arenaNPCs.containsKey(arenaName)) {
            for (NPC npc : arenaNPCs.get(arenaName)) {
                npc.destroy();
            }
            arenaNPCs.remove(arenaName);
        }
    }
    
    /**
     * Remove all NPCs created by this plugin across all arenas
     * This method only removes NPCs with names starting with "NPC_" to avoid deleting user-created NPCs
     */
    public static void removeAllNPCs() {
        int removedCount = 0;
        // First, remove all NPCs from our internal tracking
        for (List<NPC> npcs : arenaNPCs.values()) {
            for (NPC npc : npcs) {
                npc.destroy();
                removedCount++;
            }
        }
        arenaNPCs.clear();
        
        // Additionally, scan all NPCs in the registry and remove any that match our naming pattern
        // This ensures we catch any NPCs that might not be in our map (e.g., from previous sessions)
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getName().startsWith("NPC_")) {
                npc.destroy();
                removedCount++;
            }
        }
        
        Bukkit.getLogger().info("[BedWarsNPCFill] Removed " + removedCount + " plugin-created NPCs");
    }
    
    /**
     * Force a scoreboard update to refresh team displays
     */
    private static void forceScoreboardUpdate() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            // Force update by modifying a team temporarily
            for (Team team : scoreboard.getTeams()) {
                team.setDisplayName(team.getDisplayName()); // This might trigger an update
            }
            Bukkit.getLogger().info("[BedWarsNPCFill] Forced scoreboard update");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Error forcing scoreboard update: " + e.getMessage());
        }
    }

    /**
     * Teleport all NPCs to their respective team islands
     * @param arena The arena containing the NPCs
     */
    public static void teleportNPCsToIslands(IArena arena) {
        try {
            if (arena == null) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Cannot teleport NPCs - arena is null");
                return;
            }

            String arenaName = arena.getArenaName();
            if (!arenaNPCs.containsKey(arenaName)) {
                Bukkit.getLogger().info("[BedWarsNPCFill] No NPCs found for arena: " + arenaName);
                return;
            }

            List<NPC> npcs = arenaNPCs.get(arenaName);
            Bukkit.getLogger().info("[BedWarsNPCFill] Teleporting " + npcs.size() + " NPCs to their islands");

            for (NPC npc : npcs) {
                try {
                    // Extract team name from NPC name (format: NPC_arenaName_teamName)
                    String npcName = npc.getName();
                    String[] parts = npcName.split("_");
                    if (parts.length >= 3) {
                        String teamName = parts[2]; // team name is the third part
                        
                        // Find the team
                        ITeam team = arena.getTeam(teamName);
                        if (team != null) {
                            // Teleport NPC to team spawn location
                            npc.teleport(team.getSpawn(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                            Bukkit.getLogger().info("[BedWarsNPCFill] Teleported NPC " + npcName + " to team " + teamName + " spawn");
                        } else {
                            Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find team for NPC: " + npcName);
                        }
                    } else {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Invalid NPC name format: " + npcName);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Error teleporting NPC " + npc.getName() + ": " + e.getMessage());
                }
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] NPC teleportation complete");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error in NPC teleportation: " + e.getMessage());
        }
    }

    /**
     * Configure Sentinel AI for NPCs with friendly system
     * @param npc The NPC to configure
     * @param teamName The team name for friendly/enemy identification
     * @param arena The arena where the NPC is located
     * @param excludePlayer The player to ignore (can be null)
     * @param enableCombat Whether to enable combat behavior or just set ignores
     */
    private static void configureSentinelAI(NPC npc, String teamName, IArena arena, Player excludePlayer, boolean enableCombat) {
        try {
            // Check if Sentinel is available before trying to configure AI
            Class.forName("org.mcmonkey.sentinel.SentinelTrait");
            
            // Add Sentinel trait to NPC
            SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
            
            // Basic settings
            sentinel.health = 20.0; // Full health
            sentinel.damage = 4.0; // Standard player damage
            
            // Get the exact scoreboard team name for proper team matching
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team scoreboardTeam = scoreboard.getTeam(teamName);
            String actualTeamName = teamName;
            if (scoreboardTeam != null) {
                actualTeamName = scoreboardTeam.getName();
                Bukkit.getLogger().info("[BedWarsNPCFill] Found scoreboard team: " + actualTeamName + " for team: " + teamName);
            } else {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find scoreboard team: " + teamName);
            }
            
            // Ignore teammates (friends) using team name - with error handling and fallback
            try {
                sentinel.addIgnore("team:" + actualTeamName);
                Bukkit.getLogger().info("[BedWarsNPCFill] Added team ignore for NPC " + npc.getName() + ": team:" + actualTeamName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to add team ignore for NPC " + npc.getName() + ", trying alternative method: " + e.getMessage());
                // Fallback: use direct command execution
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select " + npc.getId());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc sentinel ignore add team:" + actualTeamName);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Added team ignore via command for NPC " + npc.getName());
                } catch (Exception ex) {
                    Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to add team ignore via command for NPC " + npc.getName() + ": " + ex.getMessage());
                }
            }
            
            // Note: "same-team" ignore is not working with current Sentinel version
            // Team-based ignore and player-specific ignore should be sufficient
            Bukkit.getLogger().info("[BedWarsNPCFill] Skipping same-team ignore due to Sentinel API limitations");
            
            // Add specific player ignore only if NPC is on the same team as the excludePlayer
            if (excludePlayer != null) {
                // Get the excludePlayer's team
                ITeam playerTeam = arena.getTeam(excludePlayer);
                if (playerTeam != null && playerTeam.getName().equals(teamName)) {
                    // Only ignore the player if NPC is on the same team
                    try {
                        sentinel.addIgnore("player:" + excludePlayer.getName());
                        Bukkit.getLogger().info("[BedWarsNPCFill] Added player-specific ignore for NPC " + npc.getName() + ": " + excludePlayer.getName() + " (same team)");
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Failed to add player ignore for NPC " + npc.getName() + ", trying alternative method: " + e.getMessage());
                        // Fallback: use direct command execution
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select " + npc.getId());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc sentinel ignore add player:" + excludePlayer.getName());
                            Bukkit.getLogger().info("[BedWarsNPCFill] Added player ignore via command for NPC " + npc.getName());
                        } catch (Exception ex) {
                            Bukkit.getLogger().severe("[BedWarsNPCFill] Failed to add player ignore via command for NPC " + npc.getName() + ": " + ex.getMessage());
                        }
                    }
                } else {
                    Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " is not on player's team, will attack player: " + excludePlayer.getName());
                }
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Setting ignore for NPC " + npc.getName() + " on team: " + actualTeamName);
            
            if (enableCombat) {
                // Combat-specific settings
                sentinel.attackRate = 10; // Attack every 10 ticks (0.5 seconds)
                sentinel.range = 50.0; // Detection range within 50 blocks
                sentinel.chaseRange = 50.0; // Chase players within 50 blocks
                
                // Targeting settings - target all players not on the same team
                sentinel.addTarget("players");
                
                // Determine if NPC is on the same team as excludePlayer (friendly)
                boolean isFriendly = false;
                if (excludePlayer != null) {
                    ITeam playerTeam = arena.getTeam(excludePlayer);
                    if (playerTeam != null) {
                        String playerTeamName = playerTeam.getName();
                        Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: Comparing teams - NPC team: " + teamName + ", Player team: " + playerTeamName);
                        if (playerTeamName.equals(teamName)) {
                            isFriendly = true;
                            Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " is friendly to player " + excludePlayer.getName() + ", disabling fightback");
                        } else {
                            Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " is enemy to player " + excludePlayer.getName() + ", enabling fightback");
                        }
                    } else {
                        Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find player team for " + excludePlayer.getName());
                    }
                }
                
                // Combat behavior - make NPCs aggressive but disable fightback for friendly NPCs
                sentinel.closeChase = true; // Chase targets in melee range
                sentinel.rangedChase = false; // Don't chase when using ranged weapons
                sentinel.fightback = !isFriendly; // Only fight back if not friendly
                sentinel.ignoreLOS = true; // Don't require line of sight for better detection
                
                // Log the fightback setting for debugging
                Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " fightback setting: " + sentinel.fightback + " (isFriendly: " + isFriendly + ")");
                
                // Bed breaking simulation - target beds specifically
                sentinel.addTarget("block:bed");
                
                // Movement and pathfinding
                sentinel.speed = 1.0; // Normal movement speed
                sentinel.reach = 3.0; // Attack reach distance
                
                // Debug: Log current ignores and targets
                try {
                    java.lang.reflect.Field ignoresField = sentinel.getClass().getDeclaredField("ignores");
                    ignoresField.setAccessible(true);
                    java.util.List<?> ignores = (java.util.List<?>) ignoresField.get(sentinel);
                    Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: NPC " + npc.getName() + " ignores: " + ignores.toString());
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Could not access ignores for debugging: " + e.getMessage());
                }
                
                try {
                    java.lang.reflect.Field targetsField = sentinel.getClass().getDeclaredField("targets");
                    targetsField.setAccessible(true);
                    java.util.List<?> targets = (java.util.List<?>) targetsField.get(sentinel);
                    Bukkit.getLogger().info("[BedWarsNPCFill] DEBUG: NPC " + npc.getName() + " targets: " + targets.toString());
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Could not access targets for debugging: " + e.getMessage());
                }
                
                Bukkit.getLogger().info("[BedWarsNPCFill] Enabled combat behavior for NPC " + npc.getName());
            } else {
                // Non-combat settings - make NPC passive
                sentinel.attackRate = 0; // No attacks
                sentinel.range = 0; // No detection range
                sentinel.chaseRange = 0; // No chasing
                
                Bukkit.getLogger().info("[BedWarsNPCFill] NPC " + npc.getName() + " is in passive mode until game starts");
            }
            
            Bukkit.getLogger().info("[BedWarsNPCFill] Configured Sentinel AI for NPC " + npc.getName() + " on team " + teamName + " with ignore team:" + teamName);
            
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Sentinel not found - NPC combat features disabled for NPC " + npc.getName() + ". Install Sentinel plugin for NPC combat AI.");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error configuring Sentinel AI for NPC " + npc.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure Sentinel combat AI for NPCs with friendly system (backward compatibility)
     * @param npc The NPC to configure
     * @param teamName The team name for friendly/enemy identification
     * @param arena The arena where the NPC is located
     * @param excludePlayer The player to ignore (can be null)
     */
    private static void configureSentinelAI(NPC npc, String teamName, IArena arena, Player excludePlayer) {
        configureSentinelAI(npc, teamName, arena, excludePlayer, true);
    }

    /**
     * Start NPC combat behavior when game starts
     * @param arena The arena where NPCs should start combat
     */
    public static void startNPCCombat(IArena arena) {
        try {
            // Check if arena is in playing state before starting combat
            if (arena.getStatus() != com.andrei1058.bedwars.api.arena.GameState.playing) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Arena is not in playing state, delaying combat start");
                return;
            }
            
            // Check if Sentinel is available before trying to start combat
            Class.forName("org.mcmonkey.sentinel.SentinelTrait");
            
            String arenaName = arena.getArenaName();
            if (!arenaNPCs.containsKey(arenaName)) {
                return;
            }

            List<NPC> npcs = arenaNPCs.get(arenaName);
            Bukkit.getLogger().info("[BedWarsNPCFill] Starting combat behavior for " + npcs.size() + " NPCs in arena " + arenaName);

            // Start the custom bridging system for all NPCs
            NPCBridgingSystem.startBridgingSystem();
            
            for (NPC npc : npcs) {
                try {
                    SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
                    // Get the exclude player for this arena if available
                    Player excludePlayer = arenaExcludePlayers.get(arenaName);
                    Bukkit.getLogger().info("[BedWarsNPCFill] Configuring Sentinel AI for NPC " + npc.getName() + " with excludePlayer: " + (excludePlayer != null ? excludePlayer.getName() : "null"));
                    // Configure Sentinel AI with team-specific settings and arena context, passing excludePlayer
                    configureSentinelAI(npc, extractTeamNameFromNPC(npc), arena, excludePlayer);
                    
                    Bukkit.getLogger().info("[BedWarsNPCFill] Started combat for NPC " + npc.getName());
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Error starting combat for NPC " + npc.getName() + ": " + e.getMessage());
                }
            }
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Sentinel not found - NPC combat features disabled. Install Sentinel plugin for NPC combat AI.");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] Error starting NPC combat: " + e.getMessage());
        }
    }

    /**
     * Extract team name from NPC name (format: NPC_arenaName_teamName)
     * @param npc The NPC to extract team name from
     * @return The team name or empty string if not found
     */
    private static String extractTeamNameFromNPC(NPC npc) {
        String npcName = npc.getName();
        String[] parts = npcName.split("_");
        if (parts.length >= 3) {
            return parts[2]; // team name is the third part
        }
        return "";
    }
}

package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.arena.Arena;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BedWarsEventListener implements Listener {

    @EventHandler
    public void onPlayerJoinArena(PlayerJoinArenaEvent e) {
        IArena arena = e.getArena();
        String arenaName = arena.getArenaName();
        
        // Start the 40-second timer for the arena
        ArenaScheduler.startArenaTimer(arenaName, e.getPlayer());
        
        Bukkit.getLogger().info("[BedWarsNPCFill] Player " + e.getPlayer().getName() + 
                              " joined arena " + arenaName + ". Timer started.");
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.playing) {
            Bukkit.getLogger().info("[BedWarsNPCFill] Arena " + event.getArena().getArenaName() + " entered PLAYING state - enabling NPC combat");
            NPCManager.startNPCCombat(event.getArena());
        }
        
        // Clean up NPCs when game ends/restarts
        if (event.getNewState() == GameState.restarting) {
            Bukkit.getLogger().info("[BedWarsNPCFill] Arena " + event.getArena().getArenaName() + " is restarting - cleaning up NPCs");
            NPCManager.removeNPCs(event.getArena().getArenaName());
            NPCBridgingSystem.stopBridgingSystem();
            BedBreakingSystem.stopBedBreakingSystem();
        }
    }
    
    /**
     * Listen for player deaths to check if the game should end
     * This is critical for handling the case where only NPCs remain
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Skip if this is an NPC death
        if (CitizensAPI.getNPCRegistry().isNPC(player)) {
            return;
        }
        
        // Check if player is in a BedWars arena
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) {
            return;
        }
        
        // Only check during playing state
        if (arena.getStatus() != GameState.playing) {
            return;
        }
        
        // Get the player's team
        ITeam playerTeam = arena.getTeam(player);
        if (playerTeam == null) {
            return;
        }
        
        // Check if player's bed is destroyed (final elimination)
        if (playerTeam.isBedDestroyed()) {
            Bukkit.getLogger().info("[BedWarsNPCFill] Real player " + player.getName() + " died with bed destroyed - checking game end conditions");
            
            // Delay the check to allow the death to be processed
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkAndEndGameIfOnlyNPCsRemain(arena);
                }
            }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 5L);
        }
    }
    
    /**
     * Also check when a player leaves the arena
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeaveArena(PlayerLeaveArenaEvent event) {
        IArena arena = event.getArena();
        
        if (arena.getStatus() != GameState.playing) {
            return;
        }
        
        Bukkit.getLogger().info("[BedWarsNPCFill] Player " + event.getPlayer().getName() + " left arena - checking game end conditions");
        
        // Delay the check to allow the leave to be processed
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndEndGameIfOnlyNPCsRemain(arena);
            }
        }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 5L);
    }
    
    /**
     * Check if only NPCs remain in the game and end it if so
     */
    private void checkAndEndGameIfOnlyNPCsRemain(IArena arena) {
        if (arena == null || arena.getStatus() != GameState.playing) {
            return;
        }
        
        int realPlayerCount = 0;
        ITeam teamWithRealPlayer = null;
        
        // Count real (non-NPC) players still alive in the game
        for (ITeam team : arena.getTeams()) {
            for (Player member : team.getMembers()) {
                if (!CitizensAPI.getNPCRegistry().isNPC(member)) {
                    realPlayerCount++;
                    teamWithRealPlayer = team;
                }
            }
        }
        
        Bukkit.getLogger().info("[BedWarsNPCFill] Game end check: " + realPlayerCount + " real players remaining");
        
        // If no real players remain, or only one team has real players, end the game
        if (realPlayerCount == 0) {
            Bukkit.getLogger().info("[BedWarsNPCFill] No real players remain - ending game");
            endGameAndCleanup(arena, null);
        } else if (realPlayerCount > 0 && teamWithRealPlayer != null) {
            // Check if all remaining real players are on the same team
            boolean allSameTeam = true;
            for (ITeam team : arena.getTeams()) {
                if (team.equals(teamWithRealPlayer)) continue;
                
                for (Player member : team.getMembers()) {
                    if (!CitizensAPI.getNPCRegistry().isNPC(member)) {
                        // Found a real player on another team
                        allSameTeam = false;
                        break;
                    }
                }
                if (!allSameTeam) break;
            }
            
            if (allSameTeam) {
                Bukkit.getLogger().info("[BedWarsNPCFill] Only one team has real players - " + teamWithRealPlayer.getName() + " wins!");
                endGameAndCleanup(arena, teamWithRealPlayer);
            }
        }
    }
    
    /**
     * End the game and clean up NPCs
     */
    private void endGameAndCleanup(IArena arena, ITeam winningTeam) {
        // Remove all NPCs from all teams first
        for (ITeam team : arena.getTeams()) {
            // Create a copy to avoid concurrent modification
            java.util.List<Player> membersToRemove = new java.util.ArrayList<>();
            for (Player member : team.getMembers()) {
                if (CitizensAPI.getNPCRegistry().isNPC(member)) {
                    membersToRemove.add(member);
                }
            }
            
            // Remove NPCs from team
            for (Player npcPlayer : membersToRemove) {
                team.getMembers().remove(npcPlayer);
            }
        }
        
        // Clean up our tracked NPCs
        NPCManager.removeNPCs(arena.getArenaName());
        NPCBridgingSystem.stopBridgingSystem();
        BedBreakingSystem.stopBedBreakingSystem();
        
        // Now trigger the game's normal winner check
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getLogger().info("[BedWarsNPCFill] Triggering checkWinner for arena " + arena.getArenaName());
                    arena.checkWinner();
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[BedWarsNPCFill] Error triggering checkWinner: " + e.getMessage());
                }
            }
        }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 10L);
    }
}

package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

public class ArenaScheduler {
    private static final Map<String, Integer> arenaTimers = new HashMap<>();

    public static void startArenaTimer(String arenaName, Player initialPlayer) {
        // Cancel existing timer if any
        if (arenaTimers.containsKey(arenaName)) {
            Bukkit.getScheduler().cancelTask(arenaTimers.get(arenaName));
        }

        // Start new 40-second timer
        int taskId = new BukkitRunnable() {
            int countdown = BedWarsNPCFillPlugin.getInstance().getConfigHandler().getStartDelay();

            @Override
            public void run() {
                if (countdown > 0) {
                    // Update countdown message
                    initialPlayer.sendMessage("§a[BedWarsNPCFill] §eGame starting with NPCs in " + countdown + " seconds...");
                    countdown--;
                } else {
                    // Time's up - fill with NPCs after a 5-tick delay to ensure player team assignment
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fillArenaWithNPCs(arenaName, initialPlayer);
                            
                            // Start game after 2 seconds to allow NPCs to initialize
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    startGame(arenaName, initialPlayer);
                                }
                            }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 40L); // 2 seconds (20 ticks/sec * 2)
                        }
                    }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 5L); // 0.25 second delay for team assignment
                    
                    this.cancel();
                    arenaTimers.remove(arenaName);
                }
            }
        }.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 20L).getTaskId();

        arenaTimers.put(arenaName, taskId);
        Bukkit.getLogger().info("[BedWarsNPCFill] Started 40s timer for arena: " + arenaName);
    }

    private static void fillArenaWithNPCs(String arenaName, Player initialPlayer) {
        try {
            Bukkit.getLogger().info("[BedWarsNPCFill] Filling arena with NPCs: " + arenaName);
            
            // Use NPCManager to spawn NPCs for all teams in the arena
            IArena arena = Arena.getArenaByName(arenaName);
            if (arena != null) {
                NPCManager.spawnNPCs(arenaName, initialPlayer);
            } else {
                Bukkit.getLogger().warning("[BedWarsNPCFill] Could not find arena: " + arenaName);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BedWarsNPCFill] NPC filling error: " + e.getMessage());
        }
    }

    private static void startGame(String arenaName, Player player) {
        player.sendMessage("§a[BedWarsNPCFill] §eStarting game with NPC opponents!");
        Bukkit.getLogger().info("[BedWarsNPCFill] Starting game for arena: " + arenaName);
        
        // Use the new GameStarter to start the game immediately
        GameStarter.startGameImmediately(arenaName, player);
    }

    public static void cancelArenaTimer(String arenaName) {
        if (arenaTimers.containsKey(arenaName)) {
            Bukkit.getScheduler().cancelTask(arenaTimers.get(arenaName));
            arenaTimers.remove(arenaName);
            Bukkit.getLogger().info("[BedWarsNPCFill] Canceled timer for arena: " + arenaName);
        }
    }
}

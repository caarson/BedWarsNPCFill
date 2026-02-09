package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.GameState;
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
                IArena arena = Arena.getArenaByName(arenaName);
                if (arena == null) {
                    cancelCountdown(arenaName, this, "arena not found");
                    return;
                }

                if (arena.getStatus() != GameState.waiting) {
                    cancelCountdown(arenaName, this, "arena no longer waiting");
                    return;
                }

                if (arena.getPlayers().isEmpty()) {
                    cancelCountdown(arenaName, this, "arena empty");
                    return;
                }

                Player anchor = initialPlayer;
                if (anchor == null || !arena.getPlayers().contains(anchor)) {
                    anchor = arena.getPlayers().get(0);
                }

                if (countdown > 0) {
                    for (Player player : arena.getPlayers()) {
                        player.sendMessage("§a[BedWarsNPCFill] §eGame starting with NPCs in " + countdown + " seconds...");
                    }
                    countdown--;
                    return;
                }

                if (anchor == null) {
                    cancelCountdown(arenaName, this, "no anchor player available");
                    return;
                }
                triggerFillAndStart(arenaName, anchor);
                cancelCountdown(arenaName, this, "countdown finished");
            }
        }.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 0L, 20L).getTaskId();

        arenaTimers.put(arenaName, taskId);
        Bukkit.getLogger().info("[BedWarsNPCFill] Started 40s timer for arena: " + arenaName);
    }

    public static String forceStartNow(String arenaName, Player initialPlayer) {
        IArena arena = Arena.getArenaByName(arenaName);
        if (arena == null) {
            return "Arena not found.";
        }
        if (arena.getStatus() != GameState.waiting) {
            return "Arena must be in waiting state (currently " + arena.getStatus() + ").";
        }
        if (arena.getPlayers().isEmpty()) {
            return "Arena does not have any players.";
        }

        Player anchor = initialPlayer;
        if (anchor == null || !arena.getPlayers().contains(anchor)) {
            anchor = arena.getPlayers().get(0);
        }

        if (anchor == null) {
            return "Could not determine an anchor player to seed NPCs.";
        }

        cancelArenaTimer(arenaName);
        triggerFillAndStart(arenaName, anchor);
        Bukkit.getLogger().info("[BedWarsNPCFill] Force-start triggered for arena " + arenaName + " by " + (initialPlayer != null ? initialPlayer.getName() : "system"));
        return null;
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

    private static void triggerFillAndStart(String arenaName, Player anchor) {
        Player finalAnchor = anchor;
        new BukkitRunnable() {
            @Override
            public void run() {
                fillArenaWithNPCs(arenaName, finalAnchor);

                // Start game after 2 seconds to allow NPCs to initialize
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        startGame(arenaName, finalAnchor);
                    }
                }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 40L);
            }
        }.runTaskLater(BedWarsNPCFillPlugin.getInstance(), 5L);
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

    private static void cancelCountdown(String arenaName, BukkitRunnable task, String reason) {
        task.cancel();
        arenaTimers.remove(arenaName);
        Bukkit.getLogger().info("[BedWarsNPCFill] Countdown for arena " + arenaName + " stopped: " + reason);
    }
}

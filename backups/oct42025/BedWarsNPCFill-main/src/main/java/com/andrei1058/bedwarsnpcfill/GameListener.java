package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {
    private Object bedWarsAPI;

    public GameListener() {
        // Try to get BedWars API instance using reflection
        try {
            Class<?> bedWarsClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
            this.bedWarsAPI = Bukkit.getServicesManager().getRegistration(bedWarsClass).getProvider();
            if (bedWarsAPI != null) {
                Bukkit.getLogger().info("[BedWarsNPCFill] GameListener: BedWars API available via reflection");
            } else {
                Bukkit.getLogger().warning("[BedWarsNPCFill] GameListener: BedWars API not available yet");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] GameListener: Failed to get BedWars API: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // This is a fallback listener - main functionality is in BedWarsEventListener
        // Only log for debugging purposes
        Player player = event.getPlayer();
        Bukkit.getLogger().info("[BedWarsNPCFill] GameListener: Player " + player.getName() + " joined the server");
        
        if (bedWarsAPI != null) {
            try {
                Object arenaUtil = bedWarsAPI.getClass().getMethod("getArenaUtil").invoke(bedWarsAPI);
                Object arena = arenaUtil.getClass().getMethod("getArenaByPlayer", Player.class).invoke(arenaUtil, player);
                if (arena != null) {
                    String arenaName = (String) arena.getClass().getMethod("getArenaName").invoke(arena);
                    Object status = arena.getClass().getMethod("getStatus").invoke(arena);
                    Bukkit.getLogger().info("[BedWarsNPCFill] GameListener: Player " + player.getName() + " is in arena " + arenaName + " with status " + status);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[BedWarsNPCFill] GameListener: Error checking arena for player " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // This is a fallback listener - main functionality is in BedWarsEventListener
        Player player = event.getPlayer();
        Bukkit.getLogger().info("[BedWarsNPCFill] GameListener: Player " + player.getName() + " left the server");
    }
}

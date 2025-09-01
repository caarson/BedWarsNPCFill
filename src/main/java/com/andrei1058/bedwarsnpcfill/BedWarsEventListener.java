package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
}

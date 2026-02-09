package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// Import Citizens API for NPC cleaning
import net.citizensnpcs.api.CitizensAPI;

public class BedWarsNPCFillCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§a[BedWarsNPCFill] Commands:");
            player.sendMessage("§e/bedwarsnpcfill test - Test if you're in a BedWars arena");
            player.sendMessage("§e/bedwarsnpcfill start - Force start countdown if in arena");
            player.sendMessage("§e/bedwarsnpcfill cleannpcs - Remove all spawned NPCs");
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            testPlayerArena(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            forceStartCountdown(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("cleannpcs")) {
            cleanNPCs(player);
            return true;
        }

        return false;
    }

    private void testPlayerArena(Player player) {
        try {
            // First, let's check if BedWars1058 plugin is available
            org.bukkit.plugin.Plugin bedWarsPlugin = Bukkit.getPluginManager().getPlugin("BedWars1058");
            if (bedWarsPlugin == null) {
                player.sendMessage("§c[BedWarsNPCFill] BedWars1058 plugin not found!");
                return;
            }
            
            player.sendMessage("§a[BedWarsNPCFill] BedWars1058 plugin found: " + bedWarsPlugin.getDescription().getVersion());
            
            // Try to get BedWars API using reflection with better error handling
            Class<?> bedWarsClass = null;
            Object bedWarsAPI = null;
            
            try {
                bedWarsClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
                player.sendMessage("§a[BedWarsNPCFill] BedWars API class found!");
                
                org.bukkit.plugin.RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(bedWarsClass);
                if (registration != null) {
                    bedWarsAPI = registration.getProvider();
                    player.sendMessage("§a[BedWarsNPCFill] BedWars API service provider found!");
                } else {
                    player.sendMessage("§c[BedWarsNPCFill] BedWars API service provider not registered!");
                    return;
                }
            } catch (ClassNotFoundException e) {
                player.sendMessage("§c[BedWarsNPCFill] BedWars API class not found: " + e.getMessage());
                return;
            }

            if (bedWarsAPI != null) {
                try {
                    // Get arena util with better error handling
                    Object arenaUtil = bedWarsAPI.getClass().getMethod("getArenaUtil").invoke(bedWarsAPI);
                    player.sendMessage("§a[BedWarsNPCFill] ArenaUtil obtained successfully!");
                    
                    // Get arena by player
                    Object arena = arenaUtil.getClass().getMethod("getArenaByPlayer", Player.class).invoke(arenaUtil, player);

                    if (arena != null) {
                        String arenaName = (String) arena.getClass().getMethod("getArenaName").invoke(arena);
                        Object status = arena.getClass().getMethod("getStatus").invoke(arena);
                        @SuppressWarnings("unchecked")
                        java.util.List<Player> players = (java.util.List<Player>) arena.getClass().getMethod("getPlayers").invoke(arena);

                        player.sendMessage("§a[BedWarsNPCFill] You are in arena: §e" + arenaName);
                        player.sendMessage("§a[BedWarsNPCFill] Arena status: §e" + status);
                        player.sendMessage("§a[BedWarsNPCFill] Players in arena: §e" + players.size());
                        
                        // List player names
                        StringBuilder playerNames = new StringBuilder();
                        for (Player p : players) {
                            if (playerNames.length() > 0) playerNames.append(", ");
                            playerNames.append(p.getName());
                        }
                        player.sendMessage("§a[BedWarsNPCFill] Player list: §e" + playerNames.toString());
                        
                    } else {
                        player.sendMessage("§c[BedWarsNPCFill] You are not in a BedWars arena!");
                        
                        // Let's try to get all available arenas for debugging
                        try {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> arenas = (java.util.List<Object>) arenaUtil.getClass().getMethod("getArenas").invoke(arenaUtil);
                            player.sendMessage("§a[BedWarsNPCFill] Total arenas available: §e" + arenas.size());
                            
                            if (arenas.size() > 0) {
                                player.sendMessage("§a[BedWarsNPCFill] Available arenas:");
                                for (Object a : arenas) {
                                    String name = (String) a.getClass().getMethod("getArenaName").invoke(a);
                                    Object stat = a.getClass().getMethod("getStatus").invoke(a);
                                    player.sendMessage("§e- " + name + " (status: " + stat + ")");
                                }
                            }
                        } catch (Exception e2) {
                            player.sendMessage("§c[BedWarsNPCFill] Could not get arena list: " + e2.getMessage());
                        }
                    }
                } catch (Exception e) {
                    player.sendMessage("§c[BedWarsNPCFill] Error accessing arena methods: " + e.getMessage());
                    player.sendMessage("§c[BedWarsNPCFill] Error type: " + e.getClass().getSimpleName());
                    if (e.getCause() != null) {
                        player.sendMessage("§c[BedWarsNPCFill] Cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                }
            } else {
                player.sendMessage("§c[BedWarsNPCFill] BedWars API not available!");
            }
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void forceStartCountdown(Player player) {
        try {
            // Use reflection to get BedWars API
            Object bedWarsAPI = Bukkit.getServicesManager().getRegistration(
                Class.forName("com.andrei1058.bedwars.api.BedWars")
            ).getProvider();

            if (bedWarsAPI != null) {
                // Get arena by player using reflection
                Object arenaUtil = bedWarsAPI.getClass().getMethod("getArenaUtil").invoke(bedWarsAPI);
                Object arena = arenaUtil.getClass().getMethod("getArenaByPlayer", Player.class).invoke(arenaUtil, player);

                if (arena != null) {
                    String arenaName = (String) arena.getClass().getMethod("getArenaName").invoke(arena);
                    Object status = arena.getClass().getMethod("getStatus").invoke(arena);

                    if (status.toString().equalsIgnoreCase("waiting")) {
                        player.sendMessage("§a[BedWarsNPCFill] Starting countdown for arena: §e" + arenaName);
                        
                        player.sendMessage("§a[BedWarsNPCFill] NPC filling is currently disabled");
                        
                    } else {
                        player.sendMessage("§c[BedWarsNPCFill] Arena is not in waiting status! Current: " + status);
                    }
                } else {
                    player.sendMessage("§c[BedWarsNPCFill] You are not in a BedWars arena!");
                }
            } else {
                player.sendMessage("§c[BedWarsNPCFill] BedWars API not available!");
            }
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanNPCs(Player player) {
        try {
            // Get count of NPCs before removal for feedback
            int beforeCount = 0;
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (npc.getName().startsWith("NPC_")) {
                    beforeCount++;
                }
            }
            
            NPCManager.removeAllNPCs();
            
            // Get count after removal to confirm
            int afterCount = 0;
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (npc.getName().startsWith("NPC_")) {
                    afterCount++;
                }
            }
            
            int removedCount = beforeCount - afterCount;
            player.sendMessage("§a[BedWarsNPCFill] Removed " + removedCount + " plugin-created NPCs!");
            player.sendMessage("§a[BedWarsNPCFill] " + afterCount + " plugin NPCs remaining.");
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] Error cleaning NPCs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

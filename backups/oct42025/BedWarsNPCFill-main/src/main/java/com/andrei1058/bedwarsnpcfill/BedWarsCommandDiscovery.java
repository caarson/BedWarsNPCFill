package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;

public class BedWarsCommandDiscovery {
    
    public static void discoverBedWarsCommands(Player player) {
        player.sendMessage("§a[BedWarsNPCFill] §eDiscovering available BedWars commands...");
        
        // Check help topics (this works in 1.8.8)
        player.sendMessage("§e[BedWarsNPCFill] §eChecking help topics...");
        int helpCount = 0;
        for (HelpTopic topic : Bukkit.getHelpMap().getHelpTopics()) {
            String name = topic.getName().toLowerCase();
            if ((name.contains("bw") || name.contains("bedwar")) && !name.contains("bedwarsnpcfill")) {
                player.sendMessage("§e  /" + topic.getName().substring(1) + " - " + topic.getShortText());
                helpCount++;
                if (helpCount > 15) {
                    player.sendMessage("§e  ... and more (showing first 15)");
                    break;
                }
            }
        }
        
        if (helpCount == 0) {
            player.sendMessage("§c[BedWarsNPCFill] §eNo BedWars help topics found!");
        }
        
        // Check what plugin is providing BedWars commands
        if (Bukkit.getPluginManager().getPlugin("BedWars1058") != null) {
            org.bukkit.plugin.Plugin bwPlugin = Bukkit.getPluginManager().getPlugin("BedWars1058");
            player.sendMessage("§a[BedWarsNPCFill] §eBedWars1058 plugin info:");
            player.sendMessage("§e  Version: " + bwPlugin.getDescription().getVersion());
            player.sendMessage("§e  Main class: " + bwPlugin.getDescription().getMain());
            
            // Get commands from plugin.yml
            if (bwPlugin.getDescription().getCommands() != null) {
                player.sendMessage("§e  Commands defined in plugin.yml:");
                for (String cmdName : bwPlugin.getDescription().getCommands().keySet()) {
                    player.sendMessage("§e    /" + cmdName);
                }
            }
        }
        
        // Try some basic command tests using different approach
        player.sendMessage("§e[BedWarsNPCFill] §eTesting basic commands by execution...");
        testCommandByExecution(player, "bw help");
        testCommandByExecution(player, "bedwars help");
        testCommandByExecution(player, "bw");
        testCommandByExecution(player, "bedwars");
    }
    
    private static void testCommandByExecution(Player player, String command) {
        try {
            player.sendMessage("§e[BedWarsNPCFill] §eTesting: /" + command);
            
            // Try to execute the command and see what happens
            boolean result = Bukkit.dispatchCommand(player, command);
            
            if (result) {
                player.sendMessage("§a[BedWarsNPCFill] §e✓ Command executed: /" + command);
            } else {
                player.sendMessage("§c[BedWarsNPCFill] §e✗ Command failed: /" + command);
            }
        } catch (Exception e) {
            player.sendMessage("§c[BedWarsNPCFill] §e✗ Error testing /" + command + ": " + e.getMessage());
        }
    }
}

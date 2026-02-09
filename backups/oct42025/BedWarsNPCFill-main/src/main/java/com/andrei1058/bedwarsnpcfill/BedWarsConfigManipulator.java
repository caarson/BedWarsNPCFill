package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Attempts to modify BedWars1058 arena configuration directly
 * to temporarily set minimum players to 1 for solo testing
 */
public class BedWarsConfigManipulator {
    
    public static void tryModifyArenaConfig(String arenaName, Player player) {
        try {
            Plugin bedwars = Bukkit.getPluginManager().getPlugin("BedWars1058");
            if (bedwars == null) {
                Bukkit.getLogger().log(Level.WARNING, "[BedWarsNPCFill] BedWars1058 plugin not found!");
                return;
            }
            
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Attempting to modify arena config for: " + arenaName);
            
            // Try to access the arena manager
            tryAccessArenaManager(bedwars, arenaName, player);
            
            // Alternative: try to access arena directly through reflection
            tryDirectArenaAccess(bedwars, arenaName, player);
            
            // Last resort: try to modify configuration files
            tryConfigFileModification(arenaName, player);
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[BedWarsNPCFill] Error in config manipulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void tryAccessArenaManager(Plugin bedwars, String arenaName, Player player) {
        try {
            // Try to get the main class and arena manager
            Class<?> mainClass = bedwars.getClass();
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] BedWars main class: " + mainClass.getName());
            
            // Look for arena-related fields
            Field[] fields = mainClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName().toLowerCase();
                if (fieldName.contains("arena") || fieldName.contains("manager")) {
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Found field: " + field.getName() + " of type: " + field.getType().getName());
                    
                    Object fieldValue = field.get(bedwars);
                    if (fieldValue != null) {
                        tryManipulateArenaObject(fieldValue, arenaName, player);
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Arena manager access failed: " + e.getMessage());
        }
    }
    
    private static void tryManipulateArenaObject(Object arenaManager, String arenaName, Player player) {
        try {
            Class<?> managerClass = arenaManager.getClass();
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Arena manager class: " + managerClass.getName());
            
            // Look for methods to get arena by name
            Method[] methods = managerClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.contains("get") && methodName.contains("arena")) || 
                    methodName.contains("find") || methodName.contains("search")) {
                    
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Found method: " + method.getName());
                    
                    // Try to call the method with arena name
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                        method.setAccessible(true);
                        Object arena = method.invoke(arenaManager, arenaName);
                        
                        if (arena != null) {
                            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Found arena object: " + arena.getClass().getName());
                            tryModifyArenaMinPlayers(arena, player);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Arena object manipulation failed: " + e.getMessage());
        }
    }
    
    private static void tryModifyArenaMinPlayers(Object arena, Player player) {
        try {
            Class<?> arenaClass = arena.getClass();
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Arena class: " + arenaClass.getName());
            
            // Look for min players field
            Field[] fields = arenaClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName().toLowerCase();
                
                if (fieldName.contains("min") && fieldName.contains("player")) {
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Found min players field: " + field.getName());
                    
                    Object currentValue = field.get(arena);
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Current min players: " + currentValue);
                    
                    // Try to set it to 1
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(arena, 1);
                        Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Set min players to 1 for arena!");
                        
                        // Try to force start the arena now
                        tryForceStartModifiedArena(arena, player);
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Min players modification failed: " + e.getMessage());
        }
    }
    
    private static void tryForceStartModifiedArena(Object arena, Player player) {
        try {
            Class<?> arenaClass = arena.getClass();
            
            // Look for start methods
            Method[] methods = arenaClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if (methodName.contains("start") || methodName.contains("begin") || methodName.contains("commence")) {
                    method.setAccessible(true);
                    
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Trying to call start method: " + method.getName());
                    
                    if (method.getParameterCount() == 0) {
                        method.invoke(arena);
                    } else if (method.getParameterCount() == 1) {
                        method.invoke(arena, player);
                    }
                    
                    Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Successfully called start method!");
                    break;
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Force start failed: " + e.getMessage());
        }
    }
    
    private static void tryDirectArenaAccess(Plugin bedwars, String arenaName, Player player) {
        try {
            // This is a more direct approach - try to access static arena collections
            Class<?> mainClass = bedwars.getClass();
            
            // Look for static fields that might contain arenas
            Field[] fields = mainClass.getDeclaredFields();
            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    
                    if (value != null) {
                        String valueType = value.getClass().getName().toLowerCase();
                        if (valueType.contains("map") || valueType.contains("list") || valueType.contains("collection")) {
                            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Found static collection: " + field.getName());
                            // Try to access this collection to find arenas
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Direct arena access failed: " + e.getMessage());
        }
    }
    
    private static void tryConfigFileModification(String arenaName, Player player) {
        try {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Attempting config file modification for: " + arenaName);
            
            // This would require file system access to modify the arena config files
            // For now, just log that we tried this approach
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Config file modification not implemented yet - would need to modify arena YAML files");
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "[BedWarsNPCFill] Config file modification failed: " + e.getMessage());
        }
    }
}

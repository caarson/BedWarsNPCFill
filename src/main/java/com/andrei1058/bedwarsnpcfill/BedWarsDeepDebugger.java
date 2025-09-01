package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Deep debugging tool to understand BedWars1058 internals
 */
public class BedWarsDeepDebugger {
    
    public static void performDeepAnalysis(String arenaName, Player player) {
        try {
            Bukkit.getLogger().log(Level.INFO, "§c[DEBUG] ===== STARTING DEEP BEDWARS1058 ANALYSIS =====");
            
            Plugin bedwars = Bukkit.getPluginManager().getPlugin("BedWars1058");
            if (bedwars == null) {
                Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] BedWars1058 plugin not found!");
                return;
            }
            
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] BedWars1058 Plugin Version: " + bedwars.getDescription().getVersion());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] BedWars1058 Main Class: " + bedwars.getClass().getName());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Player World: " + player.getWorld().getName());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Target Arena: " + arenaName);
            
            // Analyze the main plugin class
            analyzePluginClass(bedwars, arenaName, player);
            
            // Try to find arena objects
            findArenaObjects(bedwars, arenaName, player);
            
            // Analyze player state
            analyzePlayerState(player);
            
            // Check for API access
            checkAPIAccess();
            
            Bukkit.getLogger().log(Level.INFO, "§c[DEBUG] ===== DEEP ANALYSIS COMPLETE =====");
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "§c[DEBUG] Deep analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void analyzePluginClass(Plugin bedwars, String arenaName, Player player) {
        try {
            Bukkit.getLogger().log(Level.INFO, "§6[DEBUG] --- ANALYZING PLUGIN CLASS ---");
            
            Class<?> mainClass = bedwars.getClass();
            
            // List all fields
            Field[] fields = mainClass.getDeclaredFields();
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Found " + fields.length + " fields in main class:");
            
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(bedwars);
                    String valueInfo = value != null ? value.getClass().getSimpleName() : "null";
                    Bukkit.getLogger().log(Level.INFO, "§e[DEBUG]   - " + field.getName() + " (" + field.getType().getSimpleName() + ") = " + valueInfo);
                    
                    // If this looks like an arena-related field, investigate further
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("arena") || fieldName.contains("manager") || fieldName.contains("game")) {
                        investigateArenaField(field, value, arenaName, player);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.INFO, "§c[DEBUG]   - " + field.getName() + " (inaccessible): " + e.getMessage());
                }
            }
            
            // List all methods
            Method[] methods = mainClass.getDeclaredMethods();
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Found " + methods.length + " methods in main class");
            
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if (methodName.contains("arena") || methodName.contains("game") || methodName.contains("start")) {
                    Bukkit.getLogger().log(Level.INFO, "§e[DEBUG]   - " + method.getName() + "(" + method.getParameterCount() + " params)");
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Plugin class analysis failed: " + e.getMessage());
        }
    }
    
    private static void investigateArenaField(Field field, Object value, String arenaName, Player player) {
        if (value == null) return;
        
        try {
            Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] --- INVESTIGATING ARENA FIELD: " + field.getName() + " ---");
            
            Class<?> valueClass = value.getClass();
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Field type: " + valueClass.getName());
            
            // Check if it's a collection
            if (value instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Map contains " + map.size() + " entries:");
                
                for (Object key : map.keySet()) {
                    Object mapValue = map.get(key);
                    Bukkit.getLogger().log(Level.INFO, "§e[DEBUG]   - Key: " + key + " -> Value: " + (mapValue != null ? mapValue.getClass().getSimpleName() : "null"));
                    
                    // Check if the key matches our arena name
                    if (key != null && key.toString().equalsIgnoreCase(arenaName)) {
                        Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] *** FOUND MATCHING ARENA: " + key + " ***");
                        analyzeArenaObject(mapValue, player);
                    }
                }
            } else if (value instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) value;
                Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] List contains " + list.size() + " items");
                
                for (int i = 0; i < Math.min(list.size(), 10); i++) { // Only show first 10
                    Object item = list.get(i);
                    Bukkit.getLogger().log(Level.INFO, "§e[DEBUG]   - [" + i + "]: " + (item != null ? item.getClass().getSimpleName() : "null"));
                }
            } else {
                // Single object - check if it has methods to get arenas
                Method[] methods = valueClass.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName().toLowerCase();
                    if ((methodName.contains("get") && methodName.contains("arena")) || 
                        methodName.contains("find") || methodName.equals("getArenaByName")) {
                        
                        Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] Found arena getter method: " + method.getName());
                        
                        if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                            try {
                                method.setAccessible(true);
                                Object arena = method.invoke(value, arenaName);
                                if (arena != null) {
                                    Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] *** FOUND ARENA VIA METHOD: " + method.getName() + " ***");
                                    analyzeArenaObject(arena, player);
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().log(Level.INFO, "§c[DEBUG] Method call failed: " + e.getMessage());
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Arena field investigation failed: " + e.getMessage());
        }
    }
    
    private static void analyzeArenaObject(Object arena, Player player) {
        if (arena == null) return;
        
        try {
            Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] --- ANALYZING ARENA OBJECT ---");
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Arena class: " + arena.getClass().getName());
            
            Class<?> arenaClass = arena.getClass();
            Field[] fields = arenaClass.getDeclaredFields();
            
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Arena fields:");
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(arena);
                    String fieldName = field.getName();
                    
                    if (fieldName.toLowerCase().contains("player") || 
                        fieldName.toLowerCase().contains("min") || 
                        fieldName.toLowerCase().contains("max") || 
                        fieldName.toLowerCase().contains("state") || 
                        fieldName.toLowerCase().contains("status")) {
                        
                        Bukkit.getLogger().log(Level.INFO, "§a[DEBUG]   - " + fieldName + " = " + value);
                        
                        // If this is min players, try to modify it
                        if (fieldName.toLowerCase().contains("min") && fieldName.toLowerCase().contains("player")) {
                            Bukkit.getLogger().log(Level.INFO, "§c[DEBUG] *** ATTEMPTING TO MODIFY MIN PLAYERS ***");
                            Object oldValue = value;
                            try {
                                if (field.getType() == int.class || field.getType() == Integer.class) {
                                    field.set(arena, 1);
                                    Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] Changed " + fieldName + " from " + oldValue + " to 1");
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Failed to modify " + fieldName + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Field inaccessible
                }
            }
            
            // Try to call start methods
            Method[] methods = arenaClass.getDeclaredMethods();
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Looking for start methods...");
            
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if (methodName.contains("start") || methodName.contains("begin") || methodName.contains("force")) {
                    Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] Found potential start method: " + method.getName() + " (" + method.getParameterCount() + " params)");
                    
                    try {
                        method.setAccessible(true);
                        if (method.getParameterCount() == 0) {
                            Bukkit.getLogger().log(Level.INFO, "§c[DEBUG] *** ATTEMPTING TO CALL " + method.getName() + " ***");
                            Object result = method.invoke(arena);
                            Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] Method result: " + result);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Method call failed: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Arena object analysis failed: " + e.getMessage());
        }
    }
    
    private static void findArenaObjects(Plugin bedwars, String arenaName, Player player) {
        try {
            Bukkit.getLogger().log(Level.INFO, "§6[DEBUG] --- SEARCHING FOR ARENA OBJECTS ---");
            
            // Try to access common static arena collections
            String[] possibleClasses = {
                "com.andrei1058.bedwars.arena.Arena",
                "com.andrei1058.bedwars.api.arena.IArena", 
                "com.andrei1058.bedwars.arena.ArenaManager",
                "com.andrei1058.bedwars.BedWars",
                "com.andrei1058.bedwars.Main"
            };
            
            for (String className : possibleClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] Found class: " + className);
                    
                    // Look for static fields
                    Field[] staticFields = clazz.getDeclaredFields();
                    for (Field field : staticFields) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            field.setAccessible(true);
                            try {
                                Object value = field.get(null);
                                if (value != null) {
                                    Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Static field " + field.getName() + ": " + value.getClass().getSimpleName());
                                    investigateArenaField(field, value, arenaName, player);
                                }
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                    }
                    
                } catch (ClassNotFoundException e) {
                    // Class doesn't exist, continue
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Arena object search failed: " + e.getMessage());
        }
    }
    
    private static void analyzePlayerState(Player player) {
        try {
            Bukkit.getLogger().log(Level.INFO, "§6[DEBUG] --- PLAYER STATE ANALYSIS ---");
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Player: " + player.getName());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] World: " + player.getWorld().getName());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Location: " + player.getLocation().toString());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Gamemode: " + player.getGameMode());
            Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Health: " + player.getHealth());
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] Player state analysis failed: " + e.getMessage());
        }
    }
    
    private static void checkAPIAccess() {
        try {
            Bukkit.getLogger().log(Level.INFO, "§6[DEBUG] --- API ACCESS CHECK ---");
            
            // Try to access BedWars1058 API
            try {
                Class<?> apiClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
                Bukkit.getLogger().log(Level.INFO, "§a[DEBUG] BedWars API class found: " + apiClass.getName());
                
                Method[] methods = apiClass.getDeclaredMethods();
                for (Method method : methods) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        Bukkit.getLogger().log(Level.INFO, "§e[DEBUG] Static API method: " + method.getName());
                    }
                }
                
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] BedWars API class not found");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "§c[DEBUG] API access check failed: " + e.getMessage());
        }
    }
}

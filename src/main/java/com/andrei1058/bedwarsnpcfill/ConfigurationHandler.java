package com.andrei1058.bedwarsnpcfill;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigurationHandler {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigurationHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getSpawnThreshold() {
        return config.getInt("spawnThresholds", 2);
    }

    public double getCombatRadius() {
        return config.getDouble("combatRadius", 10.0);
    }

    public double getBridgingRange() {
        return config.getDouble("bridgingRange", 15.0);
    }

    public long getBridgingPlaceCooldownMs() {
        return config.getLong("bridging.placeCooldownMs", 500L);
    }

    public long getBridgingStuckThresholdMs() {
        return config.getLong("bridging.stuckThresholdMs", 5000L);
    }

    public boolean isPreemptiveBridgingEnabled() {
        return config.getBoolean("bridging.preemptive", true);
    }

    public long getBridgingTickIntervalTicks() {
        return config.getLong("bridging.tickIntervalTicks", 6L);
    }

    public double getBedBreakProximity() {
        return config.getDouble("bedBreakProximity", 3.0);
    }
    
    public int getStartDelay() {
        return config.getInt("startDelay", 40);
    }

    public int getNPCAttackRateTicks() {
        // default 30 ticks (1.5 seconds) between attacks
        return config.getInt("npc.attackRateTicks", 30);
    }
    
    public double getNPCAttackDamage() {
        // default 4.0 damage per hit (2 hearts)
        return config.getDouble("npc.attackDamage", 4.0);
    }
    
    public double getNPCMovementSpeed() {
        // default 0.5 (half speed)
        return config.getDouble("npc.movementSpeed", 0.5);
    }
    
    public double getNPCBridgingSpeed() {
        // default 0.4 blocks per tick during bridging
        return config.getDouble("npc.bridgingSpeed", 0.4);
    }
    
    public String getBedBrokenTitle() {
        return config.getString("titles.bedBrokenTitle", "BED BROKEN");
    }
    
    public String getBedBrokenSubtitle() {
        return config.getString("titles.bedBrokenSubtitle", "NPC has broken your bed");
    }
}

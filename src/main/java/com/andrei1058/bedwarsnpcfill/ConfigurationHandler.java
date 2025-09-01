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

    public double getBedBreakProximity() {
        return config.getDouble("bedBreakProximity", 3.0);
    }
    
    public int getStartDelay() {
        return config.getInt("start-delay", 40);
    }
}

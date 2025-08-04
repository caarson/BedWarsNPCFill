package com.andrei1058.bedwarsnpcfill;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationHandler {
    private final BedWarsNPCFillPlugin plugin;
    private FileConfiguration config;
    
    // Spawn thresholds
    private int minPlayersToSpawn;
    private int maxNPCsPerTeam;
    
    // Combat behavior
    private int combatRadius;
    private int bridgingRange;
    
    // Bed breaking proximity
    private int bedBreakProximity;
    
    // Bed defense template
    private List<BlockOffset> bedDefenseTemplate;
    
    // Titles and messages
    private String bedBrokenTitle;
    private String bedBrokenSubtitle;
    
    public ConfigurationHandler(BedWarsNPCFillPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        config = plugin.getConfig();
        
        // Spawn thresholds
        minPlayersToSpawn = config.getInt("spawnThresholds.minPlayers", 1);
        maxNPCsPerTeam = config.getInt("spawnThresholds.maxNPCsPerTeam", 3);
        
        // Combat behavior
        combatRadius = config.getInt("combatRadius", 10);
        bridgingRange = config.getInt("bridgingRange", 20);
        
        // Bed breaking proximity
        bedBreakProximity = config.getInt("bedBreakProximity", 5);
        
        // Bed defense template
        bedDefenseTemplate = new ArrayList<>();
        ConfigurationSection templateSection = config.getConfigurationSection("bedDefenseTemplate");
        if (templateSection != null) {
            for (String key : templateSection.getKeys(false)) {
                ConfigurationSection blockSection = templateSection.getConfigurationSection(key);
                if (blockSection != null) {
                    int x = blockSection.getInt("x", 0);
                    int y = blockSection.getInt("y", 0);
                    int z = blockSection.getInt("z", 0);
                    String type = blockSection.getString("type", "AIR");
                    bedDefenseTemplate.add(new BlockOffset(x, y, z, type));
                }
            }
        } else {
            // Default template
            bedDefenseTemplate.add(new BlockOffset(0, 0, 0, "BED"));
            bedDefenseTemplate.add(new BlockOffset(1, 0, 0, "OAK_PLANKS"));
            bedDefenseTemplate.add(new BlockOffset(-1, 0, 0, "OAK_PLANKS"));
            bedDefenseTemplate.add(new BlockOffset(0, 0, 1, "OAK_PLANKS"));
            bedDefenseTemplate.add(new BlockOffset(0, 0, -1, "OAK_PLANKS"));
        }
        
        // Titles and messages
        bedBrokenTitle = config.getString("titles.bedBrokenTitle", "BED BROKEN");
        bedBrokenSubtitle = config.getString("titles.bedBrokenSubtitle", "NPC has broken your bed");
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
    
    // Getters
    public int getMinPlayersToSpawn() { return minPlayersToSpawn; }
    public int getMaxNPCsPerTeam() { return maxNPCsPerTeam; }
    public int getCombatRadius() { return combatRadius; }
    public int getBridgingRange() { return bridgingRange; }
    public int getBedBreakProximity() { return bedBreakProximity; }
    public List<BlockOffset> getBedDefenseTemplate() { return bedDefenseTemplate; }
    public String getBedBrokenTitle() { return bedBrokenTitle; }
    public String getBedBrokenSubtitle() { return bedBrokenSubtitle; }
    
    // Inner class for block offsets
    public static class BlockOffset {
        private final int x, y, z;
        private final String type;
        
        public BlockOffset(int x, int y, int z, String type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
        }
        
        // Getters
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public String getType() { return type; }
    }
}

package com.andrei1058.bedwarsnpcfill;

import org.bukkit.Color;
import org.bukkit.Location;

public class TeamData {
    private final String name;
    private final int maxPlayers;
    private final Location spawnLocation;
    private final Location bedLocation;
    private int playerCount = 0;
    private final char colorCode;
    private final Color color;
    
    public TeamData(String name, int maxPlayers, Location spawnLocation, Location bedLocation, char colorCode, Color color) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.spawnLocation = spawnLocation;
        this.bedLocation = bedLocation;
        this.colorCode = colorCode;
        this.color = color;
    }
    
    public String getName() {
        return name;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    public Location getBedLocation() {
        return bedLocation;
    }
    
    public int getPlayerCount() {
        return playerCount;
    }
    
    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }
    
    public char getColorCode() {
        return colorCode;
    }
    
    public Color getColor() {
        return color;
    }
}

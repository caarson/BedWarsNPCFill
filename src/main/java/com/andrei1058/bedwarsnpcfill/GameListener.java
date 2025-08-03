package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.events.BedWarsTeamChangeEvent;
import org.bukkit.event.Listener;
import java.util.logging.Logger;

public class GameListener implements Listener {
    private final Logger logger = Logger.getLogger(GameListener.class.getName());
    
    @Override
    public void onBedWarsTeamChange(BedWarsTeamChangeEvent event) {
        // Check if team is not full and needs NPC filling
        if (event.getNewSize() < BedWars.MAX_PLAYERS_PER_TEAM) {
            // TODO: Spawn NPCs for this team
            logger.info("Team " + event.getTeamName() + " has " + event.getNewSize() + " players, requesting NPCs");
        }
    }

    // Add more event handlers as needed
}

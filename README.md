# BedWarsNPCFill Plugin

A lightweight Minecraft BedWars plugin that fills empty teams with NPCs to keep low-population games playable.

## Features
- Spawns NPCs to fill unfilled teams
- Basic NPC combat targeting nearest players
- Simple bridging towards nearby players
- Proximity-based bed breaking simulation
- Auto bed defense placement
- Configurable parameters for gameplay tuning

## Installation
1. Place the compiled `.jar` file in your server's `plugins/` folder
2. Start your server to generate the default `config.yml`
3. Configure settings in `plugins/BedWarsNPCFill/config.yml`
4. Restart the server

## Configuration Options (`config.yml`)
```yaml
# Spawn thresholds
spawnThresholds:
  minPlayers: 1       # Minimum players to start spawning NPCs
  maxNPCsPerTeam: 3   # Maximum NPCs per team

# Combat behavior
combatRadius: 10      # Attack radius
bridgingRange: 20     # How far NPCs will bridge

# Bed breaking
bedBreakProximity: 5  # Radius around bed to trigger break

# Bed defense template
bedDefenseTemplate:
  - {x: -1, y: 0, z: -1, type: OAK_PLANKS}
  - {x: -1, y: 0, z: 0, type: OAK_PLANKS}
  - {x: -1, y: 0, z: 1, type: OAK_PLANKS}
  - {x: 0, y: 0, z: -1, type: OAK_PLANKS}
  - {x: 0, y: 0, z: 1, type: OAK_PLANKS}
  - {x: 1, y: 0, z: -1, type: OAK_PLANKS}
  - {x: 1, y: 0, z: 0, type: OAK_PLANKS}
  - {x: 1, y: 0, z: 1, type: OAK_PLANKS}

# Titles/messages
bedBreakTitle: "BED BROKEN"
bedBreakSubtitle: "NPC has broken your bed"
```

## Limitations
- Basic NPC behavior with no obstacle-aware pathfinding
- Proximity-based actions only
- Simulated bed breaking (triggers event but doesn't destroy blocks)
- Minimal performance optimizations

## Dependencies
- Spigot/Paper 1.16.5+
- BedWars 1058

## Building from Source
1. Clone the repository
2. Run `gradle build`
3. Find the compiled jar in `build/libs/`

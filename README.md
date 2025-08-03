# BedWars NPC Fill Plugin

A lightweight plugin for filling empty BedWars teams with simple NPCs to keep low-population games playable.

## Features

- Detects unfilled BedWars teams and spawns NPCs
- Basic combat behavior using Citizens/Sentinel-style targeting
- Simple bridging towards nearby players
- Bed-breaking simulation within configurable proximity
- Configurable bed defense structures
- Minimal API for future expansion

## Requirements

- Spigot/Paper 1.8+ 
- BedWars 1058
- Citizens (optional for enhanced NPC behavior)

## Installation

1. Place the compiled `.jar` file in your server's `plugins/` folder
2. Configure using `config.yml`

## Configuration Options (in config.yml)

### Spawn thresholds:
```
spawnThresholds:
  minPlayers: 1       # Minimum number of players to start spawning NPCs
  maxNPCsPerTeam: 3   # Maximum number of NPCs per team
```

### Combat behavior:
```
combatRadius: 10      # Radius around player where NPC will attack
bridgingRange: 20     # How far NPCs will bridge towards players
```

### Bed breaking:
```
bedBreakProximity: 5   # Block radius around bed to trigger break
```

### Bed defense template:
```
bedDefenseTemplate:
  - {x: 0, y: 0, z: 0, type: BED}
  - {x: 1, y: 0, z: 0, type: PLANKS}
  - {x: -1, y: 0, z: 0, type: PLANKS}
```

## Limitations

- Basic NPC behavior with no advanced pathfinding
- Proximity-based actions only (no obstacle awareness)
- Simulated bed breaking (triggers event but doesn't destroy blocks)
- Minimal performance optimizations

## TODO

1. Implement NPC cleanup when game ends or team is eliminated
2. Add proper Citizens/Sentinel fallback logic
3. Test and optimize performance
4. Add more configurable templates for different gameplay styles

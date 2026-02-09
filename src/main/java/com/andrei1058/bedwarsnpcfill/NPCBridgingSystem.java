package com.andrei1058.bedwarsnpcfill;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Bridging and movement system for NPCs.
 * Uses direct TELEPORTATION for movement — every 2 ticks, we calculate the NPC's
 * next position and teleport it there. This completely bypasses Citizens' Navigator
 * AND NMS movement internals (EntityHumanNPC.doTick / travel / moveLogic), which
 * all fail to produce actual position changes for Player-type NPCs.
 *
 * Why teleport instead of setVelocity:
 *   EntityHumanNPC.doTick() calls moveWithFallDamage(Vec3.ZERO) -> travel(Vec3.ZERO).
 *   Even with flyable=true, the NMS ServerPlayer movement pipeline consumes/zeroes
 *   our velocity before entity.move() processes it, so the NPC never physically moves.
 *   Teleportation directly sets position and is 100% reliable.
 *
 * Running at 2-tick intervals (10x/sec) produces smooth-looking movement.
 * A separate bridging sub-task places wool blocks ahead of the NPC.
 */
public class NPCBridgingSystem {

    private static final Set<NPC> activeNPCs = new HashSet<>();
    private static BukkitRunnable targetingTask;
    private static BukkitRunnable movementAndBridgingTask;

    // Track per-NPC state
    private static final java.util.Map<NPC, Player> currentTargets = new java.util.HashMap<>();
    // Track Y velocity per NPC for gravity simulation
    private static final java.util.Map<NPC, Double> npcYVelocity = new java.util.HashMap<>();

    // Constants
    private static final double CLOSE_RANGE = 3.0;       // Stop/attack range (melee)
    private static final double BRIDGE_MIN_DIST = 4.0;    // Only bridge when target is further than this
    private static final double GRAVITY = -0.08;          // Minecraft gravity per tick
    private static final double TERMINAL_VELOCITY = -3.0; // Max fall speed

    /**
     * Start the bridging and movement system for all NPCs.
     * Two tasks:
     *   - Targeting task: every 20 ticks (1 second) — finds the nearest enemy player
     *   - Movement+Bridging task: every 2 ticks — teleports NPC toward target and places bridge blocks
     */
    public static void startBridgingSystem() {
        if (targetingTask != null) {
            targetingTask.cancel();
        }
        if (movementAndBridgingTask != null) {
            movementAndBridgingTask.cancel();
        }

        // ── Targeting task: every 20 ticks, find nearest enemy player ──
        targetingTask = new BukkitRunnable() {
            private int tickCount = 0;

            @Override
            public void run() {
                tickCount++;

                if (tickCount % 15 == 0) { // Debug every ~15 seconds
                    Bukkit.getLogger().info("[BedWarsNPCFill] Targeting tick - tracking " + activeNPCs.size() + " NPCs");
                }

                for (NPC npc : new java.util.ArrayList<>(activeNPCs)) {
                    if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
                        updateTarget(npc);
                    }
                }
            }
        };
        targetingTask.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 10L, 20L);

        // ── Movement + Bridging task: every 2 ticks for smooth movement ──
        movementAndBridgingTask = new BukkitRunnable() {
            private int bridgeCounter = 0;

            @Override
            public void run() {
                bridgeCounter++;

                for (NPC npc : new java.util.ArrayList<>(activeNPCs)) {
                    if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
                        // Teleport NPC toward target every run (every 2 game ticks)
                        moveNPCTowardTarget(npc);

                        // Place bridge blocks every 3rd run (every 6 game ticks)
                        if (bridgeCounter % 3 == 0) {
                            placeBridgeBlocks(npc);
                        }
                    }
                }
            }
        };
        movementAndBridgingTask.runTaskTimer(BedWarsNPCFillPlugin.getInstance(), 5L, 2L);

        Bukkit.getLogger().info("[BedWarsNPCFill] Started NPC movement+bridging system (teleport-based, 20-tick targeting, 2-tick movement)");
    }

    /**
     * Find the nearest enemy player and store as the NPC's current target.
     */
    private static void updateTarget(NPC npc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;

            Location npcLoc = entity.getLocation();
            Player target = findNearestTarget(npc, npcLoc);
            currentTargets.put(npc, target);

            if (target != null) {
                double dist = npcLoc.distance(target.getLocation());
                Bukkit.getLogger().info("[BedWarsNPCFill] Target for " + npc.getName()
                    + " -> " + target.getName() + " (dist=" + String.format("%.1f", dist) + ")");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BedWarsNPCFill] Targeting error for " + npc.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Move NPC toward target using DIRECT TELEPORTATION.
     * Every 2 ticks we calculate a new position and teleport the NPC there.
     * This bypasses all NMS movement internals that block setVelocity() for Player NPCs.
     *
     * Movement logic:
     * - Calculate horizontal direction toward target
     * - Move a fixed distance per tick in that direction
     * - Handle gravity: if no solid block below, fall
     * - Handle jumping: if block ahead at foot level with air above, step up
     * - Face the target
     * - Stop when within melee range
     */
    private static void moveNPCTowardTarget(NPC npc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;

            Player target = currentTargets.get(npc);
            if (target == null || !target.isOnline() || target.isDead()) return;

            Location npcLoc = entity.getLocation();
            Location targetLoc = target.getLocation();

            // Calculate horizontal distance
            double dx = targetLoc.getX() - npcLoc.getX();
            double dz = targetLoc.getZ() - npcLoc.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // Already in melee range — don't move, let Sentinel handle combat
            if (horizontalDist <= CLOSE_RANGE) {
                // Face the target even when standing still
                npc.faceLocation(targetLoc);
                // Still apply gravity even when standing still
                applyGravity(npc, entity, npcLoc);
                return;
            }

            // Get configured speed
            double speed = BedWarsNPCFillPlugin.getInstance().getConfigHandler().getNPCMovementSpeed();
            // This runs every 2 ticks. Config 0.5 → 0.20 blocks per run → ~2 blocks/sec (natural walk)
            // Scale by 2 because we run every 2 ticks (need to cover 2 ticks worth of distance)
            double moveSpeed = speed * 0.4;

            // Cap to prevent glitchy long-distance teleports
            if (moveSpeed > 0.5) moveSpeed = 0.5;

            // Don't overshoot the target
            if (moveSpeed > horizontalDist) moveSpeed = horizontalDist;

            // Normalize direction
            double nx = dx / horizontalDist;
            double nz = dz / horizontalDist;

            // Calculate new position (horizontal)
            double newX = npcLoc.getX() + nx * moveSpeed;
            double newZ = npcLoc.getZ() + nz * moveSpeed;
            double newY = npcLoc.getY();

            // ── Ground-following: find the ground level at (newX, newZ) ──
            // Scan from 1 block above current Y (allow step-up) down to 10 blocks below (allow step-down)
            // This lets NPCs walk up stairs, down slopes, off edges, and descend to lower islands.
            int scanTop = (int) Math.ceil(npcLoc.getY()) + 1;  // 1 block step-up
            int scanBottom = (int) Math.floor(npcLoc.getY()) - 10; // up to 10 blocks step-down
            if (scanBottom < 0) scanBottom = 0; // Don't scan below world
            double foundGroundY = -999;

            for (int y = scanTop; y >= scanBottom; y--) {
                Block checkBlock = new Location(npcLoc.getWorld(), newX, y, newZ).getBlock();
                Block aboveBlock = new Location(npcLoc.getWorld(), newX, y + 1, newZ).getBlock();
                Block headRoom  = new Location(npcLoc.getWorld(), newX, y + 2, newZ).getBlock();

                // Found solid ground with 2 blocks of air above (room for the NPC)
                if (checkBlock.getType().isSolid() && !aboveBlock.getType().isSolid() && !headRoom.getType().isSolid()) {
                    foundGroundY = y + 1.0; // Stand on top of this block
                    break;
                }
            }

            if (foundGroundY > -999) {
                // Found ground at the destination — walk/step to it
                newY = foundGroundY;
                npcYVelocity.put(npc, 0.0);
            } else {
                // No ground found within scan range — NPC is going off an edge, apply gravity
                double yVel = npcYVelocity.getOrDefault(npc, 0.0);
                yVel += GRAVITY * 2; // 2 ticks worth of gravity
                if (yVel < TERMINAL_VELOCITY) yVel = TERMINAL_VELOCITY;
                npcYVelocity.put(npc, yVel);
                newY += yVel;

                // Snap to ground if we'd pass through a solid block
                Block landingBlock = new Location(npcLoc.getWorld(), newX, newY - 0.1, newZ).getBlock();
                if (landingBlock.getType().isSolid()) {
                    newY = landingBlock.getY() + 1.0;
                    npcYVelocity.put(npc, 0.0);
                }
            }

            // Calculate facing direction (yaw) toward target
            double yawDx = targetLoc.getX() - newX;
            double yawDz = targetLoc.getZ() - newZ;
            float yaw = (float) Math.toDegrees(Math.atan2(-yawDx, yawDz));
            float pitch = 0; // Look straight ahead

            // Create the new location with facing direction
            Location newLoc = new Location(npcLoc.getWorld(), newX, newY, newZ, yaw, pitch);

            // Teleport the NPC to the new position
            entity.teleport(newLoc);

            // Also tell Citizens to face the target (updates head rotation)
            npc.faceLocation(targetLoc);

        } catch (Exception e) {
            // Silent fail — don't spam console every 2 ticks
        }
    }

    /**
     * Apply gravity to an NPC that is standing still (in melee range but might be in air).
     */
    private static void applyGravity(NPC npc, Entity entity, Location npcLoc) {
        try {
            Block below = npcLoc.clone().add(0, -0.1, 0).getBlock();
            if (!below.getType().isSolid()) {
                double yVel = npcYVelocity.getOrDefault(npc, 0.0);
                yVel += GRAVITY * 2;
                if (yVel < TERMINAL_VELOCITY) yVel = TERMINAL_VELOCITY;
                npcYVelocity.put(npc, yVel);

                double newY = npcLoc.getY() + yVel;

                // Snap to ground
                Block landingBlock = new Location(npcLoc.getWorld(), npcLoc.getX(), newY - 0.1, npcLoc.getZ()).getBlock();
                if (landingBlock.getType().isSolid()) {
                    newY = landingBlock.getY() + 1.0;
                    npcYVelocity.put(npc, 0.0);
                }

                Location fallLoc = new Location(npcLoc.getWorld(), npcLoc.getX(), newY, npcLoc.getZ(),
                        npcLoc.getYaw(), npcLoc.getPitch());
                entity.teleport(fallLoc);
            } else {
                npcYVelocity.put(npc, 0.0);
            }
        } catch (Exception ignore) {}
    }

    /**
     * Place bridge blocks under and ahead of the NPC toward its target.
     */
    @SuppressWarnings("deprecation")
    private static void placeBridgeBlocks(NPC npc) {
        try {
            Entity entity = npc.getEntity();
            if (entity == null) return;

            Location npcLoc = entity.getLocation();

            // Need a target to bridge toward
            Player target = currentTargets.get(npc);
            if (target == null) {
                target = findNearestTarget(npc, npcLoc);
            }
            if (target == null) return;

            Location targetLoc = target.getLocation();
            double horizontalDist = Math.sqrt(
                Math.pow(targetLoc.getX() - npcLoc.getX(), 2) +
                Math.pow(targetLoc.getZ() - npcLoc.getZ(), 2)
            );

            byte woolColor = getTeamWoolColor(npc);
            IArena arena = getArenaFromNPC(npc);

            // Only place platform under the NPC if there is NO natural ground below
            // (i.e., NPC is bridging over void). If there IS ground within 6 blocks
            // below, skip the platform so the NPC can step down naturally.
            boolean hasGroundBelow = false;
            for (int checkY = 1; checkY <= 6; checkY++) {
                Block checkBlock = npcLoc.clone().add(0, -checkY, 0).getBlock();
                if (checkBlock.getType().isSolid()) {
                    hasGroundBelow = true;
                    break;
                }
            }
            if (!hasGroundBelow) {
                // Over void — place safety platform
                ensurePlatformAround(npcLoc, woolColor, npc);
            }

            // Only build bridge ahead when crossing void (target is at same level or above)
            // If target is significantly below us, DON'T bridge ahead — let the NPC descend naturally
            double yDiff = targetLoc.getY() - npcLoc.getY();
            if (horizontalDist > BRIDGE_MIN_DIST && yDiff >= -2.0) {
                placeBridgeBlocksToward(npc, npcLoc, targetLoc, woolColor, arena);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Place bridge blocks in a line from NPC towards a target location.
     * Creates a 3-wide path of colored wool.
     */
    @SuppressWarnings("deprecation")
    private static void placeBridgeBlocksToward(NPC npc, Location from, Location to, byte woolColor, IArena arena) {
        try {
            if (arena == null) {
                arena = getArenaFromNPC(npc);
            }

            Vector direction = to.toVector().subtract(from.toVector());
            direction.setY(0);
            if (direction.lengthSquared() == 0) return;
            direction = direction.normalize();

            Vector side = new Vector(-direction.getZ(), 0, direction.getX());

            // Calculate Y slope: bridge should gradually move toward target's Y level
            double yDiff = to.getY() - from.getY();
            double totalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
            // Clamp slope so bridge doesn't go too steeply (max 0.5 blocks down per block forward)
            double slopePerBlock = 0;
            if (totalDist > 0) {
                slopePerBlock = yDiff / totalDist;
                if (slopePerBlock < -0.5) slopePerBlock = -0.5;
                if (slopePerBlock > 0.5) slopePerBlock = 0.5;
            }

            // Place blocks ahead in a line (up to 8 blocks for smoother bridging)
            for (int i = 0; i <= 8; i++) {
                double bx = direction.getX() * i;
                double bz = direction.getZ() * i;
                // Y offset slopes toward target level
                double by = -1 + (slopePerBlock * i);

                // Center block
                placeWoolBlock(from.clone().add(bx, by, bz), woolColor, arena);

                // Side blocks (1 block each side for 3-wide path)
                placeWoolBlock(from.clone().add(bx + side.getX(), by, bz + side.getZ()), woolColor, arena);
                placeWoolBlock(from.clone().add(bx - side.getX(), by, bz - side.getZ()), woolColor, arena);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Place a single wool block if the location is air.
     */
    @SuppressWarnings("deprecation")
    private static void placeWoolBlock(Location loc, byte woolColor, IArena arena) {
        Block block = loc.getBlock();
        if (block.getType() == Material.AIR) {
            block.setType(Material.WOOL);
            block.setData(woolColor);
            if (arena != null) {
                arena.addPlacedBlock(block);
            }
        }
    }

    /**
     * Get the arena from an NPC's name.
     */
    private static IArena getArenaFromNPC(NPC npc) {
        try {
            String npcName = npc.getName();
            // NPC name format: NPC_arenaName_teamName
            String[] parts = npcName.split("_");
            if (parts.length >= 2) {
                String arenaName = parts[1];
                return Arena.getArenaByName(arenaName);
            }
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }

    /**
     * Find the nearest enemy player target for an NPC.
     */
    private static Player findNearestTarget(NPC npc, Location npcLoc) {
        Player target = null;
        double closestDist = 200.0;

        for (Player p : npcLoc.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.hasMetadata("NPC")) continue;

            double dist = p.getLocation().distance(npcLoc);
            if (dist < closestDist) {
                closestDist = dist;
                target = p;
            }
        }
        return target;
    }

    /**
     * Get wool color data value based on NPC's team.
     */
    @SuppressWarnings("deprecation")
    private static byte getTeamWoolColor(NPC npc) {
        try {
            String npcName = npc.getName();
            String[] parts = npcName.split("_");
            if (parts.length >= 3) {
                String teamName = parts[2].toLowerCase();

                switch (teamName) {
                    case "red": return 14;
                    case "blue": return 11;
                    case "green": return 13;
                    case "yellow": return 4;
                    case "aqua":
                    case "cyan": return 9;
                    case "white": return 0;
                    case "pink": return 6;
                    case "gray":
                    case "grey": return 7;
                    case "orange": return 1;
                    case "purple": return 10;
                    case "lime": return 5;
                    case "black": return 15;
                    case "brown": return 12;
                    case "magenta": return 2;
                    case "lightblue":
                    case "light_blue": return 3;
                    default: return 0;
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return 0;
    }

    /**
     * Ensure a 3x3 platform exists under a location with colored wool.
     */
    @SuppressWarnings("deprecation")
    private static void ensurePlatformAround(Location loc, byte woolColor, NPC npc) {
        IArena arena = getArenaFromNPC(npc);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = loc.clone().add(x, -1, z);
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WOOL);
                    block.setData(woolColor);
                    if (arena != null) {
                        arena.addPlacedBlock(block);
                    }
                }
            }
        }
    }

    /**
     * Stop the bridging and movement system.
     */
    public static void stopBridgingSystem() {
        if (targetingTask != null) {
            targetingTask.cancel();
            targetingTask = null;
        }
        if (movementAndBridgingTask != null) {
            movementAndBridgingTask.cancel();
            movementAndBridgingTask = null;
        }
        activeNPCs.clear();
        currentTargets.clear();
        npcYVelocity.clear();
        Bukkit.getLogger().info("[BedWarsNPCFill] Stopped NPC movement+bridging system");
    }

    /**
     * Add an NPC to the movement+bridging system.
     */
    public static void addNPCToBridging(NPC npc) {
        if (npc != null) {
            activeNPCs.add(npc);
            npcYVelocity.put(npc, 0.0);

            // Cancel any Citizens navigation that might interfere
            try {
                if (npc.getNavigator().isNavigating()) {
                    npc.getNavigator().cancelNavigation();
                }
            } catch (Exception ignore) {}

            Bukkit.getLogger().info("[BedWarsNPCFill] Added " + npc.getName() + " to teleport-based movement system. Total NPCs: " + activeNPCs.size());
        }
    }

    /**
     * Remove an NPC from the movement+bridging system.
     */
    public static void removeNPCFromBridging(NPC npc) {
        if (npc != null) {
            activeNPCs.remove(npc);
            currentTargets.remove(npc);
            npcYVelocity.remove(npc);
        }
    }
}

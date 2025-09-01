package org.mcmonkey.sentinel.targeting;

import net.citizensnpcs.api.astar.AStarMachine;
import net.citizensnpcs.api.astar.pathfinder.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mcmonkey.sentinel.*;
import org.mcmonkey.sentinel.events.SentinelNoMoreTargetsEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Helper for targeting logic on an NPC.
 */
public class SentinelTargetingHelper extends SentinelHelperObject {

    /**
     * Returns whether the NPC can see the target entity.
     */
    public boolean canSee(LivingEntity entity) {
        if (!getLivingEntity().getWorld().equals(entity.getWorld())) {
            return false;
        }
        if (getLivingEntity().getEyeLocation().distanceSquared(entity.getEyeLocation()) > sentinel.range * sentinel.range) {
            return false;
        }
        if (sentinel.realistic && !SentinelUtilities.isLookingTowards(getLivingEntity().getEyeLocation(), entity.getLocation(), 90, 110)) {
            return false;
        }
        if (!sentinel.ignoreLOS && !SentinelUtilities.checkLineOfSightWithTransparency(getLivingEntity(), entity)) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether the NPC should target a specific entity.
     */
    public boolean shouldTarget(LivingEntity entity) {
        if (entity.getUniqueId().equals(getLivingEntity().getUniqueId())) {
            return false;
        }
        if (entity.getType() == EntityType.ARMOR_STAND && !SentinelPlugin.instance.allowArmorStandTargets) {
            return false;
        }
        return isTargeted(entity) && !isIgnored(entity);
    }

    /**
     * Returns whether the NPC should avoid a specific entity.
     */
    public boolean shouldAvoid(LivingEntity entity) {
        if (entity.getUniqueId().equals(getLivingEntity().getUniqueId())) {
            return false;
        }
        if (entity.getType() == EntityType.ARMOR_STAND && !SentinelPlugin.instance.allowArmorStandTargets) {
            return false;
        }
        return isAvoided(entity) && !isIgnored(entity);
    }

    /**
     * The set of all current targets for this NPC.
     */
    public HashMap<UUID, SentinelCurrentTarget> currentTargets = new HashMap<>();

    /**
     * The set of all current avoids for this NPC.
     */
    public HashMap<UUID, SentinelCurrentTarget> currentAvoids = new HashMap<>();

    /**
     * Adds a temporary avoid to this NPC.
     */
    public void addAvoid(UUID id) {
        if (id.equals(getLivingEntity().getUniqueId())) {
            return;
        }
        if (!(SentinelUtilities.getEntityForID(id) instanceof LivingEntity)) {
            return;
        }
        ensureTargetDirect(currentAvoids, SentinelPlugin.instance.runAwayTime, id);
    }

    /**
     * Adds a temporary target to this NPC (and squadmates if relevant).
     */
    public void addTarget(UUID id) {
        if (id.equals(getLivingEntity().getUniqueId())) {
            return;
        }
        if (!(SentinelUtilities.getEntityForID(id) instanceof LivingEntity)) {
            return;
        }
        addTargetNoBounce(id);
        if (sentinel.squad != null) {
            for (SentinelTrait squadMate : SentinelPlugin.instance.cleanCurrentList()) {
                if (squadMate.squad != null && squadMate.squad.equals(sentinel.squad)) {
                    squadMate.targetingHelper.addTargetNoBounce(id);
                }
            }
        }
    }

    /**
     * Removes a temporary target from this NPC (and squadmates if relevant).
     * Returns whether anything was removed.
     */
    public boolean removeTarget(UUID id) {
        boolean removed = removeTargetNoBounce(id);
        if (removed && sentinel.squad != null) {
            for (SentinelTrait squadMate : SentinelPlugin.instance.cleanCurrentList()) {
                if (squadMate.squad != null && squadMate.squad.equals(sentinel.squad)) {
                    squadMate.targetingHelper.removeTargetNoBounce(id);
                }
            }
        }
        return removed;
    }

    /**
     * Removes a target directly from the NPC. Prefer {@code removeTarget} over this in most cases.
     * Returns whether anything was removed.
     */
    public boolean removeTargetNoBounce(UUID target) {
        if (currentTargets.remove(target) != null) {
            if (currentTargets.isEmpty()) {
                Bukkit.getPluginManager().callEvent(new SentinelNoMoreTargetsEvent(getNPC()));
            }
            return true;
        }
        return false;
    }

    private void ensureTargetDirect(HashMap<UUID, SentinelCurrentTarget> set, long time, UUID id) {
        SentinelCurrentTarget target = set.get(id);
        if (target == null) {
            target = new SentinelCurrentTarget();
            target.targetID = id;
            target.hasLos = true;
            set.put(id, target);
        }
        target.ticksLeft = time;
    }

    /**
     * Informs the tracker that the given currentTarget UUID is an entity that cannot currently be seen.
     */
    public void informTargetHasNoLos(UUID id) {
        SentinelCurrentTarget target = currentTargets.get(id);
        if (target != null) {
            target.hasLos = false;
        }
    }

    /**
     * Adds a target directly to the NPC. Prefer {@code addTarget} over this in most cases.
     */
    public void addTargetNoBounce(UUID id) {
        if (sentinel.reactionSlowdown == 0) {
            ensureTargetDirect(currentTargets, sentinel.enemyTargetTime, id);
        }
        else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(SentinelPlugin.instance, () -> {
                ensureTargetDirect(currentTargets, sentinel.enemyTargetTime, id);
            }, sentinel.reactionSlowdown);
        }
    }

    /**
     * Returns whether an entity is invisible to this NPC.
     */
    public boolean isInvisible(LivingEntity entity) {
        return !currentTargets.containsKey(entity.getUniqueId()) && SentinelUtilities.isInvisible(entity);
    }

    /**
     * Returns whether an entity is ignored by this NPC's ignore lists.
     */
    public boolean isIgnored(LivingEntity entity) {
        if (isUntargetable(entity)) {
            return true;
        }
        if (isInvisible(entity)) {
            return true;
        }
        if (entity.getUniqueId().equals(getLivingEntity().getUniqueId())) {
            return true;
        }
        if (entity.getType() == EntityType.ARMOR_STAND && !SentinelPlugin.instance.allowArmorStandTargets) {
            return true;
        }
        if (sentinel.getGuarding() != null && SentinelUtilities.uuidEquals(entity.getUniqueId(), sentinel.getGuarding())) {
            return true;
        }
        return sentinel.allIgnores.isTarget(entity, sentinel);
    }

    /**
     * Returns whether an entity is targeted by this NPC's target lists.
     * Consider calling 'shouldTarget' instead.
     */
    public boolean isTargeted(LivingEntity entity) {
        if (isInvisible(entity)) {
            return false;
        }
        if (entity.getUniqueId().equals(getLivingEntity().getUniqueId())) {
            return false;
        }
        if (sentinel.getGuarding() != null && SentinelUtilities.uuidEquals(entity.getUniqueId(), sentinel.getGuarding())) {
            return false;
        }
        if (isUntargetable(entity)) {
            return false;
        }
        if (currentTargets.containsKey(entity.getUniqueId())) {
            return true;
        }
        return sentinel.allTargets.isTarget(entity, sentinel);
    }

    /**
     * Returns whether an entity is marked to be avoided by this NPC's avoid lists.
     */
    public boolean isAvoided(LivingEntity entity) {
        if (isInvisible(entity)) {
            return false;
        }
        if (entity.getUniqueId().equals(getLivingEntity().getUniqueId())) {
            return false;
        }
        if (sentinel.getGuarding() != null && SentinelUtilities.uuidEquals(entity.getUniqueId(), sentinel.getGuarding())) {
            return false;
        }
        if (currentAvoids.containsKey(entity.getUniqueId())) {
            return true;
        }
        return sentinel.allAvoids.isTarget(entity, sentinel);
    }

    private ArrayList<LivingEntity> avoidanceList = new ArrayList<>();

    /**
     * Process avoid necessary avoidance. Builds a list of things we need to run away from, and then runs.
     */
    public void processAvoidance() {
        avoidanceList.clear();
        if (currentAvoids.isEmpty() && sentinel.allAvoids.totalTargetsCount() == 0) { // Opti
            return;
        }
        double range = sentinel.avoidRange + 10;
        for (Entity entity : getLivingEntity().getWorld().getNearbyEntities(getLivingEntity().getLocation(), range, 16, range)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            if (shouldAvoid((LivingEntity) entity)) {
                if (!currentAvoids.containsKey(entity.getUniqueId()) && !canSee((LivingEntity) entity)) {
                    continue;
                }
                avoidanceList.add((LivingEntity) entity);
                addAvoid(entity.getUniqueId());
            }
        }
        if (avoidanceList.isEmpty()) {
            return;
        }
        Location runTo = findBestRunSpot();
        if (runTo != null) {
            sentinel.pathTo(runTo);
            if (SentinelPlugin.debugMe) {
                sentinel.debug("Running from threats, movement vector: " +
                        runTo.clone().subtract(getLivingEntity().getLocation()).toVector().toBlockVector().toString());
            }
        }
        else {
            sentinel.debug("I have nowhere to run!");
        }
    }

    /**
     * Finds a spot this NPC should run to, to avoid threats. Returns null if there's nowhere to run.
     */
    public Location findBestRunSpot() {
        if (sentinel.avoidReturnPoint != null
                && sentinel.avoidReturnPoint.getWorld().equals(getLivingEntity().getWorld())) {
            sentinel.debug("I have an avoid return point, I'll go there");
            return sentinel.avoidReturnPoint.clone();
        }
        Location pos = sentinel.getGuardZone();
        if (!pos.getWorld().equals(getLivingEntity().getWorld())) {
            sentinel.debug("Run spot out-of-world, must teleport away!");
            // Emergency corrective measures...
            getNPC().getNavigator().cancelNavigation();
            Bukkit.getScheduler().scheduleSyncDelayedTask(SentinelPlugin.instance, () ->  getLivingEntity().teleport(sentinel.getGuardZone()), 1);
            return null;
        }
        LivingEntity closestThreat = null;
        double threatRangeSquared = 1000 * 1000;
        for (LivingEntity entity : avoidanceList) {
            double dist = entity.getLocation().distanceSquared(pos);
            if (dist < threatRangeSquared) {
                closestThreat = entity;
                threatRangeSquared = dist;
            }
        }
        if (closestThreat == null) {
            sentinel.debug("No threats in range, actually I won't run away");
            return null;
        }
        if (threatRangeSquared >= sentinel.avoidRange * sentinel.avoidRange) {
            sentinel.debug("Threats are getting close... holding my post.");
            return pos.clone();
        }
        sentinel.debug("I'll just pick a direction to run in I guess...");
        return runDirection(pos);
    }

    private double[] threatDists = new double[36];

    private static Vector[] directionReferenceVectors = new Vector[36];

    static {
        for (int i = 0; i < 36; i++) {
            double yaw = i * 10;
            // negative yaw in x because Minecraft worlds are inverted
            directionReferenceVectors[i] = new Vector(Math.sin(-yaw * (Math.PI / 180)), 0, Math.cos(yaw * (Math.PI / 180)));
        }
    }

    private static AStarMachine ASTAR = AStarMachine.createWithDefaultStorage();

    private static BlockExaminer examiner = new MinecraftBlockExaminer();

    /**
     * Returns a spot to run to if running in a certain direction.
     * Returns null if can't reasonably run that direction.
     */
    public static Location findSpotForRunDirection(Location start, double distance, Vector direction) {
        VectorGoal goal = new VectorGoal(start.clone().add(direction.clone().multiply(distance)), 4);
        VectorNode startNode = new VectorNode(goal, start, new ChunkBlockSource(start, (float)distance + 10), examiner);
        Path resultPath = (Path) ASTAR.runFully(goal, startNode, (int)(distance * 50));
        if (resultPath == null || resultPath.isComplete()) {
            return null;
        }
        Vector current = resultPath.getCurrentVector();
        while (!resultPath.isComplete()) {
            current = resultPath.getCurrentVector();
            resultPath.update(null);
        }
        return current.toLocation(start.getWorld());
    }

    /**
     * Returns a direction to run in, avoiding threatening entities as best as possible.
     * Returns a location of the spot to run to.
     * Returns null if nowhere to run.
     */
    public Location runDirection(Location center) {
        for (int i = 0; i < 36; i++) {
            threatDists[i] = 1000 * 1000;
        }
        double range = sentinel.avoidRange;
        Vector centerVec = center.toVector();
        for (LivingEntity entity : avoidanceList) {
            Vector relative = entity.getLocation().toVector().subtract(centerVec);
            for (int i = 0; i < 36; i++) {
                double dist = relative.distanceSquared(directionReferenceVectors[i].clone().multiply(range));
                if (dist < threatDists[i]) {
                    threatDists[i] = dist;
                }
            }
        }
        double longestDistance = 0;
        Location runTo = null;
        for (int i = 0; i < 36; i++) {
            if (threatDists[i] > longestDistance) {
                Location newRunTo = findSpotForRunDirection(center, range, directionReferenceVectors[i].clone());
                if (newRunTo != null) {
                    runTo = newRunTo;
                    longestDistance = threatDists[i];
                }
            }
        }
        if (SentinelPlugin.debugMe) {
            SentinelPlugin.instance.getLogger().info("(TEMP) Run to get threat distance: " + longestDistance + " to " + runTo + " from " + center.toVector());
        }
        return runTo;
    }

    /**
     * This method searches for the nearest targetable entity with direct line-of-sight.
     * Failing a direct line of sight, the nearest entity in range at all will be chosen.
     */
    public LivingEntity findBestTarget() {
        boolean ignoreGlow = itemHelper.usesSpectral(itemHelper.getHeldItem());
        double rangesquared = sentinel.range * sentinel.range;
        double crsq = sentinel.chaseRange * sentinel.chaseRange;
        Location pos = sentinel.getGuardZone();
        if (!pos.getWorld().equals(getLivingEntity().getWorld())) {
            // Emergency corrective measures...
            getNPC().getNavigator().cancelNavigation();
            Bukkit.getScheduler().scheduleSyncDelayedTask(SentinelPlugin.instance, () ->  getLivingEntity().teleport(sentinel.getGuardZone()), 1);
            return null;
        }
        if (sentinel.chasing != null && sentinel.retainTarget) {
            double dist = sentinel.chasing.getEyeLocation().distanceSquared(pos);
            if (dist < crsq && shouldTarget(sentinel.chasing) && sentinel.canPathTo(sentinel.chasing.getLocation())) {
                return sentinel.chasing;
            }
        }
        LivingEntity closest = null;
        boolean wasLos = false;
        for (Entity loopEnt : getLivingEntity().getWorld().getNearbyEntities(pos, sentinel.range, sentinel.range, sentinel.range)) {
            if (!(loopEnt instanceof LivingEntity)) {
                continue;
            }
            LivingEntity ent = (LivingEntity) loopEnt;
            if ((ignoreGlow && ent.isGlowing()) || ent.isDead()) {
                continue;
            }
            double dist = ent.getEyeLocation().distanceSquared(pos);
            SentinelCurrentTarget curTarg = null;
            if (dist < crsq && dist < rangesquared) {
                curTarg = currentTargets.get(ent.getUniqueId());
                if (curTarg != null && !sentinel.canPathTo(ent.getLocation())) {
                    curTarg = null;
                }
            }
            if (curTarg != null || (dist < rangesquared && shouldTarget(ent))) {
                boolean hasLos = canSee(ent);
                if (!hasLos && (curTarg == null || !curTarg.hasLos)) {
                    continue;
                }
                if (hasLos && curTarg != null) {
                    curTarg.hasLos = true;
                }
                if (curTarg == null && sentinel.reactionSlowdown != 0) {
                    addTarget(ent.getUniqueId());
                    continue;
                }
                if (!wasLos || hasLos) {
                    rangesquared = dist;
                    closest = ent;
                    wasLos = hasLos;
                }
            }
        }
        if (closest != null) {
            addTarget(closest.getUniqueId());
        }
        return closest;
    }

    /**
     * Process all current multi-targets.
     * This is an internal call as part of the main logic loop.
     */
    public void processAllMultiTargets() {
        processMultiTargets(sentinel.allTargets, TargetListType.TARGETS);
        processMultiTargets(sentinel.allAvoids, TargetListType.AVOIDS);
    }

    /**
     * The types of target lists.
     */
    public enum TargetListType {
        TARGETS, IGNORES, AVOIDS
    }

    /**
     * Process a specific set of multi-targets.
     * This is an internal call as part of the main logic loop.
     */
    public void processMultiTargets(SentinelTargetList baseList, TargetListType type) {
        if (type == null) {
            return;
        }
        if (baseList.byMultiple.isEmpty()) {
            return;
        }
        ArrayList<SentinelTargetList> subList = new ArrayList<>(baseList.byMultiple.size());
        for (SentinelTargetList list : baseList.byMultiple) {
            SentinelTargetList toAdd = list.duplicate();
            toAdd.recalculateCacheNoClear();
            subList.add(toAdd);
            if (SentinelPlugin.debugMe) {
                SentinelPlugin.instance.getLogger().info("Multi-Target Debug: " + toAdd.totalTargetsCount() + " at start: " + toAdd.toMultiTargetString());
            }
        }
        Location pos = sentinel.getGuardZone();
        for (Entity loopEnt : getLivingEntity().getWorld().getNearbyEntities(pos, sentinel.range, sentinel.range, sentinel.range)) {
            if (!(loopEnt instanceof LivingEntity)) {
                continue;
            }
            LivingEntity ent = (LivingEntity) loopEnt;
            if (ent.isDead()) {
                continue;
            }
            if (isIgnored(ent)) {
                continue;
            }
            if (!canSee(ent)) {
                continue;
            }
            for (SentinelTargetList lister : subList) {
                if (lister.ifIsTargetDeleteTarget(ent)) {
                    if (SentinelPlugin.debugMe) {
                        SentinelPlugin.instance.getLogger().info("Multi-Target Debug: " + ent.getName() + " (" + ent.getType().name() + ") checked off for a list.");
                    }
                    lister.tempTargeted.add(ent);
                    if (lister.totalTargetsCount() == 0) {
                        if (SentinelPlugin.debugMe) {
                            SentinelPlugin.instance.getLogger().info("Multi-Target Debug: " + lister.totalTargetsCount() + " completed: " + lister.toMultiTargetString());
                        }
                        for (LivingEntity subEnt : lister.tempTargeted) {
                            if (type == TargetListType.TARGETS) {
                                addTarget(subEnt.getUniqueId());
                            }
                            else if (type == TargetListType.AVOIDS) {
                                addAvoid(subEnt.getUniqueId());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a nearby target that can be hit with a melee attack.
     */
    public LivingEntity findQuickMeleeTarget() {
        double range = sentinel.reach * 0.75;
        Location pos = getLivingEntity().getEyeLocation();
        for (Entity loopEnt : getLivingEntity().getWorld().getNearbyEntities(pos, range, range, range)) {
            if (loopEnt instanceof LivingEntity && shouldTarget((LivingEntity) loopEnt)
                    && canSee((LivingEntity) loopEnt)) {
                return (LivingEntity) loopEnt;
            }
        }
        return null;
    }

    /**
     * Updates the current avoids set for the NPC.
     * This is an internal call as part of the main logic loop.
     */
    public void updateAvoids() {
        for (SentinelCurrentTarget curTarg : new ArrayList<>(currentAvoids.values())) {
            Entity e = SentinelUtilities.getEntityForID(curTarg.targetID);
            if (e == null) {
                currentAvoids.remove(curTarg.targetID);
                continue;
            }
            if (e.isDead()) {
                currentAvoids.remove(curTarg.targetID);
                continue;
            }
            if (curTarg.ticksLeft > 0) {
                curTarg.ticksLeft -= SentinelPlugin.instance.tickRate;
                if (curTarg.ticksLeft <= 0) {
                    currentAvoids.remove(curTarg.targetID);
                }
            }
        }
    }

    /**
     * Returns whether an entity is not able to be targeted at all.
     */
    public static boolean isUntargetable(Entity e) {
        if (e == null) {
            return true;
        }
        if (e.isDead()) {
            return true;
        }
        if (e instanceof Player) {
            GameMode mode = ((Player) e).getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the current targets set for the NPC.
     * This is an internal call as part of the main logic loop.
     */
    public void updateTargets() {
        for (SentinelCurrentTarget curTarg : new ArrayList<>(currentTargets.values())) {
            Entity e = SentinelUtilities.getEntityForID(curTarg.targetID);
            if (isUntargetable(e)) {
                removeTargetNoBounce(curTarg.targetID);
                continue;
            }
            if (!e.getWorld().equals(getLivingEntity().getWorld())) {
                removeTargetNoBounce(curTarg.targetID);
                continue;
            }
            double d = e.getLocation().distanceSquared(getLivingEntity().getLocation());
            if (d > sentinel.range * sentinel.range * 4 && d > sentinel.chaseRange * sentinel.chaseRange * 4) {
                removeTargetNoBounce(curTarg.targetID);
                continue;
            }
            if (e instanceof LivingEntity && isIgnored((LivingEntity) e)) {
                removeTargetNoBounce(curTarg.targetID);
                continue;
            }
            if (curTarg.ticksLeft > 0) {
                curTarg.ticksLeft -= SentinelPlugin.instance.tickRate;
                if (curTarg.ticksLeft <= 0) {
                    removeTargetNoBounce(curTarg.targetID);
                }
            }
        }
        if (sentinel.chasing != null) {
            if (!currentTargets.containsKey(sentinel.chasing.getUniqueId())) {
                if (sentinel.tryUpdateChaseTarget(null)) {
                    getNPC().getNavigator().cancelNavigation();
                }
            }
        }
    }
}

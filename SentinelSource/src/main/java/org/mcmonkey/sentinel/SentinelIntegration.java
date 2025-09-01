package org.mcmonkey.sentinel;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an integration of an external plugin or system into Sentinel.
 */
public class SentinelIntegration {

    /**
     * Gets the 'target help' data for this integration (empty string if not relevant).
     * Example format is: "myintegration:MY_TARGET_IDENTIFIER" like "squad:SQUAD_NAME" or "healthabove:PERCENTAGE"
     */
    public String getTargetHelp() {
        return "";
    }

    /**
     * Returns whether the values for a target should be automatically lowercased in advance.
     */
    public boolean shouldLowerCaseValue() {
        return false;
    }

    /**
     * Gets the list of target prefixes that this integration handles.
     * For a "squad:SQUAD_NAME" target, this should return: new String[] { "squad" }
     * For integrations that don't have targets, return new String[0];
     */
    public String[] getTargetPrefixes() {
        return new String[0];
    }

    /**
     * Returns whether an entity is a target of the integration label.
     */
    public boolean isTarget(LivingEntity ent, String prefix, String value) {
        return isTarget(ent, prefix + ":" + value);
    }

    /**
     * Returns whether an entity is a target of the integration label.
     */
    @Deprecated
    public boolean isTarget(LivingEntity ent, String text) {
        return false;
    }

    /**
     * Runs when an NPC intends to attack a target - return 'true' to indicate the integration ran its own attack methodology
     * (and no default attack handling is needed).
     */
    public boolean tryAttack(SentinelTrait st, LivingEntity ent) {
        return false;
    }

    /**
     * For autoswitch logic, return 'true' if the item should be considered a valid ranged weapon to swap to.
     * If Sentinel's core and all integrations return 'false', the item will be considered a melee weapon.
     */
    public boolean itemIsRanged(SentinelTrait sentinel, ItemStack item) {
        return false;
    }
}

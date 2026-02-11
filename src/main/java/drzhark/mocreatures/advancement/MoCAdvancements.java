/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Central place for Mo' Creatures advancement triggers.
 */
public final class MoCAdvancements {

    public static final MoCTameTrigger TAME_MOC_CREATURE = new MoCTameTrigger();

    private MoCAdvancements() {}

    /**
     * Call when a player has just tamed a Mo' Creatures entity (e.g. from MoCTools.tameWithName).
     * Only runs on server; no-op if player is not a ServerPlayer or entity is not a tameable MoC entity.
     */
    public static void triggerTame(ServerPlayer player, Entity entity) {
        if (player == null || entity == null) return;
        TAME_MOC_CREATURE.trigger(player, entity);
    }
}

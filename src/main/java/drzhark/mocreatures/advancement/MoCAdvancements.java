/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import drzhark.mocreatures.entity.hunter.MoCEntityManticorePet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Central place for Mo' Creatures advancement triggers.
 */
public final class MoCAdvancements {

    public static final MoCTameTrigger TAME_MOC_CREATURE = new MoCTameTrigger();
    public static final MoCManticoreEquippedTrigger MANTICORE_EQUIPPED = new MoCManticoreEquippedTrigger();
    public static final MoCBreedHorseTrigger BREED_MOC_HORSE = new MoCBreedHorseTrigger();
    public static final MoCObtainUnicornTrigger OBTAIN_UNICORN = new MoCObtainUnicornTrigger();
    public static final MoCObtainBatHorseTrigger OBTAIN_BAT_HORSE = new MoCObtainBatHorseTrigger();
    public static final MoCObtainNightmareTrigger OBTAIN_NIGHTMARE = new MoCObtainNightmareTrigger();
    public static final MoCObtainPegasusTrigger OBTAIN_PEGASUS = new MoCObtainPegasusTrigger();
    public static final MoCObtainDarkPegasusTrigger OBTAIN_DARK_PEGASUS = new MoCObtainDarkPegasusTrigger();

    private MoCAdvancements() {}

    /**
     * Call when a player has just tamed a Mo' Creatures entity (e.g. from MoCTools.tameWithName).
     * Only runs on server; no-op if player is not a ServerPlayer or entity is not a tameable MoC entity.
     */
    public static void triggerTame(ServerPlayer player, Entity entity) {
        if (player == null || entity == null) return;
        TAME_MOC_CREATURE.trigger(player, entity);
    }

    /**
     * Call when a tamed Manticore pet has both saddle and chest equipped (e.g. from MoCEntityBigCat.mobInteract).
     * Only runs on server; no-op if player is not the owner or manticore is not fully equipped.
     */
    public static void triggerManticoreEquipped(ServerPlayer player, MoCEntityManticorePet manticore) {
        if (player == null || manticore == null) return;
        MANTICORE_EQUIPPED.trigger(player, manticore);
    }

    /**
     * Call when a horse breeding produces a baby (e.g. from MoCEntityHorse).
     */
    public static void triggerBreedHorse(ServerPlayer player, int babyHorseTypeMoC) {
        if (player == null) return;
        BREED_MOC_HORSE.trigger(player, babyHorseTypeMoC);
    }

    /**
     * Call when a player successfully uses an Essence of Fire on a zorse, transforming it into a Nightmare (e.g. from MoCEntityHorse mobInteract).
     */
    public static void triggerObtainNightmare(ServerPlayer player) {
        if (player == null) return;
        OBTAIN_NIGHTMARE.trigger(player);
    }

    /**
     * Call when a player successfully uses an Essence of Light on a Nightmare, transforming it into a Unicorn (e.g. from MoCEntityHorse mobInteract).
     */
    public static void triggerObtainUnicorn(ServerPlayer player) {
        if (player == null) return;
        OBTAIN_UNICORN.trigger(player);
    }

    /**
     * Call when a player successfully uses an Essence of Darkness on a zorse, transforming it into a Bat Horse (e.g. from MoCEntityHorse mobInteract).
     */
    public static void triggerObtainBatHorse(ServerPlayer player) {
        if (player == null) return;
        OBTAIN_BAT_HORSE.trigger(player);
    }

    /**
     * Call when a player successfully uses an Essence of Light on a Bat Horse above cloud level,
     * transforming it into a Pegasus (e.g. from MoCEntityHorse mobInteract).
     */
    public static void triggerObtainPegasus(ServerPlayer player) {
        if (player == null) return;
        OBTAIN_PEGASUS.trigger(player);
    }

    /**
     * Call when a player successfully uses an Essence of Darkness on a Pegasus above cloud level,
     * transforming it into a Dark Pegasus (e.g. from MoCEntityHorse mobInteract).
     */
    public static void triggerObtainDarkPegasus(ServerPlayer player) {
        if (player == null) return;
        OBTAIN_DARK_PEGASUS.trigger(player);
    }

}

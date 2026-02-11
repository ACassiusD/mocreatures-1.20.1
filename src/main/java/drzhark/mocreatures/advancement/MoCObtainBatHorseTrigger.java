/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import com.google.gson.JsonObject;
import drzhark.mocreatures.MoCConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Advancement trigger for "Obtain a Bat Horse".
 * Fired when a player successfully uses an Essence of Darkness on a zorse (horse type 61),
 * transforming it into a Bat Horse (type 32).
 * Only grants if the player has completed "Breed a Zorse".
 */
public class MoCObtainBatHorseTrigger extends SimpleCriterionTrigger<MoCObtainBatHorseTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "obtain_bat_horse");
    private static final ResourceLocation BREED_ZORSE = new ResourceLocation(MoCConstants.MOD_ID, "breed_zorse_horse");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        return new TriggerInstance(playerPredicate);
    }

    public void trigger(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return;
        ServerAdvancementManager advMgr = server.getAdvancements();
        Advancement breedZorse = advMgr.getAdvancement(BREED_ZORSE);
        if (breedZorse == null || !player.getAdvancements().getOrStartProgress(breedZorse).isDone()) return;
        this.trigger(player, instance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        public TriggerInstance(ContextAwarePredicate playerPredicate) {
            super(ID, playerPredicate);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            return super.serializeToJson(context);
        }
    }
}


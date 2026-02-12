/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import com.google.gson.JsonObject;
import drzhark.mocreatures.MoCConstants;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Advancement trigger for "Obtain an undead horse". Fired when a player successfully
 * uses an Essence of Undead on a tamed horse (regular breed or cross-breed), transforming it into an undead horse.
 */
public class MoCObtainUndeadHorseTrigger extends SimpleCriterionTrigger<MoCObtainUndeadHorseTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "obtain_undead_horse");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        return new TriggerInstance(playerPredicate);
    }

    public void trigger(ServerPlayer player) {
        if (player == null) return;
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

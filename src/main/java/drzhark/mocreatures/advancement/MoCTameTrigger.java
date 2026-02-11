/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import com.google.gson.JsonObject;
import drzhark.mocreatures.MoCConstants;
import drzhark.mocreatures.entity.tameable.IMoCTameable;
import drzhark.mocreatures.world.MoCSpawnRegistryCache;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Advancement trigger fired when a player tames a Mo' Creatures entity.
 * Criterion condition "creature" (optional): spawn config key e.g. "kitty", "black_bear".
 */
public class MoCTameTrigger extends SimpleCriterionTrigger<MoCTameTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "tame_moc_creature");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String creature = json.has("creature") ? json.get("creature").getAsString() : null;
        return new TriggerInstance(playerPredicate, creature);
    }

    public void trigger(ServerPlayer player, Entity entity) {
        if (!(entity instanceof IMoCTameable)) return;
        String creatureName = MoCSpawnRegistryCache.getCreatureName(entity.getType());
        if (creatureName == null) return;
        this.trigger(player, instance -> instance.matches(creatureName));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String creature;

        public TriggerInstance(ContextAwarePredicate playerPredicate, String creature) {
            super(ID, playerPredicate);
            this.creature = creature;
        }

        public boolean matches(String creatureName) {
            if (creature == null || creature.isEmpty()) return true;
            return creature.equals(creatureName);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject obj = super.serializeToJson(context);
            if (creature != null && !creature.isEmpty()) {
                obj.addProperty("creature", creature);
            }
            return obj;
        }
    }
}

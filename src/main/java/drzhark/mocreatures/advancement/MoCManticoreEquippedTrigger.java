/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.advancement;

import com.google.gson.JsonObject;
import drzhark.mocreatures.MoCConstants;
import drzhark.mocreatures.entity.hunter.MoCEntityManticorePet;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerAdvancementManager;

/**
 * Advancement trigger fired when a tamed Manticore pet has both saddle and chest equipped.
 * Criterion condition "creature" (optional): variant key e.g. "plain_manticore", "fire_manticore".
 * getTypeMoC() mapping: 1=fire, 2=dark, 3=frost, 4=toxic, 5=plain.
 */
public class MoCManticoreEquippedTrigger extends SimpleCriterionTrigger<MoCManticoreEquippedTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "manticore_equipped");

    private static String getCreatureNameFromType(int typeMoC) {
        return switch (typeMoC) {
            case 1 -> "fire_manticore";
            case 2 -> "dark_manticore";
            case 3 -> "frost_manticore";
            case 4 -> "toxic_manticore";
            case 5 -> "plain_manticore";
            default -> null;
        };
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String creature = json.has("creature") ? json.get("creature").getAsString() : null;
        return new TriggerInstance(playerPredicate, creature);
    }

    /** Advancement that must be completed before Manticore Master criteria can be earned. */
    private static final ResourceLocation TAME_MANTICORE_ADVANCEMENT = new ResourceLocation(MoCConstants.MOD_ID, "tame_manticore");

    public void trigger(ServerPlayer player, MoCEntityManticorePet manticore) {
        if (manticore == null || !manticore.getIsTamed() || !manticore.getIsRideable() || !manticore.getIsChested()) {
            return;
        }
        var server = player.getServer();
        if (server == null) return;
        ServerAdvancementManager advMgr = server.getAdvancements();
        Advancement tameManticore = advMgr.getAdvancement(TAME_MANTICORE_ADVANCEMENT);
        if (tameManticore == null || !player.getAdvancements().getOrStartProgress(tameManticore).isDone()) {
            return;
        }
        String creatureName = getCreatureNameFromType(manticore.getTypeMoC());
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

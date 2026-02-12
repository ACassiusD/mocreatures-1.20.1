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
 * Advancement trigger for taming a dolphin of a specific type (e.g. pink, albino).
 * Criterion condition "dolphin_type": "pink" (type 5) or "albino" (type 6).
 */
public class MoCTameDolphinTypeTrigger extends SimpleCriterionTrigger<MoCTameDolphinTypeTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "tame_dolphin_type");

    /** Dolphin typeMoC (5 = pink, 6 = albino/white) to criterion key. */
    public static String getCriterionKeyFromType(int dolphinTypeMoC) {
        return switch (dolphinTypeMoC) {
            case 5 -> "pink";
            case 6 -> "albino";
            default -> null;
        };
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String dolphinType = json.has("dolphin_type") ? json.get("dolphin_type").getAsString() : null;
        return new TriggerInstance(playerPredicate, dolphinType);
    }

    public void trigger(ServerPlayer player, int dolphinTypeMoC) {
        String key = getCriterionKeyFromType(dolphinTypeMoC);
        if (key == null || player == null) return;
        this.trigger(player, instance -> instance.matches(key));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String dolphinType;

        public TriggerInstance(ContextAwarePredicate playerPredicate, String dolphinType) {
            super(ID, playerPredicate);
            this.dolphinType = dolphinType;
        }

        public boolean matches(String criterionKey) {
            if (dolphinType == null || dolphinType.isEmpty()) return true;
            return dolphinType.equals(criterionKey);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject obj = super.serializeToJson(context);
            if (dolphinType != null && !dolphinType.isEmpty()) {
                obj.addProperty("dolphin_type", dolphinType);
            }
            return obj;
        }
    }
}

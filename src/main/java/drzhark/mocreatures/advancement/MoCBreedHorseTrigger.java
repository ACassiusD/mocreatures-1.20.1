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
 * Advancement trigger fired when a player breeds a Mo' Creatures horse and a baby is produced.
 * Criterion condition "horse_type": breed key e.g. "black_leopard", "black_tovero", "unicorn", "pegasus", "dark_pegasus", "ghost_horse", "fairy_horse", "nightmare".
 * <p>
 * Type mapping: 16=black_leopard, 17=black_tovero, 36=unicorn, 39=pegasus, 40=dark_pegasus,
 * 21/22=ghost_horse, 38=nightmare, 48-59=fairy_horse.
 */
public class MoCBreedHorseTrigger extends SimpleCriterionTrigger<MoCBreedHorseTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "breed_moc_horse");

    /**
     * Maps horse getTypeMoC() to advancement criterion key, or null if no advancement tracks this type.
     */
    public static String getCriterionKeyFromType(int horseTypeMoC) {
        return switch (horseTypeMoC) {
            case 16 -> "black_leopard";
            case 17 -> "black_tovero";
            case 21, 22 -> "ghost_horse";
            case 36 -> "unicorn";
            case 38 -> "nightmare";
            case 39 -> "pegasus";
            case 40 -> "dark_pegasus";
            case 61 -> "zorse";
            case 67 -> "zonky";
            default -> (horseTypeMoC >= 48 && horseTypeMoC <= 59) ? "fairy_horse" : null;
        };
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String horseType = json.has("horse_type") ? json.get("horse_type").getAsString() : null;
        return new TriggerInstance(playerPredicate, horseType);
    }

    public void trigger(ServerPlayer player, int horseTypeMoC) {
        String criterionKey = getCriterionKeyFromType(horseTypeMoC);
        if (criterionKey == null) return;
        this.trigger(player, instance -> instance.matches(criterionKey));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String horseType;

        public TriggerInstance(ContextAwarePredicate playerPredicate, String horseType) {
            super(ID, playerPredicate);
            this.horseType = horseType;
        }

        public boolean matches(String criterionKey) {
            if (horseType == null || horseType.isEmpty()) return true;
            return horseType.equals(criterionKey);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject obj = super.serializeToJson(context);
            if (horseType != null && !horseType.isEmpty()) {
                obj.addProperty("horse_type", horseType);
            }
            return obj;
        }
    }
}

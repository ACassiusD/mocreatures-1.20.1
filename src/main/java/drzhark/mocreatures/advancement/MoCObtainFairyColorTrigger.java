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
 * Advancement trigger for obtaining a fairy horse of a specific color (by dyeing a white fairy).
 * Criterion condition "fairy_color": one of yellow, purple, blue, pink, light_green, black, red, dark_blue, cyan, green, orange.
 */
public class MoCObtainFairyColorTrigger extends SimpleCriterionTrigger<MoCObtainFairyColorTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation(MoCConstants.MOD_ID, "obtain_fairy_color");

    /** Fairy horse type (48-59, excluding 50 white) to criterion key. White is covered by breed a fairy horse. */
    public static String getCriterionKeyFromType(int fairyTypeMoC) {
        return switch (fairyTypeMoC) {
            case 48 -> "yellow";
            case 49 -> "purple";
            case 51 -> "blue";
            case 52 -> "pink";
            case 53 -> "light_green";
            case 54 -> "black";
            case 55 -> "red";
            case 56 -> "dark_blue";
            case 57 -> "cyan";
            case 58 -> "green";
            case 59 -> "orange";
            default -> null;
        };
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String fairyColor = json.has("fairy_color") ? json.get("fairy_color").getAsString() : null;
        return new TriggerInstance(playerPredicate, fairyColor);
    }

    public void trigger(ServerPlayer player, int fairyTypeMoC) {
        String key = getCriterionKeyFromType(fairyTypeMoC);
        if (key == null || player == null) return;
        this.trigger(player, instance -> instance.matches(key));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String fairyColor;

        public TriggerInstance(ContextAwarePredicate playerPredicate, String fairyColor) {
            super(ID, playerPredicate);
            this.fairyColor = fairyColor;
        }

        public boolean matches(String criterionKey) {
            if (fairyColor == null || fairyColor.isEmpty()) return true;
            return fairyColor.equals(criterionKey);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject obj = super.serializeToJson(context);
            if (fairyColor != null && !fairyColor.isEmpty()) {
                obj.addProperty("fairy_color", fairyColor);
            }
            return obj;
        }
    }
}

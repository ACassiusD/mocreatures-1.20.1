package drzhark.mocreatures.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Item that instantly ages baby Mo' Creatures animals (e.g. horses, zorses, manticores) to adults when used on them.
 * Handling is done in MoCEntityAnimal.mobInteract().
 */
public class MoCItemEssenceOfGrowth extends Item {

    public MoCItemEssenceOfGrowth(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.mocreatures.essenceofgrowth.tooltip"));
    }
}

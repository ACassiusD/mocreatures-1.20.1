/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.item;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.init.MoCItems;
import drzhark.mocreatures.network.MoCMessageHandler;
import drzhark.mocreatures.network.message.MoCMessageTamersNotebook;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class MoCItemTamersNotebook extends Item {

    public MoCItemTamersNotebook(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MoCMessageTamersNotebook message = MoCMessageTamersNotebook.create(serverPlayer);
            if (message != null) {
                MoCMessageHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), message);
            }
        }
        return InteractionResultHolder.success(stack);
    }

    public static ItemStack createDisplayStack() {
        return new ItemStack(MoCItems.TAMERS_NOTEBOOK.get());
    }
}

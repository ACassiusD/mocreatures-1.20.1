/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client;

import drzhark.mocreatures.MoCConstants;
import drzhark.mocreatures.client.gui.MoCGUISettings;
import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.network.MoCMessageHandler;
import drzhark.mocreatures.network.message.MoCMessageEntityDive;
import drzhark.mocreatures.network.message.MoCMessageEntityJump;
import drzhark.mocreatures.proxy.MoCProxyClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MoCConstants.MOD_ID, value = Dist.CLIENT)
public class MoCInputHandler {

    @SubscribeEvent
    public static void onInput(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        // // Only run on logical client - setScreen must be called from render thread, not server thread
        // if (!e.player.level().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && MoCKeyHandler.settingsKey.consumeClick()) {
            mc.execute(() -> mc.setScreen(new MoCGUISettings()));
            return;
        }

        boolean kbJump = MoCProxyClient.mc.options.keyJump.isDown();
        boolean kbDive = MoCKeyHandler.diveBinding.isDown();

        if (kbJump && e.player.getVehicle() != null && e.player.getVehicle() instanceof IMoCEntity) {
            // jump code needs to be executed client/server simultaneously to take
            ((IMoCEntity) e.player.getVehicle()).makeEntityJump();
            MoCMessageHandler.INSTANCE.sendToServer(new MoCMessageEntityJump());
        }

        if (kbDive && e.player.getVehicle() != null && e.player.getVehicle() instanceof IMoCEntity) {
            // dive code needs to be executed client/server simultaneously to take
            ((IMoCEntity) e.player.getVehicle()).makeEntityDive();
            MoCMessageHandler.INSTANCE.sendToServer(new MoCMessageEntityDive());
        }
    }
} 
package net.fretux.knockedback.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "knockedback", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderCarryPassengerHandler {
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<LivingEntity, ?> evt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return;
        LivingEntity entity = evt.getEntity();
        if (entity instanceof Player passenger
                && passenger.getVehicle() == mc.player) {
            evt.setCanceled(true);
        }
    }
}

package net.fretux.knockedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "knockedback", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientCarryRenderHandler {
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer camera = mc.player;
        if (camera == null) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return;
        if (event.getEntity() instanceof LocalPlayer passenger
                && passenger.getVehicle() == camera) {
            event.setCanceled(true);
        }
    }
}
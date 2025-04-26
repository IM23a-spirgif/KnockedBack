package net.fretux.knockedback.client;

import net.fretux.knockedback.KnockedBack;
import net.fretux.knockedback.client.ClientKnockedState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class KnockedTimeOverlay {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!ClientKnockedState.isKnocked()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        String text = "Time until recovery: " + (ClientKnockedState.getTimeLeft() / 20) + " s";
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth / 2 - mc.font.width(text) / 2;
        int y = 10;
        guiGraphics.drawString(mc.font, text, x, y, 0xFFFFFF, true);
    }
}

package net.fretux.knockedback.client;

import net.fretux.knockedback.KnockedBack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, value = Dist.CLIENT)
public class ExecutionProgressOverlay {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 8;
    private static final int TOTAL_TICKS = net.fretux.knockedback.PlayerExecutionHandler.EXECUTION_DELAY_TICKS;

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (!ClientExecutionState.isExecuting()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gui = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = (sw - BAR_WIDTH) / 2;
        int y = (sh - BAR_HEIGHT) / 2;
        float progress = 1.0f - (ClientExecutionState.getTimeLeft() / (float) TOTAL_TICKS);
        int fillWidth = (int) (BAR_WIDTH * progress);
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x80000000);
        gui.fill(x, y, x + fillWidth, y + BAR_HEIGHT, 0x80FFFFFF);
    }
}
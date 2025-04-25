package net.fretux.knockedback.client;

import net.fretux.knockedback.CarryTogglePacket;
import net.fretux.knockedback.ExecuteKnockedPacket;
import net.fretux.knockedback.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "knockedback", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientKeyHandler {
    public static final KeyMapping EXECUTE_KEY = new KeyMapping(
            "key.knockedback.execute",
            GLFW.GLFW_KEY_B,
            "key.categories.knockedback"
    );

    // This event still needs to be registered on the MOD bus for key mapping registration.
    @Mod.EventBusSubscriber(modid = "knockedback", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class KeyMappingRegistration {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(EXECUTE_KEY);
        }
    }

    public static final KeyMapping CARRY_KEY = new KeyMapping(
            "key.knockedback.carry",
            GLFW.GLFW_KEY_V,
            "key.categories.knockedback"
    );

    @SubscribeEvent
    public static void registerCarryKey(RegisterKeyMappingsEvent e) {
        e.register(CARRY_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().screen != null) return;
        if (EXECUTE_KEY.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("Key B pressed! Sending execution request to server.")
                );
                // Send the packet to the server
                NetworkHandler.CHANNEL.sendToServer(new ExecuteKnockedPacket());
            }
        }
        if (event.phase == TickEvent.Phase.END && CARRY_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                NetworkHandler.CHANNEL.sendToServer(new CarryTogglePacket());
            }
        }
    }
}
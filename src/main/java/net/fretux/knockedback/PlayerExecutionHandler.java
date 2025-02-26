package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = "knockedback", bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerExecutionHandler {
    public static final double EXECUTION_RANGE = 2.0;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PlayerExecutionHandler());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        register();
    }

    public static void executeKnockedPlayer(ServerPlayer executor, LivingEntity knockedPlayer) {
        KnockedManager.removeKnockedState(knockedPlayer);
        knockedPlayer.setHealth(0.0F);
        executor.sendSystemMessage(Component.literal("You executed " + knockedPlayer.getName().getString() + "!"));
        if (knockedPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("You were executed by " + executor.getName().getString() + "!"));
        }
    }
}

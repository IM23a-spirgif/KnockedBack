package net.fretux.knockedback;

import com.mojang.logging.LogUtils;
import net.fretux.knockedback.effects.KnockedEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.fretux.knockedback.config.Config;
import org.slf4j.Logger;

@Mod(KnockedBack.MOD_ID)
public class KnockedBack {
    public static final String MOD_ID = "knockedback";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KnockedBack() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new KnockedEffect());
        MinecraftForge.EVENT_BUS.register(new MobKillHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerExecutionHandler());
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
        PlayerExecutionHandler.register();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("KnockedBack mod server starting");
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player player && KnockedManager.isKnocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            KnockedManager.tickKnockedStates();
        }
    }
}

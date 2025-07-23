package net.fretux.knockedback.effects;

import net.fretux.knockedback.KnockedManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "knockedback", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KnockedEffect {

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (KnockedManager.isKnocked(player)) {
            player.setSprinting(false);
            player.hurtMarked = true;
            player.setNoGravity(false);
            player.setDeltaMovement(0, player.getDeltaMovement().y(), 0);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (KnockedManager.isKnocked(event.getEntity())) {
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(PlayerEvent.BreakSpeed event) {
        if (KnockedManager.isKnocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (KnockedManager.isKnocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && KnockedManager.isKnocked(player)) {
            event.getEntity().setDeltaMovement(0, 0, 0);
        }
    }
}
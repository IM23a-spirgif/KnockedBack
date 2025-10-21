package net.fretux.knockedback;

import net.fretux.knockedback.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KnockedManager {
    private static final Map<UUID, Integer> knockedEntities = new HashMap<>();
    private static final Set<UUID> grippedEntities = new HashSet<>();
    private static final Set<UUID> killingNow = new HashSet<>();

    public static void markKillInProgress(LivingEntity e) {
        killingNow.add(e.getUUID());
    }

    public static void unmarkKillInProgress(LivingEntity e) {
        killingNow.remove(e.getUUID());
    }


    public static int getKnockedDuration() {
        return Config.COMMON.knockedDuration.get();
    }

    public static void applyKnockedState(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        if (!isKnocked(entity)) {
            knockedEntities.put(entity.getUUID(), getKnockedDuration());
            entity.setHealth(1.0F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (KnockedManager.isKnocked(sp)) {
            boolean grounded = sp.onGround() || sp.isInWater();
            sp.setNoGravity(false);
            if (!grounded) {
                sp.setDeltaMovement(0, sp.getDeltaMovement().y() - 0.08, 0); 
            }
        }

    }


    public static boolean isKnocked(LivingEntity entity) {
        return knockedEntities.containsKey(entity.getUUID());
    }

    public static void removeKnockedState(LivingEntity entity) {
        knockedEntities.remove(entity.getUUID());
        grippedEntities.remove(entity.getUUID());
        MobKillHandler.clearKillAttempt(entity.getUUID());
        if (entity instanceof ServerPlayer sp) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new KnockedTimePacket(0)
            );
        }
    }


    public static void setGripped(LivingEntity entity, boolean isGripped) {
        if (isGripped) {
            grippedEntities.add(entity.getUUID());
        } else {
            grippedEntities.remove(entity.getUUID());
        }
    }

    public static Collection<UUID> getKnockedUuids() {
        return new HashSet<>(knockedEntities.keySet());
    }

    public static void tickKnockedStates() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        Iterator<Map.Entry<UUID, Integer>> it = knockedEntities.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID playerId = entry.getKey();
            int timeLeft = entry.getValue();
            if (grippedEntities.contains(playerId)
                    || CarryManager.isBeingCarried(playerId)) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
                continue;
            }
            if (MobKillHandler.isBeingMobExecuted(playerId)
                    || PlayerExecutionHandler.isBeingPlayerExecuted(playerId)) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
                continue;
            }
            timeLeft--;
            if (timeLeft <= 0) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(0)
                    );
                }
                it.remove();
            } else {
                entry.setValue(timeLeft);
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
            }
        }
    }

    private static void killAndRemove(LivingEntity entity) {
        removeKnockedState(entity); 
        entity.setHealth(0.0F);
        PlayerExecutionHandler.cancelExecution(entity.getUUID());
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource src = event.getSource();
        if (!(entity instanceof Player player)) return;
        if (killingNow.contains(player.getUUID())) return;
        if (MobKillHandler.isBeingMobExecuted(player.getUUID())
                || PlayerExecutionHandler.isBeingPlayerExecuted(player.getUUID())) {
            if (event.getSource().getEntity() instanceof LivingEntity) {
                event.setCanceled(true);
            }
            return;
        }
        String damageType = src.getMsgId();
        boolean isFatal = player.getHealth() - event.getAmount() <= 0;
        if (isFatal) {
            if (Config.COMMON.explosionsBypassKnockdown.get() &&
                    (damageType.contains("explosion") || damageType.contains("fireworks"))) {
                killAndRemove(player);
                return;
            }
            if (Config.COMMON.fallDamageBypassesKnockdown.get() && damageType.contains("fall")) {
                killAndRemove(player);
                return;
            }
            if (Config.COMMON.lavaBypassesKnockdown.get() &&
                    (damageType.contains("lava") || damageType.contains("inFire") ||
                            damageType.contains("fire") || damageType.contains("onFire") ||
                            damageType.contains("fireball") || damageType.contains("hotFloor"))) {
                killAndRemove(player);
                return;
            }
        }
        boolean hasTotem = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        if (!isKnocked(player)) {
            if (isFatal) {
                if (!hasTotem || !Config.COMMON.totemPreventsKnockdown.get()) {
                    event.setCanceled(true);
                    applyKnockedState(player);
                }
            }
            return;
        }
        knockedEntities.put(player.getUUID(), getKnockedDuration());
        event.setCanceled(true);
    }
}
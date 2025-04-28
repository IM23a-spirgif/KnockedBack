package net.fretux.knockedback;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KnockedManager {
    private static final Map<UUID, Integer> knockedEntities = new HashMap<>();
    private static final Set<UUID> grippedEntities = new HashSet<>();
    private static final int KNOCKED_DURATION = 400;

    public static void applyKnockedState(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        if (!isKnocked(entity)) {
            knockedEntities.put(entity.getUUID(), KNOCKED_DURATION);
            entity.setHealth(1.0F);
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

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        DamageSource src = event.getSource();
        String damageType = src.getMsgId();
        boolean isFatal = player.getHealth() - event.getAmount() <= 0;
        if (!isKnocked(entity)) {
            if (isFatal) {
                if (src.getEntity() instanceof LivingEntity) {
                    event.setCanceled(true);
                    applyKnockedState(entity);
                }
            }
            return;
        }
        knockedEntities.put(entity.getUUID(), KNOCKED_DURATION);
        if (damageType.equals("fire") || damageType.equals("lava") || damageType.equals("onFire") ||
                damageType.equals("explosion") || damageType.equals("explosion.player") || damageType.equals("fireball")) {
            entity.setHealth(0.0F);
            removeKnockedState(entity);
            return;
        }
        if ((damageType.equals("fall") || damageType.equals("explosion") || damageType.equals("explosion.player")) &&
                event.getAmount() >= entity.getHealth()) {
            entity.setHealth(0.0F);
            removeKnockedState(entity);
            return;
        }
        event.setCanceled(true);
    }
}
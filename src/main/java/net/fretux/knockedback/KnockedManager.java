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
        Iterator<Map.Entry<UUID, Integer>> it = knockedEntities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID entityId = entry.getKey();
            int timeLeft = entry.getValue();

            // If the player is gripped, force the timer to 0
            if (grippedEntities.contains(entityId)) {
                entry.setValue(0);
                timeLeft = 0;
            } else {
                // If not gripped, decrease normally
                timeLeft--;
                if (timeLeft <= 0) {
                    it.remove();
                    continue;
                } else {
                    entry.setValue(timeLeft);
                }
            }

            // Send the updated time to the knocked player
            ServerPlayer knockedPlayer = NetworkHandlerHelper.getPlayerByUuid(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), entityId);
            if (knockedPlayer != null) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> knockedPlayer),
                        new KnockedTimePacket(timeLeft));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isKnocked(entity)) return;
        knockedEntities.put(entity.getUUID(), KNOCKED_DURATION);

        DamageSource source = event.getSource();
        String damageType = source.getMsgId();

        // Handle lethal damage cases (fire, explosions, etc.)
        if (damageType.equals("fire") || damageType.equals("lava") || damageType.equals("onFire") ||
                damageType.equals("explosion") || damageType.equals("explosion.player") || damageType.equals("fireball")) {
            entity.setHealth(0.0F);
            removeKnockedState(entity);
            return;
        }

        // Handle falling or lethal explosion
        if ((damageType.equals("fall") || damageType.equals("explosion") || damageType.equals("explosion.player")) &&
                event.getAmount() >= entity.getHealth()) {
            entity.setHealth(0.0F);
            removeKnockedState(entity); 
            return;
        }
        event.setCanceled(true);
    }
}
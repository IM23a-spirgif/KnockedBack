package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;

import java.util.*;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CarryManager {
    private static final Map<UUID, UUID> carrying = new HashMap<>();
    private static final double CARRY_RANGE = 2.0;

    public static void toggleCarry(ServerPlayer carrier) {
        Optional<Player> existing = carrier.getPassengers().stream()
                .filter(p -> p instanceof Player)
                .map(p -> (Player)p)
                .findFirst();
        if (existing.isPresent()) {
            stopCarry(existing.get(), carrier);
            return;
        }
        Player near = findNearestKnocked(carrier);
        if (near != null) {
            startCarry(carrier, near);
        }
    }


    private static Player findNearestKnocked(ServerPlayer carrier) {
        AABB box = carrier.getBoundingBox().inflate(CARRY_RANGE);
        return carrier
                .getCommandSenderWorld()
                .getEntitiesOfClass(Player.class, box).stream()
                .filter(p -> KnockedManager.isKnocked(p)
                        && !p.getUUID().equals(carrier.getUUID()))
                .findFirst()
                .orElse(null);
    }


    private static void startCarry(ServerPlayer carrier, Player knocked) {
        // mount
        knocked.startRiding(carrier, true);
        carrying.put(knocked.getUUID(), carrier.getUUID());
        carrier.sendSystemMessage(Component.literal("Picked up " + knocked.getName().getString()));
        knocked.sendSystemMessage(Component.literal("You are being carried by " + carrier.getName().getString()));
        knocked.startRiding(carrier, true);
        carrying.put(knocked.getUUID(), carrier.getUUID());
        carrier.connection.send(new ClientboundSetPassengersPacket(carrier));
    }

    private static void stopCarry(Player knocked, ServerPlayer carrier) {
        knocked.stopRiding();
        carrying.remove(knocked.getUUID());
        carrier.connection.send(new ClientboundSetPassengersPacket(carrier));
        carrier.sendSystemMessage(Component.literal("Dropped " + knocked.getName().getString()));
        knocked.sendSystemMessage(Component.literal("You have been dropped"));
    }

    /**
     * Called each server tick to drop if carrier hurt or out of range
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !ev.side.isServer()) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        List<UUID> toDrop = new ArrayList<>();
        for (var entry : carrying.entrySet()) {
            UUID knockedId = entry.getKey();
            UUID carrierId = entry.getValue();
            ServerPlayer carrier = NetworkHandlerHelper.getPlayerByUuid(server, carrierId);
            ServerPlayer knocked = NetworkHandlerHelper.getPlayerByUuid(server, knockedId);

            if (carrier == null || knocked == null
                    || !KnockedManager.isKnocked(knocked)
                    || carrier.distanceTo(knocked) > CARRY_RANGE) {
                toDrop.add(knockedId);
            }
        }
        for (UUID kid : toDrop) {
            UUID cid = carrying.get(kid);
            ServerPlayer carrier = NetworkHandlerHelper.getPlayerByUuid(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), cid);
            ServerPlayer knocked = NetworkHandlerHelper.getPlayerByUuid(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), kid);
            if (carrier != null && knocked != null) stopCarry(knocked, carrier);
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent evt) {
        if (!evt.isMounting()) {
            if (evt.getEntityMounting() instanceof Player knocked
                    && carrying.containsKey(knocked.getUUID())) {
                evt.setCanceled(true);
            }
        }
    }

    /**
     * Drop if carrier is hurt
     */
    @SubscribeEvent
    public static void onCarrierHurt(LivingHurtEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer carrier)) return;
        carrying.entrySet().stream()
                .filter(e -> e.getValue().equals(carrier.getUUID()))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(knockedId -> {
                    ServerPlayer knocked = NetworkHandlerHelper.getPlayerByUuid(
                            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), knockedId);
                    if (knocked != null) stopCarry(knocked, carrier);
                });
    }

    public static boolean isBeingCarried(UUID knockedId) {
        return carrying.containsKey(knockedId);
    }
}
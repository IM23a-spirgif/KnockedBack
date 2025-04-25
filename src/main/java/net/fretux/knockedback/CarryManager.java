package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CarryManager {
    private static final Map<UUID, UUID> carrying = new HashMap<>();
    private static final double CARRY_RANGE = 2.0;

    public static void toggleCarry(ServerPlayer carrier) {
        // 1) If they’re already carrying someone, drop them—and quit immediately
        carrier.getPassengers().stream()
                .filter(p -> p instanceof Player)
                .map(p -> (Player)p)
                .findFirst()
                .ifPresent(p -> {
                    stopCarry((Player)p, carrier);
                });
        if (carrier.getPassengers().size() > 0) return;

        // 2) Otherwise look for a knocked player to pick up
        ServerPlayer near = findNearestKnocked(carrier);
        if (near != null) {
            startCarry(carrier, near);
        }
    }

    private static ServerPlayer findNearestKnocked(ServerPlayer carrier) {
        AABB box = carrier.getBoundingBox().inflate(CARRY_RANGE);
        return carrier.getCommandSenderWorld()
                .getEntitiesOfClass(ServerPlayer.class, box).stream()
                .filter(p -> KnockedManager.isKnocked(p)
                        && !p.getUUID().equals(carrier.getUUID()))
                .findFirst()
                .orElse(null);
    }

    private static void startCarry(ServerPlayer carrier, ServerPlayer knocked) {
        // mount once
        knocked.startRiding(carrier, true);
        carrying.put(knocked.getUUID(), carrier.getUUID());

        // tell both clients
        ClientboundSetPassengersPacket pkt = new ClientboundSetPassengersPacket(carrier);
        carrier.connection.send(pkt);
        knocked.connection.send(pkt);

        carrier.sendSystemMessage(Component.literal("Picked up " + knocked.getName().getString()));
        knocked.sendSystemMessage(Component.literal("You are being carried by " + carrier.getName().getString()));
    }

    private static void stopCarry(Player knocked, ServerPlayer carrier) {
        knocked.stopRiding();
        carrying.remove(knocked.getUUID());

        // same packet to both sides
        ClientboundSetPassengersPacket pkt = new ClientboundSetPassengersPacket(carrier);
        carrier.connection.send(pkt);
        if (knocked instanceof ServerPlayer sp) {
            sp.connection.send(pkt);
        }

        carrier.sendSystemMessage(Component.literal("Dropped " + knocked.getName().getString()));
        knocked.sendSystemMessage(Component.literal("You have been dropped"));
    }

    /** 2) auto-drop if out of range or no longer knocked */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !ev.side.isServer()) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        List<UUID> toDrop = new ArrayList<>();
        for (var entry : carrying.entrySet()) {
            UUID k = entry.getKey();
            UUID c = entry.getValue();
            ServerPlayer carrier = NetworkHandlerHelper.getPlayerByUuid(server, c);
            ServerPlayer knocked  = NetworkHandlerHelper.getPlayerByUuid(server, k);
            if (carrier == null || knocked == null
                    || !KnockedManager.isKnocked(knocked)
                    || carrier.distanceTo(knocked) > CARRY_RANGE) {
                toDrop.add(k);
            }
        }
        toDrop.forEach(k -> {
            ServerPlayer carrier = NetworkHandlerHelper.getPlayerByUuid(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(),
                    carrying.get(k));
            ServerPlayer knocked = NetworkHandlerHelper.getPlayerByUuid(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), k);
            if (carrier != null && knocked != null) stopCarry(knocked, carrier);
        });
    }

    /** 2b) prevent sneak-dismount */
    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent evt) {
        if (!evt.isMounting()
                && evt.getEntityMounting() instanceof Player knocked
                && carrying.containsKey(knocked.getUUID())) {
            evt.setCanceled(true);
        }
    }

    /** 3) auto-drop if carrier is hurt */
    @SubscribeEvent
    public static void onCarrierHurt(LivingHurtEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer carrier)) return;
        carrying.entrySet().stream()
                .filter(e -> e.getValue().equals(carrier.getUUID()))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(k -> {
                    ServerPlayer knocked = NetworkHandlerHelper.getPlayerByUuid(
                            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), k);
                    if (knocked != null) stopCarry(knocked, carrier);
                });
    }

    public static boolean isBeingCarried(UUID knockedId) {
        return carrying.containsKey(knockedId);
    }
}
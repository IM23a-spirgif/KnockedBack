package net.fretux.knockedback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExecuteKnockedPacket {
    public ExecuteKnockedPacket() { }
    public static void encode(ExecuteKnockedPacket msg, FriendlyByteBuf buf) {

    }

    public static ExecuteKnockedPacket decode(FriendlyByteBuf buf) {
        return new ExecuteKnockedPacket();
    }

    public static void handle(ExecuteKnockedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer executor = ctx.get().getSender();
            if (executor == null) return;
            LivingEntity target = findNearestKnockedEntity(executor);
            if (target != null) {
                PlayerExecutionHandler.executeKnockedPlayer(executor, target);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static LivingEntity findNearestKnockedEntity(ServerPlayer executor) {
        return KnockedManager.getKnockedUuids().stream()
                .map(uuid -> NetworkHandlerHelper.getPlayerByUuid(executor.getServer(), uuid))
                .filter(player -> player != null && player.distanceTo(executor) <= (float) PlayerExecutionHandler.EXECUTION_RANGE)
                .findFirst()
                .orElse(null);
    }
}
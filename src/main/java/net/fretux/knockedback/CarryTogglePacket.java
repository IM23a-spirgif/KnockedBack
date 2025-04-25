package net.fretux.knockedback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CarryTogglePacket {
    public static void encode(CarryTogglePacket msg, FriendlyByteBuf buf) {}
    public static CarryTogglePacket decode(FriendlyByteBuf buf) { return new CarryTogglePacket(); }

    public static void handle(CarryTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer carrier = ctx.get().getSender();
            if (carrier == null) return;
            CarryManager.toggleCarry(carrier);
        });
        ctx.get().setPacketHandled(true);
    }
}

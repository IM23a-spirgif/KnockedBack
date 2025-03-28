package net.fretux.knockedback;

import net.fretux.knockedback.client.ClientKnockedState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class KnockedTimePacket {
    private final int timeLeft;

    public KnockedTimePacket(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public static void encode(KnockedTimePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.timeLeft);
    }

    public static KnockedTimePacket decode(FriendlyByteBuf buf) {
        return new KnockedTimePacket(buf.readInt());
    }

    public static void handle(KnockedTimePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client side: update the client state with the time left.
            ClientKnockedState.setTimeLeft(packet.timeLeft);
        });
        ctx.get().setPacketHandled(true);
    }
}
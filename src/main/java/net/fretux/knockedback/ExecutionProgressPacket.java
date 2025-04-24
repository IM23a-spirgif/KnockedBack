package net.fretux.knockedback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExecutionProgressPacket {
    private final int timeLeft;

    public ExecutionProgressPacket(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public static void encode(ExecutionProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.timeLeft);
    }

    public static ExecutionProgressPacket decode(FriendlyByteBuf buf) {
        return new ExecutionProgressPacket(buf.readInt());
    }

    public static void handle(ExecutionProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.fretux.knockedback.client.ClientExecutionState.setTimeLeft(msg.timeLeft);
        });
        ctx.get().setPacketHandled(true);
    }
}

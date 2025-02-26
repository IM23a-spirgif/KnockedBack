package net.fretux.knockedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(KnockedBack.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int id = 0;
        CHANNEL.registerMessage(id++,
                ExecuteKnockedPacket.class,
                ExecuteKnockedPacket::encode,
                ExecuteKnockedPacket::decode,
                ExecuteKnockedPacket::handle);
    }
}
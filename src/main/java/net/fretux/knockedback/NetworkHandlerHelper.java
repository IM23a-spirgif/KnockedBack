package net.fretux.knockedback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class NetworkHandlerHelper {
    public static ServerPlayer getPlayerByUuid(MinecraftServer server, UUID uuid) {
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(uuid);
    }
}
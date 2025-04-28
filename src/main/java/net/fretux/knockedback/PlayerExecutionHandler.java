package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "knockedback", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerExecutionHandler {
    public static final double EXECUTION_RANGE = 2.0;
    public static final int EXECUTION_DELAY_TICKS = 80;

    public static boolean isBeingPlayerExecuted(UUID knockedId) {
        return executionAttempts.containsKey(knockedId);
    }

    public static boolean isExecuting(UUID playerUuid) {
        return executionAttempts.values().stream().anyMatch(attempt -> attempt.executorUuid.equals(playerUuid));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp && isExecuting(sp.getUUID())) {
            sp.setDeltaMovement(0, 0, 0);
            sp.setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && isExecuting(sp.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static final Map<UUID, PlayerExecutionAttempt> executionAttempts = new HashMap<>();

    private static class PlayerExecutionAttempt {
        private final UUID executorUuid;
        private int timeLeft;

        public PlayerExecutionAttempt(UUID executorUuid) {
            this.executorUuid = executorUuid;
            this.timeLeft = EXECUTION_DELAY_TICKS;
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PlayerExecutionHandler());
    }

    public static void startExecution(ServerPlayer executor, Player target) {
        if (KnockedManager.isKnocked(executor)) {
            executor.sendSystemMessage(Component.literal("You cannot execute others while you are knocked!"));
            return;
        }
        UUID knockedId = target.getUUID();
        if (!KnockedManager.isKnocked(target)) return;
        if (CarryManager.isBeingCarried(target.getUUID())) {
            executor.sendSystemMessage(Component.literal("Cannot execute someone you're carrying!"));
            return;
        }
        if (executionAttempts.containsKey(knockedId)) return;
        executionAttempts.put(knockedId, new PlayerExecutionAttempt(executor.getUUID()));
        executor.sendSystemMessage(Component.literal("Execution started..."));
        target.sendSystemMessage(Component.literal("You're being executed!"));
    }

    public static void cancelExecution(UUID knockedId) {
        PlayerExecutionAttempt attempt = executionAttempts.remove(knockedId);
        if (attempt != null) {
            ServerPlayer knocked = getPlayerByUuid(knockedId);
            if (knocked != null) {
                knocked.sendSystemMessage(Component.literal("Execution interrupted!"));
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> knocked),
                        new ExecutionProgressPacket(0)
                );
            }
            ServerPlayer executor = getPlayerByUuid(attempt.executorUuid);
            if (executor != null) {
                executor.sendSystemMessage(Component.literal("Your execution was interrupted!"));
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> executor),
                        new ExecutionProgressPacket(0)
                );
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer hurtPlayer)) return;
        UUID hurtId = hurtPlayer.getUUID();
        boolean wasExecutor = executionAttempts.values().removeIf(attempt -> attempt.executorUuid.equals(hurtId));
        if (wasExecutor) {
            hurtPlayer.sendSystemMessage(Component.literal("Execution canceled because you were hit!"));
        }
    }

    private static ServerPlayer getPlayerByUuid(UUID uuid) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server.getPlayerList().getPlayer(uuid);
    }

    public static void executeKnockedPlayer(ServerPlayer executor, LivingEntity knockedPlayer) {
        KnockedManager.removeKnockedState(knockedPlayer);
        knockedPlayer.setHealth(0.0F);
        executor.sendSystemMessage(Component.literal("You executed " + knockedPlayer.getName().getString() + "!"));
        if (knockedPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("You were executed by " + executor.getName().getString() + "!"));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isServer()) return;
        Map<UUID, PlayerExecutionAttempt> updated = new HashMap<>();
        for (Map.Entry<UUID, PlayerExecutionAttempt> entry : executionAttempts.entrySet()) {
            UUID knockedId = entry.getKey();
            PlayerExecutionAttempt attempt = entry.getValue();
            ServerPlayer knocked = getPlayerByUuid(knockedId);
            ServerPlayer executor = getPlayerByUuid(attempt.executorUuid);
            if (knocked == null || executor == null || !KnockedManager.isKnocked(knocked)) {
                continue;
            }
            if (executor.distanceTo(knocked) > EXECUTION_RANGE) {
                cancelExecution(knockedId);
                continue;
            }
            attempt.timeLeft--;
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> executor), new ExecutionProgressPacket(attempt.timeLeft));
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> knocked), new ExecutionProgressPacket(attempt.timeLeft));
            if (attempt.timeLeft <= 0) {
                executeKnockedPlayer(executor, knocked);
            } else {
                updated.put(knockedId, attempt);
            }
        }
        executionAttempts.clear();
        executionAttempts.putAll(updated);
    }
}

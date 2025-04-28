package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static net.fretux.knockedback.KnockedManager.removeKnockedState;
import static net.fretux.knockedback.KnockedManager.setGripped;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobKillHandler {
    private static final int EXECUTION_DELAY_TICKS = 3 * 20;
    private static final Map<UUID, KillAttempt> killAttempts = new HashMap<>();

    private static class KillAttempt {
        private final UUID mobUuid;
        private int timeLeft;
        public KillAttempt(UUID mobUuid, int timeLeft) {
            this.mobUuid = mobUuid;
            this.timeLeft = timeLeft;
        }
    }

    public static boolean isBeingMobExecuted(UUID knockedId) {
        return killAttempts.containsKey(knockedId);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isServer()) return;
        tickKillAttempts();
    }

    @SubscribeEvent
    public void onMobHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        UUID mobUuid = mob.getUUID();
        UUID toRelease = null;
        for (var e : killAttempts.entrySet()) {
            if (e.getValue().mobUuid.equals(mobUuid)) {
                toRelease = e.getKey();
                break;
            }
        }
        if (toRelease != null) {
            killAttempts.remove(toRelease);
            Player p = getPlayerByUuid(toRelease);
            if (p != null) {
                if (p instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            new ExecutionProgressPacket(0)
                    );
                }
                setGripped(p, false);
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private void tickKillAttempts() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        Map<UUID, KillAttempt> updated = new HashMap<>();
        for (UUID knockedId : KnockedManager.getKnockedUuids()) {
            Player knocked = getPlayerByUuid(knockedId);
            if (knocked == null || !knocked.isAlive()) continue;
            if (CarryManager.isBeingCarried(knockedId)) {
                if (knocked instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            new ExecutionProgressPacket(0)
                    );
                }
                setGripped(knocked, false);
                continue;
            }
            Mob mob = getMobInRange(knocked);
            if (mob == null) {
                if (knocked instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            new ExecutionProgressPacket(0)
                    );
                }
                setGripped(knocked, false);
                continue;
            }
            setGripped(knocked, true);
            KillAttempt attempt = killAttempts.get(knockedId);
            if (attempt == null || !attempt.mobUuid.equals(mob.getUUID())) {
                attempt = new KillAttempt(mob.getUUID(), EXECUTION_DELAY_TICKS);
            } else {
                attempt.timeLeft--;
                if (knocked instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            new ExecutionProgressPacket(attempt.timeLeft)
                    );
                }
                if (attempt.timeLeft <= 0) {
                    executeKnockedPlayer(knocked, mob);
                    continue;
                }
            }
            updated.put(knockedId, attempt);
        }
        killAttempts.clear();
        killAttempts.putAll(updated);
    }

    private void executeKnockedPlayer(Player knocked, Mob mob) {
        removeKnockedState(knocked);
        knocked.setHealth(0.0F);
        mob.getNavigation().stop();
        if (knocked instanceof ServerPlayer sp) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new ExecutionProgressPacket(0)
            );
        }
        knocked.sendSystemMessage(Component.literal(
                "You were executed by " + mob.getName().getString() + "!"
        ));
    }

    @Nullable
    private Mob getMobInRange(Player p) {
        if (!(p.level() instanceof ServerLevel world)) return null;
        double r = 2.5;
        AABB box = new AABB(
                p.getX()-r, p.getY()-r, p.getZ()-r,
                p.getX()+r, p.getY()+r, p.getZ()+r
        );
        return world.getEntitiesOfClass(Mob.class, box).stream()
                .filter(m -> isHostile(m) || isAggressiveNeutral(m))
                .findAny().orElse(null);
    }

    private boolean isHostile(Mob m) {
        return m instanceof net.minecraft.world.entity.monster.Monster;
    }
    private boolean isAggressiveNeutral(Mob m) {
        if (m instanceof net.minecraft.world.entity.animal.Wolf w) return w.isAngry();
        if (m instanceof net.minecraft.world.entity.monster.EnderMan e) return e.isCreepy();
        return false;
    }

    @Nullable
    private Player getPlayerByUuid(UUID id) {
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return srv != null ? srv.getPlayerList().getPlayer(id) : null;
    }

    public static void clearKillAttempt(UUID playerUuid) {
        killAttempts.remove(playerUuid);
    }
}
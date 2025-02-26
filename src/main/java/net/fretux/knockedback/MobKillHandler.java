package net.fretux.knockedback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.fretux.knockedback.KnockedManager.removeKnockedState;
import static net.fretux.knockedback.KnockedManager.setGripped;

/**
 * Manages the logic for mobs killing knocked players after a 3-second delay,
 * which resets if the mob is hit during those 3 seconds.
 */
public class MobKillHandler {
    private static final int EXECUTION_DELAY_TICKS = 3 * 20;
    /**
     * A record of ongoing kill attempts.
     * Key = Knocked player's UUID
     * Value = An object tracking the mob's UUID & the countdown in ticks.
     */
    private static final Map<UUID, KillAttempt> killAttempts = new HashMap<>();

    /**
     * Data structure to hold which mob is trying to kill a knocked player,
     * along with how many ticks remain before execution.
     */
    private static class KillAttempt {
        private final UUID mobUuid;
        private int timeLeft;

        public KillAttempt(UUID mobUuid, int timeLeft) {
            this.mobUuid = mobUuid;
            this.timeLeft = timeLeft;
        }
    }

    /**
     * Runs once per server tick; we look for mobs near knocked players
     * and handle the execution countdown.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Only run logic at the END of each tick on the server
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            tickKillAttempts();
        }
    }

    /**
     * Resets the kill timer if the mob about to perform the kill is attacked.
     * "If they're hit in those 3 seconds, the timer resets."
     */
    @SubscribeEvent
    public void onMobHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Mob mob)) {
            return;
        }

        UUID mobUuid = mob.getUUID();
        boolean mobWasReset = false;
        UUID playerToRelease = null;

        for (Map.Entry<UUID, KillAttempt> entry : killAttempts.entrySet()) {
            KillAttempt attempt = entry.getValue();
            if (attempt.mobUuid.equals(mobUuid)) {
                playerToRelease = entry.getKey();
                mobWasReset = true;
                break; // Break early because we will remove later
            }
        }

        if (playerToRelease != null) {
            killAttempts.remove(playerToRelease);
            Player player = getPlayerByUuid(playerToRelease);
            if (player != null) {
                KnockedManager.setGripped(player, false);
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }

        if (mobWasReset) {
            System.out.println("Mob hit: Canceling execution attempt and releasing gripped player.");
        }
    }

    public static void clearKillAttempt(UUID playerUuid) {
        killAttempts.remove(playerUuid);
    }

    /**
     * Main logic to find mobs near knocked players and manage execution timers.
     */
    private void tickKillAttempts() {
        Map<UUID, KillAttempt> updatedAttempts = new HashMap<>();

        for (UUID knockedPlayerUuid : KnockedManager.getKnockedUuids()) {
            Player knockedPlayer = getPlayerByUuid(knockedPlayerUuid);
            if (knockedPlayer == null || !knockedPlayer.isAlive()) {
                continue;
            }
            Mob mobInRange = getMobInRange(knockedPlayer);
            if (mobInRange == null) {
                setGripped(knockedPlayer, false);
                continue;
            }
            setGripped(knockedPlayer, true);
            KillAttempt attempt = killAttempts.get(knockedPlayerUuid);
            if (attempt == null || !attempt.mobUuid.equals(mobInRange.getUUID())) {
                attempt = new KillAttempt(mobInRange.getUUID(), EXECUTION_DELAY_TICKS);
            } else {
                attempt.timeLeft--;
                gripPlayer(knockedPlayer, mobInRange);
                if (attempt.timeLeft <= 0) {
                    executeKnockedPlayer(knockedPlayer, mobInRange);
                    continue;
                }
            }
            updatedAttempts.put(knockedPlayerUuid, attempt);
        }
        killAttempts.clear();
        killAttempts.putAll(updatedAttempts);
    }

    /**
     * Handles gripping behavior where the mob focuses on the knocked player.
     */
    private void gripPlayer(Player knockedPlayer, Mob mob) {
        mob.setTarget(knockedPlayer);
        mob.getNavigation().stop();
        spawnExecutionParticles(knockedPlayer, mob);
    }

    /**
     * Executes the knocked player when the countdown finishes.
     */
    private void executeKnockedPlayer(Player knockedPlayer, Mob mob) {
        knockedPlayer.setHealth(0.0F);
        mob.getNavigation().stop();
        removeKnockedState(knockedPlayer);
        knockedPlayer.sendSystemMessage(Component.literal(
                "You were executed by " + mob.getName().getString() + "!"
        ));
    }

    /**
     * Find a mob that is within a certain range of the knocked player.
     * Only hostile mobs, aggressive neutral mobs, and players are considered.
     */
    @Nullable
    private Mob getMobInRange(Player knockedPlayer) {
        if (knockedPlayer.level() instanceof ServerLevel serverLevel) {
            double range = 2.5;
            AABB aabb = new AABB(
                    knockedPlayer.getX() - range, knockedPlayer.getY() - range, knockedPlayer.getZ() - range,
                    knockedPlayer.getX() + range, knockedPlayer.getY() + range, knockedPlayer.getZ() + range
            );
            return serverLevel.getEntitiesOfClass(Mob.class, aabb).stream()
                    .filter(mob -> isHostile(mob) || isAggressiveNeutral(mob))
                    .findAny()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Checks if a mob is hostile (e.g., zombies, skeletons).
     */
    private boolean isHostile(Mob mob) {
        return mob instanceof net.minecraft.world.entity.monster.Monster;
    }

    /**
     * Checks if a neutral mob is aggressive (e.g., a wolf that has been attacked).
     */
    private boolean isAggressiveNeutral(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.animal.Wolf wolf) {
            return wolf.isAngry();
        }
        if (mob instanceof net.minecraft.world.entity.monster.EnderMan enderman) {
            return enderman.isCreepy();
        }
        return false;
    }

    /**
     * Helper to retrieve a Player by UUID from the server.
     */
    @Nullable
    private Player getPlayerByUuid(UUID uuid) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getPlayerList().getPlayer(uuid);
        }
        return null;
    }

    private void spawnExecutionParticles(Player knockedPlayer, Mob mobInRange) {
        if (knockedPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    mobInRange.getX(),
                    mobInRange.getY() + mobInRange.getBbHeight() / 2,
                    mobInRange.getZ(),
                    1, 0.1, 0.2, 0.1, 0.005
            );
        }
    }
}
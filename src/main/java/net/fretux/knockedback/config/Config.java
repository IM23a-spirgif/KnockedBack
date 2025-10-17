package net.fretux.knockedback.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue knockedDuration;
        public final ForgeConfigSpec.IntValue executionTime;
        public final ForgeConfigSpec.BooleanValue totemPreventsKnockdown;
        public final ForgeConfigSpec.BooleanValue explosionsBypassKnockdown;
        public final ForgeConfigSpec.BooleanValue fallDamageBypassesKnockdown;
        public final ForgeConfigSpec.BooleanValue lavaBypassesKnockdown;
        public final ForgeConfigSpec.BooleanValue mobsCanExecute;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");

            knockedDuration = builder
                    .comment("How long (in ticks) a player remains knocked before recovering. Default: 400 (20 seconds).")
                    .defineInRange("knockedDuration", 400, 100, 1200);

            executionTime = builder
                    .comment("How long (in ticks) it takes to execute a knocked player. Default: 60 (3 seconds).")
                    .defineInRange("executionTime", 60, 20, 200);

            builder.pop().push("damage_behavior");

            totemPreventsKnockdown = builder
                    .comment("If true, Totems of Undying prevent entering the knocked state. Default: true.")
                    .define("totemPreventsKnockdown", true);

            explosionsBypassKnockdown = builder
                    .comment("If true, explosions immediately kill instead of knocking down. Default: true.")
                    .define("explosionsBypassKnockdown", true);

            fallDamageBypassesKnockdown = builder
                    .comment("If true, fatal fall damage kills instead of knocking down. Default: true.")
                    .define("fallDamageBypassesKnockdown", true);

            lavaBypassesKnockdown = builder
                    .comment("If true, lava or fire damage kills instead of knocking down. Default: true.")
                    .define("lavaBypassesKnockdown", true);

            builder.pop().push("mobs");

            mobsCanExecute = builder
                    .comment("If true, hostile and aggressive neutral mobs can execute knocked players. Default: true.")
                    .define("mobsCanExecute", true);

            builder.pop();
        }
    }
}
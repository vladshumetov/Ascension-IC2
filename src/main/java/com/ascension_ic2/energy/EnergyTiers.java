package com.ascension_ic2.energy;

/**
 * The addon energy ladder: 13 levels based on the IC2 scale, where every level is four times the
 * previous one. Tiers 1-5 match IC2 exactly (32, 128, 512, 2048, 8192 EU/t); tiers 6-13 extend the
 * same rule beyond the IC2 API limit. Machines at tiers 6+ need the addon's own energy block and
 * transport, because IC2 cables and addon energy blocks stop at tier 5.
 */
public final class EnergyTiers {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 13;

    private static final long BASE_EU_PER_TICK = 32L;
    /** Buffers are sized for this many ticks of full input. */
    private static final int BUFFER_TICKS = 500;

    private EnergyTiers() {}

    /** EU/t (also the packet size) of a tier: 32 * 4^(tier - 1). */
    public static long euPerTick(int tier) {
        checkTier(tier);
        return BASE_EU_PER_TICK << (2 * (tier - 1));
    }

    public static double defaultCapacity(int tier) {
        return euPerTick(tier) * (double) BUFFER_TICKS;
    }

    /** Short name shown in tooltips and configs. */
    public static String name(int tier) {
        checkTier(tier);
        return switch (tier) {
            case 1 -> "LV";
            case 2 -> "MV";
            case 3 -> "HV";
            case 4 -> "EV";
            case 5 -> "IV";
            default -> "T" + tier;
        };
    }

    public static int fromEuPerTick(long euPerTick) {
        if (euPerTick < BASE_EU_PER_TICK) {
            return MIN_TIER;
        }
        int tier = MIN_TIER;
        while (tier < MAX_TIER && euPerTick(tier) < euPerTick) {
            tier++;
        }
        return tier;
    }

    public static void checkTier(int tier) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            throw new IllegalArgumentException(
                    "Energy tier must be between " + MIN_TIER + " and " + MAX_TIER + ": " + tier);
        }
    }
}

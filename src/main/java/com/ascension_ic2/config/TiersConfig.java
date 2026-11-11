package com.ascension_ic2.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TiersConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue NANO_ITEMS_PER_OPERATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("nano");
        NANO_ITEMS_PER_OPERATION =
                builder.comment(
                                "How many items a nano tier machine processes in one operation (1-64).",
                                "Each operation still consumes energy for the full processing time.")
                        .defineInRange("itemsPerOperation", 4, 1, 64);
        builder.pop();

        SPEC = builder.build();
    }

    private TiersConfig() {}
}

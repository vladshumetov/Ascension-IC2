package com.ascension_ic2.machine;

import ic2.api.recipe.Recipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Quantum tier machines: the same classic IC2 recipes as the nano tier, but twice as fast,
 * with a higher energy demand and five independent lanes. Each lane processes up to
 * {@code quantum.itemsPerOperation} items per operation (12 by default).
 */
public enum QuantumMachineKind implements TierMachineKind {
    MACERATOR("macerator", "ic2:macerator", 150, 2048, (level, input) -> NanoMachineKind.basic(Recipes.macerator, level, input)),
    ELECTRIC_FURNACE(
            "electric_furnace",
            "ic2:electric_furnace",
            50,
            4096,
            (level, input) -> NanoMachineKind.furnace(input)),
    COMPRESSOR("compressor", "ic2:compressor", 150, 2048, (level, input) -> NanoMachineKind.basic(Recipes.compressor, level, input)),
    EXTRACTOR("extractor", "ic2:extractor", 150, 2048, (level, input) -> NanoMachineKind.basic(Recipes.extractor, level, input)),
    RECYCLER("recycler", "ic2:recycler", 30, 1024, (level, input) -> NanoMachineKind.basic(Recipes.recycler, input));

    private final String baseId;
    private final String id;
    private final String baseMachineId;
    private final int processingTicks;
    private final int energyPerTick;
    private final RecipeLookup recipeLookup;

    QuantumMachineKind(
            String baseId,
            String baseMachineId,
            int processingTicks,
            int energyPerTick,
            RecipeLookup recipeLookup) {
        this.baseId = baseId;
        this.id = "quantum_" + baseId;
        this.baseMachineId = baseMachineId;
        this.processingTicks = processingTicks;
        this.energyPerTick = energyPerTick;
        this.recipeLookup = recipeLookup;
    }

    @Override
    public String id() {
        return this.id;
    }

    public String baseMachineId() {
        return this.baseMachineId;
    }

    @Override
    public int processingTicks() {
        return this.processingTicks;
    }

    @Override
    public int energyPerTick() {
        return this.energyPerTick;
    }

    @Override
    public int progressHeight() {
        return switch (this) {
            case MACERATOR -> 21;
            case ELECTRIC_FURNACE, COMPRESSOR, EXTRACTOR -> 23;
            case RECYCLER -> 17;
        };
    }

    @Override
    public String progressSprite() {
        return this.baseId;
    }

    @Override
    public MachineRecipePlan getRecipe(Level level, ItemStack input) {
        return this.recipeLookup.getRecipe(level, input);
    }

    @Override
    public String translationKey() {
        return "block.ascension_ic2." + this.id;
    }

    @FunctionalInterface
    public interface RecipeLookup {
        MachineRecipePlan getRecipe(Level level, ItemStack input);
    }
}

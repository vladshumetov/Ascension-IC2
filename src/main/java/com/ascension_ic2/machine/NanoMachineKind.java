package com.ascension_ic2.machine;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public enum NanoMachineKind implements TierMachineKind {
    MACERATOR("macerator", "ic2:macerator", 300, 8192, (level, input) -> basic(Recipes.macerator, level, input)),
    ELECTRIC_FURNACE(
            "electric_furnace",
            "ic2:electric_furnace",
            100,
            16384,
            (level, input) -> furnace(input)),
    COMPRESSOR("compressor", "ic2:compressor", 300, 8192, (level, input) -> basic(Recipes.compressor, level, input)),
    EXTRACTOR("extractor", "ic2:extractor", 300, 8192, (level, input) -> basic(Recipes.extractor, level, input)),
    RECYCLER("recycler", "ic2:recycler", 45, 4096, (level, input) -> basic(Recipes.recycler, input));

    private final String baseId;
    private final String id;
    private final String baseMachineId;
    private final int processingTicks;
    private final int energyPerTick;
    private final RecipeLookup recipeLookup;

    NanoMachineKind(
            String baseId,
            String baseMachineId,
            int processingTicks,
            int energyPerTick,
            RecipeLookup recipeLookup) {
        this.baseId = baseId;
        this.id = "nano_" + baseId;
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

    static MachineRecipePlan basic(
            Recipes.IGetter<IBasicMachineRecipeManager> getter, Level level, ItemStack input) {
        if (getter == null) {
            return MachineRecipePlan.EMPTY;
        }
        return basic(getter.get(level), input);
    }

    static MachineRecipePlan basic(IBasicMachineRecipeManager manager, ItemStack input) {
        if (manager == null || input.isEmpty()) {
            return MachineRecipePlan.EMPTY;
        }

        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result =
                manager.apply(input.copy(), true);
        if (result == null || result.getOutput() == null) {
            return MachineRecipePlan.EMPTY;
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : result.getOutput()) {
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }
        if (outputs.isEmpty()) {
            return MachineRecipePlan.EMPTY;
        }

        int inputAmount = 1;
        if (result.recipe() != null && result.recipe().getInput() != null) {
            inputAmount = Math.max(1, result.recipe().getInput().getAmount());
        }
        return new MachineRecipePlan(inputAmount, outputs);
    }

    static MachineRecipePlan furnace(ItemStack input) {
        if (Recipes.furnace == null || input.isEmpty()) {
            return MachineRecipePlan.EMPTY;
        }

        MachineRecipeResult<ItemStack, ItemStack, ItemStack> result =
                Recipes.furnace.apply(input.copy(), true);
        if (result == null || result.getOutput().isEmpty()) {
            return MachineRecipePlan.EMPTY;
        }
        return new MachineRecipePlan(1, List.of(result.getOutput().copy()));
    }

    @FunctionalInterface
    public interface RecipeLookup {
        MachineRecipePlan getRecipe(Level level, ItemStack input);
    }
}

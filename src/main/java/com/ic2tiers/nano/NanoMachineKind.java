package com.ic2tiers.nano;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public enum NanoMachineKind {
    MACERATOR(
            "macerator",
            "Nano Macerator",
            "ic2:macerator",
            300,
            2048,
            (level, input) -> basic(Recipes.macerator, level, input)),
    ELECTRIC_FURNACE(
            "electric_furnace",
            "Nano Electric Furnace",
            "ic2:electric_furnace",
            100,
            3072,
            (level, input) -> furnace(input)),
    COMPRESSOR(
            "compressor",
            "Nano Compressor",
            "ic2:compressor",
            300,
            2048,
            (level, input) -> basic(Recipes.compressor, level, input)),
    EXTRACTOR(
            "extractor",
            "Nano Extractor",
            "ic2:extractor",
            300,
            2048,
            (level, input) -> basic(Recipes.extractor, level, input)),
    RECYCLER(
            "recycler",
            "Nano Recycler",
            "ic2:recycler",
            45,
            1024,
            (level, input) -> basic(Recipes.recycler, input));

    public record RecipePlan(int inputAmount, List<ItemStack> outputs) {
        public static final RecipePlan EMPTY = new RecipePlan(0, List.of());

        public boolean isEmpty() {
            return this.inputAmount <= 0 || this.outputs.isEmpty();
        }
    }

    @FunctionalInterface
    public interface RecipeLookup {
        RecipePlan getRecipe(Level level, ItemStack input);
    }

    private final String id;
    private final String displayName;
    private final String baseMachineId;
    private final int processingTicks;
    private final int energyPerTick;
    private final RecipeLookup recipeLookup;

    NanoMachineKind(
            String id,
            String displayName,
            String baseMachineId,
            int processingTicks,
            int energyPerTick,
            RecipeLookup recipeLookup) {
        this.id = "nano_" + id;
        this.displayName = displayName;
        this.baseMachineId = baseMachineId;
        this.processingTicks = processingTicks;
        this.energyPerTick = energyPerTick;
        this.recipeLookup = recipeLookup;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public String baseMachineId() {
        return this.baseMachineId;
    }

    public int processingTicks() {
        return this.processingTicks;
    }

    public int energyPerTick() {
        return this.energyPerTick;
    }

    public RecipePlan getRecipe(Level level, ItemStack input) {
        return this.recipeLookup.getRecipe(level, input);
    }

    public String translationKey() {
        return "block.ic2_tiers." + this.id;
    }

    private static RecipePlan basic(
            Recipes.IGetter<IBasicMachineRecipeManager> getter, Level level, ItemStack input) {
        if (getter == null) {
            return RecipePlan.EMPTY;
        }
        return basic(getter.get(level), input);
    }

    private static RecipePlan basic(IBasicMachineRecipeManager manager, ItemStack input) {
        if (manager == null || input.isEmpty()) {
            return RecipePlan.EMPTY;
        }

        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result =
                manager.apply(input.copy(), true);
        if (result == null || result.getOutput() == null) {
            return RecipePlan.EMPTY;
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : result.getOutput()) {
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }
        if (outputs.isEmpty()) {
            return RecipePlan.EMPTY;
        }

        int inputAmount = 1;
        if (result.recipe() != null && result.recipe().getInput() != null) {
            inputAmount = Math.max(1, result.recipe().getInput().getAmount());
        }
        return new RecipePlan(inputAmount, outputs);
    }

    private static RecipePlan furnace(ItemStack input) {
        if (Recipes.furnace == null || input.isEmpty()) {
            return RecipePlan.EMPTY;
        }

        MachineRecipeResult<ItemStack, ItemStack, ItemStack> result =
                Recipes.furnace.apply(input.copy(), true);
        if (result == null || result.getOutput().isEmpty()) {
            return RecipePlan.EMPTY;
        }
        return new RecipePlan(1, List.of(result.getOutput().copy()));
    }
}

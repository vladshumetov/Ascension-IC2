package com.ascension_ic2.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A concrete machine type (macerator, furnace, ...) that can exist in any {@link MachineTier}. */
public interface TierMachineKind {
    /** Registry id of the block, for example {@code nano_macerator} or {@code quantum_macerator}. */
    String id();

    String translationKey();

    int processingTicks();

    int energyPerTick();

    /** Height of the progress arrow sprite, in pixels. */
    int progressHeight();

    /**
     * Base machine name used for the progress arrow sprite, for example {@code macerator}.
     * The arrow shape depends on the machine type, not on the tier.
     */
    String progressSprite();

    default boolean isRecycler() {
        return "recycler".equals(progressSprite());
    }

    MachineRecipePlan getRecipe(Level level, ItemStack input);
}

package com.ascension_ic2.machine;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** A resolved machine recipe: how many input items it consumes and what it produces. */
public record MachineRecipePlan(int inputAmount, List<ItemStack> outputs) {
    public static final MachineRecipePlan EMPTY = new MachineRecipePlan(0, List.of());

    public boolean isEmpty() {
        return this.inputAmount <= 0 || this.outputs.isEmpty();
    }
}

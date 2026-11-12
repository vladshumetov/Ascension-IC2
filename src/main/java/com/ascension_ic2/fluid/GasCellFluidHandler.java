package com.ascension_ic2.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Per-stack fluid handler for {@link GasCellItem}: always holds natural gas. */
public class GasCellFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;

    public GasCellFluidHandler(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getContainer() {
        return this.stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int gas = GasCellItem.getGas(this.stack);
        return gas <= 0 ? FluidStack.EMPTY : new FluidStack(NaturalGas.SOURCE.get(), gas);
    }

    @Override
    public int getTankCapacity(int tank) {
        return GasCellItem.CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid() == NaturalGas.SOURCE.get()
                || stack.getFluid() == NaturalGas.FLOWING.get();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        int gas = GasCellItem.getGas(this.stack);
        int accepted = Math.min(resource.getAmount(), GasCellItem.CAPACITY - gas);
        if (accepted > 0 && action.execute()) {
            GasCellItem.setGas(this.stack, gas + accepted);
        }
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int gas = GasCellItem.getGas(this.stack);
        int drained = Math.min(maxDrain, gas);
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            GasCellItem.setGas(this.stack, gas - drained);
        }
        return new FluidStack(NaturalGas.SOURCE.get(), drained);
    }
}

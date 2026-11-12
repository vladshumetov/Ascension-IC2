package com.ascension_ic2.energy;

import com.ascension_ic2.AscensionIC2;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Blood generator: consumes 512 EU/t and produces 10 mB/t of NeoVitae essentia vitae.
 * One free input slot is reserved for future recipes; four upgrade slots are ready for custom upgrades.
 */
public class BloodGeneratorBlockEntity extends TierEnergyBlockEntity implements MenuProvider {
    public static final int ENERGY_TIER = 3;
    public static final int EU_PER_TICK = 512;
    public static final double EU_CAPACITY = 1_024_000.0;
    public static final int BLOOD_PER_TICK = 10;
    public static final int TANK_CAPACITY = 8000;
    public static final int INPUT_SLOTS = 1;
    public static final int UPGRADE_SLOTS = 4;
    public static final int SLOT_COUNT = INPUT_SLOTS + UPGRADE_SLOTS;

    private static final ResourceLocation BLOOD_FLUID =
            ResourceLocation.fromNamespaceAndPath("neovitae", "essentia_vitae_source");

    private final FluidTank tank =
            new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == bloodFluid());
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);

    public BloodGeneratorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ENERGY_TIER, EU_CAPACITY, EU_PER_TICK, 0.0);
    }

    @Override
    protected boolean acceptsOvervoltage() {
        return true;
    }

    @Override
    protected boolean unlimitedInput() {
        return true;
    }

    public static net.minecraft.world.level.material.Fluid bloodFluid() {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(BLOOD_FLUID);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public boolean isGenerating() {
        return bloodFluid() != null
                && this.tank.getFluidAmount() + BLOOD_PER_TICK <= TANK_CAPACITY
                && getStoredEu() >= EU_PER_TICK;
    }

    @Override
    public void serverTick() {
        registerWithEnergyNet();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (!isGenerating()) {
            return;
        }
        removeEu(EU_PER_TICK);
        this.tank.fill(
                new FluidStack(bloodFluid(), BLOOD_PER_TICK),
                IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new BloodGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}

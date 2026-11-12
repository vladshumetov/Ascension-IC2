package com.ascension_ic2.energy;

import com.ascension_ic2.fluid.NaturalGas;
import ic2.api.block.IAddonServerTickable;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.ILocatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Natural gas generator: burns 1 mB per tick for 225 EU/t, so 1000 mB produce 225 000 EU.
 * Buffer: 4000 mB of gas and 40 000 EU. 1000 mB produce 20 000 EU, like geothermal lava.
 */
public class GasGeneratorBlockEntity extends BlockEntity
        implements IEnergySource, ILocatable, IAddonServerTickable, MenuProvider {
    public static final int TANK_CAPACITY = 4000;
    public static final int GAS_PER_TICK = 1;
    public static final int EU_PER_TICK = 20;
    public static final double EU_CAPACITY = 40_000.0;
    public static final int SOURCE_TIER = 1;

    private final FluidTank tank =
            new FluidTank(
                    TANK_CAPACITY,
                    stack ->
                            stack.getFluid() == NaturalGas.SOURCE.get()
                                    || stack.getFluid() == NaturalGas.FLOWING.get());
    private double stored;
    private long tick = Long.MIN_VALUE;
    private double usedThisTick;
    private boolean registered;

    public GasGeneratorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public double getStoredEu() {
        return this.stored;
    }

    public double getCapacityEu() {
        return EU_CAPACITY;
    }

    public int getEuPerTick() {
        return EU_PER_TICK;
    }

    public boolean isGenerating() {
        return this.tank.getFluidAmount() >= GAS_PER_TICK && this.stored < EU_CAPACITY;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        registerWithEnergyNet();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerWithEnergyNet();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        registerWithEnergyNet();
    }

    @Override
    public void setRemoved() {
        unregisterFromEnergyNet();
        super.setRemoved();
    }

    @Override
    public void serverTick() {
        registerWithEnergyNet();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        beginTick();
        if (this.tank.getFluidAmount() >= GAS_PER_TICK && this.stored < EU_CAPACITY) {
            this.tank.drain(GAS_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            this.stored = Math.min(EU_CAPACITY, this.stored + EU_PER_TICK);
            setChanged();
        }
    }

    private void registerWithEnergyNet() {
        if (this.level != null
                && !this.level.isClientSide
                && EnergyNet.instance != null
                && !this.registered) {
            EnergyNet.instance.addBlockEntityTile(this);
            this.registered = true;
            com.ascension_ic2.AscensionIC2.LOGGER.debug(
                    "Gas generator registered at {}", this.worldPosition);
        }
    }

    private void unregisterFromEnergyNet() {
        if (this.registered && EnergyNet.instance != null) {
            EnergyNet.instance.removeTile(this);
            this.registered = false;
        }
    }

    @Override
    public Level getWorldObj() {
        return this.level;
    }

    @Override
    public BlockPos getPosition() {
        return this.worldPosition;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction side) {
        return true;
    }

    @Override
    public double getOfferedEnergy() {
        beginTick();
        return Math.min(this.stored, Math.max(0.0, EU_PER_TICK - this.usedThisTick));
    }

    @Override
    public void drawEnergy(double amount) {
        beginTick();
        double available = Math.min(this.stored, Math.max(0.0, EU_PER_TICK - this.usedThisTick));
        double drawn = Math.min(amount, available);
        if (drawn > 0.0) {
            this.stored -= drawn;
            this.usedThisTick += drawn;
            setChanged();
        }
    }

    @Override
    public int getSourceTier() {
        return SOURCE_TIER;
    }

    private void beginTick() {
        long now = this.level != null ? this.level.getGameTime() : 0L;
        if (now != this.tick) {
            this.tick = now;
            this.usedThisTick = 0.0;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new GasGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Energy", this.stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        this.stored = Math.max(0.0, Math.min(EU_CAPACITY, tag.getDouble("Energy")));
    }
}

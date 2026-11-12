package com.ascension_ic2.energy;

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

/**
 * Solar panel with an internal buffer. It produces its rated EU/t in daylight, stores it, and
 * offers up to that rate per tick from the buffer. The GUI shows generation, buffer and tier.
 */
public class SolarPanelBlockEntity extends BlockEntity
        implements IEnergySource, ILocatable, IAddonServerTickable, MenuProvider {
    private static final double MAX_CAPACITY = 2_000_000_000.0;
    private static final int BUFFER_TICKS = 1200;

    private final int tier;
    private final double outputEuPerTick;
    private final double capacity;
    private double stored;
    private double generationThisTick;
    private long tick = Long.MIN_VALUE;
    private long productionTick = Long.MIN_VALUE;
    private double usedThisTick;
    private boolean registered;

    public SolarPanelBlockEntity(
            int tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(tier, EnergyTiers.euPerTick(tier), type, pos, state);
    }

    public SolarPanelBlockEntity(
            int sourceTier,
            double outputEuPerTick,
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state) {
        super(type, pos, state);
        EnergyTiers.checkTier(sourceTier);
        if (!Double.isFinite(outputEuPerTick) || outputEuPerTick <= 0.0) {
            throw new IllegalArgumentException("outputEuPerTick must be finite and positive");
        }
        this.tier = sourceTier;
        this.outputEuPerTick = outputEuPerTick;
        this.capacity = Math.min(MAX_CAPACITY, outputEuPerTick * BUFFER_TICKS);
    }

    public int getTier() {
        return this.tier;
    }

    public double getOutputEuPerTick() {
        return this.outputEuPerTick;
    }

    public double getCapacityEu() {
        return this.capacity;
    }

    public double getStoredEu() {
        return this.stored;
    }

    public double getGenerationEuPerTick() {
        return this.generationThisTick;
    }

    public boolean isProducing() {
        return canGenerate();
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
        long now = this.level.getGameTime();
        if (now == this.productionTick) {
            return;
        }
        this.productionTick = now;
        boolean produced = canGenerate() && this.stored < this.capacity;
        if (produced) {
            double added = Math.min(this.outputEuPerTick, this.capacity - this.stored);
            this.stored += added;
            this.generationThisTick = added;
            if (added > 0.0) {
                setChanged();
            }
        } else {
            this.generationThisTick = 0.0;
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
                    "EnergyNet solar registered: tier {} at {}", this.tier, this.worldPosition);
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
        return Math.min(this.stored, Math.max(0.0, this.outputEuPerTick - this.usedThisTick));
    }

    @Override
    public void drawEnergy(double amount) {
        beginTick();
        double available = Math.min(this.stored, Math.max(0.0, this.outputEuPerTick - this.usedThisTick));
        double drawn = Math.min(amount, available);
        if (drawn > 0.0) {
            this.stored = Math.max(0.0, this.stored - drawn);
            this.usedThisTick += drawn;
            setChanged();
        }
    }

    @Override
    public int getSourceTier() {
        return this.tier;
    }

    public boolean canGenerate() {
        return this.level != null
                && this.level.dimensionType().hasSkyLight()
                && this.level.canSeeSky(this.worldPosition.above())
                && this.level.isDay();
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
        return new SolarPanelMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Energy", this.stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        double loaded = tag.getDouble("Energy");
        this.stored = Double.isFinite(loaded) ? Math.max(0.0, Math.min(this.capacity, loaded)) : 0.0;
    }
}

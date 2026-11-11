package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.ILocatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Energy block for the addon's own ladder. Unlike IC2's addon energy block it accepts any tier
 * from {@link EnergyTiers} (1-14), which is required for tiers above the IC2 limit of 5.
 */
public abstract class TierEnergyBlockEntity extends BlockEntity
        implements IEnergySink, IEnergySource, ILocatable, IAddonServerTickable {
    public static final double FE_PER_EU = 2.0;

    private final int tier;
    private final double capacity;
    private final double inputLimit;
    private final double outputLimit;
    private double stored;
    private boolean registered;
    private long transferTick = Long.MIN_VALUE;
    private double inputUsedThisTick;
    private double outputUsedThisTick;
    private final IEnergyStorage feStorage = new FeStorage();

    protected TierEnergyBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int tier,
            double capacity,
            double inputLimit,
            double outputLimit) {
        super(type, pos, state);
        EnergyTiers.checkTier(tier);
        if (!Double.isFinite(capacity) || capacity <= 0.0) {
            throw new IllegalArgumentException("capacity must be finite and positive");
        }
        if (!Double.isFinite(inputLimit) || inputLimit < 0.0) {
            throw new IllegalArgumentException("inputLimit must be finite and non-negative");
        }
        if (!Double.isFinite(outputLimit) || outputLimit < 0.0) {
            throw new IllegalArgumentException("outputLimit must be finite and non-negative");
        }
        this.tier = tier;
        this.capacity = capacity;
        this.inputLimit = inputLimit;
        this.outputLimit = outputLimit;
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

    protected void registerWithEnergyNet() {
        if (this.level != null
                && !this.level.isClientSide
                && EnergyNet.instance != null
                && !this.registered) {
            EnergyNet.instance.addBlockEntityTile(this);
            this.registered = true;
            com.ascension_ic2.AscensionIC2.LOGGER.info(
                    "EnergyNet tile registered: {} tier {} at {}",
                    getClass().getSimpleName(),
                    this.tier,
                    this.worldPosition);
        }
    }

    private void unregisterFromEnergyNet() {
        if (this.registered && EnergyNet.instance != null) {
            EnergyNet.instance.removeTile(this);
            this.registered = false;
        }
    }

    /** Runs at the end of the server tick; the machine subclass spends energy here. */
    @Override
    public void serverTick() {}

    @Override
    public Level getWorldObj() {
        return this.level;
    }

    @Override
    public BlockPos getPosition() {
        return this.worldPosition;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return true;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction side) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        return Math.min(availableInputThisTick(), Math.max(0.0, this.capacity - this.stored));
    }

    @Override
    public int getSinkTier() {
        return this.tier;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            return 0.0;
        }
        if (this.level == null || this.level.isClientSide) {
            return amount;
        }

        int sinkTier = Math.min(EnergyTiers.MAX_TIER, Math.max(EnergyTiers.MIN_TIER, getSinkTier()));
        double maxVoltage = EnergyTiers.euPerTick(sinkTier);
        if (voltage > maxVoltage * 1.001) {
            explodeFromOvervoltage();
            return amount;
        }

        double accepted =
                Math.min(amount, Math.min(availableInputThisTick(), this.capacity - this.stored));
        if (accepted > 0.0) {
            this.stored += accepted;
            this.inputUsedThisTick += accepted;
            setChanged();
        }
        return amount - accepted;
    }

    private void explodeFromOvervoltage() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.level.removeBlock(this.worldPosition, false);
        this.level.explode(
                null,
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5,
                3.0F,
                Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public double getOfferedEnergy() {
        return Math.min(this.stored, availableOutputThisTick());
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount <= 0.0 || !Double.isFinite(amount)) {
            return;
        }
        double drawn = Math.min(amount, Math.min(this.stored, availableOutputThisTick()));
        if (drawn > 0.0) {
            this.stored = Math.max(0.0, this.stored - drawn);
            this.outputUsedThisTick += drawn;
            setChanged();
        }
    }

    @Override
    public int getSourceTier() {
        return this.tier;
    }

    private void beginTransferTick() {
        long now = this.level != null ? this.level.getGameTime() : 0L;
        if (now != this.transferTick) {
            this.transferTick = now;
            this.inputUsedThisTick = 0.0;
            this.outputUsedThisTick = 0.0;
        }
    }

    private double availableInputThisTick() {
        beginTransferTick();
        return Math.max(0.0, this.inputLimit - this.inputUsedThisTick);
    }

    private double availableOutputThisTick() {
        beginTransferTick();
        return Math.max(0.0, this.outputLimit - this.outputUsedThisTick);
    }

    public final double getStoredEu() {
        return this.stored;
    }

    public final void setStoredEu(double amount) {
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be finite");
        }
        this.stored = Math.max(0.0, Math.min(this.capacity, amount));
    }

    public final double getCapacityEu() {
        return this.capacity;
    }

    public final double getInputLimitEuPerTick() {
        return this.inputLimit;
    }

    public final double getOutputLimitEuPerTick() {
        return this.outputLimit;
    }

    public final int getEnergyTier() {
        return this.tier;
    }

    public final double addEu(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            return 0.0;
        }
        double added = Math.min(amount, this.capacity - this.stored);
        if (added > 0.0) {
            this.stored += added;
            setChanged();
        }
        return added;
    }

    public final double removeEu(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            return 0.0;
        }
        double removed = Math.min(amount, this.stored);
        if (removed > 0.0) {
            this.stored -= removed;
            setChanged();
        }
        return removed;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return this.feStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("addonEu", this.stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        double loaded = tag.getDouble("addonEu");
        this.stored = Double.isFinite(loaded) ? Math.max(0.0, Math.min(this.capacity, loaded)) : 0.0;
    }

    private final class FeStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            double eu = maxReceive / FE_PER_EU;
            double accepted =
                    Math.min(
                            eu,
                            Math.min(
                                    availableInputThisTick(),
                                    TierEnergyBlockEntity.this.capacity
                                            - TierEnergyBlockEntity.this.stored));
            if (accepted <= 0.0) {
                return 0;
            }
            if (!simulate) {
                TierEnergyBlockEntity.this.stored += accepted;
                TierEnergyBlockEntity.this.inputUsedThisTick += accepted;
                setChanged();
            }
            return (int) Math.floor(accepted * FE_PER_EU);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            double eu = maxExtract / FE_PER_EU;
            double drawn =
                    Math.min(
                            eu,
                            Math.min(
                                    TierEnergyBlockEntity.this.stored,
                                    availableOutputThisTick()));
            if (drawn <= 0.0) {
                return 0;
            }
            if (!simulate) {
                TierEnergyBlockEntity.this.stored -= drawn;
                TierEnergyBlockEntity.this.outputUsedThisTick += drawn;
                setChanged();
            }
            return (int) Math.floor(drawn * FE_PER_EU);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, TierEnergyBlockEntity.this.stored * FE_PER_EU);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int)
                    Math.min(
                            Integer.MAX_VALUE,
                            TierEnergyBlockEntity.this.capacity * FE_PER_EU);
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}

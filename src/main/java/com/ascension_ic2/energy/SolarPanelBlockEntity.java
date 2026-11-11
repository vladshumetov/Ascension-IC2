package com.ascension_ic2.energy;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.ILocatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solar panel for one energy tier. Generates a full tier packet per tick while it can see the sky
 * during the day. All tiers share a single texture.
 */
public class SolarPanelBlockEntity extends BlockEntity implements IEnergySource, ILocatable {
    private final int tier;
    private boolean registered;
    private long sourceTick = Long.MIN_VALUE;
    private double usedThisTick;

    public SolarPanelBlockEntity(
            int tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        EnergyTiers.checkTier(tier);
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
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

    private void registerWithEnergyNet() {
        if (this.level != null
                && !this.level.isClientSide
                && EnergyNet.instance != null
                && !this.registered) {
            EnergyNet.instance.addBlockEntityTile(this);
            this.registered = true;
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
        beginSourceTick();
        if (!canGenerate()) {
            return 0.0;
        }
        double perTick = EnergyTiers.euPerTick(this.tier);
        return Math.max(0.0, perTick - this.usedThisTick);
    }

    @Override
    public void drawEnergy(double amount) {
        beginSourceTick();
        double perTick = EnergyTiers.euPerTick(this.tier);
        double available = Math.max(0.0, perTick - this.usedThisTick);
        this.usedThisTick += Math.min(amount, available);
    }

    @Override
    public int getSourceTier() {
        return this.tier;
    }

    private boolean canGenerate() {
        return this.level != null
                && this.level.dimensionType().hasSkyLight()
                && this.level.canSeeSky(this.worldPosition.above())
                && this.level.isDay();
    }

    private void beginSourceTick() {
        long now = this.level != null ? this.level.getGameTime() : 0L;
        if (now != this.sourceTick) {
            this.sourceTick = now;
            this.usedThisTick = 0.0;
        }
    }
}

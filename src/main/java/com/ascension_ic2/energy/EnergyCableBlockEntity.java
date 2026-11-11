package com.ascension_ic2.energy;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.info.ILocatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cable for the addon energy ladder. IC2 derives the conductor tier from the breakdown energy,
 * so each cable is registered with the voltage of its tier.
 */
public class EnergyCableBlockEntity extends BlockEntity implements IEnergyConductor, ILocatable {
    private final int tier;
    private boolean registered;

    public EnergyCableBlockEntity(
            int tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        EnergyTiers.checkTier(tier);
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
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
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return true;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction side) {
        return true;
    }

    @Override
    public double getConductionLoss() {
        return 0.025;
    }

    @Override
    public double getInsulationEnergyAbsorption() {
        return EnergyTiers.euPerTick(this.tier) * 4.0;
    }

    @Override
    public double getInsulationBreakdownEnergy() {
        return EnergyTiers.euPerTick(this.tier);
    }

    @Override
    public double getConductorBreakdownEnergy() {
        return EnergyTiers.euPerTick(this.tier);
    }

    @Override
    public void removeInsulation() {}

    @Override
    public void removeConductor() {}
}

package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.info.ILocatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cable for the addon energy ladder. IC2 derives the conductor tier from the breakdown energy,
 * so each cable is registered with the voltage of its tier.
 */
public class EnergyCableBlockEntity extends BlockEntity
        implements IEnergyConductor, ILocatable, IAddonServerTickable {
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
    }

    private void registerWithEnergyNet() {
        if (this.level != null
                && !this.level.isClientSide
                && EnergyNet.instance != null
                && !this.registered) {
            EnergyNet.instance.addBlockEntityTile(this);
            this.registered = true;
            com.ascension_ic2.AscensionIC2.LOGGER.info(
                    "EnergyNet cable registered: tier {} at {}", this.tier, this.worldPosition);
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
        if (!(emitter instanceof EnergyCableBlockEntity)) {
            com.ascension_ic2.AscensionIC2.LOGGER.info(
                    "cable accepts energy from {} at {} side {}", emitter.getClass().getSimpleName(), this.worldPosition, side);
        }
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
    public void removeConductor() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.level.playSound(
                null,
                this.worldPosition,
                SoundEvents.GENERIC_EXTINGUISH_FIRE,
                SoundSource.BLOCKS,
                4.0F,
                1.0F);
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.SMOKE,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.95,
                    this.worldPosition.getZ() + 0.5,
                    8,
                    0.3,
                    0.15,
                    0.3,
                    0.02);
        }
        this.level.removeBlock(this.worldPosition, false);
    }
}

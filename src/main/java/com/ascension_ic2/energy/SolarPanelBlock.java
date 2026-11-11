package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Solar panel block that owns its block entity (all tiers share one texture). */
public class SolarPanelBlock extends Block implements EntityBlock {
    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory;

    public SolarPanelBlock(
            Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory) {
        super(properties);
        this.factory = factory;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof IAddonServerTickable tickable) {
                tickable.serverTick();
            }
        };
    }
}

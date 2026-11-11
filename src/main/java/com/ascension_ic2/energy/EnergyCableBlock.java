package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Cable block with a small selection box, so it is highlighted like an IC2 cable instead of a cube. */
public class EnergyCableBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE =
            Shapes.box(5.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0, 11.0 / 16.0, 11.0 / 16.0, 11.0 / 16.0);

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory;

    public EnergyCableBlock(
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

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

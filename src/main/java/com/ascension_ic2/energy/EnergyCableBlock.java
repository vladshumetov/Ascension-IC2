package com.ascension_ic2.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cable block with a small selection box, so it is highlighted like an IC2 cable instead of a cube. */
public class EnergyCableBlock extends Block {
    private static final VoxelShape SHAPE =
            Shapes.box(5.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0, 11.0 / 16.0, 11.0 / 16.0, 11.0 / 16.0);

    public EnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import ic2.api.energy.tile.IEnergyTile;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Cable block shaped and connected like the IC2 glass fibre cable: a core with arms on the sides
 * that touch another energy tile.
 */
public class EnergyCableBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;
    private static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;

    // Selection is the cable core plus the connected arms, exactly like IC2. Arms touch the core
    // at 6/16 so the union merges into one outline instead of separate boxes.
    private static final VoxelShape CORE_SHAPE =
            Shapes.box(6.0 / 16.0, 6.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0, 10.0 / 16.0, 10.0 / 16.0);
    private static final VoxelShape ARM_NORTH = Shapes.box(6.0 / 16.0, 6.0 / 16.0, 0.0, 10.0 / 16.0, 10.0 / 16.0, 6.0 / 16.0);
    private static final VoxelShape ARM_SOUTH = Shapes.box(6.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0, 10.0 / 16.0, 10.0 / 16.0, 1.0);
    private static final VoxelShape ARM_WEST = Shapes.box(0.0, 6.0 / 16.0, 6.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0, 10.0 / 16.0);
    private static final VoxelShape ARM_EAST = Shapes.box(10.0 / 16.0, 6.0 / 16.0, 6.0 / 16.0, 1.0, 10.0 / 16.0, 10.0 / 16.0);
    private static final VoxelShape ARM_UP = Shapes.box(6.0 / 16.0, 10.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0, 1.0, 10.0 / 16.0);
    private static final VoxelShape ARM_DOWN = Shapes.box(6.0 / 16.0, 0.0, 6.0 / 16.0, 10.0 / 16.0, 6.0 / 16.0, 10.0 / 16.0);

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory;

    public EnergyCableBlock(
            Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory) {
        super(properties);
        this.factory = factory;
        registerDefaultState(
                this.stateDefinition.any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
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
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updateConnections(level, pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        updateConnections(level, pos, state);
    }

    private void updateConnections(Level level, BlockPos pos, BlockState state) {
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean connect = isConnectable(level, neighborPos);
            BooleanProperty property = PROPERTIES.get(direction);
            if (updated.getValue(property) != connect) {
                updated = updated.setValue(property, connect);
            }
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    private static boolean isConnectable(Level level, BlockPos pos) {
        BlockState neighborState = level.getBlockState(pos);
        if (neighborState.getBlock() instanceof EnergyCableBlock) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof IEnergyTile) {
            return true;
        }
        // IC2 cables may not have their block entity available on the client; connect by block id.
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(neighborState.getBlock());
        return "ic2".equals(key.getNamespace())
                && (key.getPath().contains("cable") || key.getPath().contains("wire"));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PROPERTIES.get(direction))) {
                shape = Shapes.or(shape, arm(direction));
            }
        }
        return shape;
    }

    private static VoxelShape arm(Direction direction) {
        return switch (direction) {
            case NORTH -> ARM_NORTH;
            case SOUTH -> ARM_SOUTH;
            case WEST -> ARM_WEST;
            case EAST -> ARM_EAST;
            case UP -> ARM_UP;
            case DOWN -> ARM_DOWN;
        };
    }
}

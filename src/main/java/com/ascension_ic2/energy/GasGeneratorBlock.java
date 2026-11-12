package com.ascension_ic2.energy;

import ic2.api.block.IAddonServerTickable;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.item.ItemClassicCell;
import com.ascension_ic2.fluid.NaturalGas;
import org.apache.commons.lang3.mutable.MutableObject;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Natural gas generator block with a fluid tank and a small information GUI. */
public class GasGeneratorBlock extends Block implements EntityBlock {
    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory;

    public GasGeneratorBlock(
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
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return openMenu(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof GasGeneratorBlockEntity generator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof ItemClassicCell cell) {
            if (!level.isClientSide) {
                emptyGasCell(level, player, stack, hand, cell, generator);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(generator, buffer -> buffer.writeBlockPos(pos));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean emptyGasCell(
            Level level,
            Player player,
            ItemStack stack,
            InteractionHand hand,
            ItemClassicCell cell,
            GasGeneratorBlockEntity generator) {
        Ic2FluidStack gas = cell.getFluidStack(stack);
        if (gas.isEmpty()
                || (gas.getFluid() != NaturalGas.SOURCE.get()
                        && gas.getFluid() != NaturalGas.FLOWING.get())) {
            return false;
        }
        int accepted =
                generator
                        .getTank()
                        .fill(
                                new FluidStack(gas.getFluid(), gas.getAmountMb()),
                                IFluidHandler.FluidAction.EXECUTE);
        if (accepted <= 0) {
            return false;
        }
        MutableObject<ItemStack> replaced = new MutableObject<>(stack);
        int drained =
                cell.drainMb(
                        stack, gas.copyWithAmountMb(accepted), false, replaced);
        if (drained <= 0) {
            return false;
        }
        if (!level.isClientSide) {
            player.setItemInHand(hand, replaced.getValue() == null ? ItemStack.EMPTY : replaced.getValue());
            level.playSound(
                    null,
                    generator.getBlockPos(),
                    net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
        return true;
    }
    private static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof GasGeneratorBlockEntity generator)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(generator, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

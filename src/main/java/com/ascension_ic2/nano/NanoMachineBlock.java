package com.ascension_ic2.nano;

import com.mojang.serialization.MapCodec;
import ic2.api.block.AddonEnergyBlock;
import ic2.api.block.AddonWrenchableBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NanoMachineBlock extends AddonWrenchableBlock {
    private static final TagKey<Item> IC2_WRENCHES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("ic2", "wrenches"));

    private final NanoMachineKind kind;

    public NanoMachineBlock(
            BlockBehaviour.Properties properties,
            NanoMachineKind kind,
            BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory) {
        super(properties, factory);
        this.kind = kind;
    }

    public NanoMachineKind getKind() {
        return this.kind;
    }

    @Override
    protected MapCodec<? extends AddonEnergyBlock> codec() {
        return simpleCodec(
                properties -> new NanoMachineBlock(properties, this.kind, this.factory));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player.getOffhandItem().is(IC2_WRENCHES)) {
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
        if (player.isShiftKeyDown()
                || stack.is(IC2_WRENCHES)
                || player.getOffhandItem().is(IC2_WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return openMenuItem(level, pos, player);
    }

    private static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof NanoMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult openMenuItem(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof NanoMachineBlockEntity machine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getWrenchDrops(
            Level world,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            Player player,
            int fortune) {
        List<ItemStack> drops =
                new ArrayList<>(super.getWrenchDrops(world, pos, state, blockEntity, player, fortune));
        if (blockEntity instanceof NanoMachineBlockEntity nano) {
            nano.collectDrops(drops);
        }
        return drops;
    }
}

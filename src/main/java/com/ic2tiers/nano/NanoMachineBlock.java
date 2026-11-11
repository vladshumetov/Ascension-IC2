package com.ic2tiers.nano;

import com.mojang.serialization.MapCodec;
import ic2.api.block.AddonEnergyBlock;
import ic2.api.block.AddonWrenchableBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NanoMachineBlock extends AddonWrenchableBlock {
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

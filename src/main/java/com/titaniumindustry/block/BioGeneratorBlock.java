package com.titaniumindustry.block;

import com.mojang.serialization.MapCodec;
import com.titaniumindustry.block.entity.BioGeneratorBlockEntity;
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

public class BioGeneratorBlock extends AddonWrenchableBlock {
    public BioGeneratorBlock(
            BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory) {
        super(properties, factory);
    }

    @Override
    protected MapCodec<? extends AddonEnergyBlock> codec() {
        return simpleCodec(properties -> new BioGeneratorBlock(properties, this.factory));
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
        if (blockEntity instanceof BioGeneratorBlockEntity generator) {
            generator.collectDrops(drops);
        }
        return drops;
    }
}

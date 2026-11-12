package com.ascension_ic2.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** A 1000 mB natural gas cell. Filled from gas blocks, emptied into tanks/generators. */
public class GasCellItem extends Item {
    public static final int CAPACITY = 1000;
    private static final String TAG_GAS = "Gas";

    public GasCellItem(Properties properties) {
        super(properties);
    }

    public static int getGas(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null
                ? 0
                : Math.min(CAPACITY, Math.max(0, data.copyTag().getInt(TAG_GAS)));
    }

    public static void setGas(ItemStack stack, int amount) {
        int clamped = Math.min(CAPACITY, Math.max(0, amount));
        if (clamped <= 0) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.remove(TAG_GAS));
        } else {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.putInt(TAG_GAS, clamped));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        int gas = getGas(stack);

        if (gas <= 0) {
            FluidState fluid = level.getFluidState(pos);
            if (fluid.getType() == NaturalGas.SOURCE.get()
                    || fluid.getType() == NaturalGas.FLOWING.get()) {
                if (!level.isClientSide) {
                    setGas(stack, CAPACITY);
                    level.playSound(
                            null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) == null) {
            return InteractionResult.PASS;
        }
        IFluidHandler handler =
                level.getCapability(Capabilities.FluidHandler.BLOCK, pos, context.getClickedFace());
        if (handler == null) {
            return InteractionResult.PASS;
        }
        int accepted =
                handler.fill(
                        new FluidStack(NaturalGas.SOURCE.get(), gas),
                        IFluidHandler.FluidAction.EXECUTE);
        if (accepted <= 0) {
            return InteractionResult.PASS;
        }
        setGas(stack, gas - accepted);
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

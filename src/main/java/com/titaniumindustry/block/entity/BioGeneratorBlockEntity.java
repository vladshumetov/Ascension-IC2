package com.titaniumindustry.block.entity;

import ic2.api.block.AddonGeneratorBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BioGeneratorBlockEntity extends AddonGeneratorBlockEntity {
    public static final int FUEL_SLOT = 0;

    private final ItemStackHandler inventory =
            new ItemStackHandler(1) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return burnTime(stack) > 0;
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return ItemStack.EMPTY;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };

    private int fuelTicks;

    public BioGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 16.0, 1, 20_000, 16.0);
    }

    @Override
    protected boolean gainFuel() {
        if (this.fuelTicks > 0) {
            this.fuelTicks--;
            return true;
        }

        ItemStack fuel = this.inventory.getStackInSlot(FUEL_SLOT);
        int burnTime = burnTime(fuel);
        if (burnTime <= 0) {
            return false;
        }

        this.inventory.extractItem(FUEL_SLOT, 1, false);
        this.fuelTicks = burnTime - 1;
        setChanged();
        return true;
    }

    private static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ItemTags.COALS)) {
            return 1600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 1200;
        }
        if (stack.is(Items.STICK) || stack.is(Items.BAMBOO)) {
            return 100;
        }
        if (stack.is(ItemTags.SAPLINGS)) {
            return 200;
        }
        if (stack.is(ItemTags.LEAVES)) {
            return 100;
        }
        if (stack.is(ItemTags.LOGS)) {
            return 300;
        }
        if (stack.is(ItemTags.PLANKS)) {
            return 150;
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FuelTicks", this.fuelTicks);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fuelTicks = tag.getInt("FuelTicks");
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public int getFuelTicks() {
        return this.fuelTicks;
    }

    public void collectDrops(List<ItemStack> drops) {
        ItemStack fuel = this.inventory.getStackInSlot(FUEL_SLOT);
        if (!fuel.isEmpty()) {
            drops.add(fuel.copy());
        }
    }
}

package com.titaniumindustry.block.entity;

import com.titaniumindustry.TitaniumIndustry;
import ic2.api.block.AddonMachineBlockEntity;
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

public class TitaniumRefinerBlockEntity extends AddonMachineBlockEntity {
    public static final int INPUT_IRON = 0;
    public static final int INPUT_CARBON = 1;
    public static final int OUTPUT = 2;

    private final ItemStackHandler inventory =
            new ItemStackHandler(3) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return switch (slot) {
                        case INPUT_IRON -> stack.is(Items.IRON_INGOT);
                        case INPUT_CARBON -> stack.is(ItemTags.COALS);
                        default -> false;
                    };
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return slot == OUTPUT ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };

    public TitaniumRefinerBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, 10_000.0, 32.0, 0.0);
    }

    @Override
    public int getProcessingTime() {
        return 200;
    }

    @Override
    public int getEnergyPerTick() {
        return 16;
    }

    @Override
    public boolean canProcess() {
        ItemStack iron = this.inventory.getStackInSlot(INPUT_IRON);
        ItemStack carbon = this.inventory.getStackInSlot(INPUT_CARBON);
        if (!iron.is(Items.IRON_INGOT) || !carbon.is(ItemTags.COALS)) {
            return false;
        }

        ItemStack result = new ItemStack(TitaniumIndustry.TITANIUM.item("ingot").get());
        ItemStack output = this.inventory.getStackInSlot(OUTPUT);
        return output.isEmpty()
                || (output.is(result.getItem()) && output.getCount() < output.getMaxStackSize());
    }

    @Override
    public void processOperation() {
        if (this.inventory.getStackInSlot(INPUT_IRON).isEmpty()
                || this.inventory.getStackInSlot(INPUT_CARBON).isEmpty()) {
            return;
        }

        this.inventory.extractItem(INPUT_IRON, 1, false);
        this.inventory.extractItem(INPUT_CARBON, 1, false);

        ItemStack output = this.inventory.getStackInSlot(OUTPUT);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(
                    OUTPUT, new ItemStack(TitaniumIndustry.TITANIUM.item("ingot").get()));
        } else {
            output.grow(1);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public void collectDrops(List<ItemStack> drops) {
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }
}

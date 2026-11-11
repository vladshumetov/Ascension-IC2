package com.ic2tiers.nano;

import com.ic2tiers.config.TiersConfig;
import ic2.api.block.AddonMachineBlockEntity;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NanoMachineBlockEntity extends AddonMachineBlockEntity {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOTS = 6;
    public static final int OUTPUT_START = INPUT_SLOTS;
    public static final int SLOT_COUNT = INPUT_SLOTS + OUTPUT_SLOTS;

    private final NanoMachineKind kind;
    private final NanoInventory inventory = new NanoInventory();

    public NanoMachineBlockEntity(
            NanoMachineKind kind, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, 1_000_000.0, 8192.0, 0.0);
        this.kind = kind;
    }

    public NanoMachineKind getKind() {
        return this.kind;
    }

    @Override
    public int getProcessingTime() {
        return this.kind.processingTicks();
    }

    @Override
    public int getEnergyPerTick() {
        return this.kind.energyPerTick();
    }

    @Override
    public boolean canProcess() {
        if (this.level == null) {
            return false;
        }

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack input = this.inventory.getStackInSlot(slot);
            if (input.isEmpty()) {
                continue;
            }

            NanoMachineKind.RecipePlan plan = this.kind.getRecipe(this.level, input);
            if (!plan.isEmpty()
                    && input.getCount() >= plan.inputAmount()
                    && canFitOutputs(plan.outputs())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void processOperation() {
        if (this.level == null) {
            return;
        }

        int remaining = TiersConfig.NANO_ITEMS_PER_OPERATION.get();
        boolean changed = false;

        while (remaining > 0) {
            boolean processedInPass = false;

            for (int slot = 0; slot < INPUT_SLOTS && remaining > 0; slot++) {
                ItemStack input = this.inventory.getStackInSlot(slot);
                if (input.isEmpty()) {
                    continue;
                }

                NanoMachineKind.RecipePlan plan = this.kind.getRecipe(this.level, input);
                if (plan.isEmpty()
                        || input.getCount() < plan.inputAmount()
                        || !canFitOutputs(plan.outputs())) {
                    continue;
                }

                this.inventory.extractItem(slot, plan.inputAmount(), false);
                if (this.kind != NanoMachineKind.RECYCLER || rollRecyclerChance()) {
                    for (ItemStack output : plan.outputs()) {
                        ItemStack remainder = insertIntoOutputs(output.copy());
                        if (!remainder.isEmpty()) {
                            Block.popResource(this.level, this.worldPosition, remainder);
                        }
                    }
                }

                remaining -= plan.inputAmount();
                if (remaining < 0) {
                    remaining = 0;
                }
                processedInPass = true;
                changed = true;
            }

            if (!processedInPass) {
                break;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    public ItemStack insertInputs(ItemStack stack) {
        ItemStack remainder = stack;
        for (int slot = 0; slot < INPUT_SLOTS && !remainder.isEmpty(); slot++) {
            remainder = this.inventory.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    private ItemStack insertIntoOutputs(ItemStack stack) {
        ItemStack remainder = stack;
        for (int slot = OUTPUT_START; slot < SLOT_COUNT && !remainder.isEmpty(); slot++) {
            remainder = this.inventory.insertOutput(slot, remainder, false);
        }
        return remainder;
    }

    private boolean canFitOutputs(List<ItemStack> outputs) {
        ItemStack[] simulated = new ItemStack[OUTPUT_SLOTS];
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            simulated[slot] = this.inventory.getStackInSlot(OUTPUT_START + slot).copy();
        }

        for (ItemStack output : outputs) {
            if (!insertSimulated(simulated, output.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertSimulated(ItemStack[] slots, ItemStack stack) {
        int limit = Math.min(64, stack.getMaxStackSize());

        for (int slot = 0; slot < slots.length && !stack.isEmpty(); slot++) {
            if (slots[slot].isEmpty() || !ItemStack.isSameItemSameComponents(slots[slot], stack)) {
                continue;
            }
            int space = limit - slots[slot].getCount();
            if (space > 0) {
                int moved = Math.min(space, stack.getCount());
                slots[slot].grow(moved);
                stack.shrink(moved);
            }
        }

        for (int slot = 0; slot < slots.length && !stack.isEmpty(); slot++) {
            if (!slots[slot].isEmpty()) {
                continue;
            }
            int moved = Math.min(limit, stack.getCount());
            slots[slot] = stack.copyWithCount(moved);
            stack.shrink(moved);
        }

        return stack.isEmpty();
    }

    private boolean rollRecyclerChance() {
        int chance;
        try {
            chance = TileEntityRecycler.recycleChance();
        } catch (Throwable ignored) {
            chance = 8;
        }

        if (chance <= 1) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return this.level != null && this.level.random.nextInt(chance) == 0;
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
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }

    private final class NanoInventory extends ItemStackHandler {
        private NanoInventory() {
            super(SLOT_COUNT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < INPUT_SLOTS ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= OUTPUT_START ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        private ItemStack insertOutput(int slot, ItemStack stack, boolean simulate) {
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            NanoMachineBlockEntity.this.setChanged();
        }
    }
}

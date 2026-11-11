package com.ic2tiers.nano;

import com.ic2tiers.config.TiersConfig;
import ic2.api.block.AddonMachineBlockEntity;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NanoMachineBlockEntity extends AddonMachineBlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOTS = 3;
    public static final int OUTPUT_START = INPUT_SLOTS;
    public static final int SLOT_COUNT = INPUT_SLOTS + OUTPUT_SLOTS;

    private final NanoMachineKind kind;
    private final NanoInventory inventory = new NanoInventory();
    private final int[] laneProgress = new int[INPUT_SLOTS];
    private final ItemStack[] laneInput = new ItemStack[INPUT_SLOTS];

    public NanoMachineBlockEntity(
            NanoMachineKind kind, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, 1_000_000.0, 8192.0, 0.0);
        this.kind = kind;
        for (int lane = 0; lane < INPUT_SLOTS; lane++) {
            this.laneInput[lane] = ItemStack.EMPTY;
        }
    }

    public NanoMachineKind getKind() {
        return this.kind;
    }

    @Override
    public int getProcessingTime() {
        return 1;
    }

    public int getLaneProcessingTime() {
        return this.kind.processingTicks();
    }

    public int getLaneProgress(int lane) {
        if (lane < 0 || lane >= INPUT_SLOTS) {
            return 0;
        }
        return Math.min(this.laneProgress[lane], getLaneProcessingTime());
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

        for (int lane = 0; lane < INPUT_SLOTS; lane++) {
            if (canProcessLane(lane)) {
                return true;
            }
        }
        return false;
    }

    private boolean canProcessLane(int lane) {
        ItemStack input = this.inventory.getStackInSlot(lane);
        if (input.isEmpty()) {
            this.laneProgress[lane] = 0;
            this.laneInput[lane] = ItemStack.EMPTY;
            return false;
        }

        if (!this.laneInput[lane].isEmpty()
                && !ItemStack.isSameItemSameComponents(this.laneInput[lane], input)) {
            this.laneProgress[lane] = 0;
        }
        this.laneInput[lane] = input.copyWithCount(1);

        NanoMachineKind.RecipePlan plan = this.kind.getRecipe(this.level, input);
        if (plan.isEmpty() || input.getCount() < plan.inputAmount()) {
            this.laneProgress[lane] = 0;
            return false;
        }

        return canFitLaneOutput(lane, plan.outputs());
    }

    @Override
    public void processOperation() {
        if (this.level == null) {
            return;
        }

        int time = getLaneProcessingTime();
        boolean changed = false;

        for (int lane = 0; lane < INPUT_SLOTS; lane++) {
            if (!canProcessLane(lane)) {
                continue;
            }

            if (this.laneProgress[lane] < time) {
                this.laneProgress[lane]++;
            }

            if (this.laneProgress[lane] >= time) {
                this.laneProgress[lane] = 0;
                processLane(lane);
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    private void processLane(int lane) {
        int remaining = TiersConfig.NANO_ITEMS_PER_OPERATION.get();
        int outputSlot = OUTPUT_START + lane;
        boolean changed = false;

        while (remaining > 0) {
            ItemStack input = this.inventory.getStackInSlot(lane);
            if (input.isEmpty()) {
                break;
            }

            NanoMachineKind.RecipePlan plan = this.kind.getRecipe(this.level, input);
            if (plan.isEmpty() || input.getCount() < plan.inputAmount()) {
                break;
            }
            if (!canFitLaneOutput(lane, plan.outputs())) {
                break;
            }

            this.inventory.extractInput(lane, plan.inputAmount(), false);
            if (this.kind != NanoMachineKind.RECYCLER || rollRecyclerChance()) {
                for (ItemStack output : plan.outputs()) {
                    ItemStack remainder = this.inventory.insertOutput(outputSlot, output.copy(), false);
                    if (!remainder.isEmpty()) {
                        Block.popResource(this.level, this.worldPosition, remainder);
                    }
                }
            }

            remaining -= plan.inputAmount();
            if (remaining < 0) {
                remaining = 0;
            }
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean canFitLaneOutput(int lane, List<ItemStack> outputs) {
        ItemStack[] simulated = new ItemStack[] {
            this.inventory.getStackInSlot(OUTPUT_START + lane).copy()
        };

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
    public Component getDisplayName() {
        return Component.translatable(this.kind.translationKey());
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new NanoMachineMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        tag.putIntArray("LaneProgress", this.laneProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }

        int[] saved = tag.getIntArray("LaneProgress");
        for (int lane = 0; lane < INPUT_SLOTS && lane < saved.length; lane++) {
            this.laneProgress[lane] = Math.max(0, saved[lane]);
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
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = getStackInSlot(slot);
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                    return stack;
                }
                limit -= existing.getCount();
            }
            if (limit <= 0) {
                return stack;
            }

            int moved = Math.min(limit, stack.getCount());
            if (!simulate) {
                ItemStack result =
                        existing.isEmpty()
                                ? stack.copyWithCount(moved)
                                : existing.copyWithCount(existing.getCount() + moved);
                setStackInSlot(slot, result);
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(moved);
            return remainder;
        }

        private ItemStack extractInput(int slot, int amount, boolean simulate) {
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            NanoMachineBlockEntity.this.setChanged();
        }
    }
}

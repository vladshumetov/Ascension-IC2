package com.ic2tiers.nano;

import com.ic2tiers.IC2Tiers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NanoMachineMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = NanoMachineBlockEntity.SLOT_COUNT;
    public static final int INPUT_SLOTS = NanoMachineBlockEntity.INPUT_SLOTS;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = INPUT_SLOTS;

    private final @Nullable NanoMachineBlockEntity machine;
    private final Container container;
    private final ContainerData data;

    public NanoMachineMenu(
            int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public NanoMachineMenu(
            int containerId, Inventory playerInventory, @Nullable NanoMachineBlockEntity machine) {
        super(IC2Tiers.NANO_MENU.get(), containerId);
        this.machine = machine;
        this.container =
                machine != null
                        ? new MachineContainer(machine)
                        : new SimpleContainer(MACHINE_SLOTS);

        this.data =
                new ContainerData() {
                    private final int[] values = new int[INPUT_SLOTS + 1];

                    @Override
                    public int get(int index) {
                        if (NanoMachineMenu.this.machine != null
                                && NanoMachineMenu.this.machine.getLevel() != null
                                && !NanoMachineMenu.this.machine.getLevel().isClientSide) {
                            if (index >= 0 && index < INPUT_SLOTS) {
                                return NanoMachineMenu.this.machine.getLaneProgress(index);
                            }
                            if (index == DATA_MAX_PROGRESS) {
                                return NanoMachineMenu.this.machine.getLaneProcessingTime();
                            }
                            return 0;
                        }
                        return index >= 0 && index < this.values.length ? this.values[index] : 0;
                    }

                    @Override
                    public void set(int index, int value) {
                        if (index >= 0 && index < this.values.length) {
                            this.values[index] = value;
                        }
                    }

                    @Override
                    public int getCount() {
                        return this.values.length;
                    }
                };
        this.addDataSlots(this.data);

        for (int i = 0; i < INPUT_SLOTS; i++) {
            this.addSlot(new Slot(this.container, i, 56 + i * 56, 28));
        }
        for (int i = 0; i < NanoMachineBlockEntity.OUTPUT_SLOTS; i++) {
            this.addSlot(
                    new Slot(this.container, NanoMachineBlockEntity.OUTPUT_START + i, 56 + i * 56, 82) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(
                        new Slot(playerInventory, col + row * 9 + 9, 40 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 40 + col * 18, 198));
        }
    }

    public int getProgressScaled(int lane, int pixels) {
        if (lane < 0 || lane >= INPUT_SLOTS) {
            return 0;
        }
        int progress = this.data.get(DATA_PROGRESS + lane);
        int max = Math.max(1, this.data.get(DATA_MAX_PROGRESS));
        return progress * pixels / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine != null
                && this.machine.getLevel() != null
                && this.machine.getLevel().getBlockEntity(this.machine.getBlockPos()) == this.machine
                && player.distanceToSqr(Vec3.atCenterOf(this.machine.getBlockPos())) <= 64.0;
    }

    private static NanoMachineBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof NanoMachineBlockEntity nano ? nano : null;
    }

    private static final class MachineContainer implements Container {
        private final NanoMachineBlockEntity machine;

        private MachineContainer(NanoMachineBlockEntity machine) {
            this.machine = machine;
        }

        @Override
        public int getContainerSize() {
            return NanoMachineBlockEntity.SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.machine.getInventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = this.machine.getInventory().getStackInSlot(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = stack.split(amount);
            this.machine.getInventory().setStackInSlot(slot, stack);
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = this.machine.getInventory().getStackInSlot(slot);
            this.machine.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.machine.getInventory().setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            this.machine.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return this.machine.getLevel() != null
                    && this.machine.getLevel().getBlockEntity(this.machine.getBlockPos()) == this.machine
                    && player.distanceToSqr(Vec3.atCenterOf(this.machine.getBlockPos())) <= 64.0;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                setItem(slot, ItemStack.EMPTY);
            }
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}

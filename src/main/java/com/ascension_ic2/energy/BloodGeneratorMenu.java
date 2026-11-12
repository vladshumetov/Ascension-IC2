package com.ascension_ic2.energy;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.machine.TierMachineBlockEntity;
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

public class BloodGeneratorMenu extends AbstractContainerMenu {
    private static final int[][] UPGRADE_POSITIONS = {{8, 28}, {8, 62}, {216, 28}, {216, 62}};
    private static final int INPUT_X = 112;
    private static final int INPUT_Y = 28;

    private final @Nullable BloodGeneratorBlockEntity machine;
    private final Container container;
    private final ContainerData data;
    private final int slotCount;
    private final int upgradeStart;

    public BloodGeneratorMenu(
            int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public BloodGeneratorMenu(
            int containerId, Inventory playerInventory, @Nullable BloodGeneratorBlockEntity machine) {
        super(AscensionIC2.BLOOD_GENERATOR_MENU.get(), containerId);
        this.machine = machine;
        this.slotCount = BloodGeneratorBlockEntity.SLOT_COUNT;
        this.upgradeStart = BloodGeneratorBlockEntity.INPUT_SLOTS;
        this.container = machine != null ? new MachineContainer(machine) : new SimpleContainer(this.slotCount);
        this.data =
                new ContainerData() {
                    private final int[] values = new int[5];

                    @Override
                    public int get(int index) {
                        BloodGeneratorBlockEntity be = BloodGeneratorMenu.this.machine;
                        if (be != null && be.getLevel() != null && !be.getLevel().isClientSide) {
                            return switch (index) {
                                case 0 -> be.getTank().getFluidAmount();
                                case 1 -> be.getTank().getCapacity();
                                case 2 -> (int) be.getStoredEu();
                                case 3 -> (int) be.getCapacityEu();
                                case 4 -> be.isGenerating() ? 1 : 0;
                                default -> 0;
                            };
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

        this.addSlot(new Slot(this.container, 0, INPUT_X, INPUT_Y));
        for (int i = 0; i < BloodGeneratorBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(
                    new Slot(
                            this.container,
                            this.upgradeStart + i,
                            UPGRADE_POSITIONS[i][0],
                            UPGRADE_POSITIONS[i][1]) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return TierMachineBlockEntity.isUpgrade(stack);
                        }
                    });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 40 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 40 + col * 18, 198));
        }
    }

    public int getBlood() {
        return this.data.get(0);
    }

    public int getBloodCapacity() {
        return this.data.get(1);
    }

    public int getEnergy() {
        return this.data.get(2);
    }

    public int getEnergyCapacity() {
        return this.data.get(3);
    }

    public boolean isGenerating() {
        return this.data.get(4) != 0;
    }

    public int getBloodScaled(int pixels) {
        long blood = this.data.get(0);
        long capacity = Math.max(1, this.data.get(1));
        return (int) Math.min(pixels, blood * pixels / capacity);
    }

    public int getEnergyScaled(int pixels) {
        long energy = this.data.get(2);
        long capacity = Math.max(1, this.data.get(3));
        return (int) Math.min(pixels, energy * pixels / capacity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < this.slotCount) {
            if (!this.moveItemStackTo(stack, this.slotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TierMachineBlockEntity.isUpgrade(stack)) {
            if (!this.moveItemStackTo(
                    stack,
                    this.upgradeStart,
                    this.upgradeStart + BloodGeneratorBlockEntity.UPGRADE_SLOTS,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, BloodGeneratorBlockEntity.INPUT_SLOTS, false)) {
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
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine != null
                && this.machine.getLevel() != null
                && this.machine.getLevel().getBlockEntity(this.machine.getBlockPos()) == this.machine
                && player.distanceToSqr(Vec3.atCenterOf(this.machine.getBlockPos())) <= 64.0;
    }

    private static BloodGeneratorBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof BloodGeneratorBlockEntity machine ? machine : null;
    }

    private static final class MachineContainer implements Container {
        private final BloodGeneratorBlockEntity machine;

        private MachineContainer(BloodGeneratorBlockEntity machine) {
            this.machine = machine;
        }

        @Override
        public int getContainerSize() {
            return this.machine.getInventory().getSlots();
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
            return this.machine.getInventory().extractItem(slot, amount, false);
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

package com.ascension_ic2.machine;

import com.ascension_ic2.AscensionIC2;
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

public class TierMachineMenu extends AbstractContainerMenu {
    private static final int[][] UPGRADE_POSITIONS = {{8, 28}, {8, 62}, {216, 28}, {216, 62}};

    private final MachineTier tier;
    private final int inputSlots;
    private final int slotCount;
    private final int upgradeStart;
    private final @Nullable TierMachineBlockEntity machine;
    private final Container container;
    private final ContainerData data;

    private final int dataEnergy;
    private final int dataCapacity;

    public TierMachineMenu(
            MachineTier tier,
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buf) {
        this(tier, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public TierMachineMenu(
            MachineTier tier,
            int containerId,
            Inventory playerInventory,
            @Nullable TierMachineBlockEntity machine) {
        super(AscensionIC2.menuFor(tier).get(), containerId);
        if (machine != null) {
            tier = machine.getTier();
        }
        this.tier = tier;
        this.inputSlots = tier.lanes();
        this.slotCount = this.inputSlots * 2 + TierMachineBlockEntity.UPGRADE_SLOTS;
        this.upgradeStart = this.inputSlots * 2;
        this.machine = machine;
        this.container =
                machine != null ? new MachineContainer(machine) : new SimpleContainer(this.slotCount);

        int dataCount = this.inputSlots + 3;
        this.dataEnergy = this.inputSlots + 1;
        this.dataCapacity = this.inputSlots + 2;
        this.data = new ContainerData() {
            private final int[] values = new int[dataCount];

            @Override
            public int get(int index) {
                TierMachineBlockEntity be = TierMachineMenu.this.machine;
                if (be != null && be.getLevel() != null && !be.getLevel().isClientSide) {
                    if (index >= 0 && index < TierMachineMenu.this.inputSlots) {
                        return be.getLaneProgress(index);
                    }
                    if (index == TierMachineMenu.this.inputSlots) {
                        return be.getLaneProcessingTime();
                    }
                    if (index == TierMachineMenu.this.dataEnergy) {
                        return (int) be.getStoredEu();
                    }
                    if (index == TierMachineMenu.this.dataCapacity) {
                        return (int) be.getCapacityEu();
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

        for (int i = 0; i < this.inputSlots; i++) {
            this.addSlot(new Slot(this.container, i, tier.slotX(i), MachineTier.INPUT_Y));
        }
        for (int i = 0; i < this.inputSlots; i++) {
            this.addSlot(
                    new Slot(this.container, this.inputSlots + i, tier.slotX(i), MachineTier.OUTPUT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
        }
        for (int i = 0; i < TierMachineBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(
                    new Slot(
                            this.container,
                            this.upgradeStart + i,
                            UPGRADE_POSITIONS[i][0],
                            UPGRADE_POSITIONS[i][1]) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return TierMachineBlockEntity.isUpgrade(stack)
                                    && (machine == null || machine.isUpgradeSuitable(stack));
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

    public MachineTier getTier() {
        return this.tier;
    }

    public int getInputSlots() {
        return this.inputSlots;
    }

    /** Machine type of the open GUI, or {@code null} if the block entity is unavailable. */
    public @Nullable TierMachineKind getKind() {
        return this.machine != null ? this.machine.getKind() : null;
    }

    public int getProgressScaled(int lane, int pixels) {
        if (lane < 0 || lane >= this.inputSlots) {
            return 0;
        }
        int progress = this.data.get(lane);
        int max = Math.max(1, this.data.get(this.inputSlots));
        return progress * pixels / max;
    }

    public int getProgressPercent(int lane) {
        if (lane < 0 || lane >= this.inputSlots) {
            return 0;
        }
        int progress = this.data.get(lane);
        int max = Math.max(1, this.data.get(this.inputSlots));
        return progress * 100 / max;
    }

    public int getEnergy() {
        return this.data.get(this.dataEnergy);
    }

    public int getCapacity() {
        return this.data.get(this.dataCapacity);
    }

    public int getEnergyScaled(int pixels) {
        long energy = this.data.get(this.dataEnergy);
        long capacity = Math.max(1, this.data.get(this.dataCapacity));
        return (int) Math.min(pixels, energy * pixels / capacity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
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
                        this.upgradeStart + TierMachineBlockEntity.UPGRADE_SLOTS,
                        false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.inputSlots, false)) {
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

    private static TierMachineBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TierMachineBlockEntity machine ? machine : null;
    }

    private static final class MachineContainer implements Container {
        private final TierMachineBlockEntity machine;

        private MachineContainer(TierMachineBlockEntity machine) {
            this.machine = machine;
        }

        @Override
        public int getContainerSize() {
            return this.machine.getSlotCount();
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

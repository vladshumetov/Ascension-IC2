package com.ascension_ic2.energy;

import com.ascension_ic2.AscensionIC2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Read-only gas generator GUI data: gas in tank, EU buffer, generation state. */
public class GasGeneratorMenu extends AbstractContainerMenu {
    private final @Nullable GasGeneratorBlockEntity generator;
    private final ContainerData data;

    public GasGeneratorMenu(
            int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public GasGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable GasGeneratorBlockEntity generator) {
        super(AscensionIC2.GAS_GENERATOR_MENU.get(), containerId);
        this.generator = generator;
        this.data =
                new ContainerData() {
                    private final int[] values = new int[5];

                    @Override
                    public int get(int index) {
                        GasGeneratorBlockEntity be = GasGeneratorMenu.this.generator;
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
    }

    public int getGas() {
        return this.data.get(0);
    }

    public int getGasCapacity() {
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

    public int getEnergyScaled(int pixels) {
        long energy = this.data.get(2);
        long capacity = Math.max(1, this.data.get(3));
        return (int) Math.min(pixels, energy * pixels / capacity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.generator != null
                && this.generator.getLevel() != null
                && this.generator.getLevel().getBlockEntity(this.generator.getBlockPos()) == this.generator
                && player.distanceToSqr(Vec3.atCenterOf(this.generator.getBlockPos())) <= 64.0;
    }

    private static GasGeneratorBlockEntity getBlockEntity(
            Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof GasGeneratorBlockEntity generator ? generator : null;
    }
}

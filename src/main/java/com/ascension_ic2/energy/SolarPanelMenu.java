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

/** Read-only solar panel information screen: generation, buffer, tier and status. */
public class SolarPanelMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 6;

    private final @Nullable SolarPanelBlockEntity panel;
    private final ContainerData data;

    public SolarPanelMenu(
            int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public SolarPanelMenu(
            int containerId, Inventory playerInventory, @Nullable SolarPanelBlockEntity panel) {
        super(AscensionIC2.SOLAR_MENU.get(), containerId);
        this.panel = panel;
        this.data =
                new ContainerData() {
                    private final int[] values = new int[DATA_COUNT];

                    @Override
                    public int get(int index) {
                        SolarPanelBlockEntity be = SolarPanelMenu.this.panel;
                        if (be != null && be.getLevel() != null && !be.getLevel().isClientSide) {
                            return switch (index) {
                                case 0 -> (int) be.getGenerationEuPerTick();
                                case 1 -> (int) be.getOutputEuPerTick();
                                case 2 -> (int) be.getStoredEu();
                                case 3 -> (int) be.getCapacityEu();
                                case 4 -> be.isProducing() ? 1 : 0;
                                case 5 -> be.getTier();
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

    public int getGeneration() {
        return this.data.get(0);
    }

    public int getOutput() {
        return this.data.get(1);
    }

    public int getStored() {
        return this.data.get(2);
    }

    public int getCapacity() {
        return this.data.get(3);
    }

    public boolean isProducing() {
        return this.data.get(4) != 0;
    }

    public int getTier() {
        return this.data.get(5);
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
        return this.panel != null
                && this.panel.getLevel() != null
                && this.panel.getLevel().getBlockEntity(this.panel.getBlockPos()) == this.panel
                && player.distanceToSqr(Vec3.atCenterOf(this.panel.getBlockPos())) <= 64.0;
    }

    private static SolarPanelBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SolarPanelBlockEntity panel ? panel : null;
    }
}

package com.ascension_ic2.energy;

import com.ascension_ic2.integration.botania.BotaniaManaCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Botania greenhouse: consumes Botania flowers and 256 EU/t to create mana, pushing it into
 * adjacent mana pools. Accepts any voltage and never explodes.
 */
public class BotaniaGreenhouseBlockEntity extends TierEnergyBlockEntity implements MenuProvider {
    public static final int ENERGY_TIER = 3;
    public static final int EU_PER_TICK = 256;
    public static final double EU_CAPACITY = 1_024_000.0;
    public static final int MANA_PER_TICK = 100;
    public static final int MANA_CAPACITY = 100_000;
    public static final int FLOWER_TICKS = 100;
    public static final int FLOWER_SLOTS = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int SLOT_COUNT = FLOWER_SLOTS + UPGRADE_SLOTS;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
    private int mana;
    private int flowerProgress;

    public BotaniaGreenhouseBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ENERGY_TIER, EU_CAPACITY, EU_PER_TICK, 0.0);
    }

    @Override
    protected boolean acceptsOvervoltage() {
        return true;
    }

    @Override
    protected boolean unlimitedInput() {
        return true;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public int getMana() {
        return this.mana;
    }

    public int getManaCapacity() {
        return MANA_CAPACITY;
    }

    public boolean isGenerating() {
        return hasFlower() && getStoredEu() >= EU_PER_TICK && this.mana < MANA_CAPACITY;
    }

    public static boolean isBotaniaFlower(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "botania".equals(id.getNamespace())
                && (id.getPath().contains("flower") || id.getPath().contains("mushroom"));
    }

    private boolean hasFlower() {
        for (int slot = 0; slot < FLOWER_SLOTS; slot++) {
            if (isBotaniaFlower(this.inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeFlower() {
        for (int slot = 0; slot < FLOWER_SLOTS; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (isBotaniaFlower(stack)) {
                this.inventory.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverTick() {
        registerWithEnergyNet();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (!isGenerating()) {
            return;
        }
        removeEu(EU_PER_TICK);
        this.mana = Math.min(MANA_CAPACITY, this.mana + MANA_PER_TICK);
        if (ModList.get() != null && ModList.get().isLoaded("botania")) {
            int leftover =
                    BotaniaManaCompat.pushToNeighbours(
                            this.level, this.worldPosition, MANA_PER_TICK);
            if (leftover < MANA_PER_TICK) {
                this.mana = Math.max(0, this.mana - (MANA_PER_TICK - leftover));
            }
        }
        if (++this.flowerProgress >= FLOWER_TICKS) {
            this.flowerProgress = 0;
            consumeFlower();
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new BotaniaGreenhouseMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mana", this.mana);
        tag.putInt("FlowerProgress", this.flowerProgress);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mana = Math.max(0, Math.min(MANA_CAPACITY, tag.getInt("Mana")));
        this.flowerProgress = Math.max(0, tag.getInt("FlowerProgress"));
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}

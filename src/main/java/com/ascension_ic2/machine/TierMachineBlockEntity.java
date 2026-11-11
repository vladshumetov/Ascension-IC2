package com.ascension_ic2.machine;

import ic2.api.block.AddonMachineBlockEntity;
import ic2.api.upgrade.IEnergyStorageUpgrade;
import ic2.api.upgrade.IProcessingUpgrade;
import ic2.api.upgrade.ITransformerUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class TierMachineBlockEntity extends AddonMachineBlockEntity
        implements MenuProvider, IUpgradableBlock {
    public static final int UPGRADE_SLOTS = 4;

    private static final Set<UpgradableProperty> UPGRADABLE_PROPERTIES =
            Set.of(
                    UpgradableProperty.Processing,
                    UpgradableProperty.Transformer,
                    UpgradableProperty.ItemProducing,
                    UpgradableProperty.ItemConsuming);

    private static final TagKey<Item> UPGRADE_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("ascension_ic2", "upgrades"));

    public static boolean isUpgrade(ItemStack stack) {
        return stack.is(UPGRADE_TAG);
    }

    private final MachineTier tier;
    private final TierMachineKind kind;
    private final int inputSlots;
    private final int outputStart;
    private final int upgradeStart;
    private final int slotCount;
    private final TierInventory inventory;
    private final int[] laneProgress;
    private final ItemStack[] laneInput;

    // Upgrade rates, recalculated whenever the upgrade slots change (same maths as IC2 InvSlotUpgrade).
    private int extraProcessTime;
    private double processTimeMultiplier = 1.0;
    private int extraEnergyDemand;
    private double energyDemandMultiplier = 1.0;
    private int extraTier;
    private int extraEnergyStorage;
    private double energyStorageMultiplier = 1.0;

    public TierMachineBlockEntity(
            MachineTier tier,
            TierMachineKind kind,
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state) {
        super(type, pos, state, tier.energyTier(), tier.capacity(), tier.maxTransfer(), 0.0);
        this.tier = tier;
        this.kind = kind;
        this.inputSlots = tier.lanes();
        this.outputStart = this.inputSlots;
        this.upgradeStart = this.inputSlots * 2;
        this.slotCount = this.upgradeStart + UPGRADE_SLOTS;
        this.inventory = new TierInventory(this.slotCount);
        this.laneProgress = new int[this.inputSlots];
        this.laneInput = new ItemStack[this.inputSlots];
        for (int lane = 0; lane < this.inputSlots; lane++) {
            this.laneInput[lane] = ItemStack.EMPTY;
        }
        recalcUpgrades();
    }

    public MachineTier getTier() {
        return this.tier;
    }

    public TierMachineKind getKind() {
        return this.kind;
    }

    public int getInputSlots() {
        return this.inputSlots;
    }

    public int getOutputStart() {
        return this.outputStart;
    }

    public int getUpgradeStart() {
        return this.upgradeStart;
    }

    public int getSlotCount() {
        return this.slotCount;
    }

    @Override
    public int getProcessingTime() {
        return 1;
    }

    /** Processing time in ticks after overclocker upgrades. */
    public int getLaneProcessingTime() {
        double ticks = (this.kind.processingTicks() + this.extraProcessTime) * this.processTimeMultiplier;
        return Math.max(1, (int) Math.round(ticks));
    }

    public int getLaneProgress(int lane) {
        if (lane < 0 || lane >= this.inputSlots) {
            return 0;
        }
        return Math.min(this.laneProgress[lane], getLaneProcessingTime());
    }

    /** Energy per tick after overclocker upgrades. */
    @Override
    public int getEnergyPerTick() {
        double eu = (this.kind.energyPerTick() + this.extraEnergyDemand) * this.energyDemandMultiplier;
        return Math.max(1, (int) Math.round(eu));
    }

    /** Tier after transformer upgrades. The base energy block already caps input at its own limit. */
    @Override
    public int getSinkTier() {
        return super.getSinkTier() + this.extraTier;
    }

    @Override
    public int getSourceTier() {
        return super.getSourceTier() + this.extraTier;
    }

    // ---------- IC2 upgrades ----------

    @Override
    public double getEnergy() {
        return getStoredEu();
    }

    @Override
    public boolean useEnergy(double amount) {
        return removeEu(amount) + 1.0E-9 >= amount;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return UPGRADABLE_PROPERTIES;
    }

    /** Whether IC2 considers this upgrade suitable for the machine (used by the upgrade slot). */
    public boolean isUpgradeSuitable(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeItem upgrade
                && upgrade.isSuitableFor(stack, UPGRADABLE_PROPERTIES);
    }

    private void recalcUpgrades() {
        this.extraProcessTime = 0;
        this.processTimeMultiplier = 1.0;
        this.extraEnergyDemand = 0;
        this.energyDemandMultiplier = 1.0;
        this.extraTier = 0;
        this.extraEnergyStorage = 0;
        this.energyStorageMultiplier = 1.0;

        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack stack = this.inventory.getStackInSlot(this.upgradeStart + i);
            if (stack.isEmpty() || !(stack.getItem() instanceof IUpgradeItem upgrade)) {
                continue;
            }
            int count = stack.getCount();

            if (upgrade instanceof IProcessingUpgrade processing) {
                this.extraProcessTime += processing.getExtraProcessTime(stack, this) * count;
                this.processTimeMultiplier *=
                        Math.pow(processing.getProcessTimeMultiplier(stack, this), count);
                this.extraEnergyDemand += processing.getExtraEnergyDemand(stack, this) * count;
                this.energyDemandMultiplier *=
                        Math.pow(processing.getEnergyDemandMultiplier(stack, this), count);
            }
            if (upgrade instanceof IEnergyStorageUpgrade storage) {
                this.extraEnergyStorage += storage.getExtraEnergyStorage(stack, this) * count;
                this.energyStorageMultiplier *=
                        Math.pow(storage.getEnergyStorageMultiplier(stack, this), count);
            }
            if (upgrade instanceof ITransformerUpgrade transformer) {
                this.extraTier += transformer.getExtraTier(stack, this) * count;
            }
        }
    }

    /** Ticks ejector/pulling upgrades; returns true if the upgrade changed anything. */
    private boolean tickUpgrades() {
        boolean changed = false;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack stack = this.inventory.getStackInSlot(this.upgradeStart + i);
            if (!stack.isEmpty() && stack.getItem() instanceof IUpgradeItem upgrade) {
                changed |= upgrade.onTick(stack, this);
            }
        }
        return changed;
    }

    private List<ItemStack> applyProcessEnd(List<ItemStack> outputs) {
        if (outputs.isEmpty()) {
            return outputs;
        }
        Collection<ItemStack> result = outputs;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack stack = this.inventory.getStackInSlot(this.upgradeStart + i);
            if (!stack.isEmpty() && stack.getItem() instanceof IUpgradeItem upgrade) {
                result = upgrade.onProcessEnd(stack, this, result);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public boolean canProcess() {
        if (this.level == null) {
            return false;
        }

        // Ejector/pulling upgrades must keep working even when no lane can process.
        if (tickUpgrades()) {
            setChanged();
        }

        for (int lane = 0; lane < this.inputSlots; lane++) {
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

        MachineRecipePlan plan = this.kind.getRecipe(this.level, input);
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

        for (int lane = 0; lane < this.inputSlots; lane++) {
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
        int remaining = this.tier.itemsPerOperation();
        int outputSlot = this.outputStart + lane;
        boolean changed = false;

        while (remaining > 0) {
            ItemStack input = this.inventory.getStackInSlot(lane);
            if (input.isEmpty()) {
                break;
            }

            MachineRecipePlan plan = this.kind.getRecipe(this.level, input);
            if (plan.isEmpty() || input.getCount() < plan.inputAmount()) {
                break;
            }
            if (!canFitLaneOutput(lane, plan.outputs())) {
                break;
            }

            this.inventory.extractInput(lane, plan.inputAmount(), false);
            if (!this.kind.isRecycler() || rollRecyclerChance()) {
                for (ItemStack output : applyProcessEnd(plan.outputs())) {
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
            this.inventory.getStackInSlot(this.outputStart + lane).copy()
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
        return new TierMachineMenu(this.tier, containerId, playerInventory, this);
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
        recalcUpgrades();

        int[] saved = tag.getIntArray("LaneProgress");
        for (int lane = 0; lane < this.inputSlots && lane < saved.length; lane++) {
            this.laneProgress[lane] = Math.max(0, saved[lane]);
        }
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public void collectDrops(List<ItemStack> drops) {
        for (int slot = 0; slot < this.slotCount; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }

    private final class TierInventory extends ItemStackHandler {
        private TierInventory(int size) {
            super(size);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < TierMachineBlockEntity.this.inputSlots
                    || (slot >= TierMachineBlockEntity.this.upgradeStart && isUpgrade(stack));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return (slot < TierMachineBlockEntity.this.outputStart
                            || slot >= TierMachineBlockEntity.this.upgradeStart)
                    ? super.insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= TierMachineBlockEntity.this.outputStart
                    ? super.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
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
            if (slot >= TierMachineBlockEntity.this.upgradeStart) {
                recalcUpgrades();
            }
            TierMachineBlockEntity.this.setChanged();
        }
    }
}

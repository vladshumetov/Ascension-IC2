package com.ascension_ic2.energy;

import appeng.api.config.Actionable;
import com.ascension_ic2.integration.botania.BotaniaManaCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;
import vazkii.botania.ae2.ae2.SafeMana;
import vazkii.botania.api.mana.ManaReceiver;

/**
 * Botania greenhouse: keeps generating/functional Botania flowers alive and consumes resource
 * items (water buckets, lava buckets, coal, food...) to create mana. Accepts any voltage and
 * never explodes. Mana is pushed into adjacent ManaPools/ManaReceivers (also Applied Botanics).
 */
public class BotaniaGreenhouseBlockEntity extends TierEnergyBlockEntity
        implements MenuProvider, ManaReceiver, SafeMana {
    public static final int ENERGY_TIER = 3;
    public static final int EU_PER_TICK = 256;
    public static final double EU_CAPACITY = 1_024_000.0;
    public static final int MANA_PER_TICK = 100;
    public static final int MANA_CAPACITY = 100_000;
    public static final int RESOURCE_TICKS = 100;
    public static final int FLOWER_SLOTS = 3;
    public static final int RESOURCE_SLOTS = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int UPGRADE_START = FLOWER_SLOTS + RESOURCE_SLOTS;
    public static final int SLOT_COUNT = UPGRADE_START + UPGRADE_SLOTS;

    private static final TagKey<Item> GENERATING_FLOWERS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("botania", "generating_special_flowers"));

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
    private int mana;
    private int resourceProgress;

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

    private static boolean ascension$injectLogged;

    @Override
    public double injectEnergy(
            net.minecraft.core.Direction direction, double amount, double voltage) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            return 0.0;
        }
        if (this.level == null || this.level.isClientSide) {
            return amount;
        }
        if (!ascension$injectLogged) {
            ascension$injectLogged = true;
            com.ascension_ic2.AscensionIC2.LOGGER.debug(
                    "Greenhouse injectEnergy: amount={}, voltage={}, stored={}",
                    amount, voltage, getStoredEu());
        }
        double accepted = Math.min(amount, getCapacityEu() - getStoredEu());
        if (accepted > 0.0) {
            addEu(accepted);
            setChanged();
        }
        return amount - accepted;
    }
    @Override
    public net.minecraft.world.level.Level getManaReceiverLevel() {
        return this.level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return this.worldPosition;
    }

    @Override
    public int getCurrentMana() {
        return this.mana;
    }

    @Override
    public boolean isFull() {
        return this.mana >= MANA_CAPACITY;
    }

    @Override
    public void receiveMana(int amount) {
        if (amount <= 0) {
            return;
        }
        this.mana = Math.min(MANA_CAPACITY, this.mana + amount);
        setChanged();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }

    @Override
    public int insert(int amount, Actionable actionable) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, MANA_CAPACITY - this.mana);
        if (accepted > 0 && actionable == Actionable.MODULATE) {
            this.mana += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int extract(int amount, Actionable actionable) {
        if (amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, this.mana);
        if (extracted > 0 && actionable == Actionable.MODULATE) {
            this.mana -= extracted;
            setChanged();
        }
        return extracted;
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

    public int getResourceStart() {
        return FLOWER_SLOTS;
    }

    public boolean isGenerating() {
        return getFlower() != null
                && hasResourceFor(getFlower())
                && getStoredEu() >= EU_PER_TICK
                && this.mana < MANA_CAPACITY;
    }

    public static boolean isBotaniaFlower(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GENERATING_FLOWERS);
    }

    private ItemStack getFlower() {
        for (int slot = 0; slot < FLOWER_SLOTS; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (isBotaniaFlower(stack)) {
                return stack;
            }
        }
        return null;
    }

    private boolean hasResourceFor(ItemStack flower) {
        for (int slot = FLOWER_SLOTS; slot < FLOWER_SLOTS + RESOURCE_SLOTS; slot++) {
            if (resourceMatches(flower, this.inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeResource(ItemStack flower) {
        for (int slot = FLOWER_SLOTS; slot < FLOWER_SLOTS + RESOURCE_SLOTS; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (resourceMatches(flower, stack)) {
                this.inventory.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    /** Resource mapping for the common generating flowers, mirroring their Botania fuel type. */
    private static boolean resourceMatches(ItemStack flower, ItemStack resource) {
        if (resource.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(flower.getItem()).getPath();
        if (path.contains("hydroangeas")) {
            return resource.is(Items.WATER_BUCKET);
        }
        if (path.contains("thermalily")) {
            return resource.is(Items.LAVA_BUCKET);
        }
        if (path.contains("gourmaryllis")) {
            return resource.get(DataComponents.FOOD) != null;
        }
        if (path.contains("endoflame")) {
            return resource.is(ItemTags.COALS)
                    || resource.is(Items.CHARCOAL)
                    || resource.is(Items.BLAZE_ROD);
        }
        // Other generating flowers accept a generic resource for now.
        return true;
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
        if (++this.resourceProgress >= RESOURCE_TICKS) {
            this.resourceProgress = 0;
            ItemStack flower = getFlower();
            if (flower != null) {
                consumeResource(flower);
            }
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
        tag.putInt("ResourceProgress", this.resourceProgress);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mana = Math.max(0, Math.min(MANA_CAPACITY, tag.getInt("Mana")));
        this.resourceProgress = Math.max(0, tag.getInt("ResourceProgress"));
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}

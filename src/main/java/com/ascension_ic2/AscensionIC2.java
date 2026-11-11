package com.ascension_ic2;

import com.ascension_ic2.config.TiersConfig;
import com.ascension_ic2.energy.EnergyCableBlock;
import com.ascension_ic2.energy.EnergyCableBlockEntity;
import com.ascension_ic2.energy.EnergyTiers;
import com.ascension_ic2.energy.SolarPanelBlockEntity;
import com.ascension_ic2.event.MachineEvents;
import com.ascension_ic2.machine.MachineTier;
import com.ascension_ic2.machine.NanoMachineKind;
import com.ascension_ic2.machine.QuantumMachineKind;
import com.ascension_ic2.machine.TierMachineBlock;
import com.ascension_ic2.machine.TierMachineBlockEntity;
import com.ascension_ic2.machine.TierMachineBlockItem;
import com.ascension_ic2.machine.TierMachineKind;
import com.ascension_ic2.machine.TierMachineMenu;
import ic2.api.registry.Ic2AddonContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

@Mod(AscensionIC2.MOD_ID)
public final class AscensionIC2 {
    public static final String MOD_ID = "ascension_ic2";
    public static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("ascension_ic2");

    public static final Ic2AddonContent CONTENT = Ic2AddonContent.create(MOD_ID);

    public record MachineRegistration(
            MachineTier tier,
            TierMachineKind kind,
            DeferredBlock<TierMachineBlock> block,
            DeferredItem<? extends BlockItem> item,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<TierMachineBlockEntity>> blockEntity) {}
    /** Every registered machine, nano first, then quantum. */
    public static final List<MachineRegistration> MACHINES = new ArrayList<>();

    public record EnergyBlockRegistration(
            int tier,
            DeferredBlock<Block> block,
            DeferredItem<BlockItem> item,
            DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> blockEntity) {}

    public static final List<EnergyBlockRegistration> CABLES = new ArrayList<>();
    public static final List<EnergyBlockRegistration> SOLAR_PANELS = new ArrayList<>();

    static {
        for (MachineTier tier : MachineTier.values()) {
            for (TierMachineKind kind : kindsFor(tier)) {
                MACHINES.add(registerMachine(tier, kind));
            }
        }
        for (int tier = 6; tier <= EnergyTiers.MAX_TIER; tier++) {
            CABLES.add(registerCable(tier));
        }
        for (int tier = EnergyTiers.MIN_TIER; tier <= EnergyTiers.MAX_TIER; tier++) {
            SOLAR_PANELS.add(registerSolarPanel(tier));
        }
    }

    private static TierMachineKind[] kindsFor(MachineTier tier) {
        return switch (tier) {
            case NANO -> NanoMachineKind.values();
            case QUANTUM -> QuantumMachineKind.values();
        };
    }

    private static MachineRegistration registerMachine(MachineTier tier, TierMachineKind kind) {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<?>, BlockEntityType<TierMachineBlockEntity>>[] holder =
                new DeferredHolder[1];

        DeferredBlock<TierMachineBlock> block =
                CONTENT.registerBlock(
                        kind.id(),
                        properties ->
                                new TierMachineBlock(
                                        properties,
                                        kind,
                                        (pos, state) ->
                                                new TierMachineBlockEntity(
                                                        tier, kind, holder[0].get(), pos, state)),
                        casingProperties(tier == MachineTier.NANO ? 5.0F : 8.0F));
        DeferredItem<TierMachineBlockItem> item =
                CONTENT.registerItem(
                        kind.id(),
                        () ->
                                new TierMachineBlockItem(
                                        block.get(), new Item.Properties(), tier, kind));
        holder[0] =
                CONTENT.registerBlockEntityType(
                        kind.id(),
                        (pos, state) -> new TierMachineBlockEntity(tier, kind, holder[0].get(), pos, state),
                        block);
        return new MachineRegistration(tier, kind, block, item, holder[0]);
    }

    private static EnergyBlockRegistration registerCable(int tier) {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>>[] holder =
                new DeferredHolder[1];

        String id = "energy_cable_" + tier;
        DeferredBlock<Block> block =
                CONTENT.registerBlock(
                        id,
                        EnergyCableBlock::new,
                        BlockBehaviour.Properties.of()
                                .strength(0.4F)
                                .sound(SoundType.GLASS));
        DeferredItem<BlockItem> item = CONTENT.registerBlockItem(id, block);
        holder[0] =
                CONTENT.registerBlockEntityType(
                        id,
                        (pos, state) -> new EnergyCableBlockEntity(tier, holder[0].get(), pos, state),
                        block);
        return new EnergyBlockRegistration(tier, block, item, holder[0]);
    }

    private static EnergyBlockRegistration registerSolarPanel(int tier) {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>>[] holder =
                new DeferredHolder[1];

        String id = "solar_panel_" + tier;
        DeferredBlock<Block> block =
                CONTENT.registerBlock(
                        id,
                        Block::new,
                        BlockBehaviour.Properties.of()
                                .strength(3.0F, 6.0F)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.METAL));
        DeferredItem<BlockItem> item = CONTENT.registerBlockItem(id, block);
        holder[0] =
                CONTENT.registerBlockEntityType(
                        id,
                        (pos, state) -> new SolarPanelBlockEntity(tier, holder[0].get(), pos, state),
                        block);
        return new EnergyBlockRegistration(tier, block, item, holder[0]);
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TierMachineMenu>> NANO_MENU =
            MENUS.register(
                    "nano_machine",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, buf) ->
                                            new TierMachineMenu(MachineTier.NANO, id, inventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<TierMachineMenu>> QUANTUM_MENU =
            MENUS.register(
                    "quantum_machine",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, buf) ->
                                            new TierMachineMenu(MachineTier.QUANTUM, id, inventory, buf)));

    public static DeferredHolder<MenuType<?>, MenuType<TierMachineMenu>> menuFor(MachineTier tier) {
        return switch (tier) {
            case NANO -> NANO_MENU;
            case QUANTUM -> QUANTUM_MENU;
        };
    }

    /** Same block properties as the IC2 machine casings: metal, pickaxe, blast resistance 10. */
    private static BlockBehaviour.Properties casingProperties(float hardness) {
        return BlockBehaviour.Properties.of()
                .strength(hardness, 10.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CONTENT.registerCreativeModeTab(
                    "main",
                    () -> new ItemStack(MACHINES.getFirst().item().get()),
                    AscensionIC2::fillTab);

    public static final DeferredBlock<Block> NANO_CASING_BLOCK =
            CONTENT.registerBlock(
                    "nano_casing", Block::new, casingProperties(5.0F));

    public static final DeferredBlock<Block> QUANTUM_CASING_BLOCK =
            CONTENT.registerBlock(
                    "quantum_casing", Block::new, casingProperties(8.0F));

    public static final DeferredItem<BlockItem> NANO_CASING =
            CONTENT.registerBlockItem("nano_casing", NANO_CASING_BLOCK);

    public static final DeferredItem<BlockItem> QUANTUM_CASING =
            CONTENT.registerBlockItem("quantum_casing", QUANTUM_CASING_BLOCK);

    public static final DeferredItem<Item> NANO_CIRCUIT =
            CONTENT.registerItem("nano_circuit", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> QUANTUM_CIRCUIT =
            CONTENT.registerItem("quantum_circuit", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> QUANTUM_CRYSTAL =
            CONTENT.registerItem("quantum_crystal", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SINGULAR_CRYSTAL =
            CONTENT.registerItem("singular_crystal", () -> new Item(new Item.Properties()));

    public AscensionIC2(IEventBus modEventBus, ModContainer modContainer) {
        CONTENT.register(modEventBus);
        MENUS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TiersConfig.SPEC);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(MachineEvents.class);
    }

    private static void fillTab(
            CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (MachineRegistration registration : MACHINES) {
            output.accept(registration.item().get());
        }
        output.accept(NANO_CASING.get());
        output.accept(QUANTUM_CASING.get());
        output.accept(NANO_CIRCUIT.get());
        output.accept(QUANTUM_CIRCUIT.get());
        output.accept(QUANTUM_CRYSTAL.get());
        output.accept(SINGULAR_CRYSTAL.get());
        for (EnergyBlockRegistration registration : CABLES) {
            output.accept(registration.item().get());
        }
        for (EnergyBlockRegistration registration : SOLAR_PANELS) {
            output.accept(registration.item().get());
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (MachineRegistration registration : MACHINES) {
            BlockEntityType<TierMachineBlockEntity> type = registration.blockEntity().get();
                event.registerBlockEntity(
                        Capabilities.EnergyStorage.BLOCK,
                        type,
                        (blockEntity, side) -> blockEntity.getEnergyStorage(side));
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    type,
                    (blockEntity, side) -> blockEntity.getInventory());
        }
    }
}

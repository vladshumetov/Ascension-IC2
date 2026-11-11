package com.ascension_ic2;

import com.ascension_ic2.config.TiersConfig;
import com.ascension_ic2.event.MachineEvents;
import com.ascension_ic2.nano.NanoMachineBlock;
import com.ascension_ic2.nano.NanoMachineBlockEntity;
import com.ascension_ic2.nano.NanoMachineKind;
import com.ascension_ic2.nano.NanoMachineMenu;
import ic2.api.registry.AddonEnergyCapabilities;
import ic2.api.registry.Ic2AddonContent;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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

@Mod(AscensionIC2.MOD_ID)
public final class AscensionIC2 {
    public static final String MOD_ID = "ascension_ic2";

    public static final Ic2AddonContent CONTENT = Ic2AddonContent.create(MOD_ID);

    public static final Map<NanoMachineKind, DeferredBlock<NanoMachineBlock>> NANO_BLOCKS =
            new EnumMap<>(NanoMachineKind.class);
    public static final Map<NanoMachineKind, DeferredItem<net.minecraft.world.item.BlockItem>>
            NANO_ITEMS = new EnumMap<>(NanoMachineKind.class);
    public static final Map<
                    NanoMachineKind,
                    DeferredHolder<BlockEntityType<?>, BlockEntityType<NanoMachineBlockEntity>>>
            NANO_BES = new EnumMap<>(NanoMachineKind.class);

    static {
        for (NanoMachineKind kind : NanoMachineKind.values()) {
            DeferredBlock<NanoMachineBlock> block =
                    CONTENT.registerBlock(
                            kind.id(),
                            properties ->
                                    new NanoMachineBlock(
                                            properties,
                                            kind,
                                            (pos, state) ->
                                                    new NanoMachineBlockEntity(
                                                            kind, NANO_BES.get(kind).get(), pos, state)),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
            NANO_BLOCKS.put(kind, block);
            NANO_ITEMS.put(kind, CONTENT.registerBlockItem(kind.id(), block));
            NANO_BES.put(
                    kind,
                    CONTENT.registerBlockEntityType(
                            kind.id(),
                            (pos, state) ->
                                    new NanoMachineBlockEntity(
                                            kind, NANO_BES.get(kind).get(), pos, state),
                            block));
        }
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<NanoMachineMenu>> NANO_MENU =
            MENUS.register("nano_machine", () -> IMenuTypeExtension.create(NanoMachineMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CONTENT.registerCreativeModeTab(
                    "main",
                    () -> new ItemStack(NANO_ITEMS.get(NanoMachineKind.MACERATOR).get()),
                    AscensionIC2::fillTab);

    public AscensionIC2(IEventBus modEventBus, ModContainer modContainer) {
        CONTENT.register(modEventBus);
        MENUS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TiersConfig.SPEC);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(MachineEvents.class);
    }

    private static void fillTab(
            CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (DeferredItem<net.minecraft.world.item.BlockItem> item : NANO_ITEMS.values()) {
            output.accept(item.get());
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredHolder<BlockEntityType<?>, BlockEntityType<NanoMachineBlockEntity>> holder :
                NANO_BES.values()) {
            AddonEnergyCapabilities.registerEnergyStorage(event, holder.get());
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    holder.get(),
                    (blockEntity, side) -> blockEntity.getInventory());
        }
    }

}


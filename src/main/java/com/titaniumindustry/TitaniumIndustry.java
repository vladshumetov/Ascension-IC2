package com.titaniumindustry;

import com.titaniumindustry.block.BioGeneratorBlock;
import com.titaniumindustry.block.TitaniumRefinerBlock;
import com.titaniumindustry.block.entity.BioGeneratorBlockEntity;
import com.titaniumindustry.block.entity.TitaniumRefinerBlockEntity;
import com.titaniumindustry.event.TitaniumEvents;
import ic2.api.item.AddonElectricMiningTool;
import ic2.api.registry.AddonEnergyCapabilities;
import ic2.api.registry.AddonMaterialSet;
import ic2.api.registry.Ic2AddonContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

@Mod(TitaniumIndustry.MOD_ID)
public final class TitaniumIndustry {
    public static final String MOD_ID = "titanium_industry";

    public static final Ic2AddonContent CONTENT = Ic2AddonContent.create(MOD_ID);

    public static final DeferredBlock<TitaniumRefinerBlock> REFINER_BLOCK =
            CONTENT.registerBlock(
                    "titanium_refiner",
                    properties -> new TitaniumRefinerBlock(properties, TitaniumIndustry::createRefiner),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> REFINER_ITEM =
            CONTENT.registerBlockItem("titanium_refiner", REFINER_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TitaniumRefinerBlockEntity>>
            REFINER_BE =
                    CONTENT.registerBlockEntityType(
                            "titanium_refiner", TitaniumIndustry::createRefiner, REFINER_BLOCK);

    public static final DeferredBlock<BioGeneratorBlock> GENERATOR_BLOCK =
            CONTENT.registerBlock(
                    "bio_generator",
                    properties -> new BioGeneratorBlock(properties, TitaniumIndustry::createGenerator),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> GENERATOR_ITEM =
            CONTENT.registerBlockItem("bio_generator", GENERATOR_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BioGeneratorBlockEntity>>
            GENERATOR_BE =
                    CONTENT.registerBlockEntityType(
                            "bio_generator", TitaniumIndustry::createGenerator, GENERATOR_BLOCK);

    public static final AddonMaterialSet TITANIUM =
            AddonMaterialSet.builder(CONTENT, "titanium")
                    .itemForm("ingot")
                    .itemForm("dust")
                    .itemForm("plate")
                    .blockForm("block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
                    .build();

    public static final DeferredItem<AddonElectricMiningTool> TITANIUM_DRILL =
            CONTENT.registerElectricMiningTool(
                    "titanium_drill",
                    new Item.Properties().stacksTo(1),
                    30_000.0,
                    100.0,
                    1,
                    false,
                    50,
                    40,
                    8.0F);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CONTENT.registerCreativeModeTab(
                    "main",
                    () -> new ItemStack(REFINER_ITEM.get()),
                    TitaniumIndustry::fillTab);

    public TitaniumIndustry(IEventBus modEventBus) {
        CONTENT.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(TitaniumEvents.class);
    }

    private static TitaniumRefinerBlockEntity createRefiner(BlockPos pos, BlockState state) {
        return new TitaniumRefinerBlockEntity(REFINER_BE.get(), pos, state);
    }

    private static BioGeneratorBlockEntity createGenerator(BlockPos pos, BlockState state) {
        return new BioGeneratorBlockEntity(GENERATOR_BE.get(), pos, state);
    }

    private static void fillTab(
            CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(REFINER_ITEM.get());
        output.accept(GENERATOR_ITEM.get());
        output.accept(TITANIUM_DRILL.get());
        output.accept(TITANIUM.item("ingot").get());
        output.accept(TITANIUM.item("dust").get());
        output.accept(TITANIUM.item("plate").get());
        output.accept(TITANIUM.blockItem("block").get());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        AddonEnergyCapabilities.registerEnergyStorage(event, REFINER_BE.get());
        AddonEnergyCapabilities.registerEnergyStorage(event, GENERATOR_BE.get());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                REFINER_BE.get(),
                (blockEntity, side) -> blockEntity.getInventory());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GENERATOR_BE.get(),
                (blockEntity, side) -> blockEntity.getInventory());
    }
}

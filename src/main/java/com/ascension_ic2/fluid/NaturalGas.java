package com.ascension_ic2.fluid;

import com.ascension_ic2.AscensionIC2;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Natural gas fluid, its source/flowing variants, block and bucket. */
public final class NaturalGas {
    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, AscensionIC2.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, AscensionIC2.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> TYPE =
            TYPES.register(
                    "natural_gas",
                    () ->
                            new FluidType(
                                    FluidType.Properties.create()
                                            .density(-100)
                                            .viscosity(80)
                                            .canConvertToSource(false)
                                            .descriptionId(
                                                    "fluid_type.ascension_ic2.natural_gas")));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source>
            SOURCE =
                    FLUIDS.register(
                            "natural_gas", () -> new BaseFlowingFluid.Source(properties()));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing>
            FLOWING =
                    FLUIDS.register(
                            "flowing_natural_gas",
                            () -> new BaseFlowingFluid.Flowing(properties()));

    public static final DeferredBlock<LiquidBlock> BLOCK =
            AscensionIC2.CONTENT.registerBlock(
                    "natural_gas",
                    props -> new LiquidBlock(SOURCE.get(), props),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                            .noCollission()
                            .strength(100.0F)
                            .noLootTable());


    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING)
                .block(BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    private NaturalGas() {}
}

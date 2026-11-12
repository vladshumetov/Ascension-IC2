package com.ascension_ic2.integration.jade;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.client.GuiNumbers;
import com.ascension_ic2.energy.GasGeneratorBlockEntity;
import com.ascension_ic2.energy.SolarPanelBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.config.IPluginConfig;

/** Jade provider for addon solar panels and the natural gas generator. */
public enum EnergyComponentProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AscensionIC2.MOD_ID, "energy");

    public static void registerCommon(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(INSTANCE, Block.class);
    }

    public static void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(INSTANCE, Block.class);
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof SolarPanelBlockEntity solar) {
            tag.putString("kind", "solar");
            tag.putDouble("energy", solar.getStoredEu());
            tag.putDouble("capacity", solar.getCapacityEu());
            tag.putDouble("generation", solar.getGenerationEuPerTick());
            tag.putInt("tier", solar.getTier());
        } else if (accessor.getBlockEntity() instanceof GasGeneratorBlockEntity gas) {
            tag.putString("kind", "gas");
            tag.putDouble("energy", gas.getStoredEu());
            tag.putDouble("capacity", gas.getCapacityEu());
            tag.putInt("gas", gas.getTank().getFluidAmount());
            tag.putInt("gas_capacity", gas.getTank().getCapacity());
            tag.putInt("output", gas.getEuPerTick());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        String kind = data.getString("kind");
        if ("solar".equals(kind)) {
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.solar.energy",
                                    GuiNumbers.format((long) data.getDouble("energy")),
                                    GuiNumbers.format((long) data.getDouble("capacity")))
                            .withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.solar.generation",
                                    GuiNumbers.format((long) data.getDouble("generation")))
                            .withStyle(ChatFormatting.AQUA));
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.solar.tier", data.getInt("tier"))
                            .withStyle(ChatFormatting.DARK_GRAY));
        } else if ("gas".equals(kind)) {
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.gas.energy",
                                    GuiNumbers.format((long) data.getDouble("energy")),
                                    GuiNumbers.format((long) data.getDouble("capacity")))
                            .withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.gas.gas",
                                    GuiNumbers.format(data.getInt("gas")),
                                    GuiNumbers.format(data.getInt("gas_capacity")))
                            .withStyle(ChatFormatting.GOLD));
            tooltip.add(
                    Component.translatable(
                                    "jade.ascension_ic2.gas.output",
                                    GuiNumbers.format(data.getInt("output")))
                            .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

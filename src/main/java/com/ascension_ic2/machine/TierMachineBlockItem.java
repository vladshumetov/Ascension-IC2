package com.ascension_ic2.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item that shows the machine's energy tier, buffer and lane count in its tooltip. */
public class TierMachineBlockItem extends BlockItem {
    private final MachineTier tier;

    public TierMachineBlockItem(
            Block block, Properties properties, MachineTier tier, TierMachineKind kind) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(
                Component.translatable(
                                "tooltip.ascension_ic2.energy_tier",
                                this.tier.energyTier(),
                                formatEu(this.tier.maxTransfer()))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.ascension_ic2.buffer", formatEu(this.tier.capacity()))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.ascension_ic2.lanes",
                                this.tier.lanes(),
                                this.tier.itemsPerOperation())
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String formatEu(double value) {
        return String.format("%,d", (long) value);
    }
}

package com.ascension_ic2.event;

import com.ascension_ic2.nano.NanoMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class MachineEvents {
    private MachineEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        if (event.getPlayer().isCreative()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof NanoMachineBlockEntity machine)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        machine.collectDrops(drops);
        for (ItemStack stack : drops) {
            Block.popResource(level, event.getPos(), stack);
        }
    }
}

package com.titaniumindustry.event;

import com.titaniumindustry.block.entity.BioGeneratorBlockEntity;
import com.titaniumindustry.block.entity.TitaniumRefinerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class TitaniumEvents {
    private TitaniumEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        Player player = event.getEntity();
        if (player.isShiftKeyDown()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        ItemStack held = event.getItemStack();

        if (blockEntity instanceof TitaniumRefinerBlockEntity refiner) {
            if (!held.isEmpty()) {
                ItemStack remainder =
                        ItemHandlerHelper.insertItemStacked(
                                refiner.getInventory(), held.copy(), false);
                if (remainder.getCount() != held.getCount()) {
                    player.setItemInHand(event.getHand(), remainder);
                    cancel(event);
                }
            } else {
                ItemStack extracted =
                        refiner.getInventory().extractItem(TitaniumRefinerBlockEntity.OUTPUT, 64, false);
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    cancel(event);
                }
            }
            return;
        }

        if (blockEntity instanceof BioGeneratorBlockEntity generator) {
            if (!held.isEmpty() && generator.getInventory().isItemValid(0, held)) {
                ItemStack remainder =
                        ItemHandlerHelper.insertItemStacked(
                                generator.getInventory(), held.copy(), false);
                if (remainder.getCount() != held.getCount()) {
                    player.setItemInHand(event.getHand(), remainder);
                    cancel(event);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        if (event.getPlayer().isCreative()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        List<ItemStack> drops = new ArrayList<>();
        if (blockEntity instanceof TitaniumRefinerBlockEntity refiner) {
            refiner.collectDrops(drops);
        } else if (blockEntity instanceof BioGeneratorBlockEntity generator) {
            generator.collectDrops(drops);
        }

        for (ItemStack stack : drops) {
            Block.popResource(level, event.getPos(), stack);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}

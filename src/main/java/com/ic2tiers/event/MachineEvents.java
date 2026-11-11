package com.ic2tiers.event;

import com.ic2tiers.nano.NanoMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class MachineEvents {
    private static final TagKey<Item> IC2_WRENCHES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("ic2", "wrenches"));

    private MachineEvents() {}

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
        if (!(blockEntity instanceof NanoMachineBlockEntity machine)) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (!held.isEmpty()) {
            if (held.is(IC2_WRENCHES)) {
                return;
            }

            ItemStack remainder = machine.insertInputs(held.copy());
            if (remainder.getCount() != held.getCount()) {
                player.setItemInHand(event.getHand(), remainder);
                cancel(event);
            }
            return;
        }

        boolean extracted = false;
        for (int slot = NanoMachineBlockEntity.OUTPUT_START;
                slot < NanoMachineBlockEntity.SLOT_COUNT;
                slot++) {
            ItemStack output = machine.getInventory().extractItem(slot, 64, false);
            if (output.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(output)) {
                player.drop(output, false);
            }
            extracted = true;
        }

        if (extracted) {
            cancel(event);
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
        if (!(blockEntity instanceof NanoMachineBlockEntity machine)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        machine.collectDrops(drops);
        for (ItemStack stack : drops) {
            Block.popResource(level, event.getPos(), stack);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}

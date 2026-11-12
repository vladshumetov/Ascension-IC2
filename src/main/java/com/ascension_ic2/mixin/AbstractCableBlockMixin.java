package com.ascension_ic2.mixin;

import com.ascension_ic2.energy.EnergyCableBlock;
import com.ascension_ic2.energy.EnergyCableBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ic2.core.block.wiring.AbstractCableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * IC2 1.2.6 ships {@code ICableConnectable}/{@code CableConnectableRegistry}, but the core never
 * calls the registry in {@code isEuConnectable}. This temporary mixin only flips the result for
 * our own cable, until the official hook is wired in IC2. Uses {@code @ModifyReturnValue} so it
 * composes with other mixins instead of cancelling.
 */
@Mixin(AbstractCableBlock.class)
public abstract class AbstractCableBlockMixin {
    private static boolean ascension$logged;

    @ModifyReturnValue(
            method =
                    "isEuConnectable(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("RETURN"))
    private static boolean ascension$connectAddonCables(
            boolean original, Level level, BlockPos pos, Direction side) {
        if (original || level == null || pos == null) {
            return original;
        }
        boolean connect =
                level.getBlockEntity(pos) instanceof EnergyCableBlockEntity
                        || level.getBlockState(pos).getBlock() instanceof EnergyCableBlock;
        if (connect && !ascension$logged) {
            ascension$logged = true;
            com.ascension_ic2.AscensionIC2.LOGGER.debug(
                    "Cable mixin active: IC2 connects to addon cable at {}", pos);
        }
        return connect;
    }
}

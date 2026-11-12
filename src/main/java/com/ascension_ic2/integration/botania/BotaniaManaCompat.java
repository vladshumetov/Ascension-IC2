package com.ascension_ic2.integration.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;

/** Optional Botania integration: pushes mana into adjacent mana pools/receivers. */
public final class BotaniaManaCompat {
    private BotaniaManaCompat() {}

    public static int pushToNeighbours(Level level, BlockPos pos, int amount) {
        int remaining = amount;
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                break;
            }
            ManaReceiver receiver =
                    ManaReceiver.LOOKUP.find(
                            level, pos.relative(direction), direction.getOpposite());
            if (receiver == null || receiver.isFull()) {
                continue;
            }
            int space = remaining;
            if (receiver instanceof ManaPool pool) {
                space = Math.max(0, pool.getMaxMana() - pool.getCurrentMana());
            }
            int send = Math.min(remaining, space);
            if (send > 0) {
                receiver.receiveMana(send);
                remaining -= send;
            }
        }
        return remaining;
    }
}

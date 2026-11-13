package com.ascension_ic2.integration.botania;

import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.capability.registration.BlockRegistrationWithContext;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;

/** Registers our greenhouse as a Botania ManaReceiver so AE2 import buses can pull mana. */
public final class BotaniaCapabilities {
    private BotaniaCapabilities() {}

    public static void register(
            RegisterCapabilitiesEvent event, BlockEntityType<?> greenhouseType) {
        BotaniaNeoForgeCapabilities.getProviderRegistration(event)
                .register(
                        ManaReceiver.LOOKUP,
                        List.of(
                                BlockRegistrationWithContext.forBlockEntities(
                                        (BlockEntity blockEntity, net.minecraft.core.Direction side) ->
                                                blockEntity instanceof ManaReceiver receiver
                                                        ? receiver
                                                        : null,
                                        greenhouseType)));
    }
}

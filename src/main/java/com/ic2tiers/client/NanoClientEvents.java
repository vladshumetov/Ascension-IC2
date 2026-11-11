package com.ic2tiers.client;

import com.ic2tiers.IC2Tiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2Tiers.MOD_ID, value = Dist.CLIENT)
public final class NanoClientEvents {
    private NanoClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(IC2Tiers.NANO_MENU.get(), NanoMachineScreen::new);
    }
}

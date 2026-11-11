package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AscensionIC2.MOD_ID, value = Dist.CLIENT)
public final class MachineClientEvents {
    private MachineClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AscensionIC2.NANO_MENU.get(), TierMachineScreen::new);
        event.register(AscensionIC2.QUANTUM_MENU.get(), TierMachineScreen::new);
    }
}

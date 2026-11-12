package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.ascension_ic2.fluid.NaturalGas;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = AscensionIC2.MOD_ID, value = Dist.CLIENT)
public final class MachineClientEvents {
    private MachineClientEvents() {}

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private final ResourceLocation still = ResourceLocation.fromNamespaceAndPath(
                            AscensionIC2.MOD_ID, "block/natural_gas_still");
                    private final ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(
                            AscensionIC2.MOD_ID, "block/natural_gas_flowing");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return this.still;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return this.flowing;
                    }

                    @Override
                    public int getTintColor() {
                        return 0xFFFFFFFF;
                    }
                },
                NaturalGas.TYPE.get());
    }    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AscensionIC2.NANO_MENU.get(), TierMachineScreen::new);
        event.register(AscensionIC2.QUANTUM_MENU.get(), TierMachineScreen::new);
        event.register(AscensionIC2.SOLAR_MENU.get(), SolarPanelScreen::new);
        event.register(AscensionIC2.GAS_GENERATOR_MENU.get(), GasGeneratorScreen::new);
        event.register(AscensionIC2.BLOOD_GENERATOR_MENU.get(), BloodGeneratorScreen::new);
        event.register(AscensionIC2.BOTANIA_GREENHOUSE_MENU.get(), BotaniaGreenhouseScreen::new);
    }
}

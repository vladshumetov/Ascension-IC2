package com.ascension_ic2.client;

import com.ascension_ic2.machine.TierMachineBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Client-side registry of looping machine sounds, one per working block entity. */
public final class MachineSoundHandler {
    private static final Map<TierMachineBlockEntity, MachineSoundInstance> SOUNDS =
            new WeakHashMap<>();

    private MachineSoundHandler() {}

    public static void start(TierMachineBlockEntity machine, String path) {
        stop(machine);
        SoundEvent event =
                BuiltInRegistries.SOUND_EVENT.get(
                        ResourceLocation.fromNamespaceAndPath("ic2", path));
        if (event == null) {
            return;
        }
        MachineSoundInstance instance = new MachineSoundInstance(event, machine);
        SOUNDS.put(machine, instance);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    public static void stop(TierMachineBlockEntity machine) {
        MachineSoundInstance instance = SOUNDS.remove(machine);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }
}

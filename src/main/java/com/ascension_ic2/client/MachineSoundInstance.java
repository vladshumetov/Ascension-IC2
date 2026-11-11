package com.ascension_ic2.client;

import com.ascension_ic2.machine.TierMachineBlockEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Looping machine sound bound to a block entity; stops as soon as the machine is gone. */
public class MachineSoundInstance extends AbstractTickableSoundInstance {
    private final TierMachineBlockEntity machine;

    public MachineSoundInstance(SoundEvent event, TierMachineBlockEntity machine) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.machine = machine;
        this.looping = true;
        this.volume = 0.5F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.x = machine.getBlockPos().getX() + 0.5;
        this.y = machine.getBlockPos().getY() + 0.5;
        this.z = machine.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (this.machine.isRemoved()
                || this.machine.getLevel() == null
                || this.machine.getLevel().getBlockEntity(this.machine.getBlockPos())
                        != this.machine) {
            stop();
        }
    }
}

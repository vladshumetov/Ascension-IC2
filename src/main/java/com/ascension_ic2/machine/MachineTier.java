package com.ascension_ic2.machine;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.config.TiersConfig;
import com.ascension_ic2.energy.EnergyTiers;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared layout and balance of a machine tier. The GUI texture, lane count and slot grid are
 * driven by these values so the nano and quantum tiers use the same code.
 */
public enum MachineTier {
    NANO("nano", 3, 86, 6),
    QUANTUM("quantum", 5, 60, 8);

    public static final int INPUT_Y = 28;
    public static final int OUTPUT_Y = 76;
    public static final int LANE_STEP = 26;

    private final String id;
    private final int lanes;
    private final int firstSlotX;
    private final int energyTier;

    MachineTier(String id, int lanes, int firstSlotX, int energyTier) {
        EnergyTiers.checkTier(energyTier);
        this.id = id;
        this.lanes = lanes;
        this.firstSlotX = firstSlotX;
        this.energyTier = energyTier;
    }

    public String id() {
        return this.id;
    }

    public int lanes() {
        return this.lanes;
    }

    public int firstSlotX() {
        return this.firstSlotX;
    }

    public int slotX(int lane) {
        return this.firstSlotX + lane * LANE_STEP;
    }

    public int energyTier() {
        return this.energyTier;
    }

    public double capacity() {
        return EnergyTiers.defaultCapacity(this.energyTier);
    }

    public double maxTransfer() {
        return EnergyTiers.euPerTick(this.energyTier);
    }

    public int itemsPerOperation() {
        return switch (this) {
            case NANO -> TiersConfig.NANO_ITEMS_PER_OPERATION.get();
            case QUANTUM -> TiersConfig.QUANTUM_ITEMS_PER_OPERATION.get();
        };
    }

    public ResourceLocation guiTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                AscensionIC2.MOD_ID, "textures/gui/" + this.id + "_machine.png");
    }
}

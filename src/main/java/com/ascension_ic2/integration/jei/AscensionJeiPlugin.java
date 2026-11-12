package com.ascension_ic2.integration.jei;

import com.ascension_ic2.AscensionIC2;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Optional JEI integration: hides our ASP-style panels when the real ASP mod is installed. */
@JeiPlugin
public class AscensionJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AscensionIC2.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!AscensionIC2.isAdvancedSolarPanelsLoaded()) {
            return;
        }
        List<ItemStack> hidden = new ArrayList<>();
        for (AscensionIC2.AdvancedSolarRegistration panel : AscensionIC2.ADVANCED_SOLAR_PANELS) {
            hidden.add(new ItemStack(panel.item().get()));
        }
        if (!hidden.isEmpty()) {
            AscensionIC2.LOGGER.debug("Hiding {} ASP panels from JEI", hidden.size());
            runtime.getIngredientManager()
                    .removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}

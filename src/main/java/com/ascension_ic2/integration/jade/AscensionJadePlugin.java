package com.ascension_ic2.integration.jade;

import com.ascension_ic2.AscensionIC2;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade integration; loaded only when Jade is installed. */
@WailaPlugin(AscensionIC2.MOD_ID)
public class AscensionJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        EnergyComponentProvider.registerCommon(registration);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        EnergyComponentProvider.registerClient(registration);
    }
}

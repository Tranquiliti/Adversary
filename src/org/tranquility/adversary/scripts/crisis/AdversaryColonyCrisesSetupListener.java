package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.campaign.listeners.ColonyCrisesSetupListener;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;

public class AdversaryColonyCrisesSetupListener implements ColonyCrisesSetupListener {
    // Add activity immediately if Colony Crisis intel already exists
    public AdversaryColonyCrisesSetupListener() {
        HostileActivityEventIntel intel = HostileActivityEventIntel.get();
        if (intel != null)
            intel.addActivity(new AdversaryHostileActivityFactor(intel), new AdversaryActivityCause(intel));
    }

    @Override
    public void finishedAddingCrisisFactors(HostileActivityEventIntel intel) {
        intel.addActivity(new AdversaryHostileActivityFactor(intel), new AdversaryActivityCause(intel));
    }
}
package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.campaign.listeners.ColonyCrisesSetupListener;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;

public class AdversaryColonyCrisesSetupListener implements ColonyCrisesSetupListener {
    @Override
    public void finishedAddingCrisisFactors(HostileActivityEventIntel hostileActivityEventIntel) {
        hostileActivityEventIntel.addActivity(new AdversaryHostileActivityFactor(hostileActivityEventIntel), new AdversaryActivityCause(hostileActivityEventIntel));
    }
}
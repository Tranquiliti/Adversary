package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import static org.tranquility.adversary.AdversaryStrings.HA_PUNITIVE_EXPEDITION_BASE_NAME;
import static org.tranquility.adversary.AdversaryStrings.HA_PUNITIVE_EXPEDITION_NOUN;
import static org.tranquility.adversary.scripts.crisis.AdversaryHostileActivityFactor.wasAdversaryEverSatBombardedByPlayer;

public class AdversaryPunitiveExpedition extends GenericRaidFGI {
    private static final String ADVERSARY_FLEET = "$AdversaryPE_fleet";
    private static final String KEY = "$AdversaryPE_ref";
    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.3f);

    public AdversaryPunitiveExpedition(GenericRaidParams params) {
        super(params);
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
    }

    @Override
    protected void notifyEnding() {
        super.notifyEnding();
        Global.getSector().getMemoryWithoutUpdate().unset(KEY);
    }

    @Override
    protected void notifyEnded() {
        super.notifyEnded();
    }

    @Override
    public String getNoun() {
        return HA_PUNITIVE_EXPEDITION_NOUN;
    }

    @Override
    public String getForcesNoun() {
        return super.getForcesNoun();
    }

    @Override
    public String getBaseName() {
        return HA_PUNITIVE_EXPEDITION_BASE_NAME;
    }

    @Override
    protected void preConfigureFleet(int size, FleetCreatorMission m) {
        m.setFleetTypeMedium(FleetTypes.TASK_FORCE); // default would be "Patrol", don't want that
    }

    @Override
    protected void configureFleet(int size, FleetCreatorMission m) {
        m.triggerSetFleetFlag(ADVERSARY_FLEET);
        if (size < 8) m.triggerSetFleetDoctrineOther(1, -1); // more frigates/ships in small fleets
        if (wasAdversaryEverSatBombardedByPlayer()) { // Die
            m.triggerSetFleetQuality(HubMissionWithTriggers.FleetQuality.SMOD_3);
            m.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.ALL_SHIPS, HubMissionWithTriggers.OfficerQuality.HIGHER);
        }
        m.triggerFleetAddCommanderSkill(Skills.CYBERNETIC_AUGMENTATION, 1);
    }

    @Override
    public void abort() {
        if (!isAborted()) for (CampaignFleetAPI curr : getFleets())
            curr.getMemoryWithoutUpdate().unset(ADVERSARY_FLEET);
        super.abort();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        interval.advance(Misc.getDays(amount));
        if (interval.intervalElapsed() && isCurrent(PAYLOAD_ACTION)) for (CampaignFleetAPI curr : getFleets())
            Misc.setFlagWithReason(curr.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_MAKE_HOSTILE, "AdversaryPunEx", true, 1f);
    }

    public static AdversaryPunitiveExpedition get() {
        return (AdversaryPunitiveExpedition) Global.getSector().getMemoryWithoutUpdate().get(KEY);
    }
}
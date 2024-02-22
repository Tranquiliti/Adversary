package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class AdversaryPunitiveExpedition extends GenericRaidFGI {

    public static final String ADVERSARY_FLEET = "$AdversaryPE_fleet";

    public static String KEY = "$AdversaryPE_ref";

    public static AdversaryPunitiveExpedition get() {
        return (AdversaryPunitiveExpedition) Global.getSector().getMemoryWithoutUpdate().get(KEY);
    }

    protected IntervalUtil interval = new IntervalUtil(0.1f, 0.3f);


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
        return super.getNoun();
    }

    @Override
    public String getForcesNoun() {
        return super.getForcesNoun();
    }

    @Override
    public String getBaseName() {
        return super.getBaseName();
    }

    @Override
    protected void preConfigureFleet(int size, FleetCreatorMission m) {
        m.setFleetTypeMedium(FleetTypes.TASK_FORCE); // default would be "Patrol", don't want that
    }

    @Override
    protected void configureFleet(int size, FleetCreatorMission m) {
        m.triggerSetFleetFlag(ADVERSARY_FLEET);
        if (size >= 8) {
            m.triggerSetFleetDoctrineOther(5, 0); // more capitals in large fleets
        }
    }

    @Override
    public void abort() {
        if (!isAborted()) {
            for (CampaignFleetAPI curr : getFleets()) {
                curr.getMemoryWithoutUpdate().unset(ADVERSARY_FLEET);
            }
        }
        super.abort();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        float days = Misc.getDays(amount);
        interval.advance(days);

        if (interval.intervalElapsed()) {
            if (isCurrent(PAYLOAD_ACTION)) {
                String reason = "AdversaryPunEx";
                for (CampaignFleetAPI curr : getFleets()) {
                    Misc.setFlagWithReason(curr.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_MAKE_HOSTILE, reason, true, 1f);
                }
            }
        }
    }
}
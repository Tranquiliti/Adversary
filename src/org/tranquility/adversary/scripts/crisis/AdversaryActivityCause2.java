package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

import static org.tranquility.adversary.AdversaryUtil.getAdvString;

public class AdversaryActivityCause2 extends BaseHostileActivityCause2 {
    public static float MAX_MAG = 1f;

    public AdversaryActivityCause2(HostileActivityEventIntel intel) {
        super(intel);
    }

    @Override
    public TooltipCreator getTooltip() {
        return new BaseFactorTooltip() {
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                float opad = 10f;
                tooltip.addPara(getAdvString("HA_activityCause2Tooltip1"), 0f, Misc.getHighlightColor(), "to the absolute maximum");

                tooltip.addPara(getAdvString("HA_activityCause2Tooltip2"), opad, Misc.getHighlightColor(), "Defeating the Adversary's incoming attack");
            }
        };
    }

    @Override
    public boolean shouldShow() {
        return getProgress() != 0;
    }

    @Override
    public String getProgressStr() {
        return super.getProgressStr();
    }

    @Override
    public Color getProgressColor(BaseEventIntel intel) {
        return super.getProgressColor(intel);
    }

    @Override
    public int getProgress() {
        if (AdversaryHostileActivityFactor.isPlayerDefeatedAdversaryAttack()) return 0;

        if (AdversaryHostileActivityFactor.wasAdversaryEverSatBombardedByPlayer())
            return HostileActivityEventIntel.MAX_PROGRESS;
        else return 0;
    }

    @Override
    public String getDesc() {
        return getAdvString("HA_activityCause2Desc");
    }

    @Override
    public float getMagnitudeContribution(StarSystemAPI system) {
        if (getProgress() <= 0) return 0f;

        return (0.2f + 0.8f * intel.getMarketPresenceFactor(system)) * MAX_MAG;
    }
}
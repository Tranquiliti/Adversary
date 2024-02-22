package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

import static org.tranquility.adversary.AdversaryUtil.FACTION_ADVERSARY;

public class AdversaryActivityCause extends BaseHostileActivityCause2 {
    public static int LARGE_COLONY = 6;
    public static int MEDIUM_COLONY = 5;
    public static int COUNT_IF_MEDIUM = 4;

    public static float MAX_MAG = 0.5f;

    public AdversaryActivityCause(HostileActivityEventIntel intel) {
        super(intel);
    }

    @Override
    public TooltipCreator getTooltip() {
        return new BaseFactorTooltip() {
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addPara("Event progress value is based on the number and size of colonies under your control. Requires one size %s colony, or at least %s colonies with one being at least size %s.", 0f, Misc.getHighlightColor(), "" + LARGE_COLONY, "" + COUNT_IF_MEDIUM, "" + MEDIUM_COLONY);
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
        if (AdversaryHostileActivityFactor.wasAdversaryEverSatBombardedByPlayer() || AdversaryHostileActivityFactor.isPlayerDefeatedAdversaryAttack() || Global.getSector().getFaction(FACTION_ADVERSARY).getRelToPlayer().isAtWorst(RepLevel.INHOSPITABLE) || !isThreatToAdversary())
            return 0;

        int score = 0;
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            int size = market.getSize();
            switch (size) {
                case 5:
                    score += 2;
                    break;
                case 6:
                    score += 4;
                    break;
                default:
                    score += 1;
                    break;
            }
        }
        return score;
    }

    @Override
    public String getDesc() {
        return "Colony presence and size";
    }

    @Override
    public float getMagnitudeContribution(StarSystemAPI system) {
        if (getProgress() <= 0) return 0f;

        return (0.2f + 0.8f * intel.getMarketPresenceFactor(system)) * MAX_MAG;
    }

    private boolean isThreatToAdversary() {
        int large = 0;
        int count = 0;
        int medium = 0;
        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            int size = market.getSize();
            if (size >= LARGE_COLONY) large++;
            if (size >= MEDIUM_COLONY) medium++;
            count++;
        }
        return large > 0 || (medium > 0 && count >= COUNT_IF_MEDIUM);
    }
}
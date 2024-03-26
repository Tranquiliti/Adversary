package org.tranquility.adversary;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import org.tranquility.adversary.scripts.crisis.AdversaryActivityCause;
import org.tranquility.adversary.scripts.crisis.AdversaryHostileActivityFactor;

import java.util.Comparator;
import java.util.TreeSet;

import static org.tranquility.adversary.AdversaryStrings.FACTION_ADVERSARY;

public final class AdversaryUtil {
    public static final boolean LUNALIB_ENABLED = Global.getSettings().getModManager().isModEnabled("lunalib");

    public static void addAdversaryColonyCrisis() {
        HostileActivityEventIntel intel = HostileActivityEventIntel.get();
        if (intel != null && intel.getActivityOfClass(AdversaryHostileActivityFactor.class) == null)
            intel.addActivity(new AdversaryHostileActivityFactor(intel), new AdversaryActivityCause(intel));
    }

    public static TreeSet<MarketAPI> getAdversaryMilitaryMarkets() {
        TreeSet<MarketAPI> adversaryMarkets = new TreeSet<>(new Comparator<MarketAPI>() {
            public int compare(MarketAPI m1, MarketAPI m2) {
                int comp = Integer.compare(getScore(m1), getScore(m2));
                if (comp != 0) return comp;
                return Integer.compare(m1.getSize(), m2.getSize());
            }

            private int getScore(MarketAPI market) {
                int score = 0;
                if (market.hasIndustry(Industries.HIGHCOMMAND)) {
                    score += 2;
                    Industry highCommand = market.getIndustry(Industries.HIGHCOMMAND);
                    if (highCommand.isImproved()) score++;
                    if (highCommand.getAICoreId() != null && highCommand.getAICoreId().equals(Commodities.ALPHA_CORE))
                        score++;
                    if (highCommand.getSpecialItem() != null) score++;
                } else if (market.hasIndustry(Industries.MILITARYBASE)) score++;
                return score;
            }
        });

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
            if (market.getFactionId().equals(FACTION_ADVERSARY)) adversaryMarkets.add(market);

        return adversaryMarkets;
    }
}

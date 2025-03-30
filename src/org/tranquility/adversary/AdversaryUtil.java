package org.tranquility.adversary;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

import static org.tranquility.adversary.AdversaryStrings.FACTION_ADVERSARY;

public final class AdversaryUtil {
    public static final boolean LUNALIB_ENABLED = Global.getSettings().getModManager().isModEnabled("lunalib");
    public static final String MEMKEY_SPAWNED_OPTIMAL = "$adversary_spawnedOptimal";

    /**
     * Returns a set of all Adversary markets, sorted by High Command/Military Base presence
     *
     * @return A List containing all Adversary markets, sorted by military power in descending order
     */
    public static List<MarketAPI> getAdversaryMarkets() {
        List<MarketAPI> adversaryMarkets = Misc.getFactionMarkets(FACTION_ADVERSARY);

        adversaryMarkets.sort((m1, m2) -> {
            int comp = Integer.compare(getScore(m2), getScore(m1));
            if (comp != 0) return comp;
            return Integer.compare(m2.getSize(), m1.getSize());
        });

        return adversaryMarkets;
    }

    private static int getScore(MarketAPI market) {
        int score = 0;
        if (market.hasIndustry(Industries.HIGHCOMMAND)) {
            score += 2;
            Industry highCommand = market.getIndustry(Industries.HIGHCOMMAND);
            if (highCommand.isImproved()) score++;
            if (highCommand.getAICoreId() != null && highCommand.getAICoreId().equals(Commodities.ALPHA_CORE)) score++;
            if (highCommand.getSpecialItem() != null) score++;
        } else if (market.hasIndustry(Industries.MILITARYBASE)) score++;
        return score;
    }
}
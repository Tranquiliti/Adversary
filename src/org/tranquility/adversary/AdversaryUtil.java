package org.tranquility.adversary;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

import static org.tranquility.adversary.AdversaryStrings.FACTION_ADVERSARY;

public final class AdversaryUtil {
    public static final boolean LUNALIB_ENABLED = Global.getSettings().getModManager().isModEnabled("lunalib");
    public static final String MEMKEY_SPAWNED_OPTIMAL = "$adversary_spawnedOptimal";

    /**
     * Returns a set of all Adversary markets, sorted by overall fleet strength (ship quality * fleet size multiplier)
     *
     * @return A List containing all Adversary markets, sorted by military power in descending order
     */
    public static List<MarketAPI> getAdversaryMarkets() {
        List<MarketAPI> adversaryMarkets = Misc.getFactionMarkets(FACTION_ADVERSARY);

        adversaryMarkets.sort((m1, m2) -> {
            int comp = Float.compare(getScore(m2), getScore(m1));
            if (comp != 0) return comp;
            return Integer.compare(m2.getSize(), m1.getSize());
        });

        return adversaryMarkets;
    }

    private static float getScore(MarketAPI market) {
        return Misc.getShipQuality(market, market.getFactionId()) * market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
    }
}
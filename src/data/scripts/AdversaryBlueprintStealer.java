package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdversaryBlueprintStealer implements EconomyTickListener {
    protected String factionId;
    protected String[] targetIds;
    protected short elapsedMonths, delayInMonths;

    public AdversaryBlueprintStealer(String faction, short elapsed, JSONObject stealerSettings) throws JSONException {
        factionId = faction;
        JSONArray targetFactions = stealerSettings.getJSONArray("targetFactions");
        targetIds = new String[targetFactions.length()];
        for (int i = targetFactions.length() - 1; i >= 0; i--) targetIds[i] = targetFactions.getString(i);
        elapsedMonths = elapsed;
        delayInMonths = (short) Math.max(stealerSettings.getInt("blueprintStealingDelay"), 1);

        Global.getLogger(AdversaryBlueprintStealer.class).info("Faction blueprint stealer active for: " + factionId);
    }

    // Unused
    @Override
    public void reportEconomyTick(int iterIndex) {
    }

    // Thieving faction will periodically steal targeted factions' current blueprints
    @Override
    public void reportEconomyMonthEnd() {
        elapsedMonths++;
        if (elapsedMonths >= delayInMonths) {
            FactionAPI thiefFaction = Global.getSector().getFaction(factionId);
            for (String id : targetIds) stealBlueprints(thiefFaction, id);
        }
    }

    // Look in com.fs.starfarer.api.impl.campaign.DelayedBlueprintLearnScript's doAction() for vanilla implementation
    protected void stealBlueprints(FactionAPI thiefFaction, String targetId) {
        FactionAPI targetFaction = Global.getSector().getFaction(targetId);

        for (String id : targetFaction.getKnownShips())
            if (!thiefFaction.knowsShip(id)) {
                thiefFaction.addKnownShip(id, true);
                thiefFaction.addUseWhenImportingShip(id);
            }

        for (String id : targetFaction.getKnownWeapons())
            if (!thiefFaction.knowsWeapon(id)) thiefFaction.addKnownWeapon(id, true);

        for (String id : targetFaction.getKnownFighters())
            if (!thiefFaction.knowsFighter(id)) thiefFaction.addKnownFighter(id, true);

        Global.getLogger(AdversaryBlueprintStealer.class).info(factionId + " has stolen blueprints from " + targetId + "!");
    }
}

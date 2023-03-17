package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.json.JSONArray;
import org.json.JSONException;

public class AdversaryBlueprintStealer implements EconomyTickListener {
    protected String factionId;
    protected String[] targetIds;
    protected short elapsedMonths, delayInMonths; // TODO: change to byte after Starsector update

    public AdversaryBlueprintStealer(String faction, short elapsed, short delay, JSONArray targetFactions) throws JSONException {
        factionId = faction;
        targetIds = new String[targetFactions.length()];
        for (int i = targetFactions.length() - 1; i >= 0; i--) targetIds[i] = targetFactions.getString(i);
        elapsedMonths = elapsed;
        delayInMonths = delay > 0 ? delay : (short) 1;

        Global.getLogger(AdversaryBlueprintStealer.class).info("Faction blueprint stealer active for: " + factionId);
    }

    public void changeDelay(short newDelay) {
        delayInMonths = newDelay;
        Global.getLogger(AdversaryBlueprintStealer.class).info("Set " + factionId + " blueprint stealer delay to " + delayInMonths);
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

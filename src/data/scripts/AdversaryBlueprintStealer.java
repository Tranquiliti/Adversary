package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.json.JSONArray;
import org.json.JSONException;

public class AdversaryBlueprintStealer implements EconomyTickListener {
    protected String factionId;
    protected String[] targetIds;
    protected short elapsedMonths, delayInMonths; // TODO: switch to byte after next Starsector update (really!)

    public AdversaryBlueprintStealer(String faction, byte elapsed, byte delay, JSONArray targetFactions) throws JSONException {
        factionId = faction;
        elapsedMonths = elapsed;
        delayInMonths = delay > 0 ? delay : (byte) 1;
        targetIds = new String[targetFactions.length()];
        for (int i = 0; i < targetFactions.length(); i++) targetIds[i] = targetFactions.getString(i);
        Global.getLogger(AdversaryBlueprintStealer.class).info("Faction blueprint stealer active for: " + factionId);
    }

    public void setDelay(byte newDelay) {
        delayInMonths = newDelay;
        Global.getLogger(AdversaryBlueprintStealer.class).info("Set " + factionId + " blueprint stealer delay to " + delayInMonths);
    }

    // Unused
    @Override
    public void reportEconomyTick(int iterIndex) {
    }

    // Stealer faction will periodically steal targeted factions' current blueprints
    @Override
    public void reportEconomyMonthEnd() {
        elapsedMonths++;
        if (elapsedMonths >= delayInMonths) {
            elapsedMonths = 0;
            FactionAPI stealerFaction = Global.getSector().getFaction(factionId);
            for (String id : targetIds) stealBlueprints(stealerFaction, Global.getSector().getFaction(id));
        }
    }

    // Look in com.fs.starfarer.api.impl.campaign.DelayedBlueprintLearnScript's doAction() for vanilla implementation
    protected void stealBlueprints(FactionAPI stealer, FactionAPI target) {
        for (String id : target.getKnownShips())
            if (!stealer.knowsShip(id)) {
                stealer.addKnownShip(id, true);
                stealer.addUseWhenImportingShip(id);
            }
        for (String id : target.getKnownWeapons()) if (!stealer.knowsWeapon(id)) stealer.addKnownWeapon(id, true);
        for (String id : target.getKnownFighters()) if (!stealer.knowsFighter(id)) stealer.addKnownFighter(id, true);
        Global.getLogger(AdversaryBlueprintStealer.class).info(factionId + " has stolen blueprints from " + target.getId() + "!");
    }
}

package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.json.JSONArray;
import org.json.JSONException;

public class AdversaryBlueprintStealer implements EconomyTickListener {
    private final String factionId;
    private final String[] targetIds;
    private byte elapsedMonths, delayInMonths;

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

    @Override
    public void reportEconomyMonthEnd() {
        elapsedMonths++;
        if (elapsedMonths >= delayInMonths) {
            elapsedMonths = 0;
            FactionAPI stealerFaction = Global.getSector().getFaction(factionId);
            for (String id : targetIds) stealBlueprints(stealerFaction, Global.getSector().getFaction(id));
        }
    }

    // See com.fs.starfarer.api.impl.campaign.DelayedBlueprintLearnScript's doAction() for vanilla implementation
    protected void stealBlueprints(FactionAPI stealer, FactionAPI target) {
        for (String shipId : target.getKnownShips())
            if (!stealer.knowsShip(shipId)) {
                stealer.addKnownShip(shipId, true);
                stealer.addUseWhenImportingShip(shipId);
            }
        for (String weaponId : target.getKnownWeapons())
            if (!stealer.knowsWeapon(weaponId)) stealer.addKnownWeapon(weaponId, true);
        for (String fighterId : target.getKnownFighters())
            if (!stealer.knowsFighter(fighterId)) stealer.addKnownFighter(fighterId, true);
        Global.getLogger(AdversaryBlueprintStealer.class).info(factionId + " has stolen blueprints from " + target.getId() + "!");
    }
}
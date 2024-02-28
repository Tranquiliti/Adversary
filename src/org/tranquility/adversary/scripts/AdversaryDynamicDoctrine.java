package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static org.tranquility.adversary.AdversaryUtil.addAdversaryColonyCrisis;

public class AdversaryDynamicDoctrine implements EconomyTickListener {
    protected String factionId;
    protected byte elapsedMonths, delayInMonths;
    protected WeightedRandomPicker priorityDoctrinePicker;
    protected Random factionSeed;

    public AdversaryDynamicDoctrine(String faction, byte elapsed, byte delay, JSONArray possibleDoctrines) throws JSONException {
        factionId = faction;
        elapsedMonths = elapsed;
        delayInMonths = delay > 0 ? delay : (byte) 1;
        priorityDoctrinePicker = new WeightedRandomPicker();
        factionSeed = new Random();

        // Iterating in reverse order so that the first doctrine in JSONArray is considered the selected doctrine
        for (int i = possibleDoctrines.length() - 1; i >= 0; i--) {
            JSONObject doctrine = possibleDoctrines.getJSONObject(i);
            int weight = doctrine.optInt(Global.getSettings().getString("adversary", "settings_weight"), 1);
            if (weight > 0) priorityDoctrinePicker.add(new PriorityDoctrine(doctrine, weight));
        }
        priorityDoctrinePicker.ready(factionId);
        refresh(); // Immediately apply the default doctrine
        Global.getLogger(AdversaryDynamicDoctrine.class).info("Faction dynamic doctrine active for: " + factionId);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        addAdversaryColonyCrisis(); // HACK: More convenient to just do this on an existing listener
    }

    @Override
    public void reportEconomyMonthEnd() {
        elapsedMonths++;
        if (elapsedMonths >= delayInMonths) {
            elapsedMonths = 0;
            setPriorityDoctrine(priorityDoctrinePicker.pick(factionSeed));
        }
    }

    public void setDelay(byte newDelay) {
        delayInMonths = newDelay;
        Global.getLogger(AdversaryDynamicDoctrine.class).info("Set " + factionId + " dynamic doctrine delay to " + delayInMonths);
    }

    // Sets this faction's priority lists to a specific priority doctrine
    protected void setPriorityDoctrine(PriorityDoctrine thisPriority) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        FactionDoctrineAPI factionDoctrine = faction.getDoctrine();
        Logger doctrineLogger = Global.getLogger(AdversaryDynamicDoctrine.class);

        factionDoctrine.setWarships(thisPriority.warships);
        factionDoctrine.setCarriers(thisPriority.carriers);
        factionDoctrine.setPhaseShips(thisPriority.phaseShips);
        doctrineLogger.info(factionId + " fleet composition set to " + factionDoctrine.getWarships() + "-" + factionDoctrine.getCarriers() + "-" + factionDoctrine.getPhaseShips());

        factionDoctrine.setAggression(thisPriority.aggression);
        doctrineLogger.info(factionId + " aggression set to " + factionDoctrine.getAggression());

        faction.getPriorityShips().clear();
        if (thisPriority.priorityShips != null && thisPriority.priorityShips.length != 0)
            Collections.addAll(faction.getPriorityShips(), thisPriority.priorityShips);
        infoPrioritySet(doctrineLogger, faction.getPriorityShips(), "ships");

        faction.getPriorityWeapons().clear();
        if (thisPriority.priorityWeapons != null && thisPriority.priorityWeapons.length != 0)
            Collections.addAll(faction.getPriorityWeapons(), thisPriority.priorityWeapons);
        infoPrioritySet(doctrineLogger, faction.getPriorityWeapons(), "weapons");

        faction.getPriorityFighters().clear();
        if (thisPriority.priorityFighters != null && thisPriority.priorityFighters.length != 0)
            Collections.addAll(faction.getPriorityFighters(), thisPriority.priorityFighters);
        infoPrioritySet(doctrineLogger, faction.getPriorityFighters(), "fighters");

        faction.clearShipRoleCache(); // Required after any direct manipulation of faction ship lists
    }

    protected void infoPrioritySet(Logger thisLogger, Set<String> set, String text) {
        if (set.isEmpty()) thisLogger.info(factionId + " has no priority " + text);
        else {
            StringBuilder contents = new StringBuilder();
            for (String s : set) contents.append(s).append(',');
            thisLogger.info(factionId + " priority " + text + ": [" + contents.deleteCharAt(contents.length() - 1) + "]");
        }
    }

    // Refreshes the currently-set doctrine
    public void refresh() {
        setPriorityDoctrine(priorityDoctrinePicker.items.get(priorityDoctrinePicker.items.size() - 1));
    }

    // A lighter, nested WeightedRandomPicker designed specifically for this class
    // This Picker will not select the same element twice in a row
    protected static class WeightedRandomPicker {
        // The last element in items is considered the selected doctrine
        private final ArrayList<PriorityDoctrine> items = new ArrayList<>();
        private int total = 0;

        public void add(PriorityDoctrine item) {
            if (item.weight <= 0) return;
            items.add(item);
            total += item.weight;
        }

        // Readies the Picker for use; shouldn't add any more elements after calling this function
        public void ready(String factionId) {
            if (items.isEmpty())
                add(new PriorityDoctrine(Global.getSettings().getFactionSpec(factionId).getFactionDoctrine()));
            items.trimToSize();
            if (items.size() > 1) total -= items.get(items.size() - 1).weight;
        }

        public PriorityDoctrine pick(Random seed) {
            int random = seed.nextInt(total + 1), weightSoFar = 0, index = 0;
            while (index < items.size()) {
                weightSoFar += items.get(index).weight;
                if (random <= weightSoFar) break;
                index++;
            }
            PriorityDoctrine selected = items.get(index);

            // Ensure selected doctrine does not get picked again for the next pick()
            // Not necessary if only one doctrine exists
            if (items.size() > 1) {
                PriorityDoctrine previous = items.get(items.size() - 1);
                items.set(index, previous);
                items.set(items.size() - 1, selected);
                total += previous.weight - selected.weight;
            }

            return selected;
        }
    }

    // A static nested class to make managing a faction's current doctrine simpler
    protected static class PriorityDoctrine {
        public int weight;
        public byte warships, carriers, phaseShips;
        public byte aggression;
        public String[] priorityShips, priorityWeapons, priorityFighters;

        // Default doctrine, using a faction's current fleet composition/doctrine settings
        public PriorityDoctrine(FactionDoctrineAPI defaultDoctrine) {
            weight = 1;
            warships = (byte) defaultDoctrine.getWarships();
            carriers = (byte) defaultDoctrine.getCarriers();
            phaseShips = (byte) defaultDoctrine.getPhaseShips();
            aggression = (byte) defaultDoctrine.getAggression();
        }

        // Creates a priority doctrine from a JSONObject
        // Defaults are those of the Adversary's default fleet composition/doctrine
        public PriorityDoctrine(JSONObject priorityObject, int weight) throws JSONException {
            this.weight = weight;

            String compId = Global.getSettings().getString("adversary", "settings_fleetComposition");
            if (priorityObject.isNull(compId)) {
                warships = 3;
                carriers = 2;
                phaseShips = 2;
            } else {
                JSONArray fleetComp = priorityObject.getJSONArray(compId);
                warships = (byte) fleetComp.getInt(0);
                carriers = (byte) fleetComp.getInt(1);
                phaseShips = (byte) fleetComp.getInt(2);
            }

            aggression = (byte) priorityObject.optInt(Global.getSettings().getString("adversary", "settings_aggression"), 5);

            String shipsId = Global.getSettings().getString("adversary", "settings_priorityShips");
            if (!priorityObject.isNull(shipsId)) {
                JSONArray shipList = priorityObject.getJSONArray(shipsId);
                if (shipList.length() > 0) {
                    priorityShips = new String[shipList.length()];
                    for (int i = 0; i < shipList.length(); i++) priorityShips[i] = shipList.getString(i);
                }
            }

            String weaponsId = Global.getSettings().getString("adversary", "settings_priorityWeapons");
            if (!priorityObject.isNull(weaponsId)) {
                JSONArray weaponList = priorityObject.getJSONArray(weaponsId);
                if (weaponList.length() > 0) {
                    priorityWeapons = new String[weaponList.length()];
                    for (int i = 0; i < weaponList.length(); i++) priorityWeapons[i] = weaponList.getString(i);
                }
            }

            String fightersId = Global.getSettings().getString("adversary", "settings_priorityFighters");
            if (!priorityObject.isNull(fightersId)) {
                JSONArray fighterList = priorityObject.getJSONArray(fightersId);
                if (fighterList.length() > 0) {
                    priorityFighters = new String[fighterList.length()];
                    for (int i = 0; i < fighterList.length(); i++) priorityFighters[i] = fighterList.getString(i);
                }
            }
        }
    }
}

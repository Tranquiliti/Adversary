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

import static org.tranquility.adversary.AdversaryStrings.*;
import static org.tranquility.adversary.AdversaryUtil.addAdversaryColonyCrisis;

public class AdversaryDynamicDoctrine implements EconomyTickListener {
    private final String factionId;
    private byte elapsedMonths, delayInMonths;
    private final WeightedRandomPicker priorityDoctrinePicker;
    private final Random factionSeed;

    public AdversaryDynamicDoctrine(String faction, byte elapsed, byte delay, JSONArray possibleDoctrines) throws JSONException {
        factionId = faction;
        elapsedMonths = elapsed;
        delayInMonths = delay > 0 ? delay : (byte) 1;
        priorityDoctrinePicker = new WeightedRandomPicker();
        factionSeed = new Random();

        // Iterating in reverse order so that the first doctrine in JSONArray is considered the selected doctrine
        for (int i = possibleDoctrines.length() - 1; i >= 0; i--) {
            JSONObject doctrine = possibleDoctrines.getJSONObject(i);
            int weight = doctrine.optInt(SETTINGS_WEIGHT, 1);
            if (weight > 0) priorityDoctrinePicker.add(new PriorityDoctrine(doctrine, weight));
        }
        priorityDoctrinePicker.ready(factionId);
        refresh(); // Immediately apply the default doctrine
        Global.getLogger(AdversaryDynamicDoctrine.class).info("Faction dynamic doctrine active for: " + factionId);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        addAdversaryColonyCrisis(); // HACK: More convenient to add the crisis mid-game using an existing listener
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

    // Refreshes the currently-set doctrine
    public void refresh() {
        setPriorityDoctrine(priorityDoctrinePicker.items.get(priorityDoctrinePicker.items.size() - 1));
    }

    // Sets this faction's priority lists to a specific priority doctrine
    private void setPriorityDoctrine(PriorityDoctrine newDoctrine) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        FactionDoctrineAPI factionDoctrine = faction.getDoctrine();
        Logger doctrineLogger = Global.getLogger(AdversaryDynamicDoctrine.class);

        factionDoctrine.setWarships(newDoctrine.warships);
        factionDoctrine.setCarriers(newDoctrine.carriers);
        factionDoctrine.setPhaseShips(newDoctrine.phaseShips);
        doctrineLogger.info(factionId + " fleet composition set to " + factionDoctrine.getWarships() + "-" + factionDoctrine.getCarriers() + "-" + factionDoctrine.getPhaseShips());

        factionDoctrine.getOfficerSkills().clear();
        if (newDoctrine.officerSkills != null && newDoctrine.officerSkills.length != 0)
            Collections.addAll(factionDoctrine.getOfficerSkills(), newDoctrine.officerSkills);

        factionDoctrine.setAggression(newDoctrine.aggression);
        doctrineLogger.info(factionId + " aggression set to " + factionDoctrine.getAggression());

        faction.getPriorityShips().clear();
        if (newDoctrine.priorityShips != null && newDoctrine.priorityShips.length != 0)
            Collections.addAll(faction.getPriorityShips(), newDoctrine.priorityShips);
        infoPrioritySet(doctrineLogger, faction.getPriorityShips(), "ships");

        faction.getPriorityWeapons().clear();
        if (newDoctrine.priorityWeapons != null && newDoctrine.priorityWeapons.length != 0)
            Collections.addAll(faction.getPriorityWeapons(), newDoctrine.priorityWeapons);
        infoPrioritySet(doctrineLogger, faction.getPriorityWeapons(), "weapons");

        faction.getPriorityFighters().clear();
        if (newDoctrine.priorityFighters != null && newDoctrine.priorityFighters.length != 0)
            Collections.addAll(faction.getPriorityFighters(), newDoctrine.priorityFighters);
        infoPrioritySet(doctrineLogger, faction.getPriorityFighters(), "fighters");

        faction.clearShipRoleCache(); // Required after any direct manipulation of faction ship lists
    }

    private void infoPrioritySet(Logger thisLogger, Set<String> set, String text) {
        if (set.isEmpty()) thisLogger.info(factionId + " has no priority " + text);
        else {
            StringBuilder contents = new StringBuilder();
            for (String s : set) contents.append(s).append(',');
            thisLogger.info(factionId + " priority " + text + ": [" + contents.deleteCharAt(contents.length() - 1) + "]");
        }
    }

    // A lighter, nested WeightedRandomPicker designed specifically for this class
    // This Picker will not select the same element twice in a row
    private static class WeightedRandomPicker {
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
    private static class PriorityDoctrine {
        public int weight;
        public byte warships, carriers, phaseShips;
        public byte aggression;
        public String[] officerSkills, priorityShips, priorityWeapons, priorityFighters;

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

            String compId = SETTINGS_FLEET_COMPOSITION;
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

            if (!priorityObject.isNull(SETTINGS_OFFICER_SKILLS)) {
                JSONArray skillList = priorityObject.getJSONArray(SETTINGS_OFFICER_SKILLS);
                if (skillList.length() > 0) {
                    officerSkills = new String[skillList.length()];
                    for (int i = 0; i < skillList.length(); i++) officerSkills[i] = skillList.getString(i);
                }
            }

            aggression = (byte) priorityObject.optInt(SETTINGS_AGGRESSION, 5);

            if (!priorityObject.isNull(SETTINGS_PRIORITY_SHIPS)) {
                JSONArray shipList = priorityObject.getJSONArray(SETTINGS_PRIORITY_SHIPS);
                if (shipList.length() > 0) {
                    priorityShips = new String[shipList.length()];
                    for (int i = 0; i < shipList.length(); i++) priorityShips[i] = shipList.getString(i);
                }
            }

            if (!priorityObject.isNull(SETTINGS_PRIORITY_WEAPONS)) {
                JSONArray weaponList = priorityObject.getJSONArray(SETTINGS_PRIORITY_WEAPONS);
                if (weaponList.length() > 0) {
                    priorityWeapons = new String[weaponList.length()];
                    for (int i = 0; i < weaponList.length(); i++) priorityWeapons[i] = weaponList.getString(i);
                }
            }

            if (!priorityObject.isNull(SETTINGS_PRIORITY_FIGHTERS)) {
                JSONArray fighterList = priorityObject.getJSONArray(SETTINGS_PRIORITY_FIGHTERS);
                if (fighterList.length() > 0) {
                    priorityFighters = new String[fighterList.length()];
                    for (int i = 0; i < fighterList.length(); i++) priorityFighters[i] = fighterList.getString(i);
                }
            }
        }
    }
}

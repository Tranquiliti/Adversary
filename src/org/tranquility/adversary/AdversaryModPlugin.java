package org.tranquility.adversary;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import org.json.JSONException;
import org.tranquility.adversary.scripts.AdversaryBlueprintStealer;
import org.tranquility.adversary.scripts.AdversaryDynamicDoctrine;
import org.tranquility.adversary.scripts.AdversaryPersonalFleet;
import org.tranquility.adversary.scripts.crisis.AdversaryActivityCause;
import org.tranquility.adversary.scripts.crisis.AdversaryActivityCause2;
import org.tranquility.adversary.scripts.crisis.AdversaryHostileActivityFactor;

import java.util.List;
import java.util.TreeSet;

import static org.tranquility.adversary.AdversaryUtil.*;

@SuppressWarnings("unused")
public class AdversaryModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        if (LUNALIB_ENABLED) AdversaryLunaUtil.addSettingsListener();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        boolean enableSilliness;
        String sillyBountyId = Global.getSettings().getString("adversary", "settings_enableAdversarySillyBounties");
        if (LUNALIB_ENABLED)
            enableSilliness = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, sillyBountyId));
        else enableSilliness = Global.getSettings().getBoolean(sillyBountyId);

        if (enableSilliness) Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");

        if (!newGame) addAdversaryListeners(false);

        HostileActivityEventIntel intel = HostileActivityEventIntel.get();
        if (intel != null && intel.getActivityOfClass(AdversaryHostileActivityFactor.class) == null) {
            AdversaryHostileActivityFactor adversaryFactor = new AdversaryHostileActivityFactor(intel);
            intel.addActivity(adversaryFactor, new AdversaryActivityCause(intel));
            intel.addActivity(adversaryFactor, new AdversaryActivityCause2(intel));
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        FactionAPI adversary = Global.getSector().getFaction(FACTION_ADVERSARY);
        // Recent history has made them cold and hateful against almost everyone
        for (FactionAPI faction : Global.getSector().getAllFactions())
            adversary.setRelationship(faction.getId(), -100f);
        adversary.setRelationship(FACTION_ADVERSARY, 100f);
        adversary.setRelationship(Factions.NEUTRAL, 0f);

        addAdversaryListeners(true);
    }

    @Override
    public void onNewGameAfterTimePass() {
        boolean doPersonalFleet;
        String personalFleetId = Global.getSettings().getString("adversary", "settings_enableAdversaryPersonalFleet");
        if (LUNALIB_ENABLED)
            doPersonalFleet = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, personalFleetId));
        else doPersonalFleet = Global.getSettings().getBoolean(personalFleetId);

        if (doPersonalFleet) addAdversaryPersonalFleet();
    }

    // Remove or add listeners to a game depending on currently-set settings
    private void addAdversaryListeners(boolean newGame) {
        boolean dynaDoctrine, stealBlueprints;
        String doctrineId = Global.getSettings().getString("adversary", "settings_enableAdversaryDynamicDoctrine");
        String blueprintId = Global.getSettings().getString("adversary", "settings_enableAdversaryBlueprintStealing");
        if (LUNALIB_ENABLED) { // LunaLib settings overrides settings.json
            dynaDoctrine = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, doctrineId));
            stealBlueprints = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, blueprintId));
        } else { // Just load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean(doctrineId);
            stealBlueprints = Global.getSettings().getBoolean(blueprintId);
        }

        if (newGame) { // Called presumably after onNewGameAfterEconomyLoad()
            if (dynaDoctrine) addAdversaryDynamicDoctrine(true);
            if (stealBlueprints) addAdversaryBlueprintStealer(true);
        } else {
            ListenerManagerAPI listMan = Global.getSector().getListenerManager();
            if (dynaDoctrine) {
                List<AdversaryDynamicDoctrine> doctrineListeners = listMan.getListeners(AdversaryDynamicDoctrine.class);
                if (doctrineListeners.isEmpty()) addAdversaryDynamicDoctrine(false);
                else // Refresh needed since restarting Starsector also resets faction doctrines
                    doctrineListeners.get(0).refresh();
            } else listMan.removeListenerOfClass(AdversaryDynamicDoctrine.class); // Disable dynamic doctrine

            if (stealBlueprints && listMan.getListeners(AdversaryBlueprintStealer.class).isEmpty())
                addAdversaryBlueprintStealer(false);
            else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
        }
    }

    private void addAdversaryDynamicDoctrine(boolean newGame) {
        SettingsAPI settings = Global.getSettings();

        Integer doctrineDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryDynamicDoctrineDelay");
        if (LUNALIB_ENABLED) doctrineDelay = AdversaryLunaUtil.getInt(MOD_ID_ADVERSARY, delayId);
        if (doctrineDelay == null) doctrineDelay = settings.getInt(delayId);

        // Starting the time pass immediately calls reportEconomyMonthEnd(), hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryDynamicDoctrine(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), doctrineDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryPossibleDoctrines"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdversaryBlueprintStealer(boolean newGame) {
        SettingsAPI settings = Global.getSettings();

        Integer stealDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryBlueprintStealingDelay");
        if (LUNALIB_ENABLED) stealDelay = AdversaryLunaUtil.getInt(MOD_ID_ADVERSARY, delayId);
        if (stealDelay == null) stealDelay = settings.getInt(delayId);

        // Starting the time pass immediately calls reportEconomyMonthEnd(), hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), stealDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryStealsFromFactions"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdversaryPersonalFleet() {
        TreeSet<MarketAPI> adversaryMarkets = AdversaryUtil.getAdversaryMilitaryMarkets();

        if (!adversaryMarkets.isEmpty())
            new AdversaryPersonalFleet(Global.getSettings().getString("adversary", "person_id_adversary_personal_commander"), Global.getSettings().getString("adversary", "name_adversary_personal_fleet"), FACTION_ADVERSARY, adversaryMarkets.last().getId());
    }
}
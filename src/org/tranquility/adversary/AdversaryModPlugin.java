package org.tranquility.adversary;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.json.JSONException;
import org.tranquility.adversary.lunalib.AdversaryLunaUtil;
import org.tranquility.adversary.scripts.AdversaryBlueprintStealer;
import org.tranquility.adversary.scripts.AdversaryDynamicDoctrine;
import org.tranquility.adversary.scripts.AdversaryPersonalFleet;

import java.util.List;
import java.util.TreeSet;

import static org.tranquility.adversary.AdversaryStrings.*;
import static org.tranquility.adversary.AdversaryUtil.LUNALIB_ENABLED;
import static org.tranquility.adversary.AdversaryUtil.addAdversaryColonyCrisis;

@SuppressWarnings("unused")
public class AdversaryModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        if (LUNALIB_ENABLED) AdversaryLunaUtil.addSettingsListener();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        toggleSillyBounties();

        // Does not immediately apply if Colony Crisis intel gets (re)added mid-game; it only gets added in after a save & load
        // If the CC intel is removed mid-game (e.g. by losing all colonies), the Adversary crisis gets removed too, leading to the above problem
        // (HACK: also added this on AdversaryDynamicDoctrine's reportEconomyTick() so crisis can get applied mid-game)
        addAdversaryColonyCrisis();

        if (!newGame) addAdversaryListeners(false);
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        setAdversaryRelationship();
        addAdversaryListeners(true);
    }

    @Override
    public void onNewGameAfterTimePass() {
        boolean doPersonalFleet;
        if (LUNALIB_ENABLED)
            doPersonalFleet = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_PERSONAL_FLEET));
        else doPersonalFleet = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_PERSONAL_FLEET);

        if (doPersonalFleet) addAdversaryPersonalFleet();
    }

    private void toggleSillyBounties() {
        boolean enableSilliness;
        if (LUNALIB_ENABLED)
            enableSilliness = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES));
        else enableSilliness = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES);

        if (enableSilliness) Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");
    }

    // Recent history has made them cold and hateful against almost everyone
    private void setAdversaryRelationship() {
        FactionAPI adversary = Global.getSector().getFaction(FACTION_ADVERSARY);
        for (FactionAPI faction : Global.getSector().getAllFactions())
            adversary.setRelationship(faction.getId(), -1f);
        adversary.setRelationship(FACTION_ADVERSARY, 1f);
        adversary.setRelationship(Factions.NEUTRAL, 0f);
    }

    // Remove or add listeners to a game depending on currently-set settings
    private void addAdversaryListeners(boolean newGame) {
        boolean dynaDoctrine, stealBlueprints;
        if (LUNALIB_ENABLED) {
            dynaDoctrine = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE));
            stealBlueprints = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING));
        } else { // Load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE);
            stealBlueprints = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING);
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
        Integer doctrineDelay = null;
        if (LUNALIB_ENABLED)
            doctrineDelay = AdversaryLunaUtil.getInt(MOD_ID_ADVERSARY, SETTINGS_ADVERSARY_DYNAMIC_DOCTRINE_DELAY);
        if (doctrineDelay == null)
            doctrineDelay = Global.getSettings().getInt(SETTINGS_ADVERSARY_DYNAMIC_DOCTRINE_DELAY);

        // Starting the time pass immediately calls reportEconomyMonthEnd(), hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryDynamicDoctrine(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), doctrineDelay.byteValue(), Global.getSettings().getJSONArray(SETTINGS_ADVERSARY_POSSIBLE_DOCTRINES)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdversaryBlueprintStealer(boolean newGame) {
        Integer stealDelay = null;
        if (LUNALIB_ENABLED)
            stealDelay = AdversaryLunaUtil.getInt(MOD_ID_ADVERSARY, SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY);
        if (stealDelay == null) stealDelay = Global.getSettings().getInt(SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY);

        // Starting the time pass immediately calls reportEconomyMonthEnd(), hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), stealDelay.byteValue(), Global.getSettings().getJSONArray(SETTINGS_ADVERSARY_STEALS_FROM_FACTIONS)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdversaryPersonalFleet() {
        TreeSet<MarketAPI> adversaryMarkets = AdversaryUtil.getAdversaryMarkets();
        if (!adversaryMarkets.isEmpty()) new AdversaryPersonalFleet(adversaryMarkets.last().getId());
    }
}
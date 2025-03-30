package org.tranquility.adversary;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONException;
import org.tranquility.adversary.scripts.*;
import org.tranquility.adversary.scripts.crisis.AdversaryColonyCrisesSetupListener;

import java.util.List;

import static org.tranquility.adversary.AdversaryStrings.*;
import static org.tranquility.adversary.AdversaryUtil.LUNALIB_ENABLED;
import static org.tranquility.adversary.AdversaryUtil.MEMKEY_SPAWNED_OPTIMAL;

@SuppressWarnings("unused")
public class AdversaryModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        if (LUNALIB_ENABLED) LunaSettings.addSettingsListener(new AdversaryLunaSettingsListener());
    }

    @Override
    public void onGameLoad(boolean newGame) {
        toggleSillyBounties();

        if (!newGame) {
            addAdversaryListeners(false);
            if (canSpawnOptimal()) {
                new AdversaryOptimal(true);
                addAdversaryAIAdmins();
                setAdversaryRelationship();
                if (canDoSDFAdversary()) addAdversarySDF();
            }
        }
    }

    @Override
    public void onNewGameAfterProcGen() {
        // Done in this step so people can be added via createInitialPeople() in CoreLifecyclePluginImpl.java
        if (canSpawnOptimal()) new AdversaryOptimal(false);
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_SPAWNED_OPTIMAL))
            addAdversaryAIAdmins(); // Needs to be done here to prevent createInitialPeople() from overriding AI admin
        setAdversaryRelationship();
        addAdversaryListeners(true);
    }

    @Override
    public void onNewGameAfterTimePass() {
        if (canDoSDFAdversary()) addAdversarySDF();
    }

    private boolean canSpawnOptimal() {
        boolean spawnOptimal;
        if (LUNALIB_ENABLED)
            spawnOptimal = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_OPTIMAL));
        else spawnOptimal = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_OPTIMAL);
        return spawnOptimal && !Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_SPAWNED_OPTIMAL);
    }

    private void addAdversaryAIAdmins() {
        final AICoreAdminPluginImpl impl = new AICoreAdminPluginImpl();
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(Global.getSector().getStarSystem(NAME_STAR_1)))
            market.setAdmin(impl.createPerson(Commodities.ALPHA_CORE, FACTION_ADVERSARY, 0));
    }

    private boolean canDoSDFAdversary() {
        if (LUNALIB_ENABLED)
            return Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_SDF_ADVERSARY));
        return Global.getSettings().getBoolean(SETTINGS_ENABLE_SDF_ADVERSARY);
    }

    private void toggleSillyBounties() {
        boolean enableSilliness;
        if (LUNALIB_ENABLED)
            enableSilliness = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES));
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
            dynaDoctrine = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE));
            stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING));
        } else { // Load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE);
            stealBlueprints = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING);
        }

        ListenerManagerAPI listenerManager = Global.getSector().getListenerManager();
        if (newGame) { // Called presumably after onNewGameAfterEconomyLoad()
            if (dynaDoctrine) addAdversaryDynamicDoctrine(true);
            if (stealBlueprints) addAdversaryBlueprintStealer(true);
        } else {
            if (dynaDoctrine) {
                List<AdversaryDynamicDoctrine> doctrineListeners = listenerManager.getListeners(AdversaryDynamicDoctrine.class);
                if (doctrineListeners.isEmpty()) addAdversaryDynamicDoctrine(false);
                else // Refresh needed since restarting Starsector also resets faction doctrines
                    doctrineListeners.get(0).refresh();
            } else listenerManager.removeListenerOfClass(AdversaryDynamicDoctrine.class); // Disable dynamic doctrine

            if (stealBlueprints && listenerManager.getListeners(AdversaryBlueprintStealer.class).isEmpty())
                addAdversaryBlueprintStealer(false);
            else listenerManager.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
        }

        // Add Colony Crisis listener
        if (listenerManager.getListeners(AdversaryColonyCrisesSetupListener.class).isEmpty())
            listenerManager.addListener(new AdversaryColonyCrisesSetupListener());
    }

    private void addAdversaryDynamicDoctrine(boolean newGame) {
        Integer doctrineDelay = null;
        if (LUNALIB_ENABLED)
            doctrineDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, SETTINGS_ADVERSARY_DYNAMIC_DOCTRINE_DELAY);
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
            stealDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY);
        if (stealDelay == null) stealDelay = Global.getSettings().getInt(SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY);

        // Starting the time pass immediately calls reportEconomyMonthEnd(), hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), stealDelay.byteValue(), Global.getSettings().getJSONArray(SETTINGS_ADVERSARY_STEALS_FROM_FACTIONS)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdversarySDF() {
        List<MarketAPI> adversaryMarkets = AdversaryUtil.getAdversaryMarkets();
        if (!adversaryMarkets.isEmpty()) new SDFAdversary(adversaryMarkets.get(0).getId());
    }
}
package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import data.scripts.world.systems.AdversaryCustomStarSystem;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class AdversaryModPlugin extends BaseModPlugin {
    private transient HashMap<MarketAPI, String> marketsToOverrideAdmin;

    // Adding LunaSettingsListener when game starts
    @Override
    public void onApplicationLoad() {
        LunaSettings.INSTANCE.addListener(new AdversaryLunaSettingsListener());
    }

    // Re-applying or removing listeners on an existing game.
    @Override
    public void onGameLoad(boolean newGame) {
        if (newGame || Global.getSector().getFaction("adversary") == null) return;
        addAdversaryListeners(false);
    }

    // Generates mod systems after proc-gen so that planet markets can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        boolean doCustomStarSystems;
        if (Global.getSettings().getModManager().isModEnabled("lunalib"))
            doCustomStarSystems = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "enableCustomStarSystems"));
        else doCustomStarSystems = Global.getSettings().getBoolean("enableCustomStarSystems");

        if (doCustomStarSystems) try {
            JSONArray systemList = Global.getSettings().getMergedJSONForMod("data/config/customStarSystems.json", "adversary").getJSONArray("customStarSystems");
            AdversaryUtil util = new AdversaryUtil();
            for (int i = 0; i < systemList.length(); i++) {
                JSONObject systemOptions = systemList.getJSONObject(i);
                if (systemOptions.optBoolean("isEnabled", true))
                    for (int numOfSystems = systemOptions.optInt("numberOfSystems", 1); numOfSystems > 0; numOfSystems--)
                        new AdversaryCustomStarSystem().generate(util, systemOptions);
            }
            marketsToOverrideAdmin = util.marketsToOverrideAdmin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        // Gives selected markets the admins they're supposed to have (can't do it before economy load)
        if (marketsToOverrideAdmin != null) {
            AICoreAdminPluginImpl aiPlugin = new AICoreAdminPluginImpl();
            for (MarketAPI market : marketsToOverrideAdmin.keySet())
                switch (marketsToOverrideAdmin.get(market)) {
                    case "player":
                        market.setAdmin(null);
                        break;
                    case "alpha_core":
                        market.setAdmin(aiPlugin.createPerson("alpha_core", market.getFaction().getId(), 0));
                }
            // No need for the HashMap afterwards, so clear it and set it to null to minimize memory use, just in case
            marketsToOverrideAdmin.clear();
            marketsToOverrideAdmin = null;
        }

        FactionAPI adversary = Global.getSector().getFaction("adversary");
        if (adversary != null) { // Null check so determined people can properly remove the faction from the mod without errors
            // Recent history has made them cold and hateful against almost everyone
            for (FactionAPI faction : Global.getSector().getAllFactions())
                adversary.setRelationship(faction.getId(), -100f);
            adversary.setRelationship("adversary", 100f);
            adversary.setRelationship("neutral", 0f);

            addAdversaryListeners(true);
        }
    }

    // Remove or add listeners to a game depending on currently-set settings
    private void addAdversaryListeners(boolean newGame) {
        boolean dynaDoctrine, stealBlueprints;
        boolean lunaLibEnabled = Global.getSettings().getModManager().isModEnabled("lunalib");
        if (lunaLibEnabled) { // LunaLib settings overrides settings.json
            dynaDoctrine = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "enableAdversaryDynamicDoctrine"));
            stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "enableAdversaryBlueprintStealing"));
        } else { // Just load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean("enableAdversaryDynamicDoctrine");
            stealBlueprints = Global.getSettings().getBoolean("enableAdversaryBlueprintStealing");
        }

        if (newGame) { // Assumes it gets called during onNewGameAfterEconomyLoad()
            if (dynaDoctrine) addAdversaryDynamicDoctrine(true, lunaLibEnabled);
            if (stealBlueprints) addAdversaryBlueprintStealer(true, lunaLibEnabled);
        } else { // Loading existing game
            ListenerManagerAPI listMan = Global.getSector().getListenerManager();
            if (dynaDoctrine) {
                List<AdversaryDoctrineChanger> doctrineListeners = listMan.getListeners(AdversaryDoctrineChanger.class);
                if (doctrineListeners.isEmpty()) addAdversaryDynamicDoctrine(false, lunaLibEnabled);
                else // Refresh needed since Adversary's current doctrine resets upon loading a new Starsector application.
                    doctrineListeners.get(0).refresh();
            } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // Disable dynamic doctrine

            if (stealBlueprints && listMan.getListeners(AdversaryBlueprintStealer.class).isEmpty())
                addAdversaryBlueprintStealer(false, lunaLibEnabled);
            else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
        }
    }

    // Adds a AdversaryDynamicDoctrine with settings
    private void addAdversaryDynamicDoctrine(boolean newGame, boolean lunaLibEnabled) {
        Integer doctrineDelay = null;
        if (lunaLibEnabled) doctrineDelay = LunaSettings.getInt("adversary", "adversaryDynamicDoctrineDelay");
        if (doctrineDelay == null) doctrineDelay = Global.getSettings().getInt("adversaryDynamicDoctrineDelay");

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryDoctrineChanger("adversary", (byte) (newGame ? -1 : 0), doctrineDelay.byteValue(), Global.getSettings().getJSONArray("adversaryPossibleDoctrines")));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Adds a AdversaryBlueprintStealer with settings
    private void addAdversaryBlueprintStealer(boolean newGame, boolean lunaLibEnabled) {
        Integer stealDelay = null;
        if (lunaLibEnabled) stealDelay = LunaSettings.getInt("adversary", "adversaryBlueprintStealingDelay");
        if (stealDelay == null) stealDelay = Global.getSettings().getInt("adversaryDynamicDoctrineDelay");

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer("adversary", (byte) (newGame ? -1 : 0), stealDelay.byteValue(), Global.getSettings().getJSONArray("adversaryStealsFromFactions")));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
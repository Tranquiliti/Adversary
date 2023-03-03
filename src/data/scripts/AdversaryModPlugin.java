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

    // Remove or add listeners to a game depending on currently-set settings
    @Override
    public void onGameLoad(boolean newGame) {
        if (newGame) return;

        boolean changeDoctrines, stealBlueprints;
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) { // LunaLib settings overrides settings.json
            changeDoctrines = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "adversary_enableAdversaryDoctrineChange"));
            stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "adversary_enableAdversaryBlueprintStealing"));
        } else { // Just load from settings.json
            changeDoctrines = Global.getSettings().getBoolean("enableAdversaryDoctrineChange");
            stealBlueprints = Global.getSettings().getBoolean("enableAdversaryBlueprintStealing");
        }

        ListenerManagerAPI listMan = Global.getSector().getListenerManager();
        if (changeDoctrines) {
            List<AdversaryDoctrineChanger> doctrineListeners = listMan.getListeners(AdversaryDoctrineChanger.class);
            if (doctrineListeners.isEmpty()) try {
                listMan.addListener(new AdversaryDoctrineChanger("adversary", (short) 0, Global.getSettings().getJSONObject("adversaryDoctrineChangeSettings")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else doctrineListeners.get(0).refresh(); // Doctrine changer already in-game, so refresh the doctrine
            // Need to refresh since the Adversary's current doctrine get reset upon loading a new Starsector application.
        } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // If disabled, remove doctrine changer

        if (stealBlueprints) {
            if (listMan.getListeners(AdversaryBlueprintStealer.class).isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer("adversary", (short) 0, Global.getSettings().getJSONObject("adversaryBlueprintStealingSettings")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // If disabled, remove blueprint stealer
    }

    // Generates mod systems after proc-gen so that planet markets can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        boolean doCustomStarSystems;
        if (Global.getSettings().getModManager().isModEnabled("lunalib"))
            doCustomStarSystems = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "adversary_enableCustomStarSystems"));
        else doCustomStarSystems = Global.getSettings().getBoolean("enableCustomStarSystems");

        if (doCustomStarSystems) try {
            AdversaryUtil util = new AdversaryUtil();
            JSONArray systemList = Global.getSettings().getJSONArray("customStarSystems");
            for (int i = 0; i < systemList.length(); i++) {
                JSONObject systemOptions = systemList.getJSONObject(i);
                if (systemOptions.isNull("isEnabled") || systemOptions.getBoolean("isEnabled"))
                    for (int numOfSystems = systemOptions.isNull("numberOfSystems") ? 1 : systemOptions.getInt("numberOfSystems"); numOfSystems > 0; numOfSystems--)
                        new AdversaryCustomStarSystem().generate(util, systemOptions);
            }
            marketsToOverrideAdmin = util.marketsToOverrideAdmin;
        } catch (JSONException e) {
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

            boolean changeDoctrines, stealBlueprints;
            if (Global.getSettings().getModManager().isModEnabled("lunalib")) { // LunaLib settings overrides settings.json
                changeDoctrines = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "adversary_enableAdversaryDoctrineChange"));
                stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "adversary_enableAdversaryBlueprintStealing"));
            } else { // Just load from settings.json
                changeDoctrines = Global.getSettings().getBoolean("enableAdversaryDoctrineChange");
                stealBlueprints = Global.getSettings().getBoolean("enableAdversaryBlueprintStealing");
            }

            // Allows the Adversary to change fleet doctrine in-game if enabled
            // Doing this here, so it can work during the initial 2-month time pass
            if (changeDoctrines) try {
                // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
                Global.getSector().getListenerManager().addListener(new AdversaryDoctrineChanger("adversary", (short) -1, Global.getSettings().getJSONObject("adversaryDoctrineChangeSettings")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            if (stealBlueprints) try {
                Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer("adversary", (short) -1, Global.getSettings().getJSONObject("adversaryBlueprintStealingSettings")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
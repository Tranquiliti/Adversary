package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
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
    private transient final String FACTION_ADVERSARY = Global.getSettings().getString("adversary", "faction_id_adversary");
    private transient final String MOD_ID_ADVERSARY = Global.getSettings().getString("adversary", "mod_id_adversary"); // For LunaLib
    private transient final boolean LUNALIB_ENABLED = Global.getSettings().getModManager().isModEnabled("lunalib");

    // Adding LunaSettingsListener when game starts
    @Override
    public void onApplicationLoad() {
        if (LUNALIB_ENABLED) LunaSettings.addSettingsListener(new AdversaryLunaSettingsListener());
    }

    // Re-applying or removing listeners on an existing game.
    @Override
    public void onGameLoad(boolean newGame) {
        if (newGame || Global.getSector().getFaction(FACTION_ADVERSARY) == null) return;
        addAdversaryListeners(false);

        boolean enableSilliness;
        String sillyBountyId = Global.getSettings().getString("adversary", "settings_enableAdversarySillyBounties");
        if (LUNALIB_ENABLED)
            enableSilliness = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, sillyBountyId));
        else enableSilliness = Global.getSettings().getBoolean(sillyBountyId);

        if (enableSilliness) Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");
    }

    // Generates mod systems after proc-gen so that planet markets can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        boolean doCustomStarSystems;
        String systemsId = Global.getSettings().getString("adversary", "settings_enableCustomStarSystems");
        if (LUNALIB_ENABLED)
            doCustomStarSystems = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, systemsId));
        else doCustomStarSystems = Global.getSettings().getBoolean(systemsId);

        if (doCustomStarSystems) try {
            JSONArray systemList = Global.getSettings().getMergedJSONForMod(Global.getSettings().getString("adversary", "path_merged_json_customStarSystems"), "adversary").getJSONArray(Global.getSettings().getString("adversary", "settings_customStarSystems"));
            AdversaryUtil util = new AdversaryUtil();
            for (int i = 0; i < systemList.length(); i++) {
                JSONObject systemOptions = systemList.getJSONObject(i);
                if (systemOptions.optBoolean(util.OPT_IS_ENABLED, true))
                    for (int numOfSystems = systemOptions.optInt(util.OPT_NUMBER_OF_SYSTEMS, 1); numOfSystems > 0; numOfSystems--)
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
                    case Factions.PLAYER:
                        market.setAdmin(null);
                        break;
                    case Commodities.ALPHA_CORE:
                        market.setAdmin(aiPlugin.createPerson(Commodities.ALPHA_CORE, market.getFaction().getId(), 0));
                }
            // No need for the HashMap afterwards, so clear it and set it to null to minimize memory use, just in case
            marketsToOverrideAdmin.clear();
            marketsToOverrideAdmin = null;
        }

        FactionAPI adversary = Global.getSector().getFaction(FACTION_ADVERSARY);
        if (adversary != null) { // Null check so determined people can properly remove the faction from the mod without errors
            // Recent history has made them cold and hateful against almost everyone
            for (FactionAPI faction : Global.getSector().getAllFactions())
                adversary.setRelationship(faction.getId(), -100f);
            adversary.setRelationship(FACTION_ADVERSARY, 100f);
            adversary.setRelationship(Factions.NEUTRAL, 0f);

            addAdversaryListeners(true);
        }
    }

    // Remove or add listeners to a game depending on currently-set settings
    private void addAdversaryListeners(boolean newGame) {
        boolean dynaDoctrine, stealBlueprints;
        String doctrineId = Global.getSettings().getString("adversary", "settings_enableAdversaryDynamicDoctrine");
        String blueprintId = Global.getSettings().getString("adversary", "settings_enableAdversaryBlueprintStealing");
        if (LUNALIB_ENABLED) { // LunaLib settings overrides settings.json
            dynaDoctrine = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, doctrineId));
            stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, blueprintId));
        } else { // Just load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean(doctrineId);
            stealBlueprints = Global.getSettings().getBoolean(blueprintId);
        }

        if (newGame) { // Assumes it gets called during onNewGameAfterEconomyLoad()
            if (dynaDoctrine) addAdversaryDynamicDoctrine(true, LUNALIB_ENABLED);
            if (stealBlueprints) addAdversaryBlueprintStealer(true, LUNALIB_ENABLED);
        } else { // Loading existing game
            ListenerManagerAPI listMan = Global.getSector().getListenerManager();
            if (dynaDoctrine) {
                List<AdversaryDoctrineChanger> doctrineListeners = listMan.getListeners(AdversaryDoctrineChanger.class);
                if (doctrineListeners.isEmpty()) addAdversaryDynamicDoctrine(false, LUNALIB_ENABLED);
                else // Refresh needed since Adversary's current doctrine resets upon loading a new Starsector application.
                    doctrineListeners.get(0).refresh();
            } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // Disable dynamic doctrine

            if (stealBlueprints && listMan.getListeners(AdversaryBlueprintStealer.class).isEmpty())
                addAdversaryBlueprintStealer(false, LUNALIB_ENABLED);
            else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
        }
    }

    // Adds a AdversaryDynamicDoctrine with settings
    private void addAdversaryDynamicDoctrine(boolean newGame, boolean lunaLibEnabled) {
        SettingsAPI settings = Global.getSettings();

        Integer doctrineDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryDynamicDoctrineDelay");
        if (lunaLibEnabled) doctrineDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, delayId);
        if (doctrineDelay == null) doctrineDelay = settings.getInt(delayId);

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryDoctrineChanger(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), doctrineDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryPossibleDoctrines"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Adds a AdversaryBlueprintStealer with settings
    private void addAdversaryBlueprintStealer(boolean newGame, boolean lunaLibEnabled) {
        SettingsAPI settings = Global.getSettings();

        Integer stealDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryBlueprintStealingDelay");
        if (lunaLibEnabled) stealDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, delayId);
        if (stealDelay == null) stealDelay = settings.getInt(delayId);

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), stealDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryStealsFromFactions"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import data.scripts.world.systems.AdversaryCustomStarSystem;
import data.scripts.world.systems.AdversaryOptimal;
import exerelin.campaign.SectorManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class AdversaryModPlugin extends BaseModPlugin {
    private transient HashMap<MarketAPI, String> marketsToOverrideAdmin;

    // Generates mod systems after proc-gen so that planet markets can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        SectorAPI sector = Global.getSector();

        try {
            SettingsAPI settings = Global.getSettings();
            AdversaryUtil util = new AdversaryUtil();

            // Generates Optimal star system if enabled and not on Random Core Worlds mode
            if (settings.getBoolean("enableOptimalStarSystem") && (!settings.getModManager().isModEnabled("nexerelin") || SectorManager.getManager().isCorvusMode()))
                new AdversaryOptimal().generate(util, sector, settings.getJSONObject("optimalStarSystem"));

            // Generates any custom star systems if enabled
            if (settings.getBoolean("enableCustomStarSystems")) {
                JSONArray systemList = settings.getJSONArray("customStarSystems");
                for (int i = 0; i < systemList.length(); i++) {
                    JSONObject systemOptions = systemList.getJSONObject(i);
                    if ((boolean) util.getJSONValue(systemOptions, 'B', "isEnabled", true))
                        for (int numOfSystems = (int) util.getJSONValue(systemOptions, 'I', "numberOfSystems", 1); numOfSystems > 0; numOfSystems--)
                            new AdversaryCustomStarSystem().generate(util, sector, systemOptions);
                }
            }

            marketsToOverrideAdmin = util.marketsToOverrideAdmin;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // Recent history has made them cold and hateful against almost everyone
        FactionAPI adversary = sector.getFaction("adversary");
        if (adversary != null) { // Null check so determined people can properly remove the faction from the mod without errors
            for (FactionAPI faction : sector.getAllFactions()) adversary.setRelationship(faction.getId(), -100f);

            adversary.setRelationship("adversary", 100f);
            adversary.setRelationship("neutral", 0f);
        }
    }

    // Gives selected markets the admins they're supposed to have (can't do it before economy load)
    @Override
    public void onNewGameAfterEconomyLoad() {
        AICoreAdminPluginImpl aiPlugin = new AICoreAdminPluginImpl();
        for (MarketAPI market : marketsToOverrideAdmin.keySet()) {
            if (marketsToOverrideAdmin.get(market).equals("alpha_core"))
                market.setAdmin(aiPlugin.createPerson("alpha_core", market.getFaction().getId(), 0));
            else market.setAdmin(null); // For player markets
        }

        // No need for the HashMap afterwards, so clear it and set it to null to minimize memory use, just in case
        marketsToOverrideAdmin.clear();
        marketsToOverrideAdmin = null;
    }

    // Allow the Adversary to change fleet doctrine in-game if enabled
    @Override
    public void onNewGameAfterTimePass() {
        SectorAPI sector = Global.getSector();
        if (sector.getFaction("adversary") != null && Global.getSettings().getBoolean("enableAdversaryDoctrineChange"))
            sector.addScript(new AdversaryFactionDoctrineChanger(sector.getFaction("adversary").getDoctrine()));
    }
}
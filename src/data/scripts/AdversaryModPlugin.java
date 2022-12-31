package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.world.systems.AdversaryCustomStarSystem;
import data.scripts.world.systems.AdversaryOptimal;
import exerelin.campaign.SectorManager;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Random;

public class AdversaryModPlugin extends BaseModPlugin {
    private transient HashMap<MarketAPI, String> marketsToOverrideAdmin;

    // Generates mod systems after proc-gen so that planet markets can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        SectorAPI sector = Global.getSector();

        try {
            SettingsAPI settings = Global.getSettings();
            Random randomSeed = StarSystemGenerator.random;
            boolean haveNexerelin = settings.getModManager().isModEnabled("nexerelin");
            AdversaryUtil advUtil = new AdversaryUtil();

            // Generates Optimal star system if enabled and not on Random Core Worlds mode
            if (settings.getBoolean("enableOptimalStarSystem") && (!haveNexerelin || SectorManager.getManager().isCorvusMode())) {
                new AdversaryOptimal().generate(advUtil, sector, settings.getJSONObject("optimalStarSystem"), randomSeed);
            }

            // Generates any custom star systems if enabled
            if (settings.getBoolean("enableCustomStarSystems")) {
                // Generates custom star systems with a random star type, ignoring constellation age
                final String[] STAR_TYPES = {"star_orange_giant", "star_red_giant", "star_red_supergiant", "star_blue_giant", "star_blue_supergiant"};
                JSONArray systemList = settings.getJSONArray("customStarSystems");
                for (int i = 0; i < systemList.length(); i++) {
                    new AdversaryCustomStarSystem().generate(advUtil, sector, systemList.getJSONObject(i), STAR_TYPES[randomSeed.nextInt(STAR_TYPES.length)], randomSeed);
                }
            }

            marketsToOverrideAdmin = advUtil.marketsToOverrideAdmin;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // Recent history has made them cold and hateful against almost everyone
        FactionAPI adversary = sector.getFaction("adversary");
        for (FactionAPI faction : sector.getAllFactions()) {
            adversary.setRelationship(faction.getId(), -100f);
        }
        adversary.setRelationship("adversary", 100f);
        adversary.setRelationship("neutral", 0f);
    }

    // Gives selected markets the admins they're supposed to have (can't do it before economy load)
    @Override
    public void onNewGameAfterEconomyLoad() {
        AICoreAdminPluginImpl aiPlugin = new AICoreAdminPluginImpl();
        for (MarketAPI market : marketsToOverrideAdmin.keySet()) {
            String adminOverride = marketsToOverrideAdmin.get(market);
            if (adminOverride == null) {
                market.setAdmin(null);
            } else if (adminOverride.equals("alpha_core")) {
                market.setAdmin(aiPlugin.createPerson("alpha_core", market.getFaction().getId(), 0));
            }
        }

        // No need for the HashMap afterwards, so clear it and set it to null to minimize memory use, just in case
        marketsToOverrideAdmin.clear();
        marketsToOverrideAdmin = null;
    }
}
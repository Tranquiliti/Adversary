package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.world.systems.AdversaryOptimal;
import data.scripts.world.systems.AdversaryRandomStarSystem;
import exerelin.campaign.SectorManager;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Random;

public class AdversaryModPlugin extends BaseModPlugin {
    // Generates mod systems after proc-gen so the systems can properly generate
    @Override
    public void onNewGameAfterProcGen() {
        SettingsAPI settings = Global.getSettings();
        boolean haveNexerelin = settings.getModManager().isModEnabled("nexerelin");
        SectorAPI sector = Global.getSector();
        Random randomSeed = StarSystemGenerator.random;

        try {
            if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
                new AdversaryOptimal().generate(sector, randomSeed);
            }

            if (settings.getBoolean("enableRandomStarSystems")) {
                // Generates random star systems with a random star type in a random constellation
                // NOTE: ignores constellation age
                final String[] STAR_TYPES = {"star_orange_giant", "star_red_giant", "star_red_supergiant", "star_blue_giant", "star_blue_supergiant"};
                JSONArray starSystemList = settings.getJSONArray("randomStarSystemsList");
                for (int i = 0; i < starSystemList.length(); i++) {
                    new AdversaryRandomStarSystem().generate(sector, starSystemList.getJSONArray(i), STAR_TYPES[randomSeed.nextInt(STAR_TYPES.length)], randomSeed);
                }
            }
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

    // Do other stuff to planets in Optimal system, if applicable
    @Override
    public void onNewGameAfterEconomyLoad() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            if (Global.getSettings().getBoolean("optimalOccupation")) {
                AICoreAdminPluginImpl aiCoreGen = new AICoreAdminPluginImpl();
                for (PlanetAPI planet : Global.getSector().getStarSystem("Optimal").getPlanets()) {
                    String factionId = planet.getFaction().getId();
                    if (factionId.equals("adversary")) {
                        // Add alpha-core admins to all Adversary planets in the Optimal star system
                        planet.getMarket().setAdmin(aiCoreGen.createPerson("alpha_core", "adversary", 0));
                    } else if (factionId.equals("player")) {
                        // TODO: fix faction not being properly set until colonizing another planet and needing to pay for storage access
                        MarketAPI planetMarket = planet.getMarket();
                        planetMarket.setPlayerOwned(true);
                        planetMarket.setAdmin(null);
                        planetMarket.addSubmarket("local_resources");
                        planetMarket.removeSubmarket("open_market");
                        planetMarket.removeSubmarket("generic_military");
                        planetMarket.removeSubmarket("black_market");
                    }
                }
            }
        }
    }
}
package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.world.systems.AdversaryCustomStarSystem;
import data.scripts.world.systems.AdversaryOptimal;
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

            if (settings.getBoolean("enableCustomStarSystems")) {
                // Generates custom star systems with a random star type, ignoring constellation age
                final String[] STAR_TYPES = {"star_orange_giant", "star_red_giant", "star_red_supergiant", "star_blue_giant", "star_blue_supergiant"};
                JSONArray systemList = settings.getJSONArray("customStarSystems");
                for (int i = 0; i < systemList.length(); i++) {
                    new AdversaryCustomStarSystem().generate(sector, systemList.getJSONObject(i), STAR_TYPES[randomSeed.nextInt(STAR_TYPES.length)], randomSeed);
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

    @Override
    public void onNewGameAfterEconomyLoad() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            // Add alpha-core admins to all Adversary planets in the Optimal star system
            AICoreAdminPluginImpl aiCoreGen = new AICoreAdminPluginImpl();
            for (PlanetAPI planet : Global.getSector().getStarSystem("Optimal").getPlanets()) {
                String factionId = planet.getFaction().getId();
                if (factionId.equals("adversary")) {
                    planet.getMarket().setAdmin(aiCoreGen.createPerson("alpha_core", "adversary", 0));
                }
            }
        }
    }
}
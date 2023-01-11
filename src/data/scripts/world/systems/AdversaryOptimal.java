package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdversaryOptimal {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemOptions) throws JSONException {
        // Create the system and set its location
        StarSystemAPI system = sector.createStarSystem("Optimal");
        float fringeRadius = (int) util.getJSONValue(systemOptions, 'I', "fringeJumpPointOrbitRadius", util.DEFAULT_FRINGE_ORBIT_RADIUS);
        util.setLocation(system, (fringeRadius / 10f) + 100f, (boolean) util.getJSONValue(systemOptions, 'B', "enableRandomLocation", util.DEFAULT_DO_RANDOM_LOCATION));

        // Generate the center stars
        util.addStarsInCenter(system, systemOptions.getJSONArray("stars"), (int) util.getJSONValue(systemOptions, 'I', "starsOrbitRadius", util.DEFAULT_STARS_ORBIT_RADIUS));
        int numOfCenterStars = system.getPlanets().size();

        // Create Fringe Jump-point
        util.addOrbitingJumpPoint(system, system.getCenter(), "Fringe Jump-point", fringeRadius);

        // Create planets from JSON list
        JSONArray planetList = (JSONArray) util.getJSONValue(systemOptions, 'A', "planets", null);
        boolean hasFactionPresence = false;
        for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanetWithOptions(system, numOfCenterStars, planetList.getJSONObject(i), i);
            if (!newPlanet.getFaction().getId().equals("neutral")) hasFactionPresence = true;
            if (newPlanet.isStar()) newPlanet.setId("system_Optimal:star_" + i);
            else newPlanet.setId("system_Optimal:planet_" + i);
        }

        // Add the system features
        JSONArray systemFeatures = (JSONArray) util.getJSONValue(systemOptions, 'A', "systemFeatures", null);
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.addOrbitingSystemFeature(system, numOfCenterStars, systemFeatures.getJSONObject(i));

        // Adds a coronal hypershunt if enabled
        if ((boolean) util.getJSONValue(systemOptions, 'B', "addCoronalHypershunt", util.DEFAULT_ADD_HYPERSHUNT))
            util.addHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if ((boolean) util.getJSONValue(systemOptions, 'B', "addDomainCryosleeper", util.DEFAULT_ADD_CRYOSLEEPER))
            util.addCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE); // Technically not part of the Core Worlds
        system.addTag(Tags.THEME_INTERESTING);

        // Adds a hidden supply cache containing either the Hypershunt Tap or the Dealmaker Holosuite
        if (hasFactionPresence) {
            SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
            drop.chances = 1;
            drop.addCustom("item_coronal_portal", 1f);
            drop.addCustom("item_dealmaker_holosuite", 1f);
            util.addOrbitingSalvageEntity(system, "supply_cache_small", system.getCenter(), fringeRadius + 7777f).addDropRandom(drop);
        } else system.setProcgen(true);

        util.setDefaultLightColorBasedOnStars(system, numOfCenterStars);
        util.generateHyperspace(system);
    }
}
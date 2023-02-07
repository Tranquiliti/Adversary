package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AdversaryOptimal {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemOptions) throws JSONException {
        // Create the system and set its location
        StarSystemAPI system = sector.createStarSystem("Optimal");
        float fringeRadius = (int) util.getJSONValue(systemOptions, 'I', "fringeJumpPointOrbitRadius", util.DEFAULT_FRINGE_ORBIT_RADIUS);
        util.setLocation(system, (fringeRadius / 10f) + 100f, (boolean) util.getJSONValue(systemOptions, 'B', "enableRandomLocation", util.DEFAULT_DO_RANDOM_LOCATION));

        // Generate the center stars
        util.addStarsInCenter(system, systemOptions.getJSONArray("stars"), (int) util.getJSONValue(systemOptions, 'I', "starsOrbitRadius", util.DEFAULT_STARS_ORBIT_RADIUS));
        ArrayList<PlanetAPI> starsInSystem = new ArrayList<>(system.getPlanets());
        int numOfCenterStars = starsInSystem.size();

        // Create Fringe Jump-point
        util.addJumpPoint(system, "Fringe Jump-point").setCircularOrbit(system.getCenter(), StarSystemGenerator.random.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f));

        // Create planets from JSON list
        JSONArray planetList = (JSONArray) util.getJSONValue(systemOptions, 'A', "planets", null);
        boolean hasFactionPresence = false;
        for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanetWithOptions(system, numOfCenterStars, planetList.getJSONObject(i), i);
            if (!newPlanet.getFaction().getId().equals("neutral")) hasFactionPresence = true;
            if (newPlanet.isStar()) {
                starsInSystem.add(newPlanet);
                newPlanet.setId("system_Optimal:star_" + i);
            } else newPlanet.setId("system_Optimal:planet_" + i);
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

        if (hasFactionPresence) {
            // Adds a hidden supply cache containing either the Hypershunt Tap or the Dealmaker Holosuite
            SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
            drop.chances = 1;
            drop.addCustom("item_coronal_portal", 1f);
            drop.addCustom("item_dealmaker_holosuite", 1f);
            util.addOrbitingSalvageEntity(system, "supply_cache_small", "Suspicious Cache", system.getCenter(), fringeRadius + 7777f).addDropRandom(drop);
        } else {
            // Add relevant system tags for an uninhabited system
            system.removeTag(Tags.THEME_CORE);
            system.addTag(Tags.THEME_MISC);
            system.addTag(Tags.THEME_INTERESTING_MINOR);
            system.setProcgen(true);
        }

        util.setDefaultLightColorBasedOnStars(system, starsInSystem);
        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);

        // Set the appropriate background, if applicable
        String background = (String) util.getJSONValue(systemOptions, 'S', "systemBackground", null);
        if (background != null) system.setBackgroundTextureFilename("graphics/backgrounds/" + background);

        // Set the appropriate system music, if applicable
        String music = (String) util.getJSONValue(systemOptions, 'S', "systemMusic", null);
        if (music != null)
            system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, music);
    }
}
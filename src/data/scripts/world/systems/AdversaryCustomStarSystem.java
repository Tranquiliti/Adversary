package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemOptions) throws JSONException {
        // Create the star system with either name of the first star or a random proc-gen name.
        JSONArray centerStars = systemOptions.getJSONArray("starsInSystemCenter");
        if (centerStars.length() == 0)
            throw new RuntimeException("Cannot create a system with no center stars! Custom star systems require at least one star in the \"starsInSystemCenter\" list!");
        String systemName = (String) util.getJSONValue(centerStars.getJSONObject(0), 'S', "name", null);
        if (systemName == null) systemName = util.getProcGenName("star", null);
        StarSystemAPI system = sector.createStarSystem(systemName);

        // Set location of star system
        float fringeRadius = (int) util.getJSONValue(systemOptions, 'I', "fringeJumpPointOrbitRadius", util.DEFAULT_FRINGE_ORBIT_RADIUS);
        JSONArray locationOverride = (JSONArray) util.getJSONValue(systemOptions, 'A', "setLocationOverride", null);
        if (locationOverride == null) // Place star system in a constellation
            util.setLocation(system, (fringeRadius / 10f) + 100f, (int) util.getJSONValue(systemOptions, 'I', "setLocation", util.DEFAULT_DO_RANDOM_LOCATION));
        else // Place star system in an exact location
            system.getLocation().set(locationOverride.getInt(0), locationOverride.getInt(1));

        // Generate the center stars
        util.addStarsInCenter(system, centerStars, (int) util.getJSONValue(systemOptions, 'I', "starsOrbitRadius", util.DEFAULT_STARS_ORBIT_RADIUS));
        ArrayList<PlanetAPI> starsInSystem = new ArrayList<>(system.getPlanets());
        int numOfCenterStars = starsInSystem.size();

        // Create Fringe Jump-point
        util.addJumpPoint(system, "Fringe Jump-point").setCircularOrbit(system.getCenter(), StarSystemGenerator.random.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f));

        // Create planets from JSON list
        JSONArray planetList = (JSONArray) util.getJSONValue(systemOptions, 'A', "orbitingBodies", null);
        boolean hasFactionPresence = false;
        if (planetList != null) for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanetWithOptions(system, numOfCenterStars, planetList.getJSONObject(i), i);
            if (!newPlanet.getFaction().getId().equals("neutral")) hasFactionPresence = true;
            if (newPlanet.isStar()) starsInSystem.add(newPlanet);
        }

        // Add the system features
        JSONArray systemFeatures = (JSONArray) util.getJSONValue(systemOptions, 'A', "systemFeatures", null);
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.addOrbitingSystemFeature(system, numOfCenterStars, systemFeatures.getJSONObject(i));

        // Adds a coronal hypershunt if enabled
        if ((boolean) util.getJSONValue(systemOptions, 'B', "addCoronalHypershunt", false))
            util.addHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if ((boolean) util.getJSONValue(systemOptions, 'B', "addDomainCryosleeper", false))
            util.addCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags if applicable
        if (!((boolean) util.getJSONValue(systemOptions, 'B', "isCoreWorldSystem", false))) {
            system.removeTag(Tags.THEME_CORE);
            system.addTag(Tags.THEME_MISC);
            system.addTag(Tags.THEME_INTERESTING_MINOR);
        }

        // Set the appropriate background, if applicable
        String background = (String) util.getJSONValue(systemOptions, 'S', "systemBackground", null);
        if (background != null) system.setBackgroundTextureFilename("graphics/backgrounds/" + background);

        // Set the appropriate system music, if applicable
        String music = (String) util.getJSONValue(systemOptions, 'S', "systemMusic", null);
        if (music != null) system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, music);

        util.setDefaultLightColorBasedOnStars(system, starsInSystem);
        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);
    }
}
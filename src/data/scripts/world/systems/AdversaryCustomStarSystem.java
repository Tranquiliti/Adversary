package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemOptions) throws JSONException {
        // Create the star system with either name of the first star or a random proc-gen name.
        JSONObject centerStars = systemOptions.getJSONObject("starsInSystemCenter");
        JSONArray starsList = centerStars.getJSONArray("stars");
        if (starsList.length() == 0)
            throw new RuntimeException("Cannot create a system with no center stars! Custom star systems require at least one star in the \"stars\" list of \"starsInSystemCenter\"!");
        StarSystemAPI system = sector.createStarSystem(starsList.getJSONObject(0).isNull("name") ? util.getProcGenName("star", null) : starsList.getJSONObject(0).getString("name"));

        // Generate the center stars
        util.addStarsInCenter(system, centerStars);
        ArrayList<PlanetAPI> starsInSystem = new ArrayList<>(system.getPlanets());
        int numOfCenterStars = starsInSystem.size();

        // Create the fringe Jump-point and save its orbit radius
        float fringeRadius = util.addFringeJumpPoint(system, systemOptions.isNull("fringeJumpPoint") ? null : systemOptions.getJSONObject("fringeJumpPoint"));

        // Create planets from JSON list
        JSONArray planetList = systemOptions.isNull("orbitingBodies") ? null : systemOptions.getJSONArray("orbitingBodies");
        boolean hasFactionPresence = false;
        if (planetList != null) for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanetWithOptions(system, numOfCenterStars, planetList.getJSONObject(i), i);
            if (newPlanet.isStar()) starsInSystem.add(newPlanet);
            if (!(hasFactionPresence || newPlanet.getFaction().getId().equals("neutral"))) hasFactionPresence = true;
        }

        // Add the system features
        JSONArray systemFeatures = systemOptions.isNull("systemFeatures") ? null : systemOptions.getJSONArray("systemFeatures");
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.addOrbitingSystemFeature(system, numOfCenterStars, systemFeatures.getJSONObject(i));

        // Adds a coronal hypershunt if enabled
        if (!systemOptions.isNull("addCoronalHypershunt") && systemOptions.getBoolean("addCoronalHypershunt"))
            util.addHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if (!systemOptions.isNull("addDomainCryosleeper") && systemOptions.getBoolean("addDomainCryosleeper"))
            util.addCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags if applicable
        if (systemOptions.isNull("isCoreWorldSystem") || !systemOptions.getBoolean("isCoreWorldSystem")) {
            system.removeTag(Tags.THEME_CORE);
            system.addTag(Tags.THEME_MISC);
            system.addTag(Tags.THEME_INTERESTING_MINOR);
        }

        // Set the appropriate background, if applicable
        String background = systemOptions.isNull("systemBackground") ? null : systemOptions.getString("systemBackground");
        if (background != null) system.setBackgroundTextureFilename("graphics/backgrounds/" + background);

        // Set the appropriate system music, if applicable
        String music = systemOptions.isNull("systemMusic") ? null : systemOptions.getString("systemMusic");
        if (music != null) system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, music);

        // Set location of star system
        JSONArray locationOverride = systemOptions.isNull("setLocationOverride") ? null : systemOptions.getJSONArray("setLocationOverride");
        if (locationOverride == null) // Place star system in a constellation
            util.setLocation(system, (fringeRadius / 10f) + 100f, systemOptions.isNull("setLocation") ? 0 : systemOptions.getInt("setLocation"));
        else // Place star system in an exact location
            system.getLocation().set(locationOverride.getInt(0), locationOverride.getInt(1));

        util.setDefaultLightColorBasedOnStars(system, starsInSystem);
        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);
    }
}
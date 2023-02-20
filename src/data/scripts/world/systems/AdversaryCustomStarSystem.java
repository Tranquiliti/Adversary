package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, JSONObject systemOptions) throws JSONException {
        // Create the star system with either name of the first star or a random proc-gen name.
        JSONObject centerStars = systemOptions.getJSONObject("starsInSystemCenter");
        JSONArray starsList = centerStars.getJSONArray("stars");
        if (starsList.length() == 0)
            throw new RuntimeException("Cannot create a system with no center stars! Custom star systems require at least one star in the \"stars\" list of \"starsInSystemCenter\"!");
        StarSystemAPI system = Global.getSector().createStarSystem(starsList.getJSONObject(0).isNull("name") ? util.getProcGenName("star", null) : starsList.getJSONObject(0).getString("name"));

        // Generate the center stars
        util.addStarsInCenter(system, centerStars);
        ArrayList<PlanetAPI> starsInSystem = new ArrayList<>(system.getPlanets());
        int numOfCenterStars = starsInSystem.size();

        // Create the fringe Jump-point and save its orbit radius
        float fringeRadius = util.addFringeJumpPoint(system, systemOptions.isNull("fringeJumpPoint") ? null : systemOptions.getJSONObject("fringeJumpPoint"));

        // Create orbiting bodies
        JSONArray planetList = systemOptions.isNull("orbitingBodies") ? null : systemOptions.getJSONArray("orbitingBodies");
        boolean hasFactionPresence = false;
        if (planetList != null) for (int i = 0; i < planetList.length(); i++) {
            PlanetAPI newPlanet = util.addPlanetWithOptions(system, numOfCenterStars, planetList.getJSONObject(i), i);
            if (newPlanet.isStar()) starsInSystem.add(newPlanet);
            if (!hasFactionPresence && !newPlanet.getFaction().getId().equals("neutral")) hasFactionPresence = true;
        }

        // Add the system features
        JSONArray systemFeatures = systemOptions.isNull("systemFeatures") ? null : systemOptions.getJSONArray("systemFeatures");
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.addSystemFeature(system, numOfCenterStars, systemFeatures.getJSONObject(i));

        // Adds a coronal hypershunt if enabled, defaulting to false
        if (!systemOptions.isNull("addCoronalHypershunt") && systemOptions.getBoolean("addCoronalHypershunt"))
            util.addHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled, defaulting to false
        if (!systemOptions.isNull("addDomainCryosleeper") && systemOptions.getBoolean("addDomainCryosleeper"))
            util.addCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags if applicable, defaulting to false
        if (systemOptions.isNull("isCoreWorldSystem") || !systemOptions.getBoolean("isCoreWorldSystem")) {
            system.removeTag(Tags.THEME_CORE);
            system.addTag(Tags.THEME_MISC);
            system.addTag(Tags.THEME_INTERESTING_MINOR);
        }

        // Set the appropriate background, if applicable
        if (!systemOptions.isNull("systemBackground"))
            system.setBackgroundTextureFilename("graphics/backgrounds/" + systemOptions.getString("systemBackground"));

        // Set the appropriate system music, if applicable
        if (!systemOptions.isNull("systemMusic"))
            system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, systemOptions.getString("systemMusic"));

        // Set location of star system either in a constellation or a specified location
        JSONArray locationOverride = systemOptions.isNull("setLocationOverride") ? null : systemOptions.getJSONArray("setLocationOverride");
        if (locationOverride == null)
            util.setLocation(system, (fringeRadius / 10f) + 100f, systemOptions.isNull("setLocation") ? 0 : systemOptions.getInt("setLocation"));
        else system.getLocation().set(locationOverride.getInt(0), locationOverride.getInt(1));

        util.setDefaultLightColorBasedOnStars(system, starsInSystem);
        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);
    }
}
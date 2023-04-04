package data.scripts.world.systems;

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
        // Create the star system
        StarSystemAPI system = util.generateStarSystem(systemOptions);

        // Generate the center stars
        util.generateSystemCenter(system, systemOptions.getJSONObject("starsInSystemCenter"));
        ArrayList<PlanetAPI> starsInSystem = new ArrayList<>(system.getPlanets());
        int numOfCenterStars = starsInSystem.size();

        // Create the fringe Jump-point and save its orbit radius
        float fringeRadius = util.generateFringeJumpPoint(system, systemOptions.optJSONObject("fringeJumpPoint"));

        // Create orbiting bodies
        JSONArray planetList = systemOptions.optJSONArray("orbitingBodies");
        boolean hasFactionPresence = false;
        if (planetList != null) for (int i = 0; i < planetList.length(); i++) {
            PlanetAPI newBody = util.generateOrbitingBody(system, planetList.getJSONObject(i), numOfCenterStars, i);
            if (newBody.isStar()) starsInSystem.add(newBody);
            else if (newBody.hasCondition("solar_array")) util.addSolarArray(newBody, newBody.getFaction().getId());
            if (!hasFactionPresence && !newBody.getFaction().getId().equals("neutral")) hasFactionPresence = true;
        }

        // Add the system features
        JSONArray systemFeatures = systemOptions.optJSONArray("systemFeatures");
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.generateSystemFeature(system, systemFeatures.getJSONObject(i), numOfCenterStars);

        // Adds a coronal hypershunt if enabled
        if (systemOptions.optBoolean("addCoronalHypershunt", false))
            util.generateHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if (systemOptions.optBoolean("addDomainCryosleeper", false))
            util.generateCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags if applicable
        if (systemOptions.optBoolean("isCoreWorldSystem", false)) {
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
        JSONArray locationOverride = systemOptions.optJSONArray("setLocationOverride");
        if (locationOverride == null)
            util.setLocation(system, (fringeRadius / 10f) + 100f, systemOptions.optInt("setLocation", 0));
        else system.getLocation().set(locationOverride.getInt(0), locationOverride.getInt(1));

        util.setSystemType(system);
        util.setLightColor(system, starsInSystem);
        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);
    }
}
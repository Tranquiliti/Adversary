package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, JSONObject systemOptions) throws JSONException {
        // Create the star system
        StarSystemAPI system = util.generateStarSystem(systemOptions);

        // Generate the center stars
        List<PlanetAPI> starsInSystem = util.generateSystemCenter(system, systemOptions.getJSONObject(util.OPT_STARS_IN_SYSTEM_CENTER));
        int numOfCenterStars = starsInSystem.size();

        // Create the fringe Jump-point and save its orbit radius
        float fringeRadius = util.generateFringeJumpPoint(system, systemOptions.optJSONObject(util.OPT_FRINGE_JUMP_POINT));

        // Create orbiting bodies
        JSONArray planetList = systemOptions.optJSONArray(util.OPT_ORBITING_BODIES);
        boolean hasFactionPresence = false;
        if (planetList != null) for (int i = 0; i < planetList.length(); i++) {
            PlanetAPI newBody = util.generateOrbitingBody(system, planetList.getJSONObject(i), numOfCenterStars, i);
            if (newBody.isStar()) starsInSystem.add(newBody);
            if (!hasFactionPresence && !newBody.getFaction().getId().equals(util.DEFAULT_FACTION_TYPE))
                hasFactionPresence = true;
        }

        // Add the system features
        JSONArray systemFeatures = systemOptions.optJSONArray(util.OPT_SYSTEM_FEATURES);
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.generateSystemFeature(system, systemFeatures.getJSONObject(i), numOfCenterStars);

        // Adds a coronal hypershunt if enabled
        if (systemOptions.optBoolean(util.OPT_ADD_CORONAL_HYPERSHUNT, false))
            util.generateHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if (systemOptions.optBoolean(util.OPT_ADD_DOMAIN_CRYOSLEEPER, false))
            util.generateCryosleeper(system, util.DEFAULT_CRYOSLEEPER_NAME, fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags it is NOT a Core World system
        if (!systemOptions.optBoolean(util.OPT_IS_CORE_WORLD_SYSTEM, false)) {
            system.removeTag(Tags.THEME_CORE);
            system.addTag(Tags.THEME_MISC);
            system.addTag(Tags.THEME_INTERESTING_MINOR);
        }

        // Set the appropriate background, if applicable
        if (!systemOptions.isNull(util.OPT_SYSTEM_BACKGROUND))
            system.setBackgroundTextureFilename(util.PATH_GRAPHICS_BACKGROUND + systemOptions.getString(util.OPT_SYSTEM_BACKGROUND));

        // Set the appropriate system music, if applicable
        if (!systemOptions.isNull(util.OPT_SYSTEM_MUSIC))
            system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, systemOptions.getString(util.OPT_SYSTEM_MUSIC));

        // Set location of star system either in a constellation or a specified location
        JSONArray locationOverride = systemOptions.optJSONArray(util.OPT_SET_LOCATION_OVERRIDE);
        if (locationOverride == null)
            util.setLocation(system, (fringeRadius / 10f) + 100f, systemOptions.optInt(util.OPT_SET_LOCATION, 0));
        else system.getLocation().set(locationOverride.getInt(0), locationOverride.getInt(1));

        util.setSystemType(system);
        util.setLightColor(system, starsInSystem);

        if (systemOptions.optBoolean(util.OPT_HAS_SYSTEMWIDE_NEBULA, false))
            StarSystemGenerator.addSystemwideNebula(system, system.getAge());

        util.generateHyperspace(system);
        util.addRemnantWarningBeacons(system);
    }
}
package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemSettings) throws JSONException {
        // Create the system and set its location
        StarSystemAPI system = sector.createStarSystem(util.getProcGenName("star", null));
        float fringeRadius = (int) util.getJSONValue(systemSettings, 'I', "fringeJumpPointOrbitRadius", util.DEFAULT_FRINGE_ORBIT_RADIUS);
        util.setLocation(system, (fringeRadius / 10f) + 100f, (boolean) util.getJSONValue(systemSettings, 'B', "enableRandomLocation", util.DEFAULT_DO_RANDOM_LOCATION));

        // Generate the center stars
        util.addStarsInCenter(system, systemSettings.getJSONArray("stars"), (int) util.getJSONValue(systemSettings, 'I', "starsOrbitRadius", util.DEFAULT_STARS_ORBIT_RADIUS));
        List<PlanetAPI> systemPlanetList = system.getPlanets();
        int numOfCenterStars = systemPlanetList.size();

        // Create Fringe Jump-point
        util.addOrbitingJumpPoint(system, system.getCenter(), "Fringe Jump-point", fringeRadius);

        // Create planets from JSON list
        JSONArray planetList = (JSONArray) util.getJSONValue(systemSettings, 'A', "planets", null);
        boolean hasFactionPresence = false;
        for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            JSONObject planetOptions = planetList.getJSONObject(i);
            int planetIndex = (int) util.getJSONValue(planetOptions, 'I', "focus", util.DEFAULT_FOCUS);
            SectorEntityToken focus = planetIndex <= 0 ? system.getCenter() : systemPlanetList.get(numOfCenterStars + planetIndex - 1);
            PlanetAPI newPlanet = util.addPlanet(system, focus, i, planetOptions);

            if (!newPlanet.isStar()) {
                String planetFaction = newPlanet.getFaction().getId();

                // Check if the system is occupied by a faction
                if (planetFaction != null && !planetFaction.equals("neutral")) hasFactionPresence = true;

                // Adds solar mirrors and shades if applicable
                if (newPlanet.hasCondition("solar_array")) util.addSolarArrayToPlanet(newPlanet, planetFaction);
            } else { // New "planet" is an orbiting star
                if (system.getSecondary() == null) { // Second star, orbiting far
                    system.setSecondary(newPlanet);
                    system.setType(StarSystemGenerator.StarSystemType.BINARY_FAR);
                } else if (system.getTertiary() == null) { // Third star, orbiting far
                    system.setTertiary(newPlanet);
                    if (system.getType() == StarSystemGenerator.StarSystemType.BINARY_CLOSE)
                        system.setType(StarSystemGenerator.StarSystemType.TRINARY_1CLOSE_1FAR);
                    else if (system.getType() == StarSystemGenerator.StarSystemType.BINARY_FAR)
                        system.setType(StarSystemGenerator.StarSystemType.TRINARY_2FAR);
                }
            }

            // Adds any entities to this planet's lagrange points if applicable
            JSONArray lagrangePoints = (JSONArray) util.getJSONValue(planetOptions, 'A', "entitiesAtStablePoints", null);
            if (lagrangePoints != null) util.addToLagrangePoints(newPlanet, lagrangePoints);
        }

        // Add the system features
        JSONArray systemFeatures = (JSONArray) util.getJSONValue(systemSettings, 'A', "systemFeatures", null);
        if (systemFeatures != null) for (int i = 0; i < systemFeatures.length(); i++)
            util.addOrbitingSystemFeature(system, numOfCenterStars, systemFeatures.getJSONObject(i));

        // Adds a coronal hypershunt if enabled
        if ((boolean) util.getJSONValue(systemSettings, 'B', "addCoronalHypershunt", util.DEFAULT_ADD_HYPERSHUNT))
            util.addHypershunt(system, !hasFactionPresence, true);

        // Adds a Domain-era cryosleeper if enabled
        if ((boolean) util.getJSONValue(systemSettings, 'B', "addDomainCryosleeper", util.DEFAULT_ADD_CRYOSLEEPER))
            util.addCryosleeper(system, "Domain-era Cryosleeper \"Sisyphus\"", fringeRadius + 4000f, !hasFactionPresence);

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE);
        system.addTag(Tags.THEME_INTERESTING);

        system.setProcgen(true);
        util.setDefaultLightColorBasedOnStars(system, numOfCenterStars);
        util.generateHyperspace(system);
    }
}
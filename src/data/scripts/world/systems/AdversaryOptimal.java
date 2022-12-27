package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Random;

public class AdversaryOptimal {
    public void generate(SectorAPI sector, Random randomSeed) throws JSONException {
        SettingsAPI settings = Global.getSettings();
        JSONArray optimalPlanetList = settings.getJSONArray("optimalPlanetList");

        // Create the system and its star
        boolean hasFactionPresence = settings.getBoolean("optimalOccupation");
        StarSystemAPI system = sector.createStarSystem("Optimal");
        String starType = settings.getBoolean("addHypershuntToOptimal") ? "star_blue_supergiant" : "star_orange_giant";
        if (settings.getString("whoOwnsOptimal").equals("player")) {
            starType = "star_red_supergiant";
        }

        PlanetAPI systemStar = system.initStar(null, starType, 1200f, 500f);
        systemStar.setId("system_Optimal");

        // Create Fringe Jump-point
        float fringeRadius = optimalPlanetList.getInt(0);
        JumpPointAPI fringePoint = Global.getFactory().createJumpPoint(null, "Fringe Jump-point");
        fringePoint.setCircularOrbit(systemStar, randomSeed.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + randomSeed.nextFloat() * 5f));
        fringePoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(fringePoint);

        // Create planets from JSON list
        String factionOwner = hasFactionPresence ? settings.getString("whoOwnsOptimal") : null;
        int numOfPlanetsOrbitingStar = 0;
        for (int i = 1; i < optimalPlanetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = AdversaryUtil.addPlanet(system, i, optimalPlanetList.getJSONObject(i), factionOwner, randomSeed);

            // Adds solar mirrors and shades if applicable
            if (newPlanet.hasCondition("solar_array")) {
                int numOfMirrors = 3;
                int numOfShades = 1;
                if (newPlanet.hasCondition("poor_light")) {
                    numOfMirrors += (randomSeed.nextBoolean() ? 2 : 0);
                }
                if (newPlanet.hasCondition("hot")) {
                    numOfShades += (randomSeed.nextBoolean() ? 2 : 0);
                }
                AdversaryUtil.addSolarArray(newPlanet, numOfMirrors, numOfShades, factionOwner);
            }

            // Adds campaign entities at lagrange points of the first two planets orbiting the star
            if (newPlanet.getOrbitFocus().isStar()) {
                numOfPlanetsOrbitingStar++;
                if (numOfPlanetsOrbitingStar == 2) { // 2nd planet to orbit star
                    // Create nav buoy and sensor array on second planet's L4 and L5 points
                    SectorEntityToken navBuoy = system.addCustomEntity(null, null, "nav_buoy", factionOwner);
                    SectorEntityToken sensorArray = system.addCustomEntity(null, null, "sensor_array", factionOwner);
                    if (factionOwner == null) {
                        navBuoy.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                        sensorArray.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    }
                    AdversaryUtil.addToLagrangePoints(newPlanet, null, navBuoy, sensorArray);
                } else if (numOfPlanetsOrbitingStar == 1) { // 1st planet to orbit star
                    // Create comm relay, inactive gate, and inner system jump-point on first planet's Lagrange points
                    JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, "Inner System Jump-point");
                    jumpPoint.setStandardWormholeToHyperspaceVisual();
                    system.addEntity(jumpPoint);
                    SectorEntityToken commRelay = system.addCustomEntity(null, null, "comm_relay", factionOwner);
                    if (factionOwner == null) {
                        commRelay.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    }
                    AdversaryUtil.addToLagrangePoints(newPlanet, commRelay, system.addCustomEntity(null, null, "inactive_gate", null), jumpPoint);
                }
            }
        }

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE); // Technically not part of the Core Worlds
        system.addTag(Tags.THEME_INTERESTING);

        // Adds a coronal hypershunt if enabled
        if (settings.getBoolean("addHypershuntToOptimal")) {
            AdversaryUtil.addHypershunt(system, randomSeed, !hasFactionPresence);
        }

        // Adds a Domain-era cryosleeper if enabled
        if (settings.getBoolean("addCryosleeperToOptimal")) {
            AdversaryUtil.addCryosleeper(system, randomSeed, 15000f, !hasFactionPresence);
        }

        // Finds a suitable location in the constellation nearest to Core Worlds
        AdversaryUtil.setLocation(system, (fringeRadius / 10) + 100f, sector, !settings.getBoolean("enableRandomLocationForOptimal"));
        AdversaryUtil.generateHyperspace(system);
    }
}
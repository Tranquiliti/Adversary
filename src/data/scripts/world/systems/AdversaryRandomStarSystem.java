package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Random;

public class AdversaryRandomStarSystem {
    public void generate(SectorAPI sector, JSONArray starSystemSettings, String starType, Random randomSeed) throws JSONException {
        // Create the system and set its location
        float fringeRadius = starSystemSettings.getInt(0);
        StarSystemAPI system = sector.createStarSystem(ProcgenUsedNames.pickName("star", null, null).nameWithRomanSuffixIfAny);
        AdversaryUtil.setLocation(system, (fringeRadius / 10) + 100f, sector, false);

        // Creates star for this system
        PlanetAPI systemStar = system.initStar(null, starType, 1200f, 500f);
        systemStar.setId("system_" + systemStar.getId()); // Id format for randomly-generated stars

        // Create Fringe Jump-point
        JumpPointAPI fringePoint = Global.getFactory().createJumpPoint(null, "Fringe Jump-point");
        fringePoint.setCircularOrbit(systemStar, randomSeed.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + randomSeed.nextFloat() * 5f));
        fringePoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(fringePoint);

        // Create planets from JSON list
        int numOfPlanetsOrbitingStar = 0;
        for (int i = 1; i < starSystemSettings.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = AdversaryUtil.addPlanet(system, i, starSystemSettings.getJSONObject(i), null, randomSeed);

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
                AdversaryUtil.addSolarArray(newPlanet, numOfMirrors, numOfShades, null);
            }

            // Adds campaign entities at lagrange points of the first two planets orbiting the star
            if (newPlanet.getOrbitFocus().isStar()) {
                numOfPlanetsOrbitingStar++;
                if (numOfPlanetsOrbitingStar == 2) { // 2nd planet to orbit star
                    // Create nav buoy and sensor array on second planet's L4 and L5 points
                    SectorEntityToken navBuoy = system.addCustomEntity(null, null, "nav_buoy", null);
                    SectorEntityToken sensorArray = system.addCustomEntity(null, null, "sensor_array", null);
                    navBuoy.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    sensorArray.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);

                    AdversaryUtil.addToLagrangePoints(newPlanet, null, navBuoy, sensorArray);
                } else if (numOfPlanetsOrbitingStar == 1) { // 1st planet to orbit star
                    // Create comm relay, inactive gate, and inner system jump-point on first planet's Lagrange points
                    JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, "Inner System Jump-point");
                    jumpPoint.setStandardWormholeToHyperspaceVisual();
                    system.addEntity(jumpPoint);
                    SectorEntityToken commRelay = system.addCustomEntity(null, null, "comm_relay", null);
                    commRelay.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    AdversaryUtil.addToLagrangePoints(newPlanet, commRelay, system.addCustomEntity(null, null, "inactive_gate", null), jumpPoint);
                }
            }
        }

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE);
        system.addTag(Tags.THEME_INTERESTING);

        AdversaryUtil.generateHyperspace(system);
    }
}

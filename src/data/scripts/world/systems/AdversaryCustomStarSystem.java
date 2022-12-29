package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import data.scripts.AdversaryUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class AdversaryCustomStarSystem {
    public void generate(AdversaryUtil util, SectorAPI sector, JSONObject systemSettings, String starType, Random randomSeed) throws JSONException {
        // Create the system and set its location
        float fringeRadius = systemSettings.getInt("fringeJumpPointOrbitRadius");
        StarSystemAPI system = sector.createStarSystem(ProcgenUsedNames.pickName("star", null, null).nameWithRomanSuffixIfAny);
        util.setLocation(system, (fringeRadius / 10) + 100f, sector, systemSettings.getBoolean("enableRandomLocation"));

        // Creates star for this system
        if (systemSettings.getBoolean("addCoronalHypershunt")) {
            starType = "star_blue_supergiant";
        }
        PlanetAPI systemStar = system.initStar(null, starType, 1200f, 500f);
        systemStar.setId("system_" + systemStar.getId()); // Id format for randomly-generated stars

        // Create Fringe Jump-point
        JumpPointAPI fringePoint = Global.getFactory().createJumpPoint(null, "Fringe Jump-point");
        fringePoint.setCircularOrbit(systemStar, randomSeed.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + randomSeed.nextFloat() * 5f));
        fringePoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(fringePoint);

        // Create planets from JSON list
        JSONArray planetList = systemSettings.getJSONArray("planetList");
        int numOfPlanetsOrbitingStar = 0;
        boolean hasFactionPresence = false;
        for (int i = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanet(system, i + 1, planetList.getJSONObject(i), randomSeed);
            String planetFaction = newPlanet.getFaction().getId();

            // Check if the system is occupied by a faction
            if (!(planetFaction == null || planetFaction.equals("neutral"))) {
                hasFactionPresence = true;
            }

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
                util.addSolarArray(newPlanet, numOfMirrors, numOfShades, planetFaction);
            }

            // Adds campaign entities at lagrange points of the first two planets orbiting the star
            if (newPlanet.getOrbitFocus().isStar()) {
                numOfPlanetsOrbitingStar++;
                if (numOfPlanetsOrbitingStar == 2) { // 2nd planet to orbit star
                    // Create nav buoy and sensor array on second planet's L4 and L5 points
                    SectorEntityToken navBuoy = system.addCustomEntity(null, null, "nav_buoy", planetFaction);
                    SectorEntityToken sensorArray = system.addCustomEntity(null, null, "sensor_array", planetFaction);
                    if (planetFaction == null || planetFaction.equals("neutral")) {
                        navBuoy.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                        sensorArray.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    }

                    util.addToLagrangePoints(newPlanet, null, navBuoy, sensorArray);
                } else if (numOfPlanetsOrbitingStar == 1) { // 1st planet to orbit star
                    // Create comm relay, inactive gate, and inner system jump-point on first planet's Lagrange points
                    JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, "Inner System Jump-point");
                    jumpPoint.setStandardWormholeToHyperspaceVisual();
                    system.addEntity(jumpPoint);
                    SectorEntityToken commRelay = system.addCustomEntity(null, null, "comm_relay", planetFaction);
                    if (planetFaction == null || planetFaction.equals("neutral")) {
                        commRelay.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
                    }

                    util.addToLagrangePoints(newPlanet, commRelay, system.addCustomEntity(null, null, "inactive_gate", null), jumpPoint);
                }
            }
        }

        // Adds a coronal hypershunt if enabled
        if (systemSettings.getBoolean("addCoronalHypershunt")) {
            util.addHypershunt(system, randomSeed, !hasFactionPresence);
        }

        // Adds a Domain-era cryosleeper if enabled
        if (systemSettings.getBoolean("addDomainCryosleeper")) {
            util.addCryosleeper(system, null, 15000f, !hasFactionPresence, randomSeed);
        }

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE);
        system.addTag(Tags.THEME_INTERESTING);

        util.generateHyperspace(system);
    }
}

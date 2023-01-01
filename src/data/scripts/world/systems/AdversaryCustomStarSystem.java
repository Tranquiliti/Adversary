package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
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
        if (systemSettings.getBoolean("addCoronalHypershunt")) starType = "star_blue_supergiant";
        PlanetAPI systemStar = system.initStar(null, starType, 1200f, 500f);
        systemStar.setId("system_" + systemStar.getId()); // Id format for randomly-generated stars

        // Create Fringe Jump-point
        JumpPointAPI fringePoint = Global.getFactory().createJumpPoint(null, "Fringe Jump-point");
        fringePoint.setCircularOrbit(systemStar, randomSeed.nextFloat() * 360f, fringeRadius, fringeRadius / (15f + randomSeed.nextFloat() * 5f));
        fringePoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(fringePoint);

        // Create planets from JSON list
        JSONArray planetList = systemSettings.getJSONArray("planetList");
        boolean hasFactionPresence = false;
        for (int i = 0, numOfPlanetsOrbitingStar = 0; i < planetList.length(); i++) {
            // Creates planet with appropriate characteristics
            PlanetAPI newPlanet = util.addPlanet(system, i + 1, planetList.getJSONObject(i), randomSeed);
            String planetFaction = newPlanet.getFaction().getId();

            // Check if the system is occupied by a faction
            if (!(planetFaction == null || planetFaction.equals("neutral"))) {
                hasFactionPresence = true;
            }

            // Adds solar mirrors and shades if applicable
            util.addSolarArrayToPlanet(newPlanet, planetFaction, randomSeed);

            // Adds campaign entities at lagrange points of the first two planets orbiting the star
            if (newPlanet.getOrbitFocus().isStar()) {
                switch (numOfPlanetsOrbitingStar++) {
                    case 2: // 2nd planet to orbit star
                        // Create nav buoy and sensor array on second planet's L4 and L5 points
                        util.addToLagrangePoints(newPlanet, null, util.addObjective(system, "nav_buoy", planetFaction), util.addObjective(system, "sensor_array", planetFaction));
                        break;
                    case 1: // 1st planet to orbit star
                        // Create comm relay, inactive gate, and inner system jump-point on first planet's Lagrange points
                        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, "Inner System Jump-point");
                        jumpPoint.setStandardWormholeToHyperspaceVisual();
                        system.addEntity(jumpPoint);
                        util.addToLagrangePoints(newPlanet, util.addObjective(system, "comm_relay", planetFaction), system.addCustomEntity(null, null, "inactive_gate", null), jumpPoint);
                        break;
                }
            }
        }

        // Add the system terrain
        JSONArray terrainFeatures = systemSettings.getJSONArray("terrainFeatures");
        for (int i = 0; i < terrainFeatures.length(); i++) {
            JSONObject terrain = terrainFeatures.getJSONObject(i);
            switch (terrain.getString("type")) {
                case "asteroidBelt":
                    util.addAsteroidBelt(system.getPlanets().get(terrain.getInt("focus")), terrain.getInt("orbitRadius"));
                    break;
                case "ringBand":
                    util.addRingBand(system.getPlanets().get(terrain.getInt("focus")), terrain.getInt("orbitRadius"));
                    break;
                case "magneticField":
                    util.addMagneticField(system.getPlanets().get(terrain.getInt("focus")));
                    break;
            }
        }

        // Adds a coronal hypershunt if enabled
        if (systemSettings.getBoolean("addCoronalHypershunt")) {
            util.addHypershunt(system, randomSeed, !hasFactionPresence, true);
        }

        // Adds a Domain-era cryosleeper if enabled
        if (systemSettings.getBoolean("addDomainCryosleeper")) {
            util.addCryosleeper(system, null, fringeRadius + 5000f, !hasFactionPresence, randomSeed);
        }

        // Add relevant system tags
        system.removeTag(Tags.THEME_CORE);
        system.addTag(Tags.THEME_INTERESTING);

        util.generateHyperspace(system);
    }
}
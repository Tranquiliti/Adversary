package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoronalTapParticleScript;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A utility class for the Adversary mod
 */
public class AdversaryUtil {
    public HashMap<MarketAPI, String> marketsToOverrideAdmin; // Is updated in the addMarket private helper method

    // Making a utility class instantiable just so I can modify admins properly D:
    public AdversaryUtil() {
        marketsToOverrideAdmin = new HashMap<>();
    }

    /**
     * Adds an asteroid belt around a planet (note that stars are technically planets too)
     *
     * @param planet      The focus
     * @param orbitRadius How far it is located from center of system
     */
    public void addAsteroidBelt(PlanetAPI planet, float orbitRadius) {
        // "Nemo's Band" Corvus asteroid belt
        StarSystemAPI system = planet.getStarSystem();
        system.addAsteroidBelt(planet, Math.round(orbitRadius / 60), orbitRadius, 256f, Math.round(orbitRadius / 38f), Math.round(orbitRadius / 19f), Terrain.ASTEROID_BELT, null);
        system.addRingBand(planet, "misc", "rings_dust0", 256f, 3, Color.white, 256f, orbitRadius - 60f, Math.round(orbitRadius / 18f), null, null);
        system.addRingBand(planet, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, orbitRadius + 60f, Math.round(orbitRadius / 19.5f), null, null);
    }

    /**
     * Adds a ring band around a planet
     *
     * @param planet      The focus
     * @param orbitRadius How far it is located from center of system
     */
    public void addRingBand(PlanetAPI planet, float orbitRadius) {
        // Barad ring band
        planet.getStarSystem().addRingBand(planet, "misc", "rings_ice0", 256f, 2, Color.white, 256f, orbitRadius, Math.round(orbitRadius / 23f), Terrain.RING, null);
    }

    /**
     * Adds a magnetic field around a planet
     *
     * @param planet The focus
     */
    public void addMagneticField(PlanetAPI planet) {
        // Barad magnetic field
        float planetRadius = planet.getRadius();
        planet.getStarSystem().addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(planetRadius + 200f, // terrain effect band width
                (planetRadius + 200f) / 2f, // terrain effect middle radius
                planet, // entity that it's around
                planetRadius + 50f, // visual band start
                planetRadius + 50f + 250f, // visual band end
                new Color(50, 20, 100, 40), // base color
                0.5f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                new Color(140, 100, 235), new Color(180, 110, 210), new Color(150, 140, 190), new Color(140, 190, 210), new Color(90, 200, 170), new Color(65, 230, 160), new Color(20, 220, 70))).setCircularOrbit(planet, 0, 0, 100f);
    }

    /**
     * Adds a Domain-era cryosleeper in a star system
     *
     * @param system       Star system to modify
     * @param orbitRadius  How far cryosleeper is located from center of system
     * @param discoverable Whether cryosleeper needs to be discovered before being revealed in map
     * @param randomSeed   Seed for cryosleeper generation
     */
    public void addCryosleeper(StarSystemAPI system, String name, float orbitRadius, boolean discoverable, Random randomSeed) {
        SectorEntityToken cryosleeper = system.addCustomEntity(null, name, "derelict_cryosleeper", "derelict");
        cryosleeper.setCircularOrbitWithSpin(system.getStar(), randomSeed.nextFloat() * 360f, orbitRadius, orbitRadius / (10f + randomSeed.nextFloat() * 5f), 1f, 2f);
        cryosleeper.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

        if (discoverable) {
            cryosleeper.setSensorProfile(1f);
            cryosleeper.getDetectedRangeMod().modifyFlat("gen", 3500f);
            cryosleeper.setDiscoverable(true);
        }

        system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
    }

    /**
     * Adds a coronal hypershunt in a star system.
     *
     * @param system             Star system to modify
     * @param randomSeed         Seed for hypershunt generation
     * @param discoverable       Whether hypershunt needs to be discovered before being revealed in map
     * @param hasParticleEffects Whether the hypershunt should emit particle effects upon activation; set to false for better performance
     */
    public void addHypershunt(StarSystemAPI system, Random randomSeed, boolean discoverable, boolean hasParticleEffects) {
        PlanetAPI star = system.getStar();
        SectorEntityToken hypershunt = system.addCustomEntity(null, null, "coronal_tap", null);
        float coronalOrbitRadius = star.getRadius() + hypershunt.getRadius() + 100f;
        hypershunt.setCircularOrbitPointingDown(star, randomSeed.nextFloat() * 360f, coronalOrbitRadius, coronalOrbitRadius / (20f + randomSeed.nextFloat() * 5f));

        if (discoverable) {
            hypershunt.setSensorProfile(1f);
            hypershunt.getDetectedRangeMod().modifyFlat("gen", 3500f);
            hypershunt.setDiscoverable(true);
        }

        if (hasParticleEffects) {
            Global.getSector().addScript(new CoronalTapParticleScript(hypershunt));
        }

        system.addTag(Tags.HAS_CORONAL_TAP);
    }

    /**
     * Adds a system objective in a star system
     *
     * @param system      Star system to modify
     * @param objectiveId System objective id; should either be "comm_relay", "sensor_array", or "nav_buoy"
     * @param factionId   Faction owning the system objective
     * @return The newly-created system objective
     */
    public SectorEntityToken addObjective(StarSystemAPI system, String objectiveId, String factionId) {
        SectorEntityToken objective = system.addCustomEntity(null, null, objectiveId, factionId);
        if (factionId == null || factionId.equals("neutral")) {
            objective.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
        }

        return objective;
    }

    /**
     * Adds a planet in a star system
     *
     * @param system        Star system
     * @param id            Planet number
     * @param planetOptions Planet characteristics
     * @param randomSeed    Seed for planet generation
     * @return The newly-generated Planet
     * @throws JSONException if planetOptions is invalid or has wrong format
     */
    public PlanetAPI addPlanet(StarSystemAPI system, int id, JSONObject planetOptions, Random randomSeed) throws JSONException {
        int planetIndex = planetOptions.getInt("focus");
        SectorEntityToken focus = planetIndex <= 0 ? system.getStar() : system.getPlanets().get(planetIndex);
        float orbitRadius = planetOptions.getInt("orbitRadius");

        PlanetAPI newPlanet = system.addPlanet(system.getStar().getId() + ":planet_" + id, focus, ProcgenUsedNames.pickName("planet", system.getStar().getName(), null).nameWithRomanSuffixIfAny, planetOptions.getString("type"), randomSeed.nextFloat() * 360f, planetOptions.getInt("radius"), orbitRadius, orbitRadius / (20f + randomSeed.nextFloat() * 5f));
        newPlanet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

        if (planetOptions.getInt("marketSize") <= 0) {
            addPlanetConditions(newPlanet, planetOptions);
        } else {
            addPlanetMarket(newPlanet, planetOptions);
        }

        return newPlanet;
    }

    // Adds planetary conditions to planet
    private void addPlanetConditions(PlanetAPI planet, JSONObject planetOptions) throws JSONException {
        Misc.initConditionMarket(planet);
        MarketAPI planetMarket = planet.getMarket();
        JSONArray conditions = planetOptions.getJSONArray("conditions");
        for (int i = 0; i < conditions.length(); i++) {
            planetMarket.addCondition(conditions.getString(i));
        }
    }

    // Adds a populated market with specified options
    private void addPlanetMarket(PlanetAPI planet, JSONObject planetOptions) throws JSONException {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();

        // Create planet market
        int size = planetOptions.getInt("marketSize");
        String factionId = planetOptions.getString("factionId");
        MarketAPI planetMarket = Global.getFactory().createMarket(planet.getId() + "_market", planet.getName(), size);
        planetMarket.setFactionId(factionId);
        planetMarket.setPrimaryEntity(planet);
        planetMarket.getTariff().setBaseValue(0.3f); // Default tariff value
        planetMarket.setFreePort(false);

        planetMarket.addCondition("population_" + size);
        JSONArray conditions = planetOptions.getJSONArray("conditions");
        for (int i = 0; i < conditions.length(); i++) {
            planetMarket.addCondition(conditions.getString(i));
        }

        JSONArray industries = planetOptions.getJSONArray("industries");
        for (int i = 0; i < industries.length(); i++) {
            planetMarket.addIndustry(industries.getString(i));
        }

        JSONObject specials = planetOptions.getJSONObject("specials");
        Iterator<String> specialIterator = specials.keys();
        while (specialIterator.hasNext()) {
            String industry = specialIterator.next();
            JSONArray items = specials.getJSONArray(industry);

            String specialItem = items.getString(0);
            if (specialItem != null && !specialItem.isEmpty()) {
                planetMarket.getIndustry(industry).setSpecialItem(new SpecialItemData(specialItem, null));
            }

            String aiCore = items.getString(1);
            if (aiCore != null && !aiCore.isEmpty()) {
                planetMarket.getIndustry(industry).setAICoreId(aiCore);
            }
        }

        // Add the appropriate submarkets
        planetMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (factionId.equals("player")) {
            // TODO: fix faction not being properly set until colonizing another planet and still needing to pay for storage access
            planetMarket.setPlayerOwned(true);
            planetMarket.addSubmarket(Submarkets.LOCAL_RESOURCES);
            marketsToOverrideAdmin.put(planetMarket, null);
        } else {
            planetMarket.addSubmarket(Submarkets.SUBMARKET_OPEN);
            if (planetMarket.hasIndustry("militarybase") || planetMarket.hasIndustry("highcommand")) {
                planetMarket.addSubmarket(Submarkets.GENERIC_MILITARY);
            }
            planetMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        }

        // Adds an AI core admin to the market if enabled
        if (planetOptions.getBoolean("aiCoreAdmin")) {
            marketsToOverrideAdmin.put(planetMarket, "alpha_core");
        }

        //set market in global, factions, and assign market, also submarkets
        globalEconomy.addMarket(planetMarket, true);
        planet.setMarket(planetMarket);
        planet.setFaction(factionId);
    }

    /**
     * Adds solar array entities near a planet
     *
     * @param planet       Planet to modify
     * @param numOfMirrors Number of solar mirrors
     * @param numOfShades  Number of solar shades
     * @param factionId    Faction owning the solar array
     * @throws IllegalArgumentException if numOfMirrors > 5, numOfShades > 3, or either of the numbers are even
     */
    public void addSolarArray(PlanetAPI planet, int numOfMirrors, int numOfShades, String factionId) {
        if (numOfMirrors % 2 == 0 || numOfShades % 2 == 0 || numOfMirrors > 5 || numOfShades > 3) {
            throw new IllegalArgumentException("Invalid number of solar mirrors and/or shades");
        }

        StarSystemAPI system = planet.getStarSystem();
        float planetRadius = planet.getRadius();
        float planetOrbitPeriod = planet.getCircularOrbitPeriod();

        // Create solar mirrors
        String[] mirrorNames = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon"};
        float mirrorAngle = planet.getCircularOrbitAngle() - 30f * (numOfMirrors >>> 1);
        int mirrorIndex = 2 - (numOfMirrors / 2);
        for (int i = 0; i < numOfMirrors; i++) {
            SectorEntityToken mirror = system.addCustomEntity(null, "Stellar Mirror " + mirrorNames[mirrorIndex], "stellar_mirror", factionId);
            mirror.setCircularOrbitPointingDown(planet, mirrorAngle, planetRadius + 250f, planetOrbitPeriod);

            if (factionId == null || factionId.equals("neutral")) {
                mirror.setSensorProfile(1f);
                mirror.getDetectedRangeMod().modifyFlat("gen", 2200f);
                mirror.setDiscoveryXP(300f);
                mirror.setDiscoverable(true);
            }

            mirrorIndex++;
            mirrorAngle += 30f;
        }

        // Create solar shades
        String[] shadeNames = {"Chi", "Psi", "Omega"};
        float shadeAngle = ((planet.getCircularOrbitAngle() + 180f) % 360f) - 30f * (numOfShades >>> 1);
        int shadeIndex = 1 - (numOfShades / 2);
        for (int i = 0; i < numOfShades; i++) {
            SectorEntityToken shade = system.addCustomEntity(null, "Stellar Shade " + shadeNames[shadeIndex], "stellar_shade", factionId);
            shade.setCircularOrbitPointingDown(planet, shadeAngle, planetRadius + 240f, planetOrbitPeriod);

            if (factionId == null || factionId.equals("neutral")) {
                shade.setSensorProfile(1f);
                shade.getDetectedRangeMod().modifyFlat("gen", 2200f);
                shade.setDiscoveryXP(300f);
                shade.setDiscoverable(true);
            }

            shadeIndex++;
            shadeAngle += 30f;
        }
    }

    /**
     * Adds a solar array near a planet, taking into account planetary conditions
     *
     * @param planet     Planet to modify
     * @param factionId  Faction owning the solar array
     * @param randomSeed Seed for planet generation
     */
    public void addSolarArrayToPlanet(PlanetAPI planet, String factionId, Random randomSeed) {
        // Adds solar mirrors and shades if applicable
        if (planet.hasCondition("solar_array")) {
            int numOfMirrors = 3;
            int numOfShades = 1;
            if (planet.hasCondition("poor_light")) {
                numOfMirrors += (randomSeed.nextBoolean() ? 2 : 0);
            }
            if (planet.hasCondition("hot")) {
                numOfShades += (randomSeed.nextBoolean() ? 2 : 0);
            }
            addSolarArray(planet, numOfMirrors, numOfShades, factionId);
        }
    }

    /**
     * Adds system entities in a planet's L3, L4, and L5 points, respectively
     *
     * @param planet   Planet
     * @param entityL3 Entity to add at L3 point
     * @param entityL4 Entity to add at L4 point
     * @param entityL5 Entity to add at L5 point
     */
    public void addToLagrangePoints(PlanetAPI planet, SectorEntityToken entityL3, SectorEntityToken entityL4, SectorEntityToken entityL5) {
        PlanetAPI systemStar = planet.getStarSystem().getStar();
        float planetAngle = planet.getCircularOrbitAngle();
        float planetOrbitRadius = planet.getCircularOrbitRadius();
        float planetOrbitPeriod = planet.getCircularOrbitPeriod();
        if (entityL3 != null) {
            entityL3.setCircularOrbitPointingDown(systemStar, planetAngle - 180f, planetOrbitRadius, planetOrbitPeriod);
        }
        if (entityL4 != null) {
            entityL4.setCircularOrbitPointingDown(systemStar, planetAngle + 60f, planetOrbitRadius, planetOrbitPeriod);
        }
        if (entityL5 != null) {
            entityL5.setCircularOrbitPointingDown(systemStar, planetAngle - 60f, planetOrbitRadius, planetOrbitPeriod);
        }
    }

    /**
     * Generates a system's hyperspace jump points and clears nearby nebula
     *
     * @param system Star system to modify
     */
    public void generateHyperspace(StarSystemAPI system) {
        system.autogenerateHyperspaceJumpPoints(true, false);

        // Clear nebula in hyperspace
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float totalRadius = system.getMaxRadiusInHyperspace() + plugin.getTileSize() * 2f;
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, totalRadius, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, totalRadius, 0f, 360f, 0.25f);
    }

    /**
     * Set a star system's location to the middle of a constellation;
     * will affect the seed used by random sector generation if setToNearest is false
     * <p>
     * Modified from the constellation proc-gen code made originally by Audax.
     *
     * @param system           Star system to relocate
     * @param hyperspaceRadius Radius of star system in hyperspace
     * @param sector           Sector
     * @param isRandom         If true, set location to a random constellation; else, set location to the nearest constellation (to Core Worlds)
     */
    public void setLocation(StarSystemAPI system, float hyperspaceRadius, SectorAPI sector, boolean isRandom) {
        // Get all proc-gen constellations in Sector hyperspace
        LinkedHashSet<Constellation> constellations = new LinkedHashSet<>();
        for (StarSystemAPI sys : sector.getStarSystems()) {
            if (sys.isInConstellation() && sys.isProcgen()) {
                constellations.add(sys.getConstellation());
            }
        }

        // If no constellations exist (for whatever reason), just set location to middle of Core Worlds
        // (you could consider them a special constellation?)
        final Vector2f CORE_WORLD_CENTER = new Vector2f(-6000, -6000);
        if (constellations.isEmpty()) {
            system.getLocation().set(CORE_WORLD_CENTER);
            return;
        }

        // Select the constellation
        Constellation selectedConstellation = null;
        if (isRandom) { // Set location to a random constellation
            Random randomSeed = StarSystemGenerator.random;
            int currentIndex = 0;
            int indexToStop = randomSeed.nextInt(constellations.size());
            for (Constellation thisConst : constellations) {
                if (currentIndex == indexToStop) {
                    selectedConstellation = thisConst;
                    break;
                }
                currentIndex++;
            }
        } else { // Set location to the constellation closest to Core Worlds
            float minDistance = Float.MAX_VALUE;
            for (Constellation thisConst : constellations) {
                float distance = Misc.getDistance(CORE_WORLD_CENTER, thisConst.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    selectedConstellation = thisConst;
                }
            }
        }

        // Get centroid point of the selected constellation
        float centroidX = 0;
        float centroidY = 0;
        assert selectedConstellation != null; // Should never be null due to previous checks, but who knows?
        List<StarSystemAPI> nearestSystems = selectedConstellation.getSystems();
        for (StarSystemAPI sys : nearestSystems) {
            Vector2f loc = sys.getHyperspaceAnchor().getLocationInHyperspace();
            centroidX += loc.getX();
            centroidY += loc.getY();
        }
        centroidX /= nearestSystems.size();
        centroidY /= nearestSystems.size();

        // Find an empty spot in the constellation, starting at the middle and
        // then searching for locations around it in a square pattern
        Vector2f newLoc = null;
        final float STEP_SIZE = 25f; // How far apart each prospective location should be from each other
        int curX = 0;
        int curY = 0;
        int squareSize = 0;
        byte move = 3; // 0 = left, 1 = down; 2 = right; 3 = up
        while (newLoc == null) {
            float thisX = curX * STEP_SIZE + centroidX;
            float thisY = curY * STEP_SIZE + centroidY;
            boolean intersects = false;
            for (StarSystemAPI sys : nearestSystems) {
                Vector2f sysLoc = sys.getHyperspaceAnchor().getLocation();
                float dX = thisX - sysLoc.getX();
                float dY = thisY - sysLoc.getY();
                float dR = hyperspaceRadius + sys.getMaxRadiusInHyperspace();
                if (dX * dX + dY * dY < dR * dR) { // Formula to check if two circular areas intersect
                    intersects = true;
                    break;
                }
            }

            if (!intersects) { // Found an empty location
                newLoc = new Vector2f(thisX, thisY);
            } else if (move == 0) { // moving left
                if (curX == -squareSize) {
                    move = 1;
                    curY--;
                } else {
                    curX--;
                }
            } else if (move == 1) { // moving down
                if (curY == -squareSize) {
                    move = 2;
                    curX++;
                } else {
                    curY--;
                }
            } else if (move == 2) { // moving right
                if (curX == squareSize) {
                    move = 3;
                    curY++;
                } else {
                    curX++;
                }
            } else { // moving up
                if (curY == squareSize) { // Checked the full perimeter, so increase search size
                    squareSize++;
                    curX = squareSize - 1;
                    curY = squareSize;
                    move = 0;
                } else {
                    curY++;
                }
            }
        }

        // Generate system as part of the selected constellation
        nearestSystems.add(system);
        system.setConstellation(selectedConstellation);
        system.getLocation().set(newLoc);
    }
}
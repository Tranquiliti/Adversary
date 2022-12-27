package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

public class AdversaryUtil {
    /**
     * Adds a coronal hypershunt to a star system
     *
     * @param system       Star system to modify
     * @param randomSeed   Seed for hypershunt generation
     * @param discoverable Whether hypershunt needs to be discovered before being revealed in map
     */
    public static void addHypershunt(StarSystemAPI system, Random randomSeed, boolean discoverable) {
        PlanetAPI star = system.getStar();
        SectorEntityToken coronalTap = system.addCustomEntity(null, null, "coronal_tap", null);
        float coronalOrbitRadius = star.getRadius() + coronalTap.getRadius() + 100f;
        coronalTap.setCircularOrbitPointingDown(star, randomSeed.nextFloat() * 360f, coronalOrbitRadius, coronalOrbitRadius / (20f + randomSeed.nextFloat() * 5f));

        if (discoverable) {
            coronalTap.setSensorProfile(1f);
            coronalTap.getDetectedRangeMod().modifyFlat("gen", 3500f);
            coronalTap.setDiscoverable(true);
        }

        system.addTag(Tags.HAS_CORONAL_TAP);
    }

    /**
     * Adds a Domain-era cryosleeper to a star system
     *
     * @param system       Star system to modify
     * @param randomSeed   Seed for cryosleeper generation
     * @param orbitRadius  How far cryosleeper is located from center of system
     * @param discoverable Whether cryosleeper needs to be discovered before being revealed in map
     */
    public static void addCryosleeper(StarSystemAPI system, Random randomSeed, float orbitRadius, boolean discoverable) {
        SectorEntityToken cryosleeper = system.addCustomEntity(null, "Domain-era Cryosleeper \"Sisyphus\"", "derelict_cryosleeper", "derelict");
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
     * Adds a planet in a star system
     *
     * @param system        Star system
     * @param id            Planet number
     * @param planetOptions Planet characteristics
     * @param factionOwner  Faction id; set to 'null' if planet should remain unclaimed
     * @param randomSeed    Seed for planet generation
     * @return The newly-generated Planet
     * @throws JSONException if planetOptions is invalid or has wrong format
     */
    public static PlanetAPI addPlanet(StarSystemAPI system, int id, JSONObject planetOptions, String factionOwner, Random randomSeed) throws JSONException {
        int orbitFocus = planetOptions.getInt("focus");
        SectorEntityToken focus = orbitFocus <= 0 ? system.getStar() : system.getPlanets().get(orbitFocus);
        float orbitRadius = planetOptions.getInt("orbitRadius");

        PlanetAPI newPlanet = system.addPlanet(system.getStar().getId() + ":planet_" + id, focus, ProcgenUsedNames.pickName("planet", system.getStar().getName(), null).nameWithRomanSuffixIfAny, planetOptions.getString("type"), randomSeed.nextFloat() * 360f, planetOptions.getInt("radius"), orbitRadius, orbitRadius / (20f + randomSeed.nextFloat() * 5f));
        newPlanet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

        if (factionOwner == null || planetOptions.getInt("marketSize") <= 0) {
            addConditions(newPlanet, planetOptions);
        } else {
            addMarket(newPlanet, planetOptions, factionOwner);
        }

        return newPlanet;
    }

    // Adds planetary conditions to planet
    private static void addConditions(PlanetAPI planet, JSONObject planetOptions) throws JSONException {
        Misc.initConditionMarket(planet);
        MarketAPI planetMarket = planet.getMarket();
        JSONArray conditions = planetOptions.getJSONArray("conditions");
        for (int i = 0; i < conditions.length(); i++) {
            planetMarket.addCondition(conditions.getString(i));
        }
    }

    // Adds a populated market with specified options
    private static void addMarket(PlanetAPI planet, JSONObject planetOptions, String owner) throws JSONException {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();

        // Create planet market
        int size = planetOptions.getInt("marketSize");
        MarketAPI planetMarket = Global.getFactory().createMarket(planet.getId() + "_market", planet.getName(), size);
        planetMarket.setFactionId(owner);
        planetMarket.setPrimaryEntity(planet);
        planetMarket.getTariff().setBaseValue(0.3f); // Default tariff value

        // Add the submarkets
        planetMarket.addSubmarket(Submarkets.GENERIC_MILITARY);
        planetMarket.addSubmarket(Submarkets.SUBMARKET_OPEN);
        planetMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        planetMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);

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

        //Set free port
        planetMarket.setFreePort(false);

        //set market in global, factions, and assign market, also submarkets
        globalEconomy.addMarket(planetMarket, true);
        planet.setMarket(planetMarket);
        planet.setFaction(owner);
    }

    /**
     * Adds solar array entities to a planet
     *
     * @param planet       Planet to modify
     * @param numOfMirrors Number of solar mirrors
     * @param numOfShades  Number of solar shades
     * @param factionOwner Faction id
     * @throws IllegalArgumentException if numOfMirrors > 5, numOfShades > 3, or either of the numbers are even
     */
    public static void addSolarArray(PlanetAPI planet, int numOfMirrors, int numOfShades, String factionOwner) {
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
            SectorEntityToken mirror = system.addCustomEntity(null, "Stellar Mirror " + mirrorNames[mirrorIndex], "stellar_mirror", factionOwner);
            mirror.setCircularOrbitPointingDown(planet, mirrorAngle, planetRadius + 250f, planetOrbitPeriod);

            if (factionOwner == null || factionOwner.equals("neutral")) {
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
            SectorEntityToken shade = system.addCustomEntity(null, "Stellar Shade " + shadeNames[shadeIndex], "stellar_shade", factionOwner);
            shade.setCircularOrbitPointingDown(planet, shadeAngle, planetRadius + 240f, planetOrbitPeriod);

            if (factionOwner == null || factionOwner.equals("neutral")) {
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
     * Adds entities to a planet's L3, L4, and L5 points, respectively
     *
     * @param planet   Planet
     * @param entityL3 Entity to add at L3 point
     * @param entityL4 Entity to add at L4 point
     * @param entityL5 Entity to add at L5 point
     */
    public static void addToLagrangePoints(PlanetAPI planet, SectorEntityToken entityL3, SectorEntityToken entityL4, SectorEntityToken entityL5) {
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
    public static void generateHyperspace(StarSystemAPI system) {
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
     * Modified from the constellation proc-gen code originally made by Audax.
     *
     * @param system           Star system to relocate
     * @param hyperspaceRadius Radius of star system in hyperspace
     * @param sector           Sector
     * @param setToNearest     Whether to set location to the nearest constellation (to Core Worlds) or to a random constellation
     */
    public static void setLocation(StarSystemAPI system, float hyperspaceRadius, SectorAPI sector, boolean setToNearest) {
        // Get all proc-gen constellations in Sector hyperspace
        LinkedHashSet<Constellation> constellations = new LinkedHashSet<>();
        for (StarSystemAPI sys : sector.getStarSystems()) {
            if (sys.isInConstellation() && sys.isProcgen()) {
                constellations.add(sys.getConstellation());
            }
        }

        // If no constellations exist (for whatever reason), just set location to middle of Core Worlds
        // (you could consider them a special constellation?)
        final Vector2f SECTOR_ORIGIN = new Vector2f(-6000, -6000);
        if (constellations.isEmpty()) {
            system.getLocation().set(SECTOR_ORIGIN);
            return;
        }

        // Select the constellation
        Constellation selectedConstellation = null;
        if (setToNearest) { // Find the nearest constellation to Core Worlds
            float minDistance = Float.MAX_VALUE;
            for (Constellation thisConst : constellations) {
                float distance = Misc.getDistance(SECTOR_ORIGIN, thisConst.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    selectedConstellation = thisConst;
                }
            }
        } else { // Find a random constellation
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
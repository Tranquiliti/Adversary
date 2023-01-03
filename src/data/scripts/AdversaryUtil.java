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
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
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
    // List of star giants that can have 1200 radius and 500 min corona
    private final String[] STAR_GIANT_TYPES = {"star_orange_giant", "star_red_giant", "star_blue_giant", "star_blue_supergiant"};
    public HashMap<MarketAPI, String> marketsToOverrideAdmin; // Is updated in the addMarket private helper method

    // Making a utility class instantiable just so I can modify admins properly D:
    public AdversaryUtil() {
        marketsToOverrideAdmin = new HashMap<>();
    }

    /**
     * Adds a system feature in a system
     *
     * @param system           System to modify
     * @param numOfCenterStars Number of stars in the center of the system
     * @param featureOptions   Options for a feature
     * @throws JSONException If featureOptions is invalid
     */
    public void addSystemFeature(StarSystemAPI system, int numOfCenterStars, JSONObject featureOptions) throws JSONException {
        int focusIndex = numOfCenterStars + featureOptions.getInt("focus");
        SectorEntityToken focus = (focusIndex == numOfCenterStars) ? system.getCenter() : system.getPlanets().get(focusIndex - 1);
        float orbitRadius = featureOptions.getInt("orbitRadius");
        switch (featureOptions.getString("type")) {
            case "asteroid_belt":
                addAsteroidBelt(system, focus, orbitRadius);
                break;
            case "ring_band":
                addRingBand(system, focus, orbitRadius);
                break;
            case "magnetic_field":
                addMagneticField(system, focus, orbitRadius);
                break;
            case "stable_location":
                system.addCustomEntity(null, null, "stable_location", null).setCircularOrbitWithSpin(focus, StarSystemGenerator.random.nextFloat() * 360f, orbitRadius, orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f), 1f, 11f);
                break;
            case "inactive_gate":
                system.addCustomEntity(null, null, "inactive_gate", null).setCircularOrbit(focus, StarSystemGenerator.random.nextFloat() * 360f, orbitRadius, orbitRadius / (10f + StarSystemGenerator.random.nextFloat() * 5f));
                break;
            case "comm_relay":
            case "comm_relay_makeshift":
            case "nav_buoy":
            case "nav_buoy_makeshift":
            case "sensor_array":
            case "sensor_array_makeshift":
                addObjective(system, featureOptions.getString("type"), featureOptions.getString("factionId")).setCircularOrbitWithSpin(focus, StarSystemGenerator.random.nextFloat() * 360f, orbitRadius, orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f), 1f, 11f);
                break;
            case "jump_point":
                addJumpPoint(system, focus, featureOptions.getString("name"), orbitRadius);
                break;
            default: // Any salvage entities defined in salvage_entity_gen_data.csv
                addSalvageEntity(system, featureOptions.getString("type"), focus, orbitRadius);
                break;
        }
    }

    /**
     * Adds an orbiting jump-point in a system
     *
     * @param system      System to modify
     * @param focus       Entity to orbit around
     * @param name        Name of the jump-point
     * @param orbitRadius How far away jump-point is from focus
     */
    public void addJumpPoint(StarSystemAPI system, SectorEntityToken focus, String name, float orbitRadius) {
        JumpPointAPI fringePoint = Global.getFactory().createJumpPoint(null, name);
        fringePoint.setCircularOrbit(focus, StarSystemGenerator.random.nextFloat() * 360f, orbitRadius, orbitRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f));
        fringePoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(fringePoint);
    }

    /**
     * Adds an orbiting salvage entity in a system
     *
     * @param system      System to modify
     * @param type        Type of salvage entity
     * @param focus       Entity to orbit around
     * @param orbitRadius How far away the salvage entity orbits the focus
     * @return The newly-created salvage entity
     */
    public SectorEntityToken addSalvageEntity(StarSystemAPI system, String type, SectorEntityToken focus, float orbitRadius) {
        SalvageEntityGenDataSpec salvageData = (SalvageEntityGenDataSpec) Global.getSettings().getSpec(SalvageEntityGenDataSpec.class, type, true);
        Random randomSeed = StarSystemGenerator.random;

        SectorEntityToken salvageEntity = system.addCustomEntity(null, null, type, null);
        salvageEntity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());
        salvageEntity.setSensorProfile(1f);
        salvageEntity.setDiscoverable(true);
        salvageEntity.getDetectedRangeMod().modifyFlat("gen", salvageData.getDetectionRange());
        salvageEntity.setCircularOrbitWithSpin(focus, randomSeed.nextFloat() * 360f, orbitRadius, orbitRadius / (15f + randomSeed.nextFloat() * 5f), 1f, 11f);

        return salvageEntity;
    }

    /**
     * Adds an asteroid belt around a planet (note that stars are technically planets too)
     *
     * @param focus       The focus
     * @param orbitRadius How far it is located from center of system
     */
    public void addAsteroidBelt(StarSystemAPI system, SectorEntityToken focus, float orbitRadius) {
        // "Nemo's Band" Corvus asteroid belt
        system.addAsteroidBelt(focus, Math.round(orbitRadius / 60), orbitRadius, 256f, Math.round(orbitRadius / 38f), Math.round(orbitRadius / 19f), Terrain.ASTEROID_BELT, null);
        system.addRingBand(focus, "misc", "rings_dust0", 256f, 3, Color.white, 256f, orbitRadius - 60f, Math.round(orbitRadius / 18f), null, null);
        system.addRingBand(focus, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, orbitRadius + 60f, Math.round(orbitRadius / 19.5f), null, null);
    }

    /**
     * Adds a ring band around a planet
     *
     * @param focus       The focus
     * @param orbitRadius How far it is located from center of system
     */
    public void addRingBand(StarSystemAPI system, SectorEntityToken focus, float orbitRadius) {
        // Barad ring band
        system.addRingBand(focus, "misc", "rings_ice0", 256f, 2, Color.white, 256f, orbitRadius, Math.round(orbitRadius / 23f), Terrain.RING, null);
    }

    /**
     * Adds a magnetic field around a planet
     *
     * @param focus The focus
     */
    public void addMagneticField(StarSystemAPI system, SectorEntityToken focus, float orbitRadius) {
        // TODO: need to figure out how magnetic field radius params work so I can properly configure its "orbitRadius"
        // Barad magnetic field
        float planetRadius = focus.getRadius();
        system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(planetRadius + 200f, // terrain effect band width
                orbitRadius, //(planetRadius + 200f) / 2f, // terrain effect middle radius
                focus, // entity that it's around
                planetRadius + 50f, // visual band start
                planetRadius + 50f + 250f, // visual band end
                new Color(50, 20, 100, 40), // base color
                0.5f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                new Color(140, 100, 235), new Color(180, 110, 210), new Color(150, 140, 190), new Color(140, 190, 210), new Color(90, 200, 170), new Color(65, 230, 160), new Color(20, 220, 70))).setCircularOrbit(focus, 0, 0, 100f);
    }

    /**
     * <p>Adds a Domain-era cryosleeper in a star system</p>
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator's
     * addCryosleeper() for vanilla implementation</p>
     *
     * @param system       Star system to modify
     * @param orbitRadius  How far cryosleeper is located from center of system
     * @param discoverable Whether cryosleeper needs to be discovered before being revealed in map
     */
    public void addCryosleeper(StarSystemAPI system, String name, float orbitRadius, boolean discoverable) {
        Random randomSeed = StarSystemGenerator.random;
        SectorEntityToken cryosleeper = system.addCustomEntity(null, name, "derelict_cryosleeper", "derelict");
        cryosleeper.setCircularOrbitWithSpin(system.getCenter(), randomSeed.nextFloat() * 360f, orbitRadius, orbitRadius / (10f + randomSeed.nextFloat() * 5f), 1f, 3f);
        cryosleeper.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

        if (discoverable) {
            cryosleeper.setSensorProfile(1f);
            cryosleeper.setDiscoverable(true);
            cryosleeper.getDetectedRangeMod().modifyFlat("gen", 3500f);
        }

        system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
    }

    /**
     * <p>Adds a coronal hypershunt in a star system.</p>
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator's
     * addCoronalTaps() for vanilla implementation</p>
     *
     * @param system             Star system to modify
     * @param discoverable       Whether hypershunt needs to be discovered before being revealed in map
     * @param hasParticleEffects Whether the hypershunt should emit particle effects upon activation; set to false for better performance
     */
    public void addHypershunt(StarSystemAPI system, boolean discoverable, boolean hasParticleEffects) {
        SectorEntityToken systemCenter = system.getCenter();
        SectorEntityToken hypershunt = system.addCustomEntity(null, null, "coronal_tap", null);
        if (systemCenter.isStar()) { // Orbit the sole star
            float coronalOrbitRadius = systemCenter.getRadius() + hypershunt.getRadius() + 100f;
            hypershunt.setCircularOrbitPointingDown(systemCenter, StarSystemGenerator.random.nextFloat() * 360f, coronalOrbitRadius, coronalOrbitRadius / 20f);
        } else { // Stay in the center, facing towards the primary star
            PlanetAPI primaryStar = system.getStar();
            hypershunt.setCircularOrbitPointingDown(primaryStar, (primaryStar.getCircularOrbitAngle() - 180f) % 360f, primaryStar.getCircularOrbitRadius(), primaryStar.getCircularOrbitPeriod());
        }

        if (discoverable) {
            hypershunt.setSensorProfile(1f);
            hypershunt.setDiscoverable(true);
            hypershunt.getDetectedRangeMod().modifyFlat("gen", 3500f);
        }

        if (hasParticleEffects) {
            system.addScript(new CoronalTapParticleScript(hypershunt));
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
     * <p>Adds stars in the center of a system</p>
     * <p>Look in  com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator's
     * addStars() for vanilla implementation</p>
     *
     * @param system         The system to modify
     * @param systemSettings System settings
     * @throws JSONException If systemSettings has invalid format
     */
    public void addStarsInCenter(StarSystemAPI system, JSONObject systemSettings) throws JSONException {
        // TODO: find way to fix issue where only the primary star gets displayed on system UI menu (apparently, the Tia-Ta'xet Core World system has this problem too)
        JSONArray starList = systemSettings.getJSONArray("stars");
        int numOfCenterStars = starList.length();
        String id = Misc.genUID();

        if (numOfCenterStars == 1) { // Only one star to create
            JSONObject starSettings = starList.getJSONObject(0);
            system.setCenter(addStar(system, null, "system_" + id, starSettings.getString("type"), starSettings.getInt("radius"), starSettings.getInt("coronaSize")));
        } else { // Multiple stars
            SectorEntityToken systemCenter = system.initNonStarCenter(); // Center in which the stars will orbit
            systemCenter.setId(id); // Set the center's id to the unique id

            char starChar = 'b';
            float starsAngle = StarSystemGenerator.random.nextFloat() * 360f;
            float starsAngleDifference = 360f / numOfCenterStars;
            float starsOrbitRadius = systemSettings.getInt("starsOrbitRadius") - numOfCenterStars + 1;
            float starsOrbitDays = starsOrbitRadius / ((60f / numOfCenterStars) + StarSystemGenerator.random.nextFloat() * 50f);
            for (int i = 0; i < numOfCenterStars; i++) {
                JSONObject starOptions = starList.getJSONObject(i);
                PlanetAPI star = addStar(system, null, "system_" + id, starOptions.getString("type"), starOptions.getInt("radius"), starOptions.getInt("coronaSize"));
                if (i != 0) { // Name any stars after the first
                    String starName = getProcGenName("star", system.getName());
                    star.setName(starName);
                    star.setId(star.getId() + "_" + starChar);
                    starChar++;
                    if (i == 1) { // Second star
                        system.setSecondary(star);
                    } else if (i == 2) { // Third star
                        system.setTertiary(star);
                    }
                }
                // Make the first stars a tiny bit closer to center so their gravity wells get generated first
                star.setCircularOrbit(systemCenter, starsAngle, starsOrbitRadius + i, starsOrbitDays);
                starsAngle = (starsAngle + starsAngleDifference) % 360f;
            }
        }
    }

    /**
     * Adds a star in a system; initializes system's star if one does not already exist
     *
     * @param system     The system to modify
     * @param name       Star name; is not used if creating system's first star
     * @param id         Star id
     * @param starType   Star type
     * @param radius     Radius of the star
     * @param coronaSize Size of the star's corona
     * @return The newly-created star
     */
    public PlanetAPI addStar(StarSystemAPI system, String name, String id, String starType, float radius, float coronaSize) {
        if (starType.equals("random_star_giant")) {
            starType = STAR_GIANT_TYPES[StarSystemGenerator.random.nextInt(STAR_GIANT_TYPES.length)];
        }

        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, starType, true);

        PlanetAPI star;
        if (system.getStar() == null) { // Initialize system star
            star = system.initStar(id, starType, radius, coronaSize, starData.getSolarWind(), starData.getMinFlare(), starData.getCrLossMult());
        } else { // Add another star in the system
            star = system.addPlanet(id, null, name, starType, 0f, radius, 10000f, 1000f);
            system.addCorona(star, coronaSize, starData.getSolarWind(), starData.getMinFlare(), starData.getCrLossMult());
        }

        // Add special star hazards if applicable
        if (starType.equals("black_hole") || starType.equals("star_neutron")) {
            StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(star);
            if (coronaPlugin != null) {
                system.removeEntity(coronaPlugin.getEntity());
            }

            String coronaType = starType.equals("black_hole") ? "event_horizon" : "pulsar_beam";
            if (coronaType.equals("pulsar_beam")) {
                system.addCorona(star, 300, 3, 0, 3);
            }
            system.addTerrain(coronaType, new StarCoronaTerrainPlugin.CoronaParams(star.getRadius() + coronaSize, (star.getRadius() + coronaSize) / 2f, star, starData.getSolarWind(), starData.getMinFlare(), starData.getCrLossMult())).setCircularOrbit(star, 0, 0, 100);
        }

        return star;
    }

    /**
     * Adds a planet or orbiting star in a star system
     *
     * @param system        Star system
     * @param id            Planet number
     * @param planetOptions Planet characteristics
     * @return The newly-generated Planet
     * @throws JSONException if planetOptions is invalid or has wrong format
     */
    public PlanetAPI addPlanet(StarSystemAPI system, SectorEntityToken focus, int id, JSONObject planetOptions) throws JSONException {
        Random randomSeed = StarSystemGenerator.random;
        String systemId = system.getCenter().getId();
        if (!systemId.contains("system_")) systemId = "system_" + systemId;
        float orbitRadius = planetOptions.getInt("orbitRadius");

        PlanetAPI newPlanet;
        String planetType = planetOptions.getString("type");
        // TODO: see if there's a better way to check if string is a star type (probably, there isn't)
        if (planetType.contains("star_") || planetType.equals("black_hole")) {
            String starName = getProcGenName("star", system.getName());
            newPlanet = addStar(system, starName, systemId + ":star_" + id, planetType, planetOptions.getInt("radius"), planetOptions.getInt("coronaSize"));
            newPlanet.setCircularOrbit(focus, randomSeed.nextFloat() * 360f, orbitRadius, orbitRadius / (20f + randomSeed.nextFloat() * 5f));
        } else {
            String planetName = getProcGenName("planet", system.getStar().getName());
            newPlanet = system.addPlanet(systemId + ":planet_" + id, focus, planetName, planetType, randomSeed.nextFloat() * 360f, planetOptions.getInt("radius"), orbitRadius, orbitRadius / (20f + randomSeed.nextFloat() * 5f));
            newPlanet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

            if (planetOptions.getInt("marketSize") <= 0) {
                addPlanetConditions(newPlanet, planetOptions);
            } else {
                addPlanetMarket(newPlanet, planetOptions);
            }
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
            // TODO: fix faction not being properly set until colonizing another planet and still needing to pay for storage access (Nex automatically fixes the first issue)
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
     * Adds a solar array near a planet, taking into account planetary conditions
     *
     * @param planet    Planet to modify
     * @param factionId Faction owning the solar array
     */
    public void addSolarArrayToPlanet(PlanetAPI planet, String factionId) {
        int numOfShades = 0;
        int numOfMirrors = 0;
        String planetType = planet.getTypeId();
        String starType = planet.getStarSystem().getStar().getTypeId();
        if (planet.hasCondition("hot") || planetType.equals("desert") || planetType.equals("desert1") || planetType.equals("arid") || starType.equals("star_blue_giant") || starType.equals("star_blue_supergiant")) {
            numOfShades += (StarSystemGenerator.random.nextBoolean() ? 3 : 1);
        }
        if (planet.hasCondition("poor_light") || planetType.equals("terran-eccentric") || starType.equals("star_red_dwarf") || starType.equals("star_brown_dwarf")) {
            numOfMirrors += (StarSystemGenerator.random.nextBoolean() ? 5 : 3);
        }

        if (numOfShades == 0 && numOfMirrors == 0) { // Force a solar array if none of the conditions are met
            addSolarArray(planet, 3, 1, factionId);
        } else {
            addSolarArray(planet, numOfMirrors, numOfShades, factionId);
        }
    }

    /**
     * <p>Adds solar array entities near a planet</p>
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator's
     * addSolarShadesAndMirrors() for vanilla implementation</p>
     *
     * @param planet       Planet to modify
     * @param numOfMirrors Number of solar mirrors
     * @param numOfShades  Number of solar shades
     * @param factionId    Faction owning the solar array
     * @throws IllegalArgumentException if numOfMirrors > 5 or numOfShades > 3
     */
    public void addSolarArray(PlanetAPI planet, int numOfMirrors, int numOfShades, String factionId) {
        if (numOfMirrors > 5 || numOfShades > 3) {
            throw new IllegalArgumentException("Invalid number of solar mirrors and/or shades");
        }

        StarSystemAPI system = planet.getStarSystem();
        float radius = 270f + planet.getRadius();
        float planetOrbitPeriod = planet.getCircularOrbitPeriod();

        // Create solar mirrors
        String[] mirrorNames = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon"};
        float mirrorAngle = planet.getCircularOrbitAngle() - 30f * (numOfMirrors >>> 1);
        int mirrorIndex = 2 - (numOfMirrors / 2);
        for (int i = 0; i < numOfMirrors; i++) {
            SectorEntityToken mirror = system.addCustomEntity(null, "Stellar Mirror " + mirrorNames[mirrorIndex], "stellar_mirror", factionId);
            mirror.setCircularOrbitPointingDown(planet, mirrorAngle, radius, planetOrbitPeriod);

            if (factionId == null || factionId.equals("neutral")) {
                mirror.setDiscoverable(true);
                mirror.setDiscoveryXP(300f);
                mirror.setSensorProfile(2000f);
            }

            mirrorIndex++;
            mirrorAngle += 30f;
        }

        // Create solar shades
        String[] shadeNames = {"Omega", "Psi", "Chi"};
        float shadeAngle = ((planet.getCircularOrbitAngle() + 180f) % 360f) - 26f * (numOfShades >>> 1);
        int shadeIndex = 1 - (numOfShades / 2);
        for (int i = 0; i < numOfShades; i++) {
            SectorEntityToken shade = system.addCustomEntity(null, "Stellar Shade " + shadeNames[shadeIndex], "stellar_shade", factionId);
            shade.setCircularOrbitPointingDown(planet, shadeAngle, radius + ((numOfShades == 3 && (i % 2) == 0) ? -10 : 25), planetOrbitPeriod);

            if (factionId == null || factionId.equals("neutral")) {
                shade.setDiscoverable(true);
                shade.setDiscoveryXP(300f);
                shade.setSensorProfile(2000f);
            }

            shadeAngle += 26f;
            shadeIndex++;
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
        SectorEntityToken systemCenter = planet.getStarSystem().getCenter();
        float planetAngle = planet.getCircularOrbitAngle();
        float planetOrbitRadius = planet.getCircularOrbitRadius();
        float planetOrbitPeriod = planet.getCircularOrbitPeriod();
        if (entityL3 != null) {
            entityL3.setCircularOrbitPointingDown(systemCenter, planetAngle - 180f, planetOrbitRadius, planetOrbitPeriod);
        }
        if (entityL4 != null) {
            entityL4.setCircularOrbitPointingDown(systemCenter, planetAngle + 60f, planetOrbitRadius, planetOrbitPeriod);
        }
        if (entityL5 != null) {
            entityL5.setCircularOrbitPointingDown(systemCenter, planetAngle - 60f, planetOrbitRadius, planetOrbitPeriod);
        }
    }


    /**
     * Sets a system's light color based on its existing stars
     *
     * @param system           The system to modify
     * @param numOfCenterStars Number of stars in the center of the system
     */
    public void setDefaultLightColorBasedOnStars(StarSystemAPI system, int numOfCenterStars) {
        Random randomSeed = StarSystemGenerator.random;
        Color result = Color.WHITE;
        List<PlanetAPI> starList = system.getPlanets();
        for (int i = 0; i < numOfCenterStars; i++) {
            if (i != 0) {
                result = Misc.interpolateColor(result, pickLightColorForStar(starList.get(i), randomSeed), 0.5f);
            } else { // Set result to color of first star
                result = pickLightColorForStar(starList.get(i), randomSeed);
            }
        }
        system.setLightColor(result); // light color in entire system, affects all entities
    }

    // Gets star's light color based on it's specs
    private Color pickLightColorForStar(PlanetAPI star, Random randomSeed) {
        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), true);
        return Misc.interpolateColor(starData.getLightColorMin(), starData.getLightColorMax(), randomSeed.nextFloat());
    }

    /**
     * Gets a unique proc-gen name
     *
     * @param tag    Which name pool to draw from
     * @param parent What the name should depend on
     * @return A unique proc-gen name
     */
    public String getProcGenName(String tag, String parent) {
        String name = ProcgenUsedNames.pickName(tag, parent, null).nameWithRomanSuffixIfAny;
        ProcgenUsedNames.notifyUsed(name);
        return name;
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
     * @param isRandom         If true, set location to a random constellation; else, set location to the nearest constellation (to Core Worlds)
     */
    public void setLocation(StarSystemAPI system, float hyperspaceRadius, boolean isRandom) {
        SectorAPI sector = Global.getSector();

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
            int currentIndex = 0;
            int indexToStop = StarSystemGenerator.random.nextInt(constellations.size());
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
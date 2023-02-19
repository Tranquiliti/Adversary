package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CoronalTapParticleScript;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.*;
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
    // Default values
    public final int DEFAULT_FOCUS = 0;
    public final int DEFAULT_SET_TO_PROC_GEN = -1; // For user's sake, exampleSettings.json uses 0 to specify proc-gen
    public final int DEFAULT_MARKET_SIZE = 0;
    public final int DEFAULT_STARS_ORBIT_RADIUS = 2000;
    public final int DEFAULT_FRINGE_ORBIT_RADIUS = 5000;
    public final int DEFAULT_ORBIT_ANGLE = -1;
    public final int DEFAULT_ORBIT_DAYS = -1;
    public final String DEFAULT_STAR_TYPE = "star_red_dwarf";
    public final String DEFAULT_PLANET_TYPE = "barren";
    public HashMap<MarketAPI, String> marketsToOverrideAdmin; // Is updated in the addMarket private helper method

    // List of proc-gen constellations, filled in during the first setLocation() call
    // Is an ArrayList to more easily get constellations by index
    private ArrayList<Constellation> procGenConstellations;

    // List of all vanilla star giants
    private final String[] STAR_GIANT_TYPES = {"star_orange_giant", "star_red_giant", "star_red_supergiant", "star_blue_giant", "star_blue_supergiant"};

    private final Vector2f CORE_WORLD_CENTER = new Vector2f(-6000, -6000);

    // Making a utility class instantiable just so I can modify admins properly D:
    public AdversaryUtil() {
        marketsToOverrideAdmin = new HashMap<>();
    }

    /**
     * Adds system features to a planet's lagrange points; attempts to add a custom entity if handling unsupported types
     *
     * @param planet         The planet to modify
     * @param lagrangePoints List of JSONObjects representing system features
     * @throws JSONException If lagrangePoints is invalid
     */
    public void addToLagrangePoints(PlanetAPI planet, JSONArray lagrangePoints) throws JSONException {
        int length = lagrangePoints.length();
        if (length >= 3) addSystemFeatureToLagrangePoint(planet, lagrangePoints.getJSONObject(2), 5);
        if (length >= 2) addSystemFeatureToLagrangePoint(planet, lagrangePoints.getJSONObject(1), 4);
        if (length >= 1) addSystemFeatureToLagrangePoint(planet, lagrangePoints.getJSONObject(0), 3);
    }

    // Adds a system feature to a specific lagrange point of a planet
    private void addSystemFeatureToLagrangePoint(PlanetAPI planet, JSONObject featureOptions, int lagrangePoint) throws JSONException {
        String type = featureOptions.isNull("type") ? null : featureOptions.getString("type");
        if (type == null) return; // Lagrange point should remain empty
        float lagrangeAngle = planet.getCircularOrbitAngle();
        switch (lagrangePoint) {
            case 3:
                lagrangeAngle -= 180f;
                break;
            case 4:
                lagrangeAngle += 60f;
                break;
            case 5:
                lagrangeAngle -= 60f;
                break;
        }

        SectorEntityToken entity;
        StarSystemAPI system = planet.getStarSystem();
        String name = featureOptions.isNull("name") ? null : featureOptions.getString("name");
        switch (type) {
            case "asteroid_field":
                entity = addAsteroidField(system, name, featureOptions.isNull("size") ? 400 : featureOptions.getInt("size"));
                break;
            case "remnant_station":
                entity = addRemnantStation(system, !featureOptions.isNull("isDamaged") && featureOptions.getBoolean("isDamaged"));
                break;
            case "station":
                entity = addStation(system, name, featureOptions);
                break;
            case "inactive_gate":
            case "stable_location":
                entity = system.addCustomEntity(null, name, type, null);
                break;
            case "jump_point":
                entity = addJumpPoint(system, name);
                break;
            case "comm_relay":
            case "comm_relay_makeshift":
            case "nav_buoy":
            case "nav_buoy_makeshift":
            case "sensor_array":
            case "sensor_array_makeshift":
                entity = addObjective(system, name, type, featureOptions.isNull("factionId") ? null : featureOptions.getString("factionId"));
                break;
            default: // Default option in case of mods adding their own system entities
                entity = system.addCustomEntity(null, name, type, featureOptions.isNull("factionId") ? null : featureOptions.getString("factionId"));
                break;
        }
        if (entity == null) throw new RuntimeException("Invalid entity " + type + " for stable point!");
        entity.setCircularOrbitPointingDown(planet.getOrbitFocus(), lagrangeAngle, planet.getCircularOrbitRadius(), planet.getCircularOrbitPeriod());
    }

    /**
     * Adds an orbiting system feature in a system; attempts to add a salvage entity if handling unsupported types
     *
     * @param system           System to modify
     * @param numOfCenterStars Number of stars in the center of the system
     * @param featureOptions   Options for a feature
     * @throws JSONException If featureOptions is invalid
     */
    public void addOrbitingSystemFeature(StarSystemAPI system, int numOfCenterStars, JSONObject featureOptions) throws JSONException {
        int focusIndex = numOfCenterStars + (featureOptions.isNull("focus") ? DEFAULT_FOCUS : featureOptions.getInt("focus"));
        SectorEntityToken focus = (focusIndex == numOfCenterStars) ? system.getCenter() : system.getPlanets().get(focusIndex - 1);
        String type = featureOptions.getString("type");
        String name = featureOptions.isNull("name") ? null : featureOptions.getString("name");
        float orbitRadius = featureOptions.isNull("orbitRadius") ? focus.getRadius() + 100f : featureOptions.getInt("orbitRadius");
        float orbitAngle = featureOptions.isNull("orbitAngle") ? DEFAULT_ORBIT_ANGLE : featureOptions.getInt("orbitAngle");
        float orbitDays = featureOptions.isNull("orbitDays") ? DEFAULT_ORBIT_DAYS : featureOptions.getInt("orbitDays");
        switch (type) {
            case "magnetic_field":
                addMagneticField(system, focus, featureOptions.isNull("size") ? 300 : featureOptions.getInt("size"), orbitRadius);
                break;
            case "rings_ice":
            case "rings_dust":
            case "rings_special":
                addRingBand(system, focus, type + '0', orbitRadius, name, featureOptions.isNull("bandIndex") ? 1 : featureOptions.getInt("bandIndex"));
                break;
            case "asteroid_belt":
                addAsteroidBelt(system, focus, orbitRadius, name, featureOptions.isNull("size") ? 256 : featureOptions.getInt("size"), featureOptions.isNull("innerBandIndex") ? 0 : featureOptions.getInt("innerBandIndex"), featureOptions.isNull("outerBandIndex") ? 0 : featureOptions.getInt("outerBandIndex"));
                break;
            case "asteroid_field":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f);
                addAsteroidField(system, name, featureOptions.isNull("size") ? 400 : featureOptions.getInt("size")).setCircularOrbit(focus, orbitAngle, orbitRadius, orbitDays);
                break;
            case "remnant_station":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f);
                addRemnantStation(system, !featureOptions.isNull("isDamaged") && featureOptions.getBoolean("isDamaged")).setCircularOrbitWithSpin(focus, orbitAngle, orbitRadius, orbitDays, 5f, 5f);
                break;
            case "station":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f);
                addStation(system, name, featureOptions).setCircularOrbitPointingDown(focus, orbitAngle, orbitRadius, orbitDays);
                break;
            case "inactive_gate":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (10f + StarSystemGenerator.random.nextFloat() * 5f);
                system.addCustomEntity(null, name, type, null).setCircularOrbit(focus, orbitAngle, orbitRadius, orbitDays);
                break;
            case "stable_location":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f);
                system.addCustomEntity(null, name, type, null).setCircularOrbitWithSpin(focus, orbitAngle, orbitRadius, orbitDays, 1f, 11f);
                break;
            case "jump_point":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f);
                addJumpPoint(system, name).setCircularOrbit(focus, orbitAngle, orbitRadius, orbitDays);
                break;
            case "comm_relay":
            case "comm_relay_makeshift":
            case "nav_buoy":
            case "nav_buoy_makeshift":
            case "sensor_array":
            case "sensor_array_makeshift":
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                if (orbitDays <= 0) orbitDays = orbitRadius / (20f + StarSystemGenerator.random.nextFloat() * 5f);
                addObjective(system, name, type, featureOptions.isNull("factionId") ? null : featureOptions.getString("factionId")).setCircularOrbitWithSpin(focus, orbitAngle, orbitRadius, orbitDays, 1f, 11f);
                break;
            default: // Any salvage entities defined in salvage_entity_gen_data.csv (including ones added by mods)
                if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
                addOrbitingSalvageEntity(system, type, name, focus, orbitAngle, orbitRadius, orbitDays);
                break;
        }
    }

    /**
     * Adds a station in a system
     *
     * @param system         The system to modify
     * @param stationOptions JSONObject representing station options
     * @throws JSONException If stationOptions is invalid
     */
    public SectorEntityToken addStation(StarSystemAPI system, String name, JSONObject stationOptions) throws JSONException {
        SectorEntityToken station = system.addCustomEntity(system.getStar().getId() + ":station_" + Misc.genUID(), name, stationOptions.isNull("stationType") ? "station_side06" : stationOptions.getString("stationType"), stationOptions.isNull("factionId") ? null : stationOptions.getString("factionId"));
        int marketSize = stationOptions.isNull("marketSize") ? DEFAULT_MARKET_SIZE : stationOptions.getInt("marketSize");
        if (marketSize <= 0) Misc.setAbandonedStationMarket(station.getId(), station);
        else addPlanetMarket(station, stationOptions, marketSize);

        return station;
    }

    /**
     * <p>Creates a Remnant battlestation to this system</p>
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator's
     * addBattlestations() for vanilla implementation</p>
     *
     * @param system    The system to modify
     * @param isDamaged Is the Remnant station to create damaged?
     * @return The newly-created station
     */
    public CampaignFleetAPI addRemnantStation(StarSystemAPI system, boolean isDamaged) {
        CampaignFleetAPI station = FleetFactoryV3.createEmptyFleet("remnant", "battlestation", null);

        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, isDamaged ? "remnant_station2_Damaged" : "remnant_station2_Standard");
        station.getFleetData().addFleetMember(member);

        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        station.addTag(Tags.NEUTRINO_HIGH);

        station.setStationMode(true);
        RemnantThemeGenerator.addRemnantStationInteractionConfig(station);
        system.addEntity(station);

        station.clearAbilities();
        station.addAbility("transponder");
        station.getAbility("transponder").activate();
        station.getDetectedRangeMod().modifyFlat("gen", 1000f);

        station.setAI(null);

        Random random = StarSystemGenerator.random;
        PersonAPI commander = Misc.getAICoreOfficerPlugin("alpha_core").createPerson("alpha_core", "remnant", random);

        station.setCommander(commander);
        station.getFlagship().setCaptain(commander);

        system.addTag(Tags.THEME_INTERESTING);
        system.addTag(Tags.THEME_REMNANT);
        system.addTag(Tags.THEME_REMNANT_SECONDARY);
        system.addTag(Tags.THEME_UNSAFE);
        if (isDamaged) {
            station.getMemoryWithoutUpdate().set("$damagedStation", true);
            station.setName(station.getName() + " (Damaged)");

            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 2 + random.nextInt(3), 25f, 6, 12));
            system.addTag(Tags.THEME_REMNANT_SUPPRESSED);
        } else {
            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(station.getFlagship());
            RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, station, null, 3, random);

            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 8 + random.nextInt(5), 15f, 8, 24));
            system.addTag(Tags.THEME_REMNANT_RESURGENT);
        }

        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        return station;
    }

    /**
     * If applicable, adds the appropriate Remnant warning beacon to a system
     *
     * @param system The system to modify
     */
    public void addRemnantWarningBeacons(StarSystemAPI system) {
        if (system.hasTag(Tags.THEME_REMNANT_RESURGENT))
            RemnantThemeGenerator.addBeacon(system, RemnantThemeGenerator.RemnantSystemType.RESURGENT);
        else if (system.hasTag(Tags.THEME_REMNANT_SUPPRESSED))
            RemnantThemeGenerator.addBeacon(system, RemnantThemeGenerator.RemnantSystemType.SUPPRESSED);
        else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED))
            RemnantThemeGenerator.addBeacon(system, RemnantThemeGenerator.RemnantSystemType.DESTROYED);
    }

    /**
     * Adds an asteroid belt around a focus
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.AsteroidBeltGenPlugin's
     * generate() for vanilla implementation</p>
     *
     * @param system         The system to modify
     * @param focus          The focus
     * @param orbitRadius    How far it is located from center of system
     * @param name           Name of the asteroid belt
     * @param width          Width of the asteroid belt
     * @param innerBandIndex The inner belt texture to use
     * @param outerBandIndex The outer belt texture to use
     */
    public void addAsteroidBelt(StarSystemAPI system, SectorEntityToken focus, float orbitRadius, String name, float width, int innerBandIndex, int outerBandIndex) {
        Random randomSeed = StarSystemGenerator.random;
        if (innerBandIndex < 0 || innerBandIndex > 3) innerBandIndex = 0;
        if (outerBandIndex < 0 || outerBandIndex > 3) outerBandIndex = 0;
        float orbitDays = orbitRadius / (15f + 5f * randomSeed.nextFloat());
        int count = (int) (orbitDays * (0.25f + 0.5f * StarSystemGenerator.random.nextFloat()));
        if (count > 100) count = (int) (100f + (count - 100f) * 0.25f);
        if (count > 250) count = 250;
        system.addAsteroidBelt(focus, count, orbitRadius, width, orbitDays * .75f, orbitDays * 1.5f, Terrain.ASTEROID_BELT, name);
        system.addRingBand(focus, "misc", "rings_asteroids0", 256f, innerBandIndex, Color.white, 256f, orbitRadius - width * 0.25f, orbitDays * 1.05f, null, null);
        system.addRingBand(focus, "misc", "rings_asteroids0", 256f, outerBandIndex, Color.white, 256f, orbitRadius + width * 0.25f, orbitDays, null, null);
    }

    /**
     * Adds an asteroid field around a focus
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.AsteroidFieldGenPlugin's
     * generate() for vanilla implementation</p>
     *
     * @param system The system to modify
     * @param name   Name of the asteroid field
     * @param radius Radius of the asteroid field
     * @return The newly-created asteroid field
     */
    public SectorEntityToken addAsteroidField(StarSystemAPI system, String name, float radius) {
        int count = (int) (radius * radius * 3.14f / 80000f);
        if (count < 10) count = 10;
        if (count > 100) count = 100;
        return system.addTerrain(Terrain.ASTEROID_FIELD, new AsteroidFieldTerrainPlugin.AsteroidFieldParams(radius, radius + 100f, count, count, 4f, 16f, name));
    }

    /**
     * Adds a ring band around a focus
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.RingGenPlugin's
     * generate() for vanilla implementation</p>
     *
     * @param system      The system to modify
     * @param focus       The focus
     * @param orbitRadius How far it is located from center of system
     * @param name        Name of the ring band
     * @param bandIndex   The ring band texture to use
     */
    public void addRingBand(StarSystemAPI system, SectorEntityToken focus, String type, float orbitRadius, String name, int bandIndex) {
        if (type.equals("rings_special0") ? bandIndex != 1 : (bandIndex < 0 || bandIndex > 3)) bandIndex = 1;
        system.addRingBand(focus, "misc", type, 256f, bandIndex, Color.white, 256f, orbitRadius, orbitRadius / (15f + 5f * StarSystemGenerator.random.nextFloat()), Terrain.RING, name);
    }

    /**
     * Adds a magnetic field around a focus
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin's
     * generate() for vanilla implementation</p>
     *
     * @param system      The system to modify
     * @param focus       The focus
     * @param width       Width of the magnetic field band
     * @param orbitRadius How far away the magnetic field orbits from focus
     */
    public void addMagneticField(StarSystemAPI system, SectorEntityToken focus, float width, float orbitRadius) {
        system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(width, orbitRadius + width / 2f, focus, orbitRadius, orbitRadius + width, new Color(50, 20, 100, 40), 0.25f + 0.75f * StarSystemGenerator.random.nextFloat(), new Color(140, 100, 235), new Color(180, 110, 210), new Color(150, 140, 190), new Color(140, 190, 210), new Color(90, 200, 170), new Color(65, 230, 160), new Color(20, 220, 70))).setCircularOrbit(focus, 0, 0, 100f);
    }

    /**
     * Adds a jump-point in a system
     *
     * @param system System to modify
     * @param name   Name of the jump-point
     */
    public JumpPointAPI addJumpPoint(StarSystemAPI system, String name) {
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, name);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
        return jumpPoint;
    }

    /**
     * Adds an orbiting salvage entity in a system
     *
     * @param system      System to modify
     * @param type        Type of salvage entity
     * @param focus       Entity to orbit around
     * @param orbitAngle  Initial orbit angle
     * @param orbitRadius How far away the salvage entity orbits the focus
     * @param orbitDays   Time it takes for entity to fully orbit the focus
     */
    public void addOrbitingSalvageEntity(StarSystemAPI system, String type, String name, SectorEntityToken focus, float orbitAngle, float orbitRadius, float orbitDays) {
        SalvageEntityGenDataSpec salvageData = (SalvageEntityGenDataSpec) Global.getSettings().getSpec(SalvageEntityGenDataSpec.class, type, true);
        if (salvageData == null) throw new RuntimeException("Salvage entity " + type + " not found!");

        Random randomSeed = StarSystemGenerator.random;
        SectorEntityToken salvageEntity = system.addCustomEntity(null, name, type, null);
        salvageEntity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());
        salvageEntity.setSensorProfile(1f);
        salvageEntity.setDiscoverable(true);
        salvageEntity.getDetectedRangeMod().modifyFlat("gen", salvageData.getDetectionRange());

        // Set proper attributes for certain entities
        switch (type) {
            case "coronal_tap":
                system.addTag(Tags.HAS_CORONAL_TAP);
                system.addTag(Tags.THEME_INTERESTING);
                if (orbitDays <= 0) orbitDays = orbitRadius / 20f;
                salvageEntity.setCircularOrbitPointingDown(focus, orbitAngle, orbitRadius, orbitDays);
                break;
            case "derelict_cryosleeper":
                salvageEntity.setFaction("derelict");
                system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
                system.addTag(Tags.THEME_INTERESTING);
                // Continue to default
            default:
                if (orbitDays <= 0) orbitDays = orbitRadius / (15f + randomSeed.nextFloat() * 5f);
                salvageEntity.setCircularOrbitWithSpin(focus, orbitAngle, orbitRadius, orbitDays, 1f, 11f);
                break;
        }
    }

    /**
     * Adds a system objective in a star system
     *
     * @param system      Star system to modify
     * @param objectiveId System objective id; should either be "comm_relay", "sensor_array", or "nav_buoy"
     * @param factionId   Faction owning the system objective
     * @return The newly-created system objective
     */
    public SectorEntityToken addObjective(StarSystemAPI system, String name, String objectiveId, String factionId) {
        SectorEntityToken objective = system.addCustomEntity(null, name, objectiveId, factionId);
        if (factionId == null || factionId.equals("neutral"))
            objective.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);

        return objective;
    }

    /**
     * Adds a fringe jump-point in a system
     *
     * @param system      System to modify
     * @param jumpOptions Jump-point options
     * @return The orbit radius of the newly-created jump-point
     * @throws JSONException If jumpOptions is invalid
     */
    public float addFringeJumpPoint(StarSystemAPI system, JSONObject jumpOptions) throws JSONException {
        if (jumpOptions == null) { // Create default jump-point
            addJumpPoint(system, "Fringe Jump-point").setCircularOrbit(system.getCenter(), StarSystemGenerator.random.nextFloat() * 360f, DEFAULT_FRINGE_ORBIT_RADIUS, DEFAULT_FRINGE_ORBIT_RADIUS / (15f + StarSystemGenerator.random.nextFloat() * 5f));
            return DEFAULT_FRINGE_ORBIT_RADIUS;
        }

        float orbitRadius = jumpOptions.isNull("orbitRadius") ? DEFAULT_FRINGE_ORBIT_RADIUS : jumpOptions.getInt("orbitRadius");
        float orbitAngle = jumpOptions.isNull("orbitAngle") ? DEFAULT_ORBIT_ANGLE : jumpOptions.getInt("orbitAngle");
        if (orbitAngle < 0) orbitAngle = StarSystemGenerator.random.nextFloat() * 360f;
        float orbitDays = jumpOptions.isNull("orbitDays") ? DEFAULT_ORBIT_DAYS : jumpOptions.getInt("orbitDays");
        if (orbitDays <= 0) orbitDays = orbitRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f);

        addJumpPoint(system, jumpOptions.isNull("name") ? "Fringe Jump-point" : jumpOptions.getString("name")).setCircularOrbit(system.getCenter(), orbitAngle, orbitRadius, orbitDays);
        return orbitRadius;
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
        cryosleeper.setCircularOrbitWithSpin(system.getCenter(), randomSeed.nextFloat() * 360f, orbitRadius, orbitRadius / (15f + randomSeed.nextFloat() * 5f), 1f, 11);
        cryosleeper.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

        if (discoverable) {
            cryosleeper.setSensorProfile(1f);
            cryosleeper.setDiscoverable(true);
            cryosleeper.getDetectedRangeMod().modifyFlat("gen", 3500f);
        }

        system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
        system.addTag(Tags.THEME_INTERESTING);
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

        if (hasParticleEffects) system.addScript(new CoronalTapParticleScript(hypershunt));

        system.addTag(Tags.HAS_CORONAL_TAP);
        system.addTag(Tags.THEME_INTERESTING);
    }

    // Credit to theDragn#0580 for publishing the original code on #advanced-modmaking in the Unofficial Starsector Discord
    // (It was apparently taken from the vanilla implementation in the source API, but I couldn't find exactly where in the API)
    private static void addAccretionDisk(PlanetAPI star) {
        StarSystemAPI system = star.getStarSystem();
        Random randomSeed = StarSystemGenerator.random;
        float orbitRadius = star.getRadius() * (10f + randomSeed.nextFloat() * 5f);
        float bandWidth = 256f;
        int numBands = 12 + randomSeed.nextInt(7);
        for (int i = 0; i < numBands; i++) {
            float radius = orbitRadius - (i * bandWidth * 0.25f) - (i * bandWidth * 0.1f);
            RingBandAPI visual = system.addRingBand(star, "misc", "rings_ice0", 256f, 0, new Color(46, 35, 173), bandWidth, radius + bandWidth / 2f, -(radius / (30f + 10f * randomSeed.nextFloat())));
            visual.setSpiral(true);
            visual.setMinSpiralRadius(0);
            visual.setSpiralFactor(2f + randomSeed.nextFloat() * 5f);
        }

        SectorEntityToken ring = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(orbitRadius, orbitRadius / 2f, star, "Accretion Disk"));
        ring.addTag(Tags.ACCRETION_DISK);
        ring.setCircularOrbit(star, 0, 0, -100);
    }

    /**
     * <p>Adds stars in the center of a system</p>
     * <p>Look in  com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator's
     * addStars() for vanilla implementation</p>
     *
     * @param system        The system to modify
     * @param centerOptions Options for the center stars
     * @throws JSONException If any of the stars have an invalid format
     */
    public void addStarsInCenter(StarSystemAPI system, JSONObject centerOptions) throws JSONException {
        JSONArray starsList = centerOptions.getJSONArray("stars");
        int numOfCenterStars = starsList.length();
        String id = Misc.genUID();

        if (numOfCenterStars == 1) { // Only one star to create
            JSONObject starOptions = starsList.getJSONObject(0);
            String type = starOptions.isNull("type") ? DEFAULT_STAR_TYPE : starOptions.getString("type");
            system.setCenter(addStar(system, starOptions.isNull("name") ? null : starOptions.getString("name"), "system_" + id, type, starOptions.isNull("radius") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("radius"), starOptions.isNull("coronaRadius") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("coronaRadius"), starOptions.isNull("flareChance") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("flareChance")));

            // Adds an accretion disk if only one center black hole
            if (type.equals("black_hole")) addAccretionDisk(system.getStar());
        } else { // Multiple stars
            SectorEntityToken systemCenter = system.initNonStarCenter(); // Center in which the stars will orbit
            systemCenter.setId(id); // Set the center's id to the unique id

            float orbitRadius = centerOptions.isNull("orbitRadius") ? DEFAULT_STARS_ORBIT_RADIUS : centerOptions.getInt("orbitRadius");
            if (orbitRadius <= 0) orbitRadius = DEFAULT_STARS_ORBIT_RADIUS;

            float starsOrbitRadius = orbitRadius - numOfCenterStars + 1;
            float starsAngle = centerOptions.isNull("orbitAngle") ? StarSystemGenerator.random.nextFloat() * 360f : centerOptions.getInt("orbitAngle");
            float starsAngleDifference = 360f / numOfCenterStars;
            float starsOrbitDays = centerOptions.isNull("orbitDays") ? starsOrbitRadius / ((60f / numOfCenterStars) + StarSystemGenerator.random.nextFloat() * 50f) : centerOptions.getInt("orbitDays");

            char starChar = 'b';
            for (int i = 0; i < numOfCenterStars; i++) {
                JSONObject starOptions = starsList.getJSONObject(i);
                PlanetAPI star = addStar(system, starOptions.isNull("name") ? null : starOptions.getString("name"), "system_" + id, starOptions.isNull("type") ? DEFAULT_STAR_TYPE : starOptions.getString("type"), starOptions.isNull("radius") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("radius"), starOptions.isNull("coronaRadius") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("coronaRadius"), starOptions.isNull("flareChance") ? DEFAULT_SET_TO_PROC_GEN : starOptions.getInt("flareChance"));
                if (i != 0) {
                    star.setId(star.getId() + "_" + starChar);
                    starChar++;

                    if (i == 1) { // Second star in center
                        system.setSecondary(star);
                        system.setType(StarSystemGenerator.StarSystemType.BINARY_CLOSE);
                    } else if (i == 2) { // Third star in center
                        system.setTertiary(star);
                        system.setType(StarSystemGenerator.StarSystemType.TRINARY_2CLOSE);
                    }
                }
                // Make the first stars a tiny bit closer to center so their gravity wells get generated first
                star.setCircularOrbit(systemCenter, starsAngle, starsOrbitRadius + i, starsOrbitDays);
                starsAngle = (starsAngle + starsAngleDifference) % 360f;
            }
        }
    }

    /**
     * Adds a planet or star with specified JSON options
     *
     * @param system           The system to modify
     * @param numOfCenterStars Number of stars in center of system
     * @param planetOptions    JSONObject representing planet or star options
     * @param index            Index of this planet or star
     * @return The newly-created planet or star
     * @throws JSONException if planetOptions is invalid
     */
    public PlanetAPI addPlanetWithOptions(StarSystemAPI system, int numOfCenterStars, JSONObject planetOptions, int index) throws JSONException {
        // Creates planet with appropriate characteristics
        int indexFocus = planetOptions.isNull("focus") ? DEFAULT_FOCUS : planetOptions.getInt("focus");
        if (numOfCenterStars + indexFocus > system.getPlanets().size())
            throw new RuntimeException("Invalid \"focus\" index in " + system.getBaseName() + "'s \"orbitingBodies\" entry #" + (index + 1));
        PlanetAPI newPlanet = addPlanet(system, (indexFocus <= 0) ? system.getCenter() : system.getPlanets().get(numOfCenterStars + indexFocus - 1), index, planetOptions);

        if (newPlanet.isStar()) { // New "planet" is an orbiting star
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
        } else if (newPlanet.hasCondition("solar_array"))
            addSolarArrayToPlanet(newPlanet, newPlanet.getFaction().getId());

        // Adds any entities to this planet's lagrange points if applicable
        JSONArray lagrangePoints = planetOptions.isNull("entitiesAtStablePoints") ? null : planetOptions.getJSONArray("entitiesAtStablePoints");
        if (lagrangePoints != null) addToLagrangePoints(newPlanet, lagrangePoints);

        return newPlanet;
    }

    /**
     * Adds a star in a system; initializes system's star if one does not already exist
     *
     * @param system       The system to modify
     * @param name         Star name; is not used if creating system's first star
     * @param id           Star id
     * @param starType     Star type
     * @param radius       Radius of the star
     * @param coronaRadius Size of the star's corona
     * @param flareChance  Chance for flares to appear in star's corona
     * @return The newly-created star
     */
    public PlanetAPI addStar(StarSystemAPI system, String name, String id, String starType, float radius, float coronaRadius, float flareChance) {
        if (starType.equals("random_star_giant"))
            starType = STAR_GIANT_TYPES[StarSystemGenerator.random.nextInt(STAR_GIANT_TYPES.length)];

        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, starType, true);
        if (starData == null) throw new RuntimeException("Star type " + starType + " not found!");

        if (radius <= 0)
            radius = starData.getMinRadius() + (starData.getMaxRadius() - starData.getMinRadius()) * StarSystemGenerator.random.nextFloat();

        if (coronaRadius <= 0)
            coronaRadius = Math.max(starData.getCoronaMin(), radius * (starData.getCoronaMult() + starData.getCoronaVar() * (StarSystemGenerator.random.nextFloat() - 0.5f)));

        if (flareChance < 0)
            flareChance = starData.getMinFlare() + (starData.getMaxFlare() - starData.getMinFlare()) * StarSystemGenerator.random.nextFloat();
        else flareChance /= 100f;

        PlanetAPI star;
        if (system.getStar() == null) { // First star in system, so initialize system star
            // Note: Guaranteed to be called during addStarsInCenter()
            star = system.initStar(id, starType, radius, coronaRadius, starData.getSolarWind(), flareChance, starData.getCrLossMult());
        } else { // Add another star in the system; will have to set appropriate system type elsewhere depending if it will be on center or orbiting the center
            if (name == null) name = getProcGenName("star", system.getBaseName());
            star = system.addPlanet(id, null, name, starType, 0f, radius, 10000f, 1000f);
            system.addCorona(star, coronaRadius, starData.getSolarWind(), flareChance, starData.getCrLossMult());
        }

        // Add special star hazards if applicable
        if (starType.equals("black_hole") || starType.equals("star_neutron")) {
            StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(star);
            if (coronaPlugin != null) system.removeEntity(coronaPlugin.getEntity());

            String coronaType = starType.equals("black_hole") ? "event_horizon" : "pulsar_beam";
            if (coronaType.equals("pulsar_beam")) system.addCorona(star, 300, 3, 0, 3);
            system.addTerrain(coronaType, new StarCoronaTerrainPlugin.CoronaParams(star.getRadius() + coronaRadius, (star.getRadius() + coronaRadius) / 2f, star, starData.getSolarWind(), flareChance, starData.getCrLossMult())).setCircularOrbit(star, 0, 0, 100);
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

        PlanetAPI newPlanet;
        String planetType = planetOptions.isNull("type") ? DEFAULT_PLANET_TYPE : planetOptions.getString("type");
        PlanetGenDataSpec planetData = (PlanetGenDataSpec) Global.getSettings().getSpec(PlanetGenDataSpec.class, planetType, true);

        String name = planetOptions.isNull("name") ? null : planetOptions.getString("name");
        float radius = planetOptions.isNull("radius") ? DEFAULT_SET_TO_PROC_GEN : planetOptions.getInt("radius");
        float orbitAngle = planetOptions.isNull("orbitAngle") ? randomSeed.nextFloat() * 360f : planetOptions.getInt("orbitAngle");
        float orbitRadius = planetOptions.getInt("orbitRadius");
        float orbitDays = planetOptions.isNull("orbitDays") ? orbitRadius / (20f + randomSeed.nextFloat() * 5f) : planetOptions.getInt("orbitDays");

        if (planetData == null) { // Not a planet, so try to generate it as a star
            newPlanet = addStar(system, name, systemId + ":star_" + id, planetType, radius, planetOptions.isNull("coronaRadius") ? DEFAULT_SET_TO_PROC_GEN : planetOptions.getInt("coronaRadius"), planetOptions.isNull("flareChance") ? DEFAULT_SET_TO_PROC_GEN : planetOptions.getInt("flareChance"));
            newPlanet.setCircularOrbit(focus, orbitAngle, orbitRadius, orbitDays);
        } else {
            // Set default radius if applicable
            if (radius <= 0)
                radius = planetData.getMinRadius() + (planetData.getMaxRadius() - planetData.getMinRadius()) * StarSystemGenerator.random.nextFloat();

            if (name == null) name = getProcGenName("planet", system.getBaseName());
            newPlanet = system.addPlanet(systemId + ":planet_" + id, focus, name, planetType, orbitAngle, radius, orbitRadius, orbitDays);
            newPlanet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, randomSeed.nextLong());

            int marketSize = planetOptions.isNull("marketSize") ? DEFAULT_MARKET_SIZE : planetOptions.getInt("marketSize");
            if (marketSize <= 0) addPlanetConditions(newPlanet, planetOptions);
            else addPlanetMarket(newPlanet, planetOptions, marketSize);
        }

        return newPlanet;
    }

    // Adds planetary conditions to planet
    private void addPlanetConditions(PlanetAPI planet, JSONObject planetOptions) throws JSONException {
        Misc.initConditionMarket(planet);
        MarketAPI planetMarket = planet.getMarket();
        JSONArray conditions = planetOptions.isNull("conditions") ? null : planetOptions.getJSONArray("conditions");
        if (conditions != null) for (int i = 0; i < conditions.length(); i++)
            try {
                planetMarket.addCondition(conditions.getString(i));
            } catch (Exception e) {
                throw new RuntimeException("Error attempting to add condition \"" + conditions.getString(i) + "\" for \"" + planet.getTypeId() + "\" planet with orbit radius " + Math.round(planet.getCircularOrbitRadius()));
            }
    }

    // Adds a populated market with specified options
    private void addPlanetMarket(SectorEntityToken planet, JSONObject planetOptions, int size) throws JSONException {
        if (size > 10) size = 10;
        // Create planet market
        String factionId = planetOptions.getString("factionId");
        MarketAPI planetMarket = Global.getFactory().createMarket(planet.getId() + "_market", planet.getName(), size);
        planetMarket.setFactionId(factionId);
        planetMarket.setPrimaryEntity(planet);
        planetMarket.getTariff().setBaseValue(0.3f); // Default tariff value
        planetMarket.setFreePort(!planetOptions.isNull("freePort") && planetOptions.getBoolean("freePort"));

        planetMarket.addCondition("population_" + size);
        JSONArray conditions = planetOptions.isNull("conditions") ? null : planetOptions.getJSONArray("conditions");
        if (conditions != null) {
            for (int i = 0; i < conditions.length(); i++) {
                try {
                    planetMarket.addCondition(conditions.getString(i));
                } catch (Exception e) {
                    throw new RuntimeException("Error attempting to add condition \"" + conditions.getString(i) + "\" for Size " + size + " \"" + factionId + "\" market");
                }
            }
        }

        JSONArray industries = planetOptions.isNull("industries") ? null : planetOptions.getJSONArray("industries");
        if (industries != null) {
            for (int i = 0; i < industries.length(); i++) {
                try {
                    planetMarket.addIndustry(industries.getString(i));
                } catch (Exception e) {
                    throw new RuntimeException("Error attempting to add industry \"" + industries.getString(i) + "\" for Size " + size + " \"" + factionId + "\" market");
                }
            }
        } else { // Just give market the bare minimum colony
            planetMarket.addIndustry("population");
            planetMarket.addIndustry("spaceport");
        }

        JSONObject specials = planetOptions.isNull("specials") ? null : planetOptions.getJSONObject("specials");
        if (specials != null) {
            @SuppressWarnings("unchecked") Iterator<String> specialIterator = specials.keys();
            while (specialIterator.hasNext()) {
                String industryId = specialIterator.next();
                Industry thisIndustry = planetMarket.getIndustry(industryId);
                if (thisIndustry != null) {
                    JSONArray items = specials.getJSONArray(industryId);

                    String specialItem = items.getString(0);
                    if (specialItem != null && !specialItem.isEmpty())
                        thisIndustry.setSpecialItem(new SpecialItemData(specialItem, null));

                    String aiCore = items.getString(1);
                    if (aiCore != null && !aiCore.isEmpty()) thisIndustry.setAICoreId(aiCore);
                }
            }
        }

        // Add the appropriate submarkets
        planetMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (factionId.equals("player")) {
            planetMarket.setPlayerOwned(true);
            planetMarket.addSubmarket(Submarkets.LOCAL_RESOURCES);
            ((StoragePlugin) planetMarket.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);
            marketsToOverrideAdmin.put(planetMarket, "player");
        } else {
            planetMarket.addSubmarket(Submarkets.SUBMARKET_OPEN);
            if (planetMarket.hasIndustry("militarybase") || planetMarket.hasIndustry("highcommand"))
                planetMarket.addSubmarket(Submarkets.GENERIC_MILITARY);
            planetMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        }

        // Adds an AI core admin to the market if enabled
        if (!planetOptions.isNull("aiCoreAdmin") && planetOptions.getBoolean("aiCoreAdmin"))
            marketsToOverrideAdmin.put(planetMarket, "alpha_core");

        //set market in global, factions, and assign market, also submarkets
        Global.getSector().getEconomy().addMarket(planetMarket, true);
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
        if (planet.hasCondition("hot") || planetType.equals("desert") || planetType.equals("desert1") || planetType.equals("arid") || starType.equals("star_blue_giant") || starType.equals("star_blue_supergiant"))
            numOfShades += (StarSystemGenerator.random.nextBoolean() ? 3 : 1);

        if (planet.hasCondition("poor_light") || planetType.equals("terran-eccentric") || starType.equals("star_red_dwarf") || starType.equals("star_brown_dwarf"))
            numOfMirrors += (StarSystemGenerator.random.nextBoolean() ? 5 : 3);

        // Force a solar array if none of the above conditions are met
        if (numOfShades == 0 && numOfMirrors == 0) addSolarArray(planet, 3, 1, factionId);
        else addSolarArray(planet, numOfMirrors, numOfShades, factionId);
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
        if (numOfMirrors > 5 || numOfShades > 3)
            throw new IllegalArgumentException("Invalid number of solar mirrors and/or shades");

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
     * Sets a system's light color based on a list of stars
     *
     * @param system      The system to modify
     * @param systemStars The stars the light color will be based on
     */
    public void setDefaultLightColorBasedOnStars(StarSystemAPI system, List<PlanetAPI> systemStars) {
        Random randomSeed = StarSystemGenerator.random;
        Color result = Color.WHITE;
        for (int i = 0; i < systemStars.size(); i++) {
            if (i != 0)
                result = Misc.interpolateColor(result, pickLightColorForStar(systemStars.get(i), randomSeed), 0.5f);
            else result = pickLightColorForStar(systemStars.get(i), randomSeed); // Set color to first star
        }
        system.setLightColor(result); // light color in entire system, affects all entities
    }

    // Gets star's light color based on it's specs
    private Color pickLightColorForStar(PlanetAPI star, Random randomSeed) {
        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), true);
        return Misc.interpolateColor(starData.getLightColorMin(), starData.getLightColorMax(), randomSeed.nextFloat());
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
     * Set a star system's location to near the middle of a specified constellation
     * <p>Modified from the constellation proc-gen code originally made by Audax.</p>
     *
     * @param system           Star system to relocate
     * @param hyperspaceRadius Radius of star system in hyperspace
     * @param index            Index of constellation to set as location; if index <= 0, set system location to a random constellation.
     */
    public void setLocation(StarSystemAPI system, float hyperspaceRadius, int index) {
        // Get all proc-gen constellations in Sector hyperspace, sorted by proximty to the Core Worlds
        // Subsequent setLocation() calls should already have the full constellation list
        if (procGenConstellations == null) {
            // TreeSet orders the constellations by distance to Core Worlds, while also avoiding duplicates
            TreeSet<Constellation> sortedSet = new TreeSet<>(new Comparator<Constellation>() {
                public int compare(Constellation c1, Constellation c2) {
                    if (c1 == c2) return 0;
                    return Float.compare(Misc.getDistance(CORE_WORLD_CENTER, c1.getLocation()), Misc.getDistance(CORE_WORLD_CENTER, c2.getLocation()));
                }
            });
            for (StarSystemAPI sys : Global.getSector().getStarSystems())
                if (sys.isProcgen() && sys.isInConstellation()) sortedSet.add(sys.getConstellation());

            // Make new ArrayList to copy and store the ordered set of proc-gen constellations
            procGenConstellations = new ArrayList<>(sortedSet);
        }

        // If no constellations exist (for whatever reason), just set location to middle of Core Worlds
        // (you could consider them a special constellation?)
        if (procGenConstellations.isEmpty()) {
            system.getLocation().set(CORE_WORLD_CENTER);
            return;
        }

        // Select the constellation
        Constellation selectedConstellation;
        Random randomSeed = StarSystemGenerator.random;
        if (index <= 0) // Set location to a random constellation
            selectedConstellation = procGenConstellations.get(randomSeed.nextInt(procGenConstellations.size()));
        else // Set location to a specified constellation
            selectedConstellation = procGenConstellations.get(Math.min(index, procGenConstellations.size()) - 1);

        // Get centroid point of the selected constellation
        float centroidX = 0;
        float centroidY = 0;
        List<StarSystemAPI> nearestSystems = selectedConstellation.getSystems();
        for (StarSystemAPI sys : nearestSystems) {
            Vector2f loc = sys.getHyperspaceAnchor().getLocationInHyperspace();
            centroidX += loc.getX();
            centroidY += loc.getY();
        }
        centroidX /= nearestSystems.size();
        centroidY /= nearestSystems.size();

        // Nudge the centroid point to a nearby random location
        centroidX += (randomSeed.nextBoolean() ? 1 : -1) * randomSeed.nextFloat() * 2000f;
        centroidY += (randomSeed.nextBoolean() ? 1 : -1) * randomSeed.nextFloat() * 2000f;

        // Find an empty spot in the constellation, starting at the centroid point and
        // then searching for locations around it in a square pattern
        Vector2f newLoc = null;
        int curX = 0;
        int curY = 0;
        int squareSize = 0;
        byte move = 3; // 0 = left, 1 = down; 2 = right; 3 = up
        while (newLoc == null) {
            float thisX = curX * 25f + centroidX;
            float thisY = curY * 25f + centroidY;
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

            // Found an empty location
            if (!intersects) newLoc = new Vector2f(thisX, thisY);
                // Else pick the next location to check
            else if (move == 0) { // moving left
                if (curX != -squareSize) curX--;
                else {
                    move = 1;
                    curY--;
                }
            } else if (move == 1) { // moving down
                if (curY != -squareSize) curY--;
                else {
                    move = 2;
                    curX++;
                }
            } else if (move == 2) { // moving right
                if (curX != squareSize) curX++;
                else {
                    move = 3;
                    curY++;
                }
            } else { // moving up
                if (curY != squareSize) curY++;
                else { // Checked the full perimeter, so increase search size
                    squareSize++;
                    curX = squareSize - 1;
                    curY = squareSize;
                    move = 0;
                }
            }
        }

        // Generate system as part of the selected constellation
        nearestSystems.add(system); // Selected constellation now contains the new system
        system.setConstellation(selectedConstellation);
        system.getLocation().set(newLoc);
    }
}
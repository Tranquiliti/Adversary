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
    public final int DEFAULT_FRINGE_ORBIT_RADIUS = 5000;
    public final String DEFAULT_STAR_TYPE = "star_red_dwarf";
    public final String DEFAULT_PLANET_TYPE = "barren";
    public transient HashMap<MarketAPI, String> marketsToOverrideAdmin; // Is updated in the addMarket private helper method

    // List of proc-gen constellations, filled in during the first setLocation() call
    // Is an ArrayList to more easily get constellations by index
    private transient ArrayList<Constellation> procGenConstellations;

    // List of all vanilla star giants, for "random_star_giant" type
    private final String[] STAR_GIANT_TYPES = {"star_orange_giant", "star_red_giant", "star_red_supergiant", "star_blue_giant", "star_blue_supergiant"};

    private final Vector2f CORE_WORLD_CENTER = new Vector2f(-6000, -6000);

    // Making a utility class instantiable just so I can modify admins properly D:
    public AdversaryUtil() {
        marketsToOverrideAdmin = new HashMap<>();
    }

    // Note for using opt(): Replace with isNull() in 'if' statement when the default parameter can consume or modify unique entries (e.g. proc-gen names and Random.next())

    /**
     * Generates a star system
     *
     * @param systemOptions Star system options
     * @return The newly-created star system
     * @throws JSONException if systemOptions is invalid
     */
    public StarSystemAPI generateStarSystem(JSONObject systemOptions) throws JSONException {
        JSONArray starsList = systemOptions.getJSONObject("starsInSystemCenter").getJSONArray("stars");
        if (starsList.length() == 0)
            throw new RuntimeException("Cannot create a system with no center stars! Custom star systems require at least one star in the \"stars\" list of \"starsInSystemCenter\"!");
        return Global.getSector().createStarSystem(starsList.getJSONObject(0).isNull("name") ? getProcGenName("star", null) : starsList.getJSONObject(0).getString("name"));
    }

    /**
     * Generate a star system center with stars
     *
     * @param system        The star system to modify
     * @param centerOptions Center options
     * @return A List of the newly-created stars
     * @throws JSONException if centerOptions is invlaid
     */
    public List<PlanetAPI> generateSystemCenter(StarSystemAPI system, JSONObject centerOptions) throws JSONException {
        JSONArray starsList = centerOptions.getJSONArray("stars");
        int numOfCenterStars = starsList.length();
        String id = Misc.genUID();

        ArrayList<PlanetAPI> stars = new ArrayList<>(numOfCenterStars);
        if (numOfCenterStars == 1) {
            PlanetAPI newStar = addStar(system, starsList.getJSONObject(0), "system_" + id);
            if (newStar.getTypeId().equals("black_hole")) addAccretionDisk(system, newStar);
            system.setCenter(newStar);
            stars.add(newStar);
        } else {
            SectorEntityToken systemCenter = system.initNonStarCenter(); // Center in which the stars will orbit
            systemCenter.setId(id); // Set the center's id to the unique id

            float orbitRadius = centerOptions.optInt("orbitRadius", 2000) - numOfCenterStars + 1;
            float angle = centerOptions.isNull("orbitAngle") ? StarSystemGenerator.random.nextFloat() * 360f : centerOptions.getInt("orbitAngle");
            float angleDifference = 360f / numOfCenterStars;
            float orbitDays = centerOptions.isNull("orbitDays") ? orbitRadius / ((60f / numOfCenterStars) + StarSystemGenerator.random.nextFloat() * 50f) : centerOptions.getInt("orbitDays");
            char idChar = 'b';

            for (int i = 0; i < numOfCenterStars; i++) {
                PlanetAPI newStar = addStar(system, starsList.getJSONObject(i), "system_" + id + (i > 0 ? "_" + idChar++ : ""));
                newStar.setCircularOrbit(systemCenter, angle, orbitRadius + i, orbitDays);
                stars.add(newStar);

                angle = (angle + angleDifference) % 360f;
            }
        }

        return stars;
    }

    /**
     * Generates an orbiting body in a star system
     *
     * @param system           The star system to modify
     * @param bodyOptions      Body options
     * @param numOfCenterStars Number of stars in the star system's center
     * @param index            Index of the body
     * @return The newly-created planet or star
     * @throws JSONException if bodyOptions is invalid
     */
    public PlanetAPI generateOrbitingBody(StarSystemAPI system, JSONObject bodyOptions, int numOfCenterStars, int index) throws JSONException {
        int indexFocus = bodyOptions.optInt("focus", DEFAULT_FOCUS);
        if (numOfCenterStars + indexFocus > system.getPlanets().size())
            throw new RuntimeException("Invalid \"focus\" index in " + system.getBaseName() + "'s \"orbitingBodies\" entry #" + (index + 1));

        String systemId = system.getCenter().getId();
        if (!systemId.startsWith("system_")) systemId = "system_" + systemId;

        PlanetAPI newBody = Global.getSettings().getSpec(StarGenDataSpec.class, bodyOptions.optString("type", null), true) != null ? addStar(system, bodyOptions, systemId + ":star_" + index) : addPlanet(system, bodyOptions, systemId + ":planet_" + index);
        addCircularOrbit(newBody, (indexFocus <= 0) ? system.getCenter() : system.getPlanets().get(numOfCenterStars + indexFocus - 1), bodyOptions, 20f);
        if (newBody.hasCondition("solar_array")) addSolarArray(newBody, newBody.getFaction().getId());

        // Adds any entities to this planet's lagrange points if applicable
        JSONArray lagrangePoints = bodyOptions.optJSONArray("entitiesAtStablePoints");
        if (lagrangePoints != null) addToLagrangePoints(newBody, lagrangePoints);

        return newBody;
    }

    /**
     * Adds a star in a star system
     *
     * @param system      The star system to modify
     * @param starOptions Star options
     * @param id          Internal id of the star
     * @return The newly-created star
     * @throws JSONException if starOptions is invalid
     */
    public PlanetAPI addStar(StarSystemAPI system, JSONObject starOptions, String id) throws JSONException {
        String starType = starOptions.optString("type", DEFAULT_STAR_TYPE);
        if (starType.equals("random_star_giant"))
            starType = STAR_GIANT_TYPES[StarSystemGenerator.random.nextInt(STAR_GIANT_TYPES.length)];

        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, starType, true);
        if (starData == null) throw new RuntimeException("Star type " + starType + " not found!");

        float radius = starOptions.optInt("radius", DEFAULT_SET_TO_PROC_GEN);
        if (radius <= 0)
            radius = starData.getMinRadius() + (starData.getMaxRadius() - starData.getMinRadius()) * StarSystemGenerator.random.nextFloat();

        float coronaRadius = starOptions.optInt("coronaRadius", DEFAULT_SET_TO_PROC_GEN);
        if (coronaRadius <= 0)
            coronaRadius = Math.max(starData.getCoronaMin(), radius * (starData.getCoronaMult() + starData.getCoronaVar() * (StarSystemGenerator.random.nextFloat() - 0.5f)));

        float flareChance = starOptions.optInt("flareChance", DEFAULT_SET_TO_PROC_GEN);
        if (flareChance < 0)
            flareChance = starData.getMinFlare() + (starData.getMaxFlare() - starData.getMinFlare()) * StarSystemGenerator.random.nextFloat();
        else flareChance /= 100f;

        PlanetAPI newStar;
        if (system.getStar() == null) { // First star in system, so initialize system star
            newStar = system.initStar(id, starType, radius, coronaRadius, starData.getSolarWind(), flareChance, starData.getCrLossMult());
        } else { // Add another star in the system; will have to set appropriate system type elsewhere depending if it will be on center or orbiting the center
            String name = starOptions.optString("name", null);
            if (name == null) name = getProcGenName("star", system.getBaseName());

            // Need to set a default orbit, else new game creation will fail when attempting to save
            newStar = system.addPlanet(id, system.getCenter(), name, starType, 0f, radius, 10000f, 1000f);
            system.addCorona(newStar, coronaRadius, starData.getSolarWind(), flareChance, starData.getCrLossMult());
        }

        // Add special star hazards if applicable
        if (starType.equals("black_hole") || starType.equals("star_neutron")) {
            StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(newStar);
            if (coronaPlugin != null) system.removeEntity(coronaPlugin.getEntity());

            String coronaType = starType.equals("black_hole") ? "event_horizon" : "pulsar_beam";
            if (coronaType.equals("pulsar_beam")) system.addCorona(newStar, 300, 3, 0, 3);
            system.addTerrain(coronaType, new StarCoronaTerrainPlugin.CoronaParams(newStar.getRadius() + coronaRadius, (newStar.getRadius() + coronaRadius) / 2f, newStar, starData.getSolarWind(), flareChance, starData.getCrLossMult())).setCircularOrbit(newStar, 0, 0, 100);
        }

        // Apply any spec changes
        addSpecChanges(newStar, starOptions.optJSONObject("specChanges"));

        return newStar;
    }

    /**
     * Adds a planet in a star system
     *
     * @param system        The star system to modify
     * @param planetOptions Planet options
     * @param id            Internal id of the planet
     * @return The newly-created planet
     * @throws JSONException if planetOptions is invalid
     */
    public PlanetAPI addPlanet(StarSystemAPI system, JSONObject planetOptions, String id) throws JSONException {
        String planetType = planetOptions.optString("type", DEFAULT_PLANET_TYPE);
        PlanetGenDataSpec planetData = (PlanetGenDataSpec) Global.getSettings().getSpec(PlanetGenDataSpec.class, planetType, true);
        if (planetData == null) throw new RuntimeException("Planet type " + planetType + " not found!");

        String name = planetOptions.optString("name", null);
        if (name == null) name = getProcGenName("planet", system.getBaseName());

        float radius = planetOptions.optInt("radius", DEFAULT_SET_TO_PROC_GEN);
        if (radius <= 0)
            radius = planetData.getMinRadius() + (planetData.getMaxRadius() - planetData.getMinRadius()) * StarSystemGenerator.random.nextFloat();

        // Need to set a default orbit, else new game creation will fail when attempting to save
        PlanetAPI newPlanet = system.addPlanet(id, system.getCenter(), name, planetType, 0f, radius, 10000f, 1000f);
        newPlanet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, StarSystemGenerator.random.nextLong());

        // Apply any spec changes
        addSpecChanges(newPlanet, planetOptions.optJSONObject("specChanges"));

        int marketSize = planetOptions.optInt("marketSize", DEFAULT_MARKET_SIZE);
        if (marketSize <= 0) setPlanetConditions(newPlanet, planetOptions);
        else addMarket(newPlanet, planetOptions, marketSize);

        return newPlanet;
    }

    // TODO: Do the rest of the spec changes too!
    private void addSpecChanges(PlanetAPI body, JSONObject specChanges) throws JSONException {
        if (specChanges == null) return;

        JSONArray texture = specChanges.optJSONArray("texture");
        if (texture != null)
            body.getSpec().setTexture(Global.getSettings().getSpriteName(texture.getString(0), texture.getString(1)));

        JSONArray planetColor = specChanges.optJSONArray("planetColor");
        if (planetColor != null)
            body.getSpec().setPlanetColor(new Color(planetColor.getInt(0), planetColor.getInt(1), planetColor.getInt(2), planetColor.getInt(3)));

        JSONArray glowTexture = specChanges.optJSONArray("glowTexture");
        if (glowTexture != null)
            body.getSpec().setGlowTexture(Global.getSettings().getSpriteName(glowTexture.getString(0), glowTexture.getString(1)));

        JSONArray glowColor = specChanges.optJSONArray("glowColor");
        if (glowColor != null)
            body.getSpec().setGlowColor(new Color(glowColor.getInt(0), glowColor.getInt(1), glowColor.getInt(2), glowColor.getInt(3)));

        body.getSpec().setUseReverseLightForGlow(specChanges.optBoolean("useReverseLightForGlow", false));

        body.applySpecChanges();
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
        if (length >= 3) addLagrangePointFeature(planet, lagrangePoints.getJSONObject(2), 5);
        if (length >= 2) addLagrangePointFeature(planet, lagrangePoints.getJSONObject(1), 4);
        if (length >= 1) addLagrangePointFeature(planet, lagrangePoints.getJSONObject(0), 3);
    }

    // Adds a system feature to a specific lagrange point of a planet
    private void addLagrangePointFeature(PlanetAPI planet, JSONObject featureOptions, int lagrangePoint) throws JSONException {
        String type = featureOptions.optString("type", null);
        if (type == null) return; // Lagrange point should remain empty
        float lagrangeAngle = planet.getCircularOrbitAngle();
        switch (lagrangePoint) {
            case 3: // L3 point
                lagrangeAngle -= 180f;
                break;
            case 4: // L4 point
                lagrangeAngle += 60f;
                break;
            case 5: // L5 point
                lagrangeAngle -= 60f;
        }

        SectorEntityToken entity;
        StarSystemAPI system = planet.getStarSystem();
        String name = featureOptions.optString("name", null);
        switch (type) {
            case "asteroid_field":
                entity = addAsteroidField(system, name, featureOptions.optInt("size", 400));
                break;
            case "remnant_station":
                entity = addRemnantStation(system, featureOptions.optBoolean("isDamaged", false));
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
                entity = addObjective(system, name, type, featureOptions.optString("factionId", null));
                break;
            default: // Default option in case of mods adding their own system entities
                entity = system.addCustomEntity(null, name, type, featureOptions.optString("factionId", null));
        }
        entity.setCircularOrbitPointingDown(planet.getOrbitFocus(), lagrangeAngle, planet.getCircularOrbitRadius(), planet.getCircularOrbitPeriod());
    }

    /**
     * Generates a system feature in a star system
     *
     * @param system           The star system to modify
     * @param featureOptions   Feature options
     * @param numOfCenterStars Numbers of stars in star system's center
     * @throws JSONException if featureOptions is invalid
     */
    public void generateSystemFeature(StarSystemAPI system, JSONObject featureOptions, int numOfCenterStars) throws JSONException {
        int focusIndex = numOfCenterStars + (featureOptions.optInt("focus", DEFAULT_FOCUS));
        SectorEntityToken focus = (focusIndex == numOfCenterStars) ? system.getCenter() : system.getPlanets().get(focusIndex - 1);
        String type = featureOptions.getString("type");
        String name = featureOptions.optString("name", null);

        boolean alreadyGenerated = true;
        switch (type) { // For whole-orbit entities
            case "accretion_disk":
                addAccretionDisk(system, focus);
                break;
            case "magnetic_field":
                addMagneticField(system, focus, featureOptions.optInt("size", 300), featureOptions.optInt("orbitRadius", Math.round(focus.getRadius()) + 100));
                break;
            case "rings_ice":
            case "rings_dust":
            case "rings_special":
                addRingBand(system, focus, type + '0', featureOptions.optInt("orbitRadius", Math.round(focus.getRadius()) + 100), name, featureOptions.optInt("bandIndex", 1));
                break;
            case "asteroid_belt":
                addAsteroidBelt(system, focus, featureOptions.optInt("orbitRadius", Math.round(focus.getRadius()) + 100), name, featureOptions.optInt("size", 256), featureOptions.optInt("innerBandIndex", 0), featureOptions.optInt("outerBandIndex", 0));
                break;
            default:
                alreadyGenerated = false;
        }

        if (alreadyGenerated) return;

        SectorEntityToken entity;
        switch (type) {
            case "asteroid_field":
                entity = addAsteroidField(system, name, featureOptions.optInt("size", 400));
                addCircularOrbit(entity, focus, featureOptions, 20f);
                break;
            case "remnant_station":
                entity = addRemnantStation(system, featureOptions.optBoolean("isDamaged", false));
                addCircularOrbitWithSpin(entity, focus, featureOptions, 20f, 5f, 5f);
                break;
            case "station":
                entity = addStation(system, name, featureOptions);
                addCircularOrbitPointingDown(entity, focus, featureOptions, 20f);
                break;
            case "inactive_gate":
                entity = system.addCustomEntity(null, name, type, null);
                addCircularOrbit(entity, focus, featureOptions, 10f);
                break;
            case "stable_location":
                entity = system.addCustomEntity(null, name, type, null);
                addCircularOrbitWithSpin(entity, focus, featureOptions, 20f, 1f, 11f);
                break;
            case "jump_point":
                entity = addJumpPoint(system, name);
                addCircularOrbit(entity, focus, featureOptions, 15f);
                break;
            case "comm_relay":
            case "comm_relay_makeshift":
            case "nav_buoy":
            case "nav_buoy_makeshift":
            case "sensor_array":
            case "sensor_array_makeshift":
                entity = addObjective(system, name, type, featureOptions.optString("factionId", null));
                addCircularOrbitWithSpin(entity, focus, featureOptions, 20f, 1f, 11f);
                break;
            default: // Any salvage entities defined in salvage_entity_gen_data.csv (including ones added by mods)
                entity = addSalvageEntity(system, type, name);
                if (type.equals("coronal_tap")) addCircularOrbitPointingDown(entity, focus, featureOptions, 15f);
                else addCircularOrbitWithSpin(entity, focus, featureOptions, 20f, 1f, 11f);
        }
    }

    public void addCircularOrbit(SectorEntityToken entity, SectorEntityToken focus, JSONObject entityOptions, float defaultOrbitDaysDivisor) throws JSONException {
        float orbitRadius = entityOptions.getInt("orbitRadius");

        float angle = entityOptions.optInt("orbitAngle", DEFAULT_SET_TO_PROC_GEN);
        if (angle < 0) angle = StarSystemGenerator.random.nextFloat() * 360f;

        float orbitDays = entityOptions.optInt("orbitDays", DEFAULT_SET_TO_PROC_GEN);
        if (orbitDays <= 0)
            orbitDays = orbitRadius / (defaultOrbitDaysDivisor + StarSystemGenerator.random.nextFloat() * 5f);

        entity.setCircularOrbit(focus, angle, orbitRadius, orbitDays);
    }

    public void addCircularOrbitPointingDown(SectorEntityToken entity, SectorEntityToken focus, JSONObject entityOptions, float defaultOrbitDaysDivisor) throws JSONException {
        float orbitRadius = entityOptions.getInt("orbitRadius");

        float angle = entityOptions.optInt("orbitAngle", DEFAULT_SET_TO_PROC_GEN);
        if (angle < 0) angle = StarSystemGenerator.random.nextFloat() * 360f;

        float orbitDays = entityOptions.optInt("orbitDays", DEFAULT_SET_TO_PROC_GEN);
        if (orbitDays <= 0)
            orbitDays = orbitRadius / (defaultOrbitDaysDivisor + StarSystemGenerator.random.nextFloat() * 5f);

        entity.setCircularOrbitPointingDown(focus, angle, orbitRadius, orbitDays);
    }

    public void addCircularOrbitWithSpin(SectorEntityToken entity, SectorEntityToken focus, JSONObject entityOptions, float defaultOrbitDaysDivisor, float minSpin, float maxSpin) throws JSONException {
        float orbitRadius = entityOptions.getInt("orbitRadius");

        float angle = entityOptions.optInt("orbitAngle", DEFAULT_SET_TO_PROC_GEN);
        if (angle < 0) angle = StarSystemGenerator.random.nextFloat() * 360f;

        float orbitDays = entityOptions.optInt("orbitDays", DEFAULT_SET_TO_PROC_GEN);
        if (orbitDays <= 0)
            orbitDays = orbitRadius / (defaultOrbitDaysDivisor + StarSystemGenerator.random.nextFloat() * 5f);

        entity.setCircularOrbitWithSpin(focus, angle, orbitRadius, orbitDays, minSpin, maxSpin);
    }

    /**
     * Adds a station in a system
     *
     * @param system         The system to modify
     * @param stationOptions JSONObject representing station options
     * @throws JSONException If stationOptions is invalid
     */
    public SectorEntityToken addStation(StarSystemAPI system, String name, JSONObject stationOptions) throws JSONException {
        SectorEntityToken station = system.addCustomEntity(system.getStar().getId() + ":station_" + Misc.genUID(), name, stationOptions.optString("stationType", "station_side06"), stationOptions.optString("factionId", null));
        int marketSize = stationOptions.optInt("marketSize", DEFAULT_MARKET_SIZE);
        if (marketSize <= 0) Misc.setAbandonedStationMarket(station.getId(), station);
        else addMarket(station, stationOptions, marketSize);

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

        PersonAPI commander = Misc.getAICoreOfficerPlugin("alpha_core").createPerson("alpha_core", "remnant", StarSystemGenerator.random);

        station.setCommander(commander);
        station.getFlagship().setCaptain(commander);

        system.addTag(Tags.THEME_INTERESTING);
        system.addTag(Tags.THEME_REMNANT);
        system.addTag(Tags.THEME_REMNANT_SECONDARY);
        system.addTag(Tags.THEME_UNSAFE);
        if (isDamaged) {
            station.getMemoryWithoutUpdate().set("$damagedStation", true);
            station.setName(station.getName() + " (Damaged)");

            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 2 + StarSystemGenerator.random.nextInt(3), 25f, 6, 12));
            system.addTag(Tags.THEME_REMNANT_SUPPRESSED);
        } else {
            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(station.getFlagship());
            RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, station, null, 3, StarSystemGenerator.random);

            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 8 + StarSystemGenerator.random.nextInt(5), 15f, 8, 24));
            system.addTag(Tags.THEME_REMNANT_RESURGENT);
        }

        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        return station;
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
     * @param system      The star system to modify
     * @param focus       The focus
     * @param width       Width of the magnetic field band
     * @param orbitRadius How far away the magnetic field orbits from focus
     */
    public void addMagneticField(StarSystemAPI system, SectorEntityToken focus, float width, float orbitRadius) {
        system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(width, orbitRadius + width / 2f, focus, orbitRadius, orbitRadius + width, new Color(50, 20, 100, 40), 0.25f + 0.75f * StarSystemGenerator.random.nextFloat(), new Color(140, 100, 235), new Color(180, 110, 210), new Color(150, 140, 190), new Color(140, 190, 210), new Color(90, 200, 170), new Color(65, 230, 160), new Color(20, 220, 70))).setCircularOrbit(focus, 0, 0, 100f);
    }

    /**
     * Adds a jump-point in a star system
     *
     * @param system The star system to modify
     * @param name   Name of the jump-point
     * @return The newly-created jump-point
     */
    public JumpPointAPI addJumpPoint(StarSystemAPI system, String name) {
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(null, name);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
        return jumpPoint;
    }

    /**
     * Adds a salvageable entity in a star system
     *
     * @param system The star system to modify
     * @param type   Type of salvage entity
     * @param name   Name of the salvage entity
     * @return The newly-created entity
     */
    public SectorEntityToken addSalvageEntity(StarSystemAPI system, String type, String name) {
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
                break;
            case "derelict_cryosleeper":
                salvageEntity.setFaction("derelict");
                system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
                system.addTag(Tags.THEME_INTERESTING);
        }

        return salvageEntity;
    }

    /**
     * Adds a system objective in a star system
     *
     * @param system      The star system to modify
     * @param name        Name of the system objective
     * @param objectiveId System objective id; should either be "comm_relay", "sensor_array", "nav_buoy", or their makeshift variants
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
     * <p>Adds an accretion disk to an entity</p>
     * <p>(Credit to theDragn#0580 for publishing the original code on #advanced-modmaking in the Unofficial Starsector Discord.
     * It was apparently taken from the vanilla implementation in the source API, but I couldn't find exactly where in the API)</p>
     *
     * @param system System to modify
     * @param focus  Entity to modify
     */
    public void addAccretionDisk(StarSystemAPI system, SectorEntityToken focus) {
        Random randomSeed = StarSystemGenerator.random;
        float orbitRadius = Math.max(focus.getRadius(), 90f) * (10f + randomSeed.nextFloat() * 5f);
        float bandWidth = 256f;
        int numBands = 12 + randomSeed.nextInt(7);
        for (int i = 0; i < numBands; i++) {
            float radius = orbitRadius - (i * bandWidth * 0.25f) - (i * bandWidth * 0.1f);
            RingBandAPI visual = system.addRingBand(focus, "misc", "rings_ice0", 256f, 0, new Color(46, 35, 173), bandWidth, radius + bandWidth / 2f, -(radius / (30f + 10f * randomSeed.nextFloat())));
            visual.setSpiral(true);
            visual.setMinSpiralRadius(0);
            visual.setSpiralFactor(2f + randomSeed.nextFloat() * 5f);
        }

        SectorEntityToken ring = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(orbitRadius, orbitRadius / 2f, focus, "Accretion Disk"));
        ring.addTag(Tags.ACCRETION_DISK);
        ring.setCircularOrbit(focus, 0, 0, -100);
    }

    /**
     * Adds a fringe jump-point in a system
     *
     * @param system      System to modify
     * @param jumpOptions Jump-point options
     * @return The orbit radius of the newly-created jump-point
     */
    public float generateFringeJumpPoint(StarSystemAPI system, JSONObject jumpOptions) {
        if (jumpOptions == null) { // Create default jump-point
            addJumpPoint(system, "Fringe Jump-point").setCircularOrbit(system.getCenter(), StarSystemGenerator.random.nextFloat() * 360f, DEFAULT_FRINGE_ORBIT_RADIUS, DEFAULT_FRINGE_ORBIT_RADIUS / (15f + StarSystemGenerator.random.nextFloat() * 5f));
            return DEFAULT_FRINGE_ORBIT_RADIUS;
        }

        float orbitRadius = jumpOptions.optInt("orbitRadius", DEFAULT_FRINGE_ORBIT_RADIUS);

        float angle = jumpOptions.optInt("orbitAngle", DEFAULT_SET_TO_PROC_GEN);
        if (angle < 0) angle = StarSystemGenerator.random.nextFloat() * 360f;

        float orbitDays = jumpOptions.optInt("orbitDays", DEFAULT_SET_TO_PROC_GEN);
        if (orbitDays <= 0) orbitDays = orbitRadius / (15f + StarSystemGenerator.random.nextFloat() * 5f);

        addJumpPoint(system, jumpOptions.optString("name", "Fringe Jump-point")).setCircularOrbit(system.getCenter(), angle, orbitRadius, orbitDays);
        return orbitRadius;
    }

    /**
     * <p>Adds a Domain-era cryosleeper in a star system</p>
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator's
     * addCryosleeper() for vanilla implementation</p>
     *
     * @param system       Star system to modify
     * @param name         Name of the cryosleeper
     * @param orbitRadius  How far cryosleeper is located from center of system
     * @param discoverable Whether cryosleeper needs to be discovered before being revealed in map
     */
    public void generateCryosleeper(StarSystemAPI system, String name, float orbitRadius, boolean discoverable) {
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
    public void generateHypershunt(StarSystemAPI system, boolean discoverable, boolean hasParticleEffects) {
        SectorEntityToken systemCenter = system.getCenter();
        SectorEntityToken hypershunt = system.addCustomEntity(null, null, "coronal_tap", null);
        if (systemCenter.isStar()) { // Orbit the sole star
            float orbitRadius = systemCenter.getRadius() + hypershunt.getRadius() + 100f;
            hypershunt.setCircularOrbitPointingDown(systemCenter, StarSystemGenerator.random.nextFloat() * 360f, orbitRadius, orbitRadius / 20f);
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

    // Sets planetary conditions to planet
    private void setPlanetConditions(PlanetAPI planet, JSONObject planetOptions) throws JSONException {
        Misc.initConditionMarket(planet);
        MarketAPI planetMarket = planet.getMarket();
        JSONArray conditions = planetOptions.optJSONArray("conditions");
        if (conditions != null) for (int i = 0; i < conditions.length(); i++)
            try {
                planetMarket.addCondition(conditions.getString(i));
            } catch (Exception e) {
                throw new RuntimeException("Error attempting to add condition \"" + conditions.getString(i) + "\" for \"" + planet.getTypeId() + "\" planet with orbit radius " + Math.round(planet.getCircularOrbitRadius()));
            }
    }

    // Adds a populated market to a specfied entity
    private void addMarket(SectorEntityToken entity, JSONObject marketOptions, int size) throws JSONException {
        if (size > 10) size = 10;
        // Create market on specified entity
        String factionId = marketOptions.getString("factionId");
        MarketAPI planetMarket = Global.getFactory().createMarket(entity.getId() + "_market", entity.getName(), size);
        planetMarket.setFactionId(factionId);
        planetMarket.setPrimaryEntity(entity);
        planetMarket.getTariff().setBaseValue(0.3f); // Default tariff value
        planetMarket.setFreePort(marketOptions.optBoolean("freePort", false));

        // Add conditions
        planetMarket.addCondition("population_" + size);
        JSONArray conditions = marketOptions.optJSONArray("conditions");
        if (conditions != null) for (int i = 0; i < conditions.length(); i++)
            try {
                planetMarket.addCondition(conditions.getString(i));
            } catch (Exception e) {
                throw new RuntimeException("Error attempting to add condition \"" + conditions.getString(i) + "\" for Size " + size + " \"" + factionId + "\" market");
            }

        // Add industries and, if applicable, their specials
        JSONObject industries = marketOptions.optJSONObject("industries");
        if (industries != null) {
            @SuppressWarnings("unchecked") Iterator<String> industryIterator = industries.keys();
            while (industryIterator.hasNext()) {
                String industryId = industryIterator.next();
                try {
                    planetMarket.addIndustry(industryId);
                } catch (Exception e) {
                    throw new RuntimeException("Error attempting to add industry \"" + industryId + "\" for Size " + size + " \"" + factionId + "\" market");
                }
                JSONArray specials = industries.optJSONArray(industryId);
                if (specials != null && specials.length() > 0) {
                    Industry newIndustry = planetMarket.getIndustry(industryId);

                    String aiCoreId = specials.optString(0);
                    if (!aiCoreId.isEmpty()) newIndustry.setAICoreId(aiCoreId);

                    if (specials.length() > 1) {
                        String specialItem = specials.optString(1);
                        if (!specialItem.isEmpty()) newIndustry.setSpecialItem(new SpecialItemData(specialItem, null));
                    }

                    if (specials.length() > 2) newIndustry.setImproved(specials.optBoolean(2, false));
                }
            }
        } else { // Just give market the bare minimum colony
            planetMarket.addIndustry("population");
            planetMarket.addIndustry("spaceport");
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
        if (marketOptions.optBoolean("aiCoreAdmin", false)) marketsToOverrideAdmin.put(planetMarket, "alpha_core");

        //set market in global, factions, and assign market, also submarkets
        Global.getSector().getEconomy().addMarket(planetMarket, true);
        entity.setMarket(planetMarket);
        entity.setFaction(factionId);
    }

    /**
     * Adds a solar array near a planet, taking into account planetary conditions
     * <p>Look in com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator's
     * addSolarShadesAndMirrors() for vanilla implementation</p>
     *
     * @param planet    Planet to modify
     * @param factionId Faction owning the solar array
     */
    public void addSolarArray(PlanetAPI planet, String factionId) {
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

    private void addSolarArray(PlanetAPI planet, int numOfMirrors, int numOfShades, String factionId) {
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
     * Gets a unique proc-gen name; should only be called if it WILL be used, as the name cannot be picked again
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
     * Sets a star system's type
     *
     * @param system The star system to modify
     */
    public void setSystemType(StarSystemAPI system) {
        int starCounter = 0;
        for (PlanetAPI body : system.getPlanets())
            if (body.isStar()) {
                starCounter++;
                if (starCounter == 2) { // Found at least 2 stars
                    system.setSecondary(body);

                    if (body.getId().contains(":star")) system.setType(StarSystemGenerator.StarSystemType.BINARY_FAR);
                    else system.setType(StarSystemGenerator.StarSystemType.BINARY_CLOSE);
                } else if (starCounter == 3) { // Found at least 3 stars
                    system.setTertiary(body);

                    if (body.getId().contains(":star")) { // Orbiting, or far star
                        if (system.getType() == StarSystemGenerator.StarSystemType.BINARY_FAR)
                            system.setType(StarSystemGenerator.StarSystemType.TRINARY_2FAR);
                        else // Current StarSystemType is BINARY_CLOSE
                            system.setType(StarSystemGenerator.StarSystemType.TRINARY_1CLOSE_1FAR);
                    } else system.setType(StarSystemGenerator.StarSystemType.TRINARY_2CLOSE);

                    break; // Stop loop; vanilla game only officially supports trinary systems at most
                }
            }
    }

    /**
     * Sets a system's light color based on a list of stars
     *
     * @param system      The system to modify
     * @param systemStars The stars the light color will be based on
     */
    public void setLightColor(StarSystemAPI system, List<PlanetAPI> systemStars) {
        Color result = Color.WHITE;
        for (int i = 0; i < systemStars.size(); i++)
            if (i != 0) result = Misc.interpolateColor(result, pickLightColorForStar(systemStars.get(i)), 0.5f);
            else result = pickLightColorForStar(systemStars.get(i)); // Set color to first star
        system.setLightColor(result); // light color in entire system, affects all entities
    }

    // Gets a star's light color
    private Color pickLightColorForStar(PlanetAPI star) {
        StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), true);
        return Misc.interpolateColor(starData.getLightColorMin(), starData.getLightColorMax(), StarSystemGenerator.random.nextFloat());
    }

    /**
     * Adds an appropriate Remnant warning beacon to a system. Will do nothing if system has no THEME_REMNANT_... tags
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
            else switch (move) { // Else pick the next location to check
                case 0: // Moving left
                    if (curX != -squareSize) curX--;
                    else {
                        move = 1;
                        curY--;
                    }
                    break;
                case 1: // Moving down
                    if (curY != -squareSize) curY--;
                    else {
                        move = 2;
                        curX++;
                    }
                    break;
                case 2: // Moving right
                    if (curX != squareSize) curX++;
                    else {
                        move = 3;
                        curY++;
                    }
                    break;
                case 3: // Moving up
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
        system.setAge(selectedConstellation.getAge());
    }
}
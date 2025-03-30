package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;

import static com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl.createInitialPeople;
import static org.tranquility.adversary.AdversaryStrings.*;
import static org.tranquility.adversary.AdversaryUtil.LUNALIB_ENABLED;
import static org.tranquility.adversary.AdversaryUtil.MEMKEY_SPAWNED_OPTIMAL;

public class AdversaryOptimal {
    private final StarSystemAPI system;

    public AdversaryOptimal(boolean initPeople) {
        final String optimalName = NAME_STAR_1;
        system = Global.getSector().createStarSystem(optimalName);
        setLocation();

        boolean enableUS = Global.getSettings().getModManager().isModEnabled("US");
        if (enableUS) {
            if (LUNALIB_ENABLED)
                enableUS = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_US_OPTIMAL));
            else enableUS = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_US_OPTIMAL);
            // Key to prevent conditions from being overridden; Unknown Skies defines this tag in US_manualSystemFixer.java
            system.addTag("US_skipSystem");
        }

        system.setBackgroundTextureFilename("graphics/backgrounds/" + (enableUS ? "US_background135n.jpg" : "background1.jpg"));

        SectorEntityToken center = system.initNonStarCenter();
        center.setId("adversary_optimal_center");

        final float starOrbitRadius = 1850f;
        final float starOrbitDays = 82f;

        PlanetAPI optimal = system.addPlanet("adversary_optimal", center, optimalName, enableUS ? "US_star_blue_giant" : StarTypes.BLUE_GIANT, 0f, 1100f, starOrbitRadius - 2f, starOrbitDays);
        system.addCorona(optimal, 825f, 15f, 0f, 5f);
        system.setStar(optimal);

        PlanetAPI zenith = system.addPlanet("adversary_zenith", center, NAME_STAR_2, enableUS ? "US_star_yellow" : StarTypes.YELLOW, 120f, 800f, starOrbitRadius - 1f, starOrbitDays);
        system.addCorona(zenith, 600f, 10f, 0f, 3f);
        system.setSecondary(zenith);

        PlanetAPI pinnacle = system.addPlanet("adversary_pinnacle", center, NAME_STAR_3, enableUS ? "US_star_orange" : StarTypes.ORANGE, 240f, 650f, starOrbitRadius, starOrbitDays);
        system.addCorona(pinnacle, 488f, 10f, 0f, 3f);
        system.setTertiary(pinnacle);

        system.setType(StarSystemGenerator.StarSystemType.TRINARY_2CLOSE);
        system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, OPTIMAL_MUSIC_ID);

        boolean enableIndEvo = Global.getSettings().getModManager().isModEnabled("IndEvo");
        if (enableIndEvo) {
            if (LUNALIB_ENABLED)
                enableIndEvo = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_INDEVO_OPTIMAL));
            else enableIndEvo = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_INDEVO_OPTIMAL);
        }

        // Gas Giant
        String gasGiantName = ProcgenUsedNames.pickName(Tags.PLANET, optimalName, null).nameWithRomanSuffixIfAny;
        PlanetAPI gasGiant = system.addPlanet("adversary_gas_giant", center, gasGiantName, enableUS ? "US_gas_giantB" : Planets.GAS_GIANT, 4f, 250f, 4895f, 217f);
        ProcgenUsedNames.notifyUsed(gasGiantName);
        setAdversaryMarket(gasGiant);
        setPlanetMarket(gasGiant, initPeople, enableIndEvo);

        // Comm Relay in L4
        system.addCustomEntity(null, NAME_COMM_RELAY, Entities.COMM_RELAY, FACTION_ADVERSARY).setCircularOrbitPointingDown(center, gasGiant.getCircularOrbitAngle() + 60f, gasGiant.getCircularOrbitRadius(), gasGiant.getCircularOrbitPeriod());
        // Jump-point in L5
        JumpPointAPI innerJumpPoint = Global.getFactory().createJumpPoint(null, NAME_INNER_JUMP_POINT);
        innerJumpPoint.setStandardWormholeToHyperspaceVisual();
        innerJumpPoint.setCircularOrbit(center, gasGiant.getCircularOrbitAngle() - 60f, gasGiant.getCircularOrbitRadius(), gasGiant.getCircularOrbitPeriod());
        system.addEntity(innerJumpPoint);

        // Toxic World orbiting Gas Giant
        String toxicName = ProcgenUsedNames.pickName(Tags.PLANET, optimalName, null).nameWithRomanSuffixIfAny;
        PlanetAPI toxic = system.addPlanet("adversary_toxic", gasGiant, toxicName, enableUS ? "US_green" : "toxic", 13f, 75f, 525f, 23f);
        ProcgenUsedNames.notifyUsed(toxicName);
        setAdversaryMarket(toxic);
        setPlanetMarket(toxic, initPeople, enableIndEvo);

        // Barren World orbiting Gas Giant
        String barrenName = ProcgenUsedNames.pickName(Tags.PLANET, optimalName, null).nameWithRomanSuffixIfAny;
        PlanetAPI barren = system.addPlanet("adversary_barren", gasGiant, barrenName, enableUS ? "US_barrenF" : Planets.BARREN_BOMBARDED, 42f, 60f, 860f, 38f);
        ProcgenUsedNames.notifyUsed(barrenName);
        setAdversaryMarket(barren);
        setPlanetMarket(barren, initPeople, enableIndEvo);

        // Jungle World
        String jungleName = ProcgenUsedNames.pickName(Tags.PLANET, optimalName, null).nameWithRomanSuffixIfAny;
        PlanetAPI jungle = system.addPlanet("adversary_jungle", center, jungleName, enableUS ? "US_jungle" : "jungle", 77f, 130f, 6265f, 278f);
        ProcgenUsedNames.notifyUsed(jungleName);
        setAdversaryMarket(jungle);
        setPlanetMarket(jungle, initPeople, enableIndEvo);
        addSolarShades(jungle);

        // Nav Buoy in L4
        system.addCustomEntity(null, NAME_NAV_BUOY, Entities.NAV_BUOY, FACTION_ADVERSARY).setCircularOrbitPointingDown(center, jungle.getCircularOrbitAngle() + 60f, jungle.getCircularOrbitRadius(), jungle.getCircularOrbitPeriod());
        // Sensor Array in L5
        system.addCustomEntity(null, NAME_SENSOR_ARRAY, Entities.SENSOR_ARRAY, FACTION_ADVERSARY).setCircularOrbitPointingDown(center, jungle.getCircularOrbitAngle() - 60f, jungle.getCircularOrbitRadius(), jungle.getCircularOrbitPeriod());

        // Water World with a ring band
        String waterName = ProcgenUsedNames.pickName(Tags.PLANET, optimalName, null).nameWithRomanSuffixIfAny;
        PlanetAPI water = system.addPlanet("adversary_water", center, waterName, enableUS ? "US_water" : Planets.PLANET_WATER, 121f, 130f, 6845f, 304f);
        ProcgenUsedNames.notifyUsed(waterName);
        setWaterWorld(water);
        system.addRingBand(water, "misc", "rings_ice0", 256f, 3, Color.white, 256f, 259, 14f, Terrain.RING, NAME_RING_BAND);

        // Asteroid belt
        system.addRingBand(center, "misc", "rings_asteroids0", 256f, 1, Color.WHITE, 256f, 7361f, 446f, null, null);
        system.addRingBand(center, "misc", "rings_asteroids0", 256f, 3, Color.WHITE, 256f, 7489f, 424f, null, null);
        system.addAsteroidBelt(center, 250, 7425f, 256f, 318f, 636f, Terrain.ASTEROID_BELT, NAME_ASTEROID_BELT);

        // Fringe Jump-point
        JumpPointAPI fringeJumpPoint = Global.getFactory().createJumpPoint(null, NAME_FRINGE_JUMP_POINT);
        fringeJumpPoint.setStandardWormholeToHyperspaceVisual();
        fringeJumpPoint.setCircularOrbit(center, 270f, 7725f, 441f);
        system.addEntity(fringeJumpPoint);

        // Inactive Gate
        system.addCustomEntity(null, NAME_GATE, Entities.INACTIVE_GATE, null).setCircularOrbit(center, 180f, 8025f, 642f);

        system.addTag(Tags.THEME_CORE);
        system.addTag(Tags.THEME_CORE_POPULATED);
        system.setLightColor(new Color(233, 213, 218)); // Interpolated color of all three stars
        system.autogenerateHyperspaceJumpPoints(true, false);

        // Clear nebula in hyperspace
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float totalRadius = system.getMaxRadiusInHyperspace() + plugin.getTileSize() * 2f;
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, totalRadius, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, totalRadius, 0f, 360f, 0.25f);

        system.setEnteredByPlayer(true);
        Misc.setAllPlanetsSurveyed(system, true);

        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_SPAWNED_OPTIMAL, true);
    }

    private void setLocation() {
        // Find centroid point of The Core Worlds
        HashSet<Constellation> constellations = new HashSet<>();
        float centroidX = 0;
        float centroidY = 0;
        int centroidCount = 0;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_CORE)) {
                centroidX += system.getLocation().getX();
                centroidY += system.getLocation().getY();
                centroidCount++;
            }
            if (system.isProcgen() && system.isInConstellation()) constellations.add(system.getConstellation());
        }
        Vector2f centroidPoint = centroidCount == 0 ? new Vector2f(0f, 0f) : new Vector2f(centroidX / centroidCount, centroidY / centroidCount);

        // Find constellation closest to The Core Worlds
        Constellation closestConstellation = null;
        float distance = Float.MAX_VALUE;
        for (Constellation c : constellations) {
            float cDist = Misc.getDistance(centroidPoint, c.getLocation());
            if (cDist < distance) {
                distance = cDist;
                closestConstellation = c;
            }
        }

        // Find centroid point of the closest constellation
        centroidX = 0;
        centroidY = 0;
        centroidCount = 0;
        assert closestConstellation != null;
        for (StarSystemAPI system : closestConstellation.getSystems()) {
            centroidX += system.getLocation().getX();
            centroidY += system.getLocation().getY();
            centroidCount++;
        }
        centroidPoint = new Vector2f(centroidX / centroidCount, centroidY / centroidCount);

        // Find empty spot in the closest constellation
        Vector2f newLocation = null;
        float radius = 4000f; // Technically a square with sides of 8000 units
        while (newLocation == null) {
            float x = centroidPoint.getX() + StarSystemGenerator.random.nextFloat() * radius * 2f - radius;
            float y = centroidPoint.getY() + StarSystemGenerator.random.nextFloat() * radius * 2f - radius;
            boolean intersects = false;
            for (StarSystemAPI system : closestConstellation.getSystems()) {
                Vector2f systemLocation = system.getHyperspaceAnchor().getLocation();
                float dX = x - systemLocation.getX();
                float dY = y - systemLocation.getY();
                float dR = 800f + system.getMaxRadiusInHyperspace();
                if (dX * dX + dY * dY < dR * dR) {
                    intersects = true;
                    break;
                }
            }

            if (!intersects) newLocation = new Vector2f(x, y);
            else radius += 0.5f;
        }

        closestConstellation.getSystems().add(system);
        system.setConstellation(closestConstellation);
        system.getLocation().set(newLocation);
        system.setAge(closestConstellation.getAge());
    }

    private void addSolarShades(PlanetAPI planet) {
        float period = planet.getCircularOrbitPeriod();
        float angle = planet.getCircularOrbitAngle();
        float radius = 270f + planet.getRadius();

        SectorEntityToken shade1 = system.addCustomEntity(null, NAME_SHADE_1, Entities.STELLAR_SHADE, FACTION_ADVERSARY);
        SectorEntityToken shade2 = system.addCustomEntity(null, NAME_SHADE_2, Entities.STELLAR_SHADE, FACTION_ADVERSARY);
        SectorEntityToken shade3 = system.addCustomEntity(null, NAME_SHADE_3, Entities.STELLAR_SHADE, FACTION_ADVERSARY);
        shade1.setCircularOrbitPointingDown(planet, angle + 154f, radius - 10f, period);
        shade2.setCircularOrbitPointingDown(planet, angle + 180f, radius + 25f, period);
        shade3.setCircularOrbitPointingDown(planet, angle + 206f, radius - 10f, period);
    }

    private void setWaterWorld(PlanetAPI planet) {
        Misc.initConditionMarket(planet);
        MarketAPI market = planet.getMarket();

        if (planet.getTypeId().equals("US_water")) {
            market.addCondition("US_religious");
            market.addCondition(Conditions.RUINS_VAST);
        } else market.addCondition(Conditions.RUINS_WIDESPREAD);
        market.addCondition(Conditions.HABITABLE);
        market.addCondition(Conditions.MILD_CLIMATE);
        market.addCondition(Conditions.WATER_SURFACE);
        market.addCondition(Conditions.ORE_ULTRARICH);
        market.addCondition(Conditions.RARE_ORE_MODERATE);
        market.addCondition(Conditions.VOLATILES_DIFFUSE);
        market.addCondition(Conditions.ORGANICS_PLENTIFUL);
        market.addCondition(Conditions.POLLUTION);
    }

    private void setAdversaryMarket(PlanetAPI planet) {
        Misc.initEconomyMarket(planet);
        planet.setFaction(FACTION_ADVERSARY);
        MarketAPI market = planet.getMarket();
        market.setSize(6);
        market.setFactionId(FACTION_ADVERSARY);

        market.getTariff().setBaseValue(Global.getSettings().getFactionSpec(FACTION_ADVERSARY).getTariffFraction());
        market.setFreePort(false);

        market.addCondition(Conditions.POPULATION_6);
        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.MEGAPORT);
        market.addIndustry(Industries.WAYSTATION);
        market.addIndustry(Industries.HEAVYBATTERIES);
        market.addIndustry(Industries.HIGHCOMMAND);
        market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.ALPHA_CORE);
        market.getIndustry(Industries.HIGHCOMMAND).setImproved(true);

        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        market.addSubmarket(Submarkets.GENERIC_MILITARY);
        market.addSubmarket(Submarkets.SUBMARKET_BLACK);
    }

    private void setPlanetMarket(PlanetAPI planet, boolean initPeople, boolean enableIndEvo) {
        MarketAPI market = planet.getMarket();

        // Fall-through for US planet types is intended
        switch (planet.getTypeId()) {
            case "US_gas_giantB":
                market.addCondition("US_floating");
                market.addCondition(Conditions.RUINS_EXTENSIVE); // US auto-adds ruins to Floating Continent planets
            case Planets.GAS_GIANT:
                market.addCondition(Conditions.HOT);
                market.addCondition(Conditions.HIGH_GRAVITY);
                market.addCondition(Conditions.VOLATILES_PLENTIFUL);

                market.getIndustry(Industries.POPULATION).setSpecialItem(new SpecialItemData(Items.ORBITAL_FUSION_LAMP, null));
                market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.MEGAPORT).setImproved(true);
                market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.HEAVYBATTERIES).setImproved(true);
                market.getIndustry(Industries.HIGHCOMMAND).setSpecialItem(new SpecialItemData(Items.CRYOARITHMETIC_ENGINE, null));
                market.addIndustry(Industries.STARFORTRESS);
                market.getIndustry(Industries.STARFORTRESS).setAICoreId(Commodities.ALPHA_CORE);
                market.addIndustry(Industries.MINING);
                market.getIndustry(Industries.MINING).setSpecialItem(new SpecialItemData(Items.PLASMA_DYNAMO, null));
                market.addIndustry(Industries.ORBITALWORKS);
                market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.CORRUPTED_NANOFORGE, null));
                market.addIndustry(Industries.LIGHTINDUSTRY);
                if (enableIndEvo) {
                    market.addIndustry("IndEvo_IntArray"); // Interstellar Array
                    market.addIndustry("IndEvo_Academy"); // Academy
                }
                break;
            case "US_green":
                market.addCondition("US_crash");
            case "toxic":
                market.addCondition(Conditions.TECTONIC_ACTIVITY);
                market.addCondition(Conditions.TOXIC_ATMOSPHERE);
                market.addCondition(Conditions.ORE_ULTRARICH);
                market.addCondition(Conditions.RARE_ORE_ULTRARICH);
                market.addCondition(Conditions.VOLATILES_PLENTIFUL);
                market.addCondition(Conditions.ORGANICS_PLENTIFUL);

                market.getIndustry(Industries.MEGAPORT).setSpecialItem(new SpecialItemData(Items.FULLERENE_SPOOL, null));
                market.addIndustry(Industries.STARFORTRESS_MID);
                market.getIndustry(Industries.STARFORTRESS_MID).setAICoreId(Commodities.ALPHA_CORE);
                market.addIndustry(Industries.MINING);
                market.getIndustry(Industries.MINING).setSpecialItem(new SpecialItemData(Items.MANTLE_BORE, null));
                market.addIndustry(Industries.REFINING);
                market.addIndustry(Industries.LIGHTINDUSTRY);
                if (enableIndEvo) {
                    market.addIndustry("IndEvo_IntArray");
                    market.addIndustry("IndEvo_Academy");
                }
                break;
            case "US_barrenF":
                market.addCondition("US_tunnels");
            case Planets.BARREN_BOMBARDED:
                market.addCondition(Conditions.NO_ATMOSPHERE);
                if (planet.getTypeId().equals("US_barrenF"))
                    market.addCondition(Conditions.ORE_RICH); // US_tunnels cannot appear on Ultrarich Ores
                else market.addCondition(Conditions.ORE_ULTRARICH);
                market.addCondition(Conditions.RARE_ORE_MODERATE);
                market.addCondition(Conditions.VOLATILES_TRACE);

                market.getIndustry(Industries.POPULATION).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.POPULATION).setSpecialItem(new SpecialItemData(Items.CORONAL_PORTAL, null));
                market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.MEGAPORT).setImproved(true);
                market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.HEAVYBATTERIES).setSpecialItem(new SpecialItemData(Items.DRONE_REPLICATOR, null));
                market.getIndustry(Industries.HEAVYBATTERIES).setImproved(true);
                market.addIndustry(Industries.STARFORTRESS_HIGH);
                market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
                market.addIndustry(Industries.ORBITALWORKS);
                market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));
                market.addIndustry(Industries.REFINING);
                market.getIndustry(Industries.REFINING).setSpecialItem(new SpecialItemData(Items.CATALYTIC_CORE, null));
                market.addIndustry(Industries.FUELPROD);
                market.getIndustry(Industries.FUELPROD).setSpecialItem(new SpecialItemData(Items.SYNCHROTRON, null));
                if (enableIndEvo) {
                    market.addIndustry("IndEvo_IntArray");
                    market.addIndustry("IndEvo_Academy");
                }
                break;
            case "US_jungle":
                market.addCondition("US_elevator");
            case "jungle":
                market.addCondition(Conditions.HABITABLE);
                market.addCondition(Conditions.HOT);
                market.addCondition(Conditions.MILD_CLIMATE);
                market.addCondition(Conditions.ORE_ULTRARICH);
                market.addCondition(Conditions.ORGANICS_PLENTIFUL);
                market.addCondition(Conditions.FARMLAND_BOUNTIFUL);
                if (planet.getTypeId().equals("US_jungle"))
                    market.addCondition(Conditions.RUINS_WIDESPREAD); // US_elevator cannot appear on Vast Ruins
                else market.addCondition(Conditions.RUINS_VAST);
                market.addCondition(Conditions.SOLAR_ARRAY);

                market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.MEGAPORT).setImproved(true);
                market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.HIGHCOMMAND).setImproved(true);
                market.addIndustry(Industries.STARFORTRESS);
                market.getIndustry(Industries.STARFORTRESS).setAICoreId(Commodities.ALPHA_CORE);
                market.getIndustry(Industries.STARFORTRESS).setImproved(true);
                market.addIndustry(Industries.FARMING);
                market.getIndustry(Industries.FARMING).setSpecialItem(new SpecialItemData(Items.SOIL_NANITES, null));
                market.addIndustry(Industries.LIGHTINDUSTRY);
                market.getIndustry(Industries.LIGHTINDUSTRY).setSpecialItem(new SpecialItemData(Items.BIOFACTORY_EMBRYO, null));
                market.addIndustry(Industries.COMMERCE);
                market.getIndustry(Industries.COMMERCE).setSpecialItem(new SpecialItemData(Items.DEALMAKER_HOLOSUITE, null));
                if (enableIndEvo) {
                    market.addCondition("IndEvo_ArtilleryStationCondition");
                    market.addIndustry("IndEvo_IntArray");
                    market.addIndustry("IndEvo_Academy");
                    market.addIndustry("IndEvo_Artillery_railgun");
                    market.getIndustry("IndEvo_Artillery_railgun").setImproved(true);
                }
                break;
        }

        for (Industry industry : market.getIndustries())
            if (industry.canInstallAICores() && industry.getAICoreId() == null)
                industry.setAICoreId(Commodities.GAMMA_CORE);

        if (initPeople) createInitialPeople(market, StarSystemGenerator.random);
    }
}
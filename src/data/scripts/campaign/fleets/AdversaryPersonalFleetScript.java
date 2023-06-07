package data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;

public class AdversaryPersonalFleetScript extends PersonalFleetScript {
    protected final String fleetName;
    protected final String factionId;
    protected final String marketId;

    public AdversaryPersonalFleetScript(String personId, String fleetName, String factionId, String marketId) {
        super(personId);
        this.fleetName = fleetName;
        this.factionId = factionId;
        this.marketId = marketId;

        PersonAPI commander = Global.getSector().getFaction(factionId).createRandomPerson();
        commander.setId(personId);
        commander.setRankId(Ranks.SPACE_ADMIRAL);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        commander.getStats().setLevel(5);
        commander.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        commander.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        commander.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        commander.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        commander.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
        commander.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
        commander.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        commander.getStats().setSkillLevel(Skills.OFFICER_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.OFFICER_MANAGEMENT, 1);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

        Global.getSector().getImportantPeople().addPerson(commander);

        setMinRespawnDelayDays(10f);
        setMaxRespawnDelayDays(20f);
    }

    @Override
    public CampaignFleetAPI spawnFleet() {
        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();

        MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
        m.createQualityFleet(10, factionId, market.getLocationInHyperspace());
        m.triggerSetFleetCompositionNoSupportShips();
        m.triggerSetFleetCommander(getPerson());
        m.triggerSetFleetFaction(factionId);
        m.triggerSetPatrol();
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, market);
        m.triggerFleetSetNoFactionInName();
        m.triggerFleetSetName(fleetName);
        m.triggerOrderFleetPatrol(market.getStarSystem());
        m.triggerAddCommodityDrop(Commodities.ALPHA_CORE, 1, false);

        CampaignFleetAPI fleet = m.createFleet();
        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        market.getContainingLocation().addEntity(fleet);
        fleet.setLocation(market.getPlanetEntity().getLocation().x, market.getPlanetEntity().getLocation().y);
        fleet.setFacing(random.nextFloat() * 360f);

        return fleet;
    }

    @Override
    public boolean canSpawnFleetNow() {
        MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
        if (market == null || market.hasCondition(Conditions.DECIVILIZED)) return false;
        return market.getFactionId().equals(factionId);
    }

    @Override
    public boolean shouldScriptBeRemoved() {
        return false;
    }
}

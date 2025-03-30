package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.SDFBase;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;

import static org.tranquility.adversary.AdversaryStrings.FACTION_ADVERSARY;
import static org.tranquility.adversary.AdversaryStrings.NAME_SDF_ADVERSARY;

public class SDFAdversary extends SDFBase {
    private final String marketId;

    public SDFAdversary(String marketId) {
        super();
        this.marketId = marketId;
    }

    @Override
    protected String getFactionId() {
        return FACTION_ADVERSARY;
    }

    @Override
    protected String getDefeatTriggerToUse() {
        return "SDFAdversaryDefeated";
    }

    @Override
    protected PersonAPI createOrGetPerson() {
        int commanderLevel = 5;
        PersonAPI commander = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(this.getFactionId()), commanderLevel, this.getCommanderShipSkillPreference(), false, null, false, true, commanderLevel, this.random);
        if (commander.getPersonalityAPI().getId().equals(Personalities.TIMID))
            commander.setPersonality(Personalities.CAUTIOUS);

        commander.setRankId(Ranks.SPACE_ADMIRAL);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        return commander;
    }

    @Override
    protected MarketAPI getSourceMarket() {
        return Global.getSector().getEconomy().getMarket(marketId);
    }

    @Override
    public CampaignFleetAPI spawnFleet() {
        MarketAPI market = getSourceMarket();

        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();
        m.triggerCreateFleet(HubMissionWithTriggers.FleetSize.MAXIMUM, HubMissionWithTriggers.FleetQuality.SMOD_3, this.getFactionId(), FleetTypes.PATROL_LARGE, market.getLocationInHyperspace());
        m.triggerSetFleetSizeFraction(1.25F);
        m.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.ALL_SHIPS, HubMissionWithTriggers.OfficerQuality.HIGHER);
        m.triggerSetFleetCommander(this.getPerson());
        m.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1);
        m.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
        m.triggerFleetAddCommanderSkill(Skills.WOLFPACK_TACTICS, 1);
        m.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1);
        m.triggerFleetAddCommanderSkill(Skills.CARRIER_GROUP, 1);
        m.triggerFleetAddCommanderSkill(Skills.FIGHTER_UPLINK, 1);
        m.triggerFleetAddCommanderSkill(Skills.OFFICER_TRAINING, 1);
        m.triggerFleetAddCommanderSkill(Skills.OFFICER_MANAGEMENT, 1);
        m.triggerFleetAddCommanderSkill(Skills.BEST_OF_THE_BEST, 1);
        m.triggerFleetAddCommanderSkill(Skills.SUPPORT_DOCTRINE, 1);
        m.triggerFleetAddCommanderSkill(Skills.HULL_RESTORATION, 0); // To keep commander skill count at 15
        m.triggerSetPatrol();
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, market);
        m.triggerFleetSetName(NAME_SDF_ADVERSARY);
        m.triggerPatrolAllowTransponderOff();
        m.triggerOrderFleetPatrol(market.getStarSystem());

        m.triggerSetFleetCompositionNoSupportShips();
        m.triggerAddCommodityDrop(Commodities.ALPHA_CORE, 1, false);

        CampaignFleetAPI fleet = m.createFleet();
        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        market.getContainingLocation().addEntity(fleet);
        fleet.setLocation(market.getPlanetEntity().getLocation().x, market.getPlanetEntity().getLocation().y);
        fleet.setFacing(random.nextFloat() * 360f);

        return fleet;
    }
}
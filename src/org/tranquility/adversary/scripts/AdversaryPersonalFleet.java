package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;

import static org.tranquility.adversary.AdversaryUtil.FACTION_ADVERSARY;
import static org.tranquility.adversary.AdversaryUtil.getAdvString;

public class AdversaryPersonalFleet extends PersonalFleetScript {
    protected final String marketId;

    public AdversaryPersonalFleet(String marketId) {
        super(getAdvString("person_id_adversary_personal_commander"));
        this.marketId = marketId;

        PersonAPI commander = Global.getSector().getFaction(FACTION_ADVERSARY).createRandomPerson();
        commander.setId(getAdvString("person_id_adversary_personal_commander"));
        commander.setRankId(Ranks.SPACE_ADMIRAL);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        commander.setVoice(Voices.VILLAIN);
        commander.setImportance(PersonImportance.VERY_HIGH);
        commander.getStats().setLevel(5);
        commander.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        commander.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        commander.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        commander.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        commander.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
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
        m.createQualityFleet(10, FACTION_ADVERSARY, market.getLocationInHyperspace());
        m.triggerSetFleetCompositionNoSupportShips();
        m.triggerSetFleetCommander(getPerson());
        m.triggerSetFleetFaction(FACTION_ADVERSARY);
        m.triggerSetPatrol();
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, market);
        m.triggerFleetSetNoFactionInName();
        m.triggerFleetSetName(getAdvString("name_adversary_personal_fleet"));
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
        return market.getFactionId().equals(FACTION_ADVERSARY);
    }

    @Override
    public boolean shouldScriptBeRemoved() {
        return false;
    }
}

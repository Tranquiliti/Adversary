package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.campaign.MagicFleetBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AdversaryBountyScript extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        // Safety check, if the MagicLib library mod is not enabled for some reason
        if (!Global.getSettings().getModManager().isModEnabled("MagicLib")) return true;

        String bountyId = params.get(0).getString(memoryMap);

        ActiveBounty bounty;
        try {
            bounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyId);
            if (bounty == null) throw new NullPointerException();
        } catch (Exception e) {
            Global.getLogger(AdversaryBountyScript.class).error("Unable to get MagicBounty: " + bountyId, e);
            return true;
        }

        switch (bountyId) {
            case "adversary_TT_Wolfpack":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction("adversary");
                    if (member.getHullId().equals("hyperion")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_LP_Heretics":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction(Factions.LUDDIC_PATH);
                    if (member.getHullId().equals("prometheus2")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Pirates_Derelict":
                byte atlasCount = 0;
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction("adversary");
                    if (atlasCount < 3 && member.getHullId().equals("atlas2")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkipRefresh(false);
                        atlasCount++;
                    } else if (member.getHullId().equals("falcon_p")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Hegemony_Armored":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Training, Officer Management, and Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction(Factions.HEGEMONY);
                    if (member.getHullId().equals("dominator_xiv")) {
                        PersonAPI person = createOfficer(faction, 6, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
                        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("onslaught_xiv")) {
                        PersonAPI person = createOfficer(faction, 6, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
                        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_PL_Cruiser":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction("adversary");
                    if (member.getHullId().equals("gryphon")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("champion")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("pegasus")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_LC_Carrier":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume no fleet-wide officer skills
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction("adversary");
                    if (member.getHullId().equals("eradicator")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.STEADY, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("retribution")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.AGGRESSIVE, member);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Independent_Phase":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Training and Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction("adversary");
                    if (member.getHullId().equals("doom")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.AGGRESSIVE, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("apogee")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.STEADY, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_SD_Beam":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction(Factions.LIONS_GUARD);
                    if (member.getHullId().equals("eagle_LG")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("sunder_LG")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Kite_Swarm":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume everything, because this is a silly bounty, and it should stay that way
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction(Factions.HEGEMONY);
                    if (member.getHullId().equals("kite_hegemony")) {
                        PersonAPI person = createOfficer(faction, 3, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Ziggurat_Plus":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume sleeper officers (a lot of them)
                    member.setCaptain(null);
                    FactionAPI faction = Global.getSector().getFaction(Factions.TRITACHYON);
                    if (member.getHullId().equals("scarab")) {
                        PersonAPI person = createOfficer(faction, 7, Personalities.AGGRESSIVE, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("medusa")) {
                        PersonAPI person = createOfficer(faction, 7, Personalities.AGGRESSIVE, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
                        person.getStats().setSkipRefresh(false);
                    } else if (member.getHullId().equals("aurora")) {
                        PersonAPI person = createOfficer(faction, 7, Personalities.AGGRESSIVE, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                        person.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            case "adversary_Station_Low_Tech":
            case "adversary_Station_Midline":
            case "adversary_Station_High_Tech":
            case "adversary_Station_Remnant":
                CampaignFleetAPI fleet = bounty.getFleet();
                fleet.getFlagship().getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
                fleet.addTag(Tags.NEUTRINO_HIGH);

                fleet.setStationMode(true);

                fleet.clearAbilities();
                fleet.addAbility(Abilities.TRANSPONDER);
                fleet.getAbility(Abilities.TRANSPONDER).activate();
                fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);

                String escortVariant = null;
                int count = 0;
                switch (bountyId) {
                    case "adversary_Station_Low_Tech":
                        escortVariant = "adversary_omen_Fire_Support";
                        count = 10;
                        break;
                    case "adversary_Station_Midline":
                        escortVariant = "adversary_vanguard_pirates_DIE";
                        count = 25;
                        break;
                    case "adversary_Station_High_Tech":
                        escortVariant = "adversary_vigilance_DEM";
                        count = 5;
                        break;
                    case "adversary_Station_Remnant":
                        escortVariant = "adversary_glimmer_Omega";
                        count = 5;
                        break;
                }
                HashMap<String, Integer> escorts = new HashMap<>(1);
                escorts.put(escortVariant, count);
                new MagicFleetBuilder().setFleetFaction(fleet.getFaction().getId()).setSpawnLocation(bounty.getFleetSpawnLocation()).setAssignmentTarget(bounty.getFleet()).setAssignment(FleetAssignment.ORBIT_PASSIVE).setFleetType(FleetTypes.PATROL_SMALL).setSupportFleet(escorts).create();
                break;
            default:
                Global.getLogger(AdversaryBountyScript.class).info("Failed to set custom officers for MagicBounty: " + bountyId);
                break;
        }

        return true;
    }

    // Creates a human officer with no skills
    private PersonAPI createOfficer(FactionAPI faction, int level, String personality, FleetMemberAPI member) {
        PersonAPI person = faction.createRandomPerson();
        person.getStats().setSkipRefresh(true);
        person.getStats().setLevel(level);
        person.setPersonality(personality);
        person.setRankId(Ranks.SPACE_LIEUTENANT);
        person.setPostId(Ranks.POST_OFFICER);
        member.setCaptain(person);
        return person;
    }
}
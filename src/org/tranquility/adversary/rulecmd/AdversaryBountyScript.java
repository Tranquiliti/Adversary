package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.campaign.MagicFleetBuilder;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.*;

import static org.tranquility.adversary.AdversaryStrings.*;

@SuppressWarnings({"unused", "unchecked"})
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
            case "adversary_TT_Wolfpack": {
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    if (member.getHullId().equals("hyperion")) {
                        PersonAPI person = createOfficer(faction, 5, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                break;
            }
            case "adversary_LP_Heretics": {
                FactionAPI faction = Global.getSector().getFaction(Factions.LUDDIC_PATH);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Cybernetic Augmentation
                    member.setCaptain(null);
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
            }
            case "adversary_Pirates_Derelict": {
                byte atlasCount = 0;
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Cybernetic Augmentation
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "atlas2": {
                            if (atlasCount == 3) break;
                            atlasCount++;
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "falcon_p": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.AGGRESSIVE, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_Hegemony_Armored": {
                FactionAPI faction = Global.getSector().getFaction(Factions.HEGEMONY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Training, Officer Management, and Cybernetic Augmentation
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "dominator_xiv": {
                            PersonAPI person = createOfficer(faction, 6, Personalities.RECKLESS, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                            person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
                            person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "onslaught_xiv": {
                            PersonAPI person = createOfficer(faction, 6, Personalities.RECKLESS, member);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                            person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
                            person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                            person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_PL_Cruiser": {
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "gryphon": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "champion": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "pegasus": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_LC_Carrier": {
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume no fleet-wide officer skills
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "eradicator": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.STEADY, member);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "retribution": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.AGGRESSIVE, member);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                            person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_Independent_Phase": {
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Training and Cybernetic Augmentation
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "doom": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.STEADY, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "apogee": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.STEADY, member);
                            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_SD_Beam": {
                FactionAPI faction = Global.getSector().getFaction(Factions.LIONS_GUARD);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume Officer Management and Cybernetic Augmentation
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "eagle_LG": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "sunder_LG": {
                            PersonAPI person = createOfficer(faction, 5, Personalities.CAUTIOUS, member);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_Kite_Swarm": {
                FactionAPI faction = Global.getSector().getFaction(Factions.HEGEMONY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume everything, because this is a silly bounty, and it should stay that way
                    member.setCaptain(null);

                    // It's literally just Kites; don't need to check ship type
                    PersonAPI person = createOfficer(faction, 3, Personalities.STEADY, member);
                    person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                    person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                    person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                    person.getStats().setSkipRefresh(false);
                }
                break;
            }
            case "adversary_Ziggurat_Plus": {
                FactionAPI faction = Global.getSector().getFaction(Factions.TRITACHYON);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume sleeper officers (a lot of them)
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "scarab": {
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
                            break;
                        }
                        case "medusa": {
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
                            break;
                        }
                        case "aurora": {
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
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_Remnant_Plus_Plus": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.OMEGA), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);

                    // No need to replace officers since every ship in an Omega fleet gets an Omega core regardless
                }
                break;
            }
            case "adversary_Station_Low_Tech":
            case "adversary_Station_Midline":
            case "adversary_Station_High_Tech":
            case "adversary_Station_Remnant": {
                CampaignFleetAPI fleet = bounty.getFleet();

                setSecondInCommand(bountyId, fleet.getFaction(), bounty);

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

                // fleet.setAI(null); MagicLib doesn't have null check for getAI() in its ActiveBounty despawn() script
                // So, for now, the station fleet will slowly crawl around in the campaign map

                HashMap<String, Integer> escorts = getEscortsForStationBounty(bountyId);
                new MagicFleetBuilder().setFleetFaction(fleet.getFaction().getId()).setSpawnLocation(bounty.getFleetSpawnLocation()).setAssignmentTarget(bounty.getFleet()).setAssignment(FleetAssignment.ORBIT_PASSIVE).setFleetType(FleetTypes.PATROL_SMALL).setSupportFleet(escorts).create();
                break;
            }
            case "adversary_Event_Horizon": {
                CampaignFleetAPI fleet = bounty.getFleet();
                fleet.setTransponderOn(false);
                fleet.clearAbilities();
                fleet.addAbility(Abilities.GO_DARK);
                fleet.getAbility(Abilities.GO_DARK).activate();

                FactionAPI faction = Global.getSector().getFaction(Factions.MERCENARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    ShipVariantAPI variant = member.getVariant();
                    variant.addPermaMod(HullMods.INSULATEDENGINE, true);
                    if (member.getHullSpec().getManufacturer().equals("Lion's Guard")) {
                        variant.getSModdedBuiltIns().add(HullMods.SOLAR_SHIELDING);
                        variant.addPermaMod(HullMods.HARDENED_SHIELDS, true);
                        variant.addSuppressedMod(HullMods.ANDRADA_MODS);
                    } else variant.addPermaMod(HullMods.SOLAR_SHIELDING, true);
                    member.getRepairTracker().setCR(1f);

                    if (member.isFlagship()) {
                        variant.addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                        continue; // Don't replace the bounty target
                    }

                    // Assume overpowered officers
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "onslaught_xiv":
                            createSuperOfficer(faction, member, true);
                            member.setShipName("Mars");
                            variant.addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                            variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
                            break;
                        case "conquest":
                            createSuperOfficer(faction, member, true);
                            member.setShipName("Victoria");
                            variant.addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                            variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
                            break;
                        default:
                            createSuperOfficer(faction, member, false);
                            if (variant.hasHullMod(HullMods.DEDICATED_TARGETING_CORE)) {
                                variant.removeMod(HullMods.DEDICATED_TARGETING_CORE);
                                variant.addMod(HullMods.INTEGRATED_TARGETING_UNIT);
                            }
                            variant.addPermaMod(HullMods.AUTOREPAIR, true);
                            break;
                    }
                }

                teleportFleetToPlanet(fleet, getClosestBlackHole(fleet.getContainingLocation()));
                Misc.makeHostile(fleet);
                break;
            }
            case "adversary_Derelict_Operations": {
                AICoreOfficerPluginImpl aiPlugin = new AICoreOfficerPluginImpl();

                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.DERELICT), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume integrated Alpha Cores with custom skills
                    member.setCaptain(null);
                    switch (member.getHullId()) {
                        case "rampart": {
                            PersonAPI person = aiPlugin.createPerson(Commodities.ALPHA_CORE, Factions.DERELICT, null);
                            member.setCaptain(person);
                            person.getStats().setSkipRefresh(true);
                            person.getStats().setLevel(8);
                            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 0);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                        case "sentry": {
                            PersonAPI person = aiPlugin.createPerson(Commodities.ALPHA_CORE, Factions.DERELICT, null);
                            member.setCaptain(person);
                            person.getStats().setSkipRefresh(true);
                            person.getStats().setLevel(8);
                            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 0);
                            person.getStats().setSkipRefresh(false);
                            break;
                        }
                    }
                }
                break;
            }
            case "adversary_TT_Wolfpack_Plus": {
                bounty.getCaptain().getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 0);
                FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);

                setSecondInCommand(bountyId, faction, bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    // Assume custom mercenary officers (gained by reassigning skills away from Officer Training and Cybernetic Augmentation)
                    member.setCaptain(null);
                    if (member.getHullId().equals("hyperion")) {
                        PersonAPI person = createOfficer(faction, 6, Personalities.RECKLESS, member);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                        person.getStats().setSkipRefresh(false);
                    }
                }
                Misc.makeHostile(bounty.getFleet());
                break;
            }
            default: {
                Global.getLogger(AdversaryBountyScript.class).info("Failed to set custom officers for MagicBounty: " + bountyId);
                break;
            }
        }

        for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
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

    private void createSuperOfficer(FactionAPI faction, FleetMemberAPI member, boolean ultimate) {
        PersonAPI officer = createOfficer(faction, ultimate ? 14 : 10, Personalities.STEADY, member);
        officer.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        officer.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        officer.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        officer.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        officer.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        officer.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        officer.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        officer.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        officer.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
        officer.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        if (ultimate) {
            officer.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
            officer.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
            officer.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            officer.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
        }
        officer.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
        officer.getStats().setSkipRefresh(false);
    }

    private HashMap<String, Integer> getEscortsForStationBounty(String bountyId) {
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
        return escorts;
    }

    private void teleportFleetToPlanet(CampaignFleetAPI fleet, PlanetAPI planet) {
        fleet.clearAssignments();
        fleet.getContainingLocation().removeEntity(fleet);
        planet.getContainingLocation().addEntity(fleet);
        fleet.setLocation(planet.getLocation().getX(), planet.getLocation().getY());
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, Float.MAX_VALUE);
    }

    private PlanetAPI getClosestBlackHole(LocationAPI location) {
        final Vector2f loc = location.getLocation();
        TreeSet<StarSystemAPI> systems = new TreeSet<>(new Comparator<LocationAPI>() {
            @Override
            public int compare(LocationAPI l1, LocationAPI l2) {
                if (l1 == l2) return 0;
                return Float.compare(Misc.getDistance(loc, l1.getLocation()), Misc.getDistance(loc, l2.getLocation()));
            }
        });

        // Not ideal for every black hole system (including those not proc-genned) to count here, as they might lack an
        // event horizon, but then I'll have to explain the exceptions and/or spoil the fleet location in-text, so no.
        for (StarSystemAPI system : Global.getSector().getStarSystems())
            if (system.hasBlackHole() && !system.hasTag(Tags.THEME_HIDDEN)) systems.add(system);

        StarSystemAPI picked = systems.first();
        return picked.getStar().isBlackHole() ? picked.getStar() : picked.getSecondary().isBlackHole() ? picked.getSecondary() : picked.getTertiary().isBlackHole() ? picked.getTertiary() : picked.getStar();
    }

    // Gives a bounty fleet fixed executive officers from the Second-in-Command mod
    private void setSecondInCommand(String bountyId, FactionAPI faction, ActiveBounty bounty) {
        if (!Global.getSettings().getModManager().isModEnabled("second_in_command") || !Global.getSettings().getBoolean(SETTINGS_ENABLE_SC_SUPPORT_FOR_BOUNTIES))
            return;

        SCData scData = SCUtils.getFleetData(bounty.getFleet());
        try {
            JSONObject bountyJSON = Global.getSettings().loadJSON("data/config/secondInCommand/scBountySkills.json", MOD_ID_ADVERSARY).optJSONObject(bountyId);
            if (bountyJSON == null) return;

            int currentSlot = 0;
            for (Iterator<String> it = bountyJSON.keys(); it.hasNext(); ) {
                String aptitudeId = it.next();

                SCOfficer officer = new SCOfficer(faction.createRandomPerson(), aptitudeId);
                JSONArray officerSkills = bountyJSON.getJSONArray(aptitudeId);
                for (int i = 0; i < officerSkills.length(); i++) {
                    String skillId = officerSkills.getString(i);
                    officer.addSkill(skillId);
                }

                scData.setOfficerInSlot(currentSlot, officer);
                currentSlot++;
            }
        } catch (Exception e) {
            throw new RuntimeException("ERROR: Something went wrong setting the Second-in-Command skills for Adversary bounty! Please contact the Adversary mod author about this!");
        }
    }
}
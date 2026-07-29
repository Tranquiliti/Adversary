package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.misc.SCSettings;
import second_in_command.specs.SCOfficer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.tranquility.adversary.AdversaryStrings.MOD_ID_ADVERSARY;
import static org.tranquility.adversary.AdversaryStrings.SETTINGS_ENABLE_ADVERSARY_SC_SUPPORT;
import static org.tranquility.adversary.AdversaryUtil.LUNALIB_ENABLED;

@SuppressWarnings("unused")
public class AdversaryBountyPick extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (!Global.getSettings().getModManager().isModEnabled("MagicLib")) return true;

        String bountyId = params.get(0).getString(memoryMap);

        ActiveBounty bounty;
        try {
            bounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyId);
            if (bounty == null) throw new NullPointerException();
        } catch (Exception e) {
            Global.getLogger(AdversaryBountyPick.class).error("Unable to get MagicBounty: " + bountyId, e);
            return true;
        }

        JSONObject officerData;
        try {
            officerData = Global.getSettings().loadJSON("data/config/modFiles/magicBounty_officers.json", MOD_ID_ADVERSARY).optJSONObject(bountyId);
        } catch (JSONException | IOException e) {
            officerData = null;
        }

        // Yes, the code and configs are all over the place; no, this will not get any better unless
        // MagicLib has native support for custom officers on bounty fleets
        switch (bountyId) {
            case "adversary_TriTachyon_Wolfpack", "adversary_Pirates_Derelict", "adversary_Persean_Cruiser",
                 "adversary_LuddicChurch_Carrier", "adversary_LuddicPath_Heretics", "adversary_Hegemony_Penal",
                 "adversary_Persean_Combined_Arms", "adversary_TriTachyon_Wolfpack_Plus",
                 "adversary_LuddicPath_Missile", "adversary_Swarm_Kite", "adversary_Swarm_Omen": {
                bounty.getCaptain().getStats().setSkillLevel(Skills.OFFICER_TRAINING, 0);
                bounty.getCaptain().getStats().setSkillLevel(Skills.HULL_RESTORATION, 0);
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Hegemony_Armored": {
                bounty.getCaptain().getStats().setSkillLevel(Skills.HULL_RESTORATION, 0);
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Independent_Phase", "adversary_SindrianDiktat_Beam", "adversary_Derelict_Operations": {
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Remnant_Plus_Plus": {
                bounty.getCaptain().getStats().setSkillLevel(Skills.CREW_TRAINING, 0);
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) {
                        member.setVariant(member.getVariant().clone(), false, false);
                        member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                        member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
                        continue; // Don't replace the bounty target
                    }
                    setOfficer(officerData, member);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Event_Horizon": {
                CampaignFleetAPI fleet = bounty.getFleet();
                fleet.setTransponderOn(false);
                fleet.clearAbilities();
                fleet.addAbility(Abilities.GO_DARK);
                fleet.getAbility(Abilities.GO_DARK).activate();
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    ShipVariantAPI variant = member.getVariant().clone(); // Cloning variant to avoid modifying vanilla variants
                    member.setVariant(variant, false, false);
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
                    switch (member.getHullId()) {
                        case "onslaught_xiv":
                            setSuperOfficer(member, true);
                            member.setShipName("Mars");
                            variant.addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                            variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
                            variant.addTag(Tags.TAG_RETAIN_SMODS_ON_RECOVERY);
                            break;
                        case "conquest":
                            setSuperOfficer(member, true);
                            member.setShipName("Victoria");
                            variant.addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                            variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
                            variant.addTag(Tags.TAG_RETAIN_SMODS_ON_RECOVERY);
                            break;
                        default:
                            setSuperOfficer(member, false);
                            if (variant.hasHullMod(HullMods.DEDICATED_TARGETING_CORE)) {
                                variant.removeMod(HullMods.DEDICATED_TARGETING_CORE);
                                variant.addMod(HullMods.INTEGRATED_TARGETING_UNIT);
                            }
                            variant.addPermaMod(HullMods.AUTOREPAIR, true);
                            break;
                    }
                }
                setSecondInCommand(bountyId, bounty);
                teleportFleetToPlanet(fleet, getClosestBlackHole(fleet.getContainingLocation()));
                break;
            }
            case "adversary_Ziggurat_Plus": {
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.setVariant(member.getVariant().clone(), false, false);
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                    if (member.getCaptain() != null) // Has a sleeper officer, so give them the appropriate tag
                        member.getCaptain().getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Station_Low_Tech", "adversary_Station_Midline", "adversary_Station_High_Tech",
                 "adversary_Station_Remnant": {
                CampaignFleetAPI fleet = bounty.getFleet();
                fleet.getFlagship().setVariant(fleet.getFlagship().getVariant().clone(), false, false);
                fleet.getFlagship().getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                fleet.addTag(Tags.NEUTRINO_HIGH);
                fleet.setStationMode(true); // Will cause UI issues, but also prevents objectives from spawning
                fleet.clearAbilities();
                fleet.addAbility(Abilities.TRANSPONDER);
                fleet.getAbility(Abilities.TRANSPONDER).activate();
                fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);

                // MagicLib doesn't have null check for getAI() in its ActiveBounty despawn() script
                // So, for now, the station fleet will slowly crawl around in the campaign map
                // fleet.setAI(null);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            case "adversary_Ultra_Omega", "adversary_Ultra_Threat", "adversary_Ultra_Dweller", "adversary_Ultra_Maw",
                 "adversary_Ultra_Fabricator", "adversary_Ultra_Tesseract": {
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.setVariant(member.getVariant().clone(), false, false);
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    setOfficer(officerData, member);
                    // Has a sleeper officer, so give them the appropriate tag
                    if (member.getCaptain() != null && member.getCaptain().getStats().getLevel() == 7)
                        member.getCaptain().getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);
                }
                setSecondInCommand(bountyId, bounty);
                break;
            }
            default: {
                Global.getLogger(AdversaryBountyPick.class).info("Failed to set custom officers for MagicBounty: " + bountyId);
                break;
            }
        }

        for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy())
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        return true;
    }

    // Sets a ship's officer using a JSON config
    // Thanks to wispborne for the suggested format; this is currently a watered-down version for personal use
    @SuppressWarnings("unchecked")
    private void setOfficer(JSONObject bountyConfig, FleetMemberAPI member) {
        if (bountyConfig == null) return;

        JSONObject officerConfig = bountyConfig.optJSONObject(member.getHullId());
        if (officerConfig == null) {
            member.setCaptain(null);
            return;
        }

        PersonAPI person;
        String officerFaction = officerConfig.optString("officer_faction", Factions.NEUTRAL);
        int level = officerConfig.optInt("officer_level", 1);
        String personality = officerConfig.optString("officer_personality", Personalities.STEADY);
        String aiCoreId = officerConfig.optString("officer_aiCoreId", null);
        person = createOfficer(member, officerFaction, level, personality, aiCoreId);

        JSONObject skills = officerConfig.optJSONObject("officer_skills");
        for (Iterator<String> iterator = skills.keys(); iterator.hasNext(); ) {
            String skillId = iterator.next();
            person.getStats().setSkillLevel(skillId, skills.optInt(skillId, 1));
        }
        person.getStats().setSkipRefresh(false);
    }

    // Creates an officer and assigns the officer to a ship member
    // Officer stats are set to skip refresh; should be set to true when finished with modifying stats
    private PersonAPI createOfficer(FleetMemberAPI member, String faction, int level, String personality, String aiCoreId) {
        PersonAPI officer;
        if (aiCoreId == null) {
            officer = Global.getSector().getFaction(faction).createRandomPerson();
            officer.setPersonality(personality);
            officer.setRankId(Ranks.SPACE_LIEUTENANT);
            officer.setPostId(Ranks.POST_OFFICER);
        } else officer = Misc.getAICoreOfficerPlugin(aiCoreId).createPerson(aiCoreId, faction, null);

        officer.getStats().setSkipRefresh(true);
        officer.getStats().setLevel(level);
        member.setCaptain(officer);
        return officer;
    }

    private void setSuperOfficer(FleetMemberAPI member, boolean ultimate) {
        PersonAPI officer = createOfficer(member, Factions.MERCENARY, ultimate ? 14 : 10, Personalities.STEADY, null);
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

    private void teleportFleetToPlanet(CampaignFleetAPI fleet, PlanetAPI planet) {
        fleet.clearAssignments();
        fleet.getContainingLocation().removeEntity(fleet);
        planet.getContainingLocation().addEntity(fleet);
        fleet.setLocation(planet.getLocation().getX(), planet.getLocation().getY());
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, Float.MAX_VALUE);
    }

    private PlanetAPI getClosestBlackHole(LocationAPI location) {
        // Not ideal for every black hole system (including those not proc-genned) to count here, as they might lack an
        // event horizon, but then I'll have to explain the exceptions and/or spoil the fleet location in-text, so no.
        List<StarSystemAPI> systems = new ArrayList<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems())
            if (system.hasBlackHole() && !system.hasTag(Tags.THEME_HIDDEN)) systems.add(system);

        final Vector2f loc = location.getLocation();
        systems.sort((l1, l2) -> {
            if (l1 == l2) return 0;
            return Float.compare(Misc.getDistance(loc, l1.getLocation()), Misc.getDistance(loc, l2.getLocation()));
        });
        StarSystemAPI picked = systems.get(0);
        return picked.getStar().isBlackHole() ? picked.getStar() : picked.getSecondary().isBlackHole() ? picked.getSecondary() : picked.getTertiary().isBlackHole() ? picked.getTertiary() : picked.getStar();
    }

    // Gives a bounty fleet pre-configured executive officers from the Second-in-Command mod
    private void setSecondInCommand(String bountyId, ActiveBounty bounty) {
        if (!Global.getSettings().getModManager().isModEnabled("second_in_command")) return;

        boolean enableSC;
        if (LUNALIB_ENABLED)
            enableSC = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_SC_SUPPORT));
        else enableSC = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_SC_SUPPORT);

        if (!enableSC) return;

        try {
            JSONObject bountyJSON = Global.getSettings().loadJSON("data/config/secondInCommand/scBountySkills.json", MOD_ID_ADVERSARY).optJSONObject(bountyId);
            if (bountyJSON == null) return;

            SCData scData = SCUtils.getFleetData(bounty.getFleet());
            FactionAPI faction = Global.getSector().getFaction(bountyJSON.getString("factionId"));

            JSONArray scSkills = bountyJSON.getJSONArray("skills");
            int currentSlot = 0;
            for (int i = 0; i < scSkills.length(); i++) {
                JSONArray scAptitudeSkills = scSkills.getJSONArray(i);

                String aptitudeId = scAptitudeSkills.getString(0).substring(0, scAptitudeSkills.getString(0).indexOf('_', "sc_".length()));
                SCOfficer officer = new SCOfficer(faction.createRandomPerson(), aptitudeId);
                for (int s = 0; s < scAptitudeSkills.length(); s++)
                    officer.addSkill(scAptitudeSkills.getString(s));

                scData.setOfficerInSlot(currentSlot, officer);
                currentSlot++;

                // Skip 4th slot if it is not enabled by Second-in-Command
                if (currentSlot >= 3 && !SCSettings.Companion.getAdditionalSlotForNPCFleets()) break;
            }
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e + "ERROR: Something went wrong setting the Second-in-Command skills for Adversary bounty; please contact the Adversary mod author with the error message! To avoid future errors, disable the Second-in-Command bounty support in settings.json or LunaSettings.\n");
        }
    }
}
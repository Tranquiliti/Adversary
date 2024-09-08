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
import org.tranquility.adversary.lunalib.AdversaryLunaUtil;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.*;

import static org.tranquility.adversary.AdversaryStrings.*;
import static org.tranquility.adversary.AdversaryUtil.LUNALIB_ENABLED;

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

        JSONObject officerData;
        try {
            officerData = Global.getSettings().loadJSON("data/config/modFiles/magicBounty_officers.json", MOD_ID_ADVERSARY).optJSONObject(bountyId);
        } catch (Exception e) {
            officerData = null;
        }

        // Yes, the code and configs are all over the place; no, this will not get any better unless
        // MagicLib has native support for custom officers on bounty fleets
        switch (bountyId) {
            case "adversary_TT_Wolfpack":
            case "adversary_PL_Cruiser":
            case "adversary_LC_Carrier":
            case "adversary_Independent_Phase": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(FACTION_ADVERSARY), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_LP_Heretics": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.LUDDIC_PATH), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_Pirates_Derelict": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(FACTION_ADVERSARY), bounty);

                byte atlasCount = 0;
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    if (member.getHullId().equals("atlas2")) {
                        if (atlasCount == 3) break;
                        else atlasCount++;
                    }
                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_Hegemony_Armored":
            case "adversary_Kite_Swarm": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.HEGEMONY), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_SD_Beam": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.LIONS_GUARD), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_Ziggurat_Plus": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.TRITACHYON), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_Remnant_Plus_Plus": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.OMEGA), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
                    if (member.isFlagship()) member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
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
                setSecondInCommand(bountyId, Global.getSector().getFaction(Factions.DERELICT), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }
                break;
            }
            case "adversary_TT_Wolfpack_Plus": {
                setSecondInCommand(bountyId, Global.getSector().getFaction(FACTION_ADVERSARY), bounty);

                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target
                    member.setCaptain(null);

                    setOfficers(officerData, member);
                }

                bounty.getCaptain().getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 0);
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

    // Sets officers using a JSON config
    // Thanks to wispborne for the suggested format; this is currently a watered-down version for personal use
    private void setOfficers(JSONObject bountyConfig, FleetMemberAPI member) {
        if (bountyConfig == null) return;

        JSONObject officerConfig = bountyConfig.optJSONObject(member.getHullId());
        if (officerConfig == null) return;

        PersonAPI person;
        String officerFaction = officerConfig.optString("officer_faction", Factions.NEUTRAL);
        int level = officerConfig.optInt("officer_level", 1);
        String aiCoreId = officerConfig.optString("officer_aiCoreId", null);
        if (aiCoreId != null) {
            // The AI core plugin also sets up officer skills depending on AI core, so make sure to account for that
            person = new AICoreOfficerPluginImpl().createPerson(aiCoreId, officerFaction, null);
            person.getStats().setSkipRefresh(true);
            person.getStats().setLevel(level);
            member.setCaptain(person);
        } else {
            String personality = officerConfig.optString("officer_personality", Personalities.STEADY);
            person = createOfficer(Global.getSector().getFaction(officerFaction), level, personality, member);
        }

        JSONObject skills = officerConfig.optJSONObject("officer_skills");
        for (Iterator<String> iterator = skills.keys(); iterator.hasNext(); ) {
            String skillId = iterator.next();
            person.getStats().setSkillLevel(skillId, skills.optInt(skillId, 1));
        }
        person.getStats().setSkipRefresh(false);
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

    // Gives a bounty fleet pre-configured executive officers from the Second-in-Command mod
    private void setSecondInCommand(String bountyId, FactionAPI faction, ActiveBounty bounty) {
        if (!Global.getSettings().getModManager().isModEnabled("second_in_command")) return;

        boolean enableSC;
        if (LUNALIB_ENABLED)
            enableSC = Boolean.TRUE.equals(AdversaryLunaUtil.getBoolean(MOD_ID_ADVERSARY, SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES));
        else enableSC = Global.getSettings().getBoolean(SETTINGS_ENABLE_ADVERSARY_SC_SUPPORT);

        if (!enableSC) return;

        try {
            JSONObject bountyJSON = Global.getSettings().loadJSON("data/config/secondInCommand/scBountySkills.json", MOD_ID_ADVERSARY).optJSONObject(bountyId);
            if (bountyJSON == null) return;

            SCData scData = SCUtils.getFleetData(bounty.getFleet());
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
            throw new RuntimeException("ERROR: Something went wrong setting the Second-in-Command skills for Adversary bounty! Disable the Second-in-Command bounty support in settings.json or LunaSettings, and contact the Adversary mod author about this!\n");
        }
    }
}
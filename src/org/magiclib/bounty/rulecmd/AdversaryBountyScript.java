package org.magiclib.bounty.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AdversaryBountyScript extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String bountyId = params.get(0).getString(memoryMap);

        ActiveBounty bounty;
        try {
            bounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyId);
            if (bounty == null) throw new NullPointerException();
        } catch (Exception ex) {
            Global.getLogger(BountyScriptExample.class).error("Unable to get MagicBounty: " + bountyId, ex);
            return true;
        }

        String bountyFaction = bounty.getFleet().getFaction().getId();
        WeightedRandomPicker<String> portraits = Global.getSector().getFaction(bountyFaction).getPortraits(FullName.Gender.ANY);
        switch (bountyId) {
            case "adversary_TT_Wolfpack":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target

                    member.setCaptain(null);
                    if (member.getHullId().equals("hyperion")) { // Only Hyperions get officers
                        PersonAPI person = Global.getFactory().createPerson();
                        person.setFaction(bountyFaction);
                        person.getStats().setSkipRefresh(true);
                        person.setPortraitSprite(portraits.pick());
                        // Assume Officer Management + Cybernetic Augmentation
                        person.getStats().setLevel(5);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                        person.setPersonality(Personalities.RECKLESS);
                        person.setRankId(Ranks.SPACE_CAPTAIN);
                        person.setPostId(null);
                        person.getStats().setSkipRefresh(false);
                        member.setCaptain(person);
                    }
                }
                break;
            case "adversary_LP_Heretics":
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target

                    member.setCaptain(null);
                    if (member.getHullId().equals("prometheus2")) { // Only Prometheus Mk2 get officers
                        PersonAPI person = Global.getFactory().createPerson();
                        person.setFaction(bountyFaction);
                        person.getStats().setSkipRefresh(true);
                        person.setPortraitSprite(portraits.pick());
                        // Assume no fleet-wide officer skills
                        person.getStats().setLevel(5);
                        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
                        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
                        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
                        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
                        person.setPersonality(Personalities.RECKLESS);
                        person.setRankId(Ranks.SPACE_CAPTAIN);
                        person.setPostId(null);
                        person.getStats().setSkipRefresh(false);
                        member.setCaptain(person);
                    }
                }
                break;
            case "adversary_Pirates_Derelict":
                byte atlasCount = 0;
                for (FleetMemberAPI member : bounty.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.isFlagship()) continue; // Don't replace the bounty target

                    member.setCaptain(null);
                    if (atlasCount < 3 && member.getHullId().equals("atlas2")) { // 3 Atlas Mk2 get officers
                        PersonAPI person = Global.getFactory().createPerson();
                        person.setFaction(bountyFaction);
                        person.getStats().setSkipRefresh(true);
                        person.setPortraitSprite(portraits.pick());
                        // Assume no fleet-wide officer skills
                        person.getStats().setLevel(5);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
                        person.setPersonality(Personalities.CAUTIOUS);
                        person.setRankId(Ranks.SPACE_CAPTAIN);
                        person.setPostId(null);
                        person.getStats().setSkipRefresh(false);
                        member.setCaptain(person);
                        atlasCount++;
                    } else if (member.getHullId().equals("falcon_p")) { // All Falcon (P)'s get officers
                        PersonAPI person = Global.getFactory().createPerson();
                        person.setFaction(bountyFaction);
                        person.getStats().setSkipRefresh(true);
                        person.setPortraitSprite(portraits.pick());
                        // Assume no fleet-wide officer skills
                        person.getStats().setLevel(5);
                        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
                        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
                        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
                        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
                        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                        person.setPersonality(Personalities.RECKLESS);
                        person.setRankId(Ranks.SPACE_CAPTAIN);
                        person.setPostId(null);
                        person.getStats().setSkipRefresh(false);
                        member.setCaptain(person);
                    }
                }
                break;
            default:
                break;
        }

        return true;
    }
}

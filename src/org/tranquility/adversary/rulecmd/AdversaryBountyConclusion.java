package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievementManager;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AdversaryBountyConclusion extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (!Global.getSettings().getModManager().isModEnabled("MagicLib")) return true;

        String bountyId = params.get(0).getString(memoryMap);

        switch (bountyId) {
            case "adversary_Hegemony_Penal", "adversary_Persean_Combined_Arms", "adversary_TriTachyon_Wolfpack_Plus",
                 "adversary_LuddicPath_Missile":
                if (Global.getSector().getMemoryWithoutUpdate().contains("$%s_succeeded".formatted(bountyId)))
                    MagicAchievementManager.getInstance().completeAchievement("%s_Completed".formatted(bountyId));
                break;
            case "adversary_Ultra_Maw", "adversary_Ultra_Fabricator", "adversary_Ultra_Tesseract":
                if (Global.getSector().getMemoryWithoutUpdate().contains("$%s_succeeded".formatted(bountyId)) && Global.getSettings().getBattleSize() == 400 && Global.getSettings().getMaxShipsInFleet() <= 30)
                    MagicAchievementManager.getInstance().completeAchievement("%s_Completed".formatted(bountyId));
                break;
            default:
                return false;
        }

        return true;
    }
}
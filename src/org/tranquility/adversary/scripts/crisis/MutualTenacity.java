package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class MutualTenacity extends BaseMarketConditionPlugin {
    private static final float STABILITY_BONUS = 1f;
    private static final float STABILITY_BONUS_DEFEATED_ADVERSARY_ATTACK = 1f;

    @Override
    public void apply(String id) {
        String text = Misc.ucFirst(getName().toLowerCase());
        market.getStability().modifyFlat(id, getBonus(), text);
    }

    @Override
    public void unapply(String id) {
        market.getStability().unmodifyFlat(id);
    }

    @Override
    public void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        float opad = 10f;
        tooltip.addPara("%s stability", opad, Misc.getHighlightColor(), "+" + (int) STABILITY_BONUS);

        if (AdversaryHostileActivityFactor.isPlayerDefeatedAdversaryAttack())
            tooltip.addPara("The bonus is doubled due to the inhabitants " + market.getOnOrAt() + " " + market.getName() + " feeling empowered by the outcome of the Adversary conflict.", opad, Misc.getPositiveHighlightColor(), "doubled");
    }

    public static float getBonus() {
        return AdversaryHostileActivityFactor.isPlayerDefeatedAdversaryAttack() ? STABILITY_BONUS + STABILITY_BONUS_DEFEATED_ADVERSARY_ATTACK : STABILITY_BONUS;
    }
}
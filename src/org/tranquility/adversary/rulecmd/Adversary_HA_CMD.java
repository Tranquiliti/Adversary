package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.tranquility.adversary.scripts.crisis.AdversaryActivityCause;
import org.tranquility.adversary.scripts.crisis.AdversaryHostileActivityFactor;
import org.tranquility.adversary.scripts.crisis.AdversaryPunitiveExpedition;
import org.tranquility.adversary.scripts.crisis.MutualTenacityScript;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Adversary_HA_CMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        String action = params.get(0).getString(memoryMap);

        switch (action) {
            case "canConfrontCrisis":
                return canConfrontCrisis();
            case "canMakeDeal":
                return canMakeDeal();
            case "makeDeal":
                return makeDeal();
            default:
                return false;
        }
    }

    private boolean canConfrontCrisis() {
        return AdversaryPunitiveExpedition.get() == null && !AdversaryHostileActivityFactor.wasAdversaryEverSatBombardedByPlayer() && AdversaryActivityCause.isThreateningToAdversary();
    }

    private boolean canMakeDeal() {
        if (AdversaryPunitiveExpedition.get() != null || AdversaryHostileActivityFactor.wasAdversaryEverSatBombardedByPlayer() || AdversaryActivityCause.isThreateningToAdversary())
            return false;

        return MutualTenacityScript.isTrustworthyToAdversary() && MutualTenacityScript.get() == null;
    }

    private boolean makeDeal() {
        if (MutualTenacityScript.get() == null) new MutualTenacityScript();
        return true;
    }
}
package org.tranquility.adversary.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.tranquility.adversary.scripts.crisis.MutualTenacityScript;

import java.util.List;
import java.util.Map;

public class AdversaryMakeDeal extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (MutualTenacityScript.get() == null)
            new MutualTenacityScript();

        return true;
    }
}
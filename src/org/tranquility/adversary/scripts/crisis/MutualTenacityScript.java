package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;

public class MutualTenacityScript implements EconomyUpdateListener {
    // TODO: add way to get Mutual Tenacity if either commissioned with the Adversary or at Cooperative relations
    public static String KEY = "$adversary_mt_ref";

    public static MutualTenacityScript get() {
        return (MutualTenacityScript) Global.getSector().getMemoryWithoutUpdate().get(KEY);
    }

    public MutualTenacityScript() {
        sendGainedMessage();

        // to avoid duplicates
        MutualTenacityScript existing = get();
        if (existing != null) {
            return;
        }

        Global.getSector().getEconomy().addUpdateListener(this);
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);

        economyUpdated();
    }

    public void sendGainedMessage() {
        MessageIntel msg = new MessageIntel();
        msg.addLine("Mutual Tenacity gained", Misc.getBasePlayerColor());
        msg.addLine(BaseIntelPlugin.BULLET + "Colonies receive %s to stability", Misc.getTextColor(), new String[]{"+" + (int) MutualTenacity.STABILITY_BONUS}, Misc.getHighlightColor());

        // TODO: replace placeholder sprite with something else
        msg.setIcon(Global.getSettings().getSpriteName("events", "piracy_respite"));
        msg.setSound(Sounds.REP_GAIN);
        Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
    }

    public void sendExpiredMessage() {
        MessageIntel msg = new MessageIntel();
        msg.addLine("Mutual Tenacity removed", Misc.getBasePlayerColor());
        msg.setIcon(Global.getSettings().getSpriteName("events", "piracy_respite"));
        msg.setSound(Sounds.REP_LOSS);
        Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
    }

    @Override
    public void commodityUpdated(String commodityId) {
    }

    @Override
    public void economyUpdated() {
        for (MarketAPI curr : Misc.getPlayerMarkets(false)) {
            if (!curr.hasCondition("adversary_mutual_tenacity")) {
                curr.addCondition("adversary_mutual_tenacity");
            }
        }
    }

    public void cleanup() {
        if (Global.getSector().getMemoryWithoutUpdate().contains(KEY)) {
            sendExpiredMessage();
        }
        Global.getSector().getMemoryWithoutUpdate().unset(KEY);
        for (MarketAPI curr : Misc.getPlayerMarkets(false)) {
            if (curr.hasCondition("adversary_mutual_tenacity")) {
                curr.removeCondition("adversary_mutual_tenacity");
            }
        }
    }

    @Override
    public boolean isEconomyListenerExpired() {
        return false;
    }

}
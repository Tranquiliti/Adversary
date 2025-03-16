package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;

import static org.tranquility.adversary.AdversaryStrings.FACTION_ADVERSARY;

public class MutualTenacityScript implements EconomyUpdateListener {
    private static final String KEY = "$adversary_mt_ref";

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

    @Override
    public void commodityUpdated(String commodityId) {
    }

    @Override
    public void economyUpdated() {
        for (MarketAPI curr : Misc.getPlayerMarkets(false))
            if (!curr.hasCondition("adversary_mutual_tenacity")) curr.addCondition("adversary_mutual_tenacity");
    }

    @Override
    public boolean isEconomyListenerExpired() {
        if (AdversaryHostileActivityFactor.isPlayerDefeatedAdversaryAttack()) return false;

        if (!isTrustworthyToAdversary()) {
            cleanup();
            return true;
        }
        return false;
    }

    public static boolean isTrustworthyToAdversary() {
        return (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals(FACTION_ADVERSARY)) || Global.getSector().getFaction(FACTION_ADVERSARY).getRelToPlayer().isAtWorst(RepLevel.COOPERATIVE);
    }

    public static MutualTenacityScript get() {
        return (MutualTenacityScript) Global.getSector().getMemoryWithoutUpdate().get(KEY);
    }

    private void sendGainedMessage() {
        MessageIntel msg = new MessageIntel();
        msg.addLine("Mutual Tenacity gained", Misc.getBasePlayerColor());
        msg.addLine(BaseIntelPlugin.BULLET + "Colonies receive a %s to stability", Misc.getTextColor(), new String[]{"bonus"}, Misc.getHighlightColor());

        msg.setIcon(Global.getSettings().getSpriteName("events", "stage_unknown_good"));
        msg.setSound(Sounds.REP_GAIN);
        Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
    }

    private void sendExpiredMessage() {
        MessageIntel msg = new MessageIntel();
        msg.addLine("Mutual Tenacity removed", Misc.getBasePlayerColor());
        msg.addLine(BaseIntelPlugin.BULLET + "Due to deteriorating relations with the %s", Misc.getTextColor(), new String[]{"Adversary"}, Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor());
        msg.setIcon(Global.getSettings().getSpriteName("events", "stage_unknown_bad"));
        msg.setSound(Sounds.REP_LOSS);
        Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
    }

    private void cleanup() {
        if (Global.getSector().getMemoryWithoutUpdate().contains(KEY)) sendExpiredMessage();

        Global.getSector().getMemoryWithoutUpdate().unset(KEY);
        for (MarketAPI curr : Misc.getPlayerMarkets(false))
            if (curr.hasCondition("adversary_mutual_tenacity")) curr.removeCondition("adversary_mutual_tenacity");
    }
}
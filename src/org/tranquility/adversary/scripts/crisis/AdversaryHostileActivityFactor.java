package org.tranquility.adversary.scripts.crisis;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.Stage;
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel;
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel.FGIEventListener;
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI.GenericRaidParams;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission.FleetStyle;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.tranquility.adversary.AdversaryUtil;

import java.awt.*;
import java.util.Random;

import static org.tranquility.adversary.AdversaryUtil.FACTION_ADVERSARY;

public class AdversaryHostileActivityFactor extends BaseHostileActivityFactor implements FGIEventListener {
    public static String DEFEATED_ADVERSARY_ATTACK = "$defeatedAdversaryAttack";

    public AdversaryHostileActivityFactor(HostileActivityEventIntel intel) {
        super(intel);

        Global.getSector().getListenerManager().addListener(this);
    }

    @Override
    public String getProgressStr(BaseEventIntel intel) {
        return "";
    }

    @Override
    public int getProgress(BaseEventIntel intel) {
        if (!checkFactionExists(FACTION_ADVERSARY, true)) {
            return 0;
        }
        return super.getProgress(intel);
    }

    @Override
    public String getDesc(BaseEventIntel intel) {
        return "Adversary";
    }

    @Override
    public String getNameForThreatList(boolean first) {
        return "Adversary";
    }

    @Override
    public Color getDescColor(BaseEventIntel intel) {
        if (getProgress(intel) <= 0) {
            return Misc.getGrayColor();
        }
        return Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor();
    }

    @Override
    public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
        return new BaseFactorTooltip() {
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                float opad = 10f;
                tooltip.addPara("A rival independent polity poses an existential threat to the Adversary, whose leaders view your growing colonies as nothing more than preparations to take over their \"perfect\" worlds.", 0f);

                tooltip.addPara("Adversary-aligned scouts have been occasionally sighted around your colonies - though normally non-hostile, their presence alone bears ill tidings for nearby colonies.", opad);

                if (!wasAdversaryEverSatBombardedByPlayer()) {
                    LabelAPI label = tooltip.addPara("While these scouts are unlikely to directly harass your colonies in the short-term, it is almost certain that, if not convinced of your benevolent intentions in time, the Adversary's leaders will plan something far, far worse against your colonies.", opad);
                    label.setHighlight("if not convinced of your benevolent intentions in time", "plan something far, far worse");
                    label.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor());
                } else
                    tooltip.addPara("It is all but certain that, due to your heinous actions against the Adversary worlds, the leaders of the Adversary will swiftly respond in kind against your colonies.", opad, Misc.getNegativeHighlightColor(), "swiftly respond in kind");
            }
        };
    }

    @Override
    public boolean shouldShow(BaseEventIntel intel) {
        return getProgress(intel) > 0;
    }

    @Override
    public int getMaxNumFleets(StarSystemAPI system) {
        return 1;
    }

    @Override
    public float getSpawnInHyperProbability(StarSystemAPI system) {
        return 0f;
    }

    @Override
    public CampaignFleetAPI createFleet(StarSystemAPI system, Random random) {
        // minimum is 0.66f for this factor due to it requiring some market presence
        float f = intel.getMarketPresenceFactor(system);

        int difficulty = (int) Math.max(1f, Math.round(f * 4f));
        difficulty += random.nextInt(6);

        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();

        Vector2f loc = system.getLocation();

        m.createQualityFleet(difficulty, FACTION_ADVERSARY, loc);
        m.triggerSetFleetDoctrineComp(0, 0, 5);
        m.triggerFleetSetAvoidPlayerSlowly();
        m.triggerSetFleetType(FleetTypes.MERC_SCOUT);
        m.triggerSetPatrol();
        m.triggerMakeNonHostile();
        m.triggerSetFleetFlag("$adversaryScout");

        m.triggerMakeLowRepImpact();

        CampaignFleetAPI fleet = m.createFleet();
        fleet.removeAbility(Abilities.TRANSPONDER);

        return fleet;
    }


    @Override
    public void addBulletPointForEvent(HostileActivityEventIntel intel, EventStageData stage, TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        Color c = Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor();
        info.addPara("Impending Adversary bombardment", initPad, tc, c, "Adversary");
    }

    @Override
    public void addBulletPointForEventReset(HostileActivityEventIntel intel, EventStageData stage, TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        info.addPara("Adversary bombardment averted", tc, initPad);
    }

    @Override
    public void addStageDescriptionForEvent(HostileActivityEventIntel intel, EventStageData stage, TooltipMakerAPI info) {
        float small = 8f;
        float opad = 10f;

        info.addPara("You've received intel that the Adversary is planning a massive, full-scale saturation-bombardment campaign against one of your star systems.", small, Misc.getNegativeHighlightColor(), "massive, full-scale saturation-bombardment campaign");

        LabelAPI label = info.addPara("If this all-out attack is defeated, your standing with most factions will increase substantially, your colonies will permanently gain increased stability, and the Adversary will likely abandon further efforts against you.", opad);
        label.setHighlight("most factions", "increase substantially", "permanently gain increased stability", "Adversary");
        label.setHighlightColors(Misc.getHighlightColor(), Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor());

        Color c = Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor();
        stage.beginResetReqList(info, true, "crisis", opad);
        info.addPara("The %s has no functional military bases", 0f, c, "Adversary");
        if (!wasAdversaryEverSatBombardedByPlayer())
            info.addPara("You are no longer hostile with the %s", 0f, c, "Adversary");
        stage.endResetReqList(info, false, "crisis", -1, -1);

        addBorder(info, Global.getSector().getFaction(FACTION_ADVERSARY).getBaseUIColor());
    }

    @Override
    public String getEventStageIcon(HostileActivityEventIntel intel, EventStageData stage) {
        return Global.getSector().getFaction(FACTION_ADVERSARY).getCrest();
    }

    @Override
    public TooltipCreator getStageTooltipImpl(final HostileActivityEventIntel intel, final EventStageData stage) {
        if (stage.id == Stage.HA_EVENT) return getDefaultEventTooltip("Adversary bombardment", intel, stage);

        return null;
    }

    @Override
    public float getEventFrequency(HostileActivityEventIntel intel, EventStageData stage) {
        if (stage.id == Stage.HA_EVENT) {
            if (wasAdversaryEverSatBombardedByPlayer())
                return 666f; // Maybe you shouldn't have proven their point by blowing up one of their planets

            if (pickTargetSystem() != null && pickSourceMarket() != null)
                return 1f; // Should make this crisis extremely rare to experience before dealing with most of the other crises
        }
        return 0f;
    }

    @Override
    public void rollEvent(HostileActivityEventIntel intel, EventStageData stage) {
        HAERandomEventData data = new HAERandomEventData(this, stage);
        stage.rollData = data;
        intel.sendUpdateIfPlayerHasIntel(data, false);
    }

    @Override
    public boolean fireEvent(HostileActivityEventIntel intel, EventStageData stage) {
        StarSystemAPI target = pickTargetSystem();
        MarketAPI source = pickSourceMarket();
        if (source == null || target == null) return false;

        stage.rollData = null;
        return startAttack(source, target, getRandomizedStageRandom(3));
    }

    @Override
    public void reportFGIAborted(FleetGroupIntel intel) {
        setPlayerDefeatedAdversaryAttack();

        Misc.adjustRep(Factions.INDEPENDENT, 0.6f, null);
        Misc.adjustRep(Factions.HEGEMONY, 0.3f, null);
        Misc.adjustRep(Factions.TRITACHYON, 0.3f, null);
        Misc.adjustRep(Factions.PERSEAN, 0.3f, null);
        Misc.adjustRep(Factions.DIKTAT, 0.3f, null);
        Misc.adjustRep(Factions.LUDDIC_CHURCH, 0.3f, null);

        new MutualTenacityScript();
    }

    @Override
    public void notifyFactorRemoved() {
        Global.getSector().getListenerManager().removeListener(this);
    }

    @Override
    public void notifyEventEnding() {
        notifyFactorRemoved();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        EventStageData stage = intel.getDataFor(Stage.HA_EVENT);
        if (stage != null && stage.rollData instanceof HAERandomEventData && ((HAERandomEventData) stage.rollData).factor == this) {
            if (pickSourceMarket() == null) intel.resetHA_EVENT();
        }
    }

    public static boolean isPlayerDefeatedAdversaryAttack() {
        return Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean(DEFEATED_ADVERSARY_ATTACK);
    }

    public static void setPlayerDefeatedAdversaryAttack() {
        Global.getSector().getPlayerMemoryWithoutUpdate().set(DEFEATED_ADVERSARY_ATTACK, true);
    }

    public static boolean wasAdversaryEverSatBombardedByPlayer() {
        FactionAPI faction = Global.getSector().getFaction(FACTION_ADVERSARY);
        if (faction != null)
            return faction.getMemoryWithoutUpdate().getInt(MemFlags.FACTION_SATURATION_BOMBARED_BY_PLAYER) > 0;

        return false;
    }

    public StarSystemAPI pickTargetSystem() {
        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(getRandomizedStageRandom(3));

        for (StarSystemAPI system : Misc.getPlayerSystems(false)) {
            float mag = getEffectMagnitude(system);
            if (mag < 0.1f) continue;
            picker.add(system, mag * mag);
        }

        return picker.pick();
    }

    public MarketAPI pickSourceMarket() {
        MarketAPI source = null;
        for (MarketAPI market : AdversaryUtil.getAdversaryMilitaryMarkets().descendingSet()) {
            Industry hc = market.getIndustry(Industries.HIGHCOMMAND);
            if (hc == null) hc = market.getIndustry(Industries.MILITARYBASE);
            if (hc != null && !hc.isDisrupted() && hc.isFunctional()) {
                source = market;
                break;
            }
        }
        return source;
    }

    public boolean startAttack(MarketAPI source, StarSystemAPI system, Random random) {
        GenericRaidParams params = new GenericRaidParams(new Random(random.nextLong()), true);

        params.makeFleetsHostile = true;

        params.factionId = source.getFactionId();
        params.source = source;

        params.prepDays = 14f + random.nextFloat() * 14f;
        params.payloadDays = 27f + 7f * random.nextFloat();

        params.raidParams.where = system;
        params.raidParams.tryToCaptureObjectives = false;

        params.raidParams.allowedTargets.addAll(Misc.getMarketsInLocation(system, Factions.PLAYER));

        params.raidParams.allowNonHostileTargets = true;
        params.raidParams.setBombardment(MarketCMD.BombardType.SATURATION);

        params.style = FleetStyle.QUALITY;

        float fleetSizeMult = source.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);

        float f = intel.getMarketPresenceFactor(system);

        boolean wasSatBombed = wasAdversaryEverSatBombardedByPlayer();

        float totalDifficulty = fleetSizeMult * 15f * (0.6f + 0.4f * f);

        if (wasSatBombed) totalDifficulty *= 2f;

        if (totalDifficulty < 15) return false;

        if (totalDifficulty > 100 && !wasSatBombed) totalDifficulty = 100;

        totalDifficulty -= 10;

        params.fleetSizes.add(10);

        while (totalDifficulty > 0) {
            int min = 6;
            int max = 10;

            int diff = min + random.nextInt(max - min + 1);

            params.fleetSizes.add(diff);
            totalDifficulty -= diff;
        }

        AdversaryPunitiveExpedition punex = new AdversaryPunitiveExpedition(params);
        punex.setListener(this);
        Global.getSector().getIntelManager().addIntel(punex);

        return true;
    }
}
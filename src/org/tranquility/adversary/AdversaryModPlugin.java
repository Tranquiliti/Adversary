package org.tranquility.adversary;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONException;
import org.tranquility.adversary.scripts.AdversaryBlueprintStealer;
import org.tranquility.adversary.scripts.AdversaryDynamicDoctrine;
import org.tranquility.adversary.scripts.AdversaryLunaSettingsListener;
import org.tranquility.adversary.scripts.AdversaryPersonalFleet;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class AdversaryModPlugin extends BaseModPlugin {
    private transient final String FACTION_ADVERSARY = Global.getSettings().getString("adversary", "faction_id_adversary");
    private transient final String MOD_ID_ADVERSARY = Global.getSettings().getString("adversary", "mod_id_adversary"); // For LunaLib
    private transient final boolean LUNALIB_ENABLED = Global.getSettings().getModManager().isModEnabled("lunalib");

    // Adding LunaSettingsListener when game starts
    @Override
    public void onApplicationLoad() {
        if (LUNALIB_ENABLED) LunaSettings.addSettingsListener(new AdversaryLunaSettingsListener());
    }

    // Re-applying or removing listeners on an existing game.
    @Override
    public void onGameLoad(boolean newGame) {
        boolean enableSilliness;
        String sillyBountyId = Global.getSettings().getString("adversary", "settings_enableAdversarySillyBounties");
        if (LUNALIB_ENABLED)
            enableSilliness = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, sillyBountyId));
        else enableSilliness = Global.getSettings().getBoolean(sillyBountyId);

        if (enableSilliness) Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");

        if (!newGame) addAdversaryListeners(false);
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        FactionAPI adversary = Global.getSector().getFaction(FACTION_ADVERSARY);
        // Recent history has made them cold and hateful against almost everyone
        for (FactionAPI faction : Global.getSector().getAllFactions())
            adversary.setRelationship(faction.getId(), -100f);
        adversary.setRelationship(FACTION_ADVERSARY, 100f);
        adversary.setRelationship(Factions.NEUTRAL, 0f);

        addAdversaryListeners(true);
    }

    @Override
    public void onNewGameAfterTimePass() {
        boolean doPersonalFleet;
        String personalFleetId = Global.getSettings().getString("adversary", "settings_enableAdversaryPersonalFleet");
        if (LUNALIB_ENABLED)
            doPersonalFleet = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, personalFleetId));
        else doPersonalFleet = Global.getSettings().getBoolean(personalFleetId);

        if (doPersonalFleet) addAdversaryPersonalFleet();
    }

    // Remove or add listeners to a game depending on currently-set settings
    private void addAdversaryListeners(boolean newGame) {
        boolean dynaDoctrine, stealBlueprints;
        String doctrineId = Global.getSettings().getString("adversary", "settings_enableAdversaryDynamicDoctrine");
        String blueprintId = Global.getSettings().getString("adversary", "settings_enableAdversaryBlueprintStealing");
        if (LUNALIB_ENABLED) { // LunaLib settings overrides settings.json
            dynaDoctrine = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, doctrineId));
            stealBlueprints = Boolean.TRUE.equals(LunaSettings.getBoolean(MOD_ID_ADVERSARY, blueprintId));
        } else { // Just load from settings.json
            dynaDoctrine = Global.getSettings().getBoolean(doctrineId);
            stealBlueprints = Global.getSettings().getBoolean(blueprintId);
        }

        if (newGame) { // Assumes it gets called during onNewGameAfterEconomyLoad()
            if (dynaDoctrine) addAdversaryDynamicDoctrine(true, LUNALIB_ENABLED);
            if (stealBlueprints) addAdversaryBlueprintStealer(true, LUNALIB_ENABLED);
        } else { // Loading existing game
            ListenerManagerAPI listMan = Global.getSector().getListenerManager();
            if (dynaDoctrine) {
                List<AdversaryDynamicDoctrine> doctrineListeners = listMan.getListeners(AdversaryDynamicDoctrine.class);
                if (doctrineListeners.isEmpty()) addAdversaryDynamicDoctrine(false, LUNALIB_ENABLED);
                else // Refresh needed since Adversary's current doctrine resets upon loading a new Starsector application.
                    doctrineListeners.get(0).refresh();
            } else listMan.removeListenerOfClass(AdversaryDynamicDoctrine.class); // Disable dynamic doctrine

            if (stealBlueprints && listMan.getListeners(AdversaryBlueprintStealer.class).isEmpty())
                addAdversaryBlueprintStealer(false, LUNALIB_ENABLED);
            else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
        }
    }

    // Adds a AdversaryDynamicDoctrine with settings
    private void addAdversaryDynamicDoctrine(boolean newGame, boolean lunaLibEnabled) {
        SettingsAPI settings = Global.getSettings();

        Integer doctrineDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryDynamicDoctrineDelay");
        if (lunaLibEnabled) doctrineDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, delayId);
        if (doctrineDelay == null) doctrineDelay = settings.getInt(delayId);

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryDynamicDoctrine(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), doctrineDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryPossibleDoctrines"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Adds a AdversaryBlueprintStealer with settings
    private void addAdversaryBlueprintStealer(boolean newGame, boolean lunaLibEnabled) {
        SettingsAPI settings = Global.getSettings();

        Integer stealDelay = null;
        String delayId = settings.getString("adversary", "settings_adversaryBlueprintStealingDelay");
        if (lunaLibEnabled) stealDelay = LunaSettings.getInt(MOD_ID_ADVERSARY, delayId);
        if (stealDelay == null) stealDelay = settings.getInt(delayId);

        // reportEconomyMonthEnd() procs immediately when starting time pass, hence the -1 to account for that
        try {
            Global.getSector().getListenerManager().addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) (newGame ? -1 : 0), stealDelay.byteValue(), settings.getJSONArray(settings.getString("adversary", "settings_adversaryStealsFromFactions"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Adds a Personal Fleet script
    private void addAdversaryPersonalFleet() {
        // List of Adversary markets is sorted by High-Command presence
        TreeSet<MarketAPI> adversaryMarkets = new TreeSet<>(new Comparator<MarketAPI>() {
            public int compare(MarketAPI m1, MarketAPI m2) {
                int comp = Integer.compare(getScore(m1), getScore(m2));
                if (comp != 0) return comp;
                return Integer.compare(m1.getSize(), m2.getSize());
            }

            private int getScore(MarketAPI m) {
                int score = 0;
                if (m.hasIndustry(Industries.HIGHCOMMAND)) {
                    score++;
                    Industry hc = m.getIndustry(Industries.HIGHCOMMAND);
                    if (hc.isImproved()) score++;
                    if (hc.getAICoreId().equals(Commodities.ALPHA_CORE)) score++;
                    if (hc.getSpecialItem() != null) score++;
                }
                return score;
            }
        });

        // Get all planetary Adversary markets
        for (StarSystemAPI system : Global.getSector().getEconomy().getStarSystemsWithMarkets())
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system))
                if (market.getFactionId().equals(FACTION_ADVERSARY) && market.getPlanetEntity() != null && !market.isHidden())
                    adversaryMarkets.add(market);

        if (!adversaryMarkets.isEmpty())
            new AdversaryPersonalFleet(Global.getSettings().getString("adversary", "person_id_adversary_personal_commander"), Global.getSettings().getString("adversary", "name_adversary_personal_fleet"), FACTION_ADVERSARY, adversaryMarkets.last().getId());
    }
}
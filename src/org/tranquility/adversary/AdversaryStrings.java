package org.tranquility.adversary;

import com.fs.starfarer.api.Global;

public final class AdversaryStrings {
    private static final String STRINGS_CATEGORY = "adversary";
    public static final String MOD_ID_ADVERSARY = Global.getSettings().getString(STRINGS_CATEGORY, "mod_id_adversary");
    public static final String FACTION_ADVERSARY = Global.getSettings().getString(STRINGS_CATEGORY, "faction_id_adversary");
    public static final String OPTIMAL_MUSIC_ID = Global.getSettings().getString(STRINGS_CATEGORY, "optimal_music_id");

    // Names in the Optimal system
    public static final String NAME_STAR_1 = Global.getSettings().getString(STRINGS_CATEGORY, "name_star1");
    public static final String NAME_STAR_2 = Global.getSettings().getString(STRINGS_CATEGORY, "name_star2");
    public static final String NAME_STAR_3 = Global.getSettings().getString(STRINGS_CATEGORY, "name_star3");
    public static final String NAME_INNER_JUMP_POINT = Global.getSettings().getString(STRINGS_CATEGORY, "name_innerJumpPoint");
    public static final String NAME_COMM_RELAY = Global.getSettings().getString(STRINGS_CATEGORY, "name_commRelay");
    public static final String NAME_NAV_BUOY = Global.getSettings().getString(STRINGS_CATEGORY, "name_navBuoy");
    public static final String NAME_SENSOR_ARRAY = Global.getSettings().getString(STRINGS_CATEGORY, "name_sensorArray");
    public static final String NAME_RING_BAND = Global.getSettings().getString(STRINGS_CATEGORY, "name_ringBand");
    public static final String NAME_ASTEROID_BELT = Global.getSettings().getString(STRINGS_CATEGORY, "name_asteroidBelt");
    public static final String NAME_FRINGE_JUMP_POINT = Global.getSettings().getString(STRINGS_CATEGORY, "name_fringeJumpPoint");
    public static final String NAME_GATE = Global.getSettings().getString(STRINGS_CATEGORY, "name_gate");
    public static final String NAME_SHADE_1 = Global.getSettings().getString(STRINGS_CATEGORY, "name_shade1");
    public static final String NAME_SHADE_2 = Global.getSettings().getString(STRINGS_CATEGORY, "name_shade2");
    public static final String NAME_SHADE_3 = Global.getSettings().getString(STRINGS_CATEGORY, "name_shade3");

    // Settings in settings.json
    public static final String SETTINGS_ENABLE_ADVERSARY_OPTIMAL = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversaryOptimal");
    public static final String SETTINGS_ENABLE_ADVERSARY_US_OPTIMAL = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversaryUSOptimal");
    public static final String SETTINGS_ENABLE_ADVERSARY_INDEVO_OPTIMAL = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversaryIndEvoOptimal");
    public static final String SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversaryDynamicDoctrine");
    public static final String SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversaryBlueprintStealing");
    public static final String SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversarySillyBounties");
    public static final String SETTINGS_ENABLE_ADVERSARY_SC_SUPPORT = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversarySCSupport");
    public static final String SETTINGS_ENABLE_SDF_ADVERSARY = Global.getSettings().getString(STRINGS_CATEGORY, "settings_enableAdversarySDF");

    // Settings for the Adversary Dynamic Doctrine
    public static final String SETTINGS_ADVERSARY_DYNAMIC_DOCTRINE_DELAY = Global.getSettings().getString(STRINGS_CATEGORY, "settings_adversaryDynamicDoctrineDelay");
    public static final String SETTINGS_ADVERSARY_POSSIBLE_DOCTRINES = Global.getSettings().getString(STRINGS_CATEGORY, "settings_adversaryPossibleDoctrines");
    public static final String SETTINGS_WEIGHT = Global.getSettings().getString(STRINGS_CATEGORY, "settings_weight");
    public static final String SETTINGS_FLEET_COMPOSITION = Global.getSettings().getString(STRINGS_CATEGORY, "settings_fleetComposition");
    public static final String SETTINGS_OFFICER_SKILLS = Global.getSettings().getString(STRINGS_CATEGORY, "settings_officerSkills");
    public static final String SETTINGS_AGGRESSION = Global.getSettings().getString(STRINGS_CATEGORY, "settings_aggression");
    public static final String SETTINGS_PRIORITY_SHIPS = Global.getSettings().getString(STRINGS_CATEGORY, "settings_priorityShips");
    public static final String SETTINGS_PRIORITY_WEAPONS = Global.getSettings().getString(STRINGS_CATEGORY, "settings_priorityWeapons");
    public static final String SETTINGS_PRIORITY_FIGHTERS = Global.getSettings().getString(STRINGS_CATEGORY, "settings_priorityFighters");

    // Settings for the Adversary Blueprint Stealing
    public static final String SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY = Global.getSettings().getString(STRINGS_CATEGORY, "settings_adversaryBlueprintStealingDelay");
    public static final String SETTINGS_ADVERSARY_STEALS_FROM_FACTIONS = Global.getSettings().getString(STRINGS_CATEGORY, "settings_adversaryStealsFromFactions");

    // Used for Adversary Personal Fleet
    public static final String NAME_SDF_ADVERSARY = Global.getSettings().getString(STRINGS_CATEGORY, "name_sdf_adversary");

    // Colony Crisis (highlights, and strings in MutualTenacity.java and MutualTenacityScript.java, are still hard-coded)
    public static final String ADVERSARY = Global.getSettings().getString(STRINGS_CATEGORY, "Adversary");
    public static final String HA_MAIN_ROW_TOOLTIP1 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_mainRowTooltip1");
    public static final String HA_MAIN_ROW_TOOLTIP2 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_mainRowTooltip2");
    public static final String HA_MAIN_ROW_TOOLTIP_END = Global.getSettings().getString(STRINGS_CATEGORY, "HA_mainRowTooltipEnd");
    public static final String HA_MAIN_ROW_TOOLTIP_END_ALT = Global.getSettings().getString(STRINGS_CATEGORY, "HA_mainRowTooltipEndAlt");
    public static final String HA_BULLET_POINT_FOR_EVENT = Global.getSettings().getString(STRINGS_CATEGORY, "HA_bulletPointForEvent");
    public static final String HA_BULLET_POINT_FOR_EVENT_RESET = Global.getSettings().getString(STRINGS_CATEGORY, "HA_bulletPointForEventReset");
    public static final String HA_STAGE_DESCRIPTION_FOR_EVENT1 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_stageDescriptionForEvent1");
    public static final String HA_STAGE_DESCRIPTION_FOR_EVENT2 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_stageDescriptionForEvent2");
    public static final String HA_STAGE_DESCRIPTION_FOR_EVENT_REQ_LIST_1 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_stageDescriptionForEventReqList1");
    public static final String HA_STAGE_DESCRIPTION_FOR_EVENT_REQ_LIST_2 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_stageDescriptionForEventReqList2");
    public static final String HA_STAGE_TOOLTIP = Global.getSettings().getString(STRINGS_CATEGORY, "HA_stageTooltip");
    public static final String HA_ACTIVITY_CAUSE_TOOLTIP = Global.getSettings().getString(STRINGS_CATEGORY, "HA_activityCauseTooltip");
    public static final String HA_ACTIVITY_CAUSE_DESC = Global.getSettings().getString(STRINGS_CATEGORY, "HA_activityCauseDesc");
    public static final String HA_ACTIVITY_CAUSE_ALT_TOOLTIP1 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_activityCauseAltTooltip1");
    public static final String HA_ACTIVITY_CAUSE_ALT_TOOLTIP2 = Global.getSettings().getString(STRINGS_CATEGORY, "HA_activityCauseAltTooltip2");
    public static final String HA_ACTIVITY_CAUSE_ALT_DESC = Global.getSettings().getString(STRINGS_CATEGORY, "HA_activityCauseAltDesc");
    public static final String HA_PUNITIVE_EXPEDITION_NOUN = Global.getSettings().getString(STRINGS_CATEGORY, "HA_punitiveExpeditionNoun");
    public static final String HA_PUNITIVE_EXPEDITION_BASE_NAME = Global.getSettings().getString(STRINGS_CATEGORY, "HA_punitiveExpeditionBaseName");
}
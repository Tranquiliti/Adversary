package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONException;

import java.util.List;

import static org.tranquility.adversary.AdversaryStrings.*;

public class AdversaryLunaSettingsListener implements LunaSettingsListener {
    @Override
    public void settingsChanged(String modId) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;

        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, SETTINGS_ENABLE_ADVERSARY_SILLY_BOUNTIES))) {
            Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        } else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");

        ListenerManagerAPI listMan = Global.getSector().getListenerManager();

        Integer doctrineDelay = LunaSettings.getInt(modId, SETTINGS_ADVERSARY_DYNAMIC_DOCTRINE_DELAY);
        assert doctrineDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, SETTINGS_ENABLE_ADVERSARY_DYNAMIC_DOCTRINE))) {
            List<AdversaryDynamicDoctrine> changers = listMan.getListeners(AdversaryDynamicDoctrine.class);
            if (changers.isEmpty()) try {
                listMan.addListener(new AdversaryDynamicDoctrine(FACTION_ADVERSARY, (byte) 0, doctrineDelay.byteValue(), Global.getSettings().getJSONArray(SETTINGS_ADVERSARY_POSSIBLE_DOCTRINES)));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else changers.get(0).setDelay(doctrineDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryDynamicDoctrine.class); // Disable dynamic doctrine

        Integer stealDelay = LunaSettings.getInt(modId, SETTINGS_ADVERSARY_BLUEPRINT_STEALING_DELAY);
        assert stealDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, SETTINGS_ENABLE_ADVERSARY_BLUEPRINT_STEALING))) {
            List<AdversaryBlueprintStealer> steals = listMan.getListeners(AdversaryBlueprintStealer.class);
            if (steals.isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) 0, stealDelay.byteValue(), Global.getSettings().getJSONArray(SETTINGS_ADVERSARY_STEALS_FROM_FACTIONS)));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else steals.get(0).setDelay(stealDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
    }
}
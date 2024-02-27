package org.tranquility.adversary.scripts;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONException;

import java.util.List;

import static org.tranquility.adversary.AdversaryUtil.FACTION_ADVERSARY;
import static org.tranquility.adversary.AdversaryUtil.getAdvString;

public class AdversaryLunaSettingsListener implements LunaSettingsListener {
    @Override
    public void settingsChanged(String modId) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;

        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, getAdvString("settings_enableAdversarySillyBounties")))) {
            Global.getSector().getMemoryWithoutUpdate().set("$adversary_sillyBountiesEnabled", true);
        } else Global.getSector().getMemoryWithoutUpdate().unset("$adversary_sillyBountiesEnabled");

        ListenerManagerAPI listMan = Global.getSector().getListenerManager();

        Integer doctrineDelay = LunaSettings.getInt(modId, getAdvString("settings_adversaryDynamicDoctrineDelay"));
        assert doctrineDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, getAdvString("settings_enableAdversaryDynamicDoctrine")))) {
            List<AdversaryDynamicDoctrine> changers = listMan.getListeners(AdversaryDynamicDoctrine.class);
            if (changers.isEmpty()) try {
                listMan.addListener(new AdversaryDynamicDoctrine(FACTION_ADVERSARY, (byte) 0, doctrineDelay.byteValue(), Global.getSettings().getJSONArray(getAdvString("settings_adversaryPossibleDoctrines"))));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else changers.get(0).setDelay(doctrineDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryDynamicDoctrine.class); // Disable dynamic doctrine

        Integer stealDelay = LunaSettings.getInt(modId, getAdvString("settings_adversaryBlueprintStealingDelay"));
        assert stealDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, getAdvString("settings_enableAdversaryBlueprintStealing")))) {
            List<AdversaryBlueprintStealer> steals = listMan.getListeners(AdversaryBlueprintStealer.class);
            if (steals.isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer(FACTION_ADVERSARY, (byte) 0, stealDelay.byteValue(), Global.getSettings().getJSONArray(getAdvString("settings_adversaryStealsFromFactions"))));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else steals.get(0).setDelay(stealDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
    }
}
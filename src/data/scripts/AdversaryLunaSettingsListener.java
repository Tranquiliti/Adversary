package data.scripts;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONException;

import java.util.List;

public class AdversaryLunaSettingsListener implements LunaSettingsListener {
    @Override
    public void settingsChanged(String modId) {
        if (Global.getCurrentState() != GameState.CAMPAIGN || Global.getSector().getFaction("adversary") == null)
            return; // Do nothing if not in campaign or the Adversary faction does not exist

        ListenerManagerAPI listMan = Global.getSector().getListenerManager();
        SettingsAPI settings = Global.getSettings();
        String adversaryId = settings.getString("adversary", "faction_id_adversary");

        Integer doctrineDelay = LunaSettings.getInt(modId, settings.getString("adversary", "settings_adversaryDynamicDoctrineDelay"));
        assert doctrineDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, settings.getString("adversary", "settings_enableAdversaryDynamicDoctrine")))) {
            List<AdversaryDoctrineChanger> changers = listMan.getListeners(AdversaryDoctrineChanger.class);
            if (changers.isEmpty()) try {
                listMan.addListener(new AdversaryDoctrineChanger(adversaryId, (byte) 0, doctrineDelay.byteValue(), Global.getSettings().getJSONArray(settings.getString("adversary", "settings_adversaryPossibleDoctrines"))));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else changers.get(0).setDelay(doctrineDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // Disable dynamic doctrine

        Integer stealDelay = LunaSettings.getInt(modId, settings.getString("adversary", "settings_adversaryBlueprintStealingDelay"));
        assert stealDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, settings.getString("adversary", "settings_enableAdversaryBlueprintStealing")))) {
            List<AdversaryBlueprintStealer> steals = listMan.getListeners(AdversaryBlueprintStealer.class);
            if (steals.isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer(adversaryId, (byte) 0, stealDelay.byteValue(), Global.getSettings().getJSONArray(settings.getString("adversary", "settings_adversaryStealsFromFactions"))));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else steals.get(0).setDelay(stealDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
    }
}
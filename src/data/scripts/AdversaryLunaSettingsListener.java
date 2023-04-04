package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONException;

import java.util.List;

public class AdversaryLunaSettingsListener implements LunaSettingsListener {
    public AdversaryLunaSettingsListener() {
        Global.getSector().getListenerManager().addListener(this, true);
    }

    @Override
    public void settingsChanged(String modId) {
        if (Global.getSector().getFaction("adversary") == null) return;

        Integer doctrineDelay = LunaSettings.getInt("adversary", "adversary_adversaryDoctrineChangeDelay");
        if (doctrineDelay == null) doctrineDelay = Global.getSettings().getInt("adversaryDoctrineChangeDelay");

        ListenerManagerAPI listMan = Global.getSector().getListenerManager();
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, "adversary_enableAdversaryDoctrineChange"))) {
            List<AdversaryDoctrineChanger> changers = listMan.getListeners(AdversaryDoctrineChanger.class);
            if (changers.isEmpty()) try {
                listMan.addListener(new AdversaryDoctrineChanger("adversary", (byte) 0, doctrineDelay.byteValue(), Global.getSettings().getJSONArray("adversaryPossibleDoctrines")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else changers.get(0).changeDelay(doctrineDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // Disable doctrine changer

        Integer stealDelay = LunaSettings.getInt("adversary", "adversary_adversaryBlueprintStealingDelay");
        if (stealDelay == null) stealDelay = Global.getSettings().getInt("adversaryDoctrineChangeDelay");
        if (Boolean.TRUE.equals(LunaSettings.getBoolean(modId, "adversary_enableAdversaryBlueprintStealing"))) {
            List<AdversaryBlueprintStealer> steals = listMan.getListeners(AdversaryBlueprintStealer.class);
            if (steals.isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer("adversary", (byte) 0, stealDelay.byteValue(), Global.getSettings().getJSONArray("adversaryStealsFromFactions")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else steals.get(0).changeDelay(stealDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
    }
}
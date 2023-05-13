package data.scripts;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
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

        Integer doctrineDelay = LunaSettings.getInt("adversary", "adversaryDynamicDoctrineDelay");
        assert doctrineDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "enableAdversaryDynamicDoctrine"))) {
            List<AdversaryDoctrineChanger> changers = listMan.getListeners(AdversaryDoctrineChanger.class);
            if (changers.isEmpty()) try {
                listMan.addListener(new AdversaryDoctrineChanger("adversary", (byte) 0, doctrineDelay.byteValue(), Global.getSettings().getJSONArray("adversaryPossibleDoctrines")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else changers.get(0).setDelay(doctrineDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryDoctrineChanger.class); // Disable dynamic doctrine

        Integer stealDelay = LunaSettings.getInt("adversary", "adversaryBlueprintStealingDelay");
        assert stealDelay != null;
        if (Boolean.TRUE.equals(LunaSettings.getBoolean("adversary", "enableAdversaryBlueprintStealing"))) {
            List<AdversaryBlueprintStealer> steals = listMan.getListeners(AdversaryBlueprintStealer.class);
            if (steals.isEmpty()) try {
                listMan.addListener(new AdversaryBlueprintStealer("adversary", (byte) 0, stealDelay.byteValue(), Global.getSettings().getJSONArray("adversaryStealsFromFactions")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            else steals.get(0).setDelay(stealDelay.byteValue());
        } else listMan.removeListenerOfClass(AdversaryBlueprintStealer.class); // Disable blueprint stealer
    }
}
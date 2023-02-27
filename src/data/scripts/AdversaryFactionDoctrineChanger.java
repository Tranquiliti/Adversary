package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;

public class AdversaryFactionDoctrineChanger implements EveryFrameScript {
    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
    }
}
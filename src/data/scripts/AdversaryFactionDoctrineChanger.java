package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;

import java.util.Random;

public class AdversaryFactionDoctrineChanger implements EveryFrameScript {
    // TODO: See if there's a faster, better way to check for new cycle (maybe a listener?)
    private final byte[] doctrineList = {3, 2, 1, 0}; // Each index represents a doctrine number
    //protected Logger log = Global.getLogger(AdversaryFactionDoctrineChanger.class);
    protected FactionDoctrineAPI factionDoctrine;
    protected boolean done;
    protected CampaignClockAPI clock;
    protected float currentCycle;

    public AdversaryFactionDoctrineChanger(FactionDoctrineAPI doctrine) {
        super();
        factionDoctrine = doctrine;
        clock = Global.getSector().getClock();
        currentCycle = clock.getCycle();
        //log.info("Faction doctrine changer active!");
    }

    /**
     * @return true when the script is finished and can be cleaned up by the engine.
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * @return whether advance() should be called while the campaign engine is paused.
     */
    @Override
    public boolean runWhilePaused() {
        return false;
    }

    /**
     * Use SectorAPI.getClock() to convert to campaign days.
     *
     * @param amount seconds elapsed during the last frame.
     */
    @Override
    public void advance(float amount) {
        if (done) return;

        // Change fleet doctrine every new cycle; cannot choose the same doctrine twice in a row
        if (clock.getCycle() != currentCycle) {
            currentCycle = clock.getCycle();
            int randomNum = new Random().nextInt(3); // doctrineList.length - 1, which excludes the last index

            switch (doctrineList[randomNum]) {
                case 3:  // Warship-focused
                    setFleetDoctrine(5, 1, 1);
                    break;
                case 2:  // Carrier-focused
                    setFleetDoctrine(1, 5, 1);
                    break;
                case 1:  // Phase-focused
                    setFleetDoctrine(1, 1, 5);
                    break;
                default: // Balanced
                    setFleetDoctrine(3, 2, 2);
                    break;
            }
            //log.info("Adversary fleet composition set to " + factionDoctrine.getWarships() + "-" + factionDoctrine.getCarriers() + "-" + factionDoctrine.getPhaseShips());

            // Prevent selected doctrine from being picked again next cycle
            byte temp = doctrineList[randomNum];
            doctrineList[randomNum] = doctrineList[3];
            doctrineList[3] = temp;
        }
    }

    // Set this faction's fleets to a specified composition
    private void setFleetDoctrine(int warships, int carriers, int phaseShips) {
        factionDoctrine.setWarships(warships);
        factionDoctrine.setCarriers(carriers);
        factionDoctrine.setPhaseShips(phaseShips);
    }
}
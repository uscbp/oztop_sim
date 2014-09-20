package sim.motor;

import java.io.File;

import javax.sound.sampled.*;

import sim.graphics.Point3d;
import sim.util.Log;
import sim.main.Main;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

/**
 * REACH
 * make a Gesture class and derive all the gestures from it!
 */
public class Reach extends Thread
{
    // Thread stop request flag
    protected boolean stopRequested=false;

    // The arm/hand making the reach
    protected ArmHand armHand;

    // Object that is the reach target
    protected Graspable object;

	// Obstacle
	protected Graspable obstacle;

    protected Point3d viaPoint;

    // Reach trajectory
    protected Trajectory tr;

    // Output comment
    protected String com;

    // Time to sleep for in between reach time steps
    protected int SLP;

    // Action sound clip
    public static Clip soundClip=null;

    /**
     * Creates a reach thread
     * @param a - Actor making the reach
     * @param o - Graspable object being reached to
     * @param t - Reach trajectory
     * @param com - Output comment
     */
    public Reach(final ArmHand a, final Graspable o, final Graspable obs, final Trajectory t, final String com)
    {
        armHand=a;
        object=o;
		obstacle=obs;
        tr=t;
        this.com=com;
        SLP=5;
        if (armHand.search_mode==ArmHand.VISUAL_SEARCH)
        {
            SLP=1;
            viaPoint = o.objectCenter.duplicate();
            if(obs != null)
            {
                viaPoint = obs.objectCenter.duplicate();
                viaPoint.x+=300;
                viaPoint.z+=100;
            }
            else if(o.lastPlan == Graspable.SLAP)
            {
                viaPoint.y+=300;
                viaPoint.z-=100;
            }
        }
        else if(armHand.search_mode==ArmHand.EXECUTE)
            SLP=5;
        reset();
    }

    public void reset()
    {
    }

    /**
     * Starts the reach
     */
    public void run()
    {
		// Go until told to stop
        while (!stopRequested)
        {
            // Visual search time step
            if (armHand.search_mode==ArmHand.VISUAL_SEARCH)
            {
				if(!armHand.tickVisual(object,obstacle,viaPoint))
                    stopRequested=true;
                Main.refreshDisplay();
            }
            // Execute reach time step
            else if (armHand.search_mode==ArmHand.EXECUTE)
            {
                // Load sound clip if not already loaded
                if(armHand.audible && armHand.rtime==0.0)
                    loadSound();

                if(!armHand.tickReachGesture(tr,object,obstacle))
                    stopRequested=true;

                Main.refreshDisplay();
            }
            try
            {
                sleep(SLP);
            }
            catch(InterruptedException e)
            {}
        }
        Log.println("Reach stopped.");
        // If the action is audible, the hand is contacting the object, and the sound is not finished
        Main.refreshDisplay();
        armHand.enablePanels();
        Main.setTrace(false);
        armHand.finalizeReach(com,object);
        armHand.kill_ifActive();
    }

    /**
     * Stop reach
     */
    public void stopRGest()
    {
        stopRequested=true;

        // If this is the end of a visual search - load the appropriate sound clip
        if (armHand.search_mode==ArmHand.VISUAL_SEARCH && (soundClip == null || !soundClip.isOpen()))
            loadSound();
    }

    /**
     * Loads the sound file
     */
    public void loadSound()
    {
        String soundFile;
        if((object.myname.equals("objects/box.seg") && object.lastPlan == Graspable.PRECISION &&
                armHand.congruentSound) || (object.lastPlan == Graspable.POWER &&
                !armHand.congruentSound))
            soundFile = "wood.wav";
        else if((object.lastPlan==Graspable.POWER && armHand.congruentSound) ||
                (object.myname.equals("objects/box.seg") && object.lastPlan == Graspable.PRECISION &&
                        !armHand.congruentSound))
            soundFile = "slap.wav";
        else
            soundFile =null;
        if(soundFile != null)
        {
            // Open sound file
            try
            {
                final AudioInputStream stream = AudioSystem.getAudioInputStream(new File("sounds/"+soundFile));
                final DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
                soundClip = (Clip) AudioSystem.getLine(info);
				soundClip.open(stream);
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Plays the sound file
     */
    public static void playSound()
    {
        if(soundClip != null && soundClip.isOpen())
            soundClip.start();
    }
}

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/
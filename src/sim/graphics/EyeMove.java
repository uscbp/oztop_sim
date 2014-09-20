package sim.graphics;


/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class EyeMove extends Thread
{
    private Eye eye;
    private MainPanel cv;
    private boolean stoprequested=false;
    public double dyrot,dzrot,dxrot;

    public EyeMove(final MainPanel cv, final Eye eye)
    {
        this.cv=cv;
        this.eye=eye;
    }

    public void run()
    {
        while(!stoprequested)
        {
            step0();
            try
            {
                sleep(120);
            }
            catch(InterruptedException e)
            {}
        }
        System.out.println("Loop exited. Now stopping...");
    }

    public void startXrot(final double del)
    {
        dxrot=del;
    }

    public void startYrot(final double del)
    {
        dyrot=del;
    }

    public void startZrot(final double del)
    {
        dzrot=del;
    }

    synchronized public void step0()
    {
        if (dxrot!=0)
            eye.XrotateViewPlane(dxrot);
        if (dyrot!=0)
            eye.YrotateViewPlane(dyrot);
        if (dzrot!=0)
            eye.ZrotateViewPlane(dzrot);
        Mars.project();
        cv.paint(cv.getGraphics());
    }

    public void stopSelf()
    {
        System.out.println("Will stop...");
        stoprequested=true;
    }
}

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


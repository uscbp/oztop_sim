package sim.graphics;

import sim.util.Resource;
import sim.main.Main;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class MainPanel extends JPanel
{
    private static final long serialVersionUID = 3257002176755872825L;
    static public int bgColor_R=Resource.getInt("bgColor_R");
    static public int bgColor_G=Resource.getInt("bgColor_G");
    static public int bgColor_B=Resource.getInt("bgColor_B");
    static public int fgColor_R=Resource.getInt("fgColor_R");
    static public int fgColor_G=Resource.getInt("fgColor_G");
    static public int fgColor_B=Resource.getInt("fgColor_B");
    static public Color backColor=new Color(bgColor_R, bgColor_G, bgColor_B);
    static public Color foreColor=new Color(fgColor_R, fgColor_G, fgColor_B);
    public Image buf=null;
    public int bufwi=0, bufhe=0;
    public Graphics bufg;
    public Eye eye;
    public int wi,he,midx,midy;
    static public int IMAGE_FORMAT_GIF=0;
    static public int IMAGE_FORMAT_PPM=1;

    public MainPanel(Eye eye)
    {
        this.eye  =eye;
    }

    public MainPanel()
    {
        this.eye  =Mars.eye;
    }

    public void refreshDisplay()
    {
        Mars.project();
        repaint();
    }

    public void clear()
    {
        Graphics g=getGraphics();
        g.clearRect(0,0,wi,he);
    }

    public void repaint()
    {
        paint(getGraphics());
    }

    public Graphics getGfx()
    {
        return getGraphics();
    }

    public void paint(Graphics g)
    {
        Dimension d=getSize();
        he=d.height;
        wi=d.width;
        midx= wi >> 1;
        midy= he >> 1;
        Mars.midx=midx;
        Mars.midy=midy;
        if (Main.doubleBuffering)
        {
            if (wi!=bufwi || he!=bufhe)
            {
                if (bufg!=null)
                    bufg.dispose();
                buf=null;
                bufg=null;
            }
            if (buf==null)
            {
                if (wi<=0 || he <=0)
                    return;
                bufwi=wi;
                bufhe=he;
                buf=createImage(wi,he);
                bufg=buf.getGraphics();
            }
        }
        else  //no double buffering
        {
            bufwi=wi;
            bufhe=he;
            bufg=g;
        }
        bufg.setColor(backColor);
        bufg.fillRect(0,0,bufwi,bufhe);

        Mars.drawCube(bufg);
        Mars.drawSolid(bufg);
        Mars.drawStars(bufg);

        if (Main.doubleBuffering)
        {
            g.drawImage(buf,0,0,null);
            if (Main.recordCanvas)
            {
                Main.createImage();
            }
        }

    }

    public Image lastFrame()
    {
        if (Main.doubleBuffering)
            return buf;
        System.out.println("Can grab the image only in double buffer mode!!");
        return null;
    }
}


/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


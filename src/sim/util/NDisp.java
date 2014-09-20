package sim.util;

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;


/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 * <hl>
 * Borrowed from my early MNS nsl model tries. It can display arrays 
 * contents in a visual way
 */
public class NDisp extends JFrame implements ActionListener
{
    private static final long serialVersionUID = 3258408443605235507L;
    String mytitle;
    //public double FIRING_MAX = 1.5;
    //public double MEMBRANE_MAX = 3.5;
    //public long[] def_color;
    //public int def_col_count=16;
    private int[] old_he=null;
    private JPanel canvas;
    int Lastncol;
    int Lastnrow;
    int Lastx0  ;
    int Lasty0  ;
    int Lastwi  ;
    int Lastsize;
    boolean Lastdrawn = false;
    Object owner;
    //Region ownerR;
    double[] old_a=null;
    double old_min, old_max;
    long[] old_col;
    int old_size, old_ncol;
    JButton close, draw;

    public NDisp(final Object m, final String title)
    {
        owner=m;
        if (title==null)
            mytitle="Neuron Activity";
        else
            mytitle=title;
        setTitle(mytitle);

        /*
        def_color = new long[def_col_count];
        for (int i=0;i<def_col_count;i++)
        {
            def_color[i]= (230L<<16) + (190L<<8) + (190L); // default is gray
        }
        */

        canvas= new JPanel();
        canvas.setBackground(Color.white);
        add("Center",canvas);
        final JPanel p=new JPanel();
        close=new JButton("Close");
        close.addActionListener(this);
        p.add(close);
        draw=new JButton("Draw");
        draw.addActionListener(this);
        p.add(draw);
        add("South",p);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
        enableEvents(AWTEvent.CONTAINER_EVENT_MASK);
    }

    /*
    public NDisp(final Region R, final Object m, final String title)
    {
        this(m,title);
        System.out.println("Region got by NDISPL");
        ownerR=R;
    }
    */

    /*
    public NDisp()
    {
        this(null,null);
    }
    */

    public void repaint()
    {
        old_he= null;
        if(old_a!=null)
            drawArray(old_a, old_min, old_max, old_col, old_size, old_ncol);

    }

    /*
    public boolean mouseDown(Event e, int x, int y)
    {
        Point p=canvas.getLocation();
        x=x-p.x;  y=y-p.y;

        if (!Lastdrawn)
           return true;
        x-=Lastx0; y-=Lasty0;
        if (x<0 || y<0)
            return true;
        int col= x / (Lastwi+1);
        int row= y / (Lastwi+1);
        if (col>=Lastncol)
            return true;
        if (row>=Lastnrow)
            return true;
        if (row*Lastncol+col>=Lastsize)
            return true;
        return true;
    }
    */

    public void actionPerformed(final ActionEvent e)
    {
        if (e.getActionCommand().equals("Draw") )
        {
            if(old_he!=null)
                repaint();
        }
        else if (e.getActionCommand().equals("Close"))
        {
            setVisible(false);
            if(owner==null)
                System.exit(0);
        }
    }

    protected void processWindowEvent(final WindowEvent e)
    {
        if(e.getID() == WindowEvent.WINDOW_CLOSING)
            setVisible(false);
        else
            repaint();
    }

    protected void processFocusEvent(final FocusEvent e)
    {
        repaint();
    }

    protected void processComponentEvent(final ComponentEvent e)
    {
        repaint();
    }

    protected void processContainerEvent(final ContainerEvent e)
    {
        repaint();
    }

    /*
    public Canvas getCanvas()
    {
        return canvas;
    }
    */

    public void clearCanvas()
    {
        final Graphics  g=canvas.getGraphics();
        final Dimension d=canvas.getSize();
        g.clearRect(0,0,d.width,d.height);
        forceDrawNext();
    }

    public void forceDrawNext()
    {
        old_he=null;
    }

    public void drawArray(final double[] a, final double min, final double max, final long[] col, final int size,
                          final int ncol)
    {
        old_a = a;
        old_min = min;
        old_max = max;
        old_col = col;
        old_size = size;
        old_ncol = ncol;

        int MARGIN;
        double mag;
        int wi,he,x,y,column;
        final int x0,y0,nrow;
        int R,G,B;
        boolean first;
        long v;
        Color c;
        final Graphics  g=canvas.getGraphics();
        final Dimension d=canvas.getSize();

        first=false;

        if (old_he==null)
        {
            old_he=new int[size];
            first=true;
        }

        MARGIN = d.width / 50;
        if ((d.height / 50) <MARGIN)
            MARGIN=d.height/50;

        final double yy=1.0*size/ncol;
        nrow=(int)(yy+0.9999);
        wi=(d.width/2-MARGIN-ncol)/ncol;

        if (wi>((d.height/2-MARGIN-nrow)/nrow))
            wi=(d.height/2-MARGIN-nrow)/nrow;
        wi=wi*2+1;

        column=0;
        x0=(d.width-(wi+1)*ncol)/2;
        y0=(d.height-(wi+1)*nrow)/2;
        x=x0; y=y0;

        Lastncol=ncol;
        Lastnrow=nrow;
        Lastx0  =x0;
        Lasty0  =y0;
        Lastwi  =wi;
        Lastsize=size;
        Lastdrawn=true;

        for (int i=0;i<size;i++)
        {
            mag=((a[i])/(max-min))*wi;
            he=(int)mag;

            if (col==null)
                c=Color.white;
            else
            {
                v=col[i];
                R=(int)(v>>16); v=v%(1<<16);
                G=(int)(v>>8) ; v=v%(1<<8);
                B=(int)v;
                c=new Color(R,G,B);
            }
            if (old_he[i]!=he || first)
                drawSArea(g,x,y,wi,he,c);
            old_he[i]=he;
            x+=wi+1; column++;
            if (column>=ncol)
            {
                column=0; y+=wi+1; x=x0;
            }
        }
    }

    /*
    public void notused_drawSArea0(final Graphics g, final int x, final int y, final int size,int val,Color col)
    {

        if (val<0)
        {
            val=-val;
            col=Color.yellow;
        }
        if (val>size)
        {
            val=size;
            System.err.println("NDisp: overflow (>maxvalue) value truncated to maxvalue");
        }
        g.clearRect(x+1,y+1,size-2,size-2);
        g.setColor(col);

        final int cx=(x+x+size-1)/2;
        final int cy=(y+y+size-1)/2;
        final int r =val/2;
        g.fillRect(cx-r,cy-r,2*r+1,2*r+1);
        g.drawRect(x,y,size-1,size-1);
        g.setColor(Color.red);
        g.drawLine(cx,cy,cx,cy);
    }
    */

    public static void drawSArea(final Graphics g, final int x, final int y, final int size, int val, final Color col)
    {
        boolean neg=false;
        if (val<0)
        {
            val=-val;
            neg=true;
        }
        if (val>size)
        {
            val=size;
            Log.println("NDisp: overflow ("+size+">maxvalue) value truncated to maxvalue");
        }
        g.setColor(col);
        g.fillRect(x,y,size,size);
        g.setColor(Color.red);
        g.drawRect(x,y,size,size);

        g.setColor(Color.black);
        if (neg)
            g.setColor(Color.green);
        else
            g.setColor(Color.black);

        g.fillRect(x+2,y+size-val,size-3,val);
    }

    /*
    public void drawBar(final Graphics g, final int x, int y, final int wi, int he, final Color col)
    {
        if (he<0)
        {
            y+=he-1; he=-he;
        }
        g.setColor(col);
        g.fillRect(x,y,wi,he);
        g.setColor(Color.black);
        g.drawRect(x,y,wi,he);
    }
    */

    public static void main(final String[] args)
    {
        final NDisp f=new NDisp(null,"Self Test");
        f.setSize(400,400);
        f.setVisible(true);
        final int nrows=30;
        final int ncols=30;

        final double[] arr=new double[nrows*ncols];
        for (int i=0;i<nrows; i++)
            for (int j=0;j<ncols;j++)
                arr[i*ncols+j]=Math.cos(0.1*((i-nrows/2.0)*(i-nrows/2.0)+(j-ncols/2.0)*(j-ncols/2.0)))/Math.exp(0.01*((i-nrows/2.0)*(i-nrows/2.0)+(j-ncols/2.0)*(j-ncols/2.0)));
        f.drawArray(arr,0,1,null,arr.length,ncols);
        f.repaint();
    }
} /* End of NDisp */

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


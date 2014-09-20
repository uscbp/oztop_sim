package sim.graphics;

import java.awt.Color;
import java.awt.Graphics;

import sim.util.VA;
import sim.util.Log;
import sim.motor.Graspable;
import sim.main.Main;

/**
 * Object3d - 3d object implementation
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */
public class Object3d
{
    // plane list for this object
    public int[] hindex=null;

    // plane list for this object
    public Plane[] hplist=null;

    // # of items in hplist
    public int hpc=0;

    // center of the object
    public Point3d objectCenter=new Point3d(0,0,0);

    // radius of the sphere containing plane centers
    public double objectRadius=0;

    // whether or not to draw the object
    public boolean noshow=false;

    // Object view depth
    public double objdepth;

    // all segments
    public Segment seg[]=null;

    // number of segments
    public int segc=0;

    // root segment
    public Segment root=null;

    // max 100 sided polygon!
    public int[] xx=new int[100];
    public int[] yy=new int[100];

    public void setPlaneProperties()
    {
        int u=0;
        objectCenter.x=0;
        objectCenter.y=0;
        objectCenter.z=0;
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000)
                continue;

            u++;
            hplist[k].setCenter();
            hplist[k].setGeom();
            VA._add(objectCenter,hplist[k].CP);
        }
        VA._scale(objectCenter,1.0/u);
        objectRadius=0;
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000)
                continue;
            final double r=VA.dist(hplist[k].CP,objectCenter);
            if (r>objectRadius)
                objectRadius =r;
        }
        //choose the normals outward
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000)
                continue;
            hplist[k].adjustCenterSide(objectCenter);
        }
    }

    /**
     * Returns true if the point is inside or on the surface of the object
     * @param p - The point to check
     */
    public boolean inside(final Point3d p)
    {
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000)
                continue;
            final double v=VA.inner(p,hplist[k].normal)+hplist[k].D;
            final double v1=hplist[k].objectCenterValue;
            if (v*v1<0)
                return false;
        }
        return true;
    }

    public static double new_segmentCollision(final Segment seg, final Object3d o)
    {
        if(o!=null)
        {
            if (o.inside(seg.limb_pos))
                return (5000.0/VA.dist(seg.limb_pos,o.objectCenter)+1) ;
            if (o.inside(VA.center(seg.limb_pos,seg.joint_pos)))
                return (5000.0/VA.dist(seg.limb_pos,o.objectCenter)+1) ;

            for (int i=0;i<seg.noch;i++)
            {
                final double v=new_segmentCollision(seg.child[i],o);
                if (v>0)
                    return v;
            }
        }
        return 0;
    }

    public void setupObject()
    {
        seg=root.seg;
        segc=root.segc;
        hplist=root.plane;
        hpc=root.planec;
        hindex=new int[hpc];
        for (int i=0;i<hpc;i++)
            hindex[i]=i;
        setPlaneProperties();
    }

    public void paintAllSurfaces(final int fillc, final int linec)
    {
        for (int i=0;i<hpc;i++)
        {
            hplist[i].fill_color=fillc;
            hplist[i].line_color=linec;
        }
    }

    public boolean onSurface(Point3d p)
    {
        for(int i=0; i<hpc; i++)
        {
            if(hplist[i].contained(p))
                return true;
        }
        return false;
    }

    /**
     * Constructor
     * @param s
     */
    public Object3d(final String s)
    {
        this(s,0,1);
    }

    /**
     * Constructor
     * @param s
     * @param pipesidec
     * @param piperad
     */
    public Object3d(final String s, final int pipesidec, final double piperad)
    {
        //+100 are very bad. I was just trying something remove them
        root=new Segment(s,2*pipesidec+100,15*(pipesidec+3)+100);
        //extra stuff required
        if (pipesidec>0)
            root.setupSolid(pipesidec,piperad,true);  // later will be read from file

        if (root==null)
            Log.println("Cannot create Segment from file:"+s+"!!");
        setupObject();
    }

    public void project(final Eye eye)
    {
        root.project(eye);
    }

    /**
     * assumes hplistp[i][5].x containing the correct depth for planes!!
     */
    public void updateObjectDepth()
    {
        objdepth=0;
        for (int i=0;i<hpc;i++)
                objdepth+=hplist[hindex[i]].depth;
        objdepth/=hpc;
    }

    /**
     * this is a quick cheat to merger objects for sorting planes
     */
    public void sortPlanes()
    {
        int ii,jj;
        double depi,depj;
        root.updateDepth(Mars.eye);
        if(this instanceof Graspable)
            paintAllSurfaces(6,7);
        for (int i=0;i<hpc-1;i++)
        {
            for(int j=i+1;j<hpc;j++)
            {
                ii=hindex[i];
                depi=hplist[ii].depth;
                jj=hindex[j];
                depj=hplist[jj].depth;

                if (depi < depj)
                {
                    final int t=hindex[j];
                    hindex[j]=hindex[i];
                    hindex[i]=t;
                }
            }
        }
    }

    public void drawWire(final Graphics g, final Segment seg)
    {
        if (noshow)
            return;
        g.setColor(MainPanel.foreColor);
        for (int i=0;i<hpc;i++)
        {
            int ii=hindex[i];
            final int N=hplist[ii].N;
            for (int k=0;k<N;k++)
            {
                xx[k]=Mars.midx+hplist[ii].r[k].x;
                yy[k]=Mars.midy-hplist[ii].r[k].y;
            }
            g.drawPolygon(xx,yy,N);
        }
    }

    public void drawWire(final Graphics g)
    {
        drawWire(g,root);
    }

    /*public void drawSolid(final Graphics g, final Segment seg)
    {
        Plane[] HPL;
        int fill,line;

        if (noshow)
            return;
        for (int i=0;i<hpc;i++)
        {
            int ii=hindex[i];
            if(!noshow)
                HPL=hplist;
            else
                continue;
            final int N=HPL[ii].N;
            for (int k=0;k<N;k++)
            {
                xx[k]=Mars.midx+HPL[ii].r[k].x;
                yy[k]=Mars.midy-HPL[ii].r[k].y;
            }
            fill=HPL[ii].fill_color;
            line=HPL[ii].line_color;

            g.setColor(Main.pal.C[fill]);
            g.fillPolygon(xx,yy,N);
            if (line!=-1 )
            {
                g.setColor(Main.pal.C[line]);
                g.drawPolygon(xx,yy,N);
            }
        }
    }

    public void drawSolid(final Graphics g)
    {
        drawSolid(g,root);
    }*/

    public void drawSolid(final Graphics g)
    {
        if(!noshow)
        {
            for (int i=0;i<hpc;i++)
            {
                drawSolid(g, i);
            }
        }
    }

    public void drawSolid(final Graphics g, final int planeIdx)
    {
        int[] xx=new int[hplist[hindex[planeIdx]].N];
        int[] yy=new int[hplist[hindex[planeIdx]].N];
        for (int k=0;k<hplist[hindex[planeIdx]].N;k++)
        {
            xx[k]=Mars.midx+hplist[hindex[planeIdx]].r[k].x;
            yy[k]=Mars.midy-hplist[hindex[planeIdx]].r[k].y;
        }
        int fill=hplist[hindex[planeIdx]].fill_color;
        int line=hplist[hindex[planeIdx]].line_color;

        g.setColor(Main.pal.C[fill]);
        g.fillPolygon(xx,yy,hplist[hindex[planeIdx]].N);
        if (line!=-1 )
        {
            g.setColor(Main.pal.C[line]);
            g.drawPolygon(xx,yy,hplist[hindex[planeIdx]].N);
        }
    }

    public void drawSkel(final Graphics g)
    {
        drawSkel(g,root);
    }

    public void drawSkel(final Graphics g, final Segment seg)
    {
        if (noshow)
            return;
        g.setColor(MainPanel.foreColor);
        g.drawLine(Mars.midx+seg.joint_pos2d.x,Mars.midy-seg.joint_pos2d.y,
                Mars.midx+seg.limb_pos2d.x,Mars.midy-seg.limb_pos2d.y);
        g.drawLine(1+Mars.midx+seg.joint_pos2d.x,Mars.midy-seg.joint_pos2d.y,
                1+Mars.midx+seg.limb_pos2d.x,Mars.midy-seg.limb_pos2d.y);
        g.setColor(Color.green);
        g.drawLine(Mars.midx+seg.joint_pos2d.x,Mars.midy-seg.joint_pos2d.y,
                1+Mars.midx+seg.joint_pos2d.x,Mars.midy-seg.joint_pos2d.y);


        for (int i=0;i<seg.noch;i++)
            drawSkel(g,seg.child[i]);
    }

    /**
     * Moves the object to new rectangular coordinates
     * @param newposition - new rectangular coordinates for object
     */
    synchronized public void rect_moveto(final sim.graphics.Point3d newposition)
    {
        final double dx=newposition.x - objectCenter.x; //root.joint_pos.x;
        final double dy=newposition.y - objectCenter.y;  // root.joint_pos.y;
        final double dz=newposition.z - objectCenter.z; // root.joint_pos.z;
        root._translate(dx,dy,dz);
        setPlaneProperties();
    }

    /**
     * Moves the object to new rectangular coordinates
     * @param x - new x coordinate
     * @param y - new y coordinate
     * @param z - new z coordinate
     */
    synchronized public void rect_moveto(final double x, final double y, final double z)
    {
        final double dx=x - objectCenter.x; // root.joint_pos.x;
        final double dy=y - objectCenter.y;  //root.joint_pos.y;
        final double dz=z - objectCenter.z; // root.joint_pos.z;
        root._translate(dx,dy,dz);
        setPlaneProperties();
    }

    /**
     * Moves the object to new spherical coordinates
     * @param x
     * @param y
     * @param z
     */
    synchronized public void moveto(Point3d center, final double x, final double y, final double z)
    {
        final double xx=center.x + z*Math.cos(x)*Math.sin(y);
        final double yy=center.y + z*Math.sin(x);
        final double zz=center.z + z*Math.cos(x)*Math.cos(y);

        final double dx=xx - objectCenter.x; //root.joint_pos.x;
        final double dy=yy - objectCenter.y; //root.joint_pos.y;
        final double dz=zz - objectCenter.z; //root.joint_pos.z;
        root._translate(dx,dy,dz);
        setPlaneProperties();
    }
}


/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


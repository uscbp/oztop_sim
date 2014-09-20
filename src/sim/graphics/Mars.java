package sim.graphics;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Vector;

import sim.util.Resource;
import sim.util.Log;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class Mars
{
    static public boolean ignoreClear=false;
    static public int midx,midy,wi,he;
    static public final int MAXobjects=100;

    static public String[] objname=new String[MAXobjects];  //object
    static public Object3d[] obj=new Object3d[MAXobjects];  //object
    static public int objc=0;
    static public Eye eye=null;

    static public int debug=0;

    static public int drawCube=Resource.getInt("drawCube");

    static public Point3d prev1=null,prev2,cur1=null,cur2=null;
    static public int pos=0;

    static public Point3d[] cube=null;
    static public Point[] cube2d=null;
    static int[] xx=new int[8];
    static int[] yy=new int[8];
    static int[] xx2=new int[8];
    static int[] yy2=new int[8];
    static private Point temp=new Point();
    //static public Vector<Line> comets=null;
    static public Vector stars1=null;   //currently holds the trajectory points
	static public Vector stars2=null;
    //static public Vector<Point3d> aperture=null;  // currently holds the velocity profile
    //static public Vector<Point3d> velocity=null;  // currently holds the velocity profile
    static public int starc=0;

    static public int addObject(Object3d o)
    {
        return addObject(null,o);
    }

    static public int addObject(String name,Object3d o)
    {
        if (o==null)
        {
            Log.println("graphics.Mars: object list is null!!");
            return -1;
        }
        if (objc>=obj.length)
        {
            Log.println("graphics.Mars: full!!");
            return -1;
        }
        if (name==null)
            objname[objc]=o.root.label;
        else
            objname[objc]=name;
        obj[objc++]=o;
        return objc-1;
    }

    static public void removeObject(String name)
    {
        int idx=0;
        for(int i=0; i<objc; i++)
        {
            if(objname[i].equals(name))
            {
                idx=i;
                break;
            }
        }
        for(int i=idx; i<objc-1; i++)
        {
            objname[i]=objname[i+1];
            obj[i]=obj[i+1];
        }
        objc--;
    }

    static public Object3d getObject(String name)
    {
        for (int i=0;i<objc;i++)
        {
            if (objname[i].equals(name))
                return obj[i];
        }
        System.out.println("Cannot find object named:"+name);
        return null;
    }

    static public synchronized void drawSolid(Graphics g)
    {
        /*for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].sortPlanes();
        }

        sortObjects();
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].drawSolid(g);
        }*/
        int planesToDraw=0;
        int planesDrawn=0;
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
            {
                obj[i].sortPlanes();
                planesToDraw+=obj[i].hpc;
            }
        }
        int[] objPlaneIndices=new int[objc];


        while(planesDrawn<planesToDraw)
        {
            double maxDepth=0;
            int maxDepthObjIdx=-1;
            int maxDepthPlaneIdx=-1;
            for (int i=0;i<objc;i++)
            {
                int j=objPlaneIndices[i];
                if (!obj[i].noshow && j<obj[i].hpc)
                {
                    if(obj[i].hplist[obj[i].hindex[j]].depth>maxDepth)
                    {
                        maxDepth=obj[i].hplist[obj[i].hindex[j]].depth;
                        maxDepthObjIdx=i;
                        maxDepthPlaneIdx=j;
                    }
                }
            }
            if(maxDepthObjIdx>-1 && maxDepthPlaneIdx>-1)
            {
                obj[maxDepthObjIdx].drawSolid(g, maxDepthPlaneIdx);
                objPlaneIndices[maxDepthObjIdx]++;
                planesDrawn++;
            }
            else
                break;
        }
    }

    static public void drawSkel(Graphics g)
    {
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].drawSkel(g);
        }
    }

    static public void drawWire(Graphics g)
    {
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].drawWire(g);
        }
    }

    static public void setEye(Eye eye)
    {
        Mars.eye=eye;
    }

    static public void sortObjects()
    {
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].updateObjectDepth();
        }

        for (int i=0;i<objc-1;i++)
        {
            if (obj[i].noshow)
                continue;
            for (int j=i+1;j<objc;j++)
            {
                if (obj[j].noshow)
                    continue;
                if (obj[i].objdepth<obj[j].objdepth)
                {
                    Object3d t=obj[i]; obj[i]=obj[j]; obj[j]=t;
                    String ss=objname[i]; objname[i]=objname[j]; objname[j]=ss;
                }
            }
        }
    }

    static public void project()
    {
        for (int i=0;i<objc;i++)
        {
            if (!obj[i].noshow)
                obj[i].project(eye);
        }
    }

    static public void setCube(double len)
    {
        len/=2;
        cube=new Point3d[8];
        cube2d=new Point[8];

        cube[0]=new Point3d(-len, len,-len);
        cube[1]=new Point3d(-len,-len,-len);
        cube[2]=new Point3d( len,-len,-len);
        cube[3]=new Point3d( len, len,-len);
        cube[4]=new Point3d(-len, len, len);
        cube[5]=new Point3d(-len,-len, len);
        cube[6]=new Point3d( len,-len, len);
        cube[7]=new Point3d( len, len, len);
        for (int i=0;i<8;i++)
            cube2d[i]=new Point();
    }

    static public void drawCube(Graphics g)
    {
        if (drawCube==0)
            return;
        for (int i=0;i<8;i++)
            eye._project(cube[i],cube2d[i]);
        for (int i=0;i<4;i++)
        {
            xx[i]=midx+cube2d[i].x;
            yy[i]=midy-cube2d[i].y;
        }
        g.setColor(Color.magenta);
        g.drawPolygon(xx,yy,4);
        for (int i=4;i<8;i++)
        {
            xx2[i-4]=midx+cube2d[i].x;
            yy2[i-4]=midy-cube2d[i].y;
        }
        g.drawPolygon(xx2,yy2,4);
        for (int i=0;i<4;i++)
            g.drawLine(xx[i],yy[i],xx2[i],yy2[i]);
    }

/*
    static public Point3d[] getStars()
    {
        Vector<Point3d> v=stars;
        int n=v.size();
        Point3d[] L=new Point3d[n];
        n=0;
        Enumeration<Point3d> e=v.elements();
        while (e.hasMoreElements())
        {
            Point3d p=(Point3d)e.nextElement();
            L[n++]=p.duplicate();
        }
        return L;
    }
*/

    static public void drawStars(Graphics g)
    {
        //drawComets(g);
        if (stars1!=null)
        {
            Enumeration e=stars1.elements();
            while (e.hasMoreElements())
            {
                Point3d p=(Point3d)e.nextElement();
                eye._project(p,temp);
                g.setColor(Color.red);
                g.drawRect(-2+midx+temp.x, -2+midy-temp.y,4,4);
            }
        }
        if (stars2!=null)
        {
            Enumeration e=stars2.elements();
            while (e.hasMoreElements())
            {
                Point3d p=(Point3d)e.nextElement();
                eye._project(p,temp);
                g.setColor(Color.blue);
                g.drawRect(-2+midx+temp.x, -2+midy-temp.y,4,4);
            }
        }
    }
/*
    static public void drawComets(Graphics g)
    {
        if (comets==null)
            return;
        Enumeration<Line> e=comets.elements();
        while (e.hasMoreElements())
        {
            Line ll=(Line)e.nextElement();
            eye._project(ll.P[0],temp0);
            eye._project(ll.P[1],temp1);
            g.setColor(HV.pal.C[ll.line_color]);
            g.drawLine(midx+temp0.x, midy-temp0.y,midx+temp1.x, midy-temp1.y);
        }
    }
*/
/*
    static public void velocityProfile()
    {
        Point3d prev=null,cur=null;
        if (stars==null)
            return;
        int k=0;
        Enumeration<Point3d> e=stars.elements();
        if (e.hasMoreElements())
            prev=(Point3d)e.nextElement();
        while (e.hasMoreElements())
        {
            cur=(Point3d)e.nextElement();
            double dis=VA.dist(prev,cur);
            prev=cur;
            k++;
        }
    }
*/
	
    static public void addStar(Point3d p, int n)
    {
        starc++;
        
		if(n==1)
		{
			cur1=p.duplicate();
			stars1.addElement(cur1);
		}
		else
		{
			cur2=p.duplicate();
			stars2.addElement(cur2);
		}
        if (ignoreClear)
            return; // special case don't plot. See HV.generataData
        pos++;
		
		if(n==1)
			prev1=cur1;
		else
			prev2=cur2;
    }

/*
    static public void addComet(Point3d p0,Point3d p1,int col)
    {
        comets.addElement(new Line(p0.duplicate(), p1.duplicate(),col));
    }

    static public void clearComets()
    {
        if (ignoreClear)
            return;
        comets=new Vector<Line>(40);
    }
*/
    static public int getStarCount()
    {
        return starc;
    }

    static public void clearStars(int n)
    {
        if (ignoreClear)
            return;
		if(n==1)
			stars1=new Vector(40);
		else
			stars2=new Vector(40);
        //aperture=new Vector<Point3d>(40);
        //velocity=new Vector<Point3d>(40);
        prev1=null; prev2=null; cur1=null; cur2=null;
        pos=0;
        starc=0;
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


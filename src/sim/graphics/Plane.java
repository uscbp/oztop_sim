package sim.graphics;


import java.awt.Point;
import java.util.Enumeration;
import java.util.Vector;

import sim.util.VA;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class Plane
{
    public int N;      // # of sides
    public Point[] r;  //projected
    public Point3d[] P;  //3d
    public Point3d CP; // 3d center
    public Point   Cr; // 2d center
    public Point3d normal; //normal
    public double A,B,C,D; // (A,B,C)=normal plane eq: Ax+By+Cz+D=0

    public int fill_color=0;    //
    public int line_color=1;
    public int in_fill_color=2;
    public int in_line_color=1;
    public int texture   =0;
    public double  depth;     // what is the depth wrt an eye

    static public final int side_OUT=1;
    static public final int side_IN =2;
    static public final int side_NONE =0;
    public int side      =side_NONE;  // which side is showing
    public double objectCenterValue=0; // value of the plane function at the object center.
    double tt=0;

    public Plane(Point3d P0,Point3d P1,Point3d P2,Point3d P3, Point r0  ,Point r1  ,Point r2  , Point r3)
    {
        N=4;
        P=new Point3d[N];
        r=new Point[N];
        this.P[0]=P0; this.P[1]=P1; this.P[2]=P2; this.P[3]=P3;
        this.r[0]=r0; this.r[1]=r1; this.r[2]=r2; this.r[3]=r3;
        CP=new Point3d();
        Cr=new Point();
        setCenter();
        setGeom();
    }

    public Plane(Point3d P0,Point3d P1,Point3d P2, Point r0  ,Point r1  ,Point r2)
    {
        N=3;
        P=new Point3d[N];
        r=new Point[N];
        this.P[0]=P0; this.P[1]=P1; this.P[2]=P2;
        this.r[0]=r0; this.r[1]=r1; this.r[2]=r2;
        CP=new Point3d();
        Cr=new Point();
        setCenter();
        setGeom();
    }

    public Plane( Vector v)
    {
        N= v.size() >> 1;
        P=new Point3d[N];
        r=new Point[N];
        int i=0;
        Enumeration e=v.elements();
		while (e.hasMoreElements())
        {
            Point3d p=((Point3d)e.nextElement());
            Point   p2d=((Point)e.nextElement());
            P[i]=p;
            r[i]=p2d;
            i++;
        }
        CP=new Point3d();
        Cr=new Point();
        setCenter();
        setGeom();
    }

    public void set2dCenter()
    {
        Cr.x=0; Cr.y=0;
        for (int i=0;i<N;i++)
        {
            Cr.x+=r[i].x; Cr.y+=r[i].y;
        }
        Cr.x/=N; Cr.y/=N;
    }

    public void setCenter()
    {
        CP.x=0; CP.y=0; CP.z=0;
        for (int i=0;i<N;i++)
        {
            CP.x+=P[i].x; CP.y+=P[i].y; CP.z+=P[i].z;
        }
        CP.x/=N; CP.y/=N; CP.z/=N;
    }

    public void setGeom()
    {
        normal=VA.cross(VA.subtract(CP,P[1]), VA.subtract(P[1],P[0]));
        VA._normalize(normal);
        A=normal.x;
        B=normal.y;
        C=normal.z;
        D=-VA.inner(normal,CP);
    }

    //determine object centers positions wrt plane
    public void adjustCenterSide(Point3d objCenter)
    {
        objectCenterValue=VA.inner(objCenter,normal)+D;
    }

    public String str()
    {
        StringBuilder s=new StringBuilder("Plane[");
        for (int i=0;i<N;i++)
        {
            s.append(P[i].str());
            s.append(' ');
        }
        return s.toString();
    }

    public void setColor(int fill,int line)
    {
        fill_color=fill;
        line_color=line;
    }

    // returns the intersection of the plane with the line passing through
    //   point p with direction vector u.
    //  line is parametricalyy defined as p+u*t
    //  Thus (p+u*t).normal + D = 0 hold to be on the plane
    //  Solve t and the intersection is then p+t*u
    public Point3d intersection(Point3d p, Point3d u)
    {
        setCenter();
        setGeom();
        double bot= VA.inner(normal,u);
        if (bot==0)   // line is either parallel or on the plane
        {
            if (D+VA.inner(normal,p)==0)
            {
                return p.duplicate(); // infinite sol return one
            }
            else
            {
                return null; // no solution.
            }
        }
        double t=-(D+VA.inner(normal,p)) / bot;
        Point3d res=VA.scale(u,t);
        VA._add(res,p);
        return res;
    }

    /** check whether p is contained in the polygon area. Note that p must
     lie in the plane of polygon. The sum of internal angles of a point inside the polygon
     must be 2*pi. If the point is outside this angle will be less than 2pi.
     */
    public boolean contained(Point3d q)
    {
        return contained(q, 0.001);
    }

    /** check whether p is contained in the polygon area. Note that p must
     lie in the plane of polygon. The sum of internal angles of a point inside the polygon
     must be 2*pi. If the point is outside this angle will be less than 2pi.
     */
    public boolean contained(Point3d q, double eta)
    {
        double angsum;
        angsum=0;
        Point3d last=VA.subtract(P[N-1],q);
        for (int i=0;i<N;i++)
        {
            Point3d li=VA.subtract(P[i],q);

            angsum+=VA.acos(last,li);
            last=li;
        }

        return angsum >= 2 * Math.PI - eta;
    }

    /** check whether p is contained in the polygon area. Note that p must
     lie in the plane of polygon.
     Works but slow. Makes many calls to lineintersect....
     */
    public boolean old_contained(Point3d q)
    {
        if (N<=0)
            return false;

        for (int i=0;i<N-1;i++)
        {
            lineintersect(P[i],P[i+1],CP,q);
            if (tt>=0 && tt<=1)
                return false;
        }
        lineintersect(P[N-1],P[0],CP,q);
        return !(tt >= 0 && tt <= 1);
    }

    public static Point3d align(Point3d p,Point3d rot)
    {
        Point3d np =VA.Yrotate(p,rot.y);
        VA._Xrotate(np,rot.x);
        VA._Xrotate(np,Math.PI/2);
        return np;
    }

    // find the intersection of lines A and B
    public Point3d lineintersect(Point3d A0, Point3d A1, Point3d B0, Point3d B1)
    {
        Point3d rot=VA.zap2Y(normal);
        Point3d p0=align(VA.subtract(A0,CP),rot);
        Point3d u0=align(VA.subtract(A1,CP),rot);
        Point3d p1=align(VA.subtract(B0,CP),rot);
        Point3d u1=align(VA.subtract(B1,CP),rot);
        // now we are on the x/y plane
        VA._subtract(u0,p0);
        VA._subtract(u1,p1);
        Point3d cr=intersect2d(p0,u0,p1,u1);
        if (cr==null)
        {
            tt=2;  //may be buggy
            return VA.scale(VA.add(B0,B1),1);
        }
        double t=cr.z;
        tt=t;
        Point3d k=VA.subtract(B1,B0);
        return VA.add(B0,VA.scale(k,t));
    }

    // return t1 of intersection time of second line on z coordinate. Ignore z
    // coord, in input.
    public static Point3d intersect2d(Point3d p0, Point3d u0, Point3d p1, Point3d u1)
    {
        double t1;
        double dx=p1.x-p0.x;
        double dy=p1.y-p0.y;
        
        double bot=u0.x*u1.y - u0.y*u1.x;
        if (bot==0)
            return null; // parallel

        if (u0.y==0)
            t1=-dy/u1.y;
        else if (u0.x==0)
            t1=-dx/u1.x;
        else
            t1=(dx*u0.y - dy*u0.x)/bot;

        Point3d res=VA.scale(u1,t1);
        VA._add(res,p1);
        res.z=t1;
        return res;
    }

    //is the point the same side as center point
    public int sameSide(Point3d p)
    {
        double v;
        v=VA.inner(normal,p)+D;
        if (v>0)
            return -1; //normal side
        else if (v<0)
            return   1; //opposite of normal side
        else
            return 0;
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


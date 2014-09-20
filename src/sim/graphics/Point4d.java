package sim.graphics;

import sim.util.Elib;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Apr 8, 2006
 * Time: 1:34:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Point4d implements Cloneable
{
    /** 3d coordinates */
    public double w,x,y,z;

    /** Creates zero point/vector */
    public Point4d()
    {
        w=0; x=0; y=0; z=0;
    }

    /** Creates the (xx,yy,zz) point/vector */
    public Point4d(double ww,double xx,double yy,double zz)
    {
        w=ww;x=xx; y=yy; z=zz;
    }

    /** Returns the string representation of the point*/
    public String str()
    {
        return ("("+w+','+x+','+y+','+z+')');
    }

    /** Clones itself. Note that a and b fields are not properly cloned.*/
    public Point4d duplicate()
    {
        return new Point4d(w,x,y,z);
    }

    public void set(Point4d p)
    {
        w=p.w;x=p.x; y=p.y; z=p.z;
    }

    public String nstr()
    {
        return ('('+ Elib.snice(w,1e3,6)+','+Elib.snice(x,1e3,6)+','+Elib.snice(y,1e3,6)+','+Elib.snice(z,1e3,6)+')');
    }
}
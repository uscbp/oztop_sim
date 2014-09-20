package sim.graphics;

import sim.util.Elib;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Apr 8, 2006
 * Time: 1:40:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class Point2d implements Cloneable
{
    /** 2d coordinates */
    public double x,y;

    /** Creates zero point/vector */
    public Point2d()
    {
        x=0; y=0;
    }

    /** Creates the (xx,yy) point/vector */
    public Point2d(double xx,double yy)
    {
        x=xx; y=yy;
    }

    /** Returns the string representation of the point*/
    public String str()
    {
        return ("("+x+','+y+')');
    }

    /** Clones itself. Note that a and b fields are not properly cloned.*/
    public Point2d duplicate()
    {
        return new Point2d(x,y);
    }

    public void set(Point2d p)
    {
        x=p.x; y=p.y;
    }

    public String nstr()
    {
        return ('('+ Elib.snice(x,1e3,6)+','+Elib.snice(y,1e3,6)+",)");
    }
}

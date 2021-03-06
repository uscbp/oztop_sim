package sim.graphics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.awt.*;

import sim.util.Elib;
import sim.util.VA;
import sim.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.swing.*;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 * <hl>
 * <b> This class is the basis for joint connected objects. Even though the
 * class hierarcy doesn't show the connection, Object3d is intimately
 * connected to Segment. Currently Object3d can have only one segment but
 * in future implementations Object3d may contain multiple segments.
 * </b>
 * The Segment structure is highly recursive. Most of the functions operate
 * in a recursive fashion. The main idea is a segment is limb attached to
 * a parent via a joint. All the children will affected if a movement occurs
 * in that joint.
 * see <a href="objects/erharm.seg"> the sample <b>.seg </b> file used for
 * constructing segments.</a>
 * @see Object3d
 * @see Point3d
 * @see VA
 */
public class Segment
{
    public static int segIndex=0;

    /** unique number for this segment */
    public int id;
    /** number of children this segment has. */
    public int noch;
    /** Array of child segments. */
    public Segment[] child;
    /** parent segment. It is null for the root segment.*/
    public Segment parent;

    /** used for truncating the kinematics chain and pasting on later.*/
    public int nochSave=-1;
    /** The rotation axis of this segment. null means the joint is fixed.*/
    public Point3d joint_axis;
    /** The 3d location of the end of the segment.*/
    public Point3d limb_pos;
    /** The 3d location of the joint.*/
    public Point3d joint_pos;
    /** The projected position of the joint axis. This is for display purposes.*/
    public Point joint_axis2d;
    /** The projected position of the end of the segment.*/
    public Point limb_pos2d;

    public int userTag=0;
    /** The projected position of the joint.*/
    public Point joint_pos2d;
    /** The total angle the joint rotated in radians. It always reflects the
     * the angle of the joint from the inital created orientation.
     * note that Segment itself does not check the limits. An upper module
     * like Object3d or Hand should check it.
     */
    /* reference frame for this segment. X= limpos-jointpos, Z=joint_axis,
    hiddenLayer=X crossproduct Z. note that X,hiddenLayer,Z are orthanormal.
    */
    public Point3d X=new Point3d(),Y=new Point3d(),Z=new Point3d();
    public double beta,minbeta=-Math.PI,maxbeta= Math.PI;
    public boolean jointconstraint=false;  //is not monitored by Segment

    /** When the joint cant turn, e.g. hit to object */
    public boolean blocked=false;
    /**  The force the joint receives. Force system not implemented yet.
     */
    public double torque=0;
    public double force=0 ;
    /** the following two are the components of the force when acting on a
     rigid body */
    public double linForce=0;
    public double rotForce=0;
    /**  The label of the segment. This label will be filled form the .seg file
     *  and can be used to get refrence to the segment.
     */
    public String label;
    /**  The is the radius used to extend the skeletal joint system into solid
     *    cylindirical system.
     */
    public double Rad  =5;
    /** The view depth of this segment (no effect of children) imposed.
     */
    public double segmentdepth=0;
    /**  The is the # of sides used to extend the skeletal joint system into solid
     *    cylindirical system. 4 will make extend the skeletal system into
     * rectangular prisims.
     */
    public int sidec=0;

    /** All the segments under the topmost. Valid only for the topmost segment.
     *  The list also includes the topmost as the 0th element. The order in seg[]
     *  correspondances to the ID's of the segments. Also this is the appearance
     *  order in .seg file
     */
    public Segment[] seg=null;
    public int segc=0;
    /** Array of planes. Only the topmost of a segment should use this field. */
    public Plane[] plane;
    /** # of planes in this segment. */
    public int planec=0;
    /** # of total planes as itself+descendants -not sure of validity- */
    public int totalplanec=0;   //itself+descendants
    /** Array of 3d points for this segment */
    public Point3d[] pool=null;
    /** Array of (projected) 2d points for this segment */
    public Point[] pool2d=null;
    /** number of points in the pool[] */
    public int poolc=0;

    /** The segments which will not be draw are marked with this one. This happnes
     when you need multiple axis on one point (like the wrist). You just
     want the rotations not to draw the joint. It is a nice trick to simulate
     ball joints. By default all the limb-joint will be drawn.
     */
    public boolean nodraw=false;

    public int lwid=0;

    /** .seg file suggested Eye parameters */
    public double  suggested_scale=0;
    /** .seg file suggested Eye parameters */
    public double  suggested_F=0;
    /** .seg file suggested Eye parameters */
    public double  suggested_Fz=0;

    /** Field number for parent in .seg file */
    private static final int PAR=12;

    public JPanel seg_pan=null;
    public JPanel savePan=null; //for enable disable
    public JSlider beta_sb=null;
    public JLabel     beta_txt=null;
    public JLabel     beta_lb=null;
    public JButton    beta_bt=null;
    public int dbeta=0;

    /** Create the specified Segment
     * see constructSegment
     */
    public Segment(String lb,int ident,int MAXchild,Point3d jpos,Point3d lpos,Point3d ax, int axistype, int pointcount, int planecount)
    {
        constructSegment(lb,ident,MAXchild,jpos,lpos,ax, axistype,pointcount,planecount);
    }

    /** Create the specified Segment. Point and plane counts are loaded defaults.
     * see constructSegment
     */
    public Segment(String lb,int ident,int MAXchild,Point3d jpos,Point3d lpos,Point3d ax,int axistype)
    {
        constructSegment(lb,ident,MAXchild,jpos,lpos,ax,axistype);
    }

    // note that only top-down traverse will get truncated.
    public void truncateChildren()
    {
        nochSave=noch;
        noch=0;
    }

    public void restoreChildren()
    {
        if (nochSave!=-1)
        {
            noch=nochSave;
            nochSave=-1;
        }
        else
        {
            Log.println("Nothing to restore!!");
        }
    }

    public void _updateFrame()
    {
        //System.out.println("Frame for:"+label);
        if (joint_axis==null)
        {
            Z=new Point3d(0,0,1);
            Y=new Point3d(0,1,0);
            X=new Point3d(1,0,0);
            return;
        }
        Z=VA.normalize(joint_axis.duplicate());
        X=VA.normalize(VA.subtract(limb_pos,joint_pos));
        Y=VA.cross(X,Z);
    }

    public void updateFrame()
    {
        _updateFrame();
        for (int i=0;i<noch;i++)
            child[i].updateFrame();
    }

    public void pullTo(Point3d target)
    {
        Point3d f=VA.subtract(target,limb_pos);
        applyForce(f);
        if (label.equals("WRISTz"))
            return;
        for (int i=0;i<noch;i++)
            child[i].pullTo(target);
    }

    // make it additive and provide clearForce
    public void applyForce(Point3d F)
    {
        _updateFrame();
        if (joint_axis==null)
        {
            force=0;
        }
        else
        {
            Point3d f=VA.normalize(F);
            Point3d torque=VA.cross(f,X);
            double dir=VA.inner(torque,Z);
            force=dir*10;
        }
    }

    public void moveSegment(double rate)
    {
        double delta=rate*force;
        if (delta>0.1)
            delta=0.1;
        if (delta<-0.1)
            delta=-0.1;
        if (joint_axis!=null)
            if (beta<2*Math.PI && beta>-2*Math.PI)
                rotateJoint(delta);
        for (int i=0;i<noch;i++)
            child[i].moveSegment(rate);
    }

    private int outpointOBJ()
    {
        System.out.println("OBJ-VECT "+"v "+limb_pos.x+" "+limb_pos.y+" "+limb_pos.z);
        lwid++;
        return lwid;
    }

    private void outlineOBJ(int son,int par)
    {
        System.out.println("OBJ-LINE "+"l "+son+" "+par);
    }

    public void outputOBJ()
    {
        int i=_outputOBJ(null,0,0);
        Log.println("LW object output done.["+i+" points.]");
    }

    private int _outputOBJ(Segment parent, int parentlwid,int lwidfromup)
    {
        int mylwid;
        int k;
        lwid=lwidfromup;
        if (parent==null)
        {
            lwid=0;
            mylwid=outpointOBJ();
        }
        else
        {
            mylwid=outpointOBJ();
            outlineOBJ(mylwid,parentlwid);
        }
        k=lwid;
        for (int i=0;i<noch;i++)
        {
            k=child[i]._outputOBJ(this,mylwid,k);
        }
        return k;
    }

    public void unBlock()
    {
        blocked=false;
        for (int i=0;i<noch;i++)
            child[i].unBlock();
    }

    public void setupSolid(int c,double R,boolean recursive)
    {
        setupSolid(this,c,R,recursive);
    }

    /** This method extends the skeletal system (joint_pos-limb_pos) to a
     * cylinderical form. First argument specifies the number of sides, the
     * next is the radius of the cylinder. The recursive option let's the
     * function to descent into the children segments with same parameters
     */
    public void setupSolid(Segment root, int c,double R,boolean recursive)
    {
        if (joint_axis!=null && !nodraw)
        {
            sidec=c;
            Rad=R;
            if (label.equals("WRISTz"))
                Rad*=2;
            else if (label.equals("J3"))
                Rad*=2;

            int[][] cor=new int[sidec+1][2];
            int corc=0;

            // axis may not be perp. to limb sometimes. Get a perp. offset for solidify
            Point3d limbdir=VA.normalize(VA.subtract(limb_pos,joint_pos));
            Point3d perpoff=VA.cross(joint_axis,limbdir);
            perpoff=VA.normalize(VA.cross(limbdir,perpoff));

            Point3d offset=VA.scale(perpoff,Rad);
            Point3d gap=VA.scale(limbdir,2);

            Point3d stem=VA.add(joint_pos,offset); VA._add(stem,gap);
            Point3d tip =VA.add(limb_pos,offset);
            double pie=Math.PI*2/sidec;
            for (int i=0;i<sidec;i++)
            {
                Point3d ps=VA.Lrotate(stem,joint_pos,limb_pos,i*pie);
                Point3d pt=VA.Lrotate(tip,joint_pos,limb_pos,i*pie);
                cor[corc][0]= add2pool(ps,new Point(0,0));
                cor[corc++][1]=add2pool(pt,new Point(0,0));
            }
            cor[corc][0]=cor[0][0]; //close it
            cor[corc][1]=cor[0][1]; //close it

            int texture;
            int viewside=0;
            for (int i=0;i<corc;i+=1)
            {
                int fill=9;
                int line=4;
                texture=1;
                Point3d normal=VA.normal(pool[cor[i][0]],pool[cor[i][1]],pool[cor[i+1][1]]);
                if (normal.y<0) {fill=8; line=5; texture=0;}

                root.addPlane(pool[cor[i][0]],pool[cor[i][1]],pool[cor[i+1][1]],pool[cor[i+1][0]],
                        pool2d[cor[i][0]],pool2d[cor[i][1]],pool2d[cor[i+1][1]],pool2d[cor[i+1][0]],
                        fill,line,viewside,texture);
            }

            Vector v1=new Vector(10);
            Vector v2=new Vector(10);
            for (int i=0;i<corc;i++)
            {
                v1.addElement(pool[cor[i][0]]);
                v1.addElement(pool2d[cor[i][0]]);
                v2.addElement(pool[cor[i][1]]);
                v2.addElement(pool2d[cor[i][1]]);
            }

            root.addPlane(v1,2,2,0,-1);
            root.addPlane(v2,2,2,0,-1);
        }
        if (recursive)
            for (int i=0;i<noch;i++)
                child[i].setupSolid(root,c,R,recursive);
    }

    public void setupPool(Vector pnts)
    {
        setupPool(pnts,0);
    }

    public void setupPool(Vector pnts,int extra)
    {
        int maxpool;
        if (pnts==null) maxpool=extra;
        else maxpool=extra+pnts.size();
        pool  = new Point3d[maxpool];
        pool2d= new Point  [maxpool];
        poolc=0;
    }

    public void setupPlane(int nplane,int extra)
    {
        plane = new Plane[nplane+extra] ; planec = 0;
    }

    public void setupPlane(int nplane)
    {
        setupPlane(nplane,0);
    }

    public void limbpoints2pool(Vector v)
    {
        limbpoints2pool(v,new Point3d(0,0,0));
    }

    /** Adds the points defined in the segment file into the point pool. */
    public void limbpoints2pool(Vector v, Point3d orig)
    {
        if (v==null)
            return;
        Enumeration e=v.elements();
        while (e.hasMoreElements())
        {
            Point3d pk=((Point3d)e.nextElement());
            add2pool(VA.add(orig,pk), new Point(0,0));
        }
    }

    public void updateGeom()
    {
        for (int i=0;i<planec;i++)
        {
            plane[i].setCenter();
            plane[i].setGeom();
        }
    }

    public void updateDepth(Eye eye)
    {
        int txt;
        segmentdepth=0;
        for (int i=0;i<planec;i++)
        {
            plane[i].setCenter();

            int fillc; int linec=-1;
            Point3d u1=VA.subtract(plane[i].P[0],plane[i].P[1]);
            Point3d u2=VA.subtract(plane[i].P[0],plane[i].P[2]);
            Point3d pr=VA.cross(u1,u2);
            double shade = VA.cos(pr,eye.Z); //light source is on the eye
            txt=plane[i].texture;
            if (txt==0)
            {
                fillc=20+(int)(0.5+31*Math.abs(shade));
            }
            else
            {
                fillc=20+32+(int)(0.5+31*Math.abs(shade));
            }
            plane[i].fill_color=fillc;
            plane[i].line_color=linec;

            double depth=VA.dist(plane[i].CP,eye.Fpos);
            segmentdepth+=depth;
            plane[i].depth=depth;
        }
        segmentdepth/=planec;
    }

    public void vplanes2plane(Vector v)
    {
        if (v==null)
            return;
        Vector cvec;
        Enumeration pp=v.elements();
        while ( pp.hasMoreElements())
        {
            int[] pl=(int[])pp.nextElement();
            cvec=new Vector(pl.length);
            for (int i=0;i<pl.length;i+=2)
            {
                cvec.addElement(seg[pl[i]].pool[pl[i+1]]);
                cvec.addElement(seg[pl[i]].pool2d[pl[i+1]]);
            }
            addPlane(cvec,5,7);
        }
    }

    /** Returns and SETS the <b>totalplanec</b> of the segments */
    public int setTotalplanec()
    {
        totalplanec=planec;
        for (int i=0;i<noch;i++)
            totalplanec+=child[i].setTotalplanec();
        return totalplanec;
    }

    /** calls constructSegment with 40 points and 20 planes. -check code- */
    public void constructSegment(String lb,int ident,int MAXchild,Point3d jpos,Point3d lpos,Point3d ax,int axistype)
    {
        constructSegment(lb,ident,MAXchild,jpos,lpos,ax,40,20,axistype);
    }

    /** Sets up the variables and the arrays required to hold this segment.*/
    public void constructSegment(String lb,int ident,int MAXchild,Point3d jpos,Point3d lpos,Point3d ax,int MAXpoint, int MAXplane,int axistype)
    {
        label=lb;
        id=ident;
        child=new Segment[MAXchild];
        parent=null;
        limb_pos=lpos;
        joint_pos=jpos;
        if (VA.norm(ax)==0 || axistype==0)
            joint_axis=null; // fixed joint
        else if (axistype==-1)
            joint_axis=VA.cross(ax,VA.subtract(jpos,lpos));
        else joint_axis=ax;

        if (VA.dist(joint_pos,limb_pos)<0.0000001)
            nodraw=true;
        limb_pos2d=new Point(0,0);
        joint_pos2d=new Point(0,0);
        joint_axis2d=new Point(0,0);
        noch=0;
        beta=0;
        if (seg_pan!=null)
            update_panel();
    }

    /** This is the constructor used to create a segment (and its descendants!) from
     * a .seg file.
     * see readSegment
     */
    public Segment(String s)
    {
        this(s,0,0);
    }

    public Segment(String s,int extrapool,int extraplane)
    {
        Segment sg=null;
        if(s.endsWith(".seg"))
            sg=readSegment(s,extrapool, extraplane);
        else if(s.endsWith(".xml"))
            sg=readSegmentXML(s, extrapool, extraplane);
        if (sg==null)
            Log.println("Cannot create segment from file:"+s+"!!");
    }

    /** Add the kid segment to this segment */
    public void addChild(Segment kid)
    {
        child[noch++]=kid;
        kid.parent=this;
    }

    /** Add 3d and corresponding 2d point to the point pool. */
    public int add2pool(Point3d p,Point pp)
    {
        pool[poolc++]=p;
        pool2d[poolc-1]=pp;
        return poolc-1;
    }

    /** Add the plane defined by vector of 3d,2d pairs */
    public int addPlane(Vector v)
    {
        return addPlane(v,0,1);
    }

    /** Add the plane defined by vector of 3d,2d pairs */
    public int addPlane(Vector v,int fillcol,int linecol)
    {
        Plane p=new Plane(v);
        p.setColor(fillcol,linecol);
        plane[planec++]=p;
        return planec-1;
    }

    /** Add the plane defined by vector of 3d,2d pairs */
    public int addPlane(Vector v,int fillcol,int linecol, int side,int texture)
    {
        Plane p=new Plane(v);
        p.setColor(fillcol,linecol);
        p.texture=texture;
        p.side=side;
        plane[planec++]=p;
        return planec-1;
    }

    /** Add the plane given by pointer  (from pool[]) with default color. */
    public int addPlane(Point3d P0,Point3d P1,Point3d P2,Point3d P3,
                        Point r0  ,Point r1  ,Point r2  , Point r3)
    {
        return addPlane(P0,P1,P2,P3,r0,r1,r2,r3,1,0);
    }

    /** Add the plane given by by pointer (from pool[]) with given fill
     * and line color.
     */
    public int addPlane(Point3d P0,Point3d P1,Point3d P2,Point3d P3,
                        Point r0  ,Point r1  ,Point r2  , Point r3,int fillcol,int linecol)
    {
        Plane p=new Plane(P0,P1,P2,P3,r0,r1,r2,r3);
        p.setColor(fillcol,linecol);
        plane[planec++]=p;
        return planec-1;
    }

    public int addPlane(Point3d P0,Point3d P1,Point3d P2,Point3d P3,
                        Point r0  ,Point r1  ,Point r2  , Point r3,int fillcol,int linecol,int side,int texture)
    {
        Plane p=new Plane(P0,P1,P2,P3,r0,r1,r2,r3);
        p.setColor(fillcol,linecol);
        p.texture=texture;
        p.side=side;
        plane[planec++]=p;
        return planec-1;
    }

    /** Recursively reset the joint angles to zero. It does the required rotations
     * to achive this.
     */
    public void resetJoints()
    {
        if (beta!=0)
            if (joint_axis!=null)
                rotateJoint(-beta);
        for (int i=0;i<noch;i++)
            child[i].resetJoints();
    }

    /** Returns a vector of segments including this and its descendants.*/
    public Vector list()
    {
        Vector v=new Vector(40);
        _list(v,this);
        return(v);
    }

    /** Used by <b>list()</b> to fetch the segments and its descendants.*/
    private void _list(Vector v, Segment seg)
    {
        v.addElement(seg);
        for (int i=0;i<seg.noch;i++)
            _list(v,seg.child[i]);
    }

    /** Searches the segment for the labeled segment and returns the refrence.*/
    public Segment getJoint(String lb)
    {
        if (label.equals(lb))
            return this;
        else
            for (int i=0;i<noch;i++)
            {
                Segment r= child[i].getJoint(lb);
                if (r!=null)
                    return r;
            }
        return null;
    }

    public void setuserTag(int what, boolean recursive)
    {
        userTag=what;
        if (recursive)
            for (int i=0;i<noch;i++)
                child[i].setuserTag(what,true);
    }

    public int getuserTaget()
    {
        return userTag;
    }

    /** Recersively prints the segment.*/
    private void recprint(Segment seg)
    {
        System.out.println("POOLC:"+seg.poolc);
        for (int i=0;i<seg.poolc;i++)
            System.out.println("pool["+i+"]:"+seg.pool[i].str());
        System.out.println(seg.str());
        for (int i=0;i<seg.noch;i++)
            recprint(seg.child[i]);
    }

    /** Recersively prints the joint angles. This way it gives the state of the
     * joint object.
     */
    public void printJointAngles()
    {
        if (joint_axis==null)
            Log.println(label+" NO JOINT");
        else
            Log.println(label+" : "+ (int)(100*beta*180/Math.PI)/100.0+" deg.");
        for (int i=0;i<noch;i++)
            child[i].printJointAngles();
    }

    /** Recersively prints the segment.
     * see recprint
     */
    public void print()
    {
        Log.println("*PRE-ORDER TRAVERSAL*");
        recprint(this);
    }

    /** returns the cosine of the angle between this limb and the other */
    public double cosineTo(Segment other)
    {
        Point3d mine=VA.subtract(limb_pos,joint_pos);
        Point3d its =VA.subtract(other.limb_pos,other.joint_pos);
        return VA.cos(its,mine);
    }

    /** returns the cosine of the angle between this projected limb and the other */
    public double cosineTo2d(Segment other)
    {
        Point3d ml=VA.promote3d(limb_pos2d);
        Point3d mj=VA.promote3d(joint_pos2d);
        Point3d ol=VA.promote3d(other.limb_pos2d);
        Point3d oj=VA.promote3d(other.joint_pos2d);

        Point3d mine=VA.subtract(ml,mj);
        Point3d its =VA.subtract(ol,oj);
        return VA.cos(its,mine);
    }

    /** Returns a string describing the segment. No child recursion is done. */
    public String str()
    {
        int pid;
        String jos;
        if (parent==null)
            pid=0;
        else
            pid=parent.id;
        if (joint_axis==null)
            jos="*NULL*";
        else
            jos=joint_axis.str();
        return label+" ID:"+id+" cc:"+noch+" jpos:"+joint_pos.str()+" lpos:"+limb_pos.str()+" rot axis:"+jos+" PAR:"+pid;
    }

    // for now eye can only be on Z axis
    /** project the joint_axis using eye. Normally project_axis need
     * not be projected.
     */
    public void projectaxis(Eye eye)
    {
        Point3d p=joint_axis;
        if (p==null)
            return;
        double x=joint_pos.x+50*p.x;
        double y=joint_pos.y+50*p.y;
        double z=joint_pos.z+50*p.x;
        eye._project(new Point3d(x,y,z),joint_axis2d);
    }

    /** Project the pool/plane system points to pool2d. Note that the skeletal
     * projection is seperate from this process.
     */
    public void projectSolid(Eye eye)
    {
        for (int i=0;i<poolc;i++)
        {
            eye._project(pool[i] ,pool2d[i]);
        }
    }

    /** Project limb_pos, joint_pos (the skeletal system) using eye. Note that
     solid system is seperate.
     */
    public void project(Eye eye)
    {
        eye._project(limb_pos ,limb_pos2d);
        eye._project(joint_pos,joint_pos2d);
        projectaxis(eye);
        projectSolid(eye);

        for (int i=0;i<noch;i++)
            child[i].project(eye);
    }

    /** This method rotates this+descendant segments joints by T radians.*/
    public void rotateEachJoint(double T)
    {
        if (joint_axis!=null)
            rotateJoint(T);
        for (int i=0;i<noch;i++)
            child[i].rotateEachJoint(T);
    }

    /** This method rotates this segment's joint by T radians. The descending
     * segments are adjusted recursively using _rotateLimb and _rotateJoint.
     * see _rotateLimb
     * see _rotateJoint
     */
    public void updateAllPanel()
    {
        if (seg_pan!=null)
            update_panel();
        for (int i=0;i<noch;i++)
            child[i].updateAllPanel( );
    }

    public void disablePanel()
    {
        savePan=seg_pan;
        seg_pan=null;
    }

    public void enablePanel()
    {
        if (savePan==null)
            return;
        seg_pan=savePan;
        savePan=null;
    }

    public void rotateJoint(double T)
    {
        if (joint_axis==null)
            return;
        beta+=T;
        if (seg_pan!=null)
            update_panel();

        //let's rotate the limbs connected to the joint first
        _rotateLimb(joint_pos,joint_axis,T);

        // now lets rotate the joint connected to this joint
        for (int i=0;i<noch;i++)
            child[i]._rotateJoint(joint_pos,joint_axis,T);
    }

    /** This method is called to rotate a JOINT because of a parent segment
     * rotation. The method adjust the axis and position of the joint.
     * the rotate command is recursivelt transmitted to children
     */
    public void _rotateJoint(Point3d place, Point3d around, double T)
    {
        //first rotate the axis of the  joint
        VA._translate(joint_axis,joint_pos.x-place.x,joint_pos.y-place.y,joint_pos.z-place.z);
        VA._rotate(joint_axis,around,T);
        VA._translate(joint_axis,place.x,place.y,place.z);

        //now rotate joint_pos (the connecting parts)
        VA._translate(joint_pos,-place.x,-place.y,-place.z);
        VA._rotate(joint_pos,around,T);
        VA._translate(joint_pos,place.x,place.y,place.z);
        VA._subtract(joint_axis,joint_pos);

        for (int i=0;i<noch;i++)
            child[i]._rotateJoint(place,around,T);
    }

    /** This method rotates the solid system (pool[]). This is NOT recursive. */
    private void _rotateSolid(Point3d place,Point3d around,double T)
    {                      //tip
        //System.out.println("rotate solid for:"+label);
        for (int i=0;i<poolc;i++)
        {
            VA._translate(pool[i],-place.x,-place.y,-place.z);
            VA._rotate(pool[i],around,T);
            VA._translate(pool[i],place.x,place.y,place.z);
        }
    }

    /** This method rotates the limb (limb_pos) around the joint. If there is
     any solid system a call to _rotateSolid is made. Then the method
     recurses down to the children.
     */
    public void _rotateLimb(Point3d place,Point3d around,double T)
    {                      //tip
        VA._translate(limb_pos,-place.x,-place.y,-place.z);
        VA._rotate(limb_pos,around,T);
        VA._translate(limb_pos,place.x,place.y,place.z);

        //System.out.println("_ratateLimb:name,poolc:"+label+","+poolc);
        if (poolc!=0)
            _rotateSolid(place,around,T);
        for (int i=0;i<noch;i++)
            child[i]._rotateLimb(place,around,T);
    }

    /** Moves the segment (and descendants) to a new position */
    public void movetoy(Point3d newposition)
    {
        double dx=newposition.x - joint_pos.x;
        double dy=newposition.y - joint_pos.y;
        double dz=newposition.z - joint_pos.z;
        _translate(dx,dy,dz);
    }

    public void _translate(Point3d delta) {
        _translate(delta.x,delta.y,delta.z);
    }

    /** Translates the segment and its descendant by dx,dy,dz. */
    public void _translate(double dx,double dy, double dz)
    {
        joint_pos.x+=dx;
        joint_pos.y+=dy;
        joint_pos.z+=dz;
        limb_pos.x+=dx;
        limb_pos.y+=dy;
        limb_pos.z+=dz;
        for (int i=0;i<poolc;i++)
        {
            pool[i].x+=dx;
            pool[i].y+=dy;
            pool[i].z+=dz;
        }
        for (int i=0;i<noch;i++)
            child[i]._translate(dx,dy,dz);
    }


    public void mirror()
    {
        limb_pos.x*=-1;
        joint_pos.x*=-1;
        for (int i=0;i<poolc;i++)
            pool[i].x*=-1;
        for (int i=0;i<noch;i++)
            child[i].mirror();
    }

    /** Scales the segment and its descendant by sc. */
    public void scale(double sc)
    {
        joint_pos.x*=sc;
        joint_pos.y*=sc;
        joint_pos.z*=sc;
        limb_pos.x*=sc;
        limb_pos.y*=sc;
        limb_pos.z*=sc;
        for (int i=0;i<poolc;i++)
        {
            pool[i].x*=sc;
            pool[i].y*=sc;
            pool[i].z*=sc;
        }
        for (int i=0;i<noch;i++)
            child[i].scale(sc);
    }

    /** Scales the segment and its descendant by sc taking place as the origin. */
    public void scale(Point3d place,double sc)
    {
        _translate(-place.x,-place.y,-place.z);
        joint_pos.x*=sc;
        joint_pos.y*=sc;
        joint_pos.z*=sc;
        limb_pos.x*=sc;
        limb_pos.y*=sc;
        limb_pos.z*=sc;
        for (int i=0;i<poolc;i++)
        {
            pool[i].x*=sc;
            pool[i].y*=sc;
            pool[i].z*=sc;
        }
        _translate(place.x,place.y,place.z);
        for (int i=0;i<noch;i++)
            child[i].scale(place,sc);
    }

    /** returns the length of this segment |limb_pos-joint_pos|. */
    double limblen()
    {
        return VA.dist(limb_pos,joint_pos);
    }

    /** rotate segment tree around X axis.*/
    public void Xrot(double t)
    {
        VA._Xrotate(joint_pos,t);
        VA._Xrotate(limb_pos,t);
        VA._Xrotate(joint_axis,t);
        for (int i=0;i<poolc;i++)
            VA._Xrotate(pool[i],t);
        for (int i=0;i<noch;i++)
            child[i].Xrot(t);
    }

    /** rotate segment tree around hiddenLayer axis.*/
    public void Yrot(double t)
    {
        VA._Yrotate(joint_pos,t);
        VA._Yrotate(limb_pos,t);
        VA._Yrotate(joint_axis,t);
        for (int i=0;i<poolc;i++)
            VA._Yrotate(pool[i],t);
        for (int i=0;i<noch;i++)
            child[i].Yrot(t);
    }

    /** rotate segment tree around Z axis.*/
    public void Zrot(double t)
    {
        VA._Zrotate(joint_pos,t);
        VA._Zrotate(limb_pos,t);
        VA._Zrotate(joint_axis,t);
        for (int i=0;i<poolc;i++)
            VA._Zrotate(pool[i],t);
        for (int i=0;i<noch;i++)
            child[i].Zrot(t);
    }

    private boolean easy_num(String u)
    {
        return (u.charAt(0) >= '0' && u.charAt(0) <= '9') ||
                u.charAt(0) == '-' || u.charAt(0) == '+';
    }

    private int resolve(String u,Vector vlimbs, Vector labels)
    {
        Enumeration v=vlimbs.elements();
        Enumeration l=labels.elements();
        int findid=-1;
        while (l.hasMoreElements())
        {
            String ss=(String)l.nextElement();
            double[] dd=(double[])v.nextElement();
            if (ss.equals(u))
            {
                findid=(int)(0.001+dd[0]);
                break;
            }
        }
        return findid;
    }

    private int resolve(String u)
    {
        int findid=-1;
        for(int i=0; i<segc; i++)
        {
            if(seg[i].label.equals(u))
            {
                findid=seg[i].id;
                break;
            }
        }
        return findid;
    }

    /**  open file for read. Should be moved to util.Elib. */
    static public DataInputStream openfileREAD(String fn) throws IOException
    {
        return new DataInputStream(new FileInputStream(fn));
    }

    /**  open file for write. Should be moved to util.Elib. */
    static public DataOutputStream openfileWRITE(String fn) throws IOException
    {
        return new DataOutputStream(new FileOutputStream(fn));
    }

    public Segment readSegmentXML(String fn, int extrapool, int extraplane)
    {
        Vector vplanes =new Vector(40);
        int allplanes=0;  // all planes over the all segments

        suggested_scale=0;
        suggested_F=0;
        suggested_Fz=0;

        int[] plbuf=new int[40]   ; //plane buffer of quadriples of (seg,poolix)

        try
        {
            Document segmentConfig = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fn);
            Element root = segmentConfig.getDocumentElement();
            NodeList children = root.getChildNodes();
            for(int i=0; i<children.getLength(); i++)
            {
                Node child = children.item(i);
                if(child.getNodeName().equals("JointNumber"))
                {
                    if(child.getChildNodes().getLength()>0)
                    {
                        segc=Integer.parseInt(child.getChildNodes().item(0).getNodeValue());
                        seg=new Segment[segc];
                        for (int j=0;j<segc;j++)
                            seg[j]=null;
                    }
                }
                else if(child.getNodeName().equals("Eye"))
                {
                    NodeList eyeChildren = child.getChildNodes();
                    for(int j=0; j<eyeChildren.getLength(); j++)
                    {
                        Node eyeChild = eyeChildren.item(j);
                        if(eyeChild.getNodeName().equals("Fz"))
                        {
                            if(eyeChild.getChildNodes().getLength()>0)
                                suggested_Fz=Double.parseDouble(eyeChild.getChildNodes().item(0).getNodeValue());
                        }
                        else if(eyeChild.getNodeName().equals("F"))
                        {
                            if(eyeChild.getChildNodes().getLength()>0)
                                suggested_F=Double.parseDouble(eyeChild.getChildNodes().item(0).getNodeValue());
                        }
                        else if(eyeChild.getNodeName().equals("Scale"))
                        {
                            if(eyeChild.getChildNodes().getLength()>0)
                                suggested_scale=Double.parseDouble(eyeChild.getChildNodes().item(0).getNodeValue());
                        }
                    }
                }
                else if(child.getNodeName().equals("Joints"))
                {
                    segIndex=0;
                    NodeList joints = child.getChildNodes();
                    for(int j=0; j<joints.getLength(); j++)
                    {
                        Node joint = joints.item(j);
                        if(joint.getNodeName().equals("Joint"))
                        {
                            readJoint(joint, extrapool, null);
                        }
                    }
                }
                else if(child.getNodeName().equals("Planes"))
                {
                    NodeList planeNodes = child.getChildNodes();
                    for(int j=0; j<planeNodes.getLength(); j++)
                    {
                        Node planeNode = planeNodes.item(j);
                        if(planeNode.getNodeName().equals("Plane"))
                        {
                            NodeList planeChildren = planeNode.getChildNodes();
                            for(int k=0; k<planeChildren.getLength(); k++)
                            {
                                Node planeChild = planeChildren.item(k);
                                if(planeChild.getNodeName().equals("Segments"))
                                {
                                    int segmentIdx=0;
                                    NodeList segments = planeChild.getChildNodes();
                                    for(int l=0; l<segments.getLength(); l++)
                                    {
                                        Node segment = segments.item(l);
                                        if(segment.getNodeName().equals("Segment"))
                                        {
                                            String name="";
                                            int index=-1;
                                            NodeList segmentChildren = segment.getChildNodes();
                                            for(int m=0; m<segmentChildren.getLength(); m++)
                                            {
                                                Node segmentChild = segmentChildren.item(m);
                                                if(segmentChild.getNodeName().equals("Name"))
                                                {
                                                    if(segmentChild.getChildNodes().getLength()>0)
                                                        name=segmentChild.getChildNodes().item(0).getNodeValue();
                                                }
                                                else if(segmentChild.getNodeName().equals("PoolIndex"))
                                                {
                                                    if(segmentChild.getChildNodes().getLength()>0)
                                                        index=Integer.parseInt(segmentChild.getChildNodes().item(0).getNodeValue());
                                                }
                                            }
                                            plbuf[segmentIdx++]=resolve(name);
                                            plbuf[segmentIdx++]=index;
                                        }
                                    }
                                    if (segmentIdx>=6)
                                    {
                                        int[] tt=new int[segmentIdx];
                                        System.arraycopy(plbuf,0,tt,0,segmentIdx);
                                        vplanes.addElement(tt);
                                        allplanes++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch(ParserConfigurationException e)
        {}
        catch(IOException e)
        {}
        catch(SAXException e)
        {}

        setupPlane(allplanes,extraplane); // create the plane holders
        vplanes2plane(vplanes);

        return this;
    }

    private static void read3dCoord(Node jointChild, Point3d jointPos)
    {
        NodeList jointPosChildren = jointChild.getChildNodes();
        for(int l=0; l<jointPosChildren.getLength(); l++)
        {
            Node jointPosChild = jointPosChildren.item(l);
            if(jointPosChild.getNodeName().equals("x"))
            {
                if(jointPosChild.getChildNodes().getLength()>0)
                    jointPos.x=Double.parseDouble(jointPosChild.getChildNodes().item(0).getNodeValue());
            }
            else if(jointPosChild.getNodeName().equals("y"))
            {
                if(jointPosChild.getChildNodes().getLength()>0)
                    jointPos.y=Double.parseDouble(jointPosChild.getChildNodes().item(0).getNodeValue());
            }
            else if(jointPosChild.getNodeName().equals("z"))
            {
                if(jointPosChild.getChildNodes().getLength()>0)
                    jointPos.z=Double.parseDouble(jointPosChild.getChildNodes().item(0).getNodeValue());
            }
        }
    }

    public Segment readJoint(Node joint, int extrapool, Point3d parentLimbPos)
    {
        String name="";
        Point3d jointPos=new Point3d();
        Point3d limbPos=new Point3d();
        Point3d jointAxis=new Point3d();
        int axisType=0;
        int maxChildren=0;
        Vector points=new Vector();
        NodeList jointChildren = joint.getChildNodes();
        Vector children=new Vector();
        int index=segIndex++;
        for(int k=0; k<jointChildren.getLength(); k++)
        {
            Node jointChild = jointChildren.item(k);
            if(jointChild.getNodeName().equals("Name"))
            {
                if(jointChild.getChildNodes().getLength()>0)
                    name=jointChild.getChildNodes().item(0).getNodeValue();
            }
            else if(jointChild.getNodeName().equals("JointPos"))
            {
                read3dCoord(jointChild, jointPos);
                if(parentLimbPos!=null)
                {
                    jointPos.x+=parentLimbPos.x;
                    jointPos.y+=parentLimbPos.y;
                    jointPos.z+=parentLimbPos.z;
                }
            }
            else if(jointChild.getNodeName().equals("LimbPos"))
            {
                read3dCoord(jointChild, limbPos);
                if(parentLimbPos!=null)
                {
                    limbPos.x+=parentLimbPos.x;
                    limbPos.y+=parentLimbPos.y;
                    limbPos.z+=parentLimbPos.z;
                }
            }
            else if(jointChild.getNodeName().equals("JointAxis"))
            {
                read3dCoord(jointChild, jointAxis);
            }
            else if(jointChild.getNodeName().equals("AxisType"))
            {
                if(jointChild.getChildNodes().getLength()>0)
                    axisType=Integer.parseInt(jointChild.getChildNodes().item(0).getNodeValue());
            }
            else if(jointChild.getNodeName().equals("Points"))
            {
                NodeList pointNodes = jointChild.getChildNodes();
                for(int l=0; l<pointNodes.getLength(); l++)
                {
                    Node pointNode = pointNodes.item(l);
                    if(pointNode.getNodeName().equals("Point"))
                    {
                        if(pointNode.getChildNodes().getLength()>0)
                        {
                            Point3d point = new Point3d();
                            read3dCoord(pointNode, point);
                            points.add(point);
                        }
                    }
                }
            }
            else if(jointChild.getNodeName().equals("Joints"))
            {
                for(int l=0; l<jointChild.getChildNodes().getLength(); l++)
                {
                    Node jointNode = jointChild.getChildNodes().item(l);
                    if(jointNode.getNodeName().equals("Joint"))
                    {
                        maxChildren++;
                        Segment child = readJoint(jointNode, extrapool, limbPos);
                        if(child!=null)
                            children.add(child);
                    }
                }
            }
        }
        if(parentLimbPos!=null)
        {
            Segment segment = new Segment(name,index,maxChildren,jointPos,limbPos,jointAxis,
                    axisType);
            segment.setupPool(points,extrapool); // create the pool holders
            Point3d plp = new Point3d(0,0,0);
            if(parentLimbPos!=null)
                plp = parentLimbPos.duplicate();
            segment.limbpoints2pool(points,plp);
            seg[segment.id]=segment;
            for(int i=0; i<children.size(); i++)
                segment.addChild((Segment)children.get(i));
            return segment;
        }
        else
        {
            constructSegment(name,index,maxChildren,jointPos,limbPos,jointAxis,
                    axisType);
            setupPool(points,extrapool); // create the pool holders
            limbpoints2pool(points,new Point3d(0,0,0));
            seg[this.id]=this;
            for(int i=0; i<children.size(); i++)
                addChild((Segment)children.get(i));
            return null;
        }
    }

    /** Reads a .seg file and creates and returns a segment build from the file.*/
    public Segment readSegment(String fn,int extrapool,int extraplane)
    {
        Vector vpoints=new Vector(40);
        //noinspection MismatchedQueryAndUpdateOfCollection
        Vector vlines =new Vector(40);
        Vector vplanes =new Vector(40);

        Vector limbpointlistvector=new Vector(40);
        Vector vlimbs =new Vector(40);
        Vector labels =new Vector(40);
        String label="";

        int limbsadded=0;
        int allplanes=0;  // all planes over the all segments
        boolean nopoints=false;

        suggested_scale=0;
        suggested_F=0;
        suggested_Fz=0;

        double[] pbuf=new double[3]; //point buffer
        int[]    lbuf=new int[3]   ; //line buffer
        int[]    plbuf=new int[40]   ; //plane buffer of quadriples of (seg,poolix)
        double[]    limbbuf=new double[PAR]   ; //limb buffer
        int tc,linec, mode=0;
        String s,u;
        boolean added;
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(Elib.openURLfile(null,fn)));
            if (in==null)
                return null ;
            linec=0;
            while (null!=(s=in.readLine()))
            {
                linec++;
                if (s.equals(""))
                    continue;
                if (s.charAt(0) == '#')
                    continue;
                StringTokenizer st= new StringTokenizer(s," ");
                tc=0;
                added=false;
                while (st.hasMoreTokens())
                {
                    u = st.nextToken();

                    if (u.charAt(0)=='=')
                        continue; //don't see this token
                    if (u.charAt(0)=='#')
                        break;   //ignore the rest of the line

                    if (u.equals("Eye"))
                    {
                        suggested_Fz  = Elib.toDouble(st.nextToken());
                        suggested_F   = Elib.toDouble(st.nextToken());
                        suggested_scale=Elib.toDouble(st.nextToken());
                        continue;
                    }

                    if (u.equals("Points"))
                    {
                        if (mode!=4)
                            Log.println("Points must follow segment definitions. graphics.Line:"+linec);
                        else
                            mode=1;
                        continue;
                    }
                    if (u.equals("EndPoints"))
                    {
                        mode=5;
                        continue;
                    }
                    if (u.equals("Lines"))
                    {
                        mode=2;
                        continue;
                    }
                    if (u.equals("Planes"))
                    {
                        if (mode!=4)
                            Log.println("Planes can not be within segment definitions. graphics.Line:"+linec);
                        else
                            mode=3;
                        continue;
                    }
                    if (u.equals("Limbs"))
                    {
                        mode=4;
                        continue;
                    }
                    if (mode==1)
                        pbuf[tc]=Elib.toDouble(u);
                    else if (mode==2)
                        lbuf[tc]=Elib.toInt(u);
                    else if (mode==3)
                    {
                        if (easy_num(u))
                            plbuf[tc]=Elib.toInt(u);
                        else
                        {
                            plbuf[tc]=resolve(u,vlimbs,labels);
                            if (plbuf[tc]<0)
                                Log.println("Cannot find the reference : "+u+" at line "+linec);
                        }
                    }
                    else if (mode==4)
                    {
                        if (tc==0)
                        {
                            label=u;
                            limbbuf[tc]=limbsadded++;
                        }
                        else if (easy_num(u))
                            limbbuf[tc]=Elib.toDouble(u);
                        else // it is a label find the corresponding id
                        {
                            limbbuf[tc]=resolve(u,vlimbs,labels);
                            if (limbbuf[tc]<0)
                                Log.println("Cannot find the reference : "+u+" at line "+linec);
                       }
                    }
                    added=true; tc++;
                }
                if (mode==5)
                {
                    limbpointlistvector.addElement(vpoints);
                    //need to start new vector for the next segment
                    vpoints=new Vector(40);
                    mode=4;
                    nopoints=false;
                    continue;
                }
                if (!added)
                    continue;
                if (mode==1)
                {
                    if (tc==3)
                        vpoints.addElement(new Point3d(pbuf[0],pbuf[1],pbuf[2]));
                    else
                        Log.println("point description wrong in : "+fn+" line "+linec);
                }
                if (mode==2)
                {
                    if (tc==2)
                        vlines.addElement(new Line3d(lbuf[0]-1,lbuf[1]-1));
                    else
                        Log.println("line description wrong in : "+fn+" line "+linec);
                }
                if (mode==3)
                {
                    if (tc>=6)
                    {
                        int[] tt=new int[tc];
                        System.arraycopy(plbuf,0,tt,0,tc);
                        vplanes.addElement(tt);
                        allplanes++;
                    }
                    else
                        Log.println("plane description wrong in : "+fn+" line "+linec);
                }
                if (mode==4)
                {
                    if (tc==PAR)
                    {
                        if (nopoints)  // if previous segment had no points
                            limbpointlistvector.addElement(null);
                        nopoints=true;  // no points for  me yet
                        vlimbs.addElement(limbbuf); limbbuf=new double[tc];
                        labels.addElement(label);
                    }
                    else
                        Log.println("limb description wrong in : "+fn+" line "+linec);
                }
            }
            in.close();
        }
        catch (IOException e)
        {
            Log.println(fn+" : Viewer.readShape() : EXCEPTION "+e);
        }

        if (nopoints)  // if previous segment had no points
            limbpointlistvector.addElement(null);

        //------------------------------
        // let's create Limb tree now
        setupPlane(allplanes,extraplane); // create the plane holders
        segc=vlimbs.size();
        seg=new Segment[segc];
        for (int i=0;i<segc;i++)
            seg[i]=null;
        Enumeration e=vlimbs.elements();
        Enumeration g=labels.elements();
        Enumeration v=limbpointlistvector.elements();
        while ( e.hasMoreElements())
        {
            int c=0;
            String lb    = (String)g.nextElement();
            double[] par = (double[])e.nextElement();
            Vector pnts  = (Vector)v.nextElement();

            Enumeration f=vlimbs.elements();
            while ( f.hasMoreElements())
            {
                double[] ch=(double[])f.nextElement();
                if (par[0]==ch[PAR-1]) c++;
            } //now the segment par has c children

            if (par[PAR-1]<0)   // if the root segment
            {
                constructSegment(lb,(int)par[0],c,new Point3d(par[1],par[2],par[3]),
                    new Point3d(par[4],par[5],par[6]),new Point3d(par[7],par[8],par[9]),
                    (int)par[PAR-2]); //axistype
                setupPool(pnts,extrapool); // create the pool holders
                limbpoints2pool(pnts,new Point3d(0,0,0));
                seg[this.id]=this;
            }
            else
            {
                //find this guys parent
                Segment ps=null;
                if (par[PAR-1]>=segc)
                    Log.println("No such parent for :"+lb);
                else
                    ps=seg[(int)par[PAR-1]];

                if (ps==null)
                    Log.println("No parent defined for:"+lb);

                Segment kid=new Segment(lb,(int)par[0],c,
                                        new Point3d(ps.limb_pos.x+par[1],ps.limb_pos.y+par[2],ps.limb_pos.z+par[3]),
                                        new Point3d(ps.limb_pos.x+par[4],ps.limb_pos.y+par[5],ps.limb_pos.z+par[6]),
                                        new Point3d(par[7],par[8],par[9]), (int)par[PAR-2]); //axistype

                kid.setupPool(pnts,extrapool); // create the pool holders
                kid.limbpoints2pool(pnts,ps.limb_pos);

                ps.addChild(kid);
                seg[kid.id]=kid;
            }
        }

        vplanes2plane(vplanes);
        return this;
    }

    public void unmakePanel()
    {
        beta_sb=null;
        beta_txt=null;
        beta_lb=null;
        beta_bt=null;
        seg_pan=null;  // this will ensure no updating will be done with beta change
    }

    public JPanel makePanel(String s)
    {
        if (joint_axis==null)
            return null;
        JPanel p=new JPanel();
        p.setLayout(new GridLayout(1,2));
        beta_lb =new JLabel(id+")"+label+" ("+dbeta/10.0+" degrees.)",JLabel.LEFT);
        //beta_sb =new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, -180*10 ,180*10+1);
        beta_sb=new JSlider(JSlider.HORIZONTAL,-180*10 ,180*10+1,0);
        beta_sb.setBackground(Color.white);
        beta_sb.setMajorTickSpacing(50);
        beta_sb.setMinorTickSpacing(10);

        p.add(beta_lb);
        p.add(beta_sb);

        update_panel();
        seg_pan=p;
        seg_pan.repaint();

        return p;
    }

    public void update_panel()
    {
        dbeta=(int)(beta*1800/Math.PI);
        if (dbeta>180*10)
            dbeta-=360*10;
        if(beta_lb!=null)
            beta_lb.setText(label+" ("+dbeta/10.0+" degrees.)") ;
        if(beta_sb!=null)
            beta_sb.setValue(dbeta);
    }
}

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/



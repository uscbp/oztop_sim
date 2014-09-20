package sim.motor;

import sim.graphics.Object3d;
import sim.graphics.Plane;
import sim.graphics.Point3d;
import sim.graphics.Segment;
import sim.util.Resource;
import sim.util.VA;
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

public class Graspable extends Object3d
{
    public Point3d X,Y,Z;
    public Point3d tilt=new Point3d(0,0,0);
    public Point3d target[]=null;  //limb targets
    public String myname="noname";
    public double objsize=0;  // emulates the object affordance. NetworkInterface uses this

    // later this will be a list of affordance classes
    static public final String[] grasps={"PRECISION", "SIDE", "POWER", "SLAP"};
    static public final int graspc=grasps.length;
    public int affordance=-1;
    public Point3d lastopposition=null;
    public int tIndex[]=null;
    public int tIc=0;

    //graphics.Point3d wristTarget=new graphics.Point3d(0,0,0);
    //public int[] thumb={-1,-1,-1,-1};
    //public int[] index={-1,-1,-1};
    //public int[] middle={-1,-1,-1};
    //public int[] ring={-1,-1,-1};
    //public int[] pinky={-1,-1,-1};
    //public int wrist=-1;
    //public int thumbTip=-1;
    //public int[][] fingers=new int[6][4];

    static public final int NONE=0;
    static public final int PRECISION=1;
    static public final int POWER=3;
    static public final int SLAP=4;
    static public final int SIDE=2;
    static public final int REACHOFFSET=6;
    static final int REACHOFFSETBYNAME=7;

    public int lastPlan=NONE;

    public double GRASP_TH=200; //reach to grasp transition error margin
    public double  RATE_TH=0.025; //was0.05
    public double BASERATE=0.0002; //

    // Whether or not the object is currently visible to the observer
    public boolean visible=true;

    static public Point3d tilter=new Point3d(0,0,0);

    public double[] init_cfg=null;

    public boolean avoidObjects=true;

    static public double softconGAIN; //500;
    static public final int softcon_J2_45=1;
    static public final int softcon_J2_60=2;

    static public Point3d[] sPN = new Point3d[5];
    static public Plane[] sPL = new Plane[5];

    public boolean obstacle=false;

    public Graspable(final ArmHand hand, final String name, final int sidec, final int rad, final String afs)
    {
        super(name,sidec,rad);
        Graspable.softconGAIN=Resource.get("softconGAIN");
        myname=name;
        tIndex=new int[hand.segc];
        tIc=0;
        //setupHandValues(hand);
        setupObjFrame();
        if (afs!=null)
            setAffordance(afs);
    }

    public void setupObjFrame()
    {
        //fixed for now
        X=new Point3d(1,0,0);
        Y=new Point3d(0,1,0);
        Z=new Point3d(0,0,1);
    }

    public void setGraspablePlanes()
    {
        int u=0;
        objectCenter.x=0;
        objectCenter.y=0;
        objectCenter.z=0;
        for (int i=0;i<hpc;i++)
        {
            u++;
            hplist[i].setCenter();
            hplist[i].setGeom();
            VA._add(objectCenter,hplist[i].CP);
        }
        VA._scale(objectCenter,1.0/u);
    }

    synchronized public void setTilt(final double t)
    {
        resetRot();
        tilter.z=t;
        Zrot(tilter.z);
    }

    synchronized void setTilt(Point3d p)
    {
        resetRot();
        Zrot(p.z);
    }

    synchronized public void Zrot(final double t)
    {
        setGraspablePlanes();
        tilt.z += t;

        final Point3d cen=objectCenter;
        root._translate(VA.scale(cen,-1));
        root.Zrot(t);
        VA._Zrotate(X,t); VA._Zrotate(Y,t); VA._Zrotate(Z,t);
        root._translate(cen);
    }

    synchronized public  void resetRot()
    {
        Zrot(-tilt.z);
    }

    public void setAffordance(final String s)
    {
        int k=-1;
        for (int i=0;i<graspc;i++)
            if (s.equals(grasps[i]))
                k=i;
        if (k==-1)
        {
            Log.println("No such grasp!");
            return;
        }
        affordance=k;
    }

    static public String getGrasp(final int i)
    {
        if (i<0)
            return "NONE";
        return grasps[i];
    }

    public void computeNaturalAffordance (final ArmHand hand)
    {
        if (affordance<0)
            computePrecisionPinchAffordance(hand); //default
        if (grasps[affordance].equals("PRECISION"))
            computePrecisionPinchAffordance(hand);
        else if (grasps[affordance].equals("SIDE"))
            computeSideGraspAffordance(hand);
        else if (grasps[affordance].equals("POWER"))
            computePowerGraspAffordance(hand);
        else if (grasps[affordance].equals("SLAP"))
            computeSlapAffordance(hand);
        else
        {
            Log.println("internal error: No such grasp!");
        }
    }

    public void computeAffordance(final String graspType, final ArmHand hand)
    {
        if(graspType.equals("PRECISION"))
            computePrecisionPinchAffordance(hand);
        else if(graspType.equals("POWER"))
            computePowerGraspAffordance(hand);
        else if(graspType.equals("SIDE"))
            computeSideGraspAffordance(hand);
        else if(graspType.equals("SLAP"))
            computeSlapAffordance(hand);
        else
            computeNaturalAffordance(hand);
    }

    /*
    public void setupHandValues(final ArmHand hand)
    {
        wrist=hand.getSegmentIndex("WRISTx");
        fingers[0][0] = thumb[0]=hand.getSegmentIndex("THUMB");
        fingers[0][1] = thumb[1]=hand.getSegmentIndex("THUMBin");
        fingers[0][2] = thumb[2]=hand.getSegmentIndex("THUMB2");
        fingers[0][3] = thumb[3]=hand.getSegmentIndex("THUMB3");

        if (thumb[3]>=0)
            thumbTip=thumb[3];
        else
            thumbTip=thumb[2];

        fingers[1][1] = index[0]=hand.getSegmentIndex("INDEX");
        fingers[1][2] = index[1]=hand.getSegmentIndex("INDEX1");
        fingers[1][3] = index[2]=hand.getSegmentIndex("INDEX2");

        fingers[2][1] = middle[0]=hand.getSegmentIndex("MIDDLE");
        fingers[2][2] = middle[1]=hand.getSegmentIndex("MIDDLE1");
        fingers[2][3] = middle[2]=hand.getSegmentIndex("MIDDLE2");

        fingers[3][1] = ring[0]=hand.getSegmentIndex("RING");
        fingers[3][2] = ring[1]=hand.getSegmentIndex("RING1");
        fingers[3][3] = ring[2]=hand.getSegmentIndex("RING2");

        fingers[4][1] = pinky[0]=hand.getSegmentIndex("PINKY");
        fingers[4][2] = pinky[1]=hand.getSegmentIndex("PINKY1");
        fingers[4][3] = pinky[2]=hand.getSegmentIndex("PINKY2");
    }
    */

    public synchronized void reachoffset(Point3d offset, int wristIndex)
    {
        Log.dprintln("Planning offset reach...", Main.DLEV);
        lastPlan=REACHOFFSET;
        GRASP_TH=2; BASERATE=0.01; RATE_TH=0.1;
        resetAvoidance();

        setPlaneProperties();

        target=new Point3d[3];  // target angles to be achived

        tIc=0;                  //  the joints that corresponds to target
        tIndex[tIc++]=wristIndex; // index[0]; // was middle[0]
        target[0]=VA.add(objectCenter,offset);
        // may need to add orientation targets too!!
        // hand flies open to the target and a touch initiates a closing.
        // try this for babble learning.
        // issue: initial orientation of hand or online corrections ?
        // what bout bottom and top grasps ?
    }

    public synchronized void reachoffsetByName(String s, ArmHand hand)
    {
        reachoffsetByName(s,new Point3d(0,0,0), hand);
    }

    synchronized void reachoffsetByName(String s, Point3d offset, ArmHand hand)
    {
        Log.dprintln("Planning offset reach ByName...", Main.DLEV);
        lastPlan=REACHOFFSETBYNAME;
        GRASP_TH=2; BASERATE=0.01; RATE_TH=0.1;
        resetAvoidance();

        setPlaneProperties();

        target=new Point3d[3];  // target angles to be achived

        tIc=0;                  //  the joints that corresponds to target
        tIndex[tIc]=hand.getSegmentIndex("INDEX2");    //default
        if (s.equals("INDEX2"))   tIndex[tIc++]=hand.getSegmentIndex("INDEX2");
        if (s.equals("INDEX1"))   tIndex[tIc++]=hand.getSegmentIndex("INDEX1");
        if (s.equals("INDEX0"))   tIndex[tIc++]=hand.getSegmentIndex("INDEX");
        if (s.equals("MIDDLE2"))  tIndex[tIc++]=hand.getSegmentIndex("MIDDLE2");
        if (s.equals("MIDDLE1"))  tIndex[tIc++]=hand.getSegmentIndex("MIDDLE1");
        if (s.equals("MIDDLE0"))  tIndex[tIc++]=hand.getSegmentIndex("MIDDLE");
        if (s.equals("THUMBTIP"))
        {
            if(hand.getSegmentIndex("THUMB3")>=0)
                tIndex[tIc++]=hand.getSegmentIndex("THUMB3");
            else
                tIndex[tIc++]=hand.getSegmentIndex("THUMB2");
        }
        if (s.equals("THUMB0")) tIndex[tIc++]=hand.getSegmentIndex("THUMB");
        if (s.equals("THUMB1")) tIndex[tIc++]=hand.getSegmentIndex("THUMB1");
        if (s.equals("THUMB2")) tIndex[tIc++]=hand.getSegmentIndex("THUMB2");
        target[0]=VA.add(objectCenter,offset);
        // may need to add orientation targets too!!
        // hand flies open to the target and a touch initiates a closing.
        // try this for babble learning.
        // issue: initial orientation of hand or online corrections ?
        // what bout bottom and top grasps ?
    }

    public static Point3d estimateWrist(final Point3d P0, final Point3d P1, final double offset, final ArmHand hand)
    {
        final Point3d dP =VA.subtract(P1,P0);
        final Point3d handdir=VA.subtract(hand.wristx.limb_pos,P0);
        final Point3d t1=VA.cross(handdir,dP);
        final Point3d wr=VA.normalize(VA.cross(dP,t1));
        VA._scale(wr,offset);
        return VA.add(wr,VA.center(P0,P1));
    }

    public void computeSideGraspAffordance(final ArmHand hand)
    {
        if (hand.getSegmentIndex("INDEX1")<0 || hand.getSegmentIndex("THUMB3")<0 ||
                hand.getSegmentIndex("WRISTx")<0)
            return;
        Log.dprintln("Planning side....", Main.DLEV);
        lastPlan=SIDE;
        GRASP_TH=200; BASERATE=0.05; RATE_TH=0.1;
        setAvoidance();
        final Plane[] PL=new Plane[6];
        final Point3d[] PN=new Point3d[6];

        setPlaneProperties();

        target=new Point3d[3];  // target angles to be achived

        tIc=0;                  //  the joints that corresponds to target
        tIndex[tIc++]=hand.getSegmentIndex("THUMB3");
        tIndex[tIc++]=hand.getSegmentIndex("INDEX1");
        tIndex[tIc++]=hand.getSegmentIndex("WRISTx");

        if (myname.equals("mug.seg"))
        {
            PN[0]=root.limb_pos.duplicate();
            PN[0].x-=50;
            PN[1]=PN[0].duplicate();
            PN[1].y-=50;

            Main.toggleTrace(hand.armHandNumber);
        }
        else
        {
            final Point3d ORG=VA.add(root.limb_pos,root.joint_pos);
            VA._scale(ORG,0.5);

            Main.toggleTrace(hand.armHandNumber);

            final Point3d opposition;
            //actually I should pick the thinnest side
            // else opposition=new graphics.Point3d(1,0,0);
            opposition=VA.normalize(new Point3d(0,1,0));
            lineIntersection(ORG, opposition, PL, PN);
            lineIntersection(PN[0].duplicate(), PL[0].normal, PL, PN);

            final Point3d ppp;
            final Plane pppl;
            if (PN[0].y < PN[1].y) //thumb up
            {
                ppp =PN[0]; PN[0]=PN[1]; PN[1]=ppp;
                pppl=PL[0]; PL[0]=PL[1]; PL[1]=pppl;
            }
            VA._resize(PN[0],PN[1],1.5);

        }
        lastopposition=VA.normalize(VA.subtract(PN[0],PN[1]));

        PN[2]=estimateWrist(PN[0],PN[1],260,hand);
        PN[2].y-=100;
        if (myname.equals("mug.seg"))
            PN[2]=VA.add(PN[0],new Point3d(200,0,0));
        Log.println(PN[0].str()+','+PN[1].str()+','+ PN[2].str());

        target=PN;

        objsize=VA.dist(target[1],target[0]);
        //Main.toggleTrace(hand.armHandNumber);
        Main.refreshDisplay();
    }

    public void computePrecisionPinchAffordance(final ArmHand hand)
    {
        if (hand.getSegmentIndex("INDEX2")<0 || (hand.getSegmentIndex("THUMB3")<0 &&
                hand.getSegmentIndex("THUMB2")<0))
            return ;
        Log.dprintln("Planning precision...", Main.DLEV);
        lastPlan=PRECISION;
        GRASP_TH=50; BASERATE=0.04; RATE_TH=0.25;
        resetAvoidance();
        final Plane[] PL=new Plane[6];
        final Point3d[] PN=new Point3d[6];
        final Point3d handdir;

        setPlaneProperties();

        target=new Point3d[3];  // target angles to be achived

        tIc=0;                  //  the joints that corresponds to target
        if(hand.getSegmentIndex("THUMB3")<0)
            tIndex[tIc++]=hand.getSegmentIndex("THUMB2");
        else
        tIndex[tIc++]=hand.getSegmentIndex("THUMB3");
        tIndex[tIc++]=hand.getSegmentIndex("INDEX2");
        tIndex[tIc++]=hand.getSegmentIndex("WRISTx");

        Point3d ORG=objectCenter;

        //Main.toggleTrace(hand.armHandNumber);

        final Point3d opposition;
        if (ORG.x<0)
            opposition=Z.duplicate();
        else
            opposition=X.duplicate();
        lineIntersection(ORG, opposition, PL, PN);

        final Point3d dP;
        final Point3d ppp;
        final Plane pppl;
        if (ORG.x<0  && PN[0].z>PN[1].z  || ORG.x>=0 && PN[0].x>PN[1].x)
        {
            ppp =PN[0]; PN[0]=PN[1]; PN[1]=ppp;
            pppl=PL[0]; PL[0]=PL[1]; PL[1]=pppl;
        }

        VA._resize(PN[0],PN[1],1.4);
        lastopposition=VA.normalize(VA.subtract(PN[0],PN[1]));

        dP = VA.subtract(PN[1],PN[0]);
        handdir=VA.subtract(hand.wristx.limb_pos,PN[0]);
        final Point3d t1=VA.cross(handdir,dP);
        final Point3d wr=VA.normalize(VA.cross(dP,t1));
        VA._scale(wr,250);
        PN[2]=VA.add(wr,VA.center(PN[0],PN[1]));

        target=PN;

        objsize=VA.dist(target[0],target[1]);
        //Main.toggleTrace(hand.armHandNumber);
    }

    public void computePowerGraspAffordance(final ArmHand hand)
    {
        if (hand.getSegmentIndex("RING")<0 || hand.getSegmentIndex("THUMB2")<0)
            return ;

        lastPlan=POWER;
        GRASP_TH=40; BASERATE=0.03; RATE_TH=0.15;
        final Plane[] PL=new Plane[6];
        final Point3d[] PN=new Point3d[6];

        //Avoid objects
        setAvoidance();

        //Turn trace on
        //Main.toggleTrace(hand.armHandNumber);

        //Compute object center and radius
        setPlaneProperties();

        //target angles to be achived
        target=new Point3d[3];
        //the joints that corresponds to target
        tIc=0;
        //ring knuckle
        tIndex[tIc++]=hand.getSegmentIndex("RING");
        //thumb joint
        tIndex[tIc++]=hand.getSegmentIndex("THUMB2");

        // Origin
        final Point3d ORG=VA.add(root.limb_pos,root.joint_pos);
        VA._scale(ORG,0.5);

        /**
         * Find ring finger intersection
         */
        // Opposition axis
        Point3d opposition;
        if (ORG.x<0)
            opposition=VA.normalize(new Point3d(1,1.13,0));
        else
            opposition=VA.normalize(new Point3d(0,1.13,-1));

        // Get intersection of object with line going through origin in direction of opposition axis
        lineIntersection(ORG, opposition, PL, PN);
        VA._resize(PN[0],PN[1],1.4);

        final Point3d tempRi;
        if (PN[0].z-PN[0].y < PN[1].z-PN[1].y)
            tempRi=VA.subtract(PN[0],ORG);
        else
            tempRi=VA.subtract(PN[1],ORG);
        Log.println(" ring Intersection:"+tempRi.str());

        // Find thumb intersection
        // Opposition axis
        if (ORG.x<0)
            opposition=VA.normalize(new Point3d(0,0,-1));
        else
            opposition=VA.normalize(new Point3d(-1,-1.13,0));

        lineIntersection(ORG, opposition, PL, PN);
        VA._resize(PN[0],PN[1],1.4);

        final Point3d tempT;
        if (PN[0].x+PN[0].z < PN[1].x+PN[1].z)
            tempT=VA.subtract(PN[0],ORG);
        else
            tempT=VA.subtract(PN[1],ORG);
        Log.println(" thumb Intersection:"+tempT.str());

        int pnc=0;
        PN[pnc++]=tempRi;
        PN[pnc++]=tempT;

        lastopposition=VA.normalize(VA.subtract(PN[0],PN[1]));

        if (pnc!=tIc)
        {
            Log.println("POWER grasp error!");
            System.exit(0);
        }

        for (int i=0;i<pnc;i++)
            VA._add(PN[i],ORG);
        target=PN;

        objsize=estimateSize();
        Main.toggleTrace(hand.armHandNumber);
        Main.refreshDisplay();
    }

    public void computeSlapAffordance(final ArmHand hand)
    {
        if (hand.getSegmentIndex("RING")<0)
            return ;

        lastPlan=SLAP;
        GRASP_TH=40; BASERATE=0.03; RATE_TH=0.15;
        final Plane[] PL=new Plane[6];
        final Point3d[] PN=new Point3d[6];

        //Avoid objects
        setAvoidance();

        //Turn trace on
        //Main.toggleTrace(hand.armHandNumber);

        //Compute object center and radius
        setPlaneProperties();

        //target angles to be achived
        target=new Point3d[3];
        //the joints that corresponds to target
        tIc=0;
        //wrist
        tIndex[tIc++]=hand.getSegmentIndex("RING");

        // Origin
        final Point3d ORG=VA.add(root.limb_pos,root.joint_pos);
        VA._scale(ORG,0.5);

        /**
         * Find ring finger intersection
         */
        // Opposition axis
        Point3d opposition;
        if (ORG.x<0)
            opposition=VA.normalize(new Point3d(1,1.13,0));
        else
            opposition=VA.normalize(new Point3d(0,1.13,-1));

        // Get intersection of object with line going through origin in direction of opposition axis
        lineIntersection(ORG, opposition, PL, PN);
        VA._resize(PN[0],PN[1],1.4);

        final Point3d tempRi;
        if (PN[0].z-PN[0].y < PN[1].z-PN[1].y)
            tempRi=VA.subtract(PN[0],ORG);
        else
            tempRi=VA.subtract(PN[1],ORG);
        Log.println(" ring Intersection:"+tempRi.str());

        int pnc=0;
        PN[pnc++]=tempRi;

        lastopposition=VA.normalize(PN[0]);

        if (pnc!=tIc)
        {
            Log.println("SLAP grasp error!");
            System.exit(0);
        }

        for (int i=0;i<pnc;i++)
            VA._add(PN[i],ORG);
        target=PN;

        objsize=estimateSize();
        Main.toggleTrace(hand.armHandNumber);
        Main.refreshDisplay();
    }

    public void beforeAction(final ArmHand hand)
    {
        init_cfg=new double[hand.segc];
        hand.storeAngles(init_cfg);
        maskHand(hand); //don't use in the error calculation

        if (lastPlan==POWER || lastPlan == SLAP)
        {
            hand.wristz.resetJoints();
            if (objectCenter.x<0)
            {
                ArmHand.constrainedRotate(hand.wristz,-80*Math.PI/180);
                //ArmHand.collisionRotate(hand.wristz,-80*Math.PI/180,null);
                ArmHand.constrainedRotate(hand.wristy,-15*Math.PI/180);
                //ArmHand.collisionRotate(hand.wristy,-15*Math.PI/180,null);
            }
        }
        else if (lastPlan==SIDE)
        {
            hand.wristz.resetJoints();
            ArmHand.constrainedRotate(hand.wristy,80*Math.PI/180);
            //ArmHand.collisionRotate(hand.wristy,80*Math.PI/180,null);
            ArmHand.constrainedRotate(hand.wristx,40*Math.PI/180);
            //ArmHand.collisionRotate(hand.wristx,40*Math.PI/180,null);
            resetAvoidance();
        }
    }

    public void afterReach(final ArmHand hand)
    {
        unmaskHand(hand); // let the hand participate in minimization
        if (lastPlan==PRECISION)
        {
            mask("WRISTx", hand);
        }
        else if (lastPlan==SIDE)
        {
            setAvoidance();
            mask("WRISTx", hand);
        }
    }

    public void maskHand(final ArmHand hand)
    {
        if (lastPlan==PRECISION)
        {
            mask("THUMB3", hand);
            mask("INDEX2", hand);
        }
        else if (lastPlan==POWER || lastPlan == SLAP)
        {
            mask("THUMB2", hand);
        }
        else
        {
            Log.dprintln("No grasp is programmed!!", Main.DLEV);
        }
    }

    public void unmaskHand(final ArmHand hand)
    {
        if (lastPlan==PRECISION)
        {
            unmask("THUMB3",hand);
            unmask("INDEX2",hand);
        }
        else if (lastPlan==POWER || lastPlan == SLAP)
        {
            unmask("THUMB2",hand);
        }
        else
        {
            Log.dprintln("No grasp is programmed!!", Main.DLEV);
        }
    }

    public void setAvoidance()
    {
        avoidObjects=true;
        Log.dprintln("Avoidance is ON.", Main.DLEV);
    }

    public void resetAvoidance()
    {
        avoidObjects=false;
        Log.dprintln("Avoidance is OFF.", Main.DLEV);
    }

    public Point3d error_vector(final ArmHand hand)
    {
        final Point3d  sum=new Point3d(0,0,0);
        for (int i=0;i<tIc;i++)
        {
            final int k=tIndex[i];
            if (k<0)
                continue;
            VA._add(sum,VA.subtract(target[i],hand.seg[k].limb_pos));
        }
        return sum;
    }

    public static Point3d via_error_vector(final ArmHand hand, final Point3d viaPoint)
    {
        final Point3d  sum=new Point3d(0,0,0);
        VA._add(sum, VA.subtract(viaPoint,hand.wristz.limb_pos));
        return sum;
    }

    /*
    public Point3d slap_error_vector(final ArmHand hand)
    {
		Point3d target = objectCenter.duplicate();
		target.y += 300;
		target.z -= 100;

        final Point3d  sum=new Point3d(0,0,0);
        VA._add(sum,VA.subtract(target,hand.wristz.limb_pos));
        return sum;
    }
    */

    public Point3d reach_error(final ArmHand hand, int softcon)
    {
        Point3d p=new Point3d();
        p.x=grasp_error(hand);
        switch(softcon)
        {
            case 0:
                p.y=0;
                break;
            case softcon_J2_45:
                p.y=softconGAIN*Math.abs(-Math.PI/4 - hand.j2.beta);
                break;
            case softcon_J2_60:
                p.y=softconGAIN*Math.abs(-Math.PI/3 - hand.j2.beta);
                break;
        }
        p.z=p.x+p.y;
        return p;
    }

    public double babble_error(final ArmHand hand)
    {
        double sum=0;
        int c=0;
        for (int i=0;i<tIc-1;i++)
        {
            final int k=tIndex[i];
            if (k<0)
                continue;
            c++;
            sum += VA.dist(hand.seg[k].limb_pos, target[i]);
        }
        if (avoidObjects)
        {
            sum+=hand.segmentCollision(this);
        }

        return sum/c;
    }

    public double grasp_error(final ArmHand hand)
    {
        double sum=0;
        int c=0;
        for (int i=0;i<tIc;i++)
        {
            final int k=tIndex[i];
            if (k<0)
                continue;
            c++;
            sum += VA.dist(hand.seg[k].limb_pos, target[i]);
        }
        if (avoidObjects)
        {
            sum+=hand.segmentCollision(this);
        }

        return sum/c;
    }

    public static double via_error(final ArmHand hand, final Point3d viaPoint)
    {
        return VA.dist(hand.wristz.limb_pos,viaPoint);
    }

    public void mask(final String s, final ArmHand hand)
    {
        for (int i=0;i<tIc;i++)
        {
            final int k=tIndex[i];
            if (k<0)
                continue;
            if (hand.seg[k].label.equals(s))
            {
                tIndex[i]*=-1;
                Log.dprintln(s+" MASKED", Main.DLEV);
                return;
            }
        }
    }

    public void unmask(final String s, final ArmHand hand)
    {
        for (int i=0;i<tIc;i++)
        {
            final int k=-tIndex[i];
            if (k<0)
                continue;
            if (hand.seg[k].label.equals(s))
            {
                tIndex[i]*=-1;
                Log.dprintln(s+" UNMASKED", Main.DLEV);
                return;
            }
        }
    }

    /* assumes plane properties are set */
    public double estimateSize()
    {
        double sum=0;
        int u=0;
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000) continue;
            u++;
            sum+=VA.dist(hplist[k].CP,objectCenter);
        }
        sum/=u;
        return sum*2;
    }

    synchronized public int segmentIntersection(final Segment seg, final Plane[] pl, final Point3d[] pn)
    {
        final Point3d P1=seg.joint_pos;
        final Point3d P2=seg.limb_pos;
        final double d =VA.dist(P1,P2);
        final int k=lineIntersection(P1, VA.subtract(P2,P1),sPL,sPN);
        if (k==0)
            return k;
        int r=0;
        for (int i=0;i<k;i++)
        {
            if (VA.dist(sPN[i],P1) <= d && VA.dist(sPN[i],P2) <= d)
            {
                pl[r]=sPL[i];
                pn[r++]=sPN[i];
            }
        }
        return r;
    }

    /** returns the intersections (for now 2) of the object with the line
     passing through
     point p with direction vector u.*/
    synchronized public int lineIntersection(final Point3d p, final Point3d u, final Plane[] PL, final Point3d[] PN)
    {
        int c=0;
        Log.dprintln(Main.DLEV, 3, "The intersection of point "+p.str()+" with direction "+u.str()+" with box planes are:");
        for (int i=0;i<hpc;i++)
        {
            final int k=hindex[i];
            if (k>=1000)
                continue; //ignore merger
            final Plane pl=hplist[k];
            final Point3d P=pl.intersection(p,u);
            Log.dprintln(Main.DLEV, 3, "        "+k+") "+pl.str());
            if (P!=null)
            {
                Log.dprintln(Main.DLEV, 3, "           INTERSECTING at "+P.str());
            }
            else
            {
                Log.dprintln(Main.DLEV, 3, "           NONE\n ");
                continue;
            }
            Log.dprintln(Main.DLEV, 3, "");

            if (pl.contained(P) )
            {
                Log.dprintln(Main.DLEV, 3, "         The above intersection is contained in the plane\n\n");

                if(c < PL.length)
                    PL[c]=pl;
                if(c < PN.length)
                    PN[c++]=P;
            }
        }
        if (c==0)
        {
            Log.dprintln(Main.DLEV, 3, "!@!@!@@@@@@ Nothing in object !!!"+hpc+" planes");
        }
        if (c==1)
        {
            Log.dprintln(Main.DLEV, 3, "@@@@@@ Only 1 in object !!!"+hpc+" planes");
            PN[1]=PN[0];
        }
        if (c>2)
        {
            Log.dprintln(Main.DLEV, 3, "@@@@@@ More than 2  in object !!!"+hpc+" planes");
        }
        if (c==2)
        {
            Log.dprintln(Main.DLEV, 3, "@@@@@@ OK.  2 points in object !!!"+hpc+" planes");
        }

        return c;
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


package sim.motor;


import sim.graphics.ArmHandFrame;
import sim.graphics.Mars;
import sim.graphics.Object3d;
import sim.graphics.Plane;
import sim.graphics.Point3d;
import sim.graphics.Segment;
import sim.util.*;
import sim.comp.VisualProcessor;
import sim.main.Main;

/**
 * Arm and hand simulator
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class ArmHand extends Object3d
{
    // Reach thread for current grasp
    protected Reach activeReachThread=null;

    // Whether or not the hand is currently bringing food to the mouth to eat
    public boolean eating;

    public Point3d X,Y,Z;

    // The direction the palm is facing?
    public Point3d palmNormal;                 // palm: Normal.P + D = 0

    // Center point of palm
    public Point3d palmCenter;

    // Palm direction
    public double palmD;

    // All degrees of freedom segments in the hand
    public Segment free[]=null;

    // Number of degrees of freedom segments in the hand
    public int freec=0;

    // All degrees of freedom segments in the arm
    public Segment armfree[]=null;

    // Number of degrees of freedom segments in the arm
    public int armfreec=0;

    // Flags for arm, wrist, hand, and base joints
    static public final int ARMJOINT=1;
    static public final int WRISTJOINT=2;
    static public final int HANDJOINT=3;
    static public final int BASEJOINT=4;

    // Threshold for how close the palm must be to an object for contact
    public double palmThreshold=Resource.get("palmThreshold");

    // Segments of the arm and hand
    public Segment j1,j2,j3,j4,arm,wristx,wristy,wristz,pinky,ring,middle,index,thumb;
    public int[][] fingers=new int[6][4];

    static public int softcon=Graspable.softcon_J2_45;

    // This distance to the object
    public double dis=1e10;

    // Contacting the target object
    public boolean contact=false;

    // Whether or not the hand is grasping an object
    public boolean grasped;

    // Reach time
    public double rtime=0;

    // Last position of the center of the index-thumb aperture
    public Point3d lastIndexPosition = new Point3d();

    // Whether or not the hand is visible
    public boolean visible=true;

    // Whether or not the action is audible
    public boolean audible=true;

    // Whether or not the sound matches the action
    public boolean congruentSound=true;

    // Calculated path for each joint for the action
    public Spline[] jointpath=null;

    // Stores current joint angles
    public double[] theta1;

    // Stores target joint angles
    public double[] theta2;

    // Stores via point joint angles
    public double[] via;

    // Armhand trajectory
    public Trajectory lasttr=null;

    // How close the hand must be to the object for a successful grasp
    public double zeroError=3.0;

    // Speed of the reach
    public double reach_speed=1;

    // Time step increments
    public double reach_deltat=1;

    // Multiply by speed to get delta time
    public double reach_basetime=0.01;

    // Number of time steps in the current action so far
    public int ntick=0;

    // Action mode
    public int search_mode=0;

    // Visual search - solve inverse kinematics for final joint angles
    static public final int VISUAL_SEARCH=0;

    // Execute - execute action
    static public final int EXECUTE=2;

    // How close the hand was to the object at the end of the last grasp
    public double lasterr=0;

    // Phase of the visual search
    public int search_phase=0;

    // Constants for the Jacobian transpose
    public double l1=800;
    public double l2=700;

    // Matrix for the Jacobian transpose
    public double[][] J=new double[3][5];

    public double[] dTheta =new double[5];

    // Joint control frame
    private ArmHandFrame ahFrame;

    boolean showInfo=false;
    String infoStr=null;

    ContactList cl=null;

    public int armHandNumber=1;

    public VisualProcessor visualProcessor;

    /**
     * Constructor
     * @param s
     * @param pipesidec
     * @param piperad
     */
    public ArmHand(final String s, final int pipesidec, final double piperad)
    {
        super(s,pipesidec,piperad);
        visualProcessor=new VisualProcessor(segc);
        setupHand();
        //adjustHand();
    }

    /**
     * Starts all eating actions - eat, record, recognize
     * @param obj - The object being eaten
     * @param kind - The type of eating action - eat, record, recognize
     */
    public void doEat(final Graspable obj, final Graspable obs, final String kind)
    {
        eating=true;

        makeNeutral();

        final double[] pre_t=new double[5];
        final double[] pre_v=new double[5];
        int pc=0;
        pre_t[pc]=0;
        pre_v[pc++]=0;
        pre_t[pc]=0.5;
        pre_v[pc++]=0.45;
        pre_t[pc]=0.75;
        pre_v[pc++]=1.2;
        pre_t[pc]=1;
        pre_v[pc]=1;

        // Store current joint angles
        theta1 =new double[segc];
        storeAngles(theta1);

        // Reset distance
        dis=1e10;

        Main.self.reportStart("     Starting to put in mouth");

        // Compute trajectory to bring food to mouth
        // This is a very well learned action, so the target joint angles are already known
        final double[] sol=new double[segc];
        storeAngles(sol);
        sol[1] = -87.7*Math.PI/180.0;
        sol[2] = 20.0*Math.PI/180.0;
        sol[3] = 48.5*Math.PI/180.0;
        sol[4] = -64.6*Math.PI/180.0;
        sol[5] = -69.2*Math.PI/180.0;
        lasttr=trimTrajectory(sol);

        fireExecution(obj,obs);
    }


    /**
     * Main reach/grasp starting entry. All kinds of reaches,
     * visearch, silet, execute, record etc. are started from here.
     * @param obj - The object that the reach is directed toward
     * @param obs - Obstacle object
     * @param kind - Type of reach
     */
    public void doReach(final Graspable obj, final Graspable obs, final String kind)
    {
        // Not eating and not yet grasped
        eating=false;
        grasped=false;

        makeNeutral();

        // Store current joint angles
        theta1 =new double[segc];
        storeAngles(theta1);

        // Rest distance
        dis=1e10;

        Main.self.reportStart("     Starting reach of type:"+kind);

        // Start the type of reach - visual, record, recognize, or execute
        if (kind.equals("visual"))
            fireVisualSearch(obj,obs);
        else
        {
            fireExecution(obj,obs);
        }
    }

    /**
     * Start visual search to find final joint angles and trajectory for a reach
     * @param obj - The object the reach is directed toward
     * @param obs - Obstacle object
     */
    public void fireVisualSearch(final Graspable obj, final Graspable obs)
    {
        lasttr=null;
        via=null;
        ntick=0;
        obj.beforeAction(this);
        Main.setTrace(false);
        search_mode=ArmHand.VISUAL_SEARCH;
        activeReachThread= createReachThread("",obj,obs);
    }

    /**
     * Start a reach toward an object - Follows trajectory computed by a visual search
     * @param obj - The object the reach is directed toward
     * @param obs - Obstacle object
     */
    public void fireExecution(final Graspable obj, final Graspable obs)
    {
        ntick=0;

        // If a reach trajectory has not already been computed
        if (lasttr==null)
        {
            Main.self.setInfo("Ready.  * Please first do a VISREACH to use this function *");
        }
        else
        {
            Log.println("lastr is non null: executing...");
            search_mode=ArmHand.EXECUTE;
            if(obj != null)
                obj.mask("WRISTx",this);  //make it more general. ok for now
            activeReachThread= createReachThread("",obj,obs);
            Log.println("NOW executing...");
        }
    }

    /**
     * Instantiates a reach thread and starts it.
     * @param com
     * @param obj - The object the reach is directed toward
     * @param obs - Obstacle object
     */
    public Reach createReachThread(final String com, final Graspable obj, final Graspable obs)
    {
        if(obs!=null && obs.visible)
            search_phase=-1;
        else
            search_phase=0;
        rtime=0;
        contact=false;
        reach_speed= Elib.toDouble(Main.self.speedCombo.getSelectedItem().toString());
        reach_deltat=reach_speed*reach_basetime;
        activeReachThread=new Reach(this,obj,obs,lasttr,com);
        activeReachThread.start();
        return activeReachThread;
    }

    /**
     * Performs final necessary steps for reaches
     * @param com
     * @param obj - The object the reach was directed towards
     */
    public void finalizeReach(final String com, final Graspable obj)
    {
        Main.refreshDisplay();
        Main.self.setInfoReady();

        String tip="EXECUTE";
        if (search_mode==ArmHand.VISUAL_SEARCH)
            tip="VISUAL_SEARCH";
        Main.self.reportFinish("   ===> ("+tip+") Grasp Final error:"+dis+".  "+com);

        // If this was a grasp and it was successful
        if(!eating && dis<zeroError*3)
        {
            grasped=true;
        }
    }

    /**
     * Returns whether or not the reach thread is currently active
     */
    synchronized public boolean reachActive()
    {
        return activeReachThread != null;
    }

    /**
     * Kills the reach thread if it is currently active
     */
    synchronized public boolean kill_ifActive()
    {
        if (activeReachThread!=null)
        {
            // Stop the thread
            activeReachThread.stopRGest();

            activeReachThread=null;
            return true;
        }
        else
            return false;
    }
    /**
     * Puts hand and arm in initial standing position
     */
    public void adjustHand()
    {
        final double diff=info3dHand();
        extendPalm(1.5*diff); // I modified diff normally no coeff. here

        makeStanding();
    }

    /**
     * Sets up the hand information
     */
    public void setHandFrame()
    {
        X=VA.normalize(VA.subtract(pinky.joint_pos,index.joint_pos));
        Y=VA.normalize(VA.subtract(index.limb_pos,index.joint_pos));
        Z=VA.normalize(VA.cross(Y,X));

        palmCenter=VA.add(pinky.joint_pos,index.joint_pos);
        VA._add(palmCenter,wristx.joint_pos);
        VA._scale(palmCenter,1.0/3);

        final Point3d indexB=VA.subtract(index.joint_pos,palmCenter);
        final Point3d wristB=VA.subtract(wristx.joint_pos,palmCenter);

        // pointing the inside of the palm [we have left handed CF]
        palmNormal=VA.normalize(VA.cross(indexB, wristB));
        palmD=-VA.inner(palmNormal,palmCenter);

        jointpath=new Spline[segc];
    }

    /**
     * Construct hand and arm from segments, add joint constraints
     */
    public void setupHand()
    {
        //-----------------------------
        free=new Segment[segc];  // more than enough, but it is OK
        armfree=new Segment[segc];  // more than enough, but it is O

        arm=getJoint("WRISTz");
        wristx=getJoint("WRISTx");
        wristy=getJoint("WRISTy");
        wristz=getJoint("WRISTz");
        pinky=getJoint("PINKY").child[0];
        ring =getJoint("RING").child[0];
        middle=getJoint("MIDDLE").child[0];
        index=getJoint("INDEX").child[0];
        thumb=getJoint("THUMB");

        setHandFrame();
        root.setuserTag(ArmHand.ARMJOINT,true);

        root.setuserTag(ArmHand.BASEJOINT,false);

        wristx.setuserTag(ArmHand.HANDJOINT,true);

        wristx.setuserTag(ArmHand.WRISTJOINT,false);
        wristy.setuserTag(ArmHand.WRISTJOINT,false);
        wristz.setuserTag(ArmHand.WRISTJOINT,false);
        int i=0;
        free[i++]=wristz;
        free[i++]=wristx;
        free[i++]=wristy;
        free[i++]=pinky;
        free[i++]=pinky.child[0];
        free[i++]=ring;
        free[i++]=ring.child[0];
        free[i++]=middle;
        free[i++]=middle.child[0];
        free[i++]=index;
        free[i++]=index.child[0];
        free[i++]=thumb;
        free[i++]=thumb.child[0];
        free[i++]=thumb.child[0].child[0];
        freec=i;

        armfreec=0;
        j1=armfree[armfreec++]=getJoint("J1");
        j2=armfree[armfreec++]=getJoint("J2");
        j3=armfree[armfreec++]=getJoint("J3");
        j4=armfree[armfreec++]=getJoint("J4");

        putJointConstraints();

        fingers[0][0] = getSegmentIndex("THUMB");
        fingers[0][1] = getSegmentIndex("THUMBin");
        fingers[0][2] = getSegmentIndex("THUMB2");
        fingers[0][3] = getSegmentIndex("THUMB3");

        fingers[1][1] = getSegmentIndex("INDEX");
        fingers[1][2] = getSegmentIndex("INDEX1");
        fingers[1][3] = getSegmentIndex("INDEX2");

        fingers[2][1] = getSegmentIndex("MIDDLE");
        fingers[2][2] = getSegmentIndex("MIDDLE1");
        fingers[2][3] = getSegmentIndex("MIDDLE2");

        fingers[3][1] = getSegmentIndex("RING");
        fingers[3][2] = getSegmentIndex("RING1");
        fingers[3][3] = getSegmentIndex("RING2");

        fingers[4][1] = getSegmentIndex("PINKY");
        fingers[4][2] = getSegmentIndex("PINKY1");
        fingers[4][3] = getSegmentIndex("PINKY2");
    }

    /**
     * Adds a segment and child object contacts to contact list
     * @param tseg - Segment
     * @param seg - Its child
     * @param PL
     * @param PN
     * @param obj - The object to check for intersection
     */
    public void addseg_and_child(final Segment tseg, Segment seg, final Plane[] PL, final Point3d[] PN, final Graspable obj)
    {
        if (seg==null)
            seg=tseg;
        int k=obj.segmentIntersection(seg,PL,PN);
        if (k!=0)
            cl.addContact(seg,PL[0],PN[0],tseg, tseg.torque);

        for (int i=0;i<seg.noch;i++)
        {
            // now each segment can give index torque
            addseg_and_child(seg.child[i],seg.child[i],PL,PN,obj);
        }
    }

    /**
     * Returns a snapshot of each joint's angle
     */
    public double[] getJointAngleSnapshot(double noiseLevel)
    {
        double[] snapshot=new double[segc];
        storeAngles(snapshot);
        for(int i=0; i<segc; i++)
        {
            snapshot[i] += ((Math.random()*noiseLevel)-(noiseLevel/2));
        }
        return snapshot;
    }

    /**
     * Returns a measure of palm extension
     */
    public double info3dHand()
    {
        final double l0=VA.dist(pinky.joint_pos,wristx.joint_pos);
        final double l1=VA.dist(index.joint_pos,wristx.joint_pos);
        final double l2=VA.dist(index.joint_pos,pinky.joint_pos);

        return ((l0+l1)*0.5-l2);
    }

    /**
     * The direction the thumb is pointed in
     */
    public Point3d thumbDir()
    {
        return VA.normalize(VA.subtract(thumb.child[0].child[0].child[0].limb_pos,thumb.joint_pos));
    }

    /**
     * The direction of the index-thumb aperture
     */
    public Point3d indexAperDir()
    {
        return VA.normalize(VA.subtract(index.child[0].limb_pos,thumb.child[0].child[0].child[0].limb_pos));
    }

    /**
     * The size of the index-thumb aperture
     */
    public double indexAperture()
    {
        return VA.dist(index.child[0].limb_pos,thumb.child[0].child[0].child[0].limb_pos);
    }

    /**
     * The center point of the index-thumb aperture
     */
    public Point3d indexApertureCenter()
    {
        return VA.center(index.child[0].limb_pos,thumb.child[0].child[0].child[0].limb_pos);
    }

    /**
     * The size of the index-thumb aperture
     */
    public double sideAperture()
    {
        return VA.dist(index.limb_pos,thumb.child[0].child[0].child[0].limb_pos);
    }

    /**
     * The direction of the index-thumb aperture
     */
    public Point3d sideAperDir()
    {
        return VA.normalize(VA.subtract(index.limb_pos,thumb.child[0].child[0].child[0].limb_pos));
    }

    /**
     * The size of the middle-thumb aperture
     */
    public double middleAperture()
    {
        return VA.dist(middle.child[0].limb_pos,thumb.child[0].child[0].child[0].limb_pos);
    }

    /**
     * Extends palm by given amount
     * @param val
     */
    public void extendPalm(final double val)
    {
        final Segment[] fing=new Segment[5];
        fing[0]=getJoint("PINKY");
        fing[1] =getJoint("RING");
        fing[2]=getJoint("MIDDLE");
        fing[3]=getJoint("INDEX");
        fing[4]=getJoint("THUMB");

        for (int i=0;i<4;i++)
        {
            double t=0;
            if (i==0)
                t=-val/2.0;
            if (i==1)
                t=-val/4.0;
            if (i==2)
                t= val/4.0;
            if (i==3)
                t= val/2.0;
            fing[i]._translate(t,0,0);
        }
    }

    /**
     * Sets constraints on joint rotations
     */
    private void putJointConstraints()
    {
        for (int i=0;i<freec;i++)
            setJointConstraint(free[i],0,+Math.PI/2+Math.PI/10);

        setJointConstraint(j1,-180*Math.PI/180,+180*Math.PI/180);
        setJointConstraint(j2,-90*Math.PI/180,+20*Math.PI/180);
        setJointConstraint(j3,-90*Math.PI/180,+90*Math.PI/180);
        setJointConstraint(j4,-90*Math.PI/180,+80*Math.PI/180);
        setJointConstraint(wristx,-90*Math.PI/180,+90*Math.PI/180);
        setJointConstraint(wristy,-20*Math.PI/180,+80*Math.PI/180);
        setJointConstraint(wristz,-180*Math.PI/180,+80*Math.PI/180);

        setJointConstraint(thumb,0.0,+Math.PI/2);
        setJointConstraint(thumb.child[0], 0,+Math.PI/4-0.1);
        setJointConstraint(thumb.child[0].child[0],0.0,+Math.PI/2);
        setJointConstraint(thumb.child[0].child[0].child[0], 0.0,+Math.PI/2);

        // these are mounting points of these fingers to the wrist.
        setJointConstraint(pinky.parent,0.0,0.0);
        setJointConstraint(ring.parent,0.0,0.0);
        setJointConstraint(middle.parent,0.0,0.0);
        setJointConstraint(index.parent,0.0,0.0);
    }

    /**
     * Puts the hand and arm in a random position within each joint's constraints
     */
    public void makeRandom()
    {
        j1.rotateJoint((Math.random()*(j1.maxbeta-j1.minbeta))+j1.minbeta);
        j2.rotateJoint((Math.random()*(j2.maxbeta-j2.minbeta))+j2.minbeta);
        j3.rotateJoint((Math.random()*(j3.maxbeta-j3.minbeta))+j3.minbeta);
        j4.rotateJoint((Math.random()*(j4.maxbeta-j4.minbeta))+j4.minbeta);
        wristx.rotateJoint((Math.random()*(wristx.maxbeta-wristx.minbeta))+wristx.minbeta);
        wristy.rotateJoint((Math.random()*(wristy.maxbeta-wristy.minbeta))+wristy.minbeta);
        wristz.rotateJoint((Math.random()*(wristz.maxbeta-wristz.minbeta))+wristz.minbeta);
        pinky.rotateJoint((Math.random()*(pinky.maxbeta-pinky.minbeta))+pinky.minbeta);
        pinky.child[0].rotateJoint((Math.random()*(pinky.child[0].maxbeta-pinky.child[0].minbeta))+pinky.child[0].minbeta);
        ring.rotateJoint((Math.random()*(ring.maxbeta-ring.minbeta))+ring.minbeta);
        ring.child[0].rotateJoint((Math.random()*(ring.child[0].maxbeta-ring.child[0].minbeta))+ring.child[0].minbeta);
        middle.rotateJoint((Math.random()*(middle.maxbeta-middle.minbeta))+middle.minbeta);
        middle.child[0].rotateJoint((Math.random()*(middle.child[0].maxbeta-middle.child[0].minbeta))+middle.child[0].minbeta);
        index.rotateJoint((Math.random()*(index.maxbeta-index.minbeta))+index.minbeta);
        index.child[0].rotateJoint((Math.random()*(index.child[0].maxbeta-index.child[0].minbeta))+index.child[0].minbeta);
        thumb.rotateJoint((Math.random()*(thumb.maxbeta-thumb.minbeta))+thumb.minbeta);
        thumb.child[0].rotateJoint((Math.random()*(thumb.child[0].maxbeta-thumb.child[0].minbeta))+thumb.child[0].minbeta);
        thumb.child[0].child[0].rotateJoint((Math.random()*(thumb.child[0].child[0].maxbeta-thumb.child[0].child[0].minbeta))+thumb.child[0].child[0].minbeta);
    }

    /**
     * Puts the hand and arm in a neutral position
     */
    public void makeNeutral()
    {
        wristx.resetJoints();
        wristz.rotateJoint(-65*Math.PI/180);
        pinky.rotateJoint(52*Math.PI/180);
        pinky.child[0].rotateJoint(15*Math.PI/180);
        ring.rotateJoint(32*Math.PI/180);
        ring.child[0].rotateJoint(44*Math.PI/180);
        middle.rotateJoint(37*Math.PI/180);
        middle.child[0].rotateJoint(33*Math.PI/180);
        index.rotateJoint(50.2*Math.PI/180);
        index.rotateJoint(18.5*Math.PI/180);
        thumb.rotateJoint(60.7*Math.PI/180);
        thumb.child[0].rotateJoint(34.7*Math.PI/180);
        thumb.child[0].child[0].rotateJoint(36.9*Math.PI/180);
        thumb.child[0].child[0].child[0].rotateJoint(5*Math.PI/180);
        Main.refreshDisplay();
    }

    /**
     * Puts the hand and arm into an upright position
     */
    public void makeUpright()
    {
        resetJoints();
        j4.rotateJoint(80*Math.PI/180);
    }

    /**
     * Puts the hand and arm into a standing position
     */
    public void makeStanding()
    {
        resetJoints();
        makeNeutral();
    }

    /**
     * Resets the ArmHand joint angles
     */
    public void resetJoints()
    {
        root.resetJoints();
    }

    /**
     * Resets the joint angles of the fingers
     */
    public void resetFingers()
    {
        pinky.resetJoints();
        ring.resetJoints();
        middle.resetJoints();
        index.resetJoints();
        thumb.resetJoints();
    }

    /**
     * Looks for a collision between ArmHand segments and the given object
     * @param o - The object to be grasped
     */
    public double segmentCollision(final Graspable o)
    {
        for (int i=0;i<segc;i++)
        {
            if (o.inside(seg[i].limb_pos))
                return (5000.0/VA.dist(seg[i].limb_pos,o.objectCenter)+1) ;
            if (o.inside(VA.center(seg[i].limb_pos,seg[i].joint_pos)))
                return (5000.0/VA.dist(seg[i].limb_pos,o.objectCenter)+1) ;
        }
        return 0;
    }

    /**
     * Creates a reach trajectory given the target joint angles
     * @param targetangles - The target joint angles
     */
    public Trajectory trimTrajectory(final double[] targetangles)
    {
        theta2 =targetangles;
        jointpath=Trajectory.jointSpline(seg,theta1,theta2,segc);

        return new Trajectory();
    }

    /**
     * Creates a reach trajectory given the target joint angles and via point angles
     * @param targetangles - The target joint angles
     * @param viaangles - The via point joint angles
     * @param viaratio - Via point ratios
     */
    public Trajectory trimTrajectory(final double[] targetangles, final double[][] viaangles, final double[] viaratio)
    {
        theta2 =targetangles;
        jointpath=Trajectory.jointSpline(seg,theta1,viaangles,theta2,segc,viaratio);

        return new Trajectory();
    }

    /**
     * Called by the reach thread when executing a stored trajectory during each time step.
     * @param tr - The reach trajectory
     * @param obj - The object that the reach is directed toward
     */
    public boolean tickReachGesture(final Trajectory tr, final Graspable obj, final Graspable obstacle)
    {
        // Get the distance to the object
        dis=obj.grasp_error(this);

        // If the reach is finished and the thread hasn't been killed by success - give it up
        if (rtime> 1.0)
        {
            Log.println("Target must be reached.");
            Log.println("Distance to target(error):"+Elib.nice(dis,1e5));
            return false;
        }
        // Reach isnt finished yet
        else
        {
            // Increment reach time
            rtime+=reach_deltat;

            // For every segment in the limb
            for (int i=0;i<segc;i++)
            {
                // If there is no path for this joint - forget about it
                if (jointpath[i]==null)
                    continue;

                // Calculate time point in path
                double mytime=rtime;

                // Stretch time for reach, not for grasp
                if (seg[i].userTag!=ArmHand.HANDJOINT && Main.bellshape )
                    mytime=tr.stretchTime(rtime);

                // If an object is grasped
                if(grasped)
                {
                    graspingRotate(seg[i],jointpath[i].eval(mytime) -seg[i].beta,obj);
                }
                else
                {
                    constrainedRotate(seg[i],jointpath[i].eval(mytime) -seg[i].beta);
                    //collisionRotate(seg[i],jointpath[i].eval(mytime) -seg[i].beta,null);
                    lastIndexPosition = indexApertureCenter();
                }
            }
        }

        // Convert reach time to time steps
        final int hh=(int)(0.5+rtime/reach_deltat);

        // Draw square for wrist trajectory
        if (Main.traceon)
            Mars.addStar(wristx.joint_pos,armHandNumber);

        // If this reach is being recorded for a training set or input into network for recognition
        // If the object has been reached
        if(dis<zeroError*3)
        {
            Log.println("**** Object contact at t="+hh+" ****");
            contact=true;
            if(audible && Reach.soundClip!=null && !indexApertureCenter().equals(lastIndexPosition))
            {
                // If this is the beginning of the sound - play the sound file
                Reach.playSound();
                Reach.soundClip=null;
            }
        }
        return true;
    }

    /** Returns the ContactList with Object obj. */
    public ContactList contact(Graspable obj)
    {
        if (cl==null)
        {
            cl=new ContactList(obj);
        }
        else
            cl.resetList(obj);

        Point3d[] PN=new Point3d[13];
        Plane[] PL=new Plane[13];
        setHandFrame();
        addseg_and_child(index,null,PL,PN,obj);
        Log.dprintln("Number of intersections of index "+cl.contc, Main.DLEV);
        addseg_and_child(thumb,null,PL,PN,obj);
        Log.dprintln("Number of intersection of index+thumb:"+cl.contc, Main.DLEV);

        // note that the thumb intersections are counted twice for two joints
        // at the thumb vertex
        addseg_and_child(middle,null,PL,PN,obj);
        addseg_and_child(ring,null,PL,PN,obj);
        addseg_and_child(pinky,null,PL,PN,obj);
        Log.dprintln("Number1 of intersection of all fingers (thumb counted twice):"+cl.contc, Main.DLEV);

        double side=(VA.inner(palmNormal,obj.objectCenter) + palmD);
        if (side>0)
        {
            int k=obj.lineIntersection(obj.objectCenter, palmNormal, PL,PN);
            Log.dprintln("@@@@@@@@@@@@@@@ OBJ INTERSECTION COUNT:"+k, Main.DLEV);
            Point3d G;
            // pick the one closes to the palm
            if (k==1 || k==2)
            {
                if (k==1)
                    G=PN[0];
                else if (VA.dist(PN[0],palmCenter) < VA.dist(PN[1],palmCenter))
                    G=PN[0];
                else
                    G=PN[1];

                double d=VA.dist(G,palmCenter);

                // close enough
                if (d<palmThreshold)
                {
                    Log.dprintln("Object facing the palm and close. Good.[d="+d+']', Main.DLEV);
                    cl.addContact(palmNormal,1);
                }
                else
                {
                    Log.dprintln("-------->Object is too far to the palm:"+d, Main.DLEV);
                }
            }
            else
            {
                Log.dprintln("----------> object cannot be intersected!!!!", Main.DLEV);
            }
        }
        else
        {
            Log.dprintln("Object not in the right side of the hand!", Main.DLEV);
            cl.graspCost=1e30;
            Log.dprintln("---------------------> Cost of grasping:"+cl.graspCost, Main.DLEV);
            return cl;
        }

        double cost=cl.searchNewton();
        Log.dprintln("---------------------> Cost of grasping:"+cost, Main.DLEV);
        double Fnorm=VA.norm(cl.netForce);
        Log.dprintln("Net Force  :"+cl.netForce.str()+" norm:"+Fnorm, Main.DLEV);
        Log.dprintln("Net Torque :"+cl.netTorque.str()+" norm:"+VA.norm(cl.netTorque), Main.DLEV);
        return cl;
    }

    /**
     * Called by the reach thread when calculating a new trajectory during each time step.
     * @param obj - The object being reached to
     */
    public boolean tickVisual(final Graspable obj, final Graspable obstacle, final Point3d viaPoint)
    {
        // Calculation of distance to object depends on phase of search
        if (search_phase==0)
        {
            dis=obj.grasp_error(this);
            if(obstacle != null && obstacle.visible)
            {
                dis+=5*segmentCollision(obstacle);
            }
        }
        else if(search_phase==1)
        {
            dis=obj.grasp_error(this);
            if(obstacle != null && obstacle.visible)
            {
                dis+=5*segmentCollision(obstacle);
            }
        }
        else if(search_phase==-1)
            dis=Graspable.via_error(this,viaPoint);

        // Target reached or tried long enough
        if ((dis < zeroError && dis<obj.GRASP_TH) || ntick==450)
        {
            // If the target was reached
            if (dis<zeroError)
                Log.println("Target reached.");
            else
                Log.println("Cannot reach; Trying my best.");

            // Try to contact object
            afterAction(obj);

            // Get final joint coordinates
            final double[] sol=new double[segc];
            storeAngles(sol);

            // Create trajectory based on final joint coordinates
            if(via == null)
                lasttr=trimTrajectory(sol);
            else
                lasttr=trimTrajectory(sol,new double[][]{via}, new double[]{.75});

            // Kill reach thread
            return false;
        }
        // Not there yet - keep trying!
        else
        {
            // Calculate the rate at which to perform gradient descent
            double rate=(Elib.cube(dis/50)+1)*obj.BASERATE;

            // Cap the gradient descent rate
            if (rate>obj.RATE_TH)
                rate=obj.RATE_TH;

            if (search_phase==-1)
            {
                dis=jacobianTransposeForVia(1,viaPoint);
                if (dis<obj.GRASP_TH)
                {
                    via = new double[segc];
                    storeAngles(via);
                    search_phase=0;
                }
            }
            else if (search_phase==0)
            {
                // Gradient descent on arm
                dis=jacobianTranspose(1,obj);
                // If close enough move to next phase of search
                if (dis<obj.GRASP_TH)
                {
                    search_phase=1;
                    obj.afterReach(this);
                }
            }
            // Gradient descent on arm and hand for rest of grasp
            else if(search_phase == 1)
                gradientDescent(1,rate,obj,false);

            // Output progress
            if (ntick%50==0)
            {
                Log.dprintln("Step:"+ntick+" Distance to target:"+Elib.nice(dis,1e5)+" Aperture:"+Elib.nice(VA.dist(index.child[0].limb_pos,thumb.child[0].child[0].child[0].limb_pos),1e4), Main.DLEV);
            }
            ntick++;
            if (Main.traceon)
                Mars.addStar(wristx.joint_pos,1);

            lastIndexPosition = indexApertureCenter();
        }
        return true;
    }

    public void afterAction(final Graspable obj)
    {
        if (obj.lastPlan==Graspable.POWER)
        {
            grabObject(obj);
            Log.println("POWER it is!");
        }
        else if (obj.lastPlan==Graspable.SIDE)
        {
            graspObject(1,2,obj);  //dont move the first joint of index
        }
    }

    /**
     * Sets constraints on a joint's min and max angles
     * @param s - The joint segment
     * @param min - The min angle
     * @param max - Max angle
     */
    public static void setJointConstraint(final Segment s, final double min, final double max)
    {
        s.minbeta=min;
        s.maxbeta=max;
        s.jointconstraint=true;
    }

    /**
     * Rotate the given joint by the given angle staying within the joint's constraints
     * @param seg - The joint segment to rotate
     * @param ang - The angle to rotate to
     */
    public static void constrainedRotate(final Segment seg,double ang)
    {
        if(ang!=Double.NaN)
        {
            // If the joint has any constraints
            if (seg.jointconstraint)
            {
                // Cap the angle of rotation by the joints constraints
                if (seg.beta+ang > seg.maxbeta)
                {
                    ang=seg.maxbeta-seg.beta;
                }
                if (seg.beta+ang < seg.minbeta)
                {
                    ang=seg.minbeta-seg.beta;
                }
            }
            // No change
            if (ang==0)
            {
                if (seg.seg_pan!=null)
                    seg.update_panel();
            }
            else
                seg.rotateJoint(ang);
        }
    }

    /**
     * Rotate the joint by the given angle with the given object in grasp
     * @param seg - The joint to be rotated
     * @param ang - The angle to rotate the joint by
     * @param o - The object in grasp
     */
    public void graspingRotate(final Segment seg, final double ang, final Object3d o)
    {
        // Rotate the joint
        constrainedRotate(seg,ang);
        //collisionRotate(seg,ang,null);

        // Update the object position to keep up with the moving hand
        if (ang != Double.NaN && ang!=0)
        {
            final Point3d oldOffset = VA.subtract(lastIndexPosition, o.objectCenter);
            final Point3d newOffset = VA.subtract(indexApertureCenter(), o.objectCenter);
            o.rect_moveto(VA.add(o.objectCenter, VA.subtract(newOffset,oldOffset)));

            lastIndexPosition = new Point3d(indexApertureCenter().x, indexApertureCenter().y, indexApertureCenter().z);
        }
    }

    /**
     * Returns the index of a segment given its name
     * @param name - The name of the segment
     */
    public int getSegmentIndex(final String name)
    {
        for (int i=0;i<segc;i++)
            if (seg[i].label.equals(name))
                return i;
        return -1;
    }

    /**
     * Returns the segment for a joint given its name
     * @param name - The name of the joint
     */
    public Segment getJoint(final String name)
    {
        return root.getJoint(name);
    }

    /**
     * Clear the last calculated reach trajectory
     */
    public void clearTrajectory()
    {
        lasttr=null;
    }

    /**
     * Perform gradient descent on all joint angles with respect to the hand's distance to the object
     * @param N - Number of iterations of gradient descent to perform
     * @param lrate - Rate of joint angle change
     * @param obj - The object being grasped
     */
    public double gradientDescent(final int N, final double lrate, final Graspable obj, boolean collision)
    {
        double rate;
        // Perform gradient descent N times
        for (int k=0;k<N;k++)
        {
            // For every segment
            for (int i=0;i<segc;i++)
            {
                // If the segment does not correspond to a joint - skip it
                if (seg[i].joint_axis==null)
                    continue;

                // Different rates of joint angle change for arm and hand joints
                if (seg[i].userTag==ArmHand.ARMJOINT)
                    rate=lrate*0.3;
                else
                    rate=lrate;

                // Calculate distance to object
                final double dis=obj.grasp_error(this);

                // Calculate random joint angle displacement given the rate of joint angle change
                final double dteta=(Math.random()-0.5)*rate;

                // Rotate the joint by this disaplacement
                if(!collision)
                    constrainedRotate(seg[i],dteta);
                else
                    collisionRotate(seg[i],dteta,obj);

                // Calculate new distance to object
                final double newdis=obj.grasp_error(this);

                // If this joint rotation brought the hand farther from the object - rotate it back
                if (newdis>dis)
                    constrainedRotate(seg[i],-1.5*dteta);
                    //collisionRotate(seg[i],-1.5*dteta,null);
            }
            Main.refreshDisplay();
        }
        return obj.grasp_error(this);
    }

    /**
     * Perform gradient descent on arm joint angles with respect to the distance from the wrist to the object
     * //@param N - Number of iterations of gradient descent to perform
     * //@param rate - Rate of joint angle change
     * //@param obj - The object being grasped
     */
    /*
    private double gradientDescent_wrist(final int N, final double rate, final Graspable obj)
    {
        // Perform gradient descent N times
        for (int k=0;k<N;k++)
        {
            // For every segment
            for (int i=0;i<segc;i++)
            {
                // If the segment does not correspond to a joint - skip it
                if (seg[i].joint_axis==null)
                    continue;

                // Only do arm joints
                if (seg[i].userTag==ArmHand.HANDJOINT)
                    break;

                // Calculate distance to object
                final Point3d dis3=obj.reach_error(softcon,this);

                // Calculate random joint angle displacement given the rate of joint angle change
                final double dteta=(Math.random()-0.5)*rate;

                // Rotate the joint by this disaplacement
                constrainedRotate(seg[i],dteta);

                // Calculate new distance to object
                final Point3d newdis3=obj.reach_error(softcon,this);

                // If this joint rotation brought the wrist farther from the object - rotate it back
                if (newdis3.z>dis3.z)
                    constrainedRotate(seg[i],-1.5*dteta);
            }
        }
        return obj.reach_error(this);
    }
    */
    /*
    synchronized private double gradientDescent_arm(int N,double rate, Graspable obj)
    {
        for (int k=0;k<N;k++)
        {
            for (int i=0;i<segc;i++)
            {
                if (seg[i].userTag==ArmHand.WRISTJOINT)
                    break;
                if (seg[i].joint_axis==null)
                    continue;
                Point3d dis3=obj.reach_error(softcon,this);
                double dteta=(Math.random()-0.5)*rate;
                constrainedRotate(seg[i],dteta);
                Point3d newdis3=obj.reach_error(softcon,this);
                if (newdis3.z>dis3.z && Math.random()>0.01)
                    constrainedRotate(seg[i],-1.5*dteta);
            }
        }
        return obj.reach_error(this);
    }
    */

    synchronized protected double jacobianTranspose(final int N, final Graspable obj)
    {
        for (int k=0;k<N;k++)
        {
            final double t1=seg[1].beta;
            final double t2=seg[2].beta;
            final double t3=seg[3].beta;
            final double t4=seg[4].beta;

            // ---from matlab
            J[0][0] = 0.0;
            J[0][1] = (-Math.cos(t2)*Math.sin(t4)+Math.sin(t2)*Math.sin(t3)*Math.cos(t4))*l2-Math.cos(t2)*l1;
            J[0][2] = -Math.cos(t2)*Math.cos(t3)*Math.cos(t4)*l2;
            J[0][3] = (-Math.sin(t2)*Math.cos(t4)+Math.cos(t2)*Math.sin(t3)*Math.sin(t4))*l2;
            J[0][4] = 0.0;

            J[1][0] = (Math.sin(t1)*Math.cos(t2)*Math.sin(t4)+(-Math.sin(t1)*Math.sin(t2)*Math.sin(t3)-Math.cos(t1)*Math.cos(t3))*Math.cos(t4))*l2+Math.sin(t1)*Math.cos(t2)*l1;
            J[1][1] = (Math.cos(t1)*Math.sin(t2)*Math.sin(t4)+Math.cos(t2)*Math.sin(t3)*Math.cos(t1)*Math.cos(t4))*l2+Math.cos(t1)*Math.sin(t2)*l1;
            J[1][2] = (Math.cos(t1)*Math.sin(t2)*Math.cos(t3)+Math.sin(t1)*Math.sin(t3))*Math.cos(t4)*l2;
            J[1][3] = (-Math.cos(t1)*Math.cos(t2)*Math.cos(t4)-(Math.cos(t1)*Math.sin(t2)*Math.sin(t3)-Math.sin(t1)*Math.cos(t3))*Math.sin(t4))*l2;
            J[1][4] = 0.0;

            J[2][0] = (-Math.cos(t1)*Math.cos(t2)*Math.sin(t4)+(Math.cos(t1)*Math.sin(t2)*Math.sin(t3)-Math.sin(t1)*Math.cos(t3))*Math.cos(t4))*l2-Math.cos(t1)*Math.cos(t2)*l1;
            J[2][1] = (Math.sin(t1)*Math.sin(t2)*Math.sin(t4)+Math.cos(t2)*Math.sin(t3)*Math.sin(t1)*Math.cos(t4))*l2+Math.sin(t1)*Math.sin(t2)*l1;
            J[2][2] = (Math.sin(t1)*Math.sin(t2)*Math.cos(t3)-Math.cos(t1)*Math.sin(t3))*Math.cos(t4)*l2;
            J[2][3] = (-Math.sin(t1)*Math.cos(t2)*Math.cos(t4)-(Math.sin(t1)*Math.sin(t2)*Math.sin(t3)+Math.cos(t1)*Math.cos(t3))*Math.sin(t4))*l2;
            J[2][4] = 0.0;
            // --from matlab up

            final Point3d DX=obj.error_vector(this);
            for (int i=0;i<5;i++)
            {
                dTheta[i]=(J[0][i]*DX.x+J[1][i]*DX.y+J[2][i]*DX.z)*0.0000005;
                if (dTheta[i]>0.1)
                    dTheta[i]=0.1;
                if (dTheta[i]<-0.1)
                    dTheta[i]=-0.1;
            }
            for (int i=0;i<5;i++)
                constrainedRotate(seg[i+1],dTheta[i]);
                //collisionRotate(seg[i+1],dTheta[i],null);
        }
        return obj.grasp_error(this);
    }

    synchronized private double jacobianTransposeForVia(final int N, final Point3d viaPoint)
    {
        for (int k=0;k<N;k++)
        {
            final double t1=seg[1].beta;
            final double t2=seg[2].beta;
            final double t3=seg[3].beta;
            final double t4=seg[4].beta;

            // ---from matlab
            J[0][0] = 0.0;
            J[0][1] = (-Math.cos(t2)*Math.sin(t4)+Math.sin(t2)*Math.sin(t3)*Math.cos(t4))*l2-Math.cos(t2)*l1;
            J[0][2] = -Math.cos(t2)*Math.cos(t3)*Math.cos(t4)*l2;
            J[0][3] = (-Math.sin(t2)*Math.cos(t4)+Math.cos(t2)*Math.sin(t3)*Math.sin(t4))*l2;
            J[0][4] = 0.0;

            J[1][0] = (Math.sin(t1)*Math.cos(t2)*Math.sin(t4)+(-Math.sin(t1)*Math.sin(t2)*Math.sin(t3)-Math.cos(t1)*Math.cos(t3))*Math.cos(t4))*l2+Math.sin(t1)*Math.cos(t2)*l1;
            J[1][1] = (Math.cos(t1)*Math.sin(t2)*Math.sin(t4)+Math.cos(t2)*Math.sin(t3)*Math.cos(t1)*Math.cos(t4))*l2+Math.cos(t1)*Math.sin(t2)*l1;
            J[1][2] = (Math.cos(t1)*Math.sin(t2)*Math.cos(t3)+Math.sin(t1)*Math.sin(t3))*Math.cos(t4)*l2;
            J[1][3] = (-Math.cos(t1)*Math.cos(t2)*Math.cos(t4)-(Math.cos(t1)*Math.sin(t2)*Math.sin(t3)-Math.sin(t1)*Math.cos(t3))*Math.sin(t4))*l2;
            J[1][4] = 0.0;

            J[2][0] = (-Math.cos(t1)*Math.cos(t2)*Math.sin(t4)+(Math.cos(t1)*Math.sin(t2)*Math.sin(t3)-Math.sin(t1)*Math.cos(t3))*Math.cos(t4))*l2-Math.cos(t1)*Math.cos(t2)*l1;
            J[2][1] = (Math.sin(t1)*Math.sin(t2)*Math.sin(t4)+Math.cos(t2)*Math.sin(t3)*Math.sin(t1)*Math.cos(t4))*l2+Math.sin(t1)*Math.sin(t2)*l1;
            J[2][2] = (Math.sin(t1)*Math.sin(t2)*Math.cos(t3)-Math.cos(t1)*Math.sin(t3))*Math.cos(t4)*l2;
            J[2][3] = (-Math.sin(t1)*Math.cos(t2)*Math.cos(t4)-(Math.sin(t1)*Math.sin(t2)*Math.sin(t3)+Math.cos(t1)*Math.cos(t3))*Math.sin(t4))*l2;
            J[2][4] = 0.0;
            // --from matlab up

            final Point3d DX=Graspable.via_error_vector(this,viaPoint);
            for (int i=0;i<5;i++)
            {
                dTheta[i]=(J[0][i]*DX.x+J[1][i]*DX.y+J[2][i]*DX.z)*0.0000005;
                if (dTheta[i]>0.1)
                    dTheta[i]=0.1;
                if (dTheta[i]<-0.1)
                    dTheta[i]=-0.1;
            }
            for (int i=0;i<4;i++)
                constrainedRotate(seg[i+1],dTheta[i]);
                //collisionRotate(seg[i+1],dTheta[i],null);
        }
        return Graspable.via_error(this,viaPoint);
    }

    /**
     * Shows the joint control frame
     */
    public void showArmHandFrame()
    {
        ahFrame=new ArmHandFrame(this);
        ahFrame.setBounds(600,100,400,500);
        ahFrame.setSize(400,500);
        ahFrame.setVisible(true);
    }

    /**
     * Closes the joint control frame
     */
    public void closeArmHandFrame()
    {
        if (ahFrame==null)
            return;
        ahFrame.closeSelf();
        ahFrame=null;
    }

    /**
     * Toggles the visibility of the joint control frame
     */
    public void toggleArmHandFrame()
    {
        if (ahFrame==null || !ahFrame.isVisible())
            showArmHandFrame();
        else
            closeArmHandFrame();
    }

    /**
     * Enables segment panels om tje joint control frame
     */
    public void enablePanels()
    {
        for (int i=0;i<segc;i++)
            seg[i].enablePanel();
        root.updateAllPanel();
    }

    public static int collisionRotate(Segment seg,double ang,Object3d o)
    {
        //double save=seg.beta;
        if (seg.blocked)
            return 1; //it has collided before
        if (seg.jointconstraint)
        {
            if (seg.beta+ang > seg.maxbeta)
            {
                ang=seg.maxbeta-seg.beta;
            }
            if (seg.beta+ang < seg.minbeta)
            {
                ang=seg.minbeta-seg.beta;
            }
        }

        // if at the joint limit
        if (ang==0)
        {
            if (seg.seg_pan!=null)
                seg.update_panel();
            seg.blocked=true;
            return 1;
        }
        else
            seg.rotateJoint(ang); // this update automatically
        double colv=new_segmentCollision(seg,o);

        if (colv>0.0001)
        {
            seg.blocked=true;
            return 1;
        }
        return 0;
    }

    /**
     * Rotates the joint to the given angle
     * @param seg - The joint to rotate
     * @param ang - The desired angle
     */
    public static void setJointAngle(final Segment seg, final double ang)
    {
        seg.rotateJoint(ang-seg.beta);
    }

    public void restoreAngles(double[] teta)
    {
        for (int i=0;i<segc;i++)
            setJointAngle(seg[i],teta[i]);
    }

    /**
     * Stores all joint angles in teta
     * @param teta - Storage array for joint angles
     */
    public void storeAngles(final double[] teta)
    {
        for (int i=0;i<segc;i++)
            teta[i]=seg[i].beta;
    }

    public void weighted_restoreAngles(double[] teta, double W)
    {
        for (int i=0;i<segc;i++)
            setJointAngle(seg[i],W*teta[i]+(1-W)*seg[i].beta);
    }

    public int findSegment(String s)
    {
        for (int i=0;i<segc;i++)
            if (seg[i].label.equals(s))
                return i;
        return -1;
     }

    public void grabObject(Graspable graspable)
    {
        graspObject(-1, 0, graspable);
    }

    // grab but excluse joint exclude.jon. If jon==-1 means exclude.*
    // exclude=-1 means no exclusion
    public void graspObject(final int exclude, final int jon, Graspable graspable)
    {
        final double prec=5*Math.PI/180;
        final int max=(int)(0.5+Math.PI/2.2/prec);

        for (int j=2;j<=3;j++)
        {
            for (int i=1;i<=4;i++) //run the fingers
            {
                if (i==exclude && (j==jon || jon==-1))
                    continue;
                int c=0;
                final Segment pick= seg[fingers[i][j]];
                pick.resetJoints();
                Log.println("Picked:"+pick.label);
                do
                {
                    constrainedRotate(pick,prec);
                    //collisionRotate(pick,prec,null);
                    Main.refreshDisplay();
                } while (segmentCollision(graspable) <= 0.0001 && c++ < max);
                constrainedRotate(pick,-2*prec);
                //collisionRotate(pick,-2*prec,null);
            }
        }
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


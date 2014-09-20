package sim.main;

import sim.graphics.*;
import sim.util.Resource;
import sim.util.Elib;
import sim.util.Log;
import sim.util.FrameUtils;
import sim.motor.Graspable;
import sim.motor.ArmHand;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Oct 17, 2005
 * Time: 10:05:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main extends JFrame implements ActionListener, MouseListener, MouseMotionListener, ChangeListener
{
    public static boolean bellshape = true;
    public static Main self;
    public static String RESfile = "KolParameters.res";
    public static MainPanel cv;
    protected EyeMove eyemove;
    public ArmHand otherArmHand;

    // scale is screen mag value for the eye
    public int rx = 30, ry = -90, rz = 1300, sc = 1, objsc = 10;
    public double object_scale_fix = 1;
    static public String[] grasps = {"NATURAL", "SIDE", "POWER", "PRECISION", "SLAP"};//, "POWER2","REACHONLY"};
    public static int graspi;
    public static int maxgraspc = grasps.length;
    public static boolean doubleBuffering = true;
    public static boolean recordCanvas;
    //public static int recordSession;  // for Canvas
    public static int recordFrame;    // for Canvas
    public static String recordImageBase = "canvas/";  // for Canvas
    public static String recordPrefix = ""; // for Canvas
    public static int recordImageFormat = 0; // 0=gif, 1=ppm
    // Debug level
    public static int DLEV;

    // Animation level
    public static int FANCY;

    public static boolean traceon;

    public static Palette pal;

    public Graspable curObj = null;
    public Graspable obsObj = null;
    protected int tiltAngle;

    protected boolean firsttimer = true;

    protected static int minPAR, maxPAR, minMER, maxMER, minRAD, maxRAD;

    public int oldy = -1, oldx = -1;

    protected boolean _reachDone = true;

    public static boolean render = true;

    protected String[] objNames, objDefinitions, objGrasps;
    protected int[] objSidec, objRad;
    protected double[] objScales;

    protected JTextField commandTextField;
    protected JPanel mainPanel, canvasPanel, sliderPanel, toolbarPanel, buttonPanel;
    protected JSlider scale, xang, yang, zang, objscale;
    protected JTextField infoLabel;
    protected JButton quitButton;
    protected JButton resetEyeButton;
    protected JButton xEyeButton;
    protected JButton yEyeButton;
    protected JButton zEyeButton;
    protected JButton tiltTargetButton;
    protected JCheckBox obstacleVisibleCheckbox;
    protected JComboBox obstacleTypeCombo;
    protected JCheckBox targetVisibleCheckbox;
    protected JComboBox targetTypeCombo;
    protected JButton recordCanvasButton;
    protected JButton breakButton;
    protected JCheckBox armVisibleCheckbox;
    protected JButton planReachButton;
    protected JButton clearTrajectoryButton;
    protected JCheckBox audibleCheckbox;
    protected JButton reachButton;
    protected JButton bringToMouthButton;
    protected JButton resetArmButton;
    protected JButton jointControlButton;
    public JComboBox speedCombo;
    protected JLabel speedLabel;
    protected JButton executeButton;
    protected JLabel targetTypeLabel;
    protected JLabel obstacleTypeLabel;
    protected JComboBox graspTypeCombo;
    protected JLabel graspTypeLabel;
    protected JCheckBox bellshapeCheckbox;
    private JButton generateDataButton;

    public Main()
    {
    }

    public Main(String argv[])
    {
        readConfig();
        final String armHandFile;
        int rad = 20;
        int sidec = 4;
        if (argv != null)
        {
            if (argv.length > 0)
                armHandFile = argv[0];
            else
                armHandFile = Resource.getString("defaultArmHandFile");
            if (argv.length >= 3)
            {
                sidec = Elib.toInt(argv[1]);
                rad = Elib.toInt(argv[2]);
            }
            if (argv.length > 3)
            {
                if (argv[3].toLowerCase().equals("false"))
                    render = false;
            }
        }
        else
            armHandFile = Resource.getString("defaultArmHandFile");

        rx = 20;
        ry = (Main.minMER + Main.maxMER) >> 1;
        rz = Main.maxRAD; //1125

        Mars.clearStars(1);  //create the lists
        Mars.clearStars(2);  //create the lists

        Main.self = this;

        prepareMain(armHandFile, sidec, rad);
        sc = (int) (0.5 + Mars.eye.Mag / 5.0);
        setupLayout();
        updateScrollValues();

        //setupArms();
        setGrasp("NATURAL");
        setObject(targetTypeCombo.getSelectedItem().toString());
        setObsObject(obstacleTypeCombo.getSelectedItem().toString());

        Mars.project(); // firs project so that first repaint works OK.

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
        enableEvents(AWTEvent.FOCUS_EVENT_MASK);

        if (render)
        {
            refreshDisplay();
            Main.cv.addMouseListener(this);
            Main.cv.addMouseMotionListener(this);
        }
    }

    protected void setupLayout()
    {
        add(mainPanel);

        scale.setMinimum(1);
        scale.setMaximum(901);
        scale.setValue(sc);
        scale.addChangeListener(this);

        objscale.setMinimum(1);
        objscale.setMaximum(101);
        objscale.setValue(objsc);
        objscale.addChangeListener(this);

        xang.setMinimum(Main.minPAR);
        xang.setMaximum(Main.maxPAR + 1);
        xang.setValue(rx);
        xang.addChangeListener(this);

        yang.setMinimum(Main.minMER);
        yang.setMaximum(Main.maxMER + 1);
        yang.setValue(ry);
        yang.addChangeListener(this);

        zang.setMinimum(Main.minRAD);
        zang.setMaximum(Main.maxRAD + 1);
        zang.setValue(rz);
        zang.addChangeListener(this);

        quitButton.addActionListener(this);
        resetEyeButton.addActionListener(this);
        xEyeButton.addActionListener(this);
        yEyeButton.addActionListener(this);
        zEyeButton.addActionListener(this);

        obstacleVisibleCheckbox.addActionListener(this);

        obstacleTypeCombo.addItem("PENT");
        obstacleTypeCombo.addItem("BOX");
        obstacleTypeCombo.addItem("SHEET");
        obstacleTypeCombo.addItem("BAR");
        obstacleTypeCombo.addItem("PLATE");
        obstacleTypeCombo.addItem("COIN");
        obstacleTypeCombo.addItem("MUG");
        obstacleTypeCombo.addItem("SCREEN");
        obstacleTypeCombo.setSelectedItem("BAR");
        obstacleTypeCombo.addActionListener(this);

        targetVisibleCheckbox.addActionListener(this);

        targetTypeCombo.addItem("PENT");
        targetTypeCombo.addItem("BOX");
        targetTypeCombo.addItem("SHEET");
        targetTypeCombo.addItem("BAR");
        targetTypeCombo.addItem("PLATE");
        targetTypeCombo.addItem("COIN");
        targetTypeCombo.addItem("MUG");
        targetTypeCombo.addItem("SCREEN");
        targetTypeCombo.setSelectedItem("BOX");
        targetTypeCombo.addActionListener(this);

        tiltTargetButton.addActionListener(this);

        for (int i = 0; i < Main.grasps.length; i++)
            graspTypeCombo.addItem(Main.grasps[i]);
        graspTypeCombo.setSelectedItem("NATURAL");
        graspTypeCombo.addActionListener(this);

        bellshapeCheckbox.addActionListener(this);
        armVisibleCheckbox.addActionListener(this);
        recordCanvasButton.addActionListener(this);
        audibleCheckbox.addActionListener(this);
        breakButton.addActionListener(this);
        planReachButton.addActionListener(this);
        clearTrajectoryButton.addActionListener(this);
        reachButton.addActionListener(this);
        bringToMouthButton.addActionListener(this);
        resetArmButton.addActionListener(this);
        jointControlButton.addActionListener(this);
        for (int i = 1; i < 11; i++)
            speedCombo.addItem((new Integer(i)).toString());
        speedCombo.setSelectedItem("3");
        speedCombo.addActionListener(this);
        executeButton.addActionListener(this);

        setTitle("3D Hand - Erhan Oztop Jan2000");

        Main.pal = new Palette(256);
        Main.pal.spread(20, 20 + 31, 175, 175, 175, 255, 255, 255);
        Main.pal.spread(20 + 32, 20 + 32 + 31, 75, 75, 75, 150, 150, 150);
        if (render)
        {
            Main.cv = new MainPanel();
            Main.cv.setBackground(Color.black);
            canvasPanel.add(Main.cv);
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds(0, 0, (int) d.getWidth() - 50, (int) d.getHeight() - 100);
            setVisible(true);
        }
    }

    public static void refreshDisplay()
    {
        if (cv != null && render)
        {
            cv.refreshDisplay();
        }
    }

    protected static void readConfig()
    {
        Resource.read(Main.RESfile);
        Main.minPAR = Resource.getInt("minPAR");
        Main.maxPAR = Resource.getInt("maxPAR");
        Main.minMER = Resource.getInt("minMER");
        Main.maxMER = Resource.getInt("maxMER");
        Main.minRAD = Resource.getInt("minRAD");
        Main.maxRAD = Resource.getInt("maxRAD");
        Main.DLEV = Resource.getInt("DLEV");
        Main.FANCY = Resource.getInt("FANCY");
    }

    /*protected void setupArms()
    {
        otherArmHand.makeUpright();
    }*/

    public String setInfo(String s)
    {
        if (DLEV >= 0)
        {
            infoLabel.setText(s);
            Dimension d = self.canvasPanel.getSize();
            cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
        return s;
    }

    public void setInfoReady()
    {
        if (DLEV >= 0)
        {
            infoLabel.setText("Ready.");
            Dimension d = self.canvasPanel.getSize();
            cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
    }

    public void setRectTargetPosition(double target_x, double target_y, double target_z)
    {
        if (curObj != null)
        {
            curObj.rect_moveto(target_x, target_y, target_z);
            if (Main.FANCY > 0)
                setInfo("Object position:" + curObj.objectCenter.str());
        }
    }

    public void setRectTargetPosition(int target_x, int target_y, int target_z)
    {
        if (curObj != null)
        {
            curObj.rect_moveto(target_x, target_y, target_z);
            if (Main.FANCY > 0)
                setInfo("Object position:" + curObj.objectCenter.str());
        }
    }

    public void setTargetPosition(double target_x, double target_y, double target_z)
    {
        if (curObj != null)
        {
            curObj.moveto(otherArmHand.root.joint_pos, target_x, target_y, target_z);
            if (Main.FANCY > 0 && curObj.objectCenter != null)
                setInfo("Object position:" + curObj.objectCenter.str());
        }
    }

    public void setTargetPosition(int target_x, int target_y, int target_z)
    {
        if (curObj != null)
        {
            curObj.moveto(otherArmHand.root.joint_pos, target_x, target_y, target_z);
            if (Main.FANCY > 0 && curObj.objectCenter != null)
                setInfo("Object position:" + curObj.objectCenter.str());
        }
    }

    public void setTargetScale(double old, double newv)
    {
        if (curObj != null)
        {
            final Point3d p = curObj.objectCenter.duplicate();
            curObj.rect_moveto(0, 0, 0);
            curObj.root.scale(newv / old);
            object_scale_fix *= newv / old;
            curObj.rect_moveto(p);
        }
    }

    public void setRectObstaclePosition(double x, double y, double z)
    {
        if (obsObj != null)
        {
            obsObj.rect_moveto(x, y, z);
            if (Main.FANCY > 0)
                setInfo("Obstacle position:" + obsObj.objectCenter.str());
        }
    }

    public void setRectObstaclePosition(int x, int y, int z)
    {
        if (obsObj != null)
        {
            obsObj.rect_moveto(x, y, z);
            if (Main.FANCY > 0)
                setInfo("Obstacle position:" + obsObj.objectCenter.str());
        }
    }

    public void setObstaclePosition(double x, double y, double z)
    {
        if (obsObj != null)
        {
            obsObj.moveto(otherArmHand.root.joint_pos, x, y, z);
            if (Main.FANCY > 0)
                setInfo("Obstacle position:" + obsObj.objectCenter.str());
        }
    }

    public void setObstaclePosition(int x, int y, int z)
    {
        if (obsObj != null)
        {
            obsObj.moveto(otherArmHand.root.joint_pos, x, y, z);
            if (Main.FANCY > 0)
                setInfo("Obstacle position:" + obsObj.objectCenter.str());
        }
    }

    public void setObstacleScale(double old, double newv)
    {
        if (obsObj != null)
        {
            final Point3d p = obsObj.objectCenter.duplicate();
            obsObj.rect_moveto(0, 0, 0);
            obsObj.root.scale(newv / old);
            object_scale_fix *= newv / old;
            obsObj.rect_moveto(p);
        }
    }

    void setObsObject(int i, boolean setObject)
    {
        setObsObject("Next in object list", i, setObject);
    }

    public void setObsObject(String s)
    {
        int k = -1;
        for (int i = 0; i < objNames.length; i++)
        {
            if (objNames[i].equals(s))
            {
                k = i;
                break;
            }
        }

        if (k != -1)
            setObsObject(s, k, true);
    }

    protected void setObsObject(String s, int i, boolean setObject)
    {
        if (i >= objNames.length)
            return;
        if (obsObj != null)
        {
            for (int j = 0; j < objDefinitions.length; j++)
            {
                if (objDefinitions[j].equals(obsObj.myname) && Mars.getObject(objNames[j]) != null &&
                        ((Graspable) Mars.getObject(objNames[j])).obstacle)
                {
                    Mars.removeObject(objNames[j]);
                    break;
                }
            }
        }
        obsObj = new Graspable(otherArmHand, objDefinitions[i], objSidec[i], objRad[i], objGrasps[i]);
        obsObj.obstacle = true;
        obsObj.root.scale(objScales[i]);
        Mars.addObject(objNames[i], obsObj);
        obsObj.visible = obstacleVisibleCheckbox.isSelected();
        obsObj.noshow = !obstacleVisibleCheckbox.isSelected();
        rx = -3121;
        ry = -1415; //whatever
        if (!firsttimer)
            setInfo("Ready.  - obstacle object set to " + s /*objlist[i].root.label*/);
        else
            firsttimer = false;
        updateScrollValues();
    }

    public void setObject(String s)//, boolean setObsObject)
    {
        int k = -1;
        for (int i = 0; i < objNames.length; i++)
        {
            if (objNames[i].equals(s))
            {
                k = i;
                break;
            }
        }
        if (k != -1)
            setObject(s, k);//,setObsObject);
    }

    protected void setObject(String s, int i)//, boolean setObsObject)
    {
        if (i >= objNames.length)
            return;
        Point3d lastOppositionAxis = null;
        if (curObj != null)
        {
            for (int j = 0; j < objDefinitions.length; j++)
            {
                if (objDefinitions[j].equals(curObj.myname) && !((Graspable) Mars.getObject(objNames[j])).obstacle)
                {
                    Mars.removeObject(objNames[j]);
                    break;
                }
            }
            lastOppositionAxis = curObj.lastopposition;
        }
        curObj = new Graspable(otherArmHand, objDefinitions[i], objSidec[i], objRad[i], objGrasps[i]);
        if (lastOppositionAxis != null)
            curObj.lastopposition = lastOppositionAxis;
        Mars.addObject(objNames[i], curObj);
        curObj.root.scale(objScales[i]);
        curObj.visible = targetVisibleCheckbox.isSelected();
        curObj.noshow = !targetVisibleCheckbox.isSelected();
        rx = -3121;
        ry = -1415; //whatever
        if (!firsttimer)
            setInfo("Ready.  - object set to " + s /*objlist[i].root.label*/);
        else
            firsttimer = false;
        updateScrollValues();
    }

    protected static void setGrasp(String s)
    {
        int j = -1;
        for (int i = 0; i < Main.maxgraspc; i++)
            if (Main.grasps[i].equals(s))
                j = i;
        if (j != -1)
            Main.graspi = j;
    }

    public void prepareMain(String armHandFile, int sidec, int rad)
    {
        otherArmHand = new ArmHand(armHandFile, sidec, rad);
        final Eye eye;
        if (otherArmHand.root.suggested_scale == 0)
            eye = new Eye(10, 20); // create an eye
        else
            eye = new Eye(otherArmHand.root.suggested_F, otherArmHand.root.suggested_scale);

        eye.lock(0, 0, 0);
        eye.YrotateViewPlane(Math.PI / 25);
        eye.XrotateViewPlane(Math.PI / 5);
        objNames = new String[]{"SHEET", "BAR", "BOX", "PENT", "PLATE", "COIN", "MUG", "SCREEN"};
        objDefinitions = new String[]{"objects/sheet.seg", "objects/ibar.seg", "objects/box.seg", "objects/pent.seg",
                "objects/plate.seg", "objects/coin.seg", "objects/ring.seg", "objects/screen.seg"};
        objSidec = new int[]{0, 6, 0, 0, 0, 7, 5, 0};
        objRad = new int[]{5, 100, 5, 5, 5, 100, 50, 5};
        objGrasps = new String[]{"SIDE", "PRECISION", "PRECISION", "POWER", "PRECISION", "SIDE", "POWER", "PRECISION"};
        objScales = new double[]{1.0, 1.0, 1.0, 0.4, 1.0, 1.0, 0.5, 1, 0};

        Mars.addObject("HAND", otherArmHand);

        Mars.setEye(eye);
        Mars.setCube(1900 * 1.3);
    }

    public static void main(String[] argv)
    {
        new Main(argv);     // when applet this is done by netscape
    }

// -------------------------------------------------------------------
// AWT events

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        final Point p = Main.cv.getLocation();
        x -= p.x;
        y -= p.y;
        oldx = x;
        oldy = y;
    }

    public void mouseReleased(MouseEvent e)
    {
        oldx = -1;
        oldy = -1;
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseDragged(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        final Point p = Main.cv.getLocation();
        x -= p.x;
        y -= p.y;
        if (oldx < 0)
        {
            oldx = x;
            oldy = y;
        }
        else
        {
            final int dy = y - oldy;
            final int dx = x - oldx;
            oldx = x;
            oldy = y;
            if (e.getModifiers() == MouseEvent.BUTTON1_MASK)
                Mars.eye.adjustViewPlane(dx, dy);
            else if (e.getModifiers() == MouseEvent.BUTTON3_MASK)
                Mars.eye.setMag(Mars.eye.Mag + dy * 5);
            Mars.project();
            Main.cv.repaint();
        }
    }

    public void mouseMoved(MouseEvent e)
    {
    }

    public void updateScrollValues()
    {
        if (objscale != null && xang != null && yang != null && zang != null && scale != null)
        {
            final int newosc = objscale.getValue();
            final int newrx = xang.getValue();
            final int newry = yang.getValue();
            final int newrz = zang.getValue();
            final int newsc = scale.getValue();
            if (newsc != sc)
            {
                Mars.eye.setMag(5 * newsc);
                sc = newsc;
            }

            if (newrx != rx || newry != ry || newrz != rz)
            {
                setTargetPosition(-newrx * Math.PI / 180, -newry * Math.PI / 180, newrz);
                rx = newrx;
                ry = newry;
                rz = newrz;
            }
            if (newosc != objsc)
            {
                setTargetScale(objsc / 10.0 + 0.1, newosc / 10.0 + 0.1);
                objsc = newosc;
            }
            refreshDisplay();
        }
    }

    public boolean executeCommand(String com)
    {
        final String[] pars = new String[40];
        for (int i = 0; i < pars.length; i++)
            pars[i] = null;

        final StringTokenizer st = new StringTokenizer(com, " ");
        int parc = 0;
        while (st.hasMoreTokens())
        {
            pars[parc++] = st.nextToken();
        }
        final String command = pars[0];
        if (command == null)
        {
            setInfo("Nothing to execute!");
            return false;
        }
        if (command.equals("tilt"))
        {
            if (parc == 1)
            {
                if (curObj != null)
                    curObj.resetRot();
            }
            else
            {
                if (curObj != null)
                    curObj.setTilt(Math.PI / 180 * Elib.toDouble(pars[1]));
            }
            refreshDisplay();
            return true;
        }
        if (command.equals("gif"))
        {
            String fname = "snapshot.gif";
            if (parc == 2)
                fname = pars[1];
            if (Main.doubleBuffering)
            {
                Log.println(setInfo("Writing snapshot " + fname));
                FrameUtils.createGif(fname);
            }
            else
                Log.println(setInfo("Double buffering must be on for gif snapshot!"));
            return true;
        }
        else if (command.equals("ppm"))
        {
            String fname = "snapshot.ppm";
            if (parc == 2)
                fname = pars[1];
            if (Main.doubleBuffering)
            {
                Log.println(setInfo("Writing snapshot " + fname));
                FrameUtils.createPpm(fname);
            }
            else
                Log.println(setInfo("Double buffering must be on for ppm snapshot!"));
            return true;
        }
        if (command.equals("clearStars"))
        {
            if (parc < 2)
            {
                setInfo("Need parameter");
                return false;
            }
            int num = 1;
            try
            {
                num = Integer.parseInt(pars[1]);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            Mars.clearStars(num);
            return true;
        }
        if (command.equals("get"))
        {
            if (parc < 2)
            {
                setInfo("Need parameter");
                return false;
            }
            if (pars[1].equals("softconGAIN"))
            {
                setInfo("Graspable.softconGAIN is : " + Graspable.softconGAIN);
                return true;
            }
            if (pars[1].equals("objectpos"))
            {
                setInfo("Object is at:" + curObj.objectCenter.str());
                return true;
            }
            if (pars[1].equals("obstaclepos"))
            {
                if (obsObj != null)
                    setInfo("Obstacle is at:" + obsObj.objectCenter.str());
                return true;
            }
            if (pars[1].equals("view"))
            {
                String s = "View is : (" + Mars.eye.Fpos.x + ',' + Mars.eye.Fpos.y + ',' + Mars.eye.Fpos.z + ") , (" + Mars.eye.X.x + ',' + Mars.eye.X.y + ',' + Mars.eye.X.z + ") , (" + Mars.eye.Y.x + ',' + Mars.eye.Y.y + ',' + Mars.eye.Y.z + ") , (" + Mars.eye.Z.x + ',' + Mars.eye.Z.y + ',' + Mars.eye.Z.z + "), " + Mars.eye.getMag();
                Log.println(s);
                setInfo(s);
                return true;
            }
            setInfo("Unknown parameter name!");
            return false;
        }

        if (command.equals("set"))
        {
            if (parc < 3)
            {
                setInfo("Need parameter and a value");
                return false;
            }

            if (pars[1].equals("bgcolor"))
            {
                MainPanel.backColor = new Color(Elib.toInt(pars[2]), Elib.toInt(pars[3]), Elib.toInt(pars[4]));
                return true;
            }
            if (pars[1].equals("softconGAIN"))
            {
                Graspable.softconGAIN = Elib.toDouble(pars[2]);
                setInfo("Graspable.softconGAIN is set to:" + Graspable.softconGAIN);
                return true;
            }
            if (pars[1].equals("objectpos"))
            {
                final double x = Elib.toDouble(pars[2]);
                final double y = Elib.toDouble(pars[3]);
                final double z = Elib.toDouble(pars[4]);
                Log.println("setObjPosition is being called from executecommand=set with xyz.");
                Log.println("x" + x + 'y' + y + 'z' + z);
                setRectTargetPosition(x, y, z);
                refreshDisplay();
                setInfo("Object is at:" + curObj.objectCenter.str());
                return true;
            }
            if (pars[1].equals("obstaclepos"))
            {
                final double x = Elib.toDouble(pars[2]);
                final double y = Elib.toDouble(pars[3]);
                final double z = Elib.toDouble(pars[4]);
                Log.println("setObsPosition is being called from executecommand=set with xyz.");
                Log.println("x" + x + 'y' + y + 'z' + z);
                setRectObstaclePosition(x, y, z);
                refreshDisplay();
                setInfo("Obstacle is at:" + obsObj.objectCenter.str());
                return true;
            }
            setInfo("Unknown parameter name!");
            return false;
        }
        if (command.equals("softcon+"))
        {
            ArmHand.softcon = Graspable.softcon_J2_45;
            setInfo("Softconstraint in gradient_arm is activated.");
            return true;
        }
        if (command.equals("softcon-"))
        {
            ArmHand.softcon = 0;
            setInfo("Softconstraint in gradient_arm is deactivated.");
            return true;
        }
        if (command.equals("dlev+"))
        {
            DLEV = 1;
            setInfo("Now -> Main.DLEV:" + DLEV);
            return true;
        }
        if (command.equals("dlev-"))
        {
            DLEV = 0;
            setInfo("Now -> Main.DLEV:" + DLEV);
            return true;
        }
        if (command.equals("cube+"))
        {
            Mars.drawCube = 1;
            setInfo("drawCube enabled. Use cube- to turnoff...");
            return true;
        }
        if (command.equals("cube-"))
        {
            Mars.drawCube = 0;
            setInfo("drawCube disabled. Use cube+ to turnon...");
            return true;
        }
        if (command.equals("fancy+"))
        {
            FANCY = 1;
            setInfo("Now -> Main.FANCY:" + FANCY);
            return true;
        }
        if (command.equals("fancy-"))
        {
            FANCY = 0;
            setInfo("Now -> Main.FANCY:" + FANCY);
            return true;
        }
        if (command.equals("Hand.visible+"))
        {
            setArmVisibility(otherArmHand, true);
            setInfo("Now -> Main.rHand.visible:" + otherArmHand.visible);
            return true;
        }
        if (command.equals("Hand.visible-"))
        {
            setArmVisibility(otherArmHand, false);
            setInfo("Now -> Main.rHand.visible:" + otherArmHand.visible);
            return true;
        }
        if (command.equals("congruentSound+"))
        {
            otherArmHand.congruentSound = true;
            setInfo("Now -> congruentSound:" + otherArmHand.congruentSound);
            return true;
        }
        if (command.equals("congruentSound-"))
        {
            otherArmHand.congruentSound = false;
            setInfo("Now -> congruentSound:" + otherArmHand.congruentSound);
            return true;

        }
        if (command.equals("help"))
        {
            System.out.println("Main- shell command list (may be incomplete see Main.java)");
            System.out.println("fancy+/:  Flag for the amount of grasphics update and visual helps");
            System.out.println("dlev+/ :  Flag for debug information");
            System.out.println("Hand.visible+/-: turns on/off hand visibility");
            System.out.println("Object.visible+/-: turns on/off object visibility");
            System.out.println("audible+/-: turns on/off action audibility");
            System.out.println("congruentSound+/-: turns on/off audio-visual congruency");
            return true;
        }
        setInfo("No such command!");
        return false;
    }

    public static void createImage(String fname)
    {
        if (recordImageFormat == MainPanel.IMAGE_FORMAT_GIF)
            FrameUtils.createGif(fname);
        else if (recordImageFormat == MainPanel.IMAGE_FORMAT_PPM)
            FrameUtils.createPpm(fname);
    }

    public static void createImage()
    {
        if (recordImageFormat == MainPanel.IMAGE_FORMAT_GIF)
            FrameUtils.createGif();
        else if (recordImageFormat == MainPanel.IMAGE_FORMAT_PPM)
            FrameUtils.createPpm();
    }

    protected void processWindowEvent(WindowEvent e)
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
            exitMain();
    }

    protected void processComponentEvent(ComponentEvent e)
    {
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED)
        {
            final Dimension d = self.canvasPanel.getSize();
            Main.cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            Main.cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
    }

    public void stateChanged(ChangeEvent e)
    {
        updateScrollValues();
    }

    /**
     * Modifies visibility of the arm and hand.
     *
     * @param arm     - the arm/hand to modify
     * @param visible - the new value of the arm/hand's visibility
     */
    public void setArmVisibility(ArmHand arm, boolean visible)
    {
        arm.visible = visible;
        arm.noshow = !visible;
        armVisibleCheckbox.setSelected(visible);

        refreshDisplay();
    }

    /**
     * Modifies the current target object's visibility.
     *
     * @param visible - the new value of the target object's visibility
     */
    public void setObjectVisibility(boolean visible)
    {
        curObj.visible = visible;
        curObj.noshow = !visible;
        targetVisibleCheckbox.setSelected(visible);
        refreshDisplay();
    }

    /**
     * Modifies the current target object's visibility.
     *
     * @param visible - the new value of the target object's visibility
     */
    public void setObstacleVisibility(boolean visible)
    {
        obsObj.visible = visible;
        obsObj.noshow = !visible;
        obstacleVisibleCheckbox.setSelected(visible);
        refreshDisplay();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource().equals(graspTypeCombo))
        {
            setGrasp(graspTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(targetTypeCombo))
        {
            setObject(targetTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(obstacleTypeCombo))
        {
            setObsObject(obstacleTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(obstacleVisibleCheckbox))
        {
            setObstacleVisibility(obstacleVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(bellshapeCheckbox))
        {
            Main.bellshape = bellshapeCheckbox.isSelected();
        }
        else if (e.getSource().equals(armVisibleCheckbox))
        {
            setArmVisibility(otherArmHand, armVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(targetVisibleCheckbox))
        {
            setObjectVisibility(targetVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(audibleCheckbox))
        {
            otherArmHand.audible = audibleCheckbox.isSelected();
        }
        else if (e.getSource().equals(resetArmButton))
        {
            otherArmHand.grasped = false;
            otherArmHand.makeStanding();
            refreshDisplay();
        }
        else if (e.getSource().equals(breakButton))
        {
            setInfo("Ready. -user interrupt");
            otherArmHand.kill_ifActive();
        }
        else if (e.getSource().equals(resetEyeButton))
        {
            Mars.eye.reset();
            Mars.project();
            if (render)
            {
                Main.cv.paint(Main.cv.getGraphics());
            }
        }
        else if (e.getSource().equals(xEyeButton))
        {
            if (!toggleEyeMove(0.1, 0, 0))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(yEyeButton))
        {
            if (!toggleEyeMove(0, 0.1, 0))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(zEyeButton))
        {
            if (!toggleEyeMove(0, 0, 0.1))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(jointControlButton))
        {
            otherArmHand.toggleArmHandFrame();
        }
        else if (e.getSource().equals(bringToMouthButton))
        {
            toggleReach("eat");
        }
        else if (e.getSource().equals(reachButton))
        {
            toggleReach("execute");
        }
        else if (e.getSource().equals(executeButton))
        {
            executeCommand(commandTextField.getText());
        }
        else if (e.getSource().equals(recordCanvasButton))
        {
            if (!Main.recordCanvas)
            {
                Main.recordFrame = 0;
                Main.recordCanvas = true;
                setInfo("Canvas recording ON. (Each refresh will be recorded).");
            }
            else
            {
                Main.recordCanvas = false;
                setInfo("Canvas recording OFF. Last session recorded " + Main.recordFrame + " frames.");
            }
        }
        else if (e.getSource().equals(clearTrajectoryButton))
        {
            otherArmHand.clearTrajectory();
        }
        else if (e.getSource().equals(planReachButton))
        {
            setInfo("VISREACH: solving inverse kinematics...");
            toggleReach("visual");
        }
        else if (e.getSource().equals(tiltTargetButton))
        {
            tiltAngle = (tiltAngle + 15) % 360;
            curObj.setTilt(tiltAngle * Math.PI / 180);
            setInfo("Object tilt angle is set to:" + tiltAngle);
            refreshDisplay();
        }
        else if (e.getSource().equals(quitButton))
        {
            exitMain();
        }
    }

    public void waitFinish()
    {
        try
        {
            while (true)
            {
                Thread.sleep(250);
                if (reachDone())
                    return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void reportFinish(String s)
    {
        _reachDone = true;
        Log.println("Reach thread finish:" + s);
    }

    synchronized public void reportStart(String s)
    {
        _reachDone = false;
        Log.println("Reach thread start:" + s);
    }

    synchronized public boolean reachDone()
    {
        return _reachDone;
    }

    public static void toggleTrace(int n)
    {
        if (Main.traceon)
        {
            setTrace(false);
        }
        else
        {
            setTrace(true);
            Mars.clearStars(n); // create new list
        }
    }

    static synchronized public void setTrace(boolean b)
    {
        Main.traceon = b;
        if (Main.traceon)
            Log.println("Trace is on now.");
        else
            Log.println("Trace is off now.");
    }

    static synchronized public void setTrace(boolean b, int n)
    {
        setTrace(b);
        if (Main.traceon)
            Mars.clearStars(n);
    }

    synchronized public void toggleReach(String s)
    {
        if (curObj == null)
            return;
        if (!otherArmHand.reachActive())
        {
            if (s.equals("visual"))
                curObj.computeAffordance(Main.grasps[Main.graspi], otherArmHand);

            if (s.equals("eat"))
            {
                otherArmHand.doEat(curObj, null, s);
            }
            else
            {
                if (obsObj != null && obsObj.visible)
                    otherArmHand.doReach(curObj, obsObj, s);
                else
                    otherArmHand.doReach(curObj, null, s);
            }
        }
        else
        {
            otherArmHand.kill_ifActive();
        }
    }

    synchronized protected boolean toggleEyeMove(double eye_x, double eye_y, double eye_z)
    {
        boolean start = false;
        if (eyemove == null)
        {
            eyemove = new EyeMove(Main.cv, Mars.eye);
            eyemove.dxrot = 0;
            eyemove.dyrot = 0;
            eyemove.dzrot = 0;
            start = true;
        }
        if (eyemove.dxrot != 0 && eye_x != 0)
        {
            eyemove.dxrot = 0;
            System.out.println("cease X rot");
        }
        else if (eye_x != 0)
        {
            System.out.println("Engage X rot");
            eyemove.dxrot = eye_x;
        }
        if (eyemove.dyrot != 0 && eye_y != 0)
        {
            eyemove.dyrot = 0;
            System.out.println("cease Y rot");
        }
        else if (eye_y != 0)
        {
            System.out.println("Engage Y rot");
            eyemove.dyrot = eye_y;
        }
        if (eyemove.dzrot != 0 && eye_z != 0)
        {
            eyemove.dzrot = 0;
            System.out.println("cease Z rot");
        }
        else if (eye_z != 0)
        {
            System.out.println("Engage Z rot");
            eyemove.dzrot = eye_z;
        }

        if (eyemove.dxrot == 0 && eyemove.dyrot == 0 && eyemove.dzrot == 0)
        {
            System.out.println("stopping...");
            eyemove.stopSelf();
            eyemove = null;
        }
        if (start)
        {
            try
            {
                eyemove.start();
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
        }
        return !(eyemove == null);
    }

    public static void exitMain()
    {
        System.exit(0);
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(toolbarPanel, gbc);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        toolbarPanel.add(buttonPanel, gbc);
        quitButton = new JButton();
        quitButton.setText("QUIT");
        quitButton.setToolTipText("Quit the simulator");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(quitButton, gbc);
        breakButton = new JButton();
        breakButton.setText("BREAK");
        breakButton.setToolTipText("Interrupt the current grasp");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(breakButton, gbc);
        speedCombo = new JComboBox();
        speedCombo.setToolTipText("Speed to execute the reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(speedCombo, gbc);
        graspTypeLabel = new JLabel();
        graspTypeLabel.setText("GRASP");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(graspTypeLabel, gbc);
        obstacleVisibleCheckbox = new JCheckBox();
        obstacleVisibleCheckbox.setText("VISIBLE");
        obstacleVisibleCheckbox.setToolTipText("Visibility of the obstacle object");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(obstacleVisibleCheckbox, gbc);
        obstacleTypeLabel = new JLabel();
        obstacleTypeLabel.setText("OBSTACLE");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(obstacleTypeLabel, gbc);
        planReachButton = new JButton();
        planReachButton.setText("PLAN");
        planReachButton.setToolTipText("Plan a reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(planReachButton, gbc);
        bellshapeCheckbox = new JCheckBox();
        bellshapeCheckbox.setLabel("BELLSHAPE");
        bellshapeCheckbox.setName("Whether or not to generate reach movements with bellshaped velocity profiles");
        bellshapeCheckbox.setSelected(true);
        bellshapeCheckbox.setText("BELLSHAPE");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(bellshapeCheckbox, gbc);
        armVisibleCheckbox = new JCheckBox();
        armVisibleCheckbox.setSelected(true);
        armVisibleCheckbox.setText("ARM VISIBLE");
        armVisibleCheckbox.setToolTipText("Visibility of the arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(armVisibleCheckbox, gbc);
        obstacleTypeCombo = new JComboBox();
        obstacleTypeCombo.setToolTipText("Type of obstacle object");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(obstacleTypeCombo, gbc);
        speedLabel = new JLabel();
        speedLabel.setText("SPEED");
        speedLabel.setToolTipText("Speed to execute the reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(speedLabel, gbc);
        reachButton = new JButton();
        reachButton.setText("REACH");
        reachButton.setToolTipText("Execute the planned reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(reachButton, gbc);
        targetTypeLabel = new JLabel();
        targetTypeLabel.setText("TARGET");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(targetTypeLabel, gbc);
        resetArmButton = new JButton();
        resetArmButton.setText("resetARM");
        resetArmButton.setToolTipText("Reset the arm configuration");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(resetArmButton, gbc);
        graspTypeCombo = new JComboBox();
        graspTypeCombo.setToolTipText("Type of grasp to plan and execute");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(graspTypeCombo, gbc);
        targetTypeCombo = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(targetTypeCombo, gbc);
        jointControlButton = new JButton();
        jointControlButton.setText("JOINTS");
        jointControlButton.setToolTipText("Manually control joint configurations");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(jointControlButton, gbc);
        bringToMouthButton = new JButton();
        bringToMouthButton.setText("EAT");
        bringToMouthButton.setToolTipText("Bring the grasped object to the mouth");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(bringToMouthButton, gbc);
        targetVisibleCheckbox = new JCheckBox();
        targetVisibleCheckbox.setSelected(true);
        targetVisibleCheckbox.setText("VISIBLE");
        targetVisibleCheckbox.setToolTipText("Visibility of the target object");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(targetVisibleCheckbox, gbc);
        resetEyeButton = new JButton();
        resetEyeButton.setText("resetEye");
        resetEyeButton.setToolTipText("Reset the eye angle");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(resetEyeButton, gbc);
        clearTrajectoryButton = new JButton();
        clearTrajectoryButton.setText("CLEAR");
        clearTrajectoryButton.setToolTipText("Clear the current reach trajectory");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(clearTrajectoryButton, gbc);
        tiltTargetButton = new JButton();
        tiltTargetButton.setText("TILT");
        tiltTargetButton.setToolTipText("Tilt the target object by 15 degrees about the z-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(tiltTargetButton, gbc);
        xEyeButton = new JButton();
        xEyeButton.setText("xEYE");
        xEyeButton.setToolTipText("Rotate eye around x-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(xEyeButton, gbc);
        recordCanvasButton = new JButton();
        recordCanvasButton.setText("RECORD");
        recordCanvasButton.setToolTipText("Write the canvas to an image file");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(recordCanvasButton, gbc);
        audibleCheckbox = new JCheckBox();
        audibleCheckbox.setSelected(true);
        audibleCheckbox.setText("AUDIBLE");
        audibleCheckbox.setToolTipText("Audibility of the grasp");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(audibleCheckbox, gbc);
        yEyeButton = new JButton();
        yEyeButton.setText("yEYE");
        yEyeButton.setToolTipText("Rotate eye around y-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(yEyeButton, gbc);
        executeButton = new JButton();
        executeButton.setText("EXECUTE");
        executeButton.setToolTipText("Execute the command");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(executeButton, gbc);
        zEyeButton = new JButton();
        zEyeButton.setText("zEYE");
        zEyeButton.setToolTipText("Rotate eye around z-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(zEyeButton, gbc);
        commandTextField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        toolbarPanel.add(commandTextField, gbc);
        infoLabel = new JTextField();
        infoLabel.setEditable(false);
        infoLabel.setText("3D Hand - Erhan Oztop Jan2000");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        toolbarPanel.add(infoLabel, gbc);
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(sliderPanel, gbc);
        xang = new JSlider();
        xang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(xang, gbc);
        yang = new JSlider();
        yang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(yang, gbc);
        zang = new JSlider();
        zang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(zang, gbc);
        scale = new JSlider();
        scale.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(scale, gbc);
        objscale = new JSlider();
        objscale.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(objscale, gbc);
        canvasPanel = new JPanel();
        canvasPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(canvasPanel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return mainPanel;
    }
}

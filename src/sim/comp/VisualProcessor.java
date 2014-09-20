package sim.comp;

/**
 * Processes all visual information - Hand state calculation, hand/object working memory, via point snapshots.
 */
public class VisualProcessor
{
    /**
     * Snapshots of joint angle via points.
     */
    public double[][] viaPointSnapshots;
    protected double[][] viaPointSnapshotTrace;
    /**
     * Ratios of the points in the sequence where the via point snapshots are taken from.
     */
    public double[] viaPointRatios;
    protected double[] viaPointRatiosTrace;
    /**
     * Index of the current via point.
     */
    public int viaPointIdx;
    /**
     * Speed threshold below which a via point snapshot is taken.
     */
    public static double viaPointSpeedThresh=0.02;
    /**
     * Level of noise in joint angle observation.
     */
    public static double viaPointNoiseLevel=0.01;

    public static int MAX_VIA_POINTS=100;

    public VisualProcessor(int numAngles)
    {
        reset(numAngles);
    }
    /**
     * Resets the via point snapshots.
     * @param numAngles - number of joint angles in each segment
     */
    public void reset(int numAngles)
    {
        viaPointSnapshots = new double[MAX_VIA_POINTS][numAngles];
        viaPointRatios = new double[MAX_VIA_POINTS];
        viaPointSnapshotTrace = new double[2][numAngles];
        viaPointRatiosTrace = new double[2];
        viaPointIdx=0;
    }
}
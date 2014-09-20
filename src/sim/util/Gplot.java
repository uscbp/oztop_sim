package sim.util;

import sim.graphics.Point3d;

import java.io.*;


/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class Gplot
{

    static int XGAP=5;
    static int YGAP=20;
    static int in_wi=255-XGAP;
    static int in_he=255-YGAP;
    static int in_xpos=0;
    static int in_ypos=0;

    static int xend=1280;
    static int yend=1024-60;

    static int wi=in_wi;
    static int he=in_he;
    static int xpos=in_xpos;
    static int ypos=in_xpos;

    static final String persist="-persist";
    static Runtime r=Runtime.getRuntime();
    static Process p=null;
    static DataOutputStream pout;
    static OutputStream rawpout;
    static BufferedReader pin;
    static String usercommand="";
    public static boolean spread_window;

    public static void plot(final String s)
    {
        construct(null, s);
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    /*
    Gplot(String s,String gifname)
    {
        if (gifname==null)
            construct(s);
        else
        {
            try
            {
                String cmd = "fakeout gnuplot "+gifname+/*persist+*///" -geometry "+wi+"x"+he+"+"+xpos+"+"+ypos;
/*                p=r.exec(cmd);
            }
            catch (Exception e)
            {
                System.err.println("Error running gnuplot");
            }
            pin =new DataInputStream(p.getInputStream());
            pout=new DataOutputStream(rawpout=p.getOutputStream());
            send("set terminal gif\n"+s);
            try
            {
                p.waitFor();
            }
            catch (Exception e)
            {
                System.err.println("Exception occured waiting for the external process!");
            }
        }
    }
  */

    public static void plot(final double[] v, String prefix, final String extracommand, final String outputFile)
    {
        plot(v,v.length,prefix,extracommand,outputFile);
    }

    public static void plot(final double[] v, final int N, String prefix, final String extracommand,
                            final String outputFile)
    {
        if (prefix ==null)
            prefix =Resource.getString("plotDataDir")+"/GP.";
        String name=prefix +System.currentTimeMillis();
        Elib.array2file(v,N,name);
        String s="";
        if(extracommand.length()>0)
            s += extracommand + ";";
        construct(s + "plot \""+name+"\" with linespoints lw 2\n",outputFile);
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    public static void plot(final Point3d[] v, final int N, final String extracommand, final String pngFile)
    {
        final String name=Resource.getString("plotDataDir")+"/GP."+System.currentTimeMillis();
        Elib.point3d2file(v, N, name, false);
        String s="";
        if(extracommand!=null && extracommand.length()>0)
            s += extracommand + ";";
        construct(s + "plot [-3:3] [-3:3] \""+name+
                //"\" with boxes");
                "\" with linespoints lw 2\n",pngFile);
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    public static void plot(final Point3d[][] v, final int N, final int M, final String extracommand, String[] titles,
                            final String pngFile)
    {
        final String suffix=Resource.getString("plotDataDir")+"/GP.";
        String s="";
        if(extracommand != null && extracommand.length()>0)
            s += usercommand+extracommand+"; ";

        s += "plot [-3:3] [-3:3] ";
        for(int i=0; i<M; i++)
        {
            final String name=suffix+System.currentTimeMillis()+i;
            Elib.point3d2file(v[i], N, name, false);
            s += "\""+name+"\" ";
            if(titles != null && titles.length>i)
                s += "title '"+titles[i]+"' ";
            s += "with linespoints linewidth 2";
            if(i!=M-1)  s += ",";
        }
        construct(s, pngFile);
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    public static void plot(final Point3d[][] v, final int[] N, final int M, final String extracommand, String[] titles,
                            final String pngFile)
    {
        final String suffix=Resource.getString("plotDataDir")+"/GP.";
        String s="";
        if(extracommand != null && extracommand.length()>0)
            s += usercommand+extracommand+"; ";

        s += "plot [-3:3] [-3:3] ";
        for(int i=0; i<M; i++)
        {
            final String name=suffix+System.currentTimeMillis()+i;
            Elib.point3d2file(v[i], N[i], name, false);
            s += "\""+name+"\" ";
            if(titles != null && titles.length>i)
                s += "title '"+titles[i]+"' ";
            s += "with linespoints linewidth 2";
            if(i!=M-1)  s += ",";
        }
        construct(s, pngFile);
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    public static void plot(final double [][] v, final String prefix, final String com, String pngFile)
    {
        plot(v,v.length,v[0].length,prefix,com,pngFile);
    }

    static void plot(double[][] v, int N,int M, String prefix, String extracommand, String pngFile)
    {
        if (prefix ==null)
            prefix =Resource.getString("plotDataDir")+"/GP.";
        String name=prefix +System.currentTimeMillis();
        double[][] Q=transpose(v);
        Elib.array2file(Q,M,N,name);
        //send("set zrange [0:0.025];set hidden3d; splot \""+dimensionName+
        //"\" matrix with lines");
        String s="";
        if(extracommand.length()>0)
            s += usercommand+extracommand+"; ";
        construct(s+"set hidden3d; splot \""+name+"\" matrix with lines\n", pngFile);
    }

    public static void plot(final double[][] v, final int N, final int M)
    {
        plot(v,N,M,null, 3, new String[M],null);
    }

    public static void plot(final Point3d[] v, String suffix, final String extracommand, String outputFile)
    {
        plot(v,suffix,extracommand,outputFile,true);
    }

    public static void plot(final Point3d[] v, String suffix, final String extracommand, String outputFile,
                            boolean lines)
    {
        if (suffix==null)
            suffix=Resource.getString("plotDataDir")+"/GP.";
        final String name=suffix+System.currentTimeMillis();
        Elib.point3d2file(v,v.length, name, !lines);
        //send("set zrange [0:0.025];set hidden3d; splot \""+name+
        //"\" matrix with lines");
        String s = usercommand+extracommand+"; set hidden3d; splot \""+name+"\"";
        if(lines)
            s+=" with lines";
        else
            s+=" with points";
        construct(s, outputFile);
    }

    /*
    public Gplot (final Point3d[] v, final String suffix)
    {
        this(v,suffix,"");
    }
    */

    public static void plot(final double[][] v, final int N, final int M, final String extracommand,
                            final int dimensions, String[] titles, final String pngFile)
    {
        String suffix=Resource.getString("plotDataDir")+"/GP.";

        String s="";
        if(extracommand != null && extracommand.length()>0)
            s += usercommand+extracommand+"; ";
        final double[][] Q=transpose(v);

        if(dimensions == 3)
        {
            final String name=suffix+System.currentTimeMillis();
            Elib.array2file(Q,M,N,name);
            s+="set hidden3d; splot \""+name+"\" matrix title '"+titles[0]+"' with lines\n";
            construct(s, pngFile);
        }
        else if(dimensions == 2)
        {
            s += "plot [0:"+(N+2)+"] [0.0:1.2] ";
            for(int i=0; i<M; i++)
            {
                final String name=suffix+System.currentTimeMillis()+i;
                Elib.array2file(Q[i], N, name);
                s += "\""+name+"\" ";
                if(titles != null && titles.length>i)
                    s += "title '"+titles[i]+"' ";
                s += "with linespoints linewidth 2";
                if(i!=M-1)  s += ",";
            }
            construct(s, pngFile);
        }
        if(System.getProperty("os.name").equals("Linux"))
            close();
    }

    /** (x,y) first util.Gplot top-left coord. (w,h) is the size of the plot and
     (xen,yen) defines the end of the gnuplot usuable area in the screen */
    /*
    static public void resetGeom(final int x, final int y, final int xen, final int yen, final int w, final int h)
    {
        in_wi=w;
        in_he=h;
        in_xpos=x;
        in_ypos=y;
        xend=xen;
        yend=yen;
        Gplot.resetGeom();
    }
    */

    static public void resetGeom(final int x, final int y, final int w, final int h)
    {
        in_wi=w;
        in_he=h;
        in_xpos=x;
        in_ypos=y;
        Gplot.resetGeom();
    }

    /*
    static public void resetGeom(final int x, final int y)
    {
        in_xpos=x;
        in_ypos=y;
    }
    */
    static public void resetGeom()
    {
        if (in_wi>640)
            in_wi=640;
        if (in_he>480)
            in_he=640;
        wi=in_wi;
        he=in_he;
        xpos=in_xpos;
        ypos=in_ypos;
    }

    static public void spreadWindow(final boolean k)
    {
        spread_window=k;
    }

    /*
    static public void spreadWindow(final int k)
    {
        spread_window = k==0?false:true;
    }
    */

    static public void setUserCommand(final String s)
    {
        usercommand=s;
    }

    public static void plot()
    {
        construct(null, null);
    }

    private static void construct(String outFile)
    {
        final String os = System.getProperty("os.name");
        String gplotcmd = Resource.getString("gnuplotExecutable");
        if(os.equals("Linux"))
        {
            if(outFile == null || outFile.length() == 0)
                gplotcmd += " " + persist+" -geometry "+wi+"x"+he+"+"+xpos+"+"+ypos;
            else
                gplotcmd += " -geometry "+wi+"x"+he+"+"+xpos+"+"+ypos;
        }
        else
        {
            if(outFile == null || outFile.length() == 0)
                gplotcmd += " " + persist;
        }
        try
        {
            Thread.sleep(500);
            p=r.exec(gplotcmd);
        }
        catch (Exception e)
        {
            Log.println("Error running gnuplot");
        }
        pin = new BufferedReader(new InputStreamReader(p.getInputStream()));
        pout=new DataOutputStream(rawpout=p.getOutputStream());
    }

    private static void construct(final String s, String outFile)
    {
        construct(outFile);
        if(outFile != null && outFile.length() > 0)
        {
            send("set terminal postscript eps color linewidth 2 solid");
            if(!outFile.endsWith(".eps"))
                outFile += ".eps";
            send("set output \""+outFile+"\"");
        }
        send(s);
        if(!System.getProperty("os.name").equals("Linux"))
            send("pause -1;");
    }
/*
    public void waitFinish()
    {
        try
        {
            p.waitFor();
        }
        catch (Exception e)
        {
            System.err.println("Error during waitFinsh ");
        }
    }
*/
    public static void send(String s)
    {
        try
        {
            if(s!=null)
            {
                if(s.charAt(s.length()-1)!='\n')
                    s += '\n';
                pout.writeBytes(s);
                pout.flush();
                rawpout.flush();
                //System.out.println("Hit any key to close util.Gplot");
                //System.in.read();
            }
        }
        catch (Exception e)
        {
            Log.println("Error during issuing command");
        }
    }
/*
    public void get()
    {
        //byte[] b=new byte[20];
        String s="fake";

        try
        {
            while (s!=null)
            {
                s=pin.readLine();
                if (s!=null) System.out.println(s);
            }
        }
        catch (Exception e)
        {
            System.err.println("Error during issuing command");
        }
    }
*/

    public static void close()
    {
        try
        {
            pin.close();
            pout.close();
        }
        catch (Exception e)
        {
            Log.println("Error during close command");
        }
    }

    /*
        public static void main(final String[] args)
        {
            final double[] x={1,2,3,4,5,6,7,8,9,8,7,6,5,4,3,2,1};
            //util.Gplot gp=new util.Gplot("plot x");
            new Gplot(x);
            //gp.startTimer(10);
        }
    */

    static double[][] transpose(final double[][] M)
    {
        final int m=M.length;
        final int n=M[0].length;
        final double [][] Q=new double[n][m];
        for (int i=0;i<m;i++)
            for (int j=0;j<n;j++)
                Q[j][i]=M[i][j];
        return Q;
    }
}

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


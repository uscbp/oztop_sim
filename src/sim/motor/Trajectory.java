package sim.motor;

//import sim.graphics.Point3d;
import sim.graphics.Segment;
import sim.util.Spline;
import sim.util.Elib;
import sim.util.Gplot;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class Trajectory
{
    //int N;
    //Spline[] teta;
    //Spline TS;
    Spline F;
    //Path X;
    //Point3d target;
    static public Segment[] usedseg;

    /*
    public Trajectory(Point3d p[])
    {
        this(p,p.length);
    }

    public Trajectory(Point3d p[],int n)
    {
        target=p[n-1].duplicate(); //check this
        N=n;
        X=new Path(p,n);
        for (double t=0;t<=1;t+=0.05) System.out.println(X.eval(t).str());
        TS=X.constStep(1000,0.001);

        //TS is a (time) spline such that d|X|/dTS(u) is constant over u in [0,1]
        // now find an f to substutide u (u=f(s)) such that d|X|/ds has a bell
        // profile.
        double[] s=new double[5],f=new double[5];

        s[0]=0;        f[0]=0;
        s[1]=0.25;     f[1]=0.15;
        s[2]=0.5 ;     f[2]=0.5;
        s[3]=0.75;     f[3]=0.9;
        s[4]=1;        f[4]=1;
        F=new Spline(5,s,f);
    }
    */

    public Trajectory()
    {
        double[] s=new double[5],f=new double[5];

        s[0]=0;        f[0]=0;
        s[1]=0.25;     f[1]=0.15;
        s[2]=0.5 ;     f[2]=0.5;
        s[3]=0.75;     f[3]=0.9;
        s[4]=1;        f[4]=1;
        F=new Spline(5,s,f);
    }

    // approx, bell shape profile
    public double stretchTime(double t)
    {
        return F.eval(t);
    }

    public static Spline[] jointSpline(Segment[] seg, double beta1[], double beta2[], int n)
    {
        int N;
        double[] x=new double[5],y=new double[5];
        Spline[] traj=new Spline[n];
        usedseg=seg;
        for (int k=0;k<n;k++)
        {
            if (seg[k].userTag==ArmHand.HANDJOINT)
            {
                N=0;
                x[N]=0;
                y[N++]=beta1[k];
                x[N]=0.7;
                y[N++]=beta2[k]-Math.PI/15; // was /10

                x[N]=1;
                y[N++]=beta2[k];
            }
            else
            {
                N=3;
                for (int r=0;r<N;r++)
                {
                    x[r]=r*(1.0/(N-1));
                    y[r]=beta1[k]+r*(beta2[k]-beta1[k])/(N-1);
                }
            }
            traj[k]=new Spline(N,x,y);
			traj[k].name=seg[k].label;
        }
        return traj;
    }

    //	This has multiple via points (ratio-way)
    static public Spline[] jointSpline(Segment[] seg, double beta1[], double viabeta[][], double beta2[], int n, double[] ratio)
    {
        int N;
        double[] x=new double[2+viabeta.length],y=new double[2+viabeta.length];
        Spline[] traj=new Spline[n];
        usedseg=seg;
        for (int k=0;k<n;k++)
        {
            if (seg[k].userTag==ArmHand.HANDJOINT) //if hand ignore the midbeta
            {
                N=0;
                x[N]=0;
                y[N++]=beta1[k];
                x[N]=0.7;
                y[N++]=beta2[k]-Math.PI/15; // was /10

                x[N]=1;
                y[N++]=beta2[k];
            }
            else
            {
                N=0;
                x[N]=0;
                y[N++]=beta1[k];
				for(int i=0; i<viabeta.length; i++)
				{
					x[N]=ratio[i];
					y[N++]=viabeta[i][k];
				}
                x[N]=1.0;
                y[N++]=beta2[k];
            }
            traj[k]=new Spline(N,x,y);
			traj[k].name=seg[k].label;
        }
        return traj;
    }
	
   public static void showSplines(Spline[] traj,int k)
    {
        int N=50;
        String base="/tmp/SP_";
        String command="plot [0:1] [-"+Math.PI+":"+Math.PI+"] ";
        for (int i=0;i<k;i++)
        {
			if(traj[i] != null && usedseg[i].userTag != ArmHand.HANDJOINT)
			{
			    double[][] A = traj[i].makeArray(0,1,N);
	            String fn=base+usedseg[i].label+System.currentTimeMillis();
	            Elib.array2file(A,N,2,fn);
	
	            command+="'"+fn+"' title '" + traj[i].name + "' with linespoints";
				command+=",";
			}
        }
		command = command.substring(0, command.length()-1);
		command+="\n";
        Gplot.plot(command);
		command="plot [0:1] [-"+Math.PI+":"+Math.PI+"] ";
        for (int i=0;i<k;i++)
        {
			if(traj[i] != null && usedseg[i].userTag == ArmHand.HANDJOINT)
			{
			    double[][] A = traj[i].makeArray(0,1,N);
	            String fn=base+usedseg[i].label+System.currentTimeMillis();
	            Elib.array2file(A,N,2,fn);
	
	            command+="'"+fn+"' title '" + traj[i].name + "' with linespoints";
				command+=",";
			}
        }
		command = command.substring(0, command.length()-1);
		command+="\n";
        Gplot.plot(command);
	}
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


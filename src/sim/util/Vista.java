package sim.util;

import sim.graphics.Point3d;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Dec 15, 2005
 * Time: 11:11:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class Vista
{
    private Runtime r = Runtime.getRuntime();
    private Process p = null;
    private DataOutputStream pout;
    private BufferedReader pin;

    private Point3d[] coords;

    public Vista(double[][] timeSeries, int duration, int dimensions, String dataName, String dataDesc,
                      String filename)
    {
        Vector coordVec=new Vector();
        MDS.createVistaFile(timeSeries, duration, dimensions, dataName, dataDesc, filename);

        try
        {
            p=r.exec(Resource.getString("vistaExecutable").replace('_',' '));
            pin=new BufferedReader(new InputStreamReader(p.getInputStream()));
            pout=new DataOutputStream(p.getOutputStream());
            waitAndEatOutput();
            sendOutput("(load-data \""+Resource.getString("homeDir")+"/"+filename+"\")\n");
            sendOutput("(multidimensional-scaling)\n");
            coordVec=readMDSCoords();
            sendOutput("(save-model \""+dataName+"\")\n");
            sendOutput("(save-data \""+dataName+"\")\n");
            quit();
        }
        catch (Exception e)
        {
            Log.println("Error running vista");
        }
        coords = new Point3d[coordVec.size()];
        for(int i=0; i<coordVec.size(); i++)
            coords[i]=(Point3d)coordVec.get(i);
    }

    private void quit() throws IOException
    {
        pout.writeBytes("(quit)\n");
        pout.flush();
        pout.writeBytes("(quit)\n");
        pout.flush();
    }

    private Vector readMDSCoords() throws IOException
    {
        Vector coordVec = new Vector();
        pout.writeBytes("(report-model)\n");
        pout.flush();
        while(!pin.ready())
        {
            try
            {
                Thread.sleep(100);
            }
            catch(Exception e)
            {}
        }
        boolean reading=false;
        String s="";
        while(!s.equals("T"))
        {
            s=pin.readLine();
            if(s.equals("Current Stimulus Coordinates:"))
               reading=true;
            else if(reading && !s.equals("T"))
            {
                StringTokenizer tokens = new StringTokenizer(s, " ");
                double x = Double.parseDouble(tokens.nextToken());
                double y = Double.parseDouble(tokens.nextToken());
                double z = Double.parseDouble(tokens.nextToken());
                coordVec.add(new Point3d(x,y,z));
            }
        }
        while(pin.ready())
        {
            pin.read();
        }
        return coordVec;
    }

    private void sendOutput(String output)
            throws IOException
    {
        pout.writeBytes(output);
        pout.flush();
        waitAndEatOutput();
    }

    private void waitAndEatOutput()
            throws IOException
    {
        while(!pin.ready())
        {
            try
            {
                Thread.sleep(100);
            }
            catch(Exception e)
            {}
        }
        while(pin.ready())
        {
            pin.read();
        }
    }

    public Point3d[] getCoords()
    {
        return coords;
    }
}

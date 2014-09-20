package sim.util;

import sim.util.Acme.JPM.Encoders.GifEncoder;
import sim.util.Acme.JPM.Encoders.PpmEncoder;
import sim.main.Main;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Aug 6, 2006
 * Time: 10:38:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class FrameUtils
{

    public static String getFrameString(int frameNumber)
    {
        StringBuilder frameString=new StringBuilder();
        if(frameNumber<10)
            frameString.append("00000");
        else if(frameNumber<100)
            frameString.append("0000");
        else if(frameNumber<1000)
            frameString.append("000");
        else if(frameNumber<10000)
            frameString.append("00");
        else if(frameNumber<100000)
            frameString.append("0");
        frameString.append(frameNumber);

        return frameString.toString();
    }

    public static void createGif(String fname)
    {
        try
        {
            final DataOutputStream d=Elib.openfileWRITE(fname);
            final GifEncoder g=new GifEncoder(Main.cv.lastFrame(),d);
            g.encode();
            Log.println(Main.self.setInfo("Wrote frame:"+fname));
        }
        catch(IOException e)
        {
            Log.println(Main.self.setInfo("Cannot create file:"+fname));
        }
    }

    public static void createGif()
    {
        StringBuilder fname=new StringBuilder(Main.recordImageBase);
        Main.recordFrame++;
        if(Main.recordPrefix.length()>0)
        {
            fname.append(Main.recordPrefix);
            fname.append('-');
        }
        fname.append(getFrameString(Main.recordFrame));
        fname.append(".gif");

        createGif(fname.toString());
    }

    public static void createPpm(String fname)
    {
        try
        {
            final DataOutputStream d=Elib.openfileWRITE(fname);
            final PpmEncoder g=new PpmEncoder(Main.cv.lastFrame(),d);
            g.encode();
            Log.println(Main.self.setInfo("Wrote frame:"+fname));
        }
        catch(IOException e)
        {
            Log.println(Main.self.setInfo("Cannot create file:"+fname));
        }
    }

    public static void createPpm()
    {
        StringBuilder fname=new StringBuilder(Main.recordImageBase);
        Main.recordFrame++;
        if(Main.recordPrefix.length()>0)
        {
            fname.append(Main.recordPrefix);
            fname.append('-');
        }
        fname.append(getFrameString(Main.recordFrame));
        fname.append(".ppm");

        createPpm(fname.toString());
    }
}

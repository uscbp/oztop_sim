package sim.util;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Mar 18, 2006
 * Time: 11:42:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Log
{
    private static String logFile=null;
    private static BufferedWriter out;

    public static void setLogFile(String logFileName)
    {
        logFile=logFileName;
        createWriter();
    }

    private static void createWriter()
    {
        if(out!=null)
        {
            try
            {
                out.flush();
                out.close();
            }
            catch(Exception e)
            {}
        }
        if(logFile!=null)
        {
            File log = new File(logFile);
            try
            {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log)));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
            out=null;
    }

    public static void dprint(String s, int level)
    {
        if (level<1)
            return;
        writeln(s);
    }

    public static void dprint(int level, int requiredLevel, String s)
    {
        if (level<requiredLevel)
            return;
        writeln(s);
    }

    public static void dprintln(String s, int level)
    {
        if (level<1)
            return;
        writeln(s);
    }

    public static void dprintln(int level, int requiredLevel, String s)
    {
        if (level<requiredLevel)
            return;
        writeln(s);
    }

    public static void println(String s)
    {
        writeln(s);
    }

    private static void writeln(String s)
    {
        try
        {
            if(out==null)
            {
                createWriter();
            }
            if(out!=null)
            {
                out.write(s+"\n");
                out.flush();
            }
            System.out.println(s);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

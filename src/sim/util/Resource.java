package sim.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

/*--------------- Resource --------------------*/
public class Resource 
{
    static private Vector vars;
    static public URL docbase=null;
    static public void read()
    {
        read("default.res");
    }

    static public DataInputStream openURLfile(final String name)
    {
        final InputStream is;
        DataInputStream dis=null;
        final BufferedInputStream bis;
        URL url=null;
        try
        {
            url=new URL(docbase,name);
        }
        catch (MalformedURLException e)
        {
            Log.println("Bad URL  address:"+url);
        }

        try
        {
            is = url.openStream();
            bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);
        }
        catch (IOException e)
        {
            Log.println("File open error:"+e.getMessage());
        }
        catch(NullPointerException e)
        {
            Log.println("File open error:"+e.getMessage());
        }
        return dis;
    }

    static public DataInputStream openFile(final String fn) throws IOException
    {
        if (docbase==null)
        {
            return new DataInputStream(new FileInputStream(fn));
        }
        return openURLfile(fn);
    }

    static public void read(final String fn)
    {
        int tc;
        final String[] t=new String[2] ;
        String s;
        vars=new Vector(40);
        try
        {
            final BufferedReader in = new BufferedReader(new InputStreamReader(openFile(fn)));
            while (null!=(s=in.readLine()))
            {
                if (s.equals(""))
                    continue;
                if (s.charAt(0) == '#')
                    continue;
                final StringTokenizer st= new StringTokenizer(s," ");
                tc=0;
                while (st.hasMoreTokens())
                {
                    if (tc>=2)
                        break;
                    t[tc++] = st.nextToken();
                }
                if (tc==2)
                {
                    vars.addElement(new ResourceVar(t[0],t[1]));
                }
            }
            in.close();
        }
        catch (IOException e)
        {
        }
    }

    static public String getString(final String s)
    {
        ResourceVar r;
        final Enumeration e=vars.elements();
        while ( e.hasMoreElements())
        {
            r=(ResourceVar)e.nextElement();
            if (s.equals(r.name))
                return r.value;
        }
        return null;
    }

    static public int getInt(final String s)
    {
        ResourceVar r;
        final Enumeration e=vars.elements();
        while ( e.hasMoreElements())
        {
            r=(ResourceVar)e.nextElement();
            if (s.equals(r.name))
                return toInt(r.value);
        }
        return 0;
    }

    static public double get(final String s)
    {
        return getDouble(s);
    }

    static public double getDouble(final String s)
    {
        ResourceVar r;
        for (Enumeration e=vars.elements(); e.hasMoreElements(); )
        {
            r=(ResourceVar)e.nextElement();
            if (s.equals(r.name))
                return toDouble(r.value);
        }
        return 0.0;
    }

    static public double toDouble(final String s)
    {
        char c;
        String t="";
        for (int i=0;i<s.length();i++)
        {
            c=s.charAt(i);
            if (c>' ')
                t+=c;
        }
        return (Double.valueOf(t)).doubleValue();
    }

    static  public int toInt(final String s)
    {
        return (int)(toDouble(s)+0.5);
    }
}

class ResourceVar
{
    public String name,value;
    public ResourceVar(final String nm, final String v)
    {
        name=nm;
        value=v;
    }
}

/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


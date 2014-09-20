package sim.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Dec 11, 2005
 * Time: 8:02:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class MDS
{
    public static void createGgobiFile(double[][] timeSeries, int duration, int dimensions, String dataName, String dataDesc,
                                       String filename)
    {
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write("<?xml version=\"1.0\"?>\n");
            out.write("<!DOCTYPE ggobidata SYSTEM \"ggobi.dtd\">\n");
            out.write("<ggobidata count=\"2\">\n");
            out.write("<data name=\""+dataName+" nodes\">\n");
            out.write("<description>\n");
            out.write(dataDesc);
            out.write("</description>\n");
            out.write("<variables count=\""+dimensions+"\">\n");
            for(int i=0; i<dimensions; i++)
                out.write("<variable name=\"unit"+i+"\" />\n");
            out.write("</variables>\n");
            out.write("<records count=\""+duration+"\" color=\"2\" glyph=\"plus 1\">\n");
            for(int i=0; i<duration; i++)
            {
                out.write("<record id=\""+dataName+"_"+i+"\">");
                for(int j=0; j<dimensions; j++)
                    out.write(" "+timeSeries[i][j]);
                out.write(" </record>\n");
            }
            out.write("</records>\n");
            out.write("</data>\n");
            out.write("<data name=\"edges\">");
            out.write("<records count=\""+(duration-1)+"\" glyph=\"fr 0\">");
            for(int i=0; i<duration-1; i++)
                out.write("<record source=\""+dataName+"_"+i+"\" destination=\""+dataName+"_"+(i+1)+"\" color=\"2\"> </record>\n");
            out.write("</records>\n");
            out.write("</data>\n");
            out.write("</ggobidata>\n");
            out.close();
        }
        catch(IOException e)
        {
            Log.println("Error creating file "+filename);
            e.printStackTrace();
        }
    }

    public static void createVistaFile(double[][] timeSeries, int duration, int dimensions, String dataName, String dataDesc,
                                       String filename)
    {
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write("(data \"");
            out.write(dataName);
            out.write("\"\n");
            out.write(":about \"");
            out.write(dataDesc);
            out.write("\"\n");
            out.write(":title \"");
            out.write(dataName);
            out.write("\"\n");
            out.write(":variables '(");
            for(int i=0; i<duration; i++)
            {
                out.write("\"Time Step "+i+"\" ");
            }
            out.write(")\n");
            out.write(":shapes     '(\"symmetric\")\n");
            out.write(":matrices   '(\"");
            out.write(dataName);
            out.write("\")\n");
            out.write(":data       '(\n");
            double[][] dissSimMatrix = calculateDissimilarityMatrix(timeSeries, duration);
            for(int i=0; i<duration; i++)
            {
                for(int j=0; j<duration; j++)
                {
                    out.write(dissSimMatrix[i][j]+" ");
                }
                out.write("\n");
            }
            out.write("))\n");
            out.close();
        }
        catch(IOException e)
        {
            Log.println("Error creating file "+filename);
            e.printStackTrace();
        }
    }
    protected static double[][] calculateDissimilarityMatrix(double[][] timeSeries, int duration)
    {
        double[][] matrix = new double[duration][duration];
        for(int i=0; i<duration; i++)
        {
            for(int j=0; j<duration; j++)
            {
                if(i==j)
                    matrix[i][j]=0;
                else
                    matrix[i][j] = VA.dist(timeSeries[i],timeSeries[j]);
            }
        }
        return matrix;
    }
}

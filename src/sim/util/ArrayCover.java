package sim.util;

public class ArrayCover 
{
	public double[] val;

    public ArrayCover(final double[] a)
    {
        val=new double[a.length] ;
        System.arraycopy(a,0,val,0,a.length);
    }

    public ArrayCover(final int size)
    {
        val=new double[size] ;
    }
}

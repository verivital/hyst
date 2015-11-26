package com.verivital.hyst.simulation;
import java.text.NumberFormat;


public class HyperPoint implements Comparable <HyperPoint>
{
	public static boolean prettyPrint = false;
	
	public double[] dims;
	
	/**
	 * Create a new hyperpoint with the given number of dimensions
	 * @param numDims the number of dimensions
	 */
	public HyperPoint(int numDims)
	{
		dims = new double[numDims];
	}
	
	public HyperPoint(HyperPoint copy)
	{
		dims = new double[copy.dims.length];
		
		for (int d = 0; d < dims.length; ++d)
			dims[d] = copy.dims[d];
	}
	
	public HyperPoint(double ... ds)
	{
		dims = new double[ds.length];
		
		for (int d = 0; d < ds.length; ++d)
			dims[d] = ds[d];
	}

	@Override
	public String toString()
	{
		String s =  "[HyperPoint (";
		boolean isFirst = true;
		
		NumberFormat nf = NumberFormat.getInstance();
		
		for (int x = 0; x < dims.length; ++x)
		{
			// print comma
			if (!isFirst)
				s += ", ";
			
			isFirst = false;
			
			// print dimension value
			if (prettyPrint)
				s += nf.format(dims[x]);
			else
				s += dims[x];
		}
		
		s += ")]";
		
		return s;
	}

	@Override
	public int compareTo(HyperPoint right)
	{
		boolean isLess = false;
		boolean isMore = false;
		
		for (int d = 0; d < dims.length; ++d)
		{
			double l = dims[d];
			double r = right.dims[d];
			
			if (Double.isInfinite(r) && Double.isInfinite(l))
				continue;
			
			if (Double.isNaN(r) && Double.isNaN(l))
				continue;
			
			if (dims[d] < right.dims[d] || Double.isInfinite(r) || Double.isNaN(l))
			{
				isLess = true;
				break;
			}
			
			if (dims[d] > right.dims[d] || Double.isInfinite(l) || Double.isNaN(r))
			{
				isMore = true;
				break;
			}
		}
		
		return isLess ? -1 : (isMore ? 1 : 0);
	}
}

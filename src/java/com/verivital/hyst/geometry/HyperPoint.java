package com.verivital.hyst.geometry;

import java.util.ArrayList;

import com.verivital.hyst.util.StringOperations;

public class HyperPoint implements Comparable<HyperPoint>
{
	public static boolean prettyPrint = false;

	public double[] dims;

	/**
	 * Create a new hyperpoint with the given number of dimensions
	 * 
	 * @param numDims
	 *            the number of dimensions
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

	public HyperPoint(double... ds)
	{
		dims = new double[ds.length];

		for (int d = 0; d < ds.length; ++d)
			dims[d] = ds[d];
	}

	@Override
	public String toString()
	{
		return "(" + StringOperations.join(", ", dims) + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		boolean rv = false;

		if (other instanceof HyperPoint)
			rv = compareTo((HyperPoint) other) == 0;

		return rv;
	}

	@Override
	public int hashCode()
	{
		ArrayList<Double> vals = new ArrayList<Double>(dims.length);

		for (double d : dims)
			vals.add(d);

		return vals.hashCode();
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

	public HyperRectangle toHyperRectangle()
	{
		HyperRectangle rv = new HyperRectangle(dims.length);

		for (int d = 0; d < dims.length; ++d)
			rv.dims[d] = new Interval(dims[d]);

		return rv;
	}

	public HyperPoint copy()
	{
		return new HyperPoint(this);
	}

	public static HyperPoint add(HyperPoint a, HyperPoint b)
	{
		HyperPoint rv = new HyperPoint(a.dims.length);

		for (int d = 0; d < b.dims.length; ++d)
			rv.dims[d] = a.dims[d] + b.dims[d];

		return rv;
	}

	public static HyperPoint subtract(HyperPoint a, HyperPoint b)
	{
		HyperPoint rv = new HyperPoint(a.dims.length);

		for (int d = 0; d < b.dims.length; ++d)
			rv.dims[d] = a.dims[d] - b.dims[d];

		return rv;
	}

	public static HyperPoint multiply(HyperPoint a, double val)
	{
		HyperPoint rv = new HyperPoint(a.dims.length);

		for (int d = 0; d < a.dims.length; ++d)
			rv.dims[d] = a.dims[d] * val;

		return rv;
	}
}

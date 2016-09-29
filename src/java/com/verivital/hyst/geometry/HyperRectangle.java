package com.verivital.hyst.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A Hyperrectangle is an n-dimensional rectangle representing a portion of the state space
 * 
 * @author Stanley Bak (sbak2@illinois.edu) 6-2011
 */

public class HyperRectangle implements Comparable<HyperRectangle>
{
	private static ArrayList<String> dimensionNames = new ArrayList<String>();
	public Interval[] dims;
	public static final double TOL = 0.00000001;

	static
	{
		dimensionNames.add("X");
		dimensionNames.add("Y");
		dimensionNames.add("Z");
	}

	public static void setDimensionNames(List<String> names)
	{
		dimensionNames.clear();
		dimensionNames.addAll(names);
	}

	/**
	 * Create a new Hyperrectangle
	 * 
	 * @param numDims
	 *            the number of dimensions
	 */
	public HyperRectangle(int numDims)
	{
		dims = new Interval[numDims];
	}

	/**
	 * Create a new point HyperRectangle
	 * 
	 * @param p
	 *            the point to use
	 */
	public HyperRectangle(HyperPoint p)
	{
		dims = new Interval[p.dims.length];

		for (int d = 0; d < p.dims.length; ++d)
			dims[d] = new Interval(p.dims[d]);
	}

	/**
	 * Deep copy a hyperrectangle
	 * 
	 * @param other
	 *            the hr to copy
	 */
	public HyperRectangle(HyperRectangle other)
	{
		int len = other.dims.length;

		dims = new Interval[len];

		// for every dimension d
		for (int d = 0; d < len; ++d)
		{
			Interval fromInterval = other.dims[d];
			dims[d] = new Interval(fromInterval);
		}
	}

	public HyperRectangle(double[][] values)
	{
		int len = values.length;

		dims = new Interval[len];

		// for every dimension d
		for (int d = 0; d < len; ++d)
		{
			dims[d] = new Interval(values[d][0], values[d][1]);
		}
	}

	public HyperRectangle(Interval... intervals)
	{
		int len = intervals.length;

		dims = new Interval[len];

		// for every dimension d
		for (int d = 0; d < len; ++d)
		{
			dims[d] = intervals[d].copy();
		}
	}

	/**
	 * Creates a hyperrectangle with one additional dimension using a shallow copy of the passed-in
	 * one
	 * 
	 * @param hr
	 *            the orignal hyperrectangle
	 * @param time
	 *            the constant value for the last dimension (usually the time in an Hrt format
	 *            hyperrectangle)
	 */
	public HyperRectangle(HyperRectangle hr, double time)
	{
		this(hr.dims.length + 1);

		for (int d = 0; d < hr.dims.length; ++d)
			dims[d] = hr.dims[d]; // shallow copy

		dims[hr.dims.length] = new Interval(time, time);
	}

	public boolean contains(HyperPoint p)
	{
		boolean rv = true;

		for (int d = 0; d < dims.length; ++d)
		{
			if (p.dims[d] < dims[d].min || p.dims[d] > dims[d].max)
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	public boolean contains(HyperRectangle p)
	{
		boolean rv = true;

		for (int d = 0; d < dims.length; ++d)
		{
			if (p.dims[d].min < dims[d].min || p.dims[d].max > dims[d].max)
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	@Override
	public int compareTo(HyperRectangle rhs)
	{
		boolean isLess = false;
		boolean isMore = false;

		for (int d = 0; d < dims.length; ++d)
		{
			Interval l = dims[d];
			Interval r = rhs.dims[d];

			// compare mins
			if (l.min + TOL < r.min)
			{
				isLess = true;
				break;
			}

			if (r.min + TOL < l.min)
			{
				isMore = true;
				break;
			}

			// compare maxes
			if (l.max + TOL < r.max)
			{
				isLess = true;
				break;
			}

			if (r.max + TOL < l.max)
			{
				isMore = true;
				break;
			}
		}

		return isLess ? -1 : (isMore ? 1 : 0);
	}

	@Override
	public boolean equals(Object r)
	{
		boolean rv = false;

		if (r != null && r instanceof HyperRectangle)
		{
			rv = true;

			rv = compareTo((HyperRectangle) r) == 0;
		}

		return rv;
	}

	/**
	 * Does this hyperrectangle intersect with another?
	 * 
	 * @param hr
	 *            the other rectangle
	 * @return true iff the intersection is not empty
	 */
	public boolean intersects(HyperRectangle hr)
	{
		boolean rv = true;

		for (int d = 0; d < dims.length; ++d)
		{
			if (hr.dims[d].max < dims[d].min || hr.dims[d].min > dims[d].max)
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	/**
	 * Is this HyperRectangle a point?
	 * 
	 * @return true iff it is a point (max == min for all dimensions)
	 */
	public boolean isPoint()
	{
		boolean rv = true;

		for (int d = 0; d < dims.length; ++d)
		{
			if (dims[d].max != dims[d].min)
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	/**
	 * Convert this hyper rectangle to a hyper point. An exception is thrown if it is not a point
	 * (if min != max for all dimensions)
	 * 
	 * @return a HyperPoint object representing this HyperRectangle
	 */
	public HyperPoint toHyperPoint()
	{
		HyperPoint p = new HyperPoint(dims.length);

		for (int d = 0; d < dims.length; ++d)
		{
			if (dims[d].max != dims[d].min)
				throw new RuntimeException("toHyperPoint called on a non-point rectangle");

			p.dims[d] = dims[d].min;
		}

		return p;
	}

	/**
	 * Get the center of this hyperrectangle
	 * 
	 * @return the center HyperPoint
	 */
	public HyperPoint center()
	{
		HyperPoint p = new HyperPoint(dims.length);

		for (int d = 0; d < dims.length; ++d)
			p.dims[d] = dims[d].middle();

		return p;
	}

	/**
	 * bloat a hyperrectangle by some factor
	 * 
	 * @param factor
	 *            the factor to bloat by. 1.0 means will return the same rectangle
	 */
	public static HyperRectangle bloatMultiplicative(HyperRectangle rect, double factor)
	{
		HyperRectangle rv = new HyperRectangle(rect.dims.length);

		for (int d = 0; d < rect.dims.length; ++d)
		{
			double halfW = rect.dims[d].width() / 2.0;
			double mid = rect.dims[d].middle();

			double min = mid - (halfW * factor);
			double max = mid + (halfW * factor);

			rv.dims[d] = new Interval(min, max);
		}

		return rv;
	}

	/**
	 * bloat this hyperrectangle by some additive amount
	 * 
	 * @param factor
	 *            the factor to bloat by. 0.0 means will return the same rectangle
	 */
	public static HyperRectangle bloatAdditive(HyperRectangle rect, double amount)
	{
		HyperRectangle rv = new HyperRectangle(rect.dims.length);

		for (int d = 0; d < rect.dims.length; ++d)
		{
			double min = rect.dims[d].min - amount;
			double max = rect.dims[d].max + amount;

			rv.dims[d] = new Interval(min, max);
		}

		return rv;
	}

	/**
	 * Compute the convex hull of two hyperrectangles
	 * 
	 * @param a
	 *            one of the rectangles
	 * @param b
	 *            another rectangle
	 * @return the convex hull of the two passed-in rectangles
	 */
	public static HyperRectangle convexHull(HyperRectangle a, HyperRectangle b)
	{
		HyperRectangle rv = new HyperRectangle(a.dims.length);

		for (int d = 0; d < a.dims.length; ++d)
		{
			double min = Math.min(a.dims[d].min, b.dims[d].min);
			double max = Math.max(a.dims[d].max, b.dims[d].max);

			rv.dims[d] = new Interval(min, max);
		}

		return rv;
	}

	/**
	 * Assign the values in this rectangles dims array to the values in the passed in rect
	 * 
	 * @param rect
	 *            the values to copy
	 */
	public void assign(HyperRectangle rect)
	{
		for (int d = 0; d < rect.dims.length; ++d)
		{
			dims[d].min = rect.dims[d].min;
			dims[d].max = rect.dims[d].max;
		}
	}

	/**
	 * Trim a rectangle to be restricted to another rectangle
	 * 
	 * @param trimmingRect
	 *            the rect to trim to
	 * @throws RuntimeException
	 *             if the rectangles are nonoverlapping
	 */
	public void trim(HyperRectangle trimmingRect)
	{
		try
		{
			for (int d = 0; d < dims.length; ++d)
				dims[d].trim(trimmingRect.dims[d]);
		}
		catch (RuntimeException e)
		{
			throw new RuntimeException("Trim called with nonoverlapping rect: " + this
					+ " trimmed to " + trimmingRect + ": " + e);
		}
	}

	/**
	 * Make sure this is a valid hyperrectangle. Throw runtimeException if ranges are invalid
	 */
	public void validate()
	{
		try
		{
			for (int d = 0; d < dims.length; ++d)
				dims[d].validate();
		}
		catch (RuntimeException e)
		{
			throw new RuntimeException("Invalid HyperRectangle: " + this + ": " + e);
		}
	}

	/**
	 * Enumerate all the corners of the hyperrectangle
	 * 
	 * @param e
	 *            for each corner, e.enumerate gets called
	 * @param param
	 *            the param to pass into e.enumerate, can be null
	 */
	public void enumerateCorners(HyperRectangleCornerEnumerator e)
	{
		/*
		 * We do this by considering an integer as a bit array of true/false values, where 0=false
		 * means min, and 1=true means max. The length of the bit array is the number of dimensions.
		 * If we iterate from 0 to 2^dim, we will have iterated all the possibilities.
		 */

		// first construct the maximum (2^dim)
		int maxIterator = 1;

		for (int dimIndex = 0; dimIndex < dims.length; ++dimIndex)
			maxIterator *= 2;

		// next iterate from 0 to maxIterator (try each bit-array combination)
		HyperPoint point = new HyperPoint(dims.length);
		boolean isMin[] = new boolean[dims.length];

		for (int iterator = 0; iterator < maxIterator; ++iterator)
		{
			// extract each dimension's boolean true/false values from iterator
			int mask = 0x01;
			for (int dimIndex = 0; dimIndex < dims.length; ++dimIndex)
			{
				isMin[dimIndex] = (iterator & mask) == 0;
				mask = mask << 1;

				// assign the current dimension of the point point
				point.dims[dimIndex] = isMin[dimIndex] ? dims[dimIndex].min : dims[dimIndex].max;
			}

			// enumerate!
			e.enumerateWithCoord(point, isMin);
		}
	}

	/**
	 * Enumerate all the corners of the hyperrectangle that are unique
	 * 
	 * @param e
	 *            for each corner, e.enumerate gets called
	 * @param param
	 *            the param to pass into e.enumerate, can be null
	 */
	public void enumerateCornersUnique(HyperRectangleCornerEnumerator e)
	{
		/*
		 * We do this by considering an integer as a bit array of true/false values, where 0=false
		 * means min, and 1=true means max. The length of the bit array is the number of dimensions.
		 * If we iterate from 0 to 2^dim, we will have iterated all the possibilities.
		 */

		// first construct the maximum (2^dim)
		int maxIterator = 1;
		ArrayList<Integer> dimensionIndex = new ArrayList<Integer>(dims.length);

		for (int dimIndex = 0; dimIndex < dims.length; ++dimIndex)
		{
			if (!dims[dimIndex].isPoint())
			{
				maxIterator *= 2;
				dimensionIndex.add(dimIndex);
			}
		}

		for (int iterator = 0; iterator < maxIterator; ++iterator)
		{
			// extract each dimension's boolean true/false values from iterator
			HyperPoint point = center();
			boolean isMin[] = new boolean[dims.length];

			// assign all the dimensions that are not flat
			int mask = 0x01;
			for (int d = 0; d < dimensionIndex.size(); ++d)
			{
				int actualDim = dimensionIndex.get(d);

				isMin[actualDim] = (iterator & mask) == 0;
				mask = mask << 1;

				// assign the current dimension of the point point
				point.dims[actualDim] = isMin[actualDim] ? dims[actualDim].min
						: dims[actualDim].max;
			}

			// enumerate!
			e.enumerateWithCoord(point, isMin);
		}
	}

	/**
	 * Compute the intersection of type hyperrectangles, returns null if empty
	 * 
	 * @param r
	 *            the rectangle we're interesecting with
	 * @return the intersection hyperrectangle, or null if intersection is empty
	 */
	public static HyperRectangle intersection(HyperRectangle a, HyperRectangle b)
	{
		if (a.dims.length != b.dims.length)
			throw new RuntimeException(
					"HyperRectange intersection requires same number of dimensions");

		HyperRectangle rv = new HyperRectangle(a.dims.length);

		for (int d = 0; d < a.dims.length; ++d)
		{
			Interval intersection = Interval.intersection(a.dims[d], b.dims[d]);

			if (intersection == null)
			{
				rv = null;
				break;
			}

			rv.dims[d] = intersection;
		}

		return rv;
	}

	/**
	 * Compute the bounding box of the union of the HyperRectangles, and return it
	 * 
	 * @param r
	 *            the other box
	 * @return the tightest bounding box which contains both input boxes
	 */
	public static HyperRectangle union(HyperRectangle a, HyperRectangle b)
	{
		if (a.dims.length != b.dims.length)
			throw new RuntimeException("HyperRectange union requires same number of dimensions");

		HyperRectangle rv = new HyperRectangle(a.dims.length);

		for (int d = 0; d < a.dims.length; ++d)
			rv.dims[d] = Interval.union(a.dims[d], b.dims[d]);

		return rv;
	}

	public String toString()
	{
		String s = "{HyperRectangle: ";

		for (int x = 0; x < dims.length; ++x)
		{
			Interval i = dims[x];
			String name = "Dim #" + x;

			if (x < dimensionNames.size())
				name = dimensionNames.get(x);

			s += name + " = [" + i.min + ", " + i.max + "]";

			if (x + 1 < dims.length)
				s += "; ";
		}

		s += "}";

		return s;
	}

	public HyperRectangle copy()
	{
		return new HyperRectangle(this);
	}

	/**
	 * Get the unique star points from this HyperRectangle. Star points are ones where, from the
	 * center point, a single coordinate is modified to be at a face. There are at most 2*d star
	 * points, where d is the number of dimensions
	 * 
	 * @return the collection of HyperPoints
	 */
	public Collection<HyperPoint> getStarPoints()
	{
		LinkedHashSet<HyperPoint> rv = new LinkedHashSet<HyperPoint>();
		HyperPoint center = center();

		for (int d = 0; d < dims.length; ++d)
		{
			Interval range = dims[d];
			HyperPoint left = new HyperPoint(center);
			HyperPoint right = new HyperPoint(center);

			left.dims[d] = range.min;
			right.dims[d] = range.max;

			rv.add(left);
			rv.add(right);
		}

		return rv;
	}
}

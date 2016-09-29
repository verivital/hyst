package com.verivital.hyst.geometry;

/**
 * This is a class used for enumerating the corners of a hyperrectangle. you can extend it and
 * override either enumerate or enumerateWithCoord
 * 
 * @author sbak
 *
 */
public class HyperRectangleCornerEnumerator
{
	protected void enumerate(HyperPoint p)
	{
	}

	/**
	 * The corner enumeration function to be called.
	 * 
	 * @param p
	 *            the corner point
	 * @param isMin
	 *            for each dimension, is p the minimum values for that dimension?
	 */
	public void enumerateWithCoord(HyperPoint p, boolean[] isMin)
	{
		enumerate(p);
	}
}

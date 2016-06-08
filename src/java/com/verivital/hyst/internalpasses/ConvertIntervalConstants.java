package com.verivital.hyst.internalpasses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.ir.Configuration;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object. By
 * convention, call the static run() method to perform the transformation.
 * 
 * @author Stanley Bak
 */
public class ConvertIntervalConstants
{
	/**
	 * Converts constants that are intervals into variables with derivative zero and an initial
	 * value of the interval.
	 */
	public static void run(Configuration config)
	{
		Collection<String> toConvert = new ArrayList<String>();

		for (Entry<String, Interval> e : config.root.getAllConstants().entrySet())
		{
			if (!e.getValue().isConstant())
				toConvert.add(e.getKey());
		}

		for (String v : toConvert)
			ConvertConstToVariable.run(config, v);
	}
}

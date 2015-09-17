package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;

/**
 * Converts constants that are intervals into variables with derivative zero and an initial value of the interval
 */
public class ConvertIntervalConstantsPass extends TransformationPass
{
	public ConvertIntervalConstantsPass()
	{
		preconditions = new Preconditions(true); // no preconditions
	}
	
	@Override
	protected void runPass(String params)
	{
		Collection <String> toConvert = new ArrayList<String>();
		
		for (Entry<String, Interval> e : config.root.getAllConstants().entrySet())
		{
			if (!e.getValue().isConstant())
				toConvert.add(e.getKey());
		}
		
		for (String v : toConvert)
			new ConvertConstToVariablePass().runVanillaPass(config, v);
	}
}

package com.verivital.hyst.passes.basic;


import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.TransformationPass;


/**
 * A model transformation pass which re-scales time
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class TimeScalePass extends TransformationPass
{
	@Override
	public String getName()
	{
		return "Scale Time Pass";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pass_scale_time";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[multiplier;ignorevar]";
	}

	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		String[] parts = params.split(";");
		String ignoreVars = null;
		
		if (parts.length == 1)
			ignoreVars = null;
		else if (parts.length == 2)
			ignoreVars = parts[1];
		else 
			throw new AutomatonExportException("Expecting two params: scale and variable to ignore when rescaling");
		
		// multiply all derivatives by the scale, then divide the reachtime by the scale
		double scale = 1;
		
		try
		{
			scale = Double.parseDouble(parts[0]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing rescale factor: " + e);
		}
		
		if (scale <= 0)
			throw new AutomatonExportException("Rescale factor must be positive: " + scale);
		
		for (AutomatonMode am : ha.modes.values())
			am.flowDynamics = rescaleFlow(scale, am.flowDynamics, ignoreVars);
		
		config.settings.spaceExConfig.timeHorizon /= scale;
		config.settings.spaceExConfig.samplingTime /= scale;
	}

	/**
	 * Rescale the flow
	 * @param scale the scale the use
	 * @param flows the mode.flowDynamics to rescale
	 * @param ignoreVar the variables to not rescale (to ignore)
	 */
	private LinkedHashMap <String, ExpressionInterval> rescaleFlow(double scale, LinkedHashMap <String, ExpressionInterval> flows, String ignoreVar)
	{
		LinkedHashMap <String, ExpressionInterval> rv = new LinkedHashMap <String, ExpressionInterval>(); 
		
		for (Entry<String, ExpressionInterval> e : flows.entrySet())
		{
			String var = e.getKey();
			
			if (var.equals(ignoreVar))
				rv.put(var, e.getValue());
			else
			{
				ExpressionInterval ei = e.getValue().copy();
				Operation o = new Operation(ei.getExpression(), Operator.MULTIPLY, new Constant(scale));
				ei.setExpression(o);
				
				rv.put(var, ei);
			}
		}
		
		return rv;
	}
	
	

}

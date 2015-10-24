package com.verivital.hyst.passes.complex;


import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;


/**
 * Perform order reduction
 * 
 * @author Taylor Johnson (October 2015)
 *
 */
public class OrderReductionPass extends TransformationPass
{
	private int reducedOrder;
	
	@Override
	public String getName()
	{
		return "Order Reduction (decrease dimensionality) Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[<reduced order dimensionality>]";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-order_reduction";
	}

	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		
		processParams(ha, params);
		
		// todo: determine list of variables removed, then call the following to drop them
		//removeVariables(ha, varName);
		

	}
	
	private void removeVariable(BaseComponent ha, String varName)
	{
		if (!ha.variables.contains(varName)) {
			throw new AutomatonExportException("Variable " + varName + " does not exist in automaton.");
		}
		
		ha.variables.remove(varName);
		
		// remove flows
		for (AutomatonMode am : ha.modes.values()) {
			am.flowDynamics.remove(varName);
		}
		
		// todo: remove variables from other places (init, resets, etc.)
	}


	private void processParams(BaseComponent ha, String params)
	{
		if (params.trim().length() == 0) {
			params = Integer.toString(ha.variables.size()); // use original automaton dimensionality n
			// TODO: use n - 1, assuming n >= 2?
		}
		
		String[] parts = params.split(";");
		
		if (parts.length != 1)
			throw new AutomatonExportException("Expected 1 pass params separated by ';', instead received: " + params);
		
		try
		{
			reducedOrder = Integer.parseInt(parts[0]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing number: " + e, e);
		}
		
		Hyst.log("Using order reduction params reducedOrder = " + reducedOrder);
	}

}

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
 * Perform regularization on the hybrid automaton (may change semantics) in order to attempt to 
 * get rid of Zeno behavior.
 * 
 * In particular, the following is performed:
 * - A maximum number of jumps (num jumps) is enforced per time interval (delta)
 * 
 * params are <num jumps>;<delta>
 * 
 * where <num jumps> is the maximum number of jumps inside urgent zeno loops, and <delta> is 
 * the minimum dwell time
 * 
 * @author Stanley Bak (March 2015)
 *
 */
public class RegularizePass extends TransformationPass
{
	static final int DEFAULT_JUMPS = 5;
	static final double DEFAULT_DELTA = 30;
	
	static final String VAR_TRANSITION_COUNTER = "_transition_counter";
	static final String VAR_TRANSITION_CLOCK = "_transition_clock";
	
	int numJumps = -1;
	double delta = -1;
	
	@Override
	public String getName()
	{
		return "Regularization (eliminate zeno behaviors) Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[<num jumps>;<delta>]";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-regularize";
	}

	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		
		processParams(params);
		addVariable(ha, VAR_TRANSITION_CLOCK, new Constant(1));
		addVariable(ha, VAR_TRANSITION_COUNTER, new Constant(0));
		
		addTransitionLimit(ha);
		
		addTransitionClock(ha);
	}
	
	private void addVariable(BaseComponent ha, String varName, Expression defaultDer)
	{
		if (ha.variables.contains(varName))
			throw new AutomatonExportException("Variable " + varName + " already exists in automaton.");
		
		ha.variables.add(varName);
		addZeroInit(ha, varName);
		
		// add default flows
		for (AutomatonMode am : ha.modes.values())
			am.flowDynamics.put(varName, new ExpressionInterval(defaultDer.copy()));
	}

	private void addTransitionClock(BaseComponent ha)
	{
		// adjust transitions
		Expression dwellInvCondition = new Operation(Operator.LESSEQUAL, new Variable(VAR_TRANSITION_CLOCK), new Constant(delta));
		Expression dwellResetCondition = new Operation(Operator.GREATEREQUAL, new Variable(VAR_TRANSITION_CLOCK), new Constant(delta));
		
		for (AutomatonMode am : ha.modes.values())
		{
			// for non-urgent modes
			if (!am.urgent)
			{
				if (am.invariant == null)
					am.invariant = dwellInvCondition.copy();
				else
					am.invariant = new Operation(Operator.AND, am.invariant, dwellInvCondition.copy());
				
				// add transition
				AutomatonTransition at = ha.createTransition(am, am);
				at.guard = dwellResetCondition.copy();
				at.reset.put(VAR_TRANSITION_CLOCK, new ExpressionInterval(new Constant(0)));
				at.reset.put(VAR_TRANSITION_COUNTER, new ExpressionInterval(new Constant(0)));
			}
		}
	}

	private void addTransitionLimit(BaseComponent ha)
	{
		// each mode needs to enforce the limit in its invariant
		Expression jumpCondition = new Operation(Operator.LESSEQUAL, new Variable(VAR_TRANSITION_COUNTER), new Constant(numJumps));
		
		for (AutomatonMode am : ha.modes.values())
		{
			am.invariant = new Operation(Operator.AND, am.invariant, jumpCondition.copy());
		}
		
		// each transition adjusts urgent_counter
		for (AutomatonTransition at : ha.transitions)
		{
			Expression e = new Operation(Operator.ADD, new Variable(VAR_TRANSITION_COUNTER), new Constant(1));
			
			at.reset.put(VAR_TRANSITION_COUNTER, new ExpressionInterval(e));
		}
	}

	private void addZeroInit(BaseComponent ha, String varName)
	{
		LinkedHashMap <String, Expression> newInit = new LinkedHashMap <String, Expression>();
		
		for (Entry<String, Expression> e : config.init.entrySet())
		{
			Expression varInit = new Operation(Operator.EQUAL, new Variable(varName), new Constant(0));
			Expression newInitEx = new Operation(Operator.AND, e.getValue(), varInit);
			
			newInit.put(e.getKey(), newInitEx);
		}
		
		config.init = newInit;
	}

	private void processParams(String params)
	{
		if (params.trim().length() == 0)
			params = DEFAULT_JUMPS + ";" + DEFAULT_DELTA;
		
		String[] parts = params.split(";");
		
		if (parts.length != 2)
			throw new AutomatonExportException("Expected 2 pass params separated by ';', instead received: " + params);
		
		try
		{
			numJumps = Integer.parseInt(parts[0]);
			delta = Double.parseDouble(parts[1]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing number: " + e, e);
		}
		
		Hyst.log("Using regularization params numJumps = " + numJumps + ", delta = " + delta);
	}

}

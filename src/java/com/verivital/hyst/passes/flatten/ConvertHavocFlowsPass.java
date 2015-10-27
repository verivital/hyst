package com.verivital.hyst.passes.flatten;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
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
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

/**
 * This pass converts interval havoc flows (variables with no differential equation defined, only invariants), into ones where
 * there's an incoming (nondeterministic) reset, and dynamics of var' == 0.
 * @author sbak
 *
 */
public class ConvertHavocFlowsPass extends TransformationPass
{
	public ConvertHavocFlowsPass()
	{
		preconditions = new Preconditions(true); // skip all checks
		
		// except require that it's flat
		preconditions.skip[PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON.ordinal()] = false;
	}
	
	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		HashSet <String> havocVariables = new HashSet <String>();
		
		// remove modes with unsatisfiable havoc flows
		ArrayList <AutomatonMode> modesToRemove = new ArrayList <AutomatonMode>();
		
		for (AutomatonMode am : ha.modes.values())
		{
			for (String name : ha.variables)
			{
				if (am.urgent)
					continue;
				
				ExpressionInterval ei = am.flowDynamics.get(name);
				
				if (ei == null)
				{
					// havoc flow for this variable
					havocVariables.add(name);
					
					Interval range = null;
					try
					{
						range = RangeExtractor.getVariableRange(am.invariant, name);
					} 
					catch (EmptyRangeException ex)
					{
						Hyst.logDebug("Removing Mode with unsatisfiable havoc range: " + am.name + " for havoc variable " + name);
						modesToRemove.add(am);
						break;
					}
					catch (ConstantMismatchException x)
					{
						Hyst.logDebug("Removing Mode '" + am.name + "', because unsatisfiable constants in invariant: " + am.invariant);
						modesToRemove.add(am);
						break;
					}
					
					if (range == null || range.isOpenInterval())
					{
						// this is only allowed if the havoc variable is not used in this location
						if (variableIsUsedInFlow(am, name))	
							throw new AutomatonExportException("Havoc flow variable " + name + 
								" must have closed range defined by invariant. range = " + range);
						else
							continue; // havoc variable is not referenced anyway
					}
									
					Hyst.logDebug("Converting havoc flow from variable " + name + " to range " + range + " in mode " + am.name);
					
					// add a reset on this variable for all incoming transitions to be the range interval
					addResetToIncomingTransitions(ha, am.name, name, range);
					
					// set the flow for this variable to be 0, since it's an interval which doesn't change
					am.flowDynamics.put(name, new ExpressionInterval(new Constant(0)));
				}
			}
		}
		
		// remove illegal havoc modes
		if (modesToRemove.size() > 0)
		{
			Hyst.log("\nRemoving " + modesToRemove.size() + " modes due to unsatisfiable havoc variables.");

			// remove from init
			for (AutomatonMode am : modesToRemove)
				config.init.remove(am.name);
			
			removeModesAndTransitions(ha, modesToRemove);
			Hyst.logDebug(ha.toString());
			
			Hyst.log("\nRunning discrete reachability to remove newly disconnected modes");
			new RemoveDiscreteUnreachablePass().runTransformationPass(config, null);
			
			Hyst.logDebug(ha.toString());
		}
		
		// set derivative of all variables in havocVariables to zero in other modes
		for (AutomatonMode am : ha.modes.values())
		{
			if (am.urgent)
				continue;
			
			for (String v : havocVariables)
			{
				if (am.flowDynamics.get(v) == null)
					am.flowDynamics.put(v, new ExpressionInterval(new Constant(0)));
			}
		}
				
		// set initial of havocVariables to zero
		for (String var : havocVariables)
		{
			for (Entry<String, Expression> e : config.init.entrySet())
			{
				Expression exp = e.getValue();
				
				if (!RangeExtractor.expressionContainsVariable(exp, var))
				{
					Expression eqZero = new Operation(Operator.EQUAL, new Variable(var), new Constant(0));
					
					e.setValue(new Operation(Operator.AND, exp, eqZero));
				}
			}
		}
		
		validateDynamicsAssigned();
	}
	

	private void validateDynamicsAssigned()
	{
		// Validation: make sure every variable has defined dynamics in every (non-urgent) mode
		BaseComponent bc = (BaseComponent)config.root;
		
		for (AutomatonMode am : bc.modes.values())
		{
			if (am.urgent == false) 
			{
				for (String v : bc.variables)
				{
					if (!am.flowDynamics.containsKey(v))
					{
						throw new AutomatonExportException("After converting urgent dynamics, flow wasn't defined for variable '" 
								+ v + "' in mode '" + am.name + "'.");
					}
				}
			}
		}
	}

	/**
	 * Remove a set of modes and (associated) transitions from the automaton
	 * @param modesToRemove a set of AutomatonModes
	 */
	private static void removeModesAndTransitions(BaseComponent ha, ArrayList<AutomatonMode> modesToRemove)
	{
		for (AutomatonMode am : modesToRemove)
			ha.modes.remove(am.name);
		
		for (Iterator<AutomatonTransition> i = ha.transitions.iterator(); i.hasNext(); /* increment in loop */)
		{
			AutomatonTransition t = i.next();
			
			if (modesToRemove.contains(t.from) || modesToRemove.contains(t.to))
				i.remove();
		}
	}
	
	private static Expression addIntervalResetToExpression(String varName, Interval range, Expression e)
	{
		if (RangeExtractor.expressionContainsVariable(e, varName))
			throw new AutomatonExportException("Initial states contain reference to havoc var: " + varName);
		
		Operation minOp = new Operation(Operator.GREATEREQUAL, new Variable(varName), new Constant(range.min));
		Operation maxOp = new Operation(Operator.LESSEQUAL, new Variable(varName), new Constant(range.max));
		
		Operation o = new Operation(Operator.AND, e, minOp);
		o = new Operation(Operator.AND, o, maxOp);
		
		return o;
	}
	
	private void addResetToIncomingTransitions(BaseComponent ha, String modeName, String varName, Interval range)
	{
		// check initial mode
		Expression initExp = config.init.get(modeName);
		
		if (initExp != null)
		{
			Expression newInit = addIntervalResetToExpression(varName, range, initExp);
			
			config.init.put(modeName, newInit);
		}
		
		// check all transitions
		for (AutomatonTransition t : ha.transitions)
		{
			if (t.to.name.equals(modeName))
				addIntervalResetToTransition(varName, range, t);
		}
	}
	
	private static void addIntervalResetToTransition(String varName, Interval range, AutomatonTransition t)
	{
		ExpressionInterval curReset = t.reset.get(varName);
		
		if (curReset != null)
		{
			if (!curReset.equalsInterval(range))
				throw new AutomatonExportException("Reset already contains different condition on havoc variable '" 
					+ varName + "' existing condition is var := " + curReset + ", we wanted to use interval: " + range);
		}
		
		t.reset.put(varName, new ExpressionInterval(new Constant(0), range));
	}

	private static boolean variableIsUsedInFlow(AutomatonMode am, String var)
	{
		boolean rv = false;
		
		for (Entry<String, ExpressionInterval> e : am.flowDynamics.entrySet())
		{
			ExpressionInterval ei = e.getValue();
			
			if (ei == null)
				continue;
			
			Expression ex = ei.getExpression();
			
			if (RangeExtractor.expressionContainsVariable(ex, var))
			{
				rv = true;
				break;
			}
		}
		
		return rv;
	}
}

package com.verivital.hyst.util;

import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.ConvertIntervalConstantsPass;
import com.verivital.hyst.passes.basic.SplitDisjunctionGuardsPass;
import com.verivital.hyst.passes.flatten.ConvertHavocFlowsPass;
import com.verivital.hyst.passes.flatten.FlattenAutomatonPass;

/**
 * This class contains the checks that should be done before running a printer or pass. For example, some may explicitly
 * reject models with urgent transitions, or require flat automata. Checks can be selectively skipped or enabled by 
 * assigning to ToolPrinter.preconditions.skip in your printer or pass. Use the PreconditionsFlag enum to index into 
 * the skip array (PreconditionsFlag.NEEDS_ONE_VARIABLE.ordinal()). 
 * 
 * Upon detecting an error, checks may either convert the model, or raise a PreconditionException.
 */
public class Preconditions
{
	public boolean[] skip; // use Flags enum to mark these
	
	public void skip(PreconditionsFlag ... flags)
	{
		for (PreconditionsFlag f : flags)
			skip[f.ordinal()] = true;
	}
	
	/**
	 * Make a default Preconditions object, where every check is either enabled or disabled
	 * @param skipAll the default state of the checks
	 */
	public Preconditions(boolean skipAll)
	{
		skip = new boolean[PreconditionsFlag.values().length]; // all false by default
		
		for (int i = 0; i < PreconditionsFlag.values().length; ++i)
			skip[i] = skipAll;
	}
	
	/**
	 * Checks if indicated preconditions are met. Raises PrinterPreconditionException if not.
	 * @param c the configuration to check
	 */
	public void check(Configuration c, String name)
	{
		Hyst.log("Checking preconditions for " + name);
		
		if (!skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS.ordinal()]) 
			Preconditions.convertNondeterministicResets(c.root); // may create interval constants
		
		if (!skip[PreconditionsFlag.CONVERT_INTERVAL_CONSTANTS.ordinal()])
			Preconditions.convertIntervalConstants(c);
		
		if (!skip[PreconditionsFlag.CONVERT_DISJUNCTIVE_GUARDS.ordinal()])
			Preconditions.convertDisjunctiveGuards(c);
		
		if (!skip[PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON.ordinal()])
		{
			Preconditions.convertToFlat(c);
			
			if (!skip[PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED.ordinal()])
				Preconditions.convertAllFlowAssigned(c);
		}
		
		// conversions should be done before checks
		
		if (!skip[PreconditionsFlag.NEEDS_ONE_VARIABLE.ordinal()])
			Preconditions.hasAtLeastOneVariable(c);
		
		if (!skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS.ordinal()])
			Preconditions.noNondeterministicDynamics(c.root);
		
		if (!skip[PreconditionsFlag.NO_URGENT.ordinal()])
			Preconditions.noUrgentDynamics(c.root);
	}

	/**
	 * Checks if all flows are assigned in the flat automaton. If not runs the havoc transformation pass to ensure this.
	 * @param c
	 */
	public static void convertAllFlowAssigned(Configuration c)
	{
		boolean convert = false;
		BaseComponent bc = (BaseComponent)c.root;
		
		for (String v : bc.variables)
		{
			for (AutomatonMode am : bc.modes.values())
			{
				if (am.urgent)
					continue;
				
				if (!am.flowDynamics.containsKey(v))
				{
					Hyst.log("Variable " + v + " didn't have dynamics defined in mode " + am.name 
							+ " as required in the preconditions. Attempting to convert using ConverHavocFlowsPass.");
					convert = true;
					break;
				}
			}
			
			if (convert)
				break;
		}
		
		if (convert)
		{
			Hyst.log("Converting Havoc Flows");
			new ConvertHavocFlowsPass().runTransformationPass(c, null);
		}
	}

	/**
	 * Checks if a model has guards that contain a top-level disjunction (or). If so, it will split the transitions into
	 * two, each with one of the conditions
	 * @param c the configuration to check and modify
	 */
	public static void convertDisjunctiveGuards(Configuration c)
	{
		new SplitDisjunctionGuardsPass().runVanillaPass(c, SplitDisjunctionGuardsPass.PRINT_PARAM);
	}

	/**
	 * Checks if the model has at least one variable. Raises PrinterPreconditionException if not.
	 * @param c the configuration to check
	 */
	public static void hasAtLeastOneVariable(Configuration c)
	{
		if (c.root.getAllVariables().size() == 0)
			throw new PreconditionsFailedException("Printer requires at least one continuous variable in the model.");
	}

	/**
	 * Checks if the model is flat. If not, prints a log message and runs the flattening pass to conver it
	 * @param c the configuration to check
	 */
	public static void convertToFlat(Configuration c)
	{
		if (!(c.root instanceof BaseComponent))
		{
			Hyst.log("Preconditions check detected a non-flat (network) automaton.");
			Hyst.log("Flattening automaton as required by preconditions.");
			FlattenAutomatonPass.flattenAndOptimize(c);
		}
	}
	
	/**
	 * Checks if the model has intervals as constants. If so, will print a log message and run the conversion pass to convert
	 * constants to variables
	 * @param c the configuration to check
	 */
	public static void convertIntervalConstants(Configuration c)
	{
		if (containsIntervalConstants(c.root))
		{
			Hyst.log("Preconditions check detected interval-valued constants. ");
			Hyst.log("Running conversion pass to make them variables, as required by the preconditions.");
			
			new ConvertIntervalConstantsPass().runVanillaPass(c, null);
		}
	}

	/**
	 * Check if a given component has constants that are mapped to intervals
	 * @param c the component to check 
	 * @return true iff c or any of its children contains interval-valued constants
	 */
	public static boolean containsIntervalConstants(Component c)
	{
		boolean rv = false;
		
		if (c instanceof BaseComponent)
		{
			// base case
			for (Interval i : c.constants.values())
			{
				if (i != null && !i.isPoint())
				{
					rv = true;
					break;
				}
			}
		}
		else
		{
			// recursive case, network component
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
			{
				if (containsIntervalConstants(ci.child))
				{
					rv = true;
					break;
				}
			}
		}
		
		return rv;
	}
	
	/**
	 * Check that there are no nondeterministic resets
	 * @param c
	 */
	public static void convertNondeterministicResets(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			BaseComponent bc = (BaseComponent)c;
			
			for (AutomatonTransition at : bc.transitions)
			{
				for (Entry<String, ExpressionInterval> e : at.reset.entrySet())
				{
					String var = e.getKey();
					ExpressionInterval ei = e.getValue();
					
					if (e.getValue().getInterval() != null)
					{
						Hyst.log("Converting nondeterministic reset " + var + "=" + 
								ei.toString(DefaultExpressionPrinter.instance) + " in automaton " 
								+ bc.getPrintableInstanceName());
						
						String intervalVar = AutomatonUtil.freshName(var + "_interval", bc.getAllNames());
						
						bc.constants.put(intervalVar, ei.getInterval());
						ei.setInterval(null);
						
						Expression sum = new Operation(Operator.ADD, ei.getExpression(), new Variable(intervalVar));
						ei.setExpression(sum);
					}
				}
			}
		}
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
				convertNondeterministicResets(ci.child);
		}
	}
		
	/**
	 * Check if there are non-deterministic dynamics (interval-expressions with nonnull intervals) in the model
	 * @param c the component to check
	 */
	public static void noNondeterministicDynamics(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			BaseComponent bc = (BaseComponent)c;
			
			for (AutomatonMode am : bc.modes.values())
			{
				if (am.urgent)
					continue;
				
				for (Entry<String, ExpressionInterval> e : am.flowDynamics.entrySet())
				{
					if (e.getValue().isInterval())
					{
						throw new PreconditionsFailedException("Nondeterministic flow detected for variable " + e.getKey() 
								+ " in automaton " + c.getPrintableInstanceName() + " not allowed by preconditions.");
					}
				}
			}
		}
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
				noNondeterministicDynamics(ci.child);
		}
	}

	/**
	 * Check that there are no urgent dynamics (instant mode changes)
	 * @param c
	 */
	public static void noUrgentDynamics(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			BaseComponent bc = (BaseComponent)c;
			
			for (AutomatonMode am : bc.modes.values())
			{
				if (am.urgent)
					throw new PreconditionsFailedException("Urgent dynamics not supported by printer. Found in automaton " 
							+ bc.getPrintableInstanceName() + ", mode " + am.name);
			}
		}
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
				noUrgentDynamics(ci.child);
		}
	}
	
	@SuppressWarnings("serial")
	public static class PreconditionsFailedException extends RuntimeException
	{
		public PreconditionsFailedException(String s)
		{
			super(s);
		}
		
	}
}

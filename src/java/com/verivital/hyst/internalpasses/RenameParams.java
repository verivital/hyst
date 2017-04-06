package com.verivital.hyst.internalpasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.ExpressionModifier;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object. By
 * convention, call the static run() method to perform the transformation.
 * 
 * @author Stanley Bak
 */
public class RenameParams
{
	/**
	 * Swaps params (variable / constant / label) names for alternates. Params are a colon separated
	 * list of names: oldname1:newname1:oldname2:newname2:...
	 * 
	 * If new name exists, a number will be appended to it (the number starts at 2 and is
	 * incremented until a fresh variable is found)
	 *
	 * @param convertMap
	 *            the mapping of oldName -> newName
	 */
	public static void run(Configuration config, Map<String, String> convertMap)
	{
		BaseComponent ha = (BaseComponent) config.root;

		swapNames(ha, convertMap);

		// modify the configuration
		SwapExpressionModifier swapper = new SwapExpressionModifier(convertMap);
		ExpressionModifier.modifyInitForbidden(config, swapper);

		swapPlotVariables(config, convertMap);
	}

	/**
	 * Swap the parameter names used in this component
	 * 
	 * @param c
	 *            the component to swap
	 */
	public static void swapNames(Component c, Map<String, String> convertMap)
	{
		swapIONames(c, convertMap);

		if (c instanceof NetworkComponent)
			swapNetworkNames((NetworkComponent) c, convertMap);
		else
			swapBaseNames((BaseComponent) c, convertMap);
	}

	private static void swapNetworkNames(NetworkComponent nc, Map<String, String> convertMap)
	{
		// we need to rename the mappings from
		for (Entry<String, ComponentInstance> e : nc.children.entrySet())
		{
			ComponentInstance ci = e.getValue();

			renameMapping(ci.varMapping, convertMap);
			renameMapping(ci.constMapping, convertMap);
			renameMapping(ci.labelMapping, convertMap);
		}
	}

	/**
	 * Rename the parent variables in a mapping list
	 * 
	 * @param mappingList
	 *            the list to rename in
	 * @param convertMap
	 *            the list of renamings
	 */
	private static void renameMapping(ArrayList<ComponentMapping> mappingList,
			Map<String, String> convertMap)
	{
		for (ComponentMapping mapping : mappingList)
		{
			String newName = convertMap.get(mapping.parentParam);

			if (newName != null)
				mapping.parentParam = newName;
		}
	}

	private static void swapBaseNames(BaseComponent bc, Map<String, String> convertMap)
	{
		SwapExpressionModifier swapper = new SwapExpressionModifier(convertMap);

		// swap names in all the expressions
		ExpressionModifier.modifyBaseComponent(bc, swapper);

		// also swap left hand side of flows
		swapFlowsLHS(bc, convertMap);

		// also swap left hand side of resets
		swapResetsLHS(bc, convertMap);

		// swap the names of labels
		swapTransitionLabels(bc, convertMap);
	}

	private static void swapTransitionLabels(BaseComponent bc, Map<String, String> convertMap)
	{
		for (AutomatonTransition at : bc.transitions)
		{
			String label = at.label;

			if (label == null)
				continue;

			String to = convertMap.get(label);

			if (to == null)
				continue;

			at.label = to;
		}
	}

	/**
	 * Swap the names stored in the variables / constants / labels
	 * 
	 * @param c
	 *            the component to swap with
	 * @param convertMap
	 *            the conversion map
	 */
	private static void swapIONames(Component c, Map<String, String> convertMap)
	{
		// rename variables
		swapVariables(c, convertMap);

		// rename constants
		swapConstants(c, convertMap);

		// rename labels
		swapLabels(c, convertMap);
	}

	private static void swapPlotVariables(Configuration config, Map<String, String> convertMap)
	{
		for (int i = 0; i < config.settings.plotVariableNames.length; ++i)
		{
			String oldName = config.settings.plotVariableNames[i];
			String newName = oldName == null ? null : convertMap.get(oldName);

			if (newName == null)
				config.settings.plotVariableNames[i] = oldName;
			else
				config.settings.plotVariableNames[i] = newName;
		}
	}

	private static void swapFlowsLHS(BaseComponent ha, Map<String, String> convertMap)
	{
		for (AutomatonMode mode : ha.modes.values())
		{
			if (mode.urgent)
				continue;

			LinkedHashMap<String, ExpressionInterval> newFlow = new LinkedHashMap<String, ExpressionInterval>();

			for (Entry<String, ExpressionInterval> e : mode.flowDynamics.entrySet())
			{
				String oldVarName = e.getKey();
				String newVarName = convertMap.get(oldVarName);

				if (newVarName == null)
					newFlow.put(oldVarName, e.getValue());
				else
					newFlow.put(newVarName, e.getValue());
			}

			mode.flowDynamics = newFlow;
		}
	}

	private static void swapResetsLHS(BaseComponent ha, Map<String, String> convertMap)
	{
		for (AutomatonTransition tran : ha.transitions)
		{
			LinkedHashMap<String, ExpressionInterval> newReset = new LinkedHashMap<String, ExpressionInterval>();

			for (Entry<String, ExpressionInterval> e : tran.reset.entrySet())
			{
				String oldVarName = e.getKey();
				String newVarName = convertMap.get(oldVarName);

				if (newVarName == null)
					newReset.put(oldVarName, e.getValue());
				else
					newReset.put(newVarName, e.getValue());
			}

			tran.reset = newReset;
		}
	}

	private static void swapConstants(Component c, Map<String, String> convertMap)
	{
		LinkedHashMap<String, Interval> newConstants = new LinkedHashMap<String, Interval>();

		for (Entry<String, Interval> e : c.constants.entrySet())
		{
			String oldName = e.getKey();
			String newName = convertMap.get(oldName);

			if (newName == null)
				newConstants.put(oldName, e.getValue());
			else
				newConstants.put(newName, e.getValue());
		}

		c.constants = newConstants;
	}

	private static void swapVariables(Component c, Map<String, String> convertMap)
	{
		ArrayList<String> newVariables = new ArrayList<String>();

		for (String oldName : c.variables)
		{
			String newName = convertMap.get(oldName);

			if (newName == null)
				newVariables.add(oldName);
			else
				newVariables.add(newName);
		}

		c.variables = newVariables;
	}

	private static void swapLabels(Component c, Map<String, String> convertMap)
	{
		ArrayList<String> newLabels = new ArrayList<String>();

		for (String oldName : c.labels)
		{
			String newName = convertMap.get(oldName);

			if (newName == null)
				newLabels.add(oldName);
			else
				newLabels.add(newName);
		}

		c.labels = newLabels;
	}

	private static class SwapExpressionModifier extends ExpressionModifier
	{
		private Map<String, Variable> convertMap = new HashMap<String, Variable>();
		private ArrayList<Variable> newVariables = new ArrayList<Variable>();

		public SwapExpressionModifier(Map<String, String> convertNameMap)
		{
			for (Entry<String, String> e : convertNameMap.entrySet())
			{
				Variable v = new Variable(e.getValue());

				newVariables.add(v);
				convertMap.put(e.getKey(), v);
			}
		}

		@Override
		protected Expression modifyExpression(Expression e)
		{
			Expression rv = e;

			if (e instanceof Variable && !newVariables.contains(e))
			{
				Variable v = (Variable) e;

				Variable to = convertMap.get(v.name);

				if (to != null)
					rv = to;
			}
			else if (e instanceof Operation)
			{
				Operation o = (Operation) e;

				for (int i = 0; i < o.children.size(); ++i)
					o.children.set(i, modifyExpression(o.children.get(i)));
			}
			else if (e instanceof LutExpression)
			{
				LutExpression lut = (LutExpression) e;

				// modify the inputs
				for (int i = 0; i < lut.inputs.length; ++i)
					lut.inputs[i] = modifyExpression(lut.inputs[i]);

				// modify each table entry
				for (Entry<int[], Expression> entry : lut.table)
				{
					int[] index = entry.getKey();

					Expression tableExp = lut.table.get(index);
					Expression moddedTableExp = modifyExpression(tableExp);

					lut.table.setExpressionAtIndex(index, moddedTableExp);
				}
			}

			return rv;
		}
	}
}

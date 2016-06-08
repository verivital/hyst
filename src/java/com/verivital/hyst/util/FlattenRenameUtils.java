package com.verivital.hyst.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.RenameParams;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.ExpressionModifier;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.complex.FlattenAutomatonPass;

/**
 * Utility function for automaton flattening dealing with renaming of variables
 */
public class FlattenRenameUtils
{
	public static final String SEPARATOR = FlattenAutomatonPass.SEPARATOR;

	/**
	 * convert every variable, constant, and label to a fully-qualified name
	 * 
	 * @param root
	 *            the root component of the automaton
	 */
	public static void convertToFullyQualifiedParams(Component root)
	{
		final String ROOT_PREFIX = "";

		if (root instanceof NetworkComponent)
		{
			for (Entry<String, ComponentInstance> e : ((NetworkComponent) root).children.entrySet())
			{
				ComponentInstance ci = e.getValue();

				convertToFullyQualifiedParams(ci, ROOT_PREFIX);
			}
		}

		root.validate();
	}

	/**
	 * Convert the params in the given component to fully-qualified names
	 * 
	 * @param ci
	 *            the component instance we're converting
	 * @param parentPrefix
	 *            the parent prefix
	 */
	private static void convertToFullyQualifiedParams(ComponentInstance ci, String parentPrefix)
	{
		Component child = ci.child;
		String prefix = parentPrefix + child.instanceName + SEPARATOR;

		// create rename mapping
		HashMap<String, String> renameMapping = new HashMap<String, String>();

		renameMapping.putAll(getRenamings(child.variables, ci.varMapping, prefix));
		renameMapping.putAll(getRenamings(child.constants.keySet(), ci.constMapping, prefix));
		renameMapping.putAll(getRenamings(child.labels, ci.labelMapping, prefix));

		RenameParams.swapNames(child, renameMapping);

		if (child instanceof NetworkComponent)
		{
			NetworkComponent childNc = (NetworkComponent) child;

			// convert the children's children
			for (ComponentInstance childCi : childNc.children.values())
				convertToFullyQualifiedParams(childCi, prefix);
		}

		// convert parent's mapping of this child's variables
		ArrayList<ArrayList<ComponentMapping>> mappingLists = new ArrayList<ArrayList<ComponentMapping>>();
		mappingLists.add(ci.constMapping);
		mappingLists.add(ci.labelMapping);
		mappingLists.add(ci.varMapping);

		for (ArrayList<ComponentMapping> mappingList : mappingLists)
		{
			for (ComponentMapping cm : mappingList)
				cm.childParam = cm.parentParam;
		}

		ci.child.validate();
	}

	/**
	 * Get a set of rename assignments (oldName->newName) for a single type of parameter. Variables
	 * are renamed if they don't have a mapping in the parent, or if the parent uses a different
	 * name.
	 * 
	 * @param childNames
	 *            the set of parameters to check (variables, labels, constants)
	 * @param parentMappings
	 *            the set of mappings for this variable type
	 * @param prefix
	 *            the prefix to apply for the renaming if it's local
	 * @return a map of oldNames->newNames
	 */
	private static Map<String, String> getRenamings(Collection<String> childNames,
			ArrayList<ComponentMapping> parentMappings, String prefix)
	{
		Map<String, String> rv = new HashMap<String, String>();

		for (String v : childNames)
		{
			// either it's a mapped name, or it's a local
			String from = null;

			// get v's mapped value in the parent
			for (ComponentMapping m : parentMappings)
			{
				if (m.childParam.equals(v))
				{
					from = m.parentParam;
					break;
				}
			}

			if (from == null)
				rv.put(v, prefix + v); // local, use prefix
			else if (from.equals(v))
				continue; // same name, ignore
			else
				rv.put(v, from); // use parent name
		}

		return rv;
	}

	private static class ChangeDotsModifier extends ExpressionModifier
	{
		private static final String DOT = ".";

		@Override
		protected Expression modifyExpression(Expression e)
		{
			Expression rv = e;

			if (e instanceof Variable)
			{
				Variable v = (Variable) e;

				if (v.name.contains(DOT))
				{
					String newName = modifyString(v.name);
					rv = new Variable(newName);
				}
			}
			else if (e instanceof Operation)
			{
				Operation o = (Operation) e;

				for (int i = 0; i < o.children.size(); ++i)
					o.children.set(i, modifyExpression(o.children.get(i)));
			}

			return rv;
		}

		public static String modifyString(String from)
		{
			return from.replace(DOT, SEPARATOR);
		}
	}

	public static void convertSettingsSeparator(Configuration c)
	{
		changeModeNamesInitForbidden(c);

		// change expressions in mode
		ExpressionModifier.modifyInitForbidden(c, new ChangeDotsModifier());

		// plot settings
		for (int i = 0; i < c.settings.plotVariableNames.length; ++i)
		{
			String plotVar = c.settings.plotVariableNames[i];

			if (plotVar != null)
				c.settings.plotVariableNames[i] = ChangeDotsModifier.modifyString(plotVar);
		}
	}

	/**
	 * Change mode names in init and forbidden from the dotted notation to the flattened equivalent
	 * 
	 * @param c
	 *            the configuration
	 */
	private static void changeModeNamesInitForbidden(Configuration c)
	{
		LinkedHashMap<String, Expression> newInit = new LinkedHashMap<String, Expression>();
		LinkedHashMap<String, Expression> newForbidden = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : c.init.entrySet())
		{
			String newName = ChangeDotsModifier.modifyString(e.getKey());
			newInit.put(newName, e.getValue());
		}

		for (Entry<String, Expression> e : c.forbidden.entrySet())
		{
			String newName = ChangeDotsModifier.modifyString(e.getKey());
			newForbidden.put(newName, e.getValue());
		}

		c.init = newInit;
		c.forbidden = newForbidden;
	}
}

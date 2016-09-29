package com.verivital.hyst.internalpasses;

import java.util.Iterator;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object. By
 * convention, call the static run() method to perform the transformation.
 * 
 * @author Stanley Bak
 */
public class ConvertConstToVariable
{
	/**
	 * Convert a single constant in the configuration to a variable.
	 * 
	 * @param varName
	 *            the constant to convert (may have dots).
	 *
	 */
	public static void run(Configuration config, String varName)
	{
		Interval value = convertRec(config.root, varName);

		// if it was a value-mapped constant, add the value as an initial state
		if (value != null)
		{
			Operation le = new Operation(Operator.LESSEQUAL, new Variable(varName),
					new Constant(value.max));
			Operation ge = new Operation(Operator.GREATEREQUAL, new Variable(varName),
					new Constant(value.min));
			Expression cond = Expression.and(ge, le);

			for (Entry<String, Expression> e : config.init.entrySet())
				e.setValue(Expression.and(e.getValue(), cond));
		}
	}

	/**
	 * Recursively run the conversion on a component
	 * 
	 * @param c
	 *            the component to run it on
	 * @param name
	 *            the variable name
	 */
	private static Interval convertRec(Component c, String name)
	{
		Interval rv = null;

		if (c instanceof BaseComponent)
			rv = convertBase((BaseComponent) c, name);
		else
			rv = convertNetwork((NetworkComponent) c, name);

		return rv;
	}

	/**
	 * Run the conversion on a network component and all it's children
	 * 
	 * @param nc
	 *            the component to run it on
	 * @param name
	 *            the (dotted) variable name to convert
	 */
	private static Interval convertNetwork(NetworkComponent nc, String name)
	{
		Interval rv = null;

		int index = name.indexOf(".");
		if (index == -1)
		{
			if (!nc.constants.containsKey(name))
				throw new AutomatonExportException("Constant not found, but conversion desired: "
						+ name + " in component " + nc.instanceName);

			rv = nc.constants.get(name);

			// add it as a variable
			changeMapping(nc, name);
			setVariableDynamics(nc, name, new ExpressionInterval(new Constant(0)));
		}
		else
		{
			// the constant is local to a subcomponent (not mapped)
			String componentName = name.substring(0, index);
			String rest = name.substring(index + 1);

			ComponentInstance ci = nc.children.get(componentName);

			if (ci == null)
				throw new AutomatonExportException("Constant not found, but conversion desired: "
						+ name + " in component " + nc.instanceName);

			rv = convertRec(ci.child, rest);
		}

		return rv;
	}

	/**
	 * Set the dynamics for a variable. If c is a network, it set in the first child component.
	 * 
	 * @param c
	 *            the component where to set the variable
	 * @param parentName
	 *            the variable name in c
	 * @param dy
	 *            the dynamics to set
	 */
	private static void setVariableDynamics(Component c, String parentName, ExpressionInterval dy)
	{
		// add the variable if it doesn't exist
		if (!c.variables.contains(parentName))
			c.variables.add(parentName);

		if (c instanceof BaseComponent)
		{
			// base case, set the dynamics for the variable in each mode
			for (AutomatonMode am : ((BaseComponent) c).modes.values())
			{
				if (!am.urgent)
					am.flowDynamics.put(parentName, dy.copy());
			}
		}
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent) c;
			ComponentInstance first = nc.children.values().iterator().next();
			String childName = null;

			// find the existing mapping
			for (ComponentMapping map : first.varMapping)
			{
				if (map.parentParam.equals(parentName))
				{
					childName = map.childParam;
					break;
				}
			}

			if (childName == null)
			{
				// if there is no mapping in the child, make one
				childName = AutomatonUtil.freshName(parentName, first.child.variables);
				first.varMapping.add(new ComponentMapping(childName, parentName));
			}

			setVariableDynamics(first.child, childName, dy);
		}
	}

	/**
	 * Change the mapping (recursively) from a const to a var
	 * 
	 * @param nc
	 *            the current component
	 * @param name
	 *            the constant's name we're changing
	 */
	private static void changeMapping(NetworkComponent nc, String name)
	{
		nc.constants.remove(name);
		nc.variables.add(name);

		// update the children mapping
		for (ComponentInstance ci : nc.children.values())
		{
			Iterator<ComponentMapping> mapIt = ci.constMapping.iterator();

			while (mapIt.hasNext())
			{
				ComponentMapping map = mapIt.next();

				if (map.parentParam.equals(name))
				{
					ci.varMapping.add(map); // add this const mapping to the var
											// mapping

					if (ci.child instanceof NetworkComponent)
					{
						changeMapping((NetworkComponent) ci.child, map.childParam);
					}
					else
					{
						// it's a base component
						ci.child.constants.remove(name);
						ci.child.variables.add(name);
					}

					mapIt.remove(); // remove the const mapping
				}
			}
		}
	}

	/**
	 * Run the conversion on a base component (base case).
	 * 
	 * @param bc
	 *            the component to convert
	 * @param name
	 *            the variable name to convert
	 */
	private static Interval convertBase(BaseComponent bc, String name)
	{
		Interval rv = null;

		if (!bc.constants.containsKey(name))
			throw new AutomatonExportException("Constant not found, but conversion desired: " + name
					+ " in component " + bc.instanceName);

		rv = bc.constants.get(name);

		bc.constants.remove(name);
		setVariableDynamics(bc, name, new ExpressionInterval(new Constant(0)));

		return rv;
	}
}

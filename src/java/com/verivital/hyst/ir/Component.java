package com.verivital.hyst.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;

/**
 * A component is a base component (single hybrid automaton with labels and I/O), or a network
 * component (composition). This is the base class for both of these.
 * 
 * the variableNames, constants, and labels, are only those visible at this level (not locals of
 * subcomponents)
 * 
 * Validation guarantees: variablesNames, and labels are not null and unique, and are valid C
 * variable names constants are valid c variable names, may be null (if inferred from parent, but
 * must be definable) for most printers, constants will map to single values, and you can use
 * Interval.asConstant() to get the value instance name should only be null for root (if parent ==
 * null) and, if valid, is a valid C variable name names should not repeat between the components
 * template may be null (for example, if it was a code-constructed automaton). It also may be this
 * (circular), if this is a template automaton
 *
 */
public abstract class Component
{
	public static boolean VALIDATE_CONSTS = true; // should constant
													// names/values be checked
	public NetworkComponent parent;
	public String instanceName; // component instance name, null for root
								// component

	public ArrayList<String> variables = new ArrayList<String>();
	public LinkedHashMap<String, Interval> constants = new LinkedHashMap<String, Interval>();
	public ArrayList<String> labels = new ArrayList<String>();

	public Component template; // the template component this was instantiated
								// from (may be null)

	public void validate()
	{
		if (!Configuration.DO_VALIDATION)
			return;

		if (instanceName != null)
			validateName(instanceName, getPrintableInstanceName());

		if ((instanceName == null) != (parent == null))
			throw new AutomatonValidationException("instanceName was "
					+ (instanceName == null ? "<null>" : instanceName) + " but parent was "
					+ (parent == null ? "<null>" : "not null") + ((template == null) ? ""
							: ". Component template is " + template.instanceName));

		if (variables == null)
			throw new AutomatonValidationException("variable names is null");

		if (constants == null)
			throw new AutomatonValidationException("constants is null");

		if (labels == null)
			throw new AutomatonValidationException("labels is null");

		// instanceName should be a valid name
		if (instanceName != null)
			validateName(instanceName, "<instance name>");

		// names should not repeat
		HashSet<String> allNames = new HashSet<String>();

		checkNameConflicts(allNames, variables, "variable names of " + getPrintableInstanceName());
		checkNameConflicts(allNames, labels, "label names of " + getPrintableInstanceName());

		// we may skip this during automaton construction
		if (VALIDATE_CONSTS)
		{
			checkNameConflicts(allNames, constants.keySet(),
					"constant names of " + getPrintableInstanceName());

			// make sure every constant has a value
			for (String cName : constants.keySet())
			{
				try
				{
					getConstantValue(cName);
				}
				catch (AutomatonExportException e)
				{
					throw new AutomatonValidationException("Constant '" + cName + "' in automaton "
							+ getPrintableInstanceName() + " is unmapped", e);
				}
			}
		}
	}

	/**
	 * Get the value of a constant, which may be inherited from the parent
	 * 
	 * @param name
	 *            the name of the constant
	 */
	public Interval getConstantValue(String name)
	{
		if (!constants.containsKey(name))
			throw new AutomatonExportException("Constants not found: " + name);

		Interval val = constants.get(name);

		if (parent != null)
		{
			for (ComponentMapping ci : parent.children.get(instanceName).constMapping)
			{
				if (ci.childParam.equals(name))
				{
					if (constants.get(name) != null)
						throw new AutomatonExportException(
								"Constant was both assigned and mapped in parent: '" + name
										+ "' in instance '" + getPrintableInstanceName() + "'");

					val = parent.getConstantValue(ci.parentParam);
					break;
				}
			}
		}

		if (val == null)
		{
			val = constants.get(name);

			if (val == null)
				throw new AutomatonExportException("Unassigned constant: '" + name
						+ "' in instance '" + getPrintableInstanceName() + "'");
		}

		return val;
	}

	/**
	 * Check if a name is valid. C identifier names are allowed (first letter is '_' or letter,
	 * remaining letters can be alphanumeric or '_')
	 * 
	 * @param s
	 *            the String to check
	 * @param componentName
	 *            the instance name of the component (for error checking)
	 */
	public static void validateName(String s, String componentName)
	{
		if (s == null)
			throw new AutomatonValidationException("null name in: '" + componentName + "'");

		if (s.contains(".") || s.length() == 0)
			throw new AutomatonValidationException(
					"forbidden name: '" + s + "' in '" + componentName + "'");

		char c = s.charAt(0);

		if (Character.isLetter(c) || c == '_')
		{
			for (int i = 0; i < s.length(); ++i)
			{
				c = s.charAt(i);

				if (!Character.isLetterOrDigit(c) && c != '_')
					throw new AutomatonValidationException(
							"forbidden name: '" + s + "' in '" + componentName + "'");
			}
		}
	}

	/**
	 * Check for name conflicts (and valid names) in variables, constants, labels. This accumulates
	 * names into allNames as it goes, and reports conflicts as they come up
	 * 
	 * @param allNames
	 *            the accumulator which checks for collisions
	 * @param newNames
	 *            the new names
	 */
	public static void checkNameConflicts(HashSet<String> allNames, Collection<String> newNames,
			String componentName)
	{
		for (String s : newNames)
		{
			if (allNames.contains(s))
				throw new AutomatonValidationException(
						"Repeated name '" + s + "' in component '" + componentName + "'");

			validateName(s, componentName);

			allNames.add(s);
		}
	}

	/**
	 * get the names of all the variables (include locals in subcomponents) in this component
	 */
	public abstract Collection<String> getAllVariables();

	/**
	 * get the names and values of all the constants (include locals in subcomponents) in this
	 * component
	 */
	public Map<String, Interval> getAllConstants()
	{
		Map<String, Interval> rv = new HashMap<String, Interval>();

		for (Entry<String, Interval> r : constants.entrySet())
		{
			String name = r.getKey();
			Interval d = r.getValue();

			boolean mappedInParent = false;

			if (parent != null)
			{
				for (ComponentMapping ci : parent.children.get(instanceName).constMapping)
				{
					if (ci.childParam.equals(name))
					{
						mappedInParent = true;
						break;
					}
				}
			}

			if (!mappedInParent)
			{
				String instName = getFullyQualifiedInstanceName();

				if (instName.length() > 0)
					instName += ".";

				rv.put(instName + name, d);
			}
		}

		return rv;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("[Component - instanceName: " + getPrintableInstanceName());

		if (parent != null)
			str.append("\nParent name: " + parent.getPrintableInstanceName());
		else
			str.append("\nParent: <null>");

		if (template != null)
			str.append("\nTemplate Name: " + template.instanceName);
		else
			str.append("\nTemplate: <null>");

		str.append("\nVariable names: " + Arrays.toString(variables.toArray()));
		str.append("\nConstants: " + Arrays.toString(constants.entrySet().toArray()));
		str.append("\nLabels: " + Arrays.toString(labels.toArray()));
		str.append("]");

		return str.toString();
	}

	/**
	 * Set the value of an existing constant
	 * 
	 * @param name
	 *            the name of the constant (can be dotted for local subcomponents)
	 * @param val
	 *            the value to assign it
	 */
	public void setConstant(String name, Interval val)
	{
		if (!constants.containsKey(name))
			throw new AutomatonExportException("Automaton does not contain constant: " + name);

		constants.put(name, val);
	}

	/**
	 * Deep-copy the component, and return it
	 */
	public Component copy()
	{
		// copy the child-specific parts
		Component rv = copyComponent();

		// copy the parent parts
		rv.parent = null; // parent will need to be re-assigned at a higher
							// level for deep copy
		rv.instanceName = null; // instance name should be assigned

		rv.variables = new ArrayList<String>();
		rv.variables.addAll(variables);

		rv.constants = new LinkedHashMap<String, Interval>();
		rv.constants.putAll(constants);

		rv.labels = new ArrayList<String>();
		rv.labels.addAll(labels);
		rv.template = template; // shallow copy template

		return rv;
	}

	/**
	 * Get the fully-qualified instance name (with dots). For root component this is the empty
	 * string
	 * 
	 * @return the instance name (may have dots)
	 */
	public String getFullyQualifiedInstanceName()
	{
		String rv = instanceName == null ? "" : instanceName;

		if (parent != null && parent.instanceName != null)
			rv = parent.getFullyQualifiedInstanceName() + "." + instanceName;

		return rv;
	}

	/**
	 * Get the fully-qualified variable name (with dots). For root component this is just the
	 * variable name. If there's a parent the name may be remapped.
	 * 
	 * @param var
	 *            the variable name in this component that we're interested in
	 * @return the variable name (may have dots if it's a local)
	 */
	public String getFullyQualifiedVariableName(String var)
	{
		String rv = var;

		if (parent != null)
		{
			// check if there's a remapping from the parent
			boolean found = false;
			ComponentInstance ci = parent.children.get(instanceName);

			if (ci == null)
				throw new AutomatonExportException(
						"Error getting instance named '" + instanceName + "' in parent automaton.");

			for (ComponentMapping mapping : ci.varMapping)
			{
				if (mapping.childParam.equals(var))
				{
					// lookup the real name of the parent param this varible was
					// mapped to
					rv = parent.getFullyQualifiedVariableName(mapping.parentParam);

					found = true;
					break;
				}
			}

			if (!found) // not found in parent mappinglocal variable
				rv = getFullyQualifiedInstanceName() + "." + var;
		}

		return rv;
	}

	/**
	 * Get the instance name, or "<root>" is it's null
	 * 
	 * @return the name
	 */
	public String getPrintableInstanceName()
	{
		return instanceName == null ? "<root>" : instanceName;
	}

	/**
	 * Deep-copy the component, and return it
	 */
	protected abstract Component copyComponent();
}

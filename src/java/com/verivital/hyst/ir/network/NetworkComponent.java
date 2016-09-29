package com.verivital.hyst.ir.network;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;

/**
 * A network component as part of a hybrid automaton. This can compose one or more subcomponents, as
 * well as do renaming of variables, labels, constants, ect.
 * 
 * The children of this class are the instantiated sub-components, not the templates. Thus, if a
 * single base component is instantiated several times, there will be several children here.
 * 
 * Validation guarantees: children is not null and size > 0, instance names are valid identifiers,
 * Components are nonnull
 * 
 */
public class NetworkComponent extends Component
{
	// should printing using toString() be recursive?
	public static boolean PRINT_RECURSIVE = true;

	// map instance name -> sub-component
	public LinkedHashMap<String, ComponentInstance> children = new LinkedHashMap<String, ComponentInstance>();

	public void validate()
	{
		if (!Configuration.DO_VALIDATION)
			return;

		super.validate();

		if (children == null || children.size() == 0)
			throw new AutomatonValidationException(
					"children was null or empty in network component");

		for (Entry<String, ComponentInstance> e : children.entrySet())
		{
			ComponentInstance ci = e.getValue();
			ci.validate();

			if (!e.getKey().equals(ci.child.instanceName))
				throw new AutomatonValidationException(
						"Instance name in child doesn't match name in NetworkComponent");
		}
	}

	@Override
	public Collection<String> getAllVariables()
	{
		TreeSet<String> vars = new TreeSet<String>();

		for (Entry<String, ComponentInstance> entry : children.entrySet())
		{
			ComponentInstance ci = entry.getValue();

			Collection<String> childVars = ci.child.getAllVariables();

			vars.addAll(childVars);
		}

		return vars;
	}

	@Override
	public Map<String, Interval> getAllConstants()
	{
		Map<String, Interval> rv = super.getAllConstants();

		for (ComponentInstance c : children.values())
			rv.putAll(c.child.getAllConstants());

		return rv;
	}

	@Override
	public void setConstant(String var, Interval val)
	{
		if (!var.contains("."))
			super.setConstant(var, val);
		else
		{
			String[] parts = var.split("\\.");

			// add it to a subcomponent
			String instance = parts[0];
			String subVarName = var.substring(instance.length() + 1);

			ComponentInstance ci = children.get(instance);

			if (ci == null)
				throw new AutomatonExportException("Invalid constant in network component: " + var);

			ci.child.setConstant(subVarName, val);
		}
	}

	@Override
	protected Component copyComponent()
	{
		NetworkComponent rv = new NetworkComponent();

		for (Entry<String, ComponentInstance> e : children.entrySet())
		{
			String childInstanceName = e.getKey();
			ComponentInstance ci = e.getValue().copy();
			ci.parent = rv;
			ci.child.parent = this;
			ci.child.instanceName = childInstanceName;

			if (ci.child != null)
				ci.child.parent = rv;

			rv.children.put(childInstanceName, ci);
		}

		return rv;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();

		str.append("[NetworkComponent: ");

		str.append(super.toString());

		str.append("\nChildren (" + children.size() + " total) :");

		int index = 0;
		for (Entry<String, ComponentInstance> e : children.entrySet())
		{
			str.append("\nChild #" + (index++) + ": " + e.getKey() + " -> " + e.getValue());
		}

		str.append("]");

		return str.toString();
	}
}

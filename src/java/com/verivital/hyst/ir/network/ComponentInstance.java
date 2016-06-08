package com.verivital.hyst.ir.network;

import java.util.ArrayList;
import java.util.Collection;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;

/**
 * A component instance is part of a network component. It consists of a component, as well as a
 * mapping of the variable / constant / label names of the parent to those of the child.
 * 
 * Validation guarantees: parent and child are nonnnull childTemplateName is nonnull
 * 
 * mappings are nonnull and valid (parent names exist, child names exist)
 */
public class ComponentInstance
{
	public Component parent;
	public Component child;

	// variables and labels can only be mapped to names
	public ArrayList<ComponentMapping> varMapping = new ArrayList<ComponentMapping>();
	public ArrayList<ComponentMapping> labelMapping = new ArrayList<ComponentMapping>();

	// constants can be mapped only to names (value mapping get removed)
	public ArrayList<ComponentMapping> constMapping = new ArrayList<ComponentMapping>();

	public ComponentInstance(Component parent, Component child)
	{
		this.parent = parent;
		this.child = child;
	}

	public void validate()
	{
		if (parent == null)
			throw new AutomatonValidationException("parent is null");

		if (child == null)
			throw new AutomatonValidationException("child is null");

		if (child.parent == null)
			throw new AutomatonValidationException("child.parent null");

		if (child.parent != parent)
			throw new AutomatonValidationException(
					"child.parent not equal to the parent in the component instance");

		validateMapping(varMapping, parent.variables, child.variables, "variable");
		validateMapping(constMapping, parent.constants.keySet(), child.constants.keySet(), "const");
		validateMapping(labelMapping, parent.labels, child.labels, "label");

		child.validate();
	}

	private void validateMapping(ArrayList<ComponentMapping> mappingList,
			Collection<String> parentList, Collection<String> childList, String type)
	{
		if (mappingList == null)
			throw new AutomatonValidationException("mappingList is null");

		for (ComponentMapping m : mappingList)
			m.validate(parentList, childList, type, parent.getPrintableInstanceName());
	}

	public ComponentInstance copy()
	{
		ComponentInstance ci = new ComponentInstance(null, null);

		if (child != null)
			ci.child = child.copy();

		// parent will need to be reassigned at a higher level
		for (ComponentMapping v : varMapping)
			ci.varMapping.add(v.copy());

		for (ComponentMapping l : labelMapping)
			ci.labelMapping.add(l.copy());

		for (ComponentMapping c : constMapping)
		{
			if (c.parentParam.startsWith(TemplateImporter.TEMPLATE_VALUE_MAP_PREFIX))
			{
				// value map in parent, assign value in child
				String varStr = c.parentParam
						.substring(TemplateImporter.TEMPLATE_VALUE_MAP_PREFIX.length());
				try
				{
					// value maps can't be intervals
					double d = Double.parseDouble(varStr);
					ci.child.constants.put(c.childParam, new Interval(d));
				}
				catch (NumberFormatException e)
				{
					throw new AutomatonExportException(
							"Couldn't parse constant in ValueMap of parameter '" + c.childParam
									+ "': " + varStr,
							e);
				}
			}
			else
				ci.constMapping.add(c.copy());
		}

		return ci;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();

		str.append("[ComponentInstance - child.instanceName: ");
		str.append(child.getPrintableInstanceName());

		str.append("\nVariable Mapping: ");
		str.append(mappingString(varMapping));

		str.append("\nConstant Mapping: ");
		str.append(mappingString(constMapping));

		str.append("\nLabel Mapping: ");
		str.append(mappingString(labelMapping));
		str.append("\n");

		if (child.template == child)
			str.append("Child is a template automaton.");
		else if (NetworkComponent.PRINT_RECURSIVE)
			str.append(child);

		str.append("]");

		return str.toString();
	}

	private StringBuilder mappingString(ArrayList<ComponentMapping> mapping)
	{
		StringBuilder str = new StringBuilder();
		str.append("[");

		for (ComponentMapping map : mapping)
		{
			if (str.length() > 1) // not first
				str.append(", ");

			str.append(map);
		}

		str.append("]");

		return str;
	}
}

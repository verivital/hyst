package com.verivital.hyst.ir.network;

import java.util.Collection;

import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;

public class ComponentMapping
{
	public String childParam; // the parameter in the child
	public String parentParam; // the parameter in the parent (may be a
								// ~constant during template instantiation)

	public ComponentMapping(String childParam, String parentParam)
	{
		this.childParam = childParam;
		this.parentParam = parentParam;
	}

	public void validate(Collection<String> parentList, Collection<String> childList,
			String mappingType, String instanceName)
	{
		if (childParam == null)
			throw new AutomatonValidationException("childParam is null in mapping of " + mappingType
					+ " in component " + instanceName);

		if (parentParam == null)
			throw new AutomatonValidationException("parentParam is null in mapping of "
					+ mappingType + " in component " + instanceName);

		if (Component.VALIDATE_CONSTS
				|| !parentParam.startsWith(TemplateImporter.TEMPLATE_VALUE_MAP_PREFIX))
		{
			if (!parentList.contains(parentParam))
				throw new AutomatonValidationException(
						"parent component doesn't contained mapped " + mappingType + " for child '"
								+ parentParam + "' in parent component named " + instanceName);
		}

		if (!childList.contains(childParam))
			throw new AutomatonValidationException(
					"child component doesn't contained mapped " + mappingType + " '" + childParam
							+ "' in parent component named " + instanceName);
	}

	public ComponentMapping copy()
	{
		return new ComponentMapping(childParam, parentParam);
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();

		str.append("child.");
		str.append(childParam);
		str.append("->");
		str.append(parentParam);

		return str.toString();
	}
}

package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;

/**
 * A model transformation pass which copies a base-component instance in a network model
 * 
 * @author Stanley Bak (September 2015)
 *
 */
public class CopyInstancePass extends TransformationPass
{
	@Option(name = "-instance", aliases = {
			"-name" }, required = false, usage = "name of instance to copy", metaVar = "NAME")
	private String instanceName = "";

	@Option(name = "-number", aliases = { "-n",
			"-num" }, required = false, usage = "the number of copies at the end (1 = no copy)", metaVar = "NUM")
	private int num = 2;

	@Option(name = "-prefix", required = false, usage = "prefix to use for the copies", metaVar = "NAME")
	private String baseName = "copy";

	// populatd by extractInitialModes()
	HashMap<String, String> componentInstanceToInitModeMap = new HashMap<String, String>();
	String copiedInitialState = null;

	public CopyInstancePass()
	{
		// skip all checks and conversions
		this.preconditions = new Preconditions(true);

		// except run these checks
		this.preconditions.skip[PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN
				.ordinal()] = false;
	}

	@Override
	public String getName()
	{
		return "Copy Base Component Instance Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "copy";
	}

	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);

		// possibly could convert to network component here...
		if (!(c.root instanceof NetworkComponent))
			throw new PreconditionsFailedException("Root must be a network component");

		NetworkComponent nc = (NetworkComponent) c.root;

		for (Entry<String, ComponentInstance> entry : nc.children.entrySet())
		{
			if (!(entry.getValue().child instanceof BaseComponent))
				throw new AutomatonExportException(
						"Expected each child component of root to be a base component. '"
								+ entry.getKey() + "' was not.");
		}

		if (c.init.size() != 1)
			throw new PreconditionsFailedException(
					"Must have single initial mode. Count was: " + c.init.size());

		if (c.forbidden.size() > 1)
		{
			throw new PreconditionsFailedException("Max single forbidden mode.");
		}
	}

	@Override
	protected void runPass()
	{
		NetworkComponent root = (NetworkComponent) config.root;

		extractInitialModes(root);

		if (instanceName.length() == 0)
		{
			if (root.children.size() == 1)
				instanceName = root.children.keySet().iterator().next();
			else
				throw new AutomatonExportException(
						"Root component has multiple children and '-name' was not provided.");
		}

		ComponentInstance ci = root.children.get(instanceName);

		if (ci == null)
			throw new AutomatonExportException(
					"Root component did not contain named child instance: '" + instanceName
							+ "'. Valid options are: " + root.children.keySet());

		if (num < 1)
			throw new AutomatonExportException("Num instances must be >= 1. got: " + num);

		// Replicate ci 'num' times
		for (int i = 2; i <= num; ++i)
		{
			Component child = ci.child.copy();
			child.parent = ci.child.parent;
			ComponentInstance newInstance = new ComponentInstance(root, child);

			String name = baseName + i + "_" + instanceName;
			child.instanceName = name;

			if (root.children.containsKey(name))
				throw new AutomatonExportException(
						"Root contains conflicting component instance name: " + name);

			root.children.put(name, newInstance);

			// adjust the label mapping in newInstance
			copyLabels(root, i, ci.labelMapping, newInstance);

			// adjust the variable mapping in newInstance
			copyVariables(root, i, ci.varMapping, newInstance);

			// copy constants directly
			for (ComponentMapping mapping : ci.constMapping)
				newInstance.constMapping.add(mapping.copy());
		}

		copyInit(root, ci);
	}

	/**
	 * Populated componentInstanceToInitModeMap
	 * 
	 * @param root
	 */
	private void extractInitialModes(NetworkComponent root)
	{
		String[] initParts = config.init.keySet().iterator().next().split("\\.");

		if (initParts.length != root.children.size())
			throw new AutomatonExportException(
					"extracted init parts and number of base components differs");

		int index = 0;

		for (Entry<String, ComponentInstance> entry : root.children.entrySet())
		{
			String instanceName = entry.getKey();
			BaseComponent bc = (BaseComponent) entry.getValue().child;
			String initMode = initParts[index++];

			if (!bc.modes.containsKey(initMode))
				throw new AutomatonExportException("Base component '" + bc.instanceName
						+ "' doesn't contain a mode named " + initMode);

			componentInstanceToInitModeMap.put(instanceName, initMode);

			if (instanceName.equals(this.instanceName))
				copiedInitialState = initMode;
		}

		if (copiedInitialState == null)
			throw new AutomatonExportException(
					"did not find basecomponent with name " + this.instanceName);
	}

	private void copyVariables(NetworkComponent root, int index,
			ArrayList<ComponentMapping> mapList, ComponentInstance newInstance)
	{
		for (ComponentMapping varMapping : mapList)
		{
			String childParam = varMapping.childParam;
			String parentParam = baseName + index + "_" + childParam;
			newInstance.varMapping.add(new ComponentMapping(childParam, parentParam));

			if (root.variables.contains(parentParam))
				throw new AutomatonExportException(
						"Root contains conflicting variable name: " + parentParam);

			root.variables.add(parentParam);
		}
	}

	private void copyLabels(NetworkComponent root, int index, ArrayList<ComponentMapping> mapList,
			ComponentInstance newInstance)
	{
		for (ComponentMapping labelMapping : mapList)
		{
			String childParam = labelMapping.childParam;
			String parentParam = baseName + index + "_" + childParam;
			newInstance.labelMapping.add(new ComponentMapping(childParam, parentParam));

			if (root.labels.contains(parentParam))
				throw new AutomatonExportException(
						"Root contains conflicting label name: " + parentParam);

			root.labels.add(parentParam);
		}
	}

	private void copyInit(NetworkComponent root, ComponentInstance ci)
	{
		// also copy init / forbidden
		Entry<String, Expression> init = config.init.entrySet().iterator().next();

		StringBuilder newInitMode = new StringBuilder();

		for (String componentName : root.children.keySet())
		{
			String initMode = componentInstanceToInitModeMap.get(componentName);

			if (newInitMode.length() != 0)
				newInitMode.append(".");

			if (initMode != null)
				newInitMode.append(initMode);
			else
				newInitMode.append(copiedInitialState);
		}

		Expression newInit = init.getValue().copy();

		for (int i = 2; i <= num; ++i)
		{
			Expression initCopy = init.getValue().copy();

			// replace every variable by the new variable name
			for (ComponentMapping varMapping : ci.varMapping)
			{
				String oldVar = varMapping.parentParam;
				String newVar = baseName + i + "_" + oldVar;

				initCopy = replaceVariables(initCopy, oldVar, newVar);
			}

			newInit = Expression.and(newInit, initCopy);
		}

		config.init.clear();
		config.init.put(newInitMode.toString(), newInit);

		// create a custom error
		if (config.forbidden.size() == 1)
		{
			Expression errorGuardExp = config.forbidden.values().iterator().next();
			Expression combinedError = errorGuardExp.copy();

			for (int i = 2; i <= num; ++i)
			{
				Expression copyError = errorGuardExp.copy();

				// replace every variable by the new variable name
				for (ComponentMapping varMapping : ci.varMapping)
				{
					String oldVar = varMapping.parentParam;
					String newVar = baseName + i + "_" + oldVar;

					copyError = replaceVariables(copyError, oldVar, newVar);
				}

				combinedError = Expression.or(combinedError, copyError);
			}

			config.forbidden.clear();
			config.forbidden.put(newInitMode.toString(), combinedError);
		}
	}

	private Expression replaceVariables(Expression e, String oldVar, String newVar)
	{
		Expression rv = e;

		if (e instanceof Variable)
		{
			Variable v = (Variable) e;

			if (v.name.equals(oldVar))
				rv = new Variable(newVar);
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();
			Operation newOp = new Operation(o.op);

			for (Expression child : o.children)
				newOp.children.add(replaceVariables(child, oldVar, newVar));

			rv = newOp;
		}

		return rv;
	}
}

package com.verivital.hyst.passes.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.RemoveDiscreteUnreachablePass;
import com.verivital.hyst.passes.basic.RemoveSimpleUnsatInvariantsPass;
import com.verivital.hyst.util.FlattenRenameUtils;
import com.verivital.hyst.util.Preconditions;

/**
 * This transformation pass performs automaton flattening. Usually you want to call
 * flattenAndOptimize, which will flatten, remove discretely-unreachable modes, and convert havoc
 * dynamics. If you instead call the pass directly you'll only get the flattening (modes are a cross
 * product of the subcomponents), and havoc dynamics may not be resolved.
 * 
 * Modes get renamed based on the modes in each base component, separated by SEPARATOR('_'). This
 * can lead to ambiguous names if modes already have underscores in them; this isn't handled
 * currently.
 */
public class FlattenAutomatonPass extends TransformationPass
{
	public static final String SEPARATOR = "_";

	public FlattenAutomatonPass()
	{
		preconditions = new Preconditions(true); // skip all checks
	}

	/**
	 * Perform automaton flattening, as well as removing (discrete) unreachable states. This
	 * guarantees that:
	 * 
	 * - the root component of the configuration is a BaseComponent - every variable must have a
	 * defined flowDyanmics in every nonurgent mode (havoc dynamics are converted)
	 * 
	 * @param c
	 *            the configuration to perform the passes on
	 */
	public static void flattenAndOptimize(Configuration c)
	{
		// 1. do flatteneing
		new FlattenAutomatonPass().runVanillaPass(c, "");
		BaseComponent ha = (BaseComponent) c.root;
		Hyst.log("\nFlattened Automaton (" + ha.modes.size() + " locations and "
				+ ha.transitions.size() + " transitions)");
		Hyst.logDebug(c.toString());

		// 2. remove unreachable (gets rid of lots of modes)
		new RemoveDiscreteUnreachablePass().runVanillaPass(c, "");
		int numModes = ha.modes.size();
		Hyst.log("\nRemoved Discrete Unreachable Modes (" + numModes + " locations and "
				+ ha.transitions.size() + " transitions)");
		Hyst.logDebug(c.toString());

		// 3 get rid of unsat modes
		new RemoveSimpleUnsatInvariantsPass().runVanillaPass(c, "");
		Hyst.log("\nRemoved Unsat Modes (" + ha.modes.size() + " locations and "
				+ ha.transitions.size() + " transitions)");

		if (numModes != ha.modes.size())
		{
			// 4. remove unreachable again (if modes were deletes
			new RemoveDiscreteUnreachablePass().runVanillaPass(c, "");
			Hyst.log("\nRemoved Discrete Unreachable Modes again, since some were removed ("
					+ ha.modes.size() + " locations and " + ha.transitions.size()
					+ " transitions)");
			Hyst.logDebug(c.toString());
		}
	}

	@Override
	public String getCommandLineFlag()
	{
		return "flatten";
	}

	@Override
	public String getName()
	{
		return "Flatten Hybrid Automaton Pass";
	}

	@Override
	public void runPass()
	{
		FlattenRenameUtils.convertToFullyQualifiedParams(config.root);

		config.root = flatten(config.root);

		if (!(config.root instanceof BaseComponent))
			throw new AutomatonExportException("Flatten failed (root is not BaseComponent)");

		FlattenRenameUtils.convertSettingsSeparator(config);
	}

	private BaseComponent flatten(Component c)
	{
		BaseComponent rv = null;

		if (c instanceof BaseComponent)
		{
			String name = c.getFullyQualifiedInstanceName();

			if (name.length() == 0)
				name = "<root>";

			Hyst.log("Flatten called on '" + name + "' which is already a BaseComponent");
			rv = (BaseComponent) c.copy();
		}
		else
		{
			NetworkComponent nc = (NetworkComponent) c;

			if (nc.children.size() == 0)
				throw new AutomatonExportException(
						"Network component with zero children are not allowed.");

			Hyst.log("Flatten called on NetworkComponent '" + c.getFullyQualifiedInstanceName()
					+ "'");

			Hyst.logDebug(nc.toString() + "\n-------------");

			for (Entry<String, ComponentInstance> e : nc.children.entrySet())
			{
				ComponentInstance ci = e.getValue();
				// instantiate merge each component into rv

				BaseComponent bc = flatten(ci.child);
				Hyst.logDebug("flattened child: " + bc.toString() + "\n-------------");

				if (rv == null)
					rv = bc;
				else
				{
					Hyst.log("Merging " + rv.instanceName + " and " + bc.instanceName);
					rv = mergeComponents(rv, bc);

					Hyst.logDebug(rv.toString() + "\n-------------");
					Hyst.logDebug("Merged");
				}
			}

			// copy some parts from parent network component
			rv.instanceName = c.instanceName;
			mergeParams(rv, nc);
			fixVariableOrder(rv, nc);
		}

		return rv;
	}

	/**
	 * The variable ordering may have gotten messed up since when merging they get adding in the
	 * order they are used in each base component. This function modifies rv's variable order to
	 * first include all variables in nc's order, and then any remaining variables
	 * 
	 * @param rv
	 * @param nc
	 */
	private void fixVariableOrder(BaseComponent rv, NetworkComponent nc)
	{
		ArrayList<String> ordered = new ArrayList<String>();

		// add all the variables that exist in nc's order
		for (String var : nc.variables)
		{
			if (rv.variables.contains(var))
				ordered.add(var);
		}

		// add all remaining variables
		for (String var : rv.variables)
		{
			if (!ordered.contains(var))
				ordered.add(var);
		}

		rv.variables = ordered;
	}

	/**
	 * Get the set of labels shared between these components
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	private static Collection<String> getSharedLabels(BaseComponent left, BaseComponent right)
	{
		ArrayList<String> rv = new ArrayList<String>();

		for (String label : left.labels)
		{
			if (right.labels.contains(label))
				rv.add(label);
		}

		return rv;
	}

	/**
	 * Add the (unique) params (constants, variables, labels) to rv
	 * 
	 * @param rv
	 *            the place to store the params
	 * @param from
	 *            the place to take them from
	 */
	private static void mergeParams(Component rv, Component from)
	{
		mergeList(rv.variables, from.variables);
		mergeList(rv.labels, from.labels);
		mergeParamMap(rv.constants, from.constants);
	}

	private static void mergeParamMap(LinkedHashMap<String, Interval> rv,
			LinkedHashMap<String, Interval> from)
	{
		for (Entry<String, Interval> e : from.entrySet())
		{
			String key = e.getKey();
			Interval fromVal = e.getValue();
			Interval rvVal = rv.get(key);

			if (rvVal == null)
				rv.put(key, fromVal);
			else if (!rvVal.equals(fromVal))
				throw new AutomatonExportException(
						"identical constants but different mapped values (diff value)");
		}
	}

	private static void mergeList(ArrayList<String> to, ArrayList<String> from)
	{
		// this is quadratic time... but expected lists are small
		for (String f : from)
		{
			if (!to.contains(f))
				to.add(f);
		}
	}

	/**
	 * And's two expressions, which maybe null
	 */
	private static Expression andExpressions(Expression a, Expression b)
	{
		Expression rv = null;

		if (a == null)
			rv = b;
		else if (b == null)
			rv = a;
		else
			rv = Expression.and(a, b);

		return rv;
	}

	private static void mergeLocations(BaseComponent left, BaseComponent right, BaseComponent rv)
	{
		for (AutomatonMode locI : left.modes.values())
		{
			for (AutomatonMode locJ : right.modes.values())
			{
				String combinedName = locI.name + SEPARATOR + locJ.name; // dots
																			// will
																			// be
																			// eliminated
																			// later

				AutomatonMode merged = rv.createMode(combinedName);
				merged.invariant = andExpressions(locI.invariant, locJ.invariant);

				if (locI.urgent || locJ.urgent)
				{
					merged.urgent = true;
					merged.flowDynamics = null;
				}
				else
				{
					try
					{
						merged.flowDynamics = mergeExpressionMap(locI.flowDynamics,
								locJ.flowDynamics);
					}
					catch (AutomatonExportException e)
					{
						throw new AutomatonExportException(
								"Conflicting dynamics (multiple drivers) detected while "
										+ "flattening automaton",
								e);
					}
				}
			}
		}
	}

	private static LinkedHashMap<String, ExpressionInterval> mergeExpressionMap(
			LinkedHashMap<String, ExpressionInterval> a,
			LinkedHashMap<String, ExpressionInterval> b)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();
		rv.putAll(a);

		for (Entry<String, ExpressionInterval> e : b.entrySet())
		{
			String key = e.getKey();
			ExpressionInterval value = e.getValue();

			if (!rv.containsKey(key))
				rv.put(key, value);
			else
			{
				ExpressionInterval rvValue = rv.get(key);

				if (rvValue == null)
					rv.put(key, value);
				else if (rvValue != null && value != null && !rvValue.equals(value))
					throw new AutomatonExportException("Conflict merging expressions for '" + key
							+ "': '" + value.toDefaultString() + "' and '"
							+ rvValue.toDefaultString() + "'");
			}
		}

		return rv;
	}

	/**
	 * Add transitions from the left base component that don't have a shared label
	 * 
	 * @param rv
	 *            where the add transitions to
	 * @param comp
	 *            which component to add from
	 * @param isLeft
	 *            is this component left-composed in the location names?
	 * @param sharedLabels
	 *            the list of shared labels (transitions with these labels are skipped)
	 */
	private static void addNonSharedTransitions(BaseComponent rv, BaseComponent comp,
			boolean isLeft, Collection<String> sharedLabels)
	{
		for (AutomatonTransition originalT : comp.transitions)
		{
			AutomatonMode fromLoc = originalT.from;
			String fromName = fromLoc.name;
			AutomatonMode toLoc = originalT.to;
			String toName = toLoc.name;

			if (sharedLabels.contains(originalT.label))
				continue;

			for (AutomatonMode locRv : rv.modes.values())
			{
				String locName = locRv.name;
				boolean matchedSource = false;

				if (isLeft)
					matchedSource = locName.startsWith(fromName + SEPARATOR);
				else
					matchedSource = locName.endsWith(SEPARATOR + fromName);

				if (matchedSource)
				{
					String targetName = null;

					if (isLeft)
						targetName = toName + SEPARATOR + locName.substring(fromName.length() + 1);
					else
					{
						int restStringLength = locName.length() - (fromName.length() + 1);
						targetName = locName.substring(0, restStringLength) + SEPARATOR + toName;
					}

					for (AutomatonMode locRv2 : rv.modes.values())
					{
						String locName2 = locRv2.name;

						if (locName2.equals(targetName))
						{
							Hyst.logDebug(
									"Adding transition '" + locName + "' -> '" + locName2 + "'");

							// add transition between locRv and locRv2
							AutomatonTransition at = rv.createTransition(locRv, locRv2);

							at.guard = originalT.guard.copy();
							at.reset = copyMap(originalT.reset);
							at.label = originalT.label;
						}
					}
				}
			}
		}
	}

	private static LinkedHashMap<String, ExpressionInterval> copyMap(
			LinkedHashMap<String, ExpressionInterval> from)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();

		for (Entry<String, ExpressionInterval> e : from.entrySet())
			rv.put(e.getKey(), e.getValue().copy());

		return rv;
	}

	/**
	 * Add transitions which are shared between components with labels
	 * 
	 * @param rv
	 *            where the add the transitions to
	 * @param left
	 *            the left-composed component
	 * @param right
	 *            the right-composed component
	 * @param sharedLabels
	 *            the labels shared between components
	 */
	private static void addSharedTransitions(BaseComponent rv, BaseComponent left,
			BaseComponent right, Collection<String> sharedLabels)
	{
		for (AutomatonTransition leftT : left.transitions)
		{
			String label = leftT.label;

			if (label == null || !sharedLabels.contains(label))
				continue;

			AutomatonMode fromLeftLoc = leftT.from;
			String fromLeftName = fromLeftLoc.name;
			AutomatonMode toLeftLoc = leftT.to;
			String toLeftName = toLeftLoc.name;

			// find all matching right transitions
			for (AutomatonTransition rightT : right.transitions)
			{
				if (!label.equals(rightT.label))
					continue;

				// at this point labels match and we can determine the full
				// source and target names
				AutomatonMode fromRightLoc = rightT.from;
				String fromRightName = fromRightLoc.name;
				AutomatonMode toRightLoc = rightT.to;
				String toRightName = toRightLoc.name;

				String sourceName = fromLeftName + SEPARATOR + fromRightName;
				String targetName = toLeftName + SEPARATOR + toRightName;

				AutomatonMode sourceMode = rv.modes.get(sourceName);

				if (sourceMode == null)
					throw new AutomatonExportException(
							"source name not found in modes: " + sourceName);

				AutomatonMode targetMode = rv.modes.get(targetName);

				if (targetMode == null)
					throw new AutomatonExportException(
							"target name not found in modes: " + targetName);

				AutomatonTransition at = rv.createTransition(sourceMode, targetMode);
				at.guard = andExpressions(leftT.guard, rightT.guard);
				at.label = label;

				try
				{
					at.reset = mergeExpressionMap(leftT.reset, rightT.reset);
				}
				catch (AutomatonExportException e)
				{
					throw new AutomatonExportException(
							"Conflicting reset (multiple drivers) detected while "
									+ "flattening automaton",
							e);
				}
			}
		}
	}

	/**
	 * Merge the (instantiated) components into one
	 * 
	 * @param left
	 *            the first component
	 * @param right
	 *            the second component
	 * @return the merged component
	 */
	private static BaseComponent mergeComponents(BaseComponent left, BaseComponent right)
	{
		BaseComponent rv = new BaseComponent();

		Collection<String> sharedLabels = getSharedLabels(left, right);

		mergeParams(rv, left);
		mergeParams(rv, right);

		// merge the automata by first constructing the cross product of the
		// mode names
		Hyst.logDebug("Merging Locations, shared labels: " + sharedLabels);
		mergeLocations(left, right, rv);

		// add every transition in left base component without a label
		Hyst.logDebug("Adding Non-shared Transitions Left");
		addNonSharedTransitions(rv, left, true, sharedLabels);

		// add every transition in right base component without a label
		Hyst.logDebug("Adding Non-shared Transitions Right");
		addNonSharedTransitions(rv, right, false, sharedLabels);

		// add labeled transitions
		Hyst.logDebug("Adding Labeled Transitions");
		addSharedTransitions(rv, left, right, sharedLabels);

		// merge the id using a '_' as the separator
		Hyst.logDebug("Setting Id");
		rv.instanceName = left.instanceName + SEPARATOR + right.instanceName;

		return rv;
	}
}

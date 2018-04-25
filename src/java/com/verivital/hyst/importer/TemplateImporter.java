package com.verivital.hyst.importer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.AutomatonUtil;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.BindMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Location;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Param;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamDynamics;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamType;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExBaseComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Transition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ValueMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.VariableParam;

/**
 * This class converts a SpaceEx document into a set of template automata
 *
 */
public class TemplateImporter
{
	// prefix used for value maps (hardcoded mappings to constants) inside
	// template automata
	// this may exist in models when Component.VALIDATE_CONSTS is false
	public static final String TEMPLATE_VALUE_MAP_PREFIX = "~";

	/**
	 * Create an instance of every base component type declared in the SpaceEx document. These are
	 * essentially templates that get copied when the network is instantiated.
	 * 
	 * @param doc
	 *            the SpaceEx document to create them from
	 * @return a list of base component templates
	 */
	public static Map<String, Component> createComponentTemplates(SpaceExDocument doc)
	{
		HashMap<String, Component> templates = new HashMap<String, Component>();

		for (int i = 0; i < doc.getComponentCount(); ++i)
		{
			SpaceExComponent c = doc.getComponent(i);

			templates.put(c.getID(), instantiateTemplate(c, templates, doc));
		}

		return templates;
	}

	/**
	 * Instantiate a template from a spaceex component, and add it to the template list
	 * 
	 * @param comp
	 *            the spaceex component to make a template for
	 * @param templates
	 *            the list of templates to add it to
	 */
	private static Component instantiateTemplate(SpaceExComponent comp,
			HashMap<String, Component> templates, SpaceExDocument doc)
	{
		String name = comp.getID();

		Component rv = templates.get(name);

		if (rv == null)
		{
			if (comp instanceof SpaceExBaseComponent)
				rv = instantiateBaseTemplate((SpaceExBaseComponent) comp);
			else
				rv = instantiateNetworkTemplate((SpaceExNetworkComponent) comp, templates, doc);

			rv.instanceName = name;
			rv.template = rv; // template components are their own templates

			templates.put(name, rv);
		}

		Hyst.logDebug("Instantiated template '" + name + "' as:\n" + rv);

		return rv;
	}

	private static Component instantiateBaseTemplate(SpaceExBaseComponent c)
	{
		BaseComponent rv = new BaseComponent();

		// params
		convertParams(rv, c);
		convertLocations(rv, c);
		convertTransitions(rv, c);

		return rv;
	}

	private static Component instantiateNetworkTemplate(SpaceExNetworkComponent n,
			HashMap<String, Component> templates, SpaceExDocument doc)
	{
		NetworkComponent rv = new NetworkComponent();

		convertParams(rv, n);

		// network component don't contain the children instances, just the
		// names and the mapping
		for (int i = 0; i < n.getBindCount(); ++i)
		{
			Bind b = n.getBind(i);

			String childComponentName = b.getComponent();
			Component child = instantiateTemplate(doc.getComponent(childComponentName), templates,
					doc);

			ComponentInstance ci = new ComponentInstance(rv, child);
			rv.children.put(b.getAs(), ci);

			for (int m = 0; m < b.getMapCount(); ++m)
			{
				BindMap bm = b.getMap(m);

				// we have to figure out what type we're mapping to
				String childParam = bm.getKey();
				String parentParam = null;

				if (bm instanceof ParamMap)
					parentParam = ((ParamMap) bm).getParamReference();
				else
				{
					// value maps will get removed later (and added as an
					// assignment in the initial states)
					// For now map to a name equal to: ~<realValue>
					parentParam = TEMPLATE_VALUE_MAP_PREFIX + ((ValueMap) bm).getRealValue(0);
				}

				Param p = getMappedParam(childComponentName, childParam, doc);

				if (p.getType() == ParamType.LABEL)
					ci.labelMapping.add(new ComponentMapping(childParam, parentParam));
				else if (p.getType() == ParamType.REAL)
				{
					VariableParam vp = (VariableParam) p;

					if (vp.getDynamics() == ParamDynamics.ANY)
						ci.varMapping.add(new ComponentMapping(childParam, parentParam));
					else if (vp.getDynamics() == ParamDynamics.CONST)
						ci.constMapping.add(new ComponentMapping(childParam, parentParam));
					else
						throw new AutomatonExportException(
								"unsupported dynamics type: " + vp.getDynamics());
				}
				else
					throw new AutomatonExportException("unsupported param type: " + p.getType());
			}
		}

		return rv;
	}

	/**
	 * Get the parameter being mapped to (label, const, or variable). This isn't present in the bind
	 * map, but instead you need to look at the params of the child component
	 * 
	 * @param componentName
	 * @param paramName
	 * @param doc
	 * @return
	 */
	private static Param getMappedParam(String componentName, String paramName, SpaceExDocument doc)
	{
		SpaceExComponent c = doc.getComponent(componentName);

		if (c == null)
			throw new AutomatonExportException(
					"Bind in SX file with undefined component type: " + componentName);

		Param p = c.getParam(paramName);

		if (p == null)
			throw new AutomatonExportException(
					"Bind in SX file with param that doesn't exist in component. Param: "
							+ paramName + "; Component: " + componentName);

		return p;
	}

	/**
	 * Convert the transitions of a SpaceExBaseComponent to a IR Base Component
	 * 
	 * @param rv
	 *            the IR base component to assign to
	 * @param c
	 *            the SpaceEx Base Component to extract from
	 */
	private static void convertTransitions(BaseComponent rv, SpaceExBaseComponent c)
	{
		for (int i = 0; i < c.getLocationCount(); i++)
		{
			Location loc = c.getLocation(i);
			String fromName = loc.getName();

			int locId = loc.getId();

			for (int j = 0; j < c.getTransitionCount(); j++)
			{
				Transition trans = c.getTransition(j);

				if (trans.getSource() != locId)
					continue;

				if (trans.isAsap())
					throw new AutomatonExportException(
							"explicit asap transitions not currently supported");

				if (trans.isTimeDriven())
					throw new AutomatonExportException(
							"explicit time-driven transitions not currently supported");

				int transTarget = trans.getTarget();
				Location targetLoc = c.getLocationByID(transTarget);

				String toName = targetLoc.getName();

				AutomatonMode fromMode = rv.modes.get(fromName);

				if (fromMode == null)
					throw new AutomatonExportException(
							"source mode in transition doesn't exist in automaton:" + fromName);

				AutomatonMode toMode = rv.modes.get(toName);

				if (toMode == null)
					throw new AutomatonExportException(
							"destination mode in transition doesn't exist in automaton:" + toName);

				AutomatonTransition t = rv.createTransition(fromMode, toMode);

				t.guard = trans.getGuard() == null ? Constant.TRUE : trans.getGuard();
				t.reset = AutomatonUtil.extractReset(trans.getAssignment(), rv.variables);
				t.label = trans.getLabel();
			}
		}
	}

	/**
	 * Convert the modes of a SpaceExBaseComponent to a IR Base Component
	 * 
	 * @param rv
	 *            the IR base component to assign to
	 * @param c
	 *            the SpaceEx Base Component to extract from
	 */
	private static void convertLocations(BaseComponent rv, SpaceExBaseComponent c)
	{
		for (int i = 0; i < c.getLocationCount(); i++)
		{
			Location loc = c.getLocation(i);
			String locName = loc.getName();

			AutomatonMode mode = rv.createMode(locName);

			Expression flow = loc.getFlow();

			if (flow == Constant.FALSE || rv.variables.size() == 0)
			{
				mode.flowDynamics = null;
				mode.urgent = true;
			}
			else
			{
				mode.flowDynamics = new LinkedHashMap<String, ExpressionInterval>();

				// flow may be null if dynamics are not defined in this
				// sub-automaton
				if (flow != null)
				{
					for (Entry<String, Expression> e : AutomatonUtil
							.parseFlowExpression(rv.variables, flow).entrySet())
						mode.flowDynamics.put(e.getKey(), new ExpressionInterval(e.getValue()));
				}
			}

			mode.invariant = loc.getInvariant();

			// no invariant = true in spaceex
			if (mode.invariant == null)
				mode.invariant = Constant.TRUE;
		}
	}

	/**
	 * Convert the Params of a SpaceExBaseComponent to a IR Base Component
	 * 
	 * @param rv
	 *            the IR base component to assign to
	 * @param c
	 *            the SpaceEx Base Component to extract from
	 */
	private static void convertParams(Component rv, SpaceExComponent c)
	{
		for (int i = 0; i < c.getParamCount(); ++i)
		{
			Param p = c.getParam(i);
			String name = p.getName();

			if (p instanceof VariableParam)
			{
				VariableParam vp = (VariableParam) p;

				if (vp.getDimensionSize(1) != 1 || vp.getDimensionSize(2) != 1)
					throw new AutomatonExportException(
							"Unsupported dimension size (not 1) for param: " + vp.getName());

				if (vp.getDynamics() == ParamDynamics.CONST)
					rv.constants.put(name, null);
				else if (vp.getDynamics() == ParamDynamics.ANY)
					rv.variables.add(name);
				else
					throw new AutomatonExportException(
							"Unsupported dynamics type for param: " + vp.getName());
			}
			else
				rv.labels.add(name);
		}
	}

}

package com.verivital.hyst.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * Takes a set of template automata, and can instantiate them into configurations.
 *
 */
public class ConfigurationMaker
{
	/**
	 * Create a Configuration from a SpaceEx doc and a template list of all the base components
	 * 
	 * @param doc
	 *            the SpaceEx doc
	 * @param templates
	 *            the map of template names -> template automaon, from
	 *            ComponentImporter.createComponentTemplates()
	 * @return a Configuration which is the networked automaton
	 */
	public static Configuration fromSpaceEx(SpaceExDocument doc, Map<String, Component> templates)
	{
		String rootName = getRootName(doc);

		Component c = instantiateComponentFromTemplate(rootName, null, templates);
		Configuration rv = new Configuration(c);
		SymbolicStateExpression.setComponent(doc.getComponent(rootName), doc);

		// import settings and such
		Expression init = doc.getInitialStateConditions();
		init = SimplifyExpressionsPass.simplifyExpression(init);
		convertInitialStates(rv, init);

		Expression forbidden = doc.getForbiddenStateConditions();

		if (forbidden != null)
		{
			forbidden = SimplifyExpressionsPass.simplifyExpression(forbidden);
			convertForbiddenStates(rv, forbidden);
		}

		convertPlotSettings(rv, doc);
		rv.settings.spaceExConfig = doc.getConfig();

		rv.validate();

		return rv;
	}

	private static Component instantiateComponentFromTemplate(String templateName,
			String instanceName, Map<String, Component> templates)
	{
		Component template = templates.get(templateName);

		if (template == null)
			throw new AutomatonExportException("Component not found in templates: " + templateName);

		// deep copy
		Component rv = template.copy();
		rv.instanceName = instanceName;

		// don't do const validation here, since full automaton is not yet
		// constructed
		Component.VALIDATE_CONSTS = false;
		rv.validate();
		Component.VALIDATE_CONSTS = true;

		return rv;
	}

	private static void convertPlotSettings(Configuration c, SpaceExDocument doc)
	{
		Collection<String> allVars = c.root.getAllVariables();

		for (String name : allVars)
		{
			// first pass, just plot the first two variabes
			String smallName = name.toLowerCase();

			if (smallName.equals("t") || smallName.equals("time"))
			{
				// time should always be the x axis
				c.settings.plotVariableNames[1] = c.settings.plotVariableNames[0];
				c.settings.plotVariableNames[0] = name;
			}
			else if (c.settings.plotVariableNames[0] == null)
				c.settings.plotVariableNames[0] = name;
			else if (c.settings.plotVariableNames[1] == null)
				c.settings.plotVariableNames[1] = name;
		}

		// second pass, check document for plot variables
		if (c.settings.plotVariableNames[0] != null)
		{
			// 1-d models aren't technically illegal
			if (c.settings.plotVariableNames[1] == null)
				c.settings.plotVariableNames[1] = c.settings.plotVariableNames[0];

			// override any detected settings if they are manually specified
			ArrayList<String> vars = doc.getConfig().outputVars;

			String var0 = vars.size() > 0 ? vars.get(0) : null;
			String var1 = vars.size() > 1 ? vars.get(1) : null;

			if (var0 != null)
				c.settings.plotVariableNames[0] = c.settings.plotVariableNames[1] = var0;

			if (var1 != null)
				c.settings.plotVariableNames[1] = var1;

			for (String var : c.settings.plotVariableNames)
			{
				if (!allVars.contains(var))
					throw new AutomatonExportException(
							"Automaton does not contain the plot variable: " + var);
			}
		}
	}

	/**
	 * Create the initial states in the configuration based on the init expression in the config
	 * file. This may require doing range extraction from the init expression. If the initial value
	 * is a range (parameter), rather than a constant, a variable is inserted with dynamics equal to
	 * zero and initial value equal to that range.
	 * 
	 * @param c
	 *            the configuration to assign initial states to
	 * @param init
	 *            the init expression in the config file
	 */
	private static void convertInitialStates(Configuration c, Expression init)
	{
		// some of the initial values get assigned from the init expression, not
		// in the automaton
		ArrayList<String> unassignedConstants = new ArrayList<String>();

		for (Entry<String, Interval> e : c.root.getAllConstants().entrySet())
		{
			if (e.getValue() == null)
				unassignedConstants.add(e.getKey());
		}

		if (init == null)
		{
			Hyst.logError("Initial states not defined. Using default values.");

			Expression e = null;

			for (String v : c.root.getAllVariables())
			{
				Expression assignment = new Operation(Operator.EQUAL, new Variable(v),
						new Constant(0));
				e = (e == null ? assignment : Expression.and(e, assignment));
			}

			for (String name : unassignedConstants)
				c.root.setConstant(name, new Interval(0));

			if (e != null)
			{
				String modeName = getFirstModeName(c.root);

				Hyst.log("Using initial mode '" + modeName + " ' with initial variable assignment: "
						+ e.toDefaultString());
				c.init.put(modeName, e);
				init = e;
			}
			else
				throw new AutomatonExportException("No variables in model root component.");
		}
		else
		{
			// every constant which isn't assigned in the model must be assigned
			// in the init expression
			for (String var : unassignedConstants)
			{
				Interval value = null;

				try
				{
					value = RangeExtractor.getVariableRange(init, var);
				}
				catch (EmptyRangeException e)
				{
					throw new AutomatonExportException("Error getting initial value of constant "
							+ var + " in init expression (empty range): " + init.toDefaultString(),
							e);
				}
				catch (ConstantMismatchException e)
				{
					throw new AutomatonExportException("Error getting initial value of constant "
							+ var + " in init expression (contradicting constants): "
							+ init.toDefaultString(), e);
				}
				catch (UnsupportedConditionException e)
				{
					throw new AutomatonExportException("Initial value of constant " + var
							+ " in init expression is NOT an interval: " + init.toDefaultString(),
							e);
				}

				if (value == null)
					throw new AutomatonExportException(
							"Could not find initial value for constant in config: " + var);

				c.root.setConstant(var, value);
			}

			insertIntoLocMap(init, c.root, c.init, "initial states");
		}
	}

	/**
	 * Get the (dotted) name of the first mode in this component. Maybe have to look at
	 * subcomponents.
	 * 
	 * @param c
	 * @return
	 */
	private static String getFirstModeName(Component c)
	{
		String rv = null;

		if (c instanceof BaseComponent)
			rv = ((BaseComponent) c).modes.keySet().iterator().next();
		else
		{
			NetworkComponent nc = (NetworkComponent) c;

			for (Entry<String, ComponentInstance> e : nc.children.entrySet())
			{
				String childMode = getFirstModeName(e.getValue().child);
				rv = (rv == null) ? childMode : (rv + "." + childMode);
			}
		}

		return rv;
	}

	/**
	 * Create the forbidden states in the configuration based on the forbidden expression in the
	 * config file.
	 * 
	 * @param c
	 *            the configuration to assign to
	 * @param f
	 *            the forbidden state expression (can be null)
	 */
	private static void convertForbiddenStates(Configuration c, Expression f)
	{
		if (f != null)
			insertIntoLocMap(f, c.root, c.forbidden, "forbidden states");
	}

	private static String getRootName(SpaceExDocument doc)
	{
		String rootName = doc.getConfig().systemID;

		if (doc.getComponentCount() == 0)
			throw new AutomatonExportException("SpaceExDocument did not contain any components!");

		if (rootName == null)
			rootName = doc.getComponent(0).getID();

		if (doc.getComponent(rootName) == null)
			throw new AutomatonExportException(
					"SpaceExDocument did not contain the desired root component: " + rootName);

		return rootName;
	}

	/**
	 * Parse the passed in expression into a location-map (initial or forbidden states)
	 * 
	 * @param e
	 *            the expression to parse
	 * @param root
	 *            the root component (for error checking if modes exist)
	 * @param map
	 *            [out] the place to store the result
	 * @param disc
	 *            a text description of the set of states being parsed (like "initial states")
	 */
	private static void insertIntoLocMap(Expression e, Component root,
			LinkedHashMap<String, Expression> map, String desc)
	{
		List<SymbolicStateExpression> startStates = SymbolicStateExpression.extractSymbolicStates(e,
				desc);

		for (SymbolicStateExpression ss : startStates)
			ss.exportStates(map);

		// validate modes
		for (String mode : map.keySet())
		{
			if (!AutomatonUtil.modeExistsInComponent(mode, root))
				throw new AutomatonExportException(
						"Discrete mode '" + mode + "' from " + desc + " not found in input model");
		}
	}
}

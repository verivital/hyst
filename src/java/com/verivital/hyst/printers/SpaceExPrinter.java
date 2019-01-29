/**
 * 
 */
package com.verivital.hyst.printers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.PreconditionsFlag;

import de.uni_freiburg.informatik.swt.spaceexxmlprinter.SpaceExXMLPrinter;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Location;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Param;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamDynamics;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamType;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExBaseComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Transition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.VariableParam;

/**
 * Takes a hybrid automaton from the internal model format and outputs a SpaceEx model. Based on
 * Chris' Boogie printer.
 * 
 * @author Stanley Bak (8-2014)
 * @author Taylor Johnson (11-2014)
 *
 */
public class SpaceExPrinter extends ToolPrinter
{
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	@Option(name = "-step", usage = "sampling time step", metaVar = "VAL")
	String step = "auto";

	@Option(name = "-scenario", usage = "spaceex solver scenario", metaVar = "VAL")
	public String scenario = "auto";

	@Option(name = "-output-format", usage = "spaceex output format", metaVar = "VAL")
	String outputFormat = "auto";

	@Option(name = "-iter-max", usage = "maximum number of jumps", metaVar = "VAL")
	String iterMax = "auto";

	@Option(name = "-directions", usage = "support function directions", metaVar = "VAL")
	String directions = "auto";

	@Option(name = "-aggregation", usage = "aggregation parameter", metaVar = "VAL")
	String aggregation = "auto";

	@Option(name = "-flowpipe_tol", usage = "flowpipe-tolerance parameter (0 = skip)", metaVar = "VAL")
	String flowpipeTol = "auto";

	@Option(name = "-skiptol", usage = "skip printing error tolerances")
	boolean skipTol = false;

	@Option(name = "-time_triggered", usage = "sets map-zero-duration-jump-sets=true")
	boolean isTimeTriggered = false;

	@Option(name = "-output_vars", usage = "comma-separated output variables", metaVar = "VAL")
	String outputVars = "auto";

	private String cfgFilename = null;
	private BaseComponent ha;

	public SpaceExPrinter()
	{
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN.ordinal()] = true;
	}

	/**
	 * set baseName
	 */
	public void setBaseName(String name)
	{
		super.setBaseName(name);
		// Replace all special characters because SpaceEx' configuration parser doesn't like
		// dashes in automata names.
		baseName = baseName.replaceAll("[^0-9a-zA-Z]", "_");
	}

	/**
	 * set basecomponent
	 */
	public void setBaseComponent(BaseComponent component)
	{
		ha = component;
	}

	/**
	 * map from mode string names to numeric ids, starting from 1 and incremented
	 */
	private TreeMap<String, Integer> modeNamesToIds = new TreeMap<String, Integer>();

	private void makeConfig()
	{
		if (outputType != OutputType.FILE)
		{
			cfgFilename = null;
		}
		else
		{
			if (outputFilename.endsWith(".xml"))
				cfgFilename = outputFilename.substring(0, outputFilename.length() - 4) + ".cfg";
			else
				cfgFilename = outputFilename + ".cfg";
		}
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls printProcedure() to
	 * print the BPL code
	 */
	private void printDocument(String originalFilename)
	{
		makeConfig();

		String note = this.getCommentHeader();

		Expression.expressionPrinter = new SpaceExExpressionPrinter();

		SpaceExDocument doc = null;

		try
		{
			doc = convert(ha);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing number param for config: " + e, e);
		}

		doc.getComponent(0).setNote(note);

		SpaceExXMLPrinter spaceex_printer = new SpaceExXMLPrinter(doc);
		printLine(spaceex_printer.stringXML());

		if (cfgFilename != null)
		{
			try
			{
				Writer w = new BufferedWriter(new FileWriter(new File(cfgFilename)));
				w.write(spaceex_printer.getCFGString(skipTol));
				w.close();
			}
			catch (IOException e)
			{
				throw new AutomatonExportException("Error writing to cfg file.", e);
			}
		}
		else
		{
			// it will get printed to stdout
			printLine("\n" + spaceex_printer.getCFGString(skipTol));
		}

	}

	/**
	 * Convert hybrid automaton in Hyst internal representation to SpaceEx representation
	 * 
	 * @param ha
	 * @return
	 */
	public SpaceExDocument convert(BaseComponent ha) throws NumberFormatException
	{
		ha.validate();

		// iterate over this.modes, etc.
		SpaceExDocument sed = new SpaceExDocument();
		sed.setVersion("0.2");
		sed.setMathFormat("SpaceEx");
		sed.setTimeTriggered(isTimeTriggered || config.settings.spaceExConfig.timeTriggered);
		// sed.setTimeHorizon(-1);
		sed.setMaxIterations(
				Integer.parseInt((getParam(iterMax, config.settings.spaceExConfig.maxIterations))));

		String format = getParam(outputFormat, config.settings.spaceExConfig.outputFormat);
		sed.setOutputFormat(format);

		String dirs = getParam(directions, config.settings.spaceExConfig.directions);
		sed.setDirections(dirs);

		double flowpipeTol = Double
				.parseDouble(getParam(this.flowpipeTol, config.settings.spaceExConfig.flowpipeTol));
		sed.setFlowpipeTolerance(flowpipeTol);

		String agg = getParam(aggregation, config.settings.spaceExConfig.aggregation);
		sed.setAggregation(agg);

		String scenario = getParam(this.scenario, config.settings.spaceExConfig.scenario);
		sed.setScenario(scenario);

		if (!scenario.equals("phaver"))
		{
			// convert explicit urgent flows
			AutomatonUtil.convertUrgentTransitions(ha, config);
		}

		sed.setSamplingTime(
				Double.parseDouble(getParam(step, config.settings.spaceExConfig.samplingTime)));
		sed.setTimeHorizon(
				Double.parseDouble(getParam(time, config.settings.spaceExConfig.timeHorizon)));

		SpaceExBaseComponent base = new SpaceExBaseComponent(sed);
		base.setID(baseName + "_sys");
		// base.setID()

		int id = 1;

		for (String modeName : ha.modes.keySet())
		{
			modeNamesToIds.put(modeName, id++);
		}

		// add variables' names
		for (String v : ha.variables)
		{
			Param p = new VariableParam(base); // note: this also adds p to base
												// in the constructor
			p.setName(v);
		}

		// add constants
		for (String v : ha.constants.keySet())
		{
			// this also adds p to base in the constructor
			new VariableParam(base, v, ParamType.REAL, ParamDynamics.CONST);
		}

		// add interval variables from nondeterminism
		Collection<String> intervalVars = getAllVariablesWithNondeterministicFlows();

		for (String var : intervalVars)
		{
			String newVar = getIntervalVariableName(var);

			ha.variables.add(newVar);
			VariableParam p = new VariableParam(base); // note: this also adds p
														// to base in the
														// constructor
			p.setName(newVar);
			p.setControlled(false); // because it can change to any value which
									// satisfies the invariant

			// also, anywhere the interval is not assigned we want to set it to
			// 0 (since it's uncontrolled)
			for (AutomatonMode am : ha.modes.values())
			{
				if (am.flowDynamics == null)
					continue;

				ExpressionInterval ei = am.flowDynamics.get(var);

				if (ei.getInterval() == null)
					ei.setInterval(new Interval(0));
			}
		}

		/**
		 * iterate over automaton modes to convert flow dynamics, invariant
		 */
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			Location loc = new Location(base);
			String name = e.getKey();
			loc.setId(modeNamesToIds.get(name));
			loc.setName(e.getKey());

			AutomatonMode mode = e.getValue();

			// set flow dynamics
			if (mode.urgent)
				loc.setFlow(Constant.FALSE); // phaver only, other scenarios
												// will have converted already
			else
				loc.setFlow(flowDynamicsToExpression(mode.flowDynamics));

			// set invariant
			Expression inv = mode.invariant;

			// additionally set invariant for nondeterministic flow variables
			Map<String, Interval> varInts = getVariableIntervals(mode.flowDynamics);

			for (Entry<String, Interval> entry : varInts.entrySet())
			{
				String intervalVar = getIntervalVariableName(entry.getKey());
				Interval i = entry.getValue();
				Operation bounds;

				if (i.isConstant())
					bounds = new Operation(Operator.EQUAL, intervalVar, i.middle());
				else
				{
					Operation topBound = new Operation(Operator.LESSEQUAL, intervalVar, i.max);
					Operation bottomBound = new Operation(Operator.GREATEREQUAL, intervalVar,
							i.min);
					bounds = new Operation(Operator.AND, topBound, bottomBound);
				}

				inv = new Operation(Operator.AND, inv, bounds);
			}

			loc.setInvariant(inv);
		}

		for (AutomatonTransition t : ha.transitions)
		{
			Transition spaceex_t = new Transition(base);

			if (!t.reset.isEmpty())
			{
				Expression e = makeResetExpression(t.reset);
				spaceex_t.setAssignment(e);
			}

			spaceex_t.setGuard(t.guard);

			spaceex_t.setLabel("");
			spaceex_t.setSource(modeNamesToIds.get(t.from.name));
			spaceex_t.setTarget(modeNamesToIds.get(t.to.name));
		}

		// Network component

		SpaceExNetworkComponent net = new SpaceExNetworkComponent(sed);
		Bind bind = new Bind(net);

		for (String v : ha.variables)
		{
			// these get added to the parent in the constructor
			new ParamMap(bind, v, v);
			new VariableParam(net, v, ParamType.REAL, ParamDynamics.ANY, false);
		}

		for (String c : ha.constants.keySet())
		{
			// ParamMap constMap = new ParamMap(bind, c ,
			// String.valueOf(ha.constants.get(c)));
			new ParamMap(bind, c, c);

			new VariableParam(net, c, ParamType.REAL, ParamDynamics.CONST, false);
		}

		sed.setSystemID(baseName + "_net");
		bind.setAs(baseName);
		bind.setComponent(baseName + "_sys");
		net.setID(baseName + "_net");
		/**
		 * 
		 */
		/**
		 * /set initial condition
		 */
		Expression initialState = initialForbiddenStateExpression(config.init);
		sed.setInitialStateConditions(initialState);

		/**
		 * /set forbidden condition
		 */
		if (config.forbidden.entrySet().iterator().hasNext())
		{
			Expression ForbiddenState = initialForbiddenStateExpression(config.forbidden);
			sed.setForbiddenStateConditions(ForbiddenState);
		}

		// add output variables
		if (outputVars.equals("auto"))
		{
			for (String v : config.settings.plotVariableNames)
				sed.addOutputVar(v);
		}
		else
		{
			for (String var : outputVars.split(","))
				sed.addOutputVar(var);
		}

		return sed;
	}

	private String getParam(String val, String defVal)
	{
		return val.equals("auto") ? defVal : val;
	}

	private String getParam(String val, int defVal)
	{
		return val.equals("auto") ? "" + defVal : val;
	}

	private String getParam(String val, double defVal)
	{
		return val.equals("auto") ? "" + defVal : val;
	}

	/**
	 * Get the names of every variable that has an interval flow
	 * 
	 * @return a list of names of variables
	 */
	private Collection<String> getAllVariablesWithNondeterministicFlows()
	{
		HashSet<String> rv = new HashSet<String>();

		for (AutomatonMode am : ha.modes.values())
		{
			Map<String, Interval> varInts = getVariableIntervals(am.flowDynamics);

			rv.addAll(varInts.keySet());
		}

		return rv;
	}

	/**
	 * Get the interval parts of the flows for each variable. May return a map of size 0.
	 * 
	 * @param flowDynamics
	 *            the flow dynamics to check
	 * @return a mapping of variable name -> interval part of flow for all nondeterministic flows
	 */
	private Map<String, Interval> getVariableIntervals(
			LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		LinkedHashMap<String, Interval> rv = new LinkedHashMap<String, Interval>();

		if (flowDynamics != null)
		{
			for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
			{
				Interval i = e.getValue().getInterval();

				if (i != null)
					rv.put(e.getKey(), i);
			}
		}

		return rv;
	}

	/**
	 * Get the name of the associated interval variable (for nondeterministic flows)
	 * 
	 * @param varName
	 *            the variable name
	 * @return the interval variable name
	 */
	private static String getIntervalVariableName(String varName)
	{
		return "_" + varName + "_interval";
	}

	/**
	 * Convert the reset dictionary to a single expression using primed variables
	 * 
	 * @return
	 */
	public Expression makeResetExpression(LinkedHashMap<String, ExpressionInterval> reset)
	{
		Expression rv = null;

		for (Entry<String, ExpressionInterval> e : reset.entrySet())
		{
			Expression part = null;
			ExpressionInterval ei = e.getValue();

			if (ei.isNondeterministicAssignment())
				part = makeNondeterministicAssignment(e.getKey());
			else if (ei.isInterval())
			{
				Interval i = ei.getInterval();

				Expression lessExp = new Operation(Operator.LESSEQUAL, new Variable(e.getKey()),
						new Constant(i.max));
				Expression moreExp = new Operation(Operator.GREATEREQUAL, new Variable(e.getKey()),
						new Constant(i.min));

				part = new Operation(Operator.AND, lessExp, moreExp);
			}
			else
				part = new Operation(Operator.EQUAL, new Variable(e.getKey()), ei.asExpression());

			if (rv == null)
				rv = part;
			else
				rv = new Operation(Operator.AND, rv, part);
		}

		return rv;
	}

	private Expression makeNondeterministicAssignment(String varName)
	{
		// 0.0 * variableName = 0.0

		Operation rv = new Operation(Operator.EQUAL);

		Operation left = new Operation(Operator.MULTIPLY);
		left.children.add(new Constant(0));
		left.children.add(new Variable(varName));

		Constant right = new Constant(0);

		rv.children.add(left);
		rv.children.add(right);

		return rv;
	}

	public static Expression flowDynamicsToExpression(Map<String, ExpressionInterval> flowDynamics)
	{
		Expression rv = null;

		for (Entry<String, ExpressionInterval> entry : flowDynamics.entrySet())
		{
			String var = entry.getKey();

			ExpressionInterval ei = entry.getValue();
			Expression assignment = ei.getExpression();
			Interval i = ei.getInterval();

			if (i != null)
			{
				String intVarName = getIntervalVariableName(var);
				assignment = new Operation(Operator.ADD, assignment, new Variable(intVarName));
			}

			Operation op = new Operation(Operator.EQUAL, new Variable(var), assignment);

			if (rv == null)
				rv = op;
			else
			{
				rv = new Operation(Operator.AND, rv, op);
			}
		}

		return rv;
	}

	/**
	 * Convert initial / forbidden states to an expression for use in the cfg
	 * 
	 * @param fromMap
	 *            the map the convert from
	 * @return an expression representation of the map
	 */
	public Expression initialForbiddenStateExpression(LinkedHashMap<String, Expression> fromMap)
	{
		Expression rv = null;

		for (Entry<String, Expression> entry : fromMap.entrySet())
		{
			Expression e;

			if (ha.modes.size() > 1)
			{
				e = new Operation(Operator.LOC, new Variable(baseName));
				e = new Operation(Operator.EQUAL, e, new Variable(entry.getKey()));

				if (entry.getValue() != null && entry.getValue() != Constant.TRUE)
					e = new Operation(Operator.AND, e, entry.getValue());
			}
			else
				e = entry.getValue();

			if (rv == null)
				rv = e;
			else
				rv = new Operation(Operator.OR, rv, e);
		}

		return rv;
	}

	// custom printer for spaceex expressions
	public static class SpaceExExpressionPrinter extends DefaultExpressionPrinter
	{
		public SpaceExExpressionPrinter()
		{
			super();

			opNames.put(Operator.AND, "&");
			opNames.put(Operator.OR, "|");
		}
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;

		// conditionally convert nondeterministic resets
		String scenario = getParam("scenario", config.settings.spaceExConfig.scenario);

		if (!scenario.equals("phaver"))
		{
			Hyst.log("scenario was not phaver, converting nondeterministic resets");
			Preconditions.convertNondeterministicResets(ha); // may create
																// interval
																// constants
		}

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "SpaceEx";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "spaceex";
	}

	@Override
	protected String getCommentPrefix()
	{
		// config file comments are // and /* */, while xml comments are <!--
		// -->
		return getCommentCharacterStart() + getCommentCharacterEnd();
	}

	protected String getCommentCharacterStart()
	{
		return "<!--";
	}

	protected String getCommentCharacterEnd()
	{
		return "-->";
	}

	protected String commentString(String s)
	{
		return this.getCommentCharacterStart() + s + this.getCommentCharacterEnd();
	}

	/**
	 * Comment a block of text using start and end comment markings
	 */
	@Override
	protected void printCommentBlock(String comment)
	{
		printLine(this.getCommentCharacterStart() + " "
				+ comment.replace("\n", "\n" + this.indentation + " ") + "\n"
				+ this.getCommentCharacterEnd());
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public String getExtension()
	{
		return ".xml";
	}
}

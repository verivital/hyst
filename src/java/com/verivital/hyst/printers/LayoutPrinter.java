/**
 *
 */
package com.verivital.hyst.printers;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.LayoutUtil;

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
 * Takes a hybrid automaton from the internal model format and
 * outputs a SpaceEx model. Based on Chris' Boogie printer.
 *
 * @author Stanley Bak (8-2014)
 * @author Taylor Johnson (11-2014)
 *
 */
public class LayoutPrinter extends ToolPrinter
{
	private String cfgFilename = null;
	private BaseComponent ha;

	/**
	 * set baseName
	 */
	public void setBaseName(String name)
	{
		baseName = name;
	}

	/**
	 * map from mode string names to numeric ids, starting from 1 and incremented
	 */
	private TreeMap<String,Integer> modeNamesToIds = new TreeMap<String, Integer>();

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
	 * This method starts the actual printing!
	 * Prepares variables etc. and calls printProcedure() to print the BPL code
	 */
	private void printDocument(String originalFilename)
	{
		makeConfig();

		String note = "Created by " + Hyst.TOOL_NAME + "\n" +
				"Hybrid Automaton in SpaceEx\n" +
				"Converted from file: " + originalFilename + "\n" +
				"Command Line arguments: " + Hyst.programArguments;


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

		LayoutUtil spaceex_printer = new LayoutUtil(doc, ha);
		printLine(spaceex_printer.stringXML());

		if (cfgFilename != null)
		{
			try
			{
				Writer w = new BufferedWriter(new FileWriter(new File(cfgFilename)));
				w.write(spaceex_printer.getCFGString());
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
			printLine("\n" + spaceex_printer.getCFGString());
		}

	}

	/**
	 * Convert hybrid automaton in Hyst internal representation to SpaceEx representation
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
		//sed.setTimeHorizon(-1);
		sed.setMaxIterations(Integer.parseInt((getParam("iter-max", config.settings.spaceExConfig.maxIterations))));

		String format = getParam("output-format", config.settings.spaceExConfig.outputFormat);
		sed.setOutputFormat(format);

		String scenario = getParam("scenario", config.settings.spaceExConfig.scenario);
		sed.setScenario(scenario);

		sed.setSamplingTime(Double.parseDouble(getParam("step", config.settings.spaceExConfig.samplingTime)));
		sed.setTimeHorizon(Double.parseDouble(getParam("time", config.settings.spaceExConfig.timeHorizon)));

		SpaceExBaseComponent base = new SpaceExBaseComponent(sed);
		base.setID(baseName + "_net");
		//base.setID()

		int id = 1;

		for (String modeName : ha.modes.keySet()) {
			modeNamesToIds.put(modeName, id++);
		}

		// add variables' names
		for (String v : ha.variables)
		{
			Param p = new VariableParam(base); // note: this also adds p to base in the constructor
			p.setName(v);
		}

		// add constants
		for (String v : ha.constants.keySet()) {
			Param p = new VariableParam(base, v, ParamType.REAL, ParamDynamics.CONST); // note: this also adds p to base in the constructor
		}

		/**
		 * iterate over automaton modes to convert flow dynamics, invariant
		 */
		for (Entry <String, AutomatonMode> e : ha.modes.entrySet())
		{
			Location loc = new Location(base);
			String name = e.getKey();
			loc.setId(modeNamesToIds.get(name));
			loc.setName(e.getKey());

			AutomatonMode mode = e.getValue();

			// if there are interval expressions, we may need to add new variables for them
			Map<String, Interval> varInts = getVariableIntervals(mode.flowDynamics);

			// create new variables for each of the intervals if they don't exist
			for (String varName : varInts.keySet())
			{
				String intVarName = getIntervalVariableName(varName);

				if (!ha.variables.contains(intVarName))
				{
					ha.variables.add(intVarName);
					Param p = new VariableParam(base); // note: this also adds p to base in the constructor
					p.setName(intVarName);
				}
			}

			// set flow dynamics
			loc.setFlow(flowDynamicsToExpression(mode.flowDynamics));

			// set invariant
			Expression inv = removeUrgentModeAssignments(mode.invariant);

			assert(inv != null);

			// accumulate invariant bounds on the variable intervals (nondeterministic flows)
			for (Entry<String, Interval> entry : varInts.entrySet())
			{
				Variable v = new Variable(getIntervalVariableName(entry.getKey()));
				Interval i = entry.getValue();

				Operation topBound = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
				Operation bottomBound = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
				Operation bounds = new Operation(Operator.AND, topBound, bottomBound);

				inv = new Operation(Operator.AND, inv, bounds);
			}

			loc.setInvariant(inv);
		}

		for (AutomatonTransition t : ha.transitions)
		{
			Transition spaceex_t = new Transition(base);

			if (!t.reset.isEmpty())
			{
				Expression e = removeUrgentModeAssignments(makeResetExpression(t.reset));
				spaceex_t.setAssignment(e);
			}

			spaceex_t.setGuard(removeUrgentModeAssignments(t.guard));

			spaceex_t.setLabel("");
			spaceex_t.setSource( modeNamesToIds.get(t.from.name) );
			spaceex_t.setTarget( modeNamesToIds.get(t.to.name) );
		}

		// Network component

		SpaceExNetworkComponent net = new SpaceExNetworkComponent(sed);
		Bind bind = new Bind(net);

		for (String v : ha.variables)
		{
			ParamMap varMap = new ParamMap(bind, v, v);
			VariableParam varMapNet = new VariableParam(net,v,ParamType.REAL,ParamDynamics.ANY,false);
		}


		for (String c : ha.constants.keySet())
		{
			//ParamMap constMap = new ParamMap(bind, c , String.valueOf(ha.constants.get(c)));
			new ParamMap(bind, c , c);

			new VariableParam(net,c,ParamType.REAL,ParamDynamics.CONST,false);
		}

		sed.setSystemID(baseName + "_sys");
		bind.setAs(baseName);
		bind.setComponent(baseName + "_net");
		net.setID(baseName + "_sys");
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

		// add ouput variables
		for (String v : config.settings.plotVariableNames)
			sed.addOutputVar(v);

		sed.setSystemID(baseName + "_sys");

		return sed;
	}

	/**
	 * Get the interval parts of the flows for each variable. May return a map of size 0
	 * @param flowDynamics the flow dynamics to check
	 * @return a mapping of variable name -> interval part of flow for all nondeterministic flows
	 */
	private Map<String, Interval> getVariableIntervals(LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		LinkedHashMap<String, Interval> rv = new LinkedHashMap<String, Interval>();

		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			Interval i = e.getValue().getInterval();

			if (i != null)
				rv.put(e.getKey(), i);
		}

		return rv;
	}

	/**
	 * Get the name of the associated interval variable (for nondeterministic flows)
	 * @param varName the variable name
	 * @return the interval variable name
	 */
	private static String getIntervalVariableName(String varName)
	{
		return "_" + varName + "_interval";
	}

	/**
	 * Convert the reset dictionary to a single expression using primed variables
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

				Expression lessExp = new Operation(Operator.LESSEQUAL, new Variable(e.getKey()), new Constant(i.max));
				Expression moreExp = new Operation(Operator.GREATEREQUAL, new Variable(e.getKey()), new Constant(i.min));

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

	private Expression removeUrgentModeAssignments(Expression e)
	{
		// TODO hack, delete layout printer eventually
		return e;
	}

	public static Expression flowDynamicsToExpression(Map <String, ExpressionInterval> flowDynamics)
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
	 * @param fromMap the map the convert from
	 * @return an expression representation of the map
	 */
	public Expression initialForbiddenStateExpression(LinkedHashMap <String, Expression> fromMap)
	{
		Expression rv = null;

		for (Entry<String, Expression> entry : fromMap.entrySet())
		{
			Expression e = new Operation(Operator.LOC, new Variable(baseName));
			e = new Operation(Operator.EQUAL, e, new Variable(entry.getKey()));

			if (entry.getValue() != null && entry.getValue() != Constant.TRUE)
				e = new Operation(Operator.AND, e, entry.getValue());

			if (rv == null)
				rv = e;
			else
				rv = new Operation(Operator.OR, rv, e);
		}

		return rv;
	}

	// custom printer for spaceex expressions, TODO
	public static class SpaceExExpressionPrinter extends DefaultExpressionPrinter
	{
		public SpaceExExpressionPrinter()
		{
			super();

			opNames.put(Operator.AND, "&"); // &amp ?
			opNames.put(Operator.OR, "|"); // | ?
		}
		/*
		public String printOperation(Operation o)
		{
			String rv = null;
			
			Operator op = o.op;
			
			// TODO: fix for spaceex, currently expects a mix of infix and prefix

 			
 			// expects a mix of infix and prefix
 			switch (op) 
 			{
 				case MULTIPLY :
 				case DIVIDE :
 				case ADD :
 				case SUBTRACT : {
 					// default
 					rv = "(" + this.printOperation(o) + ")";
 					break;
 				}
 				case EQUAL :
 				case LESS :
 				case GREATER :
 				case LESSEQUAL :
 				case GREATEREQUAL :
 				case NOTEQUAL : {
 					// infix
 					rv = "(" + o.getLeft() + " " + this.opNames.get(op) + " " + o.getRight() + ")";
 					break;
 				}
 				case NEGATIVE:
 					rv = "-" + o.children.get(0);
 					break;
 					
 					// TODO: do we need to override this?
 				default : {
 					rv = "(";
 					
					int i = o.children.size();
					for (Expression e : o.children) {
						if (i == 1) {
							rv += this.print(e);
						}
						else
						{
							rv += this.print(e) + " " + this.opNames.get(op) + " "; // infix
						}
						i--;
					}
					rv += ")";
 					break;
 				}
 			}
			
			return rv;
		}*/
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent)config.root;
		// transform resets to include identity expressions
		//new AddIdentityResetPass().run(ha, null);

		// remove from init and forbidden
		for (Entry<String, Expression> e : config.init.entrySet())
			e.setValue(removeUrgentModeAssignments(e.getValue()));

		for (Entry<String, Expression> e : config.forbidden.entrySet())
			e.setValue(removeUrgentModeAssignments(e.getValue()));

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "Layout";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-layout";
	}

	@Override
	protected String getCommentCharacter()
	{
		// TODO: config file comments are // and /* */, while xml comments are <!-- -->
		return getCommentCharacterStart() + getCommentCharacterEnd();
	}

	protected String getCommentCharacterStart() {
		return "<!--";
	}

	protected String getCommentCharacterEnd() {
		return "-->";
	}

	protected String commentString(String s) {
		return this.getCommentCharacterStart() + s + this.getCommentCharacterEnd();
	}

	/**
	 * Comment a block of text using start and end comment markings
	 */
	@Override
	protected void printCommentblock(String comment)
	{
		printLine(this.getCommentCharacterStart() + " " +
				comment.replace("\n", "\n" + this.indentation + " ") + "\n" + this.getCommentCharacterEnd());
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public Map <String, String> getDefaultParams()
	{
		LinkedHashMap <String, String> params = new LinkedHashMap <String, String>();

		params.put("scenario", "auto");
		params.put("time", "auto");
		params.put("step", "auto");
		params.put("output-format", "auto");
		params.put("iter-max", "auto");

		return params;
	}

	private String getParam(String p, double def)
	{
		String value = toolParams.get(p);

		if (value == null)
			throw new AutomatonExportException("Tool Parameter was null for '" + p + "'");

		if (value.equals("auto"))
			value = doubleToString(def);

		return value;
	}

	private String getParam(String p, String def)
	{
		String value = toolParams.get(p);

		if (value == null)
			throw new AutomatonExportException("Tool Parameter was null for '" + p + "'");

		if (value.equals("auto"))
			value = def;

		return value;
	}
}

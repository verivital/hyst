/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Classification;
import com.verivital.hyst.util.PairStringOptionHandler;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * Printer for Flow* models. Based on Chris' Boogie printer.
 * 
 * @author Stanley Bak (8-2014)
 *
 */
public class FlowstarPrinter extends ToolPrinter
{
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	@Option(name = "-step", usage = "reachability step", metaVar = "MIN-MAX")
	String step = "auto-auto";

	@Option(name = "-rem", usage = "remainder estimate", metaVar = "VAL")
	String remainder = "1e-4";

	@Option(name = "-precondition", usage = "precondition method", metaVar = "VAL")
	String precondition = "auto";

	@Option(name = "-plot", usage = "output plot line in Flow* file (for example 'gnuplot octagon x,y')", metaVar = "VAL")
	String plot = "auto";

	@Option(name = "-orders", usage = "taylor model orders", metaVar = "MIN-MAX")
	String orders = "3-8";

	@Option(name = "-cutoff", usage = "taylor model cutoff", metaVar = "VAL")
	String cutoff = "1e-15";

	@Option(name = "-precision", usage = "numerical precision", metaVar = "VAL")
	String precision = "53";

	private int DEFAULT_MAX_JUMPS = 999999999;

	@Option(name = "-jumps", usage = "maximum jumps", metaVar = "VAL")
	String jumps = "" + DEFAULT_MAX_JUMPS;

	@Option(name = "-print", usage = "print stdout output", metaVar = "VAL")
	String print = "on";

	@Option(name = "-aggregation", usage = "discrete jump successor aggregation method", metaVar = "VAL")
	String aggregation = "parallelotope";

	@Option(name = "-taylor_init", usage = "override the initial states with a taylor mode", metaVar = "MODE TM", handler = PairStringOptionHandler.class)
	public void setTaylorIinit(String[] params) throws CmdLineException
	{
		if (params.length != 2)
			throw new CmdLineException("-taylor_init expected exactly two follow-on arguments");

		taylorInit = new ArrayList<String>();
		taylorInit.add(params[0]);
		taylorInit.add(params[1]);
	}

	List<String> taylorInit = null;

	private BaseComponent ha;

	public FlowstarPrinter()
	{
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS.ordinal()] = true;
	}

	@Override
	protected String getCommentPrefix()
	{
		return "#";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls printProcedure() to
	 * print the BPL code
	 */
	private void printDocument(String originalFilename)
	{
		this.printCommentHeader();

		// begin printing the actual program
		printNewline();
		printProcedure();
	}

	/**
	 * Simplify an expression by substituting constants and then doing math simplification
	 * 
	 * @param e
	 *            the original expression
	 * @return the modified expression
	 */
	private Expression simplifyExpression(Expression ex)
	{
		Expression subbed = SubstituteConstantsPass.substituteConstantsIntoExpression(ha.constants,
				ex);

		return AutomatonUtil.simplifyExpression(subbed);
	}

	/**
	 * Print the actual Flow* code
	 */
	private void printProcedure()
	{
		printLine("hybrid reachability");
		printLine("{");
		printVars();

		printSettings();

		printModes();

		printJumps();
		printInitialStates();

		printLine("}");

		printForbidden();
	}

	private void printForbidden()
	{
		if (config.forbidden.size() > 0)
		{
			printLine("");
			printLine("unsafe set");
			printLine("{");

			for (Entry<String, Expression> e : config.forbidden.entrySet())
				printLine(e.getKey() + " {" + e.getValue() + "}");

			printLine("}");
		}
	}

	private void printSettings()
	{
		printNewline();
		printLine("setting");
		printLine("{");

		String[] step = getStepParam();

		if (step.length == 1 || step.length == 2 && step[0].equals(step[1]))
			printLine("fixed steps " + step[0]);
		else if (step.length == 2)
			printLine("adaptive steps { min " + step[0] + ", max " + step[1] + " }");
		else
			throw new AutomatonExportException(
					"Param 'step' should have one or two entries: " + step);

		printLine("time " + getTimeParam());

		printLine("remainder estimation " + remainder);

		if (precondition.equals("auto"))
		{
			// follow recommendation in 1.2 manual
			if (ha.variables.size() > 3)
				printLine("identity precondition");
			else
				printLine("QR precondition");
		}
		else
			printLine(precondition + " precondition");

		printLine(getPlotParam());

		String[] order = getOrderParam();

		if (order.length == 1 || order.length == 2 && order[0].equals(order[1]))
			printLine("fixed orders " + order[0]);
		else if (order.length == 2)
			printLine("adaptive orders { min " + order[0] + ", max " + order[1] + " } ");
		else
			throw new AutomatonExportException(
					"Param 'orders' should have one or two entries: " + orders);

		printLine("cutoff " + cutoff);
		printLine("precision " + precision);
		printLine("output out");

		int jumps = Integer.parseInt(this.jumps);

		if (jumps == DEFAULT_MAX_JUMPS && config.settings.spaceExConfig.maxIterations > 0)
			jumps = config.settings.spaceExConfig.maxIterations;

		printLine("max jumps " + jumps);
		printLine("print on");
		printLine("}");
	}

	private String[] getOrderParam()
	{
		return orders.split("-");
	}

	private String getTimeParam()
	{
		String value = time;

		if (value.equals("auto"))
			value = doubleToString(config.settings.spaceExConfig.timeHorizon);

		return value;
	}

	private String getPlotParam()
	{
		String auto = "gnuplot octagon";
		String value = plot;

		if (value.equals("auto"))
			value = auto;

		if (value.equals("gnuplot octagon") || value.equals("gnuplot interval")
				|| value.equals("matlab interval") || value.equals("matlab octagon"))
			value = value + " " + config.settings.plotVariableNames[0] + ","
					+ config.settings.plotVariableNames[1];

		return value;
	}

	private String[] getStepParam()
	{
		String value = step;

		if (value.contains("auto"))
		{
			String autoVal = doubleToString(config.settings.spaceExConfig.samplingTime);
			value = value.replace("auto", autoVal);
		}

		return value.split("-");
	}

	/**
	 * Print variable declarations and their initial value assignments plus a list of all constants
	 */
	private void printVars()
	{
		printLine("# Vars");

		String varLine = "state var ";

		boolean first = true;

		for (String v : ha.variables)
		{
			if (first)
				first = false;
			else
				varLine += ", ";

			varLine += v;
		}

		printLine(varLine);
	}

	/**
	 * Print initial states
	 */
	private void printInitialStates()
	{
		printNewline();
		printLine("init");
		printLine("{");

		if (taylorInit != null)
		{
			Hyst.log("Taylor model initial state override was provided");

			String modeName = taylorInit.get(0);
			String tm = taylorInit.get(1).replace(":", "\n");

			printLine(modeName);
			printLine("{");

			for (String line : tm.split("\n"))
				printLine(line);

			printLine("}"); // end mode
		}
		else
		{
			for (Entry<String, Expression> e : config.init.entrySet())
			{
				printLine(e.getKey());
				printLine("{");
				printFlowRangeConditions(removeConstants(e.getValue(), ha.constants.keySet()),
						true);
				printLine("}"); // end mode
			}
		}

		printLine("}"); // end all initial modes
	}

	private static Expression removeConstants(Expression e, Collection<String> constants)
	{
		Operation o = e.asOperation();
		Expression rv = e;

		if (o != null)
		{
			if (o.op == Operator.AND)
			{
				Operation rvO = new Operation(Operator.AND);

				for (Expression c : o.children)
					rvO.children.add(removeConstants(c, constants));

				rv = rvO;
			}
			else if (o.op == Operator.EQUAL)
			{
				Expression left = o.getLeft();

				if (left instanceof Variable && constants.contains(((Variable) left).name))
					rv = Constant.TRUE;
			}
		}

		return rv;
	}

	/**
	 * Prints the locations with their labels and everything that happens in them (invariant,
	 * flow...)
	 */
	private void printModes()
	{
		printNewline();
		printLine("modes");
		printLine("{");

		// modename
		boolean first = true;

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			AutomatonMode mode = e.getValue();

			if (first)
				first = false;
			else
				printNewline();

			String locName = e.getKey();
			printLine(locName);
			printLine("{");

			// From Xin Chen e-mail:
			// linear ode - linear time-invariant, can also have uncertain input
			// "poly ode 1" works more efficient than the others on low degree
			// and low dimension (<=3) ODEs.
			// "poly ode 2" works more efficient than the others on low degree
			// and medium dimension (4~6) ODEs.
			// "poly ode 3" works more efficient than the others on medium or
			// high degree and high dimension ODEs.
			// "nonpoly ode" works with nonlinear terms

			if (isNonLinearDynamics(mode.flowDynamics))
				printLine("nonpoly ode");
			else if (Classification.isLinearDynamics(mode.flowDynamics))
				printLine("linear ode");
			else if (ha.variables.size() <= 3)
				printLine("poly ode 1");
			else if (ha.variables.size() <= 6)
				printLine("poly ode 2");
			else
				printLine("poly ode 3");

			printLine("{");
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet())
			{
				ExpressionInterval ei = entry.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));

				// be explicit (even though x' == 0 is implied by Flow*)
				printLine(entry.getKey() + "' = " + ei);
			}
			printLine("}");

			// invariant
			printLine("inv");
			printLine("{");

			Expression inv = simplifyExpression(mode.invariant);

			if (!inv.equals(Constant.TRUE))
			{
				printCommentBlock("Original invariant: " + inv.toDefaultString());
				printLine(inv.toString());
			}

			printLine("}"); // end invariant

			printLine("}"); // end individual mode
		}

		printLine("}"); // end all modes
	}

	private boolean isNonLinearDynamics(LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		boolean rv = false;

		for (ExpressionInterval entry : flowDynamics.values())
		{
			Expression e = entry.getExpression();

			byte classification = AutomatonUtil.classifyExpressionOps(e);

			if ((classification & AutomatonUtil.OPS_NONLINEAR) != 0)
			{
				rv = true;
				break;
			}
		}

		return rv;
	}

	private void printFlowRangeConditions(Expression ex, boolean isAssignment)
	{
		HashMap<String, Interval> ranges = getExpressionVariableRanges(ex);

		for (Entry<String, Interval> e : ranges.entrySet())
		{
			String varName = e.getKey();
			Interval inter = e.getValue();

			if (isAssignment)
				printLine(varName + " in [" + doubleToString(inter.min) + ", "
						+ doubleToString(inter.max) + "]");
			else
			{
				// it's a comparison

				if (inter.min == inter.max)
					printLine(varName + " = " + doubleToString(inter.min));
				else
				{
					if (inter.min != -Double.MAX_VALUE)
						printLine(varName + " >= " + doubleToString(inter.min));

					if (inter.max != Double.MAX_VALUE)
						printLine(varName + " <= " + doubleToString(inter.max));
				}
			}
		}
	}

	private static HashMap<String, Interval> getExpressionVariableRanges(Expression ex)
	{
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		try
		{
			RangeExtractor.getVariableRanges(ex, ranges);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}

		return ranges;
	}

	private void printJumps()
	{
		printNewline();
		printLine("jumps");
		printLine("{");

		boolean first = true;

		for (AutomatonTransition t : ha.transitions)
		{
			Expression guard = simplifyExpression(t.guard);

			if (guard == Constant.FALSE)
				continue;

			if (first)
				first = false;
			else
				printNewline();

			String fromName = t.from.name;
			String toName = t.to.name;

			printLine(fromName + " -> " + toName);
			printLine("guard");
			printLine("{");

			if (!guard.equals(Constant.TRUE))
			{
				printCommentBlock("Original guard: " + t.guard.toDefaultString());
				printLine(guard.toString());
			}

			printLine("}");

			printLine("reset");
			printLine("{");

			for (Entry<String, ExpressionInterval> e : t.reset.entrySet())
			{
				ExpressionInterval ei = e.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));
				printLine(e.getKey() + "' := " + ei);
			}

			printLine("}");

			if (aggregation.equals("parallelotope"))
				printLine("parallelotope aggregation {}");
			else if (aggregation.equals("interval"))
				printLine("interval aggregation");
			else
				throw new AutomatonExportException("Unknown aggregation method: " + aggregation);
		}

		printLine("}");
	}

	public static class FlowstarExpressionPrinter extends DefaultExpressionPrinter
	{
		public FlowstarExpressionPrinter()
		{
			super();

			opNames.put(Operator.AND, " ");
		}

		@Override
		public String printOperator(Operator op)
		{
			if (op.equals(Operator.GREATER) || op.equals(Operator.LESS)
					|| op.equals(Operator.NOTEQUAL) || op == Operator.OR)
				throw new AutomatonExportException(
						"Flow* printer doesn't support operator " + op.toDefaultString());

			return super.printOperator(op);
		}

		@Override
		protected String printTrue()
		{
			return " ";
		}

		@Override
		protected String printFalse()
		{
			return "1 <= 0"; // not really sure if this will work
		}

		@Override
		protected String printOperation(Operation o)
		{
			String rv = "";

			if (Operator.isComparison(o.op))
			{
				Operator op = o.op;

				// make sure it's of the form p ~ c
				if (o.children.size() == 2 && o.getRight() instanceof Constant)
					rv = super.printOperation(o);
				else
				{
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
					rv += o.getLeft() + " - (" + o.getRight() + ") " + printOperator(op) + " 0";
				}
			}
			else
				rv = super.printOperation(o);

			return rv;
		}
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;
		Expression.expressionPrinter = new FlowstarExpressionPrinter();

		if (ha.modes.containsKey("init"))
			throw new AutomatonExportException("mode named 'init' is not allowed in Flow* printer");

		if (ha.modes.containsKey("start"))
			throw new AutomatonExportException(
					"mode named 'start' is not allowed in Flow* printer");

		if (!areIntervalInitialStates(config))
			convertInitialStatesToUrgent(config);

		checkBoundedInitialStates(config);

		AutomatonUtil.convertUrgentTransitions(ha, config);

		printDocument(originalFilename);
	}

	public static void convertInitialStatesToUrgent(Configuration config)
	{
		Collection<Expression> allInitExpressions = new ArrayList<Expression>();

		allInitExpressions.addAll(config.init.values());

		config.init.values();

		ConvertToStandardForm.convertInit(config);

		updateInitCondition(config, allInitExpressions);
	}

	/**
	 * Update the initial condition to be the weak union of all the other modes
	 * 
	 * @param config
	 *            the config object
	 * @param initExpressions
	 *            the list of all the initial expressions in all the modes
	 */
	private static void updateInitCondition(Configuration config,
			Collection<Expression> initExpressions)
	{
		Map<String, Interval> weakVarBounds = new HashMap<String, Interval>();

		// get weak bounds for each variable over all the initial states
		for (Expression e : initExpressions)
		{
			Map<String, Interval> bounds = getExpressionWeakVariableRanges(e);

			for (Entry<String, Interval> boundsEntry : bounds.entrySet())
			{
				String var = boundsEntry.getKey();
				Interval i = boundsEntry.getValue();

				// merge i into the existing interval bounds
				Interval cur = weakVarBounds.get(var);

				if (cur == null)
					weakVarBounds.put(var, i);
				else
					weakVarBounds.put(var, Interval.union(cur, i));
			}
		}

		// apply weak bounds for each variable to initial state
		Expression init = config.init.values().iterator().next();

		for (Entry<String, Interval> e : weakVarBounds.entrySet())
		{
			String v = e.getKey();
			Interval i = e.getValue();

			if (i.min != -Double.MAX_VALUE)
			{
				Operation cond = new Operation(i.min, Operator.LESSEQUAL, v);
				init = Expression.and(init, cond);
			}

			if (i.max != Double.MAX_VALUE)
			{
				Operation cond = new Operation(v, Operator.LESSEQUAL, i.max);
				init = Expression.and(init, cond);
			}
		}

		AutomatonMode initMode = ConvertToStandardForm.getInitMode((BaseComponent) config.root);
		config.init.put(initMode.name, init);
	}

	/**
	 * Gets the weak ranges for the given expression. Only interval ranges are extracted... other
	 * ranges are ignored.
	 * 
	 * @param ex
	 *            the input expression
	 * @return
	 */
	private static Map<String, Interval> getExpressionWeakVariableRanges(Expression ex)
	{
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		try
		{
			RangeExtractor.getWeakVariableRanges(ex, ranges);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}

		return ranges;
	}

	private static void checkBoundedInitialStates(Configuration c)
	{
		// there must be bounds on every variable in the initial state for Flow*
		// to work
		for (Entry<String, Expression> e : c.init.entrySet())
		{
			Expression exp = e.getValue();
			String mode = e.getKey();

			Collection<String> allVars = AutomatonUtil.getVariablesInExpression(exp);

			for (String v : c.root.variables)
			{
				if (!allVars.contains(v))
					throw new AutomatonExportException("Flow* requires bounds be defined for all "
							+ "variables in initial states. Variable '" + v + "' was not bounded in"
							+ " initial mode '" + mode + "' with expression: "
							+ exp.toDefaultString());
			}
		}
	}

	/**
	 * Does the following expression give a interval range for every variable?
	 * 
	 * @param ex
	 *            the expression to check
	 * @return true iff the expression imposes interval ranges
	 */
	private static boolean isIntervalRangeCondition(Expression ex)
	{
		boolean rv = true;

		try
		{
			HashMap<String, Interval> ranges = new HashMap<String, Interval>();
			RangeExtractor.getVariableRanges(ex, ranges);
		}
		catch (EmptyRangeException e)
		{
			rv = false;
		}
		catch (ConstantMismatchException e)
		{
			rv = false;
		}
		catch (UnsupportedConditionException e)
		{
			rv = false;
		}

		return rv;
	}

	/**
	 * Test if each initial mode can be defined just using intervals over the variables
	 * 
	 * @param c
	 *            the configuration
	 * @return true iff using intervals is enough
	 */
	private static boolean areIntervalInitialStates(Configuration config)
	{
		boolean rv = true;
		BaseComponent ha = (BaseComponent) config.root;

		for (Entry<String, Expression> e : config.init.entrySet())
		{
			if (!isIntervalRangeCondition(removeConstants(e.getValue(), ha.constants.keySet())))
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	@Override
	public String getToolName()
	{
		return "Flow*";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "flowstar";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public String getExtension()
	{
		return ".flowstar";
	}
}

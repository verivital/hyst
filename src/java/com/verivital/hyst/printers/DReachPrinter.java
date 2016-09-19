/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.Collection;
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
import com.verivital.hyst.internalpasses.RenameParams;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.AddIdentityResetPass;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * Takes a hybrid automaton from the internal model format and outputs a dReach 2.0 model. Based on
 * Chris' Boogie printer.
 * 
 * @author Stanley Bak (8-2014)
 * @author Taylor Johnson (9-2014)
 *
 */
public class DReachPrinter extends ToolPrinter
{
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	private BaseComponent ha;

	/**
	 * map from mode string names to numeric ids, starting from 1 and incremented
	 */
	private TreeMap<String, Integer> modeNamesToIds = new TreeMap<String, Integer>();

	@Override
	protected String getCommentPrefix()
	{
		return "//";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls printProcedure() to
	 * print the BPL code
	 */
	protected void printDocument(String originalFilename)
	{
		renameVariables();

		this.printCommentHeader();

		// begin printing the actual program
		printNewline();
		printProcedure();
	}

	private void renameVariables()
	{
		TreeMap<String, String> mapping = new TreeMap<String, String>();
		mapping.put("t", "clock");
		mapping.put("time", "clock_time");

		// also rename all variables that start with an underscore
		Collection<String> names = ha.getAllNames();

		for (String name : names)
		{
			if (name.startsWith("_"))
			{
				String newName = AutomatonUtil.freshName("v" + name, names);
				mapping.put(name, newName);
			}
		}

		RenameParams.run(config, mapping);
	}

	/**
	 * Print the actual DReach code
	 */
	private void printProcedure()
	{
		printVars();

		// printSettings(); // TODO: print to other file or command line call

		printConstants();

		printModes();

		printInitialStates();

		printGoalStates();
	}

	/**
	 * Print variable declarations and their initial value assignments plus a list of all constants
	 */
	private void printVars()
	{
		printLine(commentChar + "Vars");

		// time
		String maxTime = time;

		if (maxTime.equals("auto"))
			maxTime = doubleToString(config.settings.spaceExConfig.timeHorizon);

		printLine("[0, " + maxTime + "] time;");

		for (String v : ha.variables)
			printLine("[-1000,1000] " + v + ";"); // TODO: get bounds
													// automatically
	}

	/**
	 * Print constants using #defines
	 * 
	 * This is more efficient than printing them as part of initial condition, since if we do that,
	 * we'd have to declare them as variables, which will increase the state space size
	 * 
	 * As #defines, the pre-processor will just replace them as numbers
	 * 
	 * Note: in general, we may not always be able to just use #defines, for instance, if we have a
	 * nondeterministic range in which case, we would need to introduce another variable and add
	 * e.g. 0 <= A <= 5.2 as an initial condition constraint
	 */
	private void printConstants()
	{
		printLine("");

		if (!ha.constants.isEmpty())
		{
			for (Entry<String, Interval> e : ha.constants.entrySet())
			{
				printLine("#define " + e.getKey() + "\t"
						+ Double.toString(e.getValue().asConstant()));
				// e.getKey(),Double.toString(e.getValue())
			}
		}

		printLine("");
	}

	/**
	 * Print initial states
	 */
	private void printInitialStates()
	{
		printNewline();
		printLine("init:");

		printLine("@" + modeNamesToIds.get(config.init.keySet().iterator().next()).toString() + " "
				+ config.init.values().iterator().next() + ";");
	}

	/**
	 * Print goal states
	 */
	private void printGoalStates()
	{
		printNewline();
		printLine("goal:");

		// outStream.print("@" + ModeNamesToIds.get(ha.forbiddenMode).toString()
		// + " ");
		printLine("@" + modeNamesToIds.get(config.forbidden.keySet().iterator().next()).toString()
				+ " " + config.forbidden.values().iterator().next() + ";");
	}

	/**
	 * Prints the locations with their labels and everything that happens in them (invariant,
	 * flow...)
	 */
	private void printModes()
	{
		printNewline();

		printLine(commentChar + " start modes"); // start all modes

		// modename
		boolean first = true;

		// first pass over to create ids
		int id = 1;

		for (String modeName : ha.modes.keySet())
			modeNamesToIds.put(modeName, id++);

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			AutomatonMode mode = e.getValue();

			if (first)
				first = false;
			else
				printNewline();

			String locName = e.getKey();
			printLine(commentChar + " " + locName);
			printLine("{");
			printLine("mode " + modeNamesToIds.get(locName) + ";");

			// invariant
			printLine("invt:");
			this.increaseIndentation();

			if (!mode.invariant.equals(Constant.TRUE))
				printLine(mode.invariant + ";");

			this.decreaseIndentation();

			printLine("flow:");
			this.increaseIndentation();

			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet())
			{
				ExpressionInterval ei = entry.getValue();

				if (ei.getInterval() != null)
					throw new AutomatonExportException(
							"dReach doesn't support nondeterministic flows. Error exporting "
									+ "flow for variable " + entry.getKey() + ": " + ei);

				printLine(
						"d/dt[" + entry.getKey() + "] = " + entry.getValue().asExpression() + ";");
			}

			this.decreaseIndentation();

			printJumps(mode);

			printLine(commentChar + " end " + locName);
			printLine("}");
			this.indentation = "";
		}

		printLine(commentChar + " end modes"); // end all modes
	}

	private void printJumps(AutomatonMode mode)
	{
		printNewline();
		printLine("jump:");
		this.increaseIndentation();
		int fromId = modeNamesToIds.get(mode.name);

		boolean first = true;

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from != mode)
				continue;

			if (first)
				first = false;
			else
				printNewline();

			String toName = t.to.name;
			int toId = modeNamesToIds.get(toName);

			printLine(commentChar + " " + mode.name + " -> " + toName + " (" + fromId + " -> "
					+ toId + ")");

			this.increaseIndentation();

			String line = "";

			if (t.guard != Constant.TRUE)
				line += t.guard + " ==> @" + this.modeNamesToIds.get(toName);
			else
				line += "(true) ==> @" + this.modeNamesToIds.get(toName);

			Map<String, ExpressionInterval> reset = t.reset;

			// TODO: this check was to 0, but we could have a model with 0 vars,
			// which then would have no resets, and that would be fine
			if (reset.size() != ha.variables.size())
				throw new AutomatonExportException(
						"Since dReach requires identity resets, it should never be null (but reset was null): "
								+ reset);

			// should be be of the form (and (x' = x + 1) (y' = x + y) (z' = z))
			Operation resetExp = new Operation(Operator.AND);

			for (Entry<String, ExpressionInterval> e : reset.entrySet())
			{
				ExpressionInterval ei = e.getValue();

				if (ei.getInterval() == null)
				{
					Expression exp = new Operation(Operator.EQUAL, new Variable(e.getKey() + "'"),
							e.getValue().asExpression());

					resetExp.children.add(exp);
				}
				else
				{
					Interval i = ei.getInterval();
					// interval is nonnull, nondeterministic reset

					Operation lowerBound = new Operation(Operator.GREATEREQUAL,
							new Variable(e.getKey() + "'"), new Operation(Operator.ADD,
									e.getValue().getExpression(), new Constant(i.min)));

					Operation upperBound = new Operation(Operator.LESSEQUAL,
							new Variable(e.getKey() + "'"), new Operation(Operator.ADD,
									e.getValue().getExpression(), new Constant(i.max)));

					resetExp.children.add(lowerBound);
					resetExp.children.add(upperBound);
				}
			}

			line += resetExp + ";";
			printLine(line);

			this.decreaseIndentation();
		}

		printNewline();
		this.decreaseIndentation();
	}

	// custom printer for dreach expressions, mix of infix and prefix
	public static class DReachExpressionPrinter extends DefaultExpressionPrinter
	{
		public DReachExpressionPrinter()
		{
			super();

			opNames.put(Operator.AND, "and");
			opNames.put(Operator.OR, "or");
			opNames.put(Operator.POW, "^");

			// force to print decimals
			constFormatter.setMinimumFractionDigits(1);
		}

		public String printOperation(Operation o)
		{
			String rv = null;

			Operator op = o.op;

			// dreach expects a mix of infix and prefix
			switch (op)
			{
			case MULTIPLY:
			case DIVIDE:
			case ADD:
			case SUBTRACT:
			case POW:
				// default
				rv = super.printOperation(o);
				break;
			case EQUAL:
			case LESS:
			case GREATER:
			case LESSEQUAL:
			case GREATEREQUAL:
			case NOTEQUAL:
				// infix
				rv = "(" + print(o.getLeft()) + " " + opNames.get(op) + " " + print(o.getRight())
						+ ")";
				break;
			case NEGATIVE:
				rv = "-" + print(o.children.get(0));
				break;
			default:
				// prefix
				rv = "(" + opNames.get(op);

				for (Expression e : o.children)
					rv += " " + print(e);

				rv += ")";
				break;
			}

			return rv;
		}
	}

	@Override
	protected void printAutomaton()
	{
		Expression.expressionPrinter = new DReachExpressionPrinter(); // TODO:
																		// move
																		// to
																		// constructor?

		this.ha = (BaseComponent) config.root;

		if (config.forbidden.size() == 0)
		{
			Hyst.log(
					"DReach Printer: using initial states as forbidden states since forbidden states are not defined in model.");
			config.forbidden = config.init;
		}

		// remove this after proper support for multiple initial modes is added
		if (config.init.size() != 1)
			throw new AutomatonExportException(
					"Printer currently only supports single-initial-state models");
		else if (config.forbidden.size() != 1)
			throw new AutomatonExportException(
					"Printer currently only supports single-forbidden-state models");

		// transform resets to include identity expressions
		new AddIdentityResetPass().runTransformationPass(config, null);

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "dReach";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "dreach";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public String getExtension()
	{
		return ".drh";
	}
}

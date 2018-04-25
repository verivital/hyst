/**
 * 
 */
package com.verivital.hyst.printers;

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

/**
 * Takes a hybrid automaton from the internal model format and outputs a C2E2 (HyXML) model
 * 
 * TODO: just created base model
 * 
 * @author Taylor Johnson (9-2015)
 *
 */
public class C2E2Printer extends ToolPrinter
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
		return "# ";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls printProcedure() to
	 * print the BPL code
	 */
	protected void printDocument(String originalFilename)
	{
		TreeMap<String, String> mapping = new TreeMap<String, String>();
		mapping.put("t", "clock");
		mapping.put("time", "clock_time");

		RenameParams.run(config, mapping);

		this.printCommentHeader();

		Expression.expressionPrinter = new HyCompExpressionPrinter(); // TODO:
																		// move
																		// to
																		// constructor?

		// begin printing the actual program
		printNewline();
		printProcedure();
	}

	/**
	 * Print the actual HyComp code
	 */
	private void printProcedure()
	{
		printmodule();

		printVars();

		// printSettings(); // TODO: print to other file or command line call

		printConstants();

		printModes();

		printJumps();

		printInitialStates();

		printGoalStates();
	}

	/**
	 * Print the module definitions
	 */
	private void printmodule()
	{

		printLine("MODULE main");
		printLine("VAR");
		this.increaseIndentation();
		printLine("instance: " + config.settings.spaceExConfig.systemID.toString() + ";");
		this.decreaseIndentation();
		printLine("");
		printLine("MODULE " + config.settings.spaceExConfig.systemID.toString());
	}

	/**
	 * Print variable declarations and their initial value assignments plus a list of all constants
	 */
	private void printVars()
	{
		printLine(commentChar + "Vars");
		printLine("VAR");
		this.increaseIndentation();

		// print variable for locations
		printComment("Locations are encoded as a variable with a finite (enumeration / set) type.");
		// printLine("location : { ");
		int i = 0;
		String line = "location: {";
		for (Entry<String, AutomatonMode> m : ha.modes.entrySet())
		{
			if (i > 0)
			{
				line = line + ", ";
			}
			line = line + m.getKey();
			++i;
		}
		printLine(line + "};");

		// TODO: all variables are continuous? support further typing?
		for (String v : ha.variables)
		{
			printLine(v + ": continuous;");
		}

		// time
		String maxTime = time;

		if (maxTime.equals("auto"))
			maxTime = doubleToString(config.settings.spaceExConfig.timeHorizon);

		// printLine("[0, " + maxTime + "] time;"); // TODO: fix

		this.decreaseIndentation();
	}

	/**
	 * Print constants as FROZENVARS. Although there is also something called DEFINE, so...
	 * 
	 * This is more efficient than printing them as part of initial condition, since if we do that,
	 * we'd have to declare them as variables, which will increase the state space size
	 * 
	 * Note: in general, we may not always be able to just use #defines, for instance, if we have a
	 * nondeterministic range in which case, we would need to introduce another variable and add
	 * e.g. 0 <= A <= 5.2 as an initial condition constraint
	 */
	private void printConstants()
	{
		printLine("");
		// printLine("FROZENVAR"); // frozen variables are constants that don't
		// change
		printLine("DEFINE"); // frozen variables are constants that are
								// constant... maybe for nondetermisistic
								// values?
		this.increaseIndentation();
		printLine("true := TRUE;");
		printLine("false := FALSE;");
		if (!ha.constants.isEmpty())
		{
			for (Entry<String, Interval> e : ha.constants.entrySet())
			{
				// if frozen
				// printLine(e.getKey() + ": real");

				// if define
				printLine(e.getKey() + " :=" + Double.toString(e.getValue().asConstant()) + ";");

				// e.getKey(),Double.toString(e.getValue())
			}
		}
		// TODO: add to init I guess, but this also seems to support #defines,
		// so maybe ideal to do that there as well... but looks
		// like these then have to be inputs (see dist_controller examples from
		// TACAS 2015 paper)
		this.decreaseIndentation();
		// printLine("");
	}

	/**
	 * Print initial states
	 */
	private void printInitialStates()
	{
		printNewline();
		printLine("INIT");
		// printLine(ha.init.keySet().iterator().next());
		// modeNamesToIds.get(ha.init.keySet().iterator().next()).toString()
		this.increaseIndentation();
		printLine("location = " + config.init.keySet().iterator().next() + " " // TODO:
																				// figure
																				// out
																				// how
																				// to
																				// get
																				// the
																				// location
																				// out
																				// of
																				// this
																				// expression...
				+ " & " + config.init.values().iterator().next() + ";");
		// TODO: edge cases, check if no expression, etc.
		this.decreaseIndentation();
	}

	/**
	 * Print goal states
	 */
	private void printGoalStates()
	{
		printNewline();
		printLine("INVARSPEC ");
		this.increaseIndentation();
		// TODO "location = " + ha.init.keySet().iterator().next() +
		// printLine("!" +
		// modeNamesToIds.get(ha.forbidden.keySet().iterator().next()).toString()
		// + " " + ha.forbidden.values().iterator().next() + ";");
		printLine("!(location = " + config.forbidden.keySet().iterator().next() + " & "
				+ config.forbidden.values().iterator().next() + ");");
		this.decreaseIndentation();
	}

	/**
	 * Prints the locations with their labels and everything that happens in them (invariant,
	 * flow...)
	 */
	private void printModes()
	{
		printNewline();

		printLine(commentChar + " start locations"); // start all modes

		// modename
		boolean first = true;

		// first pass over to create ids
		int id = 1;

		for (String modeName : ha.modes.keySet())
		{
			modeNamesToIds.put(modeName, id++);
		}

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			AutomatonMode mode = e.getValue();

			if (first)
				first = false;
			else
				printNewline();

			String locName = e.getKey();
			printLine(commentChar + " " + locName);

			// INVAR will be all together
			// FLOWS may be split across
			// it MAY be optimal to combine all flows that may be equal (as
			// there is syntax to say all modes have the same ODEs, some subset
			// have some ODEs, etc.)

			// printLine("mode " + modeNamesToIds.get(locName) + ";");

			printLine("FLOW");
			this.increaseIndentation();
			// printLine("(location = " + locName + " -> "
			int j = 0;
			String line = "";
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet())
			{
				if (j > 0)
					line = line + "&";
				line = line + "der(" + entry.getKey() + ") = " + entry.getValue().asExpression();
				++j;
			}
			printLine("(location = " + locName + " -> (" + line + "));");
			this.decreaseIndentation();

			printLine(commentChar + " end " + locName);
			this.indentation = "";
		}

		// invariant
		printComment("invariants are printed all together with implications on locations, etc.");
		printLine("INVAR");
		int i = 0;
		String line = "";
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			AutomatonMode mode = e.getValue();
			this.increaseIndentation();
			if (!mode.invariant.equals(Constant.TRUE))
			{
				if (i > 0)
				{
					line = line + " & "; // TODO: use expression printer for AND
											// / the constant value like
											// HyCompExpressionPrinter.AND ?
				}
				line = line + "(location = " + mode.name + " -> " + mode.invariant + ")";
				++i;
			}

			this.decreaseIndentation();
		}
		this.printLine(line + ";");

		printLine(commentChar + " end modes"); // end all modes
	}

	private void printJumps()
	{
		printNewline();
		printLine("TRANS");
		this.increaseIndentation();

		boolean first = true;

		int i_transition = 0;

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			AutomatonMode mode = e.getValue();
			int fromId = modeNamesToIds.get(mode.name);

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

				if (i_transition >= 1)
				{
					line += " | ";
				}

				line += "(location = " + mode.name + " & ";

				if (t.guard != Constant.TRUE)
					line += t.guard + " & next(location) = " + toName;
				else
					line += "(true) & next(location) = " + toName;

				Map<String, ExpressionInterval> reset = t.reset;

				// if (reset.size() > 0) {
				// line += " & ";
				// }

				// TODO: this check was to 0, but we could have a model with 0
				// vars, which then would have no resets, and that would be fine
				if (reset.size() != ha.variables.size())
					throw new AutomatonExportException(
							"Since HyComp requires identity resets, it should never be null (but reset was null): "
									+ reset);

				// should be be of the form (and (x' = x + 1) (y' = x + y) (z' =
				// z))
				Operation resetExp = new Operation(Operator.AND);

				for (Entry<String, ExpressionInterval> eReset : reset.entrySet())
				{
					if (resetExp.children.size() == 2)
					{
						// expression can not have more than two childrens
						line += " & " + resetExp + " ";
						resetExp = new Operation(Operator.AND);
					}

					ExpressionInterval ei = eReset.getValue();

					if (ei.getInterval() == null)
					{
						Expression exp = new Operation(Operator.EQUAL,
								new Variable("next(" + eReset.getKey() + ")"),
								eReset.getValue().asExpression());

						resetExp.children.add(exp);
					}
					else
					{
						Interval i = ei.getInterval();
						// interval is nonnull, nondeterministic reset

						Operation lowerBound = new Operation(Operator.GREATEREQUAL,
								new Variable("next(" + eReset.getKey() + ")"),
								new Operation(Operator.ADD, eReset.getValue().getExpression(),
										new Constant(i.min)));

						Operation upperBound = new Operation(Operator.LESSEQUAL,
								new Variable("next(" + eReset.getKey() + ")"),
								new Operation(Operator.ADD, eReset.getValue().getExpression(),
										new Constant(i.max)));

						resetExp.children.add(lowerBound);
						resetExp.children.add(upperBound);
					}

				}
				if (resetExp.children.size() == 1)
				{
					line += " " + resetExp;
				}
				else
					line += " & " + resetExp;

				printLine(line + ") ");

				this.decreaseIndentation();
				++i_transition;
			}
		}
		this.print(";");

		printNewline();
		this.decreaseIndentation();
	}

	// custom printer for HyComp expressions, mix of infix and prefix
	public static class HyCompExpressionPrinter extends DefaultExpressionPrinter
	{
		public HyCompExpressionPrinter()
		{
			super();

			opNames.put(Operator.AND, "&amp;");
			opNames.put(Operator.OR, "|");
			opNames.put(Operator.POW, "^");
			opNames.put(Operator.LESS, "&lt;");
			opNames.put(Operator.LESSEQUAL, "&lt;=");
			opNames.put(Operator.GREATER, "&gt;");
			opNames.put(Operator.GREATEREQUAL, "&gt;=");

			// force to print decimals
			constFormatter.setMinimumFractionDigits(1);
		}
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;

		if (config.forbidden.size() == 0)
		{
			Hyst.log(
					"HyComp Printer: using initial states as forbidden states since forbidden states are not defined in model.");
			config.forbidden = config.init;
		}

		// remove this after proper support for multiple initial modes is added
		if (config.init.size() != 1)
			throw new AutomatonExportException(
					"Printer currently only supports single-initial-state models");
				// else if (ha.forbidden.size() != 1)
				// throw new AutomatonExportException("Printer currently only supports
				// single-forbidden-state models");

		// transform resets to include identity expressions
		new AddIdentityResetPass().runTransformationPass(config, null);

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "HyComp";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-hycomp";
	}

	@Override
	public boolean isInRelease()
	{
		return false; // TODO: enable for release
	}

	@Override
	public String getExtension()
	{
		return ".xml";
	}
}

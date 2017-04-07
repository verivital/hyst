/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.ExpressionPrinter;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.AddIdentityResetPass;

/**
 * Takes a hybrid automaton from the internal model format and outputs a python QBMC model. Based on
 * DReach printer.
 * 
 * @author Stanley Bak (8-2014)
 * @author Taylor Johnson (9-2014)
 * @author Djordje Maksimovic (10-2014)
 * 
 */
public class PythonQBMCPrinter extends ToolPrinter
{
	private BaseComponent ha;

	/**
	 * map from mode string names to numeric ids, starting from 1 and incremented
	 */
	private TreeMap<String, Integer> ModeNamesToIds = new TreeMap<String, Integer>();

	/**
	 * Map from bad (old) variable names to replacements
	 * 
	 * Note: necessary for reserved names by tools
	 */
	Map<String, String> swapNames = new HashMap<String, String>();

	/**
	 * TODO: update, this is just copied
	 */
	public Map<String, String> getDefaultParams()
	{
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();

		params.put("time", "auto");
		params.put("step", "auto-auto");
		params.put("remainder", "1e-4");
		params.put("precondition", "auto");
		params.put("plot", "auto");
		params.put("orders", "3-8");
		params.put("cutoff", "1e-15");
		params.put("precision", "53");
		params.put("jumps", "99999999");
		params.put("print", "on");
		params.put("aggregation", "parallelotope");

		return params;
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
	 * Print the actual Python QBMC code
	 */
	private void printProcedure()
	{
		printHeader();
		printDesignProperties();
		printVars();
		printModes();
		printInitialStates();
		printGoalStates();
		printQBFMultiplexers();
		printScopes();
		printFooter();
	}

	private void printHeader()
	{
		printLine("from z3 import *\n" + "from math import *\n\n" +

		"opt_debug = True");
		printNewline();
	}

	private void printDesignProperties()
	{
		printLine(commentChar + "Design Properties");

		printLine("k = 8");
		printLine("modeBitSize = int(ceil(log(" + (ha.modes.size() + 1) + ", 2)))");
		printLine("klog = int(ceil(log(k, 2)))");
		printNewline();
	}

	/**
	 * Print variable declarations and their initial value assignments plus a list of all constants
	 */
	private void printVars()
	{
		printLine(commentChar + "Variable definitions");

		printLine("tbv = BitVec(\'tbv\', klog)");
		printLine("all_state_vars = []"); // ALL state variables over ALL
											// iterations
		printLine("v_time = []"); // continuous time variables
		printLine("mode = []");
		printLine("time = Real(\'time\')"); // real time value
		printLine("cur_mode = BitVec(\'cur_mode\', modeBitSize)");
		printLine("next_mode = BitVec(\'next_mode\', modeBitSize)");
		printLine("for i in range(0, k+1):");
		increaseIndentation();
		printLine("mode.append( BitVec(\'mode_\' + str(i), modeBitSize) )");
		printLine("v_time.append( Real('time_' + str(i)))"); // # time variable
																// for each
																// elapse at BMC
																// step k)
		printLine("all_state_vars.append(mode[i])");
		printLine("all_state_vars.append(v_time[i])");
		decreaseIndentation();

		for (String v : ha.variables)
		{
			/*
			 * if (v.equals("t")) { String vNew = "clock"; // TODO: add replacement for clock if it
			 * exists swapNames.put(v, vNew); v = vNew; }
			 */
			printLine(v + " = []");
			printLine("cur_" + v + " = Real(\'cur_" + v + "\')");
			printLine("next_" + v + " = Real(\'next_" + v + "\')");
			printLine("for i in range(0, k+1):");
			increaseIndentation();
			printLine(v + ".append( Real(\'" + v + "_\' + str(i)) )");
			printLine("all_state_vars.append(" + v + "[i])");
			decreaseIndentation();
		}
		ha.variables.removeAll(swapNames.keySet());
		ha.variables.addAll(swapNames.values());
		printNewline();
		if (!ha.constants.isEmpty())
		{
			printLine(commentChar + "Constant definitions");
			for (Entry<String, Interval> e : ha.constants.entrySet())
			{
				printLine(e.getKey() + " = " + e.getValue().asConstant());
				printLine("cur_" + e.getKey() + " = " + e.getValue().asConstant());
			}
		}
		printLine("true = True");
		printLine("false = False");
		printNewline();
	}

	/**
	 * Print initial states
	 */
	private void printInitialStates()
	{
		printLine(commentChar + "initial states");
		printLine("initStates = And(");

		increaseIndentation();

		// TODO Stan fixed initial states printing; make sure it's right
		// Expression.expressionPrinter = curExpressionPrinter;

		// init is a mapping: Loc -> expr: need to modify each Expr: put("time",
		// new Constant(0)); // TODO: generalize?
		String text = "";
		text = config.init.values().iterator().next().toString();
		text = text.replaceAll("cur_", "");
		for (String v : ha.variables)
		{
			// text.replace("cur_"+v, v+"[0]");
			// text = text.replaceAll("cur_" + v, v + "[0]");
			text = text.replaceAll(v, v + "[0]");
		}
		printLine(text + ", v_time[0] == 0" // HACK
				+ ", mode[0] == BitVecVal("
				+ ModeNamesToIds.get(config.init.keySet().iterator().next()) + ", modeBitSize)");
		printLine(")");
		decreaseIndentation();
		printNewline();
	}

	/**
	 * Print goal states
	 */
	private void printGoalStates()
	{
		printLine(commentChar + "bad states");
		printLine("badStates = []");
		printLine("for i in range(1, k+1): ");
		increaseIndentation();

		// TODO: pull in bad states from input automaton, currently setting to
		// max of modeBitSize
		// printLine("badStates.append(mode[i] ==
		// BitVecVal(pow(2,modeBitSize)-1, modeBitSize))");
		String text = "And(";
		String badvalue = config.forbidden.values().iterator().next().toString();
		for (String v : ha.variables)
		{
			// text.replace("cur_"+v, v+"[0]");
			badvalue = badvalue.replaceAll("cur_" + v, v + "[i]");
		}
		printLine("badStates.append(" + text + "mode[i] == BitVecVal("
				+ ModeNamesToIds.get(config.forbidden.keySet().iterator().next())
				+ ", modeBitSize), " + badvalue + "))");

		decreaseIndentation();
		printLine("badStates = And(Or(");

		increaseIndentation();
		printLine("badStates");
		// outStream.print(printFlowRangeConditions(ha.initialStates, true,
		// true));

		printLine("))");
		decreaseIndentation();
		printNewline();
	}

	/**
	 * Prints the locations with their labels and everything that happens in them (invariant,
	 * flow...)
	 */
	private void printModes()
	{

		printLine("rate = 1;");

		int count = 0;
		int arg_count = 0;
		int arg_max = 255; // maximum number of arguments in function in Python

		printLine(commentChar + "Trajectories");

		// first pass over to create ids
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			ModeNamesToIds.put(e.getKey(), count++);
		}
		int mode_length = ModeNamesToIds.size();
		// TODO: different traj. in different loc
		printLine("Trajectories = And(next_mode == cur_mode, time >= 0, ");
		increaseIndentation();
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			++arg_count;
			if (arg_count == 1)
			{
				printLine("And(");
			}
			printLine("Implies(cur_mode == " + ModeNamesToIds.get(e.getKey()) + ",");
			increaseIndentation();
			Expression.expressionPrinter = InvariantExpressionPrinter;
			printLine("And(next_mode == cur_mode,");
			if (e.getValue().invariant.toString() != "true")
			{
				printLine(commentChar + "invariant");
				String invariant = e.getValue().invariant.toString();
				printLine(InvariantPrinter("next_", invariant));// Hack
				printLine(InvariantPrinter("cur_", invariant));
			}
			AutomatonMode mode = e.getValue();
			printLine(commentChar + "flow: " + mode.flowDynamics.entrySet().toString());
			Boolean match = true;

			for (String v : ha.variables)
			{
				match = false;
				for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet())
				{
					if (entry.getKey().matches(v)
							& !entry.getValue().asExpression().toString().equals("0.0"))
					{
						// double value =
						// Double.parseDouble(entry.getValue().asExpression().toString());
						// if (entry.getKey().matches(v) & (value != 0)){
						printLine("next_" + v + " == " + "cur_" + v + " + time * ("
								+ entry.getValue().asExpression() + "),");
						match = true;
					}
				}
				if (!match)
				{
					printLine("next_" + v + " == " + "cur_" + v + ",");
				}
			}
			printLine(")),");
			decreaseIndentation();
			if (arg_count == mode_length)
			{
				printLine(")");
			}
			if (arg_count == arg_max)
			{
				printLine("),");
				arg_count = 0;
				mode_length = mode_length - arg_max;
			}
		}
		decreaseIndentation();

		printLine(")");

		printNewline();

		printLine(commentChar + " Transition Relation"); // start all modes

		// modename
		boolean first = true;
		arg_count = 0;
		mode_length = ModeNamesToIds.size();
		printLine("Transitions = And(");
		increaseIndentation();

		String line = "";
		arg_count = 0;
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			++arg_count;
			if (arg_count == 1)
			{
				printLine("And(");
			}
			AutomatonMode mode = e.getValue();
			if (first)
				first = false;
			else
				line += ",\n" + indentation;

			String locName = e.getKey();

			line += "Implies(cur_mode == BitVecVal(" + ModeNamesToIds.get(locName)
					+ ", modeBitSize), ";
			printLine(line);
			line = "";

			printJumps(mode);
			// line += indentation + ")";
			printLine(")");
			if (arg_count == mode_length)
			{
				printLine(")");
			}
			if (arg_count == arg_max)
			{
				printLine("),");
				arg_count = 0;
				mode_length = mode_length - arg_max;
				first = true;
			}
		}

		// printLine(line);

		// discrete identity (modes / non-changing continuous variables are
		// fixed over transitions)
		// for (String v : ha.variableNames) {
		// printLine("next_"+v+" == cur_"+v+"[i],"); // TODO: wrong, this does
		// for all cont. vars, need to check identities vs. not
		// }

		printNewline();
		printLine(")");
		decreaseIndentation();
		printNewline();
		printLine("TransitionRelation = Or(Transitions, Trajectories)");
		printNewline();

	}

	// Hack
	private String InvariantPrinter(String text, String invariant)
	{
		String inv = "";
		String[] token = invariant.split(",  ");
		if (token.length != 0)
		{
			for (int i = 0; i < token.length; i++)
			{
				// token[i] = token[i].replaceAll("\\s","");
				inv = inv + text + token[i] + ", ";
			}
		}
		return inv;
	}

	private void printQBFMultiplexers()
	{
		printLine(commentChar + " Outer Multiplexers");
		printLine("muxes = []");

		printLine("for i in range(0, k):");
		increaseIndentation();
		printLine("if i == 0:");
		increaseIndentation();
		printQBFSubMux("v_time[i+1] >= v_time[i]");
		decreaseIndentation();
		printLine("else:");
		increaseIndentation();
		printQBFSubMux("v_time[i] > v_time[i-1]");
		decreaseIndentation();

		printLine("muxes.append( mux_i )");
		decreaseIndentation();

		printLine("mux = And(muxes)");
		printNewline();
	}

	private void printQBFSubMux(String c)
	{
		printLine("mux_i = Implies(tbv == i,");
		printLine("And(");

		for (String v : ha.variables)
		{
			printLine("cur_" + v + " == " + v + "[i],");
			printLine("next_" + v + " == " + v + "[i+1],");
		}
		// real time
		printLine("time == v_time[i],");
		printLine(c + ",");

		printLine("cur_mode == mode[i],");
		printLine("next_mode == mode[i+1]");
		printLine(") )");
	}

	private void printScopes()
	{
		printLine(commentChar + " Finilize QBF formulation");
		printLine("inner = And(initStates, TransitionRelation, badStates, mux)");
		String line = "qbf_bmc_inner = Exists([";

		for (String v : ha.variables)
			line += "cur_" + v + ", next_" + v + ", ";

		printLine(line + "cur_mode, next_mode, time], inner)");
		printLine("qbf_bmc_middle = ForAll(tbv, qbf_bmc_inner)");
		printLine("qbf_bmc_outer = qbf_bmc_middle");
	}

	private void printFooter()
	{
		printLine(commentChar + " Run z3\n" + "z3_solver = Solver()\n\n" +

		"if opt_debug:\n" + indentationAmount + "print qbf_bmc_outer\n\n" +

		"z3_solver.add( qbf_bmc_outer )\n" + "result = z3_solver.check()\n" + "print result\n\n" +

		"if result == sat:\n" + indentationAmount + "model = z3_solver.model()\n\n" +

		indentationAmount + "print model\n");
		increaseIndentation();
		printCounterExample();
		decreaseIndentation();
	}

	private void printCounterExample()
	{
		printLine(commentChar + "print counterexample in execution order");
		printLine("for i in range(0, k):");
		increaseIndentation();
		printLine("print os.linesep");
		printLine("print \"step \" + str(i) + \"/\" + str(k) + \" state:\"");
		printLine("print \"mode: \" + str(model[mode[i]])");
		printLine("print \"time: \" + str(model[v_time[i]])");
		for (String v : ha.variables)
		{
			printLine("print \"" + v + ": \" + str(model[" + v + "[i]])");
		}

	}

	private static String operatorToString(Operator o)
	{
		String rv = o.toString();

		if (o == Operator.AND)
			rv = "And";
		else if (o == Operator.OR)
			rv = "Or";
		else if (o == Operator.EQUAL)
			rv = "==";

		return rv;
	}

	/*
	 * TODO Stan removed this because it was for the old internal format private String
	 * printFlowRangeConditions(Expression ex, boolean isAssignment, boolean isInitAssignment,
	 * boolean endLine) { // HACK: drop spaceex location constraints from expressions (e.g.,
	 * goal/init) if (ex.toString().startsWith("loc(")) { return "True"; }
	 * 
	 * if (ex instanceof Operation) { Operation o = (Operation)ex;
	 * 
	 * // stupid mix of infix and prefix switch (o.op) { case MULTIPLY : case DIVIDE : case ADD :
	 * case SUBTRACT : case EQUAL : case LESS : case GREATER : case LESSEQUAL : case GREATEREQUAL :
	 * case NOTEQUAL : return printFlowRangeConditions(o.getLeft(), isAssignment, isInitAssignment,
	 * endLine) + " " + operatorToString(op) + " " + printFlowRangeConditions(o.getRight(),
	 * isAssignment, isInitAssignment, endLine); case AND : case OR : default : return
	 * operatorToString(op) + "(" + " " + printFlowRangeConditions(o.getLeft(), isAssignment,
	 * isInitAssignment, endLine) + ", " + printFlowRangeConditions(o.getRight(), isAssignment,
	 * isInitAssignment, endLine) + ")"; } } else if (ex instanceof Variable) { // swap illegal
	 * variable names String varName = ex.toString(); if (varName == null || varName.equals("null"))
	 * { throw new AutomatonExportException( "Bad variable name: " + varName + " vs. " + ex); }
	 * 
	 * if (this.swapNames.containsKey(varName)) { return this.swapNames.get(varName); } else { if
	 * (((Variable)ex).toString().equals("true")) return "True"; else if
	 * (((Variable)ex).toString().equals("false")) return "False"; else if (isInitAssignment) return
	 * ((Variable)ex).toString()+"[0]"; else return "cur_"+((Variable)ex).toString(); } } else if
	 * (ex instanceof Constant) { return ex.toString(); } else { throw new AutomatonExportException(
	 * "Bad expression printing: " + ex); } }
	 */

	private void printJumps(AutomatonMode mode)
	{
		boolean first = true;

		printLine("Or(");

		String text = "";
		Expression.expressionPrinter = curExpressionPrinter;
		for (AutomatonTransition transition : ha.transitions)
		{
			if (!transition.from.name.equals(mode.name))
				continue;

			String toName = transition.to.name;

			// printLine(commentChar + " " + fromName + " -> " + toName + " (" +
			// this.ModeNamesToIds.get(fromName) + " -> " +
			// this.ModeNamesToIds.get(toName) + ")");

			if (first)
				first = false;
			else
				text += ",\n" + indentation;

			Expression guard = transition.guard;
			if (guard != null)
			{
				// Stan TODO you'll probably want a custom printer so guards
				// print correctly
				text += indentation + "And(" + guard + ", And(";
			}
			else
			{
				text += indentation + "And(True, And(";
			}

			// TODO Stan: Tried to change this to try to match the new automaton
			// format
			// Expression.expressionPrinter = curExpressionPrinter;
			for (Entry<String, ExpressionInterval> e : transition.reset.entrySet())
			{
				String varName = e.getKey();
				Expression exp = e.getValue().asExpression();

				// "next_"+o.getLeft() + " == " + sb.toString()) + ", "

				text += "next_" + varName + " == " + exp + ", ";
			}

			/*
			 * Expression reset = transition.reset; if (reset != null) {
			 * outStream.print(getAssignments(reset)); } else { throw new AutomatonExportException(
			 * "Identity resets was null, but it should not be null: " + reset); }
			 */

			text += "next_mode == BitVecVal(" + this.ModeNamesToIds.get(toName)
					+ ", modeBitSize)))";
		}
		// increaseIndentation();
		printLine(text + ")");// decreaseIndentation();

	}

	/**
	 * Print resets recursively. These should be be of the form x' = x + 1 && y' = x + y
	 * 
	 * @param e
	 *            the expression to print
	 */
	/*
	 * TODO: Stan removed this since it's for the old format private String
	 * getAssignments(Expression e) { String result = ""; if (e instanceof Operation) { Operation o
	 * = (Operation)e;
	 * 
	 * if (o.getOperator() == Operator.AND) { result += getAssignments(o.getLeft()) +
	 * getAssignments(o.getRight()); } else if (o.getOperator() == Operator.EQUAL) { // should be x'
	 * := EXPR if (o.getLeft() instanceof Variable){ // note: already primed Variable var =
	 * ((Variable)o.getLeft()); var.setPrimed(false); String stringOfRightSide = ""+o.getRight(); if
	 * (!var.toString().equals(stringOfRightSide)) { // variables in the expression need to be
	 * modified to include the cur_ prefix // I do this by splitting up the expression and modifying
	 * each variable one at a time String[] tokens; tokens =
	 * stringOfRightSide.split("((?<=[^A-Za-z0-9_])|(?=[^A-Za-z0-9_]))"); for (int k = 0; k <
	 * tokens.length; k++) { for (String v : ha.variableNames) { if (tokens[k].equals(v)) {
	 * tokens[k] = "cur_"+v; break; } } } StringBuilder sb = new StringBuilder(); for(String str :
	 * tokens) sb.append(str); result += ("next_"+o.getLeft() + " == " + sb.toString()) + ", "; }
	 * else { result += ("True") + ", "; } }else throw new AutomatonExportException(
	 * "Assignments should be of the form \"next_<variable> = <expression>\": " + e); } else { throw
	 * new AutomatonExportException( "Assignments should only have ANDs: " + e); } } else { throw
	 * new AutomatonExportException("Not an operation: " + e); } return result; }
	 */

	/**
	 * Prints the flow assignments (derivatives)
	 * 
	 * @param expression
	 */
	// TODO Stan removed this since it assumed the old internal representation
	/*
	 * private void printFlowAssignments(Expression expression) { if (expression != null) { if
	 * (expression instanceof Operation) { Operation op = (Operation) expression;
	 * 
	 * if (op.getOperator() == Operator.EQUAL) { Expression left = op.getLeft(); Expression right =
	 * op.getRight();
	 * 
	 * if (left instanceof Variable) { String varName = ((Variable) left).get();
	 * 
	 * if (this.swapNames.containsKey(varName)) { varName = this.swapNames.get(varName); }
	 * 
	 * printLine("d/dt[" + varName + "] = " + right + ";"); } else throw new
	 * AutomatonExportException( "Flow (derivative) left-hand-side is not a variable: " +
	 * expression); } else if (op.getOperator() == Operator.AND) {
	 * printFlowAssignments(op.getLeft()); printFlowAssignments(op.getRight()); } else throw new
	 * AutomatonExportException( "Flows (derivatives) may only containt ANDs: " + expression); }
	 * else throw new AutomatonExportException( "Flows (derivatives) must be expressions: " +
	 * expression); } }
	 */

	class CurExpressionPrinter extends DefaultExpressionPrinter
	{
		public CurExpressionPrinter()
		{
			super();
			opNames.put(Operator.AND, ", "); // print comma-space instead of &
			opNames.put(Operator.EQUAL, "=="); // print == instead of =
		}

		public String printVariable(Variable v)
		{
			return "cur_" + v.name;
		}
	};

	ExpressionPrinter curExpressionPrinter = new CurExpressionPrinter();

	class InvariantExpressionPrinter extends DefaultExpressionPrinter
	{
		public InvariantExpressionPrinter()
		{
			super();
			opNames.put(Operator.AND, ", "); // print comma-space instead of &
			opNames.put(Operator.EQUAL, "=="); // print == instead of =
		}
	};

	ExpressionPrinter InvariantExpressionPrinter = new InvariantExpressionPrinter();

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;

		// remove this after proper support for multiple initial modes is added
		if (config.init.size() != 1)
		{
			throw new AutomatonExportException(
					"Printer currently only supports single-initial-state models");
		}

		// transform resets to include identity expressions
		new AddIdentityResetPass().runTransformationPass(config, null);

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "Python QBMC";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "qbmc";
	}

	@Override
	protected String getCommentPrefix()
	{
		return "#";
	}

}

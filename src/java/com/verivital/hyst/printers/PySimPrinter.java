/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.ExpressionPrinter;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * Printer for Python-based simulation models.
 * 
 * @author Stanley Bak (1-2015)
 *
 */
public class PySimPrinter extends ToolPrinter
{
	private static PySimExpressionPrinter pySimExpressionPrinter = new PySimExpressionPrinter();
	private static SympyPrinter sympyPyinter = new SympyPrinter();

	private static final String COMMENT_CHAR = "#";
	public BaseComponent ha;

	public PySimPrinter()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT); // skip the 'no urgent
															// modes' check
	}

	@Override
	protected String getCommentPrefix()
	{
		return COMMENT_CHAR + " ";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls
	 * printProcedure() to print the BPL code
	 */
	private void printDocument(String originalFilename)
	{
		this.printCommentHeader();

		// begin printing the actual program
		printNewline();
		printProcedure();
	}

	@Override
	protected String createCommentText(String text)
	{
		return "'''\n" + text + "\n'''";
	}

	private static void appendModes(StringBuilder rv, BaseComponent ha)
	{
		for (AutomatonMode am : ha.modes.values())
		{
			appendNewline(rv);

			/*
			 * one = ha.new_mode('one') one.der = lambda state, _: [2, 1]
			 * one.inv = lambda(x): x[0] <= 2
			 */

			appendIndentedLine(rv, am.name + " = ha.new_mode('" + am.name + "')");
			appendIndentedLine(rv, am.name + ".inv = lambda state: " + am.invariant);

			appendIndentedLine(rv, am.name + ".inv_sympy = " + sympyPyinter.print(am.invariant));

			if (!am.urgent)
			{
				appendIndentedLine(rv,
						am.name + ".der = lambda _, state: " + getMapString(am.flowDynamics, ha));

				appendIndentedLine(rv, am.name + ".der_interval_list = "
						+ getIntervalListString(am.flowDynamics, ha));
			}
		}
	}

	/**
	 * Gets the interval list string. if x' == x+1 + [1,2] and y' == y-x + [-1,
	 * 1], this would give: '[[1,2],[-1,1]]'
	 * 
	 * @param map
	 * @return the mapped string
	 */
	private static String getIntervalListString(LinkedHashMap<String, ExpressionInterval> flow,
			BaseComponent ha)
	{
		StringBuffer rv = new StringBuffer();
		rv.append("[");

		for (String var : ha.variables)
		{
			if (rv.length() > 1)
				rv.append(", ");

			ExpressionInterval ei = flow.get(var);

			if (ei == null)
				rv.append("None");
			else if (ei.getInterval() == null)
				rv.append("[0, 0]");
			else
			{
				Interval i = ei.getInterval();

				rv.append("[" + i.min + ", " + i.max + "]");
			}
		}

		rv.append("]");

		return rv.toString();
	}

	/**
	 * Gets a map string. Null values get mapped to the variable name if x' ==
	 * x+1 and y' == y-x, this would give: '[x + 1, y - x]'
	 * 
	 * @param map
	 * @return the mapped string
	 */
	private static String getMapString(Map<String, ExpressionInterval> map, BaseComponent ha)
	{
		StringBuffer rv = new StringBuffer();
		rv.append("[");

		for (String var : ha.variables)
		{
			if (rv.length() > 1)
				rv.append(", ");

			ExpressionInterval ei = map.get(var);

			if (ei == null)
				rv.append("None");
			else
				rv.append(ei.getExpression());
		}

		rv.append("]");

		return rv.toString();
	}

	private static void appendJumps(StringBuilder rv, BaseComponent ha)
	{
		/*
		 * t = ha.new_transition(one, two) t.guard = lambda(x): x[0] >= 2
		 * t.reset = lambda(x): (x[0] + 1, x[1])
		 */

		for (AutomatonTransition at : ha.transitions)
		{
			appendNewline(rv);

			appendIndentedLine(rv,
					"t = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");
			appendIndentedLine(rv, "t.guard = lambda state: " + at.guard);
			appendIndentedLine(rv, "t.reset = lambda state: " + getMapString(at.reset, ha));

			appendIndentedLine(rv, "t.guard_sympy = " + sympyPyinter.print(at.guard));
		}
	}

	/**
	 * Print the actual Flow* code
	 */
	private void printProcedure()
	{
		printLine("import hybridpy.pysim.simulate as sim");

		printLine(automatonToString(config));

		printLine("def simulate(max_time=" + getTimeParam() + "):");
		increaseIndentation();
		printLine("'''simulate the automaton from each initial rect'''");
		printSimulate();
		decreaseIndentation();
		printNewline();

		int xDim = ha.variables.indexOf(config.settings.plotVariableNames[0]);
		int yDim = ha.variables.indexOf(config.settings.plotVariableNames[1]);
		printLine("def plot(result, filename='plot.png', dim_x=" + xDim + ", dim_y=" + yDim + "):");
		increaseIndentation();
		printLine("'''plot a simulation result to a file'''");
		printPlot();
		decreaseIndentation();
		printNewline();

		// check if main module
		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("plot(simulate())");
		decreaseIndentation();
		printNewline();
	}

	/**
	 * Converts the given hybrid automaton to a python-parsable String
	 * 
	 * @param config
	 *            the (flat) configuration
	 * @return
	 */
	public static String automatonToString(Configuration config)
	{
		ExpressionPrinter savedPrinter = Expression.expressionPrinter;

		Expression.expressionPrinter = pySimExpressionPrinter;
		pySimExpressionPrinter.ha = (BaseComponent) config.root;
		sympyPyinter.ha = (BaseComponent) config.root;

		StringBuilder rv = new StringBuilder();

		if (!(config.root instanceof BaseComponent))
			throw new AutomatonExportException("PySim expected flat automaton");

		BaseComponent ha = (BaseComponent) config.root;
		appendLine(rv, "from hybridpy.pysim.hybrid_automaton import HybridAutomaton");
		appendLine(rv, "from hybridpy.pysim.hybrid_automaton import HyperRectangle");
		appendLine(rv, "from hybridpy.pysim.simulate import init_list_to_q_list");
		appendLine(rv, "from sympy.core import symbols");
		appendLine(rv, "from sympy import And, Or");
		appendNewline(rv);

		appendLine(rv, "def define_ha():");
		appendIndentedLine(rv, "'''make the hybrid automaton and return it'''");
		appendIndentedLine(rv, COMMENT_CHAR + " Variable ordering: " + ha.variables);
		appendNewline(rv);
		appendSymbols(config.root.variables, rv);
		appendNewline(rv);
		appendIndentedLine(rv, "ha = HybridAutomaton()");
		appendModes(rv, ha);
		appendJumps(rv, ha);
		appendNewline(rv);
		appendIndentedLine(rv, "return ha");
		appendNewline(rv);

		appendLine(rv, "def define_init_states(ha):");
		appendIndentedLine(rv, "'''returns a list of (mode, HyperRectangle)'''");
		appendInit(rv, config);
		appendNewline(rv);

		// restore expressionPrinter
		Expression.expressionPrinter = savedPrinter;

		return rv.toString();
	}

	private static void appendNewline(StringBuilder rv)
	{
		rv.append("\n");
	}

	private static void appendLine(StringBuilder rv, String string)
	{
		rv.append(string + "\n");
	}

	private static void appendIndentedLine(StringBuilder rv, String string)
	{
		rv.append("    " + string + "\n");
	}

	private static void appendInit(StringBuilder rv, Configuration config)
	{
		/*
		 * # Variable ordering: [x, t, tglobal] rv = []
		 * 
		 * r = HyperRectangle([(4.5, 5.5), (0.0, 0.0), (0.0, 0.0)])
		 * rv.append((ha.modes['loc1'], r))
		 * 
		 * r = HyperRectangle([(7.5, 8.5), (0.0, 0.0), (0.0, 0.0)])
		 * rv.append((ha.modes['loc2'], r))
		 * 
		 * return rv
		 */
		BaseComponent ha = (BaseComponent) config.root;

		appendIndentedLine(rv, COMMENT_CHAR + " Variable ordering: " + ha.variables);
		appendIndentedLine(rv, "rv = []");
		appendNewline(rv);

		for (Entry<String, Expression> e : config.init.entrySet())
		{
			String modeName = e.getKey();
			Expression exp = e.getValue();

			try
			{
				appendHyperRectangleFromInitExpression(rv, exp, ha);
			}
			catch (AutomatonExportException exception)
			{
				throw new AutomatonExportException("Error printing initial states in mode "
						+ modeName + ":" + exception.getLocalizedMessage(), exception);
			}

			appendIndentedLine(rv, "rv.append((ha.modes['" + modeName + "'], r))");
			appendNewline(rv);
		}

		appendIndentedLine(rv, "return rv");
	}

	private static void appendHyperRectangleFromInitExpression(StringBuilder rv, Expression exp,
			BaseComponent ha)
	{
		// r = HyperRectangle([(4.5, 5.5), (0.0, 0.0), (0.0, 0.0)])
		StringBuilder sb = new StringBuilder("r = HyperRectangle([");

		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		try
		{
			RangeExtractor.getVariableRanges(exp, ranges);
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

		for (String s : ha.variables)
		{
			if (s != ha.variables.get(0))
				sb.append(", ");

			Interval i = ranges.get(s);

			if (i == null)
				throw new AutomatonExportException("Initial range for variable " + s
						+ " was not defined in" + " expression: " + exp.toDefaultString());

			sb.append("(" + doubleToString(i.min) + ", " + doubleToString(i.max) + ")");
		}

		sb.append("])");

		appendIndentedLine(rv, sb.toString());
	}

	private void printSimulate()
	{
		/*
		 * ha = define_ha() init_states = define_init_states(ha) q_list =
		 * init_list_to_q_list(init_states, center=True, star=True,
		 * corners=False) result = sim.simulate_multi(q_list, max_time)
		 * 
		 * return result
		 */

		printNewline();
		printLine("ha = define_ha()");
		printLine("init_states = define_init_states(ha)");
		printLine("q_list = init_list_to_q_list(init_states, " + "center="
				+ toolParams.get("center") + ", star=" + toolParams.get("star") + ", corners="
				+ toolParams.get("corners") + ", rand=" + toolParams.get("rand") + ")");
		printLine("result = sim.simulate_multi(q_list, max_time)");
		printNewline();
		printLine("return result");
	}

	private static void appendSymbols(List<String> vars, StringBuilder text)
	{
		StringBuilder sb = new StringBuilder();

		for (String var : vars)
		{
			if (var != vars.get(0))
				sb.append(", ");

			sb.append("sym_" + var);
		}

		sb.append(" = symbols('");

		for (String var : vars)
			sb.append(var + " ");

		sb.append("')");

		appendIndentedLine(text, sb.toString());
	}

	private void printPlot()
	{
		/*
		 * draw_events = len(result) == 1 sim.plot_sim_result_multi(result,
		 * dim_x, dim_y, filename, draw_events)
		 */

		printNewline();
		printLine("draw_events = len(result) == 1");
		printLine("shouldShow = False");
		String title = toolParams.get("title");
		printLine("sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events, "
				+ "legend=" + toolParams.get("legend") + ", title="
				+ (title == "None" ? "None" : "'" + title + "'") + ", show=shouldShow)");
	}

	private String getTimeParam()
	{
		String value = toolParams.get("time");

		if (value.equals("auto"))
			value = doubleToString(config.settings.spaceExConfig.timeHorizon);

		return value;
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;
		Expression.expressionPrinter = pySimExpressionPrinter;
		pySimExpressionPrinter.ha = ha;

		printDocument(originalFilename);
	}

	private static class SympyPrinter extends DefaultExpressionPrinter
	{
		BaseComponent ha;

		public SympyPrinter()
		{
			this.opNames.put(Operator.EQUAL, "==");
			this.opNames.put(Operator.AND, "and");
			this.opNames.put(Operator.OR, "or");
			this.opNames.put(Operator.POW, "**");
		}

		@Override
		protected String printOperation(Operation o)
		{
			String s = null;

			if (o.op == Operator.AND)
				s = "And(" + print(o.getLeft()) + ", " + print(o.getRight()) + ")";
			else if (o.op == Operator.OR)
				s = "Or(" + print(o.getLeft()) + ", " + print(o.getRight()) + ")";
			else
				s = super.printOperation(o);

			return s;
		}

		@Override
		protected String printTrue()
		{
			return "True";
		}

		@Override
		protected String printFalse()
		{
			return "False";
		}

		@Override
		protected String printVariable(Variable v)
		{
			String rv = null;

			if (ha.variables.contains(v.name))
				rv = "sym_" + v.name;
			else if (ha.constants.containsKey(v.name))
			{
				Interval i = ha.constants.get(v.name);

				if (i.width() > 0)
					throw new AutomatonExportException(
							"tried to print pysim model with " + "interval constant");

				rv = printConstantValue(i.min);
			}
			else
				throw new AutomatonExportException("var '" + v.name
						+ "' in expression was not an automaton variable or cosntant");

			return rv;
		}
	}

	private static class PySimExpressionPrinter extends DefaultExpressionPrinter
	{
		BaseComponent ha;
		String BASE = "state";

		public PySimExpressionPrinter()
		{
			this.opNames.put(Operator.EQUAL, "==");
			this.opNames.put(Operator.AND, "and");
			this.opNames.put(Operator.OR, "or");
			this.opNames.put(Operator.POW, "**");

			this.opNames.put(Operator.SIN, "math.sin");
			this.opNames.put(Operator.COS, "math.cos");
			this.opNames.put(Operator.TAN, "math.tan");
			this.opNames.put(Operator.EXP, "math.exp");
			this.opNames.put(Operator.SQRT, "math.sqrt");
			this.opNames.put(Operator.LN, "math.log");
		}

		@Override
		protected String printTrue()
		{
			return "True";
		}

		@Override
		protected String printFalse()
		{
			return "False";
		}

		@Override
		protected String printVariable(Variable v)
		{
			if (ha == null)
				throw new AutomatonExportException(
						"pySimPrinter.ha must be set before printing (was null)");

			String rv = null;
			String name = v.name;

			int index = ha.variables.indexOf(name);

			if (index == -1)
			{
				Interval value = ha.constants.get(name);

				if (value == null)
					throw new AutomatonExportException("PySimPrinter tried to "
							+ "print variable/constant not found in base component: '" + name
							+ "'");

				rv = "" + value.middle();
			}
			else
				rv = BASE + "[" + index + "]";

			return rv;
		}
	}

	@Override
	public String getToolName()
	{
		return "PySim";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-pysim";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public Map<String, String> getDefaultParams()
	{
		LinkedHashMap<String, String> toolParams = new LinkedHashMap<String, String>();

		toolParams.put("time", "auto");
		toolParams.put("step", "auto");
		toolParams.put("legend", "True");
		toolParams.put("center", "True");
		toolParams.put("star", "True");
		toolParams.put("corners", "False");
		toolParams.put("rand", "0");
		toolParams.put("title", "Simulation");

		return toolParams;
	}

	@Override
	public String getExtension()
	{
		return ".py";
	}
}

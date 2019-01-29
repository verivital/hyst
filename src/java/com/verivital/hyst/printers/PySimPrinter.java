/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.ExpressionPrinter;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.DynamicsUtil;
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
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	@Option(name = "-step", usage = "simulation time step", metaVar = "VAL")
	String step = "auto";

	@Option(name = "-center", usage = "simulate from center of initial states", metaVar = "True/False")
	String center = "True";

	@Option(name = "-star", usage = "simulate from star points of initial states", metaVar = "True/False")
	String star = "True";

	@Option(name = "-corners", usage = "simulate from corners of initial states", metaVar = "True/False")
	String corners = "False";

	@Option(name = "-rand", usage = "simulate from a certain number of random initial states", metaVar = "NUM")
	String rand = "0";

	@Option(name = "-title", usage = "plot title", metaVar = "TITLE")
	String title = "Simulation";

	@Option(name = "-legend", usage = "use legend?", metaVar = "True/False")
	String legend = "True";

	@Option(name = "-xdim", usage = "plot x dim", metaVar = "DIM_INDEX")
	int plotXDim = -1;

	@Option(name = "-ydim", usage = "plot y dim", metaVar = "DIM_INDEX")
	int plotYDim = -1;

	private static PySimExpressionPrinter pySimExpressionPrinter = new PySimExpressionPrinter();

	private static final String COMMENT_CHAR = "#";
	public BaseComponent ha;

	public PySimPrinter()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT); // skip the 'no urgent
															// modes' check

		preconditions.skip(PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN); // skip the
																					// dijunction
		// conversion
	}

	@Override
	protected String getCommentPrefix()
	{
		return COMMENT_CHAR + " ";
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

	@Override
	protected String createCommentText(String text)
	{
		return "'''\n" + text + "\n'''";
	}

	private static void appendModes(StringBuilder rv, BaseComponent ha,
			PythonPrinterCustomization custom)
	{
		for (AutomatonMode am : ha.modes.values())
		{
			appendNewline(rv);

			for (String line : custom.getPrintModeLines(am))
				appendIndentedLine(rv, line);
		}
	}

	/**
	 * Gets the interval list string. if x' == x+1 + [1,2] and y' == y-x + [-1, 1], this would give:
	 * '[[1,2],[-1,1]]'
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
	 * Gets a map string. Null values get mapped to the variable name if x' == x+1 and y' == y-x,
	 * this would give: '[x + 1, y - x]'
	 * 
	 * @param map
	 * @return the mapped string
	 */
	private static String getMapString(String desc, Map<String, ExpressionInterval> map,
			BaseComponent ha)
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
			{
				try
				{
					rv.append(ei.getExpression());
				}
				catch (AutomatonExportException e)
				{
					throw new AutomatonExportException(
							"Error in " + desc + " mapping " + var + " -> " + ei.toDefaultString(),
							e);
				}
			}
		}

		rv.append("]");

		return rv.toString();
	}

	private static void appendJumps(StringBuilder rv, BaseComponent ha,
			PythonPrinterCustomization custom)
	{
		/*
		 * t = ha.new_transition(one, two) t.guard = lambda(x): x[0] >= 2 t.reset = lambda(x): (x[0]
		 * + 1, x[1])
		 */

		for (AutomatonTransition at : ha.transitions)
		{
			appendNewline(rv);

			for (String line : custom.getPrintTransitions(at))
				appendIndentedLine(rv, line);
		}
	}

	/**
	 * Print the actual Pysim code
	 */
	private void printProcedure()
	{
		printLine("import math");
		printLine("import hybridpy.pysim.simulate as sim");
		printLine("from hybridpy.pysim.simulate import init_list_to_q_list, PySimSettings");

		config.settings.spaceExConfig.timeHorizon = Double.parseDouble(getTimeParam());

		if (plotXDim >= 0)
			config.settings.plotVariableNames[0] = ha.variables.get(plotXDim);

		if (plotYDim >= 0)
			config.settings.plotVariableNames[1] = ha.variables.get(plotYDim);

		printLine(automatonToString(config));

		printLine("def define_settings():");
		increaseIndentation();
		printLine("'''defines the automaton / plot settings'''");
		printSettings(config);
		decreaseIndentation();
		printNewline();

		printLine("def simulate(init_states, settings):");
		increaseIndentation();
		printLine("'''simulate the automaton from each initial rect'''");
		printSimulate();
		decreaseIndentation();
		printNewline();

		printLine("def plot(result, init_states, image_path, settings):");
		increaseIndentation();
		printLine("'''plot a simulation result to a file'''");
		printPlot();
		decreaseIndentation();
		printNewline();

		// check if main module
		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("ha = define_ha()");
		printLine("settings = define_settings()");
		printLine("init_states = define_init_states(ha)");
		printLine("plot(simulate(init_states, settings), init_states, 'plot.png', settings)");
		decreaseIndentation();
		printNewline();
	}

	/**
	 * This class can be used to customize the printing for python targets. To use, override each
	 * function or member
	 */
	public static class PythonPrinterCustomization
	{
		public String automatonObjectName = "HybridAutomaton";

		// mode is named am.name
		public ArrayList<String> getExtraModePrintLines(AutomatonMode am)
		{
			return new ArrayList<String>();
		}

		public ArrayList<String> getPrintTransitions(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("t = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");
			rv.add("t.guard = lambda state: " + at.guard);
			rv.add("t.reset = lambda state: "
					+ getMapString("reset assignment", at.reset, at.parent));

			for (String line : getExtraTransitionPrintLines(at))
				rv.add(line);

			return rv;
		}

		public ArrayList<String> getPrintModeLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add(am.name + " = ha.new_mode('" + am.name + "')");
			rv.add(am.name + ".inv = lambda state: " + am.invariant);

			if (!am.urgent)
			{
				rv.add(am.name + ".der = lambda _, state: "
						+ getMapString("flow dynamics", am.flowDynamics, am.automaton));

				rv.add(am.name + ".der_interval_list = "
						+ getIntervalListString(am.flowDynamics, am.automaton));
			}

			for (String line : getExtraModePrintLines(am))
				rv.add(line);

			return rv;
		}

		// transition is named "t"
		public ArrayList<String> getExtraTransitionPrintLines(AutomatonTransition at)
		{
			return new ArrayList<String>();
		}

		public ArrayList<String> getExtraDeclarationPrintLines(BaseComponent ha)
		{
			return new ArrayList<String>();
		}

		public ArrayList<String> getImportLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("from hybridpy.pysim.hybrid_automaton import HybridAutomaton, HyperRectangle");

			return rv;
		}

		public ArrayList<String> getInitLines(Configuration c)
		{
			ArrayList<String> rv = new ArrayList<String>();
			rv.add("'''returns a list of (mode, HyperRectangle)'''");

			BaseComponent ha = (BaseComponent) c.root;

			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			if (nonInputVars.size() == 0)
				throw new AutomatonExportException("zero non-input variables in automaton");

			rv.add(COMMENT_CHAR + " Variable ordering: " + nonInputVars);
			rv.add("rv = []");
			rv.add("");

			for (Entry<String, Expression> e : c.init.entrySet())
			{
				String modeName = e.getKey();
				Expression exp = e.getValue();

				try
				{
					for (Expression o : DynamicsUtil.splitDisjunction(exp))
					{
						String str = "rv.append((ha.modes['" + modeName + "'],";
						str += initToHyperRectangle(o, nonInputVars) + "))";

						rv.add(str);
					}
				}
				catch (AutomatonExportException exception)
				{
					throw new AutomatonExportException("Error printing initial states in mode "
							+ modeName + ":" + exception.getLocalizedMessage(), exception);
				}
			}

			rv.add("return rv");

			return rv;
		}

		public static String initToHyperRectangle(Expression exp, List<String> variableOrder)
		{
			// r = HyperRectangle([(4.5, 5.5), (0.0, 0.0), (0.0, 0.0)])
			StringBuilder sb = new StringBuilder("HyperRectangle([");

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

			for (String s : variableOrder)
			{
				if (s != variableOrder.get(0))
					sb.append(", ");

				Interval i = ranges.get(s);

				if (i == null)
					throw new AutomatonExportException("Initial range for variable " + s
							+ " was not defined in" + " expression: " + exp.toDefaultString());

				sb.append("(" + doubleToString(i.min) + ", " + doubleToString(i.max) + ")");
			}

			sb.append("])");

			return sb.toString();
		}
	}

	/**
	 * Wrapper for automatonToString(config, null)
	 * 
	 * @param config
	 *            the configuration
	 * @return the string representation of the python automaton
	 */
	public static String automatonToString(Configuration config)
	{
		return automatonToString(config, new PythonPrinterCustomization());
	}

	/**
	 * Converts the given hybrid automaton to a python-parsable String
	 * 
	 * @param config
	 *            the (flat) configuration
	 * @return
	 */
	public static String automatonToString(Configuration config, PythonPrinterCustomization custom)
	{
		ExpressionPrinter savedPrinter = Expression.expressionPrinter;

		Expression.expressionPrinter = pySimExpressionPrinter;
		pySimExpressionPrinter.ha = (BaseComponent) config.root;

		new SubstituteConstantsPass().runTransformationPass(config, null);

		StringBuilder rv = new StringBuilder();

		if (!(config.root instanceof BaseComponent))
			throw new AutomatonExportException(
					"PySimPrinter.automatonToString expected flat automaton");

		BaseComponent ha = (BaseComponent) config.root;

		AutomatonMode someMode = ha.modes.values().iterator().next();
		ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode, ha.variables);

		if (custom != null)
			for (String line : custom.getImportLines(ha))
				appendLine(rv, line);

		appendNewline(rv);

		appendLine(rv, "def define_ha():");
		appendIndentedLine(rv, "'''make the hybrid automaton and return it'''");
		appendNewline(rv);
		appendIndentedLine(rv, "ha = " + custom.automatonObjectName + "()");
		appendIndentedLine(rv, "ha.variables = " + quotedVarList(nonInputVars));
		appendNewline(rv);

		for (String line : custom.getExtraDeclarationPrintLines(ha))
			appendIndentedLine(rv, line);

		appendModes(rv, ha, custom);
		appendJumps(rv, ha, custom);
		appendNewline(rv);
		appendIndentedLine(rv, "return ha");
		appendNewline(rv);

		appendLine(rv, "def define_init_states(ha):");

		for (String line : custom.getInitLines(config))
			appendIndentedLine(rv, line);

		appendNewline(rv);

		// restore expressionPrinter
		Expression.expressionPrinter = savedPrinter;

		return rv.toString();
	}

	public void printSettings(Configuration config)
	{
		printLine("s = PySimSettings()");
		printLine("s.max_time = " + config.settings.spaceExConfig.timeHorizon);
		printLine("s.step = " + config.settings.spaceExConfig.samplingTime);

		int xDim = config.root.variables.indexOf(config.settings.plotVariableNames[0]);

		if (xDim == -1)
			throw new AutomatonExportException(
					"Cannot find x dim in automaton: " + config.settings.plotVariableNames[0]);

		int yDim = config.root.variables.indexOf(config.settings.plotVariableNames[1]);

		if (yDim == -1)
			throw new AutomatonExportException(
					"Cannot find y dim in automaton: " + config.settings.plotVariableNames[1]);

		printLine("s.dim_x = " + xDim);
		printLine("s.dim_y = " + yDim);

		printNewline();
		printLine("return s");
	}

	public static String quotedVarList(ArrayList<String> vars)
	{
		StringBuilder rv = new StringBuilder();

		for (String v : vars)
		{
			if (rv.length() > 0)
				rv.append(", ");

			rv.append("\"" + v + "\"");
		}

		return "[" + rv.toString() + "]";
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

	private void printSimulate()
	{
		/*
		 * ha = define_ha() init_states = define_init_states(ha) q_list =
		 * init_list_to_q_list(init_states, center=True, star=True, corners=False) result =
		 * sim.simulate_multi(q_list, max_time)
		 * 
		 * return result
		 */

		printNewline();
		printLine("q_list = init_list_to_q_list(init_states, " + "center=" + center + ", star="
				+ star + ", corners=" + corners + ", rand=" + rand + ")");
		printLine("result = sim.simulate_multi(q_list, settings.max_time)");
		printNewline();
		printLine("return result");
	}

	private void printPlot()
	{
		/*
		 * draw_events = len(result) == 1 sim.plot_sim_result_multi(result, dim_x, dim_y, filename,
		 * draw_events)
		 */

		printNewline();
		printLine("draw_events = len(result) == 1");
		printLine("shouldShow = False");
		printLine(
				"sim.plot_sim_result_multi(result, settings.dim_x, settings.dim_y, image_path, draw_events, "
						+ "legend=" + legend + ", title="
						+ (title == "None" ? "None" : "'" + title + "'")
						+ ", show=shouldShow, init_states=init_states)");
	}

	private String getTimeParam()
	{
		String value = time;

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
				throw new AutomatonExportException("PySimPrinter tried to "
						+ "print variable/constant not found in base component: '" + name + "'");
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
		return "pysim";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public String getExtension()
	{
		return ".py";
	}
}

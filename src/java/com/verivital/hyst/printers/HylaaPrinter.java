/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.printers.PySimPrinter.PythonPrinterCustomization;
import com.verivital.hyst.util.DynamicsUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.StringOperations;

/**
 * Printer for Python-based Hylaa.
 * 
 * @author Stanley Bak (8-2016)
 *
 */
public class HylaaPrinter extends ToolPrinter
{
	@Option(name = "-python_simplify", aliases = { "-simplify",
			"-s" }, usage = "simplify all expressions using python's sympy (slow for large models)")
	public boolean pythonSimplify = false;

	@Option(name = "-plot_full", usage = "use plot_full plotting mode")
	public boolean plotFull = false;

	@Option(name = "-num_angles", usage = "set Hylaa's num_angles plot setting")
	public int numAngles;

	@Option(name = "-max_shown_polys", usage = "set Hylaa's max_shown_polys plot setting")
	public int max_shown_polys = -1;

	@Option(name = "-nodeaggregation", usage = "disable deaggregation")
	public boolean noDeaggregation = false;

	@Option(name = "-noaggregation", usage = "disable aggregation")
	public boolean noAggregation = false;

	@Option(name = "-xdim", usage = "x axis variable name")
	public String xdim;

	@Option(name = "-ydim", usage = "y axis variable name")
	public String ydim;

	@Option(name = "-step", usage = "step size")
	public double step;

	@Option(name = "-sim_tol", usage = "simulation tolerance (accuracy)")
	public double simTol;

	@Option(name = "-solver", usage = "lp solver (cvxopt_glpk or glpk_multi)")
	public String solver;

	@Option(name = "-extra_lines", usage = "set Hylaa's extra_lines parameter")
	public String extraLines;

	@Option(name = "-extend_plot_range_ratio", usage = "set Hylaa's extend_plot_range_ratio parameter")
	public double extendPlot;

	private static final String COMMENT_CHAR = "#";

	public HylaaPrinter()
	{
		this.preconditions.skip(PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN);
	}

	@Override
	protected String getCommentPrefix()
	{
		return COMMENT_CHAR + " ";
	}

	@Override
	protected String createCommentText(String text)
	{
		return "'''\n" + text + "\n'''";
	}

	public static class HylaaExtraPrintFuncs extends PythonPrinterCustomization
	{
		public HylaaExtraPrintFuncs()
		{
			this.automatonObjectName = "LinearHybridAutomaton";
		}

		@Override
		public ArrayList<String> getImportLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("import numpy as np");

			rv.add("from hylaa.hybrid_automaton import LinearHybridAutomaton, LinearConstraint, HyperRectangle");
			rv.add("from hylaa.engine import HylaaSettings");
			rv.add("from hylaa.engine import HylaaEngine");
			rv.add("from hylaa.plotutil import PlotSettings");

			return rv;
		}

		public ArrayList<String> getPrintTransitions(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// guard
			// trans = ha.new_transition(loc1, loc2)
			// trans.guard_list = [guard]

			rv.add("trans = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");

			if (at.guard != Constant.TRUE)
			{
				ArrayList<Operation> parts = DynamicsUtil.splitConjunction(at.guard);

				for (Operation o : parts)
				{
					ArrayList<String> conds = toLinearConstraints(o, at.parent.variables);

					for (int i = 0; i < conds.size(); ++i)
					{
						rv.add("trans.guard_list.append(" + conds.get(i) + ") # "
								+ o.toDefaultString());
					}
				}
			}

			if (at.reset.size() > 0)
				throw new PreconditionsFailedException(
						"Resets not currently supported in hylaa printer: " + at);

			return rv;
		}

		public ArrayList<String> getPrintModeLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add(am.name + " = ha.new_mode('" + am.name + "')");

			try
			{
				rv.add(am.name + ".a_matrix = np.array("
						+ toPythonListList(DynamicsUtil.extractDynamicsMatrixA(am))
						+ ", dtype=float)");
				rv.add(am.name + ".b_vector = np.array("
						+ toPythonList(DynamicsUtil.extractDynamicsVectorB(am)) + ", dtype=float)");

				// invariant
				// loc1.inv_list = [inv1]
				if (am.invariant != Constant.TRUE)
				{
					ArrayList<Operation> parts = DynamicsUtil.splitConjunction(am.invariant);

					for (Operation o : parts)
					{
						ArrayList<String> invs = toLinearConstraints(o, am.automaton.variables);

						for (int i = 0; i < invs.size(); ++i)
						{
							rv.add(am.name + ".inv_list.append(" + invs.get(i) + ") # "
									+ o.toDefaultString());
						}
					}
				}
			}
			catch (AutomatonExportException e)
			{
				throw new PreconditionsFailedException(e.toString(), e);
			}

			return rv;
		}

		/**
		 * Convert a conditions to a list of 'LinearConstraint()' initialization strings
		 * 
		 * @param condition
		 *            a basic condition
		 * @param vars
		 *            the variables
		 * 
		 * @return a list of 'LinearConstraint()' strings (one constraints may produce multiple
		 *         linear constraints)
		 */
		private ArrayList<String> toLinearConstraints(Operation o, ArrayList<String> vars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			Operator op = o.op;

			// extract the variable vector on the left and right hand sides
			ArrayList<Double> leftVec = DynamicsUtil.extractLinearVector(o.getLeft(), vars);
			double leftVal = DynamicsUtil.extractLinearValue(o.getLeft());

			ArrayList<Double> rightVec = DynamicsUtil.extractLinearVector(o.getRight(), vars);
			double rightVal = DynamicsUtil.extractLinearValue(o.getRight());

			// normal form has all variables on left and all constants on the right
			for (int i = 0; i < leftVec.size(); ++i)
				leftVec.set(i, leftVec.get(i) - rightVec.get(i));

			rightVal -= leftVal;
			// now, only work with leftVec and rightVal

			if (op == Operator.LESS || op == Operator.LESSEQUAL || op == Operator.EQUAL)
			{
				StringBuilder str = new StringBuilder("LinearConstraint(");
				str.append(toPythonList(leftVec));
				str.append(", " + ToolPrinter.doubleToString(rightVal));
				str.append(")");
				rv.add(str.toString());
			}

			if (op == Operator.GREATER || op == Operator.GREATEREQUAL || op == Operator.EQUAL)
			{
				for (int i = 0; i < leftVec.size(); ++i)
					leftVec.set(i, -leftVec.get(i));

				StringBuilder str = new StringBuilder("LinearConstraint(");
				str.append(toPythonList(leftVec));
				str.append(", " + ToolPrinter.doubleToString(-rightVal));
				str.append(")");
				rv.add(str.toString());
			}

			if (op != Operator.EQUAL && op != Operator.LESS && op != Operator.LESSEQUAL
					&& op != Operator.GREATER && op != Operator.GREATEREQUAL)
				throw new AutomatonExportException(
						"Not a linear condition: " + o.toDefaultString());

			return rv;
		}

		private String toPythonList(ArrayList<Double> list)
		{
			return "[" + StringOperations.join(", ", list.toArray(new Double[] {})) + "]";
		}

		private String toPythonListList(ArrayList<ArrayList<Double>> matrix)
		{
			ArrayList<String> convertedLists = new ArrayList<String>();

			for (ArrayList<Double> row : matrix)
				convertedLists.add(toPythonList(row));

			return "[" + StringOperations.join(", ", convertedLists.toArray(new String[] {})) + "]";
		}

		@Override
		public ArrayList<String> getExtraTransitionPrintLines(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// add the symbolic guard
			// String s = rrtSymbolicPyinter.print(at.guard);
			// rv.add("t.guard_strings = [" + s + "]");

			return rv;
		}

		@Override
		public ArrayList<String> getInitLines(Configuration c)
		{
			// Hylaa initial states can be linear constraint stars

			ArrayList<String> rv = new ArrayList<String>();
			rv.add("'''returns a list of (mode, list(LinearConstraint])'''");

			BaseComponent ha = (BaseComponent) c.root;

			rv.add(COMMENT_CHAR + " Variable ordering: " + ha.variables);
			rv.add("rv = []");
			rv.add("");

			for (Entry<String, Expression> e : c.init.entrySet())
			{
				String modeName = e.getKey();
				Expression exp = e.getValue();

				rv.add("constraints = []");

				try
				{
					ArrayList<Operation> parts = DynamicsUtil.splitConjunction(exp);

					for (Operation o : parts)
					{
						ArrayList<String> conds = toLinearConstraints(o, ha.variables);

						for (int i = 0; i < conds.size(); ++i)
						{
							String line = "constraints.append(" + conds.get(i) + ") # "
									+ o.toDefaultString();
							rv.add(line);
						}
					}
				}
				catch (AutomatonExportException exception)
				{
					throw new AutomatonExportException("Error printing initial states in mode "
							+ modeName + ":" + exception.getLocalizedMessage(), exception);
				}

				// rv.add(initToHyperRectangle(exp, ha.variables));

				rv.add("rv.append((ha.modes['" + modeName + "'], constraints))");
				rv.add("");
			}

			rv.add("return rv");

			return rv;
		}
	}

	@Override
	protected void printAutomaton()
	{
		BaseComponent ha2 = (BaseComponent) config.root;

		String passParam = SimplifyExpressionsPass.makeParam(pythonSimplify);

		new SimplifyExpressionsPass().runVanillaPass(config, passParam);

		convertErrorModes(config);

		this.printCommentHeader();

		printNewline();

		printLine(PySimPrinter.automatonToString(config, new HylaaExtraPrintFuncs()));

		printLine("def define_settings():");
		increaseIndentation();
		printSettings();
		decreaseIndentation();
		printNewline();

		printLine("def run_hylaa(settings):");
		increaseIndentation();
		printLine("'Runs hylaa with the given settings, returning the HylaaResult object.'");
		printLine("ha = define_ha()");
		printLine("init = define_init_states(ha)");
		printNewline();
		printLine("engine = HylaaEngine(ha, settings)");
		printLine("engine.run(init)");
		printNewline();
		printLine("return engine.result");
		decreaseIndentation();
		printNewline();

		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("settings = define_settings()");
		printLine("run_hylaa(settings)");
		decreaseIndentation();
		printNewline();
	}

	/**
	 * Convert error modes to guard transitions to a new mode 'error'
	 * 
	 * @param config
	 */
	public static void convertErrorModes(Configuration config)
	{
		if (config.forbidden.size() > 0)
		{
			BaseComponent ha = (BaseComponent) config.root;
			AutomatonMode errorMode = ha.createMode("error");
			errorMode.invariant = Constant.TRUE;
			errorMode.flowDynamics = new LinkedHashMap<String, ExpressionInterval>();

			for (String v : ha.variables)
				errorMode.flowDynamics.put(v, new ExpressionInterval(0));

			for (Entry<String, Expression> entry : config.forbidden.entrySet())
			{
				AutomatonMode preMode = ha.modes.get(entry.getKey());

				for (Expression e : DynamicsUtil.splitDisjunction(entry.getValue()))
				{
					AutomatonTransition at = ha.createTransition(preMode, errorMode);
					at.guard = e;
				}
			}

			config.validate();
		}
	}

	private void printSettings()
	{
		int xDim = config.root.variables.indexOf(config.settings.plotVariableNames[0]);
		int yDim = config.root.variables.indexOf(config.settings.plotVariableNames[1]);

		if (this.xdim != null)
		{
			int index = config.root.variables.indexOf(xdim);

			if (index == -1)
				throw new AutomatonExportException(
						"X dim variable " + xdim + " not found in automaton.");
			else
				xDim = index;

			index = config.root.variables.indexOf(ydim);

			if (index == -1)
				throw new AutomatonExportException(
						"Y dim variable " + ydim + " not found in automaton.");
			else
				yDim = index;
		}

		printLine("'get the hylaa settings object'");
		printLine("plot_settings = PlotSettings()");

		String plotMode = "PLOT_NONE";

		if (plotFull)
			plotMode = "PLOT_FULL";

		printLine("plot_settings.plot_mode = PlotSettings." + plotMode);
		printLine("plot_settings.xdim = " + xDim);
		printLine("plot_settings.ydim = " + yDim);

		if (numAngles > 0)
			printLine("plot_settings.numAngles = " + numAngles);

		if (max_shown_polys >= 0)
			printLine("plot_settings.max_shown_polys = " + max_shown_polys);

		if (extraLines != null)
			printLine("plot_settings.extra_lines = " + extraLines);

		if (extendPlot != 0)
			printLine("plot_settings.extend_plot_range_ratio = " + extendPlot);

		printNewline();

		double step = config.settings.spaceExConfig.samplingTime;
		double maxTime = config.settings.spaceExConfig.timeHorizon;

		if (this.step > 0)
			step = this.step;

		printLine("settings =  HylaaSettings(step=" + step + ", max_time=" + maxTime
				+ ", plot_settings=plot_settings)");

		if (simTol > 0)
			printLine("settings.sim_tol = " + simTol);

		if (noDeaggregation)
			printLine("settings.deaggregation = False");

		if (noAggregation)
			printLine("settings.aggregation = False");

		if (solver != null)
			printLine("settings.solver = \"" + solver + "\"");

		printNewline();
		printLine("return settings");
	}

	@Override
	public String getToolName()
	{
		return "Hylaa";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "hylaa";
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

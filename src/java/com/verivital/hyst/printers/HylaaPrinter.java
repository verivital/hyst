/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.printers.PySimPrinter.PythonPrinterCustomization;
import com.verivital.hyst.util.AutomatonUtil;
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
	@Option(name = "-xdim", usage = "x axis variable name")
	public String xdim;

	@Option(name = "-ydim", usage = "y axis variable name")
	public String ydim;

	@Option(name = "-step", usage = "step size")
	public double step;

	@Option(name = "-python_simplify", aliases = { "-simplify",
			"-s" }, usage = "simplify all expressions using python's sympy (slow for large models)")
	public boolean pythonSimplify = false;

	@Option(name = "-settings", usage = "space-separated hylaa settings initialization. For example, "
			+ "'-settings plot_settings.plot_mode=PlotSettings.PLOT_FULL "
			+ "settings.deaggregation=False'", handler = StringArrayOptionHandler.class)
	public List<String> settings = new ArrayList<String>();

	private static final String COMMENT_CHAR = "#";

	public HylaaPrinter()
	{
		this.preconditions.skip(PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN);
		this.preconditions.skip(PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED);
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

			rv.add("from hylaa.hybrid_automaton import LinearHybridAutomaton, LinearConstraint");
			rv.add("from hylaa.engine import HylaaSettings");
			rv.add("from hylaa.engine import HylaaEngine");
			rv.add("from hylaa.containers import PlotSettings, SimulationSettings");

			return rv;
		}

		public ArrayList<String> getPrintTransitions(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// guard
			// trans = ha.new_transition(loc1, loc2)
			// trans.condition_list = [guard]

			rv.add("trans = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");

			if (at.guard != Constant.TRUE)
			{
				ArrayList<Operation> parts = DynamicsUtil.splitConjunction(at.guard);

				for (Operation o : parts)
				{
					ArrayList<String> conds = toLinearConstraintStrings(o, at.parent);

					for (int i = 0; i < conds.size(); ++i)
					{
						rv.add("trans.condition_list.append(" + conds.get(i) + ") # "
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

			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(am,
					am.automaton.variables);

			rv.add(am.name + " = ha.new_mode('" + am.name + "')");

			if (ConvertToStandardForm.getErrorMode(am.automaton) == am)
			{
				rv.add(am.name + ".is_error = True");
			}
			else
			{
				try
				{
					if (nonInputVars.size() > 100)
						rv.addAll(getSparseDynamicsLines(am, nonInputVars));
					else
						rv.addAll(getDenseDynamicsLines(am, nonInputVars));

					rv.add(am.name + ".set_dynamics(a_matrix, c_vector)");

					// invariant
					// loc1.inv_list = [inv1]
					if (am.invariant != Constant.TRUE)
					{
						ArrayList<Operation> parts = DynamicsUtil.splitConjunction(am.invariant);

						if (nonInputVars.size() != am.automaton.variables.size())
							printInputs(am, rv, am.automaton, parts);

						printInvariantConstraints(am, rv, am.automaton, parts);
					}
					else if (nonInputVars.size() != am.automaton.variables.size())
					{
						throw new AutomatonExportException(
								"invariant was 'true', but input variables exist");
					}
				}
				catch (AutomatonExportException e)
				{
					throw new PreconditionsFailedException(e.toString(), e);
				}
			}

			return rv;
		}

		private ArrayList<String> getSparseDynamicsLines(AutomatonMode am,
				ArrayList<String> nonInputVars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			int size = nonInputVars.size();

			ArrayList<Double> data = new ArrayList<Double>();
			ArrayList<Integer> indices = new ArrayList<Integer>();

			for (int i = 0; i < size; ++i)
			{
				ArrayList<Double> row = DynamicsUtil.extractDynamicsMatrixARow(am, i);

				for (int x = 0; x < row.size(); ++x)
				{
					double val = row.get(x);

					if (val != 0) // exact comparison here is okay since it never changes
					{
						indices.add(i * nonInputVars.size() + x);
						data.add(val);
					}
				}
			}

			rv.add("a_inds = np.array(" + toPythonListInt(indices) + ")");
			rv.add("a_data = np.array(" + toPythonList(data) + ")");

			ArrayList<Double> row = DynamicsUtil.extractDynamicsVectorC(am);
			data.clear();
			indices.clear();

			for (int x = 0; x < row.size(); ++x)
			{
				double val = row.get(x);

				if (val != 0) // exact comparison here is okay since it never changes
				{
					indices.add(x);
					data.add(val);
				}
			}

			rv.add("");
			rv.add("c_inds = np.array(" + toPythonListInt(indices) + ")");
			rv.add("c_data = np.array(" + toPythonList(data) + ")");
			rv.add("");
			rv.add("a_matrix = np.zeros([" + size + ", " + size + "])");
			rv.add("c_vector = np.zeros([" + size + "])");
			rv.add("");
			rv.add("for i in xrange(len(a_inds)):");
			rv.add("    row = a_inds[i] / " + size);
			rv.add("    col = a_inds[i] % " + size);
			rv.add("    a_matrix[row, col] = a_data[i]");
			rv.add("");
			rv.add("for i in xrange(len(c_inds)):");
			rv.add("    c_vector[c_inds[i]] = c_data[i]");
			rv.add("");

			return rv;
		}

		private ArrayList<String> getDenseDynamicsLines(AutomatonMode am,
				ArrayList<String> nonInputVars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("a_matrix = np.array([ \\");

			for (int i = 0; i < nonInputVars.size(); ++i)
				rv.add("    " + toPythonList(DynamicsUtil.extractDynamicsMatrixARow(am, i))
						+ ", \\");

			rv.add("    ], dtype=float)");

			rv.add("c_vector = np.array(" + toPythonList(DynamicsUtil.extractDynamicsVectorC(am))
					+ ", dtype=float)");

			return rv;
		}

		public ArrayList<String> getExtraDeclarationPrintLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			if (nonInputVars.size() != ha.variables.size())
			{

				ArrayList<String> inputVars = new ArrayList<String>();

				for (String var : ha.variables)
				{
					if (!nonInputVars.contains(var))
						inputVars.add(var);
				}

				rv.add("# input variable order: " + inputVars);
			}

			return rv;
		}

		private void printInvariantConstraints(AutomatonMode am, ArrayList<String> rv,
				BaseComponent ha, ArrayList<Operation> parts)
		{
			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			for (Operation o : parts)
			{
				// only consider invariant constraints which contain input variables
				boolean containsInputVariables = false;

				for (String var : AutomatonUtil.getVariablesInExpression(o))
				{
					if (!nonInputVars.contains(var))
					{
						containsInputVariables = true;
						break;
					}
				}

				if (containsInputVariables)
					continue;

				// ok, Operation 'o' was an non-input constraint
				ArrayList<String> invs = toLinearConstraintStrings(o, ha);

				for (int i = 0; i < invs.size(); ++i)
				{
					rv.add(am.name + ".inv_list.append(" + invs.get(i) + ") # "
							+ o.toDefaultString());
				}
			}
		}

		private void printInputs(AutomatonMode am, ArrayList<String> rv, BaseComponent ha,
				ArrayList<Operation> parts)
		{
			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			ArrayList<String> inputVars = new ArrayList<String>();

			for (String var : am.automaton.variables)
			{
				if (!nonInputVars.contains(var))
					inputVars.add(var);
			}

			// for every input variable, we need to extract the B matrix, as well
			// as the constraints from the invariant

			// constraints are extracted from the invariant
			ArrayList<ArrayList<Double>> conditions = new ArrayList<ArrayList<Double>>();
			ArrayList<Double> vals = new ArrayList<Double>();

			ArrayList<String> inputConditions = new ArrayList<String>();

			for (Operation o : parts)
			{
				// skip invariant constraints which contain input variables
				boolean containsInputVariables = false;

				for (String var : AutomatonUtil.getVariablesInExpression(o))
				{
					if (!nonInputVars.contains(var))
					{
						containsInputVariables = true;
						break;
					}
				}

				if (!containsInputVariables)
					continue;

				extractLinearConstraints(conditions, vals, o, inputVars);
				inputConditions.add(o.toDefaultString());
			}

			rv.add("");

			// add conditions in comments
			for (String condition : inputConditions)
				rv.add("# " + condition);

			rv.add("u_constraints_a = np.array(" + toPythonListList(conditions) + ", dtype=float)");
			rv.add("u_constraints_b = np.array(" + toPythonList(vals) + ", dtype=float)");

			// b matrix is extracted from the dynamics
			if (nonInputVars.size() > 100)
				rv.addAll(getSparseInputLines(am, nonInputVars));
			else
			{
				// dense definition

				rv.add("b_matrix = np.array("
						+ toPythonListList(DynamicsUtil.extractDynamicsMatrixB(am))
						+ ", dtype=float)");
			}

			// loc1.set_inputs(u_constraints_a, u_constraints_b, b_matrix)
			rv.add(am.name + ".set_inputs(u_constraints_a, u_constraints_b, b_matrix)");
		}

		private ArrayList<String> getSparseInputLines(AutomatonMode am,
				ArrayList<String> nonInputVars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			ArrayList<ArrayList<Double>> bMatrix = DynamicsUtil.extractDynamicsMatrixB(am);
			int h = bMatrix.size();
			int w = bMatrix.get(0).size();

			ArrayList<Double> data = new ArrayList<Double>();
			ArrayList<Integer> indices = new ArrayList<Integer>();

			for (int y = 0; y < h; ++y)
			{
				for (int x = 0; x < w; ++x)
				{
					double val = bMatrix.get(y).get(x);

					if (val != 0) // exact comparison here is okay since it never changes
					{
						indices.add(y * w + x);
						data.add(val);
					}
				}
			}

			rv.add("b_inds = np.array(" + toPythonListInt(indices) + ")");
			rv.add("b_data = np.array(" + toPythonList(data) + ")");
			rv.add("");
			rv.add("b_matrix = np.zeros([" + h + ", " + w + "])");
			rv.add("for i in xrange(len(b_inds)):");
			rv.add("    row = b_inds[i] / " + w);
			rv.add("    col = b_inds[i] % " + w);
			rv.add("    b_matrix[row, col] = b_data[i]");
			rv.add("");

			return rv;
		}

		/**
		 * Extract linear conditions from an operation, and store them in storeConditions and
		 * storeVals. A single operation can cause multiple constraints to be added (for example, an
		 * equality constraints adds two conditions).
		 * 
		 * @param storeConditions
		 *            condition 'a' vectors, such that a * x <= b
		 * @param storeVals
		 *            condition 'b' values, such that a * x <= b
		 * @param o
		 *            the operation to extract from
		 * @param vars
		 *            the automaton where to extract the the list of variables 'x' in a * x <= b
		 */
		private void extractLinearConstraints(ArrayList<ArrayList<Double>> storeConditions,
				ArrayList<Double> storeVals, Operation o, ArrayList<String> vars)
		{
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
				storeConditions.add(leftVec);
				storeVals.add(rightVal);
			}

			if (op == Operator.GREATER || op == Operator.GREATEREQUAL || op == Operator.EQUAL)
			{
				ArrayList<Double> leftVecInverse = new ArrayList<Double>();

				for (double d : leftVec)
					leftVecInverse.add(-d);

				storeConditions.add(leftVecInverse);
				storeVals.add(-rightVal);
			}

			if (op != Operator.EQUAL && op != Operator.LESS && op != Operator.LESSEQUAL
					&& op != Operator.GREATER && op != Operator.GREATEREQUAL)
				throw new AutomatonExportException(
						"Not a linear condition: " + o.toDefaultString());
		}

		/**
		 * Convert a conditions to a list of 'LinearConstraint()' initialization strings, for use
		 * when printing invariants.
		 * 
		 * @param condition
		 *            a basic condition
		 * @param vars
		 *            the variables
		 * 
		 * @return a list of 'LinearConstraint()' strings (one operation may produce multiple linear
		 *         constraints)
		 */
		private ArrayList<String> toLinearConstraintStrings(Operation o, BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			AutomatonMode anyMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(anyMode,
					ha.variables);

			ArrayList<ArrayList<Double>> conditions = new ArrayList<ArrayList<Double>>();
			ArrayList<Double> vals = new ArrayList<Double>();

			extractLinearConstraints(conditions, vals, o, nonInputVars);

			for (int i = 0; i < vals.size(); ++i)
			{
				ArrayList<Double> leftVec = conditions.get(i);
				double rightVal = vals.get(i);

				StringBuilder str = new StringBuilder("LinearConstraint(");

				if (nonInputVars.size() <= 100)
					str.append(toPythonList(leftVec));
				else
					str.append(toSparsePythonList(leftVec));

				str.append(", " + ToolPrinter.doubleToString(rightVal));
				str.append(")");
				rv.add(str.toString());
			}

			return rv;
		}

		private String toSparsePythonList(ArrayList<Double> list)
		{
			String rv = null;
			int size = list.size();
			int nonzeros = 0;
			double tol = 1e-13;

			for (double d : list)
			{
				if (Math.abs(d) > tol)
					++nonzeros;
			}

			// use dense if more than 20% nonzero
			if (nonzeros > size / 5)
				rv = toPythonList(list); // use dense list
			else
			{
				// ok use sparse list
				StringBuilder sb = new StringBuilder();
				sb.append("[");

				for (int i = 0; i < size; ++i)
				{
					double d = list.get(i);

					if (Math.abs(d) > tol)
						sb.append(ToolPrinter.doubleToString(d) + " if i == " + i + " else ");
				}

				sb.append("0.0 for i in xrange(" + size + ")]");
				rv = sb.toString();
			}

			return rv;
		}

		private String toPythonList(ArrayList<Double> list)
		{
			return "[" + StringOperations.join(", ", list.toArray(new Double[] {})) + "]";
		}

		private String toPythonListInt(ArrayList<Integer> list)
		{
			return "[" + StringOperations.join(", ", list.toArray(new Integer[] {})) + "]";
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

			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			rv.add(COMMENT_CHAR + " Variable ordering: " + nonInputVars);
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
						ArrayList<String> conds = toLinearConstraintStrings(o, ha);

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
		String passParam = SimplifyExpressionsPass.makeParam(pythonSimplify);

		new SimplifyExpressionsPass().runVanillaPass(config, passParam);

		if (config.forbidden.size() > 0)
		{
			ConvertToStandardForm.convertForbidden(config);
		}

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
		printLine("run_hylaa(define_settings())");
		decreaseIndentation();
		printNewline();
	}

	private int getVariableIndex(String name)
	{
		// get the variable's index (omits inputs)
		int rv = -1;
		int index = 0;
		Map<String, ExpressionInterval> flows = ((BaseComponent) config.root).modes.values()
				.iterator().next().flowDynamics;

		for (String var : config.root.variables)
		{
			if (flows.get(var) == null)
				continue;

			if (var.equals(name))
			{
				rv = index;
				break;
			}

			++index;
		}

		return rv;
	}

	private void printSettings()
	{
		int xDim = getVariableIndex(config.settings.plotVariableNames[0]);
		int yDim = getVariableIndex(config.settings.plotVariableNames[1]);

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

		printLine("plot_settings.plot_mode = PlotSettings." + plotMode);
		printLine("plot_settings.xdim = " + xDim);
		printLine("plot_settings.ydim = " + yDim);

		printNewline();

		double step = config.settings.spaceExConfig.samplingTime;
		double maxTime = config.settings.spaceExConfig.timeHorizon;

		if (this.step > 0)
			step = this.step;

		printLine("settings = HylaaSettings(step=" + step + ", max_time=" + maxTime
				+ ", plot_settings=plot_settings)");

		if (settings.size() > 0)
		{
			printNewline();

			for (String line : settings)
				printLine(line);
		}

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

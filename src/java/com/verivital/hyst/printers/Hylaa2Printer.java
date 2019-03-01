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

import com.verivital.hyst.geometry.Interval;
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
import com.verivital.hyst.printers.PySimPrinter.PythonPrinterCustomization;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.DynamicsUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.StringOperations;

/**
 * Printer for Python-based Hylaa. This is made for version 2 of the Hylaa tool (hybrid branch)
 * 
 * @author Stanley Bak (7-2018)
 *
 */
public class Hylaa2Printer extends ToolPrinter
{
	@Option(name = "-xdim", usage = "x axis variable name")
	public String xdim;

	@Option(name = "-ydim", usage = "y axis variable name")
	public String ydim;

	@Option(name = "-step", usage = "step size")
	public double step;

	@Option(name = "-settings", usage = "space-separated hylaa settings initialization. For example, "
			+ "'-settings plot_settings.plot_mode=PlotSettings.PLOT_FULL "
			+ "settings.deaggregation=False'", handler = StringArrayOptionHandler.class)
	public List<String> settings = new ArrayList<String>();

	private static final String COMMENT_CHAR = "#";

	public Hylaa2Printer()
	{
		this.preconditions.skip(PreconditionsFlag.NO_URGENT);

		this.preconditions.skip(PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN);
		this.preconditions.skip(PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED);
		this.preconditions.skip(PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS);

		// do the affine transformation
		this.preconditions.unskip(PreconditionsFlag.CONVERT_AFFINE_TERMS);
		this.preconditions.unskip(PreconditionsFlag.SIMPLIFY_EXPRESSIONS);
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
			this.automatonObjectName = "HybridAutomaton";
		}

		@Override
		public ArrayList<String> getImportLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("import sys");
			rv.add("import numpy as np");
			rv.add("");
			rv.add("from hylaa.hybrid_automaton import HybridAutomaton");
			rv.add("from hylaa.settings import HylaaSettings, PlotSettings");
			rv.add("from hylaa.core import Core");
			rv.add("from hylaa.stateset import StateSet");
			rv.add("from hylaa import lputil");

			return rv;
		}

		public ArrayList<String> getPrintTransitions(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// guard
			// trans = ha.new_transition(m1, m2, 'transition_name')
			// trans.set_guard([[-1, 0, 0]], [-9.9])
			// trans.set_reset(reset_csr, minkowski_csr, constraints_csr, constraints_rhs)

			if (at.label != null && at.label.length() > 0)
				rv.add("trans = ha.new_transition(" + at.from.name + ", " + at.to.name + ", '"
						+ at.label + "')");
			else
				rv.add("trans = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");

			if (at.guard == Constant.TRUE)
			{
				rv.add("trans.set_guard_true()");
			}
			else
			{
				String[] extracted = extractMatrixConstraintStrings(at.guard, at.parent);
				String matrix = extracted[0];
				String rhs = extracted[1];

				rv.add(Hylaa2Printer.COMMENT_CHAR + " " + at.guard.toDefaultString());

				rv.add("trans.set_guard(" + matrix + ", " + rhs + ")");
			}

			if (at.reset.size() > 0)
			{
				rv.add("");
				rv.add(Hylaa2Printer.COMMENT_CHAR + " Reset:");

				for (Entry<String, ExpressionInterval> e : at.reset.entrySet())
				{
					String left = e.getKey();
					String right = e.getValue().toDefaultString();
					rv.add(Hylaa2Printer.COMMENT_CHAR + " " + left + " := " + right);
				}

				rv.addAll(getResetString(at.reset, at.parent));
			}

			return rv;
		}

		/**
		 * Get the arguments for the set_reset call
		 * 
		 * @param reset
		 *            the reset assignment
		 * @param ha
		 *            the automaton
		 * @return the python string in the set_reset call
		 */
		public ArrayList<String> getResetString(Map<String, ExpressionInterval> reset,
				BaseComponent ha)
		{
			ArrayList<String> nonInputVars = DynamicsUtil
					.getNonInputVariables(ha.modes.values().iterator().next(), ha.variables);

			ArrayList<String> resetMat = new ArrayList<String>();
			ArrayList<String> minkowVariables = new ArrayList<String>();
			ArrayList<ArrayList<Double>> minkowskiBounds = new ArrayList<ArrayList<Double>>();

			for (String var : nonInputVars)
			{
				StringBuilder resetMatLine = new StringBuilder("[");
				ExpressionInterval ei = reset.get(var);

				if (ei == null)
				{
					// identity reset for this variable
					for (String other : nonInputVars)
						resetMatLine.append(other == var ? "1, " : "0, ");
				}
				else
				{
					// non-identity reset for this variable
					ArrayList<Double> row = DynamicsUtil.extractLinearVector(ei.getExpression(),
							nonInputVars);

					for (Double val : row)
						resetMatLine.append(val.toString() + ", ");

					double val = DynamicsUtil.extractLinearValue(ei.getExpression());
					Interval interval = ei.getInterval();

					if (val != 0 || interval != null)
					{
						// there is a minkowski sum term
						minkowVariables.add(var);

						double min = val + (interval == null ? 0 : interval.min);
						double max = val + (interval == null ? 0 : interval.max);

						ArrayList<Double> pair = new ArrayList<Double>();
						pair.add(min);
						pair.add(max);

						minkowskiBounds.add(pair);
					}
				}

				resetMatLine.append("], \\");
				resetMat.add(resetMatLine.toString());
			}

			// create return values from extracted values

			ArrayList<String> rv = new ArrayList<String>();

			rv.add("reset_mat = [ \\");

			for (String line : resetMat)
				rv.add("    " + line);

			rv.add("    ]");

			if (minkowVariables.size() == 0)
			{
				// reset without minkowski sum
				rv.add("trans.set_reset(reset_mat)");
			}
			else
			{
				// reset with minkowski sum

				rv.add("reset_minkowski = [ \\");

				for (String var : nonInputVars)
				{
					StringBuilder line = new StringBuilder("    [");

					for (String minVar : minkowVariables)
					{
						if (var != minVar)
							line.append("0, ");
						else
							line.append("1, ");
					}

					line.append("], \\");
					rv.add(line.toString());
				}

				rv.add("    ]");

				rv.add("minkowski_constraints = [ \\");

				for (String minVar : minkowVariables)
				{
					StringBuilder line = new StringBuilder("    [");

					/////////////// upper bound
					for (String other : minkowVariables)
					{
						if (other != minVar)
							line.append("0, ");
						else
							line.append("1, ");
					}

					line.append("], \\");
					rv.add(line.toString());

					////////////////// lower bound
					line = new StringBuilder("    [");

					for (String other : minkowVariables)
					{
						if (other != minVar)
							line.append("0, ");
						else
							line.append("-1, ");
					}

					line.append("], \\");
					rv.add(line.toString());
				}

				rv.add("    ]");

				StringBuilder line = new StringBuilder("minkowski_rhs = [ ");

				for (ArrayList<Double> bounds : minkowskiBounds)
				{
					String max = Expression.expressionPrinter
							.print(new Constant(bounds.get(1).doubleValue()));
					String negMin = Expression.expressionPrinter
							.print(new Constant(-1 * bounds.get(0).doubleValue()));

					line.append(max + ", ");
					line.append(negMin + ", ");
				}

				line.append("]");
				rv.add(line.toString());

				rv.add("trans.set_reset(reset_mat, reset_minkowski, minkowski_constraints, minkowski_rhs)");
			}

			// trans.set_reset()
			return rv;
		}

		public ArrayList<String> getPrintModeLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(am,
					am.automaton.variables);

			rv.add(am.name + " = ha.new_mode('" + am.name + "')");

			if (am != ConvertToStandardForm.getErrorMode(am.automaton))
			{
				try
				{
					if (nonInputVars.size() > 100)
						rv.addAll(getSparseDynamicsLines(am, nonInputVars));
					else
						rv.addAll(getDenseDynamicsLines(am, nonInputVars));

					rv.add(am.name + ".set_dynamics(a_matrix)");

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

			rv.add("a_inds = " + toPythonListInt(indices));
			rv.add("a_data = " + toPythonList(data));

			rv.add("a_matrix = [[0 for _ in range(size)] for _ in range size]");
			rv.add("");
			rv.add("for i in range(len(a_inds)):");
			rv.add("    row = a_inds[i] / " + size);
			rv.add("    col = a_inds[i] % " + size);
			rv.add("    a_matrix[row][col] = a_data[i]");

			return rv;
		}

		private ArrayList<String> getDenseDynamicsLines(AutomatonMode am,
				ArrayList<String> nonInputVars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("a_matrix = [ \\");

			for (int i = 0; i < nonInputVars.size(); ++i)
				rv.add("    " + toPythonList(DynamicsUtil.extractDynamicsMatrixARow(am, i))
						+ ", \\");

			rv.add("    ]");

			return rv;
		}

		public ArrayList<String> getExtraDeclarationPrintLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			AutomatonMode someMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(someMode,
					ha.variables);

			rv.add("# dynamics variable order: " + nonInputVars);

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

			Expression nonInputInvariant = null;

			for (Operation o : parts)
			{
				// only consider invariant constraints which do not contain input variables
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
				if (nonInputInvariant == null)
					nonInputInvariant = o;
				else
					nonInputInvariant = new Operation(Operator.AND, nonInputInvariant, o);
			}

			// if there was an invariant
			if (nonInputInvariant != null)
			{
				// m1.set_invariant([[1, 0, 0]], [9.9])
				String[] extracted = extractMatrixConstraintStrings(nonInputInvariant,
						am.automaton);
				String matrix = extracted[0];
				String rhs = extracted[1];

				rv.add("# " + nonInputInvariant.toDefaultString());
				rv.add(am.name + ".set_invariant(" + matrix + ", " + rhs + ")");
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

			rv.add(am.name
					+ ".set_inputs(b_matrix, u_constraints_a, u_constraints_b, allow_constants=True)");
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
		private static void extractLinearConstraints(ArrayList<ArrayList<Double>> storeConditions,
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
		 * Convert a top-level Operation, which is a conjunction of constraints, to matrix and rhs
		 * form
		 * 
		 * @param exp
		 *            the Expression to extract from
		 * @param ha
		 *            the automaton
		 * @return a 2-tuple, matrix string and rhs string
		 */
		private static String[] extractMatrixConstraintStrings(Expression exp, BaseComponent ha)
		{
			assert exp != null;
			ArrayList<Operation> parts = DynamicsUtil.splitConjunction(exp);

			StringBuilder mat = new StringBuilder("[");
			StringBuilder matRhs = new StringBuilder("[");

			for (Operation part : parts)
			{
				String[] extracted = toLinearConstraintStrings(part, ha);

				String submatrix = extracted[0];
				String subRhs = extracted[1];

				mat.append(submatrix);
				matRhs.append(subRhs);
			}

			mat.append("]");
			matRhs.append("]");

			return new String[] { mat.toString(), matRhs.toString() };
		}

		/**
		 * Convert a SINGLE operation (not conjunction) to PART of a string for describing the
		 * constraints in "Ax <= rhs" form
		 * 
		 * This function returns two Strings, the first describing A, the second describing rhs
		 * 
		 * @param o
		 *            the root of the operation
		 * @param ha
		 *            the automaton object
		 * @return a 2-tuple of Strings
		 */
		private static String[] toLinearConstraintStrings(Operation o, BaseComponent ha)
		{
			AutomatonMode anyMode = ha.modes.values().iterator().next();
			ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(anyMode,
					ha.variables);

			ArrayList<ArrayList<Double>> conditions = new ArrayList<ArrayList<Double>>();
			ArrayList<Double> vals = new ArrayList<Double>();

			extractLinearConstraints(conditions, vals, o, nonInputVars);

			StringBuilder mat = new StringBuilder("");
			StringBuilder rhs = new StringBuilder("");

			for (int i = 0; i < vals.size(); ++i)
			{
				ArrayList<Double> leftVec = conditions.get(i);
				double rightVal = vals.get(i);

				if (nonInputVars.size() <= 100)
					mat.append(toPythonList(leftVec));
				else
					mat.append(toSparsePythonList(leftVec));

				mat.append(", ");

				rhs.append(ToolPrinter.doubleToString(rightVal) + ", ");
			}

			return new String[] { mat.toString(), rhs.toString() };
		}

		private static String toSparsePythonList(ArrayList<Double> list)
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

				sb.append("0.0 for i in range(" + size + ")]");
				rv = sb.toString();
			}

			return rv;
		}

		private static String toPythonList(ArrayList<Double> list)
		{
			return "[" + StringOperations.join(", ", list.toArray(new Double[] {})) + "]";
		}

		private static String toPythonListInt(ArrayList<Integer> list)
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
			rv.add("'''returns a list of StateSet objects'''");

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

				try
				{
					String[] extracted = extractMatrixConstraintStrings(exp, ha);
					rv.add(Hylaa2Printer.COMMENT_CHAR + " " + exp.toDefaultString());
					rv.add("mode = ha.modes['" + modeName + "']");

					String[] parts = extracted[0].split("\\], \\[");

					rv.add("mat = " + parts[0] + "], \\");

					for (int i = 1; i < parts.length - 1; ++i)
						rv.add("    [" + parts[i] + "], \\");

					rv.add("    [" + parts[parts.length - 1]);

					rv.add("rhs = " + extracted[1]);
					rv.add("rv.append(StateSet(lputil.from_constraints(mat, rhs, mode), mode))");
					rv.add("");
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

	}

	@Override
	protected void printAutomaton()
	{
		// convert urgent transitions
		AutomatonUtil.convertUrgentTransitions((BaseComponent) config.root, config);

		if (config.forbidden.size() > 0)
		{
			ConvertToStandardForm.convertForbidden(config);
		}

		this.printCommentHeader();

		printNewline();

		printLine(PySimPrinter.automatonToString(config, new HylaaExtraPrintFuncs()));

		printLine("def define_settings(image_path):");
		increaseIndentation();
		printSettings();
		decreaseIndentation();
		printNewline();

		printLine("def run_hylaa(image_path):");
		increaseIndentation();
		printLine("'runs hylaa, returning a HylaaResult object'");
		printLine("ha = define_ha()");
		printLine("init = define_init_states(ha)");
		printLine("settings = define_settings(image_path)");
		printNewline();
		printLine("result = Core(ha, settings).run(init)");
		printNewline();
		printLine("return result");
		decreaseIndentation();
		printNewline();

		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("image_path = None");
		printLine("");
		printLine("if len(sys.argv) > 1 and sys.argv[1].endswith('.png'):");
		increaseIndentation();
		printLine("image_path = sys.argv[1]");
		decreaseIndentation();
		printLine("");
		printLine("run_hylaa(image_path)");
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
		printLine("'''get the hylaa settings object");
		printLine("see hylaa/settings.py for a complete list of reachability settings'''");
		printNewline();

		double step = config.settings.spaceExConfig.samplingTime;
		double maxTime = config.settings.spaceExConfig.timeHorizon;

		if (this.step > 0)
			step = this.step;

		printLine(Hylaa2Printer.COMMENT_CHAR + " step_size = " + step + ", max_time = " + maxTime);
		printLine("settings = HylaaSettings(" + step + ", " + maxTime + ")");

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

		printLine("settings.plot.plot_mode = PlotSettings.PLOT_NONE");
		printLine("");

		printLine("if image_path is not None:");
		increaseIndentation();
		printLine("settings.plot.filename = image_path");
		printLine("settings.plot.plot_mode = PlotSettings.PLOT_IMAGE");
		printLine("settings.plot.xdim_dir = " + xDim);
		printLine("settings.plot.ydim_dir = " + yDim);
		decreaseIndentation();

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
		return "Hylaa2";
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

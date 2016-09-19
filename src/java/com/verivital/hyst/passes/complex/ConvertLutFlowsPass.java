package com.verivital.hyst.passes.complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.HyperRectangleCornerEnumerator;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.StringOperations;

/**
 * A model transformation pass which converts look-up tables
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class ConvertLutFlowsPass extends TransformationPass
{
	public static int MAX_CONVERSIONS = 1000;

	@Override
	public String getCommandLineFlag()
	{
		return "convertluts";
	}

	@Override
	public String getName()
	{
		return "Convert Look-Up-Tables Pass";
	};

	@Override
	protected void runPass()
	{
		if (!(config.root instanceof BaseComponent))
			throw new AutomatonExportException(
					"Only BaseComponents are supported until the IR is updated to support"
							+ "checking if a mode is initial (github issue #10).");

		convertLuts(config.root);
	}

	public static int SIMPLIFY_PYTHON = 0;
	public static int SIMPLIFY_INTERNAL = 1;
	public static int SIMPLIFY_NONE = 2;

	public static int simplifyMode = SIMPLIFY_PYTHON;

	public ConvertLutFlowsPass()
	{
		// this pass is doing the conversion, if we don't skip we'd have an
		// infinite loop
		preconditions.skip(PreconditionsFlag.CONVERT_BASIC_OPERATORS);

		// network automata are supported
		preconditions.skip(PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON);

		// urgent modes are supported
		preconditions.skip(PreconditionsFlag.NO_URGENT);

		simplifyMode = PythonBridge.hasPython() ? SIMPLIFY_PYTHON : SIMPLIFY_INTERNAL;
	}

	private void convertLuts(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			convertLutsInBaseComponent((BaseComponent) c);
		}
		else
		{
			NetworkComponent nc = (NetworkComponent) c;

			for (ComponentInstance ci : nc.children.values())
			{
				convertLuts(ci.child);
			}
		}
	}

	private void convertLutsInBaseComponent(BaseComponent ha)
	{
		// The automaton is modified in place, so we separate the process of
		// iterating the modes and finding lut dynamics
		// and the process of conversion (which modifies ha)
		int numConversions = 0;

		while (true)
		{
			// find a mode which contains a LUT in its dynamics
			AutomatonMode foundMode = null;
			String foundVar = null;
			LutExpression foundLut = null;

			for (AutomatonMode am : ha.modes.values())
			{
				if (am.flowDynamics == null) // urgent
					continue;

				for (Entry<String, ExpressionInterval> entry : am.flowDynamics.entrySet())
				{
					String var = entry.getKey();
					Expression exp = entry.getValue().getExpression();

					// look for a lut in the flow
					foundLut = getLutSubexpression(exp);

					if (foundLut != null)
					{
						foundMode = am;
						foundVar = var;
						break;
					}
				}

				if (foundLut != null)
					break;
			}

			// no luts in dynamics, break
			if (foundLut == null)
				break;

			// convert the found lut
			convertFlowInMode(foundMode, foundVar, foundLut);

			if (++numConversions > MAX_CONVERSIONS)
				throw new AutomatonExportException(
						"Reached limit of LUT conversions per hybrid automaton: "
								+ MAX_CONVERSIONS);
		}
		;
	}

	/**
	 * Look for a lut subexpression in a given expression. If it exists, return it. Else return
	 * null.
	 * 
	 * @return the lut subexpression or null
	 */
	private LutExpression getLutSubexpression(Expression e)
	{
		LutExpression rv = null;

		if (e instanceof LutExpression)
			rv = (LutExpression) e;
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			for (Expression c : o.children)
			{
				rv = getLutSubexpression(c);

				if (rv != null)
					break;
			}
		}

		return rv;
	}

	/**
	 * Convert a lut expression which is part of a flow. This modifies am's parent base-component.
	 * 
	 * @param am
	 *            the mode where we're applying the conversiopn
	 * @param variable
	 *            the lut is in which variable's derivative
	 * @param lut
	 *            the LUT sub-expression to convert
	 */
	public void convertFlowInMode(AutomatonMode am, String variable, LutExpression lut)
	{
		ArrayList<AutomatonMode> newModes = createLutModes(am, lut);

		createDynamicsAndTransitions(am, variable, lut);

		fixOutgoingTransitions(am, newModes);

		makeOriginalModeUrgent(am, newModes);
	}

	/**
	 * Should this value be skipped? We create modes between two table values, i and i + 1, so if
	 * the current value is the last one in any dimension, we skip it
	 * 
	 * @param indexList
	 *            the index
	 * @param m
	 *            the table data
	 * @return true iff we're at the last entry in any dimension
	 */
	private boolean shouldSkip(int[] indexList, MatrixExpression m)
	{
		boolean skip = false;

		for (int d = 0; d < indexList.length; ++d)
		{
			if (indexList[d] == m.getDimWidth(d) - 1)
			{
				skip = true;
				break;
			}
		}

		return skip;
	}

	/**
	 * Make the original mode an initial one with transitions to each of the newly-created ones
	 * 
	 * @param am
	 *            the original mode
	 * @param newModes
	 *            the newly-created modes
	 */
	private void makeOriginalModeUrgent(AutomatonMode am, Collection<AutomatonMode> newModes)
	{
		BaseComponent ha = am.automaton;
		am.flowDynamics = null;
		am.urgent = true;

		for (AutomatonMode newMode : newModes)
		{
			AutomatonTransition at = ha.createTransition(am, newMode);
			at.guard = newMode.invariant.copy();
		}
	}

	/**
	 * Copy the outgoing transitions to each of the created modes
	 * 
	 * @param am
	 *            the original mode
	 * @param newModes
	 *            the new modes in the table
	 */
	private void fixOutgoingTransitions(AutomatonMode am, Collection<AutomatonMode> newModes)
	{
		BaseComponent ha = am.automaton;
		ArrayList<AutomatonTransition> outgoing = new ArrayList<AutomatonTransition>();

		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from.name.equals(am.name))
				outgoing.add(at);
		}

		ha.transitions.removeAll(outgoing);

		for (AutomatonTransition at : outgoing)
		{
			for (AutomatonMode mode : newModes)
			{
				// copy transition at
				AutomatonTransition newAt = at.copy(ha);
				newAt.from = mode; // change where the transition is coming from
			}
		}
	}

	/**
	 * Create the AutomatonModes corresponding to this lut. This does not create dynamics/invariants
	 * or transitions.
	 * 
	 * @param original
	 *            the original location
	 * @param variableWithLut
	 *            the variable with dynamics that contains a look up table
	 * @param lut
	 *            the subexpression we're splitting
	 * @return the created modes
	 */
	private ArrayList<AutomatonMode> createLutModes(AutomatonMode original, LutExpression lut)
	{
		ArrayList<AutomatonMode> rv = new ArrayList<AutomatonMode>();
		BaseComponent ha = original.automaton;

		for (Entry<int[], Expression> e : lut.table)
		{
			int[] indexList = e.getKey();

			if (shouldSkip(indexList, lut.table))
				continue;

			// shouldn't skip, construct mode
			AutomatonMode am = ha
					.createMode(original.name + "_" + StringOperations.join("_", indexList));

			rv.add(am);
		}

		return rv;
	}

	/**
	 * Create the dynamics, invariants, and transitions among (already-created) lut modes.
	 * 
	 * @param original
	 *            the original mode
	 * @param variable
	 *            the variable who's flow contains a LUT subexpression
	 * @param lut
	 *            the LUT subexpression to replace
	 */
	private void createDynamicsAndTransitions(AutomatonMode original, String variableWithLut,
			LutExpression lut)
	{
		BaseComponent ha = original.automaton;
		int tableDims = lut.table.getNumDims();

		for (Entry<int[], Expression> e : lut.table)
		{
			int[] indexList = e.getKey();

			if (shouldSkip(indexList, lut.table))
				continue;

			String name = original.name + "_" + StringOperations.join("_", indexList);
			AutomatonMode am = ha.modes.get(name);
			am.invariant = original.invariant.copy();

			Interval[] rangeList = new Interval[tableDims];

			// create dynamics for all other variables
			for (String var : ha.variables)
			{
				if (!var.equals(variableWithLut))
				{
					// copy dynamics
					am.flowDynamics.put(var, original.flowDynamics.get(var).copy());
				}
			}

			// this loop populates range list, which is used to create the
			// dynamics
			for (int varIndex = 0; varIndex < tableDims; ++varIndex)
			{
				int indexInTable = indexList[varIndex];
				double[] breakpoints = lut.breakpoints[varIndex];
				Constant leftBreakpoint = new Constant(breakpoints[indexInTable]);
				Constant rightBreakpoint = new Constant(breakpoints[indexInTable + 1]); // in
																						// bounds
																						// because
																						// shouldSkip
																						// was
																						// false

				rangeList[varIndex] = new Interval(leftBreakpoint.getVal(),
						rightBreakpoint.getVal());
			}

			// create dynamics for variableWithLut
			// must be done before creating transitions, since inputs may use
			// variableWithLut
			Expression replaceLutExpression = nLinearInterpolation(lut, indexList, rangeList);
			ExpressionInterval originalExpInt = original.flowDynamics.get(variableWithLut);

			Expression newFlow = replaceLutSubexpression(originalExpInt.getExpression(), lut,
					replaceLutExpression);

			Interval newI = originalExpInt.getInterval() == null ? null
					: originalExpInt.getInterval().copy();
			am.flowDynamics.put(variableWithLut, new ExpressionInterval(newFlow, newI));

			// this loop accumulates the invariant, and creates neighbor
			// transitions
			for (int varIndex = 0; varIndex < tableDims; ++varIndex)
			{
				Expression inputExpr = lut.inputs[varIndex];
				int indexInTable = indexList[varIndex];
				double[] breakpoints = lut.breakpoints[varIndex];
				Constant leftBreakpoint = new Constant(breakpoints[indexInTable]);
				Constant rightBreakpoint = new Constant(breakpoints[indexInTable + 1]); // in
																						// bounds
																						// because
																						// shouldSkip
																						// was
																						// false

				// if there's a left neighbor
				if (indexInTable > 0)
				{
					Expression inRange = new Operation(inputExpr, Operator.GREATEREQUAL,
							leftBreakpoint);

					// accumulate inRange into invariant
					am.invariant = Expression.and(am.invariant, inRange);

					// add transition to left neighbor
					Expression guard = new Operation(inputExpr, Operator.LESSEQUAL, leftBreakpoint);
					int[] leftIndexList = Arrays.copyOf(indexList, indexList.length);
					--leftIndexList[varIndex];
					String leftName = original.name + "_"
							+ StringOperations.join("_", leftIndexList);
					AutomatonMode leftMode = ha.modes.get(leftName);

					if (leftMode == null)
						throw new AutomatonExportException(
								"Left mode named '" + leftName + "' not found in automaton");

					Expression goingLeft = new Operation(Operator.LESSEQUAL,
							AutomatonUtil.derivativeOf(inputExpr, asExpressionMap(am)),
							new Constant(0));

					ha.createTransition(am, leftMode).guard = Expression.and(guard, goingLeft);
				}

				// if there's a right neighbor (3 breakpoints = 2 modes which
				// means only index 0 has a right neighbor)
				int numModes = breakpoints.length - 1;

				if (indexInTable < numModes - 1)
				{
					Expression inRange = new Operation(inputExpr, Operator.LESSEQUAL,
							rightBreakpoint);

					// accumulate inRange into invariant
					am.invariant = Expression.and(am.invariant, inRange);

					// add transition to right neighbor
					Expression guard = new Operation(inputExpr, Operator.GREATEREQUAL,
							rightBreakpoint);
					int[] rightIndexList = Arrays.copyOf(indexList, indexList.length);
					++rightIndexList[varIndex];
					String rightName = original.name + "_"
							+ StringOperations.join("_", rightIndexList);
					AutomatonMode rightMode = ha.modes.get(rightName);

					if (rightMode == null)
						throw new AutomatonExportException(
								"Right mode named '" + rightName + "' not found in automaton");

					Expression goingRight = new Operation(Operator.GREATEREQUAL,
							AutomatonUtil.derivativeOf(inputExpr, asExpressionMap(am)),
							new Constant(0));

					ha.createTransition(am, rightMode).guard = Expression.and(guard, goingRight);
				}
			}
		}
	}

	/**
	 * Return a mode's expression map from the dynamics
	 * 
	 * @param mode
	 *            the mode
	 * @return a map of variable names -> expressions
	 */
	public Map<String, Expression> asExpressionMap(AutomatonMode mode)
	{
		Map<String, Expression> rv = new HashMap<String, Expression>(mode.flowDynamics.size());

		for (Entry<String, ExpressionInterval> e : mode.flowDynamics.entrySet())
			rv.put(e.getKey(), e.getValue().asExpression());

		return rv;
	}

	/**
	 * Replace a lut subexpression with a different expression, and return the complete new
	 * expression
	 * 
	 * @param complete
	 *            the complete expression
	 * @param lut
	 *            the lut subexpression to replace
	 * @param replaceExp
	 *            the expression to use in place of lut
	 * @return a copy of the complete expression
	 */
	private Expression replaceLutSubexpression(Expression expression, LutExpression lut,
			Expression replaceLutExpression)
	{
		Expression rv = null;

		if (expression == lut)
			rv = replaceLutExpression;
		else if (expression instanceof Operation)
		{
			Operation o = expression.asOperation();
			Operation newO = new Operation(o.op);

			for (Expression child : o.children)
				newO.children.add(replaceLutSubexpression(child, lut, replaceLutExpression));

			rv = newO;
		}
		else
			rv = expression.copy();

		return rv;
	}

	/**
	 * Perform n-linear interpolation. This is a generalization of the tri-linear scheme given in
	 * http://paulbourke.net/miscellaneous/interpolation/
	 * 
	 * @param lut
	 *            the lookup table expression
	 * @param indexList
	 *            the index values for each dimension we want to interpolate, the interpolation is
	 *            done between the elements i and i + 1
	 * @param rangeList
	 *            the ranges being interpolated (from the two relevant breakpoints in each
	 *            dimension)
	 * @return an expression which is the n-linear interpolation
	 */
	public static Expression nLinearInterpolation(LutExpression lut, int[] indexList,
			Interval[] rangeList)
	{
		Expression[] inputList = lut.inputs;
		MatrixExpression table = lut.table;
		int numDims = inputList.length;
		Expression[] vars = new Expression[numDims];
		Expression[] oneMinusVars = new Expression[numDims];

		if (numDims != table.getNumDims())
			throw new AutomatonExportException(
					"passed-in variables.length must match dimensions of table values");

		if (numDims != indexList.length)
			throw new AutomatonExportException(
					"passed-in variables.length must match size of indexList");

		if (numDims != rangeList.length)
			throw new AutomatonExportException(
					"passed-in variables.length must match number of ranges");

		for (int d = 0; d < numDims; ++d)
		{
			Expression input = inputList[d].copy();
			Expression minVal = new Constant(rangeList[d].min);
			Operation width = new Operation(Operator.SUBTRACT, new Constant(rangeList[d].max),
					new Constant(rangeList[d].min));

			vars[d] = new Operation(Operator.SUBTRACT, new Operation(Operator.DIVIDE, input, width),
					new Operation(Operator.DIVIDE, minVal, width));
			oneMinusVars[d] = new Operation(Operator.SUBTRACT, new Constant(1), vars[d].copy());
		}

		InterpolateCornerEnumerator interpolateEnumerator = new InterpolateCornerEnumerator(vars,
				oneMinusVars, table, indexList);
		HyperRectangle hr = new HyperRectangle(numDims);

		for (int d = 0; d < numDims; ++d)
			hr.dims[d] = new Interval(0, 1);

		hr.enumerateCorners(interpolateEnumerator);
		Expression e = interpolateEnumerator.accumulator;

		Hyst.logDebug("nLinearInterpolation result expression for " + Arrays.toString(indexList)
				+ ": " + e.toDefaultString());

		double CHOP_TOL = 1e-8;

		if (simplifyMode == SIMPLIFY_PYTHON)
			e = PythonUtil.pythonSimplifyExpressionChop(e, CHOP_TOL);
		else if (simplifyMode == SIMPLIFY_INTERNAL)
			e = SimplifyExpressionsPass.simplifyExpression(e);

		Hyst.logDebug("after pythonSimplifyExpressionChop: " + e.toDefaultString());

		return e;
	}

	// used to accumulate the expression when performing the interpolation
	private static class InterpolateCornerEnumerator extends HyperRectangleCornerEnumerator
	{
		public Expression accumulator = null;

		private Expression[] vars;
		private Expression[] oneMinusVars;
		private MatrixExpression table;
		private int[] indexList;

		public InterpolateCornerEnumerator(Expression[] vars, Expression[] oneMinusVars,
				MatrixExpression table, int[] indexList)
		{
			this.vars = vars;
			this.oneMinusVars = oneMinusVars;
			this.table = table;
			this.indexList = indexList;
		}

		@Override
		public void enumerateWithCoord(HyperPoint p, boolean[] isMin)
		{
			int[] index = new int[isMin.length];

			for (int i = 0; i < indexList.length; ++i)
				index[i] = isMin[i] ? indexList[i] : indexList[i] + 1;

			Expression term = table.get(index);

			for (int d = 0; d < vars.length; ++d)
			{
				Expression e = null;

				if (isMin[d])
					e = oneMinusVars[d];
				else
					e = vars[d];

				term = new Operation(Operator.MULTIPLY, term, e);
			}

			// add to accumulator
			if (accumulator == null)
				accumulator = term;
			else
				accumulator = new Operation(Operator.ADD, term, accumulator);
		}
	}
}

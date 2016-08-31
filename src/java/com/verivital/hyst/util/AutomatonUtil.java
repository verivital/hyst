package com.verivital.hyst.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * Generic importer functions
 * 
 * Holds static functions for use when importing / parsing models
 *
 */
public abstract class AutomatonUtil
{
	/**
	 * Substitute a variable in an expression and return the new expression
	 * 
	 * @param e
	 *            where to do the substitution
	 * @param var
	 *            the variable name
	 * @param sub
	 *            the expression to sub in
	 * @return the new expression
	 */
	public static Expression substituteVariable(Expression e, String var, Expression sub)
	{
		Expression rv = e;

		if (e instanceof Variable)
		{
			Variable v = (Variable) e;

			if (v.name.equals(var))
				rv = sub.copy();
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			for (int i = 0; i < o.children.size(); ++i)
			{
				Expression newChild = substituteVariable(o.children.get(i), var, sub);
				o.children.set(i, newChild);
			}
		}

		return rv;
	}

	public static boolean hasUrgentMode(BaseComponent ha)
	{
		boolean rv = false;

		for (Entry<String, AutomatonMode> entry : ha.modes.entrySet())
		{
			AutomatonMode am = entry.getValue();

			if (am.urgent)
			{
				rv = true;
				break;
			}
		}

		return rv;
	}

	/**
	 * Convert all the explicit urgent transitions to implicit urgent ones as follows:
	 * 
	 * in the following, C is a small constant, say step size / 10000
	 * 
	 * Adds a variable "_urgent_clock", which is initially C, and ticks at rate 0 in every normal
	 * mode and at rate 1 in every urgent mode.
	 * 
	 * For every urgent mode, the invariant '_urgent_clock <= C' is added.
	 * 
	 * For every outgoing transition of the urgent mode, the guard '_urgent_clock <= C' is used.
	 * Also, the reset '_urgent_clock := [0, C]' is added
	 * 
	 * In every urgent mode, all variables (except _urgent_clock) tick at rate 0
	 * 
	 * @param ha
	 *            the automaton to convert
	 */
	public static void convertUrgentTransitions(BaseComponent ha, Configuration config)
	{
		if (config.root != ha)
			throw new AutomatonExportException("Flat automaton expected");

		final double C = config.settings.spaceExConfig.samplingTime / 10000;
		final String urgentClockName = AutomatonUtil.freshName("_urgent_clock", ha.getAllNames());

		if (hasUrgentMode(ha))
		{
			// add the urgent variable to the list of variable names
			ha.variables.add(urgentClockName);

			// add initial assignment
			Operation ge = new Operation(Operator.GREATEREQUAL, new Variable(urgentClockName),
					new Constant(0));
			Operation le = new Operation(Operator.LESSEQUAL, new Variable(urgentClockName),
					new Constant(C));

			Operation urgAssign = (Operation) Expression.and(ge, le);

			for (Entry<String, Expression> e : config.init.entrySet())
			{
				Expression newE = new Operation(Operator.AND, e.getValue(), urgAssign.copy());
				e.setValue(newE);
			}

			Operation urgCondition = new Operation(Operator.LESSEQUAL,
					new Variable(urgentClockName), new Constant(C));

			for (Entry<String, AutomatonMode> entry : ha.modes.entrySet())
			{
				AutomatonMode am = entry.getValue();

				if (am.urgent)
				{
					// update flow
					am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>();

					for (String var : ha.variables)
						am.flowDynamics.put(var, new ExpressionInterval(new Constant(0)));

					am.flowDynamics.put(urgentClockName, new ExpressionInterval(new Constant(1)));

					// update invariant
					am.invariant = Expression.and(am.invariant, urgCondition.copy());

					// update urgent flag
					am.urgent = false;

					// update transitions
					for (AutomatonTransition at : ha.transitions)
					{
						if (at.to == am)
						{
							// transition to urgent mode
							at.guard = urgCondition.copy();
							at.reset.put(urgentClockName,
									new ExpressionInterval(new Constant(0), new Interval(0, C)));
						}
						else if (at.from == am)
						{
							// transition from urgent mode
							at.reset.put(urgentClockName, new ExpressionInterval(new Constant(0)));
						}
					}
				}
				else
					am.flowDynamics.put(urgentClockName, new ExpressionInterval(new Constant(0)));
			}

			ha.validate();
		}
	}

	public static LinkedHashMap<String, ExpressionInterval> extractReset(Expression exp,
			Collection<String> vars)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();

		if (exp != null)
		{
			rv.putAll(extractExactAssignments(vars, exp));
			exp = removeExactAssignments(exp, rv.keySet());

			if (exp != null)
			{
				// also check for variable ranges for non-equal variables
				for (String v : vars)
				{
					if (!rv.containsKey(v))
					{
						// there wasn't a direct assignment to this variable
						// we also can support non-deterministic resets to a
						// range of constants; check for that
						Interval interval = null;

						try
						{
							interval = RangeExtractor.getVariableRange(exp, v);

							if (interval != null)
							{
								if (interval.isOpenInterval())
									throw new AutomatonExportException(
											"Open intervals are not permitted in nondeterministic resets: "
													+ v + " := " + interval + " (from exp "
													+ exp.toDefaultString() + ")");

								rv.put(v, new ExpressionInterval(new Constant(0.0), interval));
							}
						}
						catch (EmptyRangeException e)
						{
							// ignore this variable, it's not in the reset at
							// all
						}
						catch (ConstantMismatchException e)
						{
							throw new AutomatonExportException("Reset contains constant mismatch",
									e);
						}
						catch (UnsupportedConditionException e)
						{
							throw new AutomatonExportException(
									"Reset contains noninterval assignment", e);
						}
					}
				}
			}
		}

		return rv;
	}

	/**
	 * Remove all the sub-expressions with exact assignments to a given set of variables and return
	 * the modified expression
	 * 
	 * @param e
	 *            the initial expression
	 * @param vars
	 *            the variables to remove
	 * @return the modified expressions with all assignments to any variable in vars removed, can be
	 *         null
	 */
	private static Expression removeExactAssignments(Expression e, Collection<String> vars)
	{
		Expression rv = e;

		Operation o = e.asOperation();

		if (o.op == Operator.AND)
		{
			Expression left = removeExactAssignments(o.getLeft(), vars);
			Expression right = removeExactAssignments(o.getRight(), vars);

			if (left == null)
				rv = right;
			else if (right == null)
				rv = left;
			else
				rv = new Operation(Operator.AND, left, right);

		}
		else if (o.op == Operator.EQUAL)
		{
			Expression left = o.getLeft();

			if (left instanceof Variable)
			{
				String var = ((Variable) left).name;

				if (vars.contains(var))
					rv = null;
			}
		}

		return rv;
	}

	/**
	 * Extract exact assignments from an expression
	 * 
	 * @param vars
	 *            the list of variables
	 * @param e
	 *            the expression to extract from
	 * @return a mapping of variable name -> ExpressionInterval for all exact assignments in the
	 *         expression
	 */
	public static Map<String, ExpressionInterval> extractExactAssignments(Collection<String> vars,
			Expression e)
	{
		Map<String, ExpressionInterval> rv = new HashMap<String, ExpressionInterval>();

		try
		{
			extractExactAssignmentsRec(rv, vars, e);
		}
		catch (AutomatonExportException ex)
		{
			throw new AutomatonExportException(
					"Error while extracting exact assignments from expression: "
							+ e.toDefaultString(),
					ex);
		}

		return rv;
	}

	/**
	 * Extract exact assignments from an expression (recursive version, use extractExactAssignments
	 * if calling)
	 * 
	 * @param rv
	 *            [out] where to store the output
	 * @param vars
	 *            the list of variables
	 * @param e
	 *            the expression to extract from
	 */
	private static void extractExactAssignmentsRec(Map<String, ExpressionInterval> rv,
			Collection<String> vars, Expression e)
	{
		Operation o = e.asOperation();

		if (o.op == Operator.AND)
		{
			extractExactAssignmentsRec(rv, vars, o.getLeft());
			extractExactAssignmentsRec(rv, vars, o.getRight());
		}
		else if (o.op == Operator.EQUAL)
		{
			Expression leftE = o.getLeft();

			// check if it's a nondeterministic assignment (spaceex-style havoc
			// reset)
			if (leftE instanceof Operation && ((Operation) leftE).op == Operator.MULTIPLY
					&& ((Operation) leftE).getLeft().equals(new Constant(0))
					&& ((Operation) leftE).getRight() instanceof Variable
					&& o.getRight().equals(new Constant(0)))
			{
				// 0.0 * variableName = 0.0

				Variable v = (Variable) ((Operation) leftE).getRight();

				rv.put(v.name, new ExpressionInterval(new Constant(0),
						Interval.NONDETERMINISTIC_ASSIGNMENT_INTERVAL));

				Hyst.logDebug("nondeterministic assignment extracted from reset for variable: "
						+ v.name + ", expression = " + e);
			}
			else
			{
				if (!(leftE instanceof Variable))
					throw new AutomatonExportException(
							"Expecting variable on left side of assignment: " + e);

				// left is variable, right is expression
				Variable v = (Variable) leftE;

				if (!vars.contains(v.name))
					throw new AutomatonExportException(
							"Variable defined in reset, but not in automaton: " + v.name);

				ExpressionInterval ei = rv.get(v.name);
				ExpressionInterval newConstraint = new ExpressionInterval(o.getRight());

				if (ei == null)
					rv.put(v.name, newConstraint);
				else if (!ei.equals(newConstraint)) // it's not a repeated
													// assignment to the same
													// value
				{
					throw new AutomatonExportException(
							"Reset contains multiple possibly differing assignments for variable "
									+ v.name + ": " + ei.toDefaultString() + " and "
									+ newConstraint.toDefaultString());
				}
			}
		}
		// else it's a non-exact assignment or conjunction, skip
	}

	/**
	 * Modify flow to include the derivatives in e
	 * 
	 * @param allowedVariables
	 *            the set of allowed variables, use null if any variable is fine
	 * @param flow
	 *            the flow to assign to
	 * @param e
	 *            expression
	 */
	public static LinkedHashMap<String, Expression> parseFlowExpression(
			Collection<String> allowedVariables, Expression e)
	{
		LinkedHashMap<String, Expression> rv = new LinkedHashMap<String, Expression>();

		if (e != null && simplifyExpression(e) != Constant.FALSE)
			parseFlowExpressionRecursive(allowedVariables, rv, e,
					DefaultExpressionPrinter.instance.print(e));

		return rv;
	}

	public static Expression simplifyExpression(Expression e)
	{
		return SimplifyExpressionsPass.simplifyExpression(e);
	}

	/**
	 * Modify flow to include the derivatives in e
	 * 
	 * @param flow
	 *            the flow to assign to
	 * @param e
	 *            expression
	 */
	public static LinkedHashMap<String, Expression> parseFlowExpression(Expression e)
	{
		return parseFlowExpression(null, e);
	}

	/**
	 * Modify flow to include the derivatives in e
	 * 
	 * @param allowedVariables
	 *            the set of allowed variables
	 * @param flow
	 *            the flow to assign to
	 * @param e
	 *            current subexpression
	 * @param original
	 *            original expression
	 */
	private static void parseFlowExpressionRecursive(Collection<String> allowedVariables,
			java.util.Map<String, Expression> flow, Expression e, String original)
	{
		Operation o = e.asOperation();

		if (o.op == Operator.AND)
		{
			parseFlowExpressionRecursive(allowedVariables, flow, o.getLeft(), original);
			parseFlowExpressionRecursive(allowedVariables, flow, o.getRight(), original);
		}
		else if (o.op == Operator.EQUAL)
		{
			// left is variable, right is expression
			Variable v = (Variable) o.getLeft();

			if (allowedVariables != null && !allowedVariables.contains(v.name))
				throw new AutomatonExportException(
						"Unknown variable used in flow: " + v.name + " in " + original);

			if (flow.get(v.name) != null)
			{
				String one = flow.get(v.name).toString();
				String two = o.getRight().toString();
				throw new AutomatonExportException(
						"Variable controlled by two differential equations: " + v.name + " = (1) "
								+ one + " as well as (2) " + two);
			}

			flow.put(v.name, o.getRight().copy());
		}
	}

	public static Set<String> getVariablesInExpression(Expression e)
	{
		HashSet<String> rv = new HashSet<String>();

		if (e instanceof Variable)
			rv.add(((Variable) e).name);
		else if (e instanceof Operation)
		{
			for (Expression c : e.asOperation().children)
				rv.addAll(getVariablesInExpression(c));
		}

		return rv;
	}

	/**
	 * Get the center point of the first initial mode
	 * 
	 * @param ha
	 *            the hybrid automaton
	 * @return the center point of the first initial mode, in the order of ha.variablesNames
	 */
	public static HyperPoint getInitialPoint(BaseComponent ha, Configuration config)
	{
		if (config.root != ha)
			throw new AutomatonExportException("expected flat automaton");

		// start in the middle of the initial state set
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		try
		{
			RangeExtractor.getVariableRanges(config.init.values().iterator().next(), ranges);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException("Could not determine ranges for inital values.", e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException("Constant mismatch in initial values.", e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException("Initial states contain noninterval assignment", e);
		}

		int numVars = ha.variables.size();
		double[] rv = new double[numVars];

		for (int i = 0; i < numVars; ++i)
		{
			String var = ha.variables.get(i);
			Interval in = ranges.get(var);

			if (in == null)
				rv[i] = 0;
			else
				rv[i] = in.middle();
		}

		return new HyperPoint(rv);
	}

	/**
	 * Get a fresh (unused name), while avoiding some other names. Will add a counter to the name
	 * until there's no longer a conflict
	 * 
	 * @param desired
	 *            the desired name
	 * @param avoid
	 *            the names to avoid
	 * @return a fresh name, based off of desired, not in avoid
	 */
	public static String freshName(String desired, Collection<String> avoid)
	{
		String rv = desired;
		int counter = 2;

		while (avoid.contains(rv))
			rv = desired + (counter++);

		return rv;
	}

	/**
	 * Does this mode exist in the hybrid automaton?
	 * 
	 * @param modeName
	 *            the name of the mode
	 * @param c
	 *            the component to check
	 * @return if the mode exists in the automaton
	 */
	public static boolean modeExistsInComponent(String modeName, Component c)
	{
		// if c is a network component, modename is going to be a dotted version
		// with a mode from each child, in order
		// imagine c has three children, child1(base), child2(network), and
		// child3(base)
		// child2 has two children, subchild1(base), and subchild2(base)
		// then, a mode name might be:
		// child1_mode.subchild1_mode1.subchild2_mode.child3_mode
		// this, the mode name is like a inorder or preorder traversal on the
		// tree, with only the leaves

		return "".equals(checkModeExistsRec(c, modeName));
	}

	/**
	 * Check if the mode exists in the automaton recursively. This returns a String, which is the
	 * remaining part of modeName which was not processed when doing the check, or null if no mode
	 * exists with that name
	 * 
	 * @param c
	 *            the component currently being checked
	 * @param modeName
	 *            the mode name (preorder traversal ordering, so it may contain an extra suffix)
	 * @return the unprocessed suffix (if the mode was found), or null if the mode was not found
	 */
	private static String checkModeExistsRec(Component c, String modeName)
	{
		String rv = null;

		if (c instanceof BaseComponent) // base case
			rv = checkModeExistsBase((BaseComponent) c, modeName);
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent) c;

			for (ComponentInstance ci : nc.children.values())
			{
				// modeName gets shorter here as we 'pop' modes off the front of
				// the string
				modeName = checkModeExistsRec(ci.child, modeName);

				if (modeName == null)
					break;
			}

			if (modeName != null)
				rv = modeName;
		}

		return rv;
	}

	/**
	 * Check if the mode name exists in the base component (part of recursive call)
	 * 
	 * @param c
	 *            the base component to check
	 * @param modeName
	 *            the mode name being checked (preorder traversal name)
	 * @return the remaining string of modeName (if exists), or null (if doesn't exist)
	 */
	private static String checkModeExistsBase(BaseComponent c, String modeName)
	{
		String rv = null;
		String toCheck = modeName;
		int i = modeName.indexOf(".");

		if (i != -1)
			toCheck = toCheck.substring(0, i);

		if (c.modes.containsKey(toCheck))
		{
			rv = modeName.substring(toCheck.length());

			// remove the leading dot if it exists
			if (rv.startsWith("."))
				rv = rv.substring(1);
		}

		return rv;
	}

	/**
	 * Get a string representation of a mapping from variable names to expression intervals
	 * 
	 * @param map
	 *            the map to print
	 * @return a string representation
	 */
	public static String getMapExpressionIntervalString(Map<String, ExpressionInterval> map)
	{
		String rv = "null";

		if (map != null)
		{
			rv = "";

			for (Entry<String, ExpressionInterval> e : map.entrySet())
			{
				if (rv.length() > 0)
					rv += ", ";

				String valueString = "<null>";

				if (e.getValue() != null)
					valueString = e.getValue().toString(DefaultExpressionPrinter.instance);

				rv += e.getKey() + ": " + valueString;
			}
		}

		return rv;
	}

	/**
	 * Get a string representation of a mapping from variable names to expression intervals
	 * 
	 * @param map
	 *            the map to print
	 * @return a string representation
	 */
	public static String getMapExpressionString(Map<String, Expression> map)
	{
		String rv = "null";

		if (map != null)
		{
			rv = "";

			for (Entry<String, Expression> e : map.entrySet())
			{
				if (rv.length() > 0)
					rv += ", ";

				rv += e.getKey() + ": " + DefaultExpressionPrinter.instance.print(e.getValue());
			}
		}

		return rv;
	}

	/**
	 * Extract arguments from a String. Quotes are escaped as expected from:
	 * http://stackoverflow.com/questions/7804335/split-string-on-spaces-in-java
	 * -except-if-between-quotes-i-e-treat-hello-wor
	 * 
	 * @param s
	 *            the original string
	 * @return the parsed arguments
	 */
	public static String[] extractArgs(String s)
	{
		ArrayList<String> list = new ArrayList<String>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(s);

		while (m.find())
			list.add(m.group(1).replace("\"", ""));

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Estimate the Jacobian matrix using sampling
	 * 
	 * @param dy
	 *            the dynamics
	 * @param bounds
	 *            the bounds inside which we should estimate
	 * @return the estimated Jacobian Matrix
	 */
	public static double[][] estimateJacobian(LinkedHashMap<String, ExpressionInterval> dy,
			HashMap<String, Interval> bounds)
	{
		final int NUM_VARS = dy.size();
		double[][] rv = new double[NUM_VARS][NUM_VARS];
		ArrayList<String> variables = new ArrayList<String>();
		variables.addAll(dy.keySet());

		HyperPoint center = boundsCenter(bounds, variables);

		for (int y = 0; y < NUM_VARS; ++y)
		{
			String derVariable = variables.get(y);
			Expression derFunc = dy.get(derVariable).getExpression();

			for (int x = 0; x < NUM_VARS; ++x)
			{
				String partialDerVar = variables.get(x); // the partial
															// derivative
															// variable
				double sampleOffset = bounds.get(partialDerVar).width() / 2;
				HyperPoint left = new HyperPoint(center);
				HyperPoint right = new HyperPoint(center);

				left.dims[x] -= sampleOffset;
				right.dims[x] += sampleOffset;

				double leftVal = AutomatonUtil.evaluateExpression(derFunc, left, variables);
				double rightVal = AutomatonUtil.evaluateExpression(derFunc, right, variables);

				rv[y][x] = (rightVal - leftVal) / (2 * sampleOffset);
			}
		}

		return rv;
	}

	/**
	 * Get the center point of a collection of interval bounds
	 * 
	 * @param bounds
	 *            the bounds intervals
	 * @param variables
	 *            the variable, in the order we want
	 * @return a HyperPoint at the center
	 */
	public static HyperPoint boundsCenter(HashMap<String, Interval> bounds,
			ArrayList<String> variables)
	{
		HyperPoint rv = new HyperPoint(bounds.size());
		int dim = 0;

		for (String v : variables)
			rv.dims[dim++] = bounds.get(v).middle();

		return rv;
	}

	/**
	 * Check whether a variable is an output variable of a component. It is an output variable if
	 * either a flow is defined for it, or if a reset exists where the variable is assigned.
	 * 
	 * @param bc
	 *            the base component
	 * @param varName
	 *            the variable name
	 * @return true iff the passed-in variable is an output variable
	 */
	public static boolean isOutputVariable(BaseComponent bc, String varName)
	{
		boolean rv = false;

		// if flow is defined
		for (AutomatonMode am : bc.modes.values())
		{
			if (am.urgent)
				continue;

			rv = am.flowDynamics.keySet().contains(varName);
			break;
		}

		if (!rv)
		{
			// if a reset is defined
			for (AutomatonTransition at : bc.transitions)
			{
				if (at.reset.containsKey(varName))
				{
					rv = true;
					break;
				}
			}
		}

		return rv;
	}

	// these are the flags for the bitmask returned by classifyExpression
	public static final byte OPS_LINEAR = 1 << 0; // operators like add,
													// subtract, multiply,
													// negative
	public static final byte OPS_NONLINEAR = 1 << 1; // division,
														// exponentiation, or
														// sine, sqrt, ln, ...
	public static final byte OPS_BOOLEAN = 1 << 2; // boolean expression
													// operators (==, >=, &&,
													// ||, !)
	public static final byte OPS_LOC = 1 << 3; // loc() functions
	public static final byte OPS_LUT = 1 << 4; // look up table subexpressions
	public static final byte OPS_MATRIX = 1 << 5; // matrix subexpressions
	public static final byte OPS_DISJUNCTION = 1 << 6; // or operator (||)

	/**
	 * Classify an Expression's operators. This returns a bitmask, which you can use to check for
	 * various parts of the expression. For example, val = classifyExpression(e); if (val |
	 * NONLINEAR) {expression has nonlinear operators}.
	 * 
	 * Each category is classified separately, for example classifying 'x^2' will set HAS_NONLINEAR,
	 * but not HAS_LINEAR, since there are no linear operations in the expression.
	 * 
	 * Notice that the operator classification is NOT the same as the expression classification, for
	 * example "x*y" is a nonlinear expression, but only uses linear operators (multiplication)
	 * 
	 * @param e
	 *            the expression to check
	 * @return a bitmask composed of OPS_* values binary or'd ('|') together like (OPS_LINEAR |
	 *         OPS_NONLINEAR)
	 */
	public static byte classifyExpressionOps(Expression e)
	{
		byte rv = 0;

		final Collection<Operator> LINEAR_OPS = Arrays.asList(new Operator[] { Operator.ADD,
				Operator.SUBTRACT, Operator.MULTIPLY, Operator.NEGATIVE });

		final Collection<Operator> BOOLEAN_OPS = Arrays.asList(new Operator[] { Operator.AND,
				Operator.OR, Operator.EQUAL, Operator.LESS, Operator.GREATER, Operator.LESSEQUAL,
				Operator.GREATEREQUAL, Operator.NOTEQUAL, Operator.LOGICAL_NOT });

		final Collection<Operator> NONLINEAR_OPS = Arrays
				.asList(new Operator[] { Operator.POW, Operator.DIVIDE, Operator.COS, Operator.SIN,
						Operator.SQRT, Operator.TAN, Operator.EXP, Operator.LN });

		Operation o = e.asOperation();

		if (o != null)
		{
			if (LINEAR_OPS.contains(o.op))
				rv |= OPS_LINEAR;

			if (NONLINEAR_OPS.contains(o.op))
				rv |= OPS_NONLINEAR;

			if (BOOLEAN_OPS.contains(o.op))
				rv |= OPS_BOOLEAN;

			if (o.op == Operator.LOC)
				rv |= OPS_LOC;

			if (o.op == Operator.OR)
				rv |= OPS_DISJUNCTION;

			for (Expression child : o.children)
				rv |= classifyExpressionOps(child);
		}
		else if (e == Constant.TRUE || e == Constant.FALSE)
			rv |= OPS_BOOLEAN;
		else if (e instanceof MatrixExpression)
			rv |= OPS_MATRIX;
		else if (e instanceof LutExpression)
			rv |= OPS_LUT;

		return rv;
	}

	/**
	 * Check if an Expression contains only operations from a set of allowed classes (linear,
	 * nonlinear, ect.)
	 * 
	 * @param e
	 *            the expression
	 * @param allowedClasses
	 *            a list of classes which are allowed (from the HAS_* constants), like HAS_LINEAR,
	 *            HAS_NONLINEAR
	 * @return
	 */
	public static boolean expressionContainsOnlyAllowedOps(Expression e, byte... allowedClasses)
	{
		byte val = classifyExpressionOps(e);

		for (byte b : allowedClasses)
			val &= ~b; // turns off bits in b if they were on

		return val == 0;
	}

	/**
	 * Create a twp-mode configuration with modes named "mode1" and "mode2", for unit testing
	 * 
	 * @param dynamics1
	 *            a list of variable names, dynamics and possibly initial states for each variable,
	 *            for example {{"x", "x+1", "0.5"}, {"y", "3"}} would correspond to x'==x+1, x(0) =
	 *            0.5; y'==3, y(0)=0
	 * @param inv
	 *            the invariant string of the first mode
	 * @param guard
	 *            the guard string to go from the first mode to the second
	 * @param dynamics2
	 *            a list of variable names and dynamics for the second mode
	 * 
	 * @return the constructed configuration
	 */
	public static Configuration makeDebugConfiguration(String[][] dynamics1, String inv,
			String guard, String[][] dynamics2)
	{
		final int VAR_INDEX = 0;
		final int FLOW_INDEX = 1;
		final int INIT_INDEX = 2;
		final String MODE1_NAME = "mode1";
		final String MODE2_NAME = "mode2";

		BaseComponent ha = new BaseComponent();
		AutomatonMode mode1 = ha.createMode(MODE1_NAME);
		AutomatonMode mode2 = ha.createMode(MODE2_NAME);
		StringBuilder initStringBuilder = new StringBuilder();

		if (dynamics1.length != dynamics2.length)
			throw new AutomatonExportException("Expected dynamics1 and dynamics2 of same length");

		for (int i = 0; i < dynamics1.length; ++i)
		{
			if (dynamics1[i].length < 2 || dynamics1[i].length > 3)
				throw new AutomatonExportException(
						"expected 2 or 3 values in passed-in array dynamics1 (varname, flow, init)");

			if (dynamics2[i].length != 2)
				throw new AutomatonExportException(
						"expected 2 values in passed-in array dynamics2 (varname, flow)");

			String var = dynamics1[i][VAR_INDEX];
			String flow = var + "' == " + dynamics1[i][FLOW_INDEX];

			ha.variables.add(var);

			if (!dynamics2[i][VAR_INDEX].equals(var))
				throw new AutomatonExportException(
						"expected same variable ordering in both dynamics arrays");

			String initVal = dynamics1[i].length == 3 ? dynamics1[i][INIT_INDEX] : "0";
			String initStr = var + " == " + initVal;

			if (initStringBuilder.length() > 0)
				initStringBuilder.append(" & " + initStr);
			else
				initStringBuilder.append(initStr);

			Expression flowExp = FormulaParser.parseFlow(flow).asOperation().getRight();
			mode1.flowDynamics.put(var, new ExpressionInterval(flowExp));

			// now do mode 2
			var = dynamics2[i][VAR_INDEX];
			flow = var + "' == " + dynamics2[i][FLOW_INDEX];

			flowExp = FormulaParser.parseFlow(flow).asOperation().getRight();
			mode2.flowDynamics.put(var, new ExpressionInterval(flowExp));
		}

		// add invariant
		mode1.invariant = FormulaParser.parseInvariant(inv);
		mode2.invariant = Constant.TRUE;

		// add transition / guard
		AutomatonTransition at = ha.createTransition(mode1, mode2);
		at.guard = FormulaParser.parseGuard(guard);

		Configuration c = new Configuration(ha);

		c.settings.plotVariableNames[0] = ha.variables.get(0);
		c.settings.plotVariableNames[1] = ha.variables.size() > 1 ? ha.variables.get(1)
				: ha.variables.get(0);
		c.init.put(MODE1_NAME, FormulaParser.parseInitialForbidden(initStringBuilder.toString()));

		c.validate();
		return c;
	}

	/**
	 * Create a single-mode configuration with a mode named "on", for unit testing
	 * 
	 * @param dynamics
	 *            a list of variable names, dynamics and possibly initial states for each variable,
	 *            for example {{"x", "x+1", "0.5"}, {"y", "3"}} would correspond to x'==x+1, x(0) =
	 *            0.5; y'==3, y(0)=0
	 * @return the constructed configuration
	 */
	public static Configuration makeDebugConfiguration(String[][] dynamics)
	{
		final int VAR_INDEX = 0;
		final int FLOW_INDEX = 1;
		final int INIT_INDEX = 2;
		BaseComponent ha = new BaseComponent();
		AutomatonMode am = ha.createMode("on");
		StringBuilder initStringBuilder = new StringBuilder();

		for (int i = 0; i < dynamics.length; ++i)
		{
			if (dynamics[i].length < 2 || dynamics[i].length > 3)
				throw new AutomatonExportException(
						"expected 2 or 3 values in passed-in array (varname, flow, init)");

			String var = dynamics[i][VAR_INDEX];
			String flow = var + "' == " + dynamics[i][FLOW_INDEX];

			ha.variables.add(var);

			String initVal = dynamics[i].length == 3 ? dynamics[i][INIT_INDEX] : "0";
			String initStr = var + " == " + initVal;

			if (initStringBuilder.length() > 0)
				initStringBuilder.append(" & " + initStr);
			else
				initStringBuilder.append(initStr);

			Expression flowExp = FormulaParser.parseFlow(flow).asOperation().getRight();
			am.flowDynamics.put(var, new ExpressionInterval(flowExp));
		}

		Configuration c = new Configuration(ha);

		am.invariant = Constant.TRUE;
		c.settings.plotVariableNames[0] = ha.variables.get(0);
		c.settings.plotVariableNames[1] = ha.variables.size() > 1 ? ha.variables.get(1)
				: ha.variables.get(0);
		c.init.put("on", FormulaParser.parseInitialForbidden(initStringBuilder.toString()));

		c.validate();
		return c;
	}

	/**
	 * Use a sample-based strategy to check if two expressions are equal. This returns null if they
	 * are, or a counter-example description string if they are not. Uses a tolerance of 1e-9.
	 * 
	 * @param expected
	 *            the expected expression
	 * @param actual
	 *            the actual expression
	 */
	public static String areExpressionsEqual(String expected, Expression actual)
	{
		double TOL = 1e-9;
		Expression e = FormulaParser.parseValue(expected);

		return areExpressionsEqual(e, actual, TOL);
	}

	/**
	 * Use a sample-based strategy to check if two expressions are equal. This returns null if they
	 * are, or a counter-example description string if they are not. Uses a tolerance of 1e-9.
	 * 
	 * @param expected
	 *            the expected expression
	 * @param actual
	 *            the actual expression
	 */
	public static String areExpressionsEqual(Expression expected, Expression actual)
	{
		double TOL = 1e-9;

		return areExpressionsEqual(expected, actual, TOL);
	}

	/**
	 * Use a sample-based strategy to check if two expressions are equal. This returns null if they
	 * are, or a counter-example description string if they are not.
	 * 
	 * @param expected
	 *            the expected expression
	 * @param actual
	 *            the actual expression
	 * @param tol
	 *            the tolerance to use
	 */
	public static String areExpressionsEqual(Expression expected, Expression actual, double tol)
	{
		String rv = null;

		Set<String> varSet = AutomatonUtil.getVariablesInExpression(expected);
		varSet.addAll(AutomatonUtil.getVariablesInExpression(actual));

		ArrayList<String> varList = new ArrayList<String>();
		varList.addAll(varSet);
		int numVars = varList.size();

		ArrayList<HyperPoint> samples = new ArrayList<HyperPoint>();
		samples.add(new HyperPoint(numVars)); // add 0

		// add 1 for each dimension
		for (int d = 0; d < numVars; ++d)
		{
			HyperPoint hp = new HyperPoint(numVars);
			hp.dims[d] = 1;
			samples.add(hp);
		}

		// add -1 for each dimension
		for (int d = 0; d < numVars; ++d)
		{
			HyperPoint hp = new HyperPoint(numVars);
			hp.dims[d] = -1;
			samples.add(hp);
		}

		// add 1 for every dimension
		HyperPoint one = new HyperPoint(numVars);

		for (int d = 0; d < numVars; ++d)
			one.dims[d] = 1;

		samples.add(one);

		// add a few deterministic random points
		int NUM_RANDOM_SAMPLES = 5;
		int seed = varList.hashCode();
		Random r = new Random(seed);

		for (int n = 0; n < NUM_RANDOM_SAMPLES; ++n)
		{
			HyperPoint hp = new HyperPoint(numVars);

			for (int d = 0; d < numVars; ++d)
				hp.dims[d] = r.nextDouble() * 20 - 10;

			samples.add(hp);
		}

		// compare a and b at the constructed sample points
		for (HyperPoint hp : samples)
		{
			double expectedVal = AutomatonUtil.evaluateExpression(expected, hp, varList);
			double actualVal = AutomatonUtil.evaluateExpression(actual, hp, varList);

			if (Math.abs(expectedVal - actualVal) > tol)
			{
				rv = "Expressions are NOT equal.\nexpected='" + expected.toDefaultString()
						+ "' and actual='" + actual.toDefaultString() + "' differ at point "
						+ varList + " = " + Arrays.toString(hp.dims) + ".\nexpected evalues to "
						+ expectedVal + "; actual evalutes to " + actualVal;
				break;
			}
		}

		return rv;
	}

	/**
	 * Return an expression taking the time derivative of the given expression. This consists of
	 * substituting the symbolic derivative for each variable
	 * 
	 * @param e
	 *            the expression where to do the substitution
	 * @param timeDerivatives
	 *            a map of variable->time derivative for each variable. All other Variable
	 *            expressions will be assumed to be constants
	 * @return the derivative of e
	 */
	public static Expression derivativeOf(Expression e, Map<String, Expression> timeDerivatives)
	{
		Expression rv = null;

		if (e instanceof Variable)
		{
			String v = ((Variable) e).name;

			if (timeDerivatives.keySet().contains(v))
				rv = timeDerivatives.get(v).copy();
			else
				rv = new Constant(0); // derivative of a constant is zero
		}
		else if (e instanceof Constant)
			rv = new Constant(0);
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			if (o.op == Operator.SUBTRACT)
				o = new Operation(Operator.ADD, o.getLeft(),
						new Operation(Operator.NEGATIVE, o.getRight()));

			if (o.op == Operator.NEGATIVE)
				o = new Operation(Operator.MULTIPLY, new Constant(-1), o.children.get(0));

			if (o.op == Operator.ADD)
			{
				ArrayList<Expression> childDers = new ArrayList<Expression>();

				for (Expression child : o.children)
				{
					Expression childDer = derivativeOf(child, timeDerivatives);

					if (!(childDer instanceof Constant) || ((Constant) childDer).getVal() != 0)
						childDers.add(childDer);
				}

				if (childDers.size() == 0)
					rv = new Constant(0);
				else if (childDers.size() == 1)
					rv = childDers.get(0);
				else
					rv = new Operation(Operator.ADD, childDers);
			}
			else if (o.op == Operator.MULTIPLY)
			{
				Expression leftDer = derivativeOf(o.getLeft(), timeDerivatives);
				Expression rightDer = derivativeOf(o.getRight(), timeDerivatives);

				// chain rule: (xy)' = x'y + xy'

				Operation leftSide = new Operation(Operator.MULTIPLY, leftDer, o.getRight());
				Operation rightSide = new Operation(Operator.MULTIPLY, o.getLeft(), rightDer);

				if (leftDer instanceof Constant && ((Constant) leftDer).getVal() == 0)
					rv = rightSide;
				else if (rightDer instanceof Constant && ((Constant) rightDer).getVal() == 0)
					rv = leftSide;
				else
					rv = new Operation(Operator.ADD, leftSide, rightSide);
			}
			else
				throw new AutomatonExportException("Unsupported Operation in derivativeOf '"
						+ o.op.toDefaultString() + "': " + e.toDefaultString());
		}
		else
			throw new AutomatonExportException(
					"Unsupported Expression type in derivativeOf: " + e.toDefaultString());

		return rv;
	}

	/**
	 * Get the gradient vector within a mode. This uses the 'average' dynamics if there's
	 * nondeterminism.
	 * 
	 * @param am
	 *            the mode
	 * @param pt
	 *            the point where to, in the automaton's variable ordering
	 * @return the gradient vector (with the automaton's variable ordering)
	 */
	public static double[] getGradientAtPoint(AutomatonMode am, HyperPoint pt)
	{
		List<String> vars = am.automaton.variables;
		Map<String, Expression> flowDynamics = centerDynamics(am.flowDynamics);

		double[] rv = new double[pt.dims.length];

		for (int dim = 0; dim < pt.dims.length; ++dim)
		{
			String varName = vars.get(dim);
			Expression e = flowDynamics.get(varName);

			double d = evaluateExpression(e, pt, vars);

			rv[dim] = d;
		}

		return rv;
	}

	public static double evaluateExpression(Expression e, HyperPoint pt, List<String> variableNames)
	{
		// create value map
		TreeMap<String, Expression> valMap = new TreeMap<String, Expression>();

		for (int i = 0; i < pt.dims.length; ++i)
			valMap.put(variableNames.get(i), new Constant(pt.dims[i]));

		ValueSubstituter vs = new ValueSubstituter(valMap);

		Expression substituted = vs.substitute(e);

		Expression result = SimplifyExpressionsPass.simplifyExpression(substituted);

		double rv = 0;

		if (result instanceof Constant)
			rv = ((Constant) result).getVal();
		else
			throw new AutomatonExportException(
					"Could not simplify flow expression completely while simulating: " + e);

		return rv;
	}

	public static Map<String, Expression> centerDynamics(
			Map<String, ExpressionInterval> flowDynamics)
	{
		LinkedHashMap<String, Expression> rv = new LinkedHashMap<String, Expression>();

		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			ExpressionInterval ei = e.getValue();
			double mid = 0;
			Interval i = ei.getInterval();

			if (i != null)
				mid = i.middle();

			Expression eMid = new Operation(Operator.ADD, ei.getExpression(), new Constant(mid));
			rv.put(e.getKey(), eMid);
		}

		return rv;
	}

	/**
	 * Process a reset on a point, and return the new point
	 * 
	 * @param pt
	 *            the incoming point
	 * @param variableNames
	 *            the list of variables, in order
	 * @param reset
	 *            the reset map
	 * @return the outgoing point
	 */
	public static HyperPoint processReset(HyperPoint pt, ArrayList<String> variableNames,
			LinkedHashMap<String, ExpressionInterval> reset)
	{
		HyperPoint rv = new HyperPoint(pt);

		for (int i = 0; i < variableNames.size(); ++i)
		{
			String v = variableNames.get(i);
			ExpressionInterval resetAssignment = reset.get(v);

			if (resetAssignment == null)
				rv.dims[i] = pt.dims[i];
			else
				rv.dims[i] = assignFromExpression(v, pt, variableNames, resetAssignment);
		}

		return rv;
	}

	private static double assignFromExpression(String v, HyperPoint p,
			ArrayList<String> variableNames, ExpressionInterval resetAssignment)
	{
		HyperPoint hp = new HyperPoint(p);

		double d = AutomatonUtil.evaluateExpression(resetAssignment.getExpression(), hp,
				variableNames);

		Interval i = resetAssignment.getInterval();

		if (i != null)
			d += i.middle();

		return d;
	}

	public static String areExpressionIntervalsEqual(String desiredExpressionString,
			double desiredMin, double desiredMax, ExpressionInterval actual)
	{
		Expression e = FormulaParser.parseValue(desiredExpressionString);

		return areExpressionIntervalsEqual(
				new ExpressionInterval(e, new Interval(desiredMin, desiredMax)), actual);
	}

	public static String areExpressionIntervalsEqual(ExpressionInterval desired,
			ExpressionInterval actual)
	{
		Expression desiredE = desired.getExpression();
		Interval desiredI = desired.getInterval();

		if (desiredI == null)
			desiredI = new Interval(0, 0);

		Expression desiredMin = new Operation(Operator.ADD, desiredE, new Constant(desiredI.min));
		Expression desiredMax = new Operation(Operator.ADD, desiredE, new Constant(desiredI.max));

		Expression actualE = actual.getExpression();
		Interval actualI = actual.getInterval();

		if (actualI == null)
			actualI = new Interval(0, 0);

		Expression actualMin = new Operation(Operator.ADD, actualE, new Constant(actualI.min));
		Expression actualMax = new Operation(Operator.ADD, actualE, new Constant(actualI.max));

		String res = areExpressionsEqual(desiredMin, actualMin);

		if (res != null)
			res = "\nDesired: '" + desired.toDefaultString() + "'\nActual: '"
					+ actual.toDefaultString() + "'\nMin differs:\n" + res;
		else
		{
			res = areExpressionsEqual(desiredMax, actualMax);

			if (res != null)

				res = "\nDesired: '" + desired.toDefaultString() + "'\nActual: '"
						+ actual.toDefaultString() + "'\nMax differs:\n" + res;
		}

		return res;
	}

	/**
	 * Evaluate an expression to a constant value, returning it
	 * 
	 * @param e
	 *            an Expression which simplifies to a Constant
	 * @return the double value
	 * @throws AutomatonExportException
	 *             if the expression doesn't evaluate to a constant
	 */
	public static double evaluateConstant(Expression e)
	{
		Expression s = SimplifyExpressionsPass.simplifyExpression(e);

		if (!(s instanceof Constant))
			throw new AutomatonExportException(
					"Expression did not simplify to a constant: '" + e.toDefaultString() + "'");

		return ((Constant) s).getVal();
	}
}

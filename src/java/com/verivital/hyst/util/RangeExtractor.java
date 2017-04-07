package com.verivital.hyst.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;

public class RangeExtractor
{
	/**
	 * Get ranges for variables in a logical expression, ignores loc assignments
	 * 
	 * @param expression
	 *            the expression to parse
	 * @param ranges
	 *            where to store the ranges for the variables we encounter
	 * @throws EmptyRangeException
	 *             if expression is Constant.FALSE or otherwise unsatisfiable for any variable in
	 *             'variables'
	 * @throws ConstantMismatchException
	 *             if there are conflicting assignments to constants
	 * @throws UnsupportedConditionException
	 *             if the expression contains non-interval operations on the desired variable
	 * 
	 */
	public static void getVariableRanges(Expression expression, Map<String, Interval> ranges)
			throws EmptyRangeException, ConstantMismatchException, UnsupportedConditionException
	{
		if (expression == null)
			throw new NullPointerException("null expression passed into getVariableRanges()");

		try
		{
			Map<String, Double> constantMap = getConstants(expression);
			// add all constants
			for (Entry<String, Double> e : constantMap.entrySet())
			{
				double val = e.getValue();

				ranges.put(e.getKey(), new Interval(val));
			}

			Map<String, Interval> constantIntervalMap = doubleMapToIntervalMap(constantMap);

			expression = SubstituteConstantsPass
					.substituteConstantsIntoExpression(constantIntervalMap, expression);

			expression = SimplifyExpressionsPass.simplifyExpression(expression);

			getVariableRangesRecursive(expression, ranges, null, false);
		}
		catch (AutomatonExportException e)
		{
			throw new AutomatonExportException(
					"Error while parsing expression: " + expression.toDefaultString(), e);
		}
	}

	/**
	 * Get weak ranges (outer approximations) for variables in a logical expression. This doesn't
	 * simplify or handle conditions that aren't simple comparisons with constants.
	 * 
	 * @param expression
	 *            the expression to parse
	 * @param ranges
	 *            where to store the ranges for the variables we encounter
	 */
	public static void getWeakVariableRanges(Expression expression, Map<String, Interval> ranges)
	{
		if (expression == null)
			throw new NullPointerException("null expression passed into getVariableRanges()");

		try
		{
			Map<String, Double> constantMap = new HashMap<String, Double>();

			try
			{
				constantMap = getConstants(expression);
			}
			catch (ConstantMismatchException e)
			{
				// ignore
			}

			// add all constants
			for (Entry<String, Double> e : constantMap.entrySet())
			{
				double val = e.getValue();

				ranges.put(e.getKey(), new Interval(val));
			}

			Map<String, Interval> constantIntervalMap = doubleMapToIntervalMap(constantMap);

			expression = SubstituteConstantsPass
					.substituteConstantsIntoExpression(constantIntervalMap, expression);

			// this is weak range extraction, so do not do simplification
			// expression =
			// SimplifyExpressionsPass.simplifyExpression(expression);

			getVariableRangesRecursive(expression, ranges, null, true);
		}
		catch (AutomatonExportException e)
		{
			throw new AutomatonExportException(
					"Error while parsing expression: " + expression.toDefaultString(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException("Unsupported expression during weak"
					+ "range extraction (should never happen): " + expression.toDefaultString(), e);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException("Empty ragnge during weak"
					+ "range extraction (should never happen): " + expression.toDefaultString(), e);
		}
	}

	public static Map<String, Double> getConstants(Expression expression)
			throws ConstantMismatchException
	{
		HashMap<String, Double> rv = new HashMap<String, Double>();

		Operation o = expression.asOperation();

		if (o != null)
		{
			if (o.op == Operator.AND)
			{
				Map<String, Double> left = getConstants(o.getLeft());
				Map<String, Double> right = getConstants(o.getRight());

				safePutAll(rv, left);
				safePutAll(rv, right);
			}
			else if (o.op == Operator.EQUAL)
			{
				double val = 0;
				String varName = null;

				Expression leftExp = o.getLeft();
				Expression rightExp = o.getRight();

				if (leftExp instanceof Variable && rightExp instanceof Constant)
				{
					varName = ((Variable) leftExp).name;
					val = ((Constant) rightExp).getVal();
				}
				else if (leftExp instanceof Constant && rightExp instanceof Variable)
				{
					varName = ((Variable) rightExp).name;
					val = ((Constant) leftExp).getVal();
				}

				if (varName != null && !varName.startsWith("loc("))
				{
					safePut(rv, varName, val);
				}
			}
		}

		return rv;
	}

	/**
	 * Put all the entries into a map, throwing an exception if it conflicts
	 * 
	 * @param rv
	 *            the map to store
	 * @param varName
	 *            add to
	 * @param from
	 *            take from
	 */
	private static void safePutAll(HashMap<String, Double> rv, Map<String, Double> from)
			throws ConstantMismatchException
	{
		for (Entry<String, Double> e : from.entrySet())
			safePut(rv, e.getKey(), e.getValue());
	}

	/**
	 * Put a variable into a map, throwing an exception if it conflicts
	 * 
	 * @param rv
	 *            the map to store
	 * @param varName
	 * @param val
	 */
	private static void safePut(Map<String, Double> rv, String key, double val)
			throws ConstantMismatchException
	{
		Double d = rv.get(key);

		if (d == null)
			rv.put(key, val);
		else
		{
			if (d != val)
				throw new ConstantMismatchException("Multiple different mappings for key: " + key);
		}
	}

	/**
	 * Get the range for a specific variable in an expression. May be null if the range is not
	 * defined. If only one side of the range is defined, then the min may be -Double.MAX_VALUE, and
	 * the max may be Double.MAX_VALUE
	 * 
	 * @param expression
	 *            the expression to parse
	 * @param variable
	 *            the name of the variable
	 * @throws EmptyRangeException
	 *             if expression is Constant.FALSE or otherwise unsatisfiable for any variable in
	 *             'variables'
	 * @throws ConstantMismatchException
	 *             is there are constants which contradict each other
	 * @throws UnsupportedConditionException
	 *             if there are conditions not suited for interval range extraction
	 */
	public static Interval getVariableRange(Expression expression, String variable)
			throws EmptyRangeException, ConstantMismatchException, UnsupportedConditionException
	{
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		Collection<String> equalVars = new ArrayList<String>();
		equalVars.add(variable);
		equalVars.addAll(getEqualVariables(expression, variable));
		Interval rv = null;

		try
		{
			Map<String, Double> constantMap = getConstants(expression);

			Double constValue = constantMap.get(variable);

			if (constValue != null)
				rv = new Interval(constValue);
			else
			{
				Map<String, Interval> constantIntervalMap = doubleMapToIntervalMap(constantMap);

				expression = SubstituteConstantsPass
						.substituteConstantsIntoExpression(constantIntervalMap, expression);

				expression = SimplifyExpressionsPass.simplifyExpression(expression);

				getVariableRangesRecursive(expression, ranges, equalVars, false);

				if (ranges.size() > 0)
					rv = mergeAllRanges(ranges.values());
			}
		}
		catch (AutomatonExportException e)
		{
			// this can occur, for example, if the conditions are not box-edges
			throw new EmptyRangeException("Error getting range for " + variable + " in expression: "
					+ expression.toDefaultString(), e);
		}
		catch (EmptyRangeException e)
		{
			throw new EmptyRangeException("Empty range for " + variable + " in expression: "
					+ expression.toDefaultString(), e);
		}
		catch (ConstantMismatchException e)
		{
			throw new ConstantMismatchException(
					"Constant mismatch in expression: " + expression.toDefaultString(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new UnsupportedConditionException(
					"Unsupported operation for range extraction in expression: "
							+ expression.toDefaultString(),
					e);
		}

		return rv;
	}

	private static Map<String, Interval> doubleMapToIntervalMap(Map<String, Double> constantMap)
	{
		HashMap<String, Interval> rv = new HashMap<String, Interval>();

		for (Entry<String, Double> e : constantMap.entrySet())
			rv.put(e.getKey(), new Interval(e.getValue()));

		return rv;
	}

	private static Interval mergeAllRanges(Collection<Interval> is) throws EmptyRangeException
	{
		Interval rv = new Interval(-Double.MAX_VALUE, Double.MAX_VALUE);

		for (Interval i : is)
		{
			rv.min = Math.max(rv.min, i.min);
			rv.max = Math.min(rv.max, i.max);
		}

		if (rv.max < rv.min)
			throw new EmptyRangeException("range is unsatisfiable");

		return rv;
	}

	private static Collection<String> getEqualVariables(Expression expression, String variable)
	{
		Collection<String> rv = new HashSet<String>();
		Operation o = expression.asOperation();

		if (o != null && o.children.size() == 2)
		{
			Expression leftExp = o.children.get(0);
			Expression rightExp = o.children.get(1);
			Operator op = o.op;

			if (op.equals(Operator.AND))
			{
				rv.addAll(getEqualVariables(leftExp, variable));
				rv.addAll(getEqualVariables(rightExp, variable));
			}
			else if (op.equals(Operator.EQUAL) && leftExp instanceof Variable
					&& rightExp instanceof Variable)
			{
				Variable left = (Variable) leftExp;
				Variable right = (Variable) rightExp;

				if (left.name.equals(variable))
					rv.add(right.name);
				else if (right.name.equals(variable))
					rv.add(left.name);
			}
		}

		return rv;
	}

	/**
	 * Get the variable ranges and store them into ranges
	 * 
	 * @param expression
	 *            the expression to extract from
	 * @param ranges
	 *            the place to store them
	 * @param vars
	 *            <<<<<<< HEAD a set of variables we are interested in. if null then get all
	 *            variables
	 * @param extractWeakRanges
	 *            are we just extracting an overapproximation of the range? This will ignore certain
	 *            types of errors (like unsupported comparisons) ======= a set of variables we are
	 *            interested in. if null then get all variables
	 * @param constantMap
	 *            a map of variableName -> value for all constants in the original expression
	 * @param ignoreUnsupported
	 *            should we ignore unsupported expressions, or raise an exception? >>>>>>>
	 *            hylaa-printer
	 * @throws EmptyRangeException
	 *             if expression is Constant.FALSE or otherwise unsatisfiable for the selected
	 *             variables
	 * @throws UnsupportedConditionException
	 *             if the expression contains non-interval operations on the desired variable
	 */
	private static void getVariableRangesRecursive(Expression expression,
			Map<String, Interval> ranges, Collection<String> vars, boolean extractWeakRanges)
					throws EmptyRangeException, UnsupportedConditionException
	{
		Operation o = expression.asOperation();

		if (o != null && o.children.size() == 2)
		{
			Expression leftExp = o.children.get(0);
			Expression rightExp = o.children.get(1);
			Operator op = o.op;

			if (op.equals(Operator.AND))
			{
				TreeMap<String, Interval> left = new TreeMap<String, Interval>();
				TreeMap<String, Interval> right = new TreeMap<String, Interval>();

				getVariableRangesRecursive(leftExp, ranges, vars, extractWeakRanges);
				getVariableRangesRecursive(rightExp, ranges, vars, extractWeakRanges);

				ranges = mergeTreeRanges(left, right);
			}
			else if (expressionContainsVariables(expression, vars))
			{
				double val = 0;
				String varName = null;

				// ignore location assignments
				if (leftExp instanceof Variable && ((Variable) leftExp).name.startsWith("loc("))
					return;

				boolean yodaConstraint = false; // if constraint is like '5 < x'
												// instead of the normal order
				boolean shouldSkip = false;

				if (leftExp instanceof Variable && rightExp instanceof Constant)
				{
					varName = ((Variable) leftExp).name;
					val = ((Constant) rightExp).getVal();

					yodaConstraint = false;
				}
				else if (leftExp instanceof Constant && rightExp instanceof Variable)
				{
					varName = ((Variable) rightExp).name;
					val = ((Constant) leftExp).getVal();

					yodaConstraint = true;
				}
				else if (leftExp instanceof Variable && rightExp instanceof Variable)
				{

					// both are variables. This is only allowed it it's
					// an alias assignment, which
					// we detect by checking if the operator is '=='
					if (op == Operator.EQUAL)
						shouldSkip = true;
					else
					{
						if (extractWeakRanges)
							shouldSkip = true;
						else
						{
							String message = "Unsupported condition for range extraction (one side should be variable, the "
									+ "other side a constant): " + expression.toDefaultString()
									+ ". If one side is a constant, you can try running the substitute constants transformation "
									+ "pass by adding '-pass_sub_constants \"\"' to the command line";

							throw new UnsupportedConditionException(message);
						}
					}
				}
				else
				{
					if (extractWeakRanges)
						shouldSkip = true;
					else
					{
						String message = "Unsupported condition for range extraction (one side should be variable, the "
								+ "other side a constant): " + expression.toDefaultString()
								+ ". If one side is a named constant, you can try running the substitute constants transformation "
								+ "pass by adding '-pass sub_constants \"\"' to the command line";

						throw new UnsupportedConditionException(message);
					}
				}

				if (!shouldSkip)
				{
					Interval i = null;

					if (op == Operator.EQUAL)
					{
						i = new Interval(val);
						ranges.put(varName, i);
					}
					else if ((!yodaConstraint
							&& (op == Operator.GREATER || op == Operator.GREATEREQUAL))
							|| (yodaConstraint
									&& (op == Operator.LESS || op == Operator.LESSEQUAL)))
					{
						i = ranges.get(varName);

						if (i == null)
						{
							i = new Interval(-Double.MAX_VALUE, Double.MAX_VALUE);
							ranges.put(varName, i);
						}

						i.min = val;
					}
					else if ((!yodaConstraint && (op == Operator.LESS || op == Operator.LESSEQUAL))
							|| (yodaConstraint
									&& (op == Operator.GREATER || op == Operator.GREATEREQUAL)))
					{
						i = ranges.get(varName);

						if (i == null)
						{
							i = new Interval(-Double.MAX_VALUE, Double.MAX_VALUE);

							ranges.put(varName, i);
						}

						i.max = val;
					}
					else
					{
						if (!extractWeakRanges)
							throw new UnsupportedConditionException(
									"Unsupported expression during range extraction: "
											+ expression.toDefaultString());
					}

					if (i.max < i.min)
						throw new EmptyRangeException("range for " + varName + " is unsatisfiable");
				}
			}
		}
		else if (expression instanceof Variable)
		{
			Variable v = (Variable) expression;

			if (v.name.equals("true"))
			{
				// no restrictions on variable range
			}
			else if (v.name.equals("false"))
			{
				throw new EmptyRangeException("expression contains false");
			}
			else
				throw new AutomatonExportException(
						"Unsupported expression type (variable as condition?): "
								+ expression.toDefaultString());
		}
		else if (expression == Constant.FALSE) // if any variable is false, the
												// range for all is empty
			throw new EmptyRangeException("expression contains FALSE");
		else if (expression != Constant.TRUE && expressionContainsVariables(expression, vars))
		{
			if (!extractWeakRanges)
				throw new UnsupportedConditionException(
						"Unsupported expression during range extraction: "
								+ expression.toDefaultString());
		}
	}

	/**
	 * Check if an expresssion contains any of a set of variables.
	 * 
	 * @param expression
	 *            the expression to check
	 * @param vars
	 *            the variable names ot check for (if null always returns true)
	 * @return true iff any of vars is in the expression
	 */
	public static boolean expressionContainsVariables(Expression expression,
			Collection<String> vars)
	{
		boolean rv = false;

		if (vars == null)
			rv = true;
		else
		{
			for (String var : vars)
			{
				if (expressionContainsVariable(expression, var))
				{
					rv = true;
					break;
				}
			}
		}

		return rv;
	}

	/**
	 * Check if an expresssion contains a variable. If variable is null, then the result is true.
	 * 
	 * @param expression
	 *            the expression to check
	 * @param variable
	 *            the variable name to check for
	 * @return true iff variable is null or if it's contained in the expression
	 */
	public static boolean expressionContainsVariable(Expression expression, String variable)
	{
		boolean rv = true;

		if (variable != null)
			rv = countVariableOccurances(expression, variable) > 0;

		return rv;
	}

	/**
	 * Merge the two ranges. For example [-5, 4] and [2, 10] will merge to be [2, 4].
	 * 
	 * @param left
	 *            a range for each variable
	 * @param right
	 *            the other range for each variable, may include more or less variables as well
	 * @return the merged set of constraints
	 * @throws EmptyRangeException
	 *             if the ranges don't overlap
	 */
	private static TreeMap<String, Interval> mergeTreeRanges(TreeMap<String, Interval> left,
			TreeMap<String, Interval> right) throws EmptyRangeException
	{
		TreeMap<String, Interval> rv = new TreeMap<String, Interval>();

		rv.putAll(left);

		for (Entry<String, Interval> e : right.entrySet())
		{
			String key = e.getKey();
			Interval val = e.getValue();

			Interval i = rv.get(key);

			if (i == null)
				rv.put(key, val);
			else
			{
				// take the tighter of the two extremes
				i.min = Math.max(i.min, val.min);
				i.max = Math.min(i.max, val.max);

				if (i.max < i.min)
					throw new EmptyRangeException("range for " + key + " is unsatisfiable");
			}
		}

		return rv;
	}

	@SuppressWarnings("serial")
	public static class EmptyRangeException extends Exception
	{
		public EmptyRangeException(String msg)
		{
			super(msg);
		}

		public EmptyRangeException(String msg, Exception e)
		{
			super(msg, e);
		}
	}

	@SuppressWarnings("serial")
	public static class UnsupportedConditionException extends Exception
	{
		public UnsupportedConditionException(String msg)
		{
			super(msg);
		}

		public UnsupportedConditionException(String msg, Exception e)
		{
			super(msg, e);
		}
	}

	public static int countVariableOccurances(Expression expression, String variable)
	{
		int rv = 0;

		if (expression instanceof Operation)
		{
			Operation o = expression.asOperation();

			for (Expression c : o.children)
			{
				rv += countVariableOccurances(c, variable);
			}
		}
		else if (expression instanceof Variable)
		{
			Variable v = (Variable) expression;

			if (v.name.equals(variable))
				++rv;
		}

		return rv;
	}

	@SuppressWarnings("serial")
	public static class ConstantMismatchException extends Exception
	{
		public ConstantMismatchException(String s)
		{
			super(s);
		}

		public ConstantMismatchException(String msg, Exception e)
		{
			super(msg, e);
		}
	}

	/**
	 * get the variable ranges from an expression, throwing an AutomatonExportException on error
	 * 
	 * @param e
	 *            the expression
	 * @param description
	 *            the description to print in case of error
	 * @return the extracted ranges for all the variables
	 */
	public static TreeMap<String, Interval> getVariableRanges(Expression e, String description)
	{
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		try
		{
			getVariableRanges(e, ranges);
		}
		catch (EmptyRangeException ex)
		{
			throw new AutomatonExportException(description + " variable range was empty", ex);
		}
		catch (ConstantMismatchException ex)
		{
			throw new AutomatonExportException(description + " variable range was contradictory",
					ex);
		}
		catch (UnsupportedConditionException ex)
		{
			throw new AutomatonExportException(description + " variable range was not a box", ex);
		}

		return ranges;
	}
}

package com.verivital.hyst.util;

import java.util.ArrayList;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;

public class DynamicsUtil
{
	/**
	 * Get a row of the dynamics A matrix in x' = Ax + Bu + c
	 * 
	 */
	public static ArrayList<Double> extractDynamicsMatrixARow(AutomatonMode am, int index)
	{
		ArrayList<String> nonInputVars = getNonInputVariables(am, am.automaton.variables);

		String row = nonInputVars.get(index);

		Expression der = am.flowDynamics.get(row).asExpression();

		return extractLinearVector(der, nonInputVars);
	}

	/**
	 * Get the dynamics B matrix in x' = Ax + Bu + c
	 * 
	 */
	public static ArrayList<ArrayList<Double>> extractDynamicsMatrixB(AutomatonMode am)
	{
		ArrayList<ArrayList<Double>> rv = new ArrayList<ArrayList<Double>>();
		ArrayList<String> nonInputVars = getNonInputVariables(am, am.automaton.variables);

		ArrayList<String> inputVars = new ArrayList<String>();

		for (String var : am.automaton.variables)
		{
			if (!nonInputVars.contains(var))
				inputVars.add(var);
		}

		for (String row : nonInputVars)
		{
			Expression der = am.flowDynamics.get(row).asExpression();

			ArrayList<Double> line = extractLinearVector(der, inputVars);

			rv.add(line);
		}

		return rv;
	}

	public static ArrayList<String> getNonInputVariables(AutomatonMode am,
			ArrayList<String> variables)
	{
		ArrayList<String> rv = new ArrayList<String>();

		if (am.flowDynamics == null)
		{
			// if the mode is urgent, there are no input variables...
			rv.addAll(variables);
		}
		else
		{
			for (String row : am.automaton.variables)
			{
				if (am.flowDynamics.get(row) != null)
					rv.add(row);
			}
		}

		return rv;
	}

	/**
	 * Get the dynamics C vector in x' = Ax + Bu + c
	 */
	public static ArrayList<Double> extractDynamicsVectorC(AutomatonMode am)
	{
		ArrayList<Double> rv = new ArrayList<Double>();
		ArrayList<String> allVars = getNonInputVariables(am, am.automaton.variables);

		for (String row : allVars)
		{
			// skip urgent variables
			if (am.flowDynamics.get(row) == null)
				continue;

			Expression der = am.flowDynamics.get(row).asExpression();

			rv.add(extractLinearValue(der));
		}

		return rv;
	}

	/**
	 * Split a conjunction into its suboperators
	 * 
	 * @param conj
	 *            the input expression
	 * @return a list of sub-operators which are part of a conjunction that forms conj
	 */
	public static ArrayList<Operation> splitConjunction(Expression conj)
	{
		ArrayList<Operation> rv = new ArrayList<Operation>();

		if (conj instanceof Operation)
		{
			Operation o = conj.asOperation();
			Operator op = o.op;

			if (op == Operator.AND)
			{
				rv.addAll(splitConjunction(o.getLeft()));
				rv.addAll(splitConjunction(o.getRight()));
			}
			else if (op == Operator.OR || op == Operator.LOGICAL_NOT)
				throw new AutomatonExportException("Unsupported top-level operator: '"
						+ op.toDefaultString() + "' in " + conj.toDefaultString());
			else
				rv.add(o);
		}
		else
			throw new AutomatonExportException(
					"Unsupported non-operator condition: " + conj.toDefaultString());

		return rv;
	}

	/**
	 * Split a disjunction into its suboperators
	 * 
	 * @param conj
	 *            the input expression
	 * @return a list of sub-operators which are part of a conjunction that forms conj
	 */
	public static ArrayList<Expression> splitDisjunction(Expression disj)
	{
		ArrayList<Expression> rv = new ArrayList<Expression>();

		if (disj instanceof Operation)
		{
			Operation o = disj.asOperation();
			Operator op = o.op;

			if (op == Operator.OR)
			{
				rv.addAll(splitDisjunction(o.getLeft()));
				rv.addAll(splitDisjunction(o.getRight()));
			}
			else if (op == Operator.LOGICAL_NOT)
				throw new AutomatonExportException("Unsupported top-level operator: '"
						+ op.toDefaultString() + "' in " + disj.toDefaultString());
			else
				rv.add(o);
		}
		else if (disj == Constant.TRUE || disj == Constant.FALSE)
			rv.add(disj);
		else
			throw new AutomatonExportException(
					"Unsupported non-operator condition: " + disj.toDefaultString());

		return rv;
	}

	private static Expression findMultiplier(String varName, Expression summation)
	{
		Expression rv = null;

		if (summation instanceof Operation)
		{
			Operation o = summation.asOperation();
			Operator op = o.op;

			if (op == Operator.NEGATIVE)
			{
				rv = findMultiplier(varName, o.children.get(0));

				if (rv != null)
					rv = new Operation(Operator.NEGATIVE, rv);
			}
			else if (op == Operator.MULTIPLY)
			{
				Expression left = o.getLeft();
				Expression right = o.getRight();

				if (left instanceof Variable && right instanceof Variable)
					throw new AutomatonExportException(
							"Unsupported variable-variable term in linear derivative: '"
									+ o.toDefaultString() + "'");
				else if (left instanceof Variable)
				{
					if (((Variable) left).name.equals(varName))
						rv = right;
				}
				else if (right instanceof Variable)
				{
					if (((Variable) right).name.equals(varName))
						rv = left;
				}
				else
					throw new AutomatonExportException(
							"Unsupported term in linear derivative: '" + o.toDefaultString() + "'");
			}
			else if (op == Operator.DIVIDE)
			{
				// only support variable / constant

				Expression left = o.getLeft();
				Expression right = o.getRight();

				if (left instanceof Variable && right instanceof Constant
						&& ((Constant) right).getVal() != 0)
				{
					if (((Variable) left).name.equals(varName))
						rv = new Constant(1.0 / ((Constant) right).getVal());
				}
				else
					throw new AutomatonExportException(
							"Unsupported term in linear derivative: '" + o.toDefaultString() + "'");
			}
			else if (op == Operator.ADD || op == Operator.SUBTRACT)
			{
				Expression left = o.getLeft();
				Expression right = o.getRight();

				Expression leftRv = findMultiplier(varName, left);
				Expression rightRv = findMultiplier(varName, right);

				if (leftRv != null && rightRv != null)
					throw new AutomatonExportException("Unsupported term in linear derivative ("
							+ varName + " in multiple places): " + summation.toDefaultString());
				else if (leftRv != null)
					rv = leftRv;
				else if (rightRv != null)
				{
					if (op == Operator.SUBTRACT)
						rv = new Operation(Operator.NEGATIVE, rightRv);
					else
						rv = rightRv;
				}
			}
			else
				throw new AutomatonExportException(
						"Unsupported operation in linear derivative (expecting '+', '-', '*', or'/'): '"
								+ o.toDefaultString());
		}
		else if (summation instanceof Constant)
		{
			// constant by itself

			if (varName == null)
				rv = summation;
		}
		else if (summation instanceof Variable)
		{
			// variable by itself

			Variable v = (Variable) summation;

			if (v.name.equals(varName))
				rv = new Constant(1);
		}
		else
			throw new AutomatonExportException(
					"Unsupported expression type (" + summation.getClass()
							+ ") in linear derivative (expecting sum of multiples): '"
							+ summation.toDefaultString() + "'");

		return rv;
	}

	/**
	 * Extract a linear vector from an expression
	 * 
	 * @param exp
	 *            the expression to extract from
	 * @param vars
	 *            the variables, in order
	 * @return a list of linear coefficients for each variable
	 */
	public static ArrayList<Double> extractLinearVector(Expression exp, ArrayList<String> vars)
	{
		ArrayList<Double> rv = new ArrayList<Double>();

		for (String v : vars)
		{
			Expression multiplierExpression = null;

			try
			{
				// find variable 'col' in expression 'exp'
				multiplierExpression = findMultiplier(v, exp);

				if (multiplierExpression != null)
				{
					double val = AutomatonUtil.evaluateConstant(multiplierExpression);
					rv.add(val);
				}
				else
					rv.add(0.0);
			}
			catch (AutomatonExportException ex)
			{
				throw new AutomatonExportException(
						"Error extracting linear coefficient for variable '" + v
								+ "' in expression: '" + exp.toDefaultString() + "'",
						ex);
			}
		}

		return rv;
	}

	public static double extractLinearValue(Expression exp)
	{
		double val = 0.0;

		Expression e = null;

		try
		{
			// find variable 'col' in expression 'der'
			e = findMultiplier(null, exp);
		}
		catch (AutomatonExportException ex)
		{
			throw new AutomatonExportException("Error extracting constant from linear expression: '"
					+ exp.toDefaultString() + "'", ex);
		}

		if (e != null)
		{
			val = AutomatonUtil.evaluateConstant(e);
		}

		return val;
	}
}

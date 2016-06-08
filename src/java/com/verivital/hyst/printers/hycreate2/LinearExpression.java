package com.verivital.hyst.printers.hycreate2;

import java.util.List;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;

/**
 * Class for parsing a linear expression as a condition Top level is expected to be an '=='
 * 
 * @author Stanley Bak
 *
 */
public class LinearExpression
{
	public Operator type;
	public double[] coefficients; // left hand side
	public double rhs;

	private List<String> vars;

	public LinearExpression(Operation o, List<String> variables)
	{
		this.vars = variables;

		if (o.op == Operator.EQUAL)
			type = Operator.EQUAL;
		else if (o.op == Operator.GREATER)
			type = Operator.GREATER;
		else if (o.op == Operator.LESS)
			type = Operator.LESS;
		else if (o.op == Operator.GREATEREQUAL)
			type = Operator.GREATEREQUAL;
		else if (o.op == Operator.LESSEQUAL)
			type = Operator.LESSEQUAL;
		else
			throw new AutomatonExportException(
					"Could not extract linear condition from expression (bad operator): " + o);

		// ok, now we should only have pluses and minuses on each of the terms
		// let's keep all the terms on the left, and all the constants on the
		// right
		coefficients = new double[variables.size()];

		Expression left = o.children.get(0).copy();
		Expression right = o.children.get(1).copy();

		// TODO: simplify expressions here

		populateCoefficients(left, true, variables);
		populateCoefficients(right, false, variables);
	}

	private void populateCoefficients(Expression e, boolean isLeftHandSide, List<String> vars)
	{
		if (e instanceof Constant)
		{
			Constant c = (Constant) e;

			double val = c.getVal();

			if (isLeftHandSide)
				rhs += -val;
			else
				rhs += val;
		}
		else if (e instanceof Variable)
		{
			Variable v = (Variable) e;
			int index = getVariableIndex(v, vars);

			if (isLeftHandSide)
				coefficients[index] += 1;
			else
				coefficients[index] -= 1;
		}
		else if (e instanceof Operation)
		{
			Operation o = (Operation) e;

			if (o.op == Operator.ADD)
			{
				populateCoefficients(o.getLeft(), isLeftHandSide, vars);
				populateCoefficients(o.getRight(), isLeftHandSide, vars);
			}
			else if (o.op == Operator.MULTIPLY)
			{
				Expression l = o.getLeft();
				Expression r = o.getRight();
				Variable v;
				Constant c;

				if (l instanceof Variable && r instanceof Constant)
				{
					v = (Variable) l;
					c = (Constant) r;
				}
				else if (l instanceof Constant && r instanceof Variable)
				{
					v = (Variable) r;
					c = (Constant) l;
				}
				else
				{
					throw new AutomatonExportException(
							"Couldn't parse simple linear expression: " + e.toDefaultString());
				}

				int index = getVariableIndex(v, vars);

				if (isLeftHandSide)
					coefficients[index] += c.getVal();
				else
					coefficients[index] -= c.getVal();
			}
			else
				throw new AutomatonExportException(
						"Could not parse operation '" + o.op.toDefaultString()
								+ "' in linear expression: " + e.toDefaultString());

		}
		else
			throw new AutomatonExportException(
					"Could not parse linear expression: " + e.toDefaultString());
	}

	private int getVariableIndex(Variable v, List<String> vars)
	{
		String s = v.name;

		int index = vars.indexOf(s);

		if (index == -1)
			throw new AutomatonExportException("Variable not found in automaton: " + s);

		return index;
	}

	@Override
	public String toString()
	{
		assert (coefficients.length == vars.size());

		String rv = "";

		for (int x = 0; x < coefficients.length; ++x)
		{
			double c = coefficients[x];

			if (c != 0)
			{
				if (rv.length() > 0)
					rv += " + ";

				rv += c + "*" + vars.get(x);
			}
		}

		rv += " " + type.toDefaultString() + " " + rhs;

		return rv;
	}
}

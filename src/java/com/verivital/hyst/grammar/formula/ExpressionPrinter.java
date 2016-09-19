package com.verivital.hyst.grammar.formula;

import com.verivital.hyst.ir.AutomatonExportException;

public abstract class ExpressionPrinter
{
	public String print(Expression e)
	{
		String rv = null;

		if (e == null)
			rv = "null";
		else if (e instanceof Constant)
			rv = printConstant((Constant) e);
		else if (e instanceof Operation)
			rv = printOperation((Operation) e);
		else if (e instanceof Variable)
			rv = printVariable((Variable) e);
		else if (e instanceof MatrixExpression)
			rv = printMatrix((MatrixExpression) e);
		else if (e instanceof LutExpression)
			rv = printLut((LutExpression) e);
		else
		{
			try
			{
				rv = e.toString();
			}
			catch (AutomatonExportException ex)
			{
				throw new RuntimeException("No print method defined in ExpressionPrinter for type "
						+ e.getClass().getName());
			}
		}

		return rv;
	}

	protected String printLut(LutExpression l)
	{
		return l.toString(this);
	}

	protected String printMatrix(MatrixExpression m)
	{
		return m.toString(this);
	}

	protected String printVariable(Variable v)
	{
		return v.name;
	}

	protected String printConstant(Constant c)
	{
		String rv = null;

		if (c == Constant.TRUE)
			rv = printTrue();
		else if (c == Constant.FALSE)
			rv = printFalse();
		else
			rv = printConstantValue(c.getVal());

		return rv;
	}

	protected String printConstantValue(double d)
	{
		return "" + d;
	}

	protected String printTrue()
	{
		return "true";
	}

	protected String printFalse()
	{
		return "false";
	}

	// although Operators are technically not expressions, it's better to define
	// this here to keep the printing all in once place
	public abstract String printOperator(Operator op);

	/**
	 * Prefix printing for everything
	 * 
	 * @param o
	 * @return
	 */
	protected String printOperation(Operation o)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(printOperator(o.op));

		for (Expression e : o.children)
		{
			sb.append(" ");
			sb.append(print(e));
		}

		return "(" + sb.toString() + ")";
	}
}

/**
 * 
 */
package com.verivital.hyst.grammar.formula;

/**
 * A Real number in an expression.
 */
public class Constant extends Expression
{
	public static final Constant TRUE = new Constant(1);
	public static final Constant FALSE = new Constant(0);

	private double val;

	public Constant(double value)
	{
		val = value;
	}

	public double getVal()
	{
		return val;
	}

	public void setVal(double v)
	{
		if (this == TRUE)
			throw new RuntimeException("setValue called on Constant.TRUE");

		if (this == FALSE)
			throw new RuntimeException("setValue called on Constant.FALSE");

		val = v;
	}

	@Override
	public Expression copy()
	{
		Expression rv = null;

		if (this == TRUE)
			rv = TRUE;
		else if (this == FALSE)
			rv = FALSE;
		else
			rv = new Constant(val);

		return rv;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Constant && ((Constant) o).val == val;
	}
}

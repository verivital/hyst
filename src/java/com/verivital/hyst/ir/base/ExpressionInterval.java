package com.verivital.hyst.ir.base;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.ExpressionPrinter;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;

/**
 * An ExpressionInterval an expression plus an Interval. It is used, for example, to specify
 * nondeterministic flows such as "x' = 2*y + [3, 4]". In this case, the Expression would be 2*y and
 * the interval would be [3, 4].
 * 
 * ExpressionIntervals always have a defined expression, but the interval may e null
 * 
 * @author Stanley Bak
 *
 */
public class ExpressionInterval
{
	private Expression e = null;
	private Interval i = null;

	public ExpressionInterval(Expression e, Interval i)
	{
		if (e == null)
			throw new AutomatonExportException(
					"Attempted to define ExpressionInterval with null expression.");

		this.e = e;
		this.i = i;
	}

	public ExpressionInterval(Expression e)
	{
		this(e, null);
	}

	public ExpressionInterval(Expression e, double min, double max)
	{
		this(e, new Interval(min, max));
	}

	public ExpressionInterval(double d)
	{
		this(new Constant(d));
	}

	/**
	 * Create from a text expression (interval is null)
	 * 
	 * @param text
	 *            the text to parse, like 'x + y * 2'
	 */
	public ExpressionInterval(String text)
	{
		this(FormulaParser.parseValue(text));
	}

	public ExpressionInterval(String expString, Interval i)
	{
		this(FormulaParser.parseValue(expString), i);
	}

	public ExpressionInterval copy()
	{
		ExpressionInterval rv = null;

		if (i == null)
			rv = new ExpressionInterval(e.copy(), null);
		else
			rv = new ExpressionInterval(e.copy(), new Interval(i));

		return rv;
	}

	public Expression getExpression()
	{
		return e;
	}

	public void setExpression(Expression e)
	{
		this.e = e;
	}

	public Interval getInterval()
	{
		return i;
	}

	public void setInterval(Interval i)
	{
		this.i = i;
	}

	/**
	 * Get the Expression which equals this ExpressionInterval exactly. This is only valid if
	 * min==max==0, otherwise an AutomatonExportException is raised.
	 * 
	 * @return the Expression
	 * @throws AutomatonExportException
	 *             if the interval is not [0,0]
	 */
	public Expression asExpression()
	{
		if (i != null)
		{
			// check if we can convert to just an expression
			if (i.isPoint() && e instanceof Constant)
			{
				Constant c = (Constant) e;

				c.setVal(c.getVal() + i.min);

				i = null;
			}
			else
				throw new AutomatonExportException(
						"ExpressionInterval.asExpression called, but interval is nonnull and"
								+ " not convertable: " + this
								+ " (is there explicit support for ExpressionIntervals?)");
		}
		return e;
	}

	/**
	 * Default printing. If you want something different you'll need to manually write it externally
	 * as this type is not part of ExpressionPrinter
	 */
	public String toString()
	{
		return toString(Expression.expressionPrinter);
	}

	/**
	 * Add a range to the range defined in this ExpresionInterval. This has the effect of min =
	 * left.min + right.min, max = left.max + right.max;
	 * 
	 * @param range
	 */
	public void addToInterval(Interval range)
	{
		if (i == null)
			i = new Interval(range);
		else
		{
			i.min += range.min;
			i.max += range.max;
		}
	}

	/**
	 * Is this ExpressionInterval exactly equal to the given interval?
	 * 
	 * @param range
	 * @return
	 */
	public boolean equalsInterval(Interval range)
	{
		boolean rv = false;

		if (e instanceof Constant)
		{
			double expVal = ((Constant) e).getVal();

			double min = expVal;
			double max = expVal;

			if (i != null)
			{
				min += i.min;
				max += i.max;
			}

			rv = new Interval(min, max).equals(range);
		}
		return rv;
	}

	@Override
	public boolean equals(Object o)
	{
		boolean rv = false;

		if (o instanceof ExpressionInterval)
		{
			ExpressionInterval other = (ExpressionInterval) o;

			rv = toString(DefaultExpressionPrinter.instance)
					.equals(other.toString(DefaultExpressionPrinter.instance)); // not
																				// ideal
																				// but
																				// it
																				// works
		}

		return rv;
	}

	public boolean isNondeterministicAssignment()
	{
		if (i != null)
		{
			return e.equals(new Constant(0))
					&& i.equals(Interval.NONDETERMINISTIC_ASSIGNMENT_INTERVAL);
		}
		else
		{
			return false;
		}
	}

	public boolean isInterval()
	{
		if (i != null)
		{
			return e.equals(new Constant(0)) && i.width() > 0;
		}
		else
		{
			return false;
		}
	}

	public String toString(ExpressionPrinter printer)
	{
		String rv = printer.print(e);

		if (i != null)
			rv += " " + printer.printOperator(Operator.ADD) + " ["
					+ printer.print(new Constant(i.min)) + ", " + printer.print(new Constant(i.max))
					+ "]";

		return rv;
	}

	public String toDefaultString()
	{
		return toString(DefaultExpressionPrinter.instance);
	}
}

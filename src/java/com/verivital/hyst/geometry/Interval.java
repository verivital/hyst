package com.verivital.hyst.geometry;

import java.util.Map;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;

/**
 * 
 * @author Stanley Bak
 *
 */
public class Interval
{
	public static double COMPARE_TOL = 1e-10;
	public static final Interval NONDETERMINISTIC_ASSIGNMENT_INTERVAL = new Interval(
			Integer.MIN_VALUE, Integer.MAX_VALUE);
	public double min = 0, max = 0;

	public Interval()
	{

	}

	public Interval(double val)
	{
		min = max = val;
	}

	public Interval(double min, double max)
	{
		this.min = min;
		this.max = max;

		if (min > max)
			throw new RuntimeException("min > max in Interval");
	}

	/**
	 * Deep copy from another interval
	 * 
	 * @param i
	 *            the interval to copy from
	 */
	public Interval(Interval i)
	{
		min = i.min;
		max = i.max;
	}

	/**
	 * Get the constant value of this interval. If this interval is not a constant, then an
	 * AutomatonExportException is raised. You can check if it's a constant by calling isPoint() or
	 * isConstant()
	 * 
	 * @return the constant value of this interval
	 */
	public double asConstant()
	{
		double rv;

		if (isPoint())
			rv = min;
		else
			throw new AutomatonExportException(
					"Interval.asConstant() called on an Interval that had a range: " + this);

		return rv;
	}

	/**
	 * Does this interval contain a certain values. This does not count the endpoints as part of the
	 * interval.
	 * 
	 * @param d
	 *            the value to check
	 * @return true iff min < d < max
	 */
	public boolean containsExclusive(double d)
	{
		return min < d && d < max;
	}

	/**
	 * Does this interval contain a certain value? This includes the endpoints as part of the
	 * interval.
	 * 
	 * @param d
	 *            the value to check
	 * @return true iff min < d < max
	 */
	public boolean contains(double d)
	{
		return min <= d && d <= max;
	}

	public boolean contains(Interval i)
	{
		return min <= i.min && i.max <= max;
	}

	@Override
	public String toString()
	{
		return "[Interval: (" + min + ", " + max + ")]";
	}

	/**
	 * get the middle of this interval
	 * 
	 * @return the average of the two endpoints
	 */
	public double middle()
	{
		return (max + min) / 2.0;
	}

	public double width()
	{
		return max - min;
	}

	/**
	 * Is this interval a single real-valued point (min==max, up to a small tolerance).
	 * 
	 * @return true iff min ~= max and min and max are not infinite or NaN
	 */
	public boolean isConstant()
	{
		return isPoint();
	}

	/**
	 * Is this interval a single real-valued point (min==max, up to a small tolerance).
	 * 
	 * @return true iff min ~= max and min and max are not infinite or NaN
	 */
	public boolean isPoint()
	{
		boolean rv = false;
		double TOL = 0.0000000001;

		if (!Double.isInfinite(max) && !Double.isNaN(max)
				&& (max == min || (max >= min && max - min < TOL)))
			rv = true;

		return rv;
	}

	public double randomPoint()
	{
		return Math.random() * (max - min) + min;
	}

	public boolean isExactly(double val)
	{
		return this.max == val && this.min == val;
	}

	public void set(double min, double max)
	{
		this.min = min;
		this.max = max;

		if (min > max)
			throw new RuntimeException("min > max in Interval");
	}

	/**
	 * Set the interval to a constant
	 * 
	 * @param value
	 *            the value to set both min and max to
	 */
	public void set(double value)
	{
		set(value, value);
	}

	/**
	 * Trim an interval to be restricted to the trimming interval
	 * 
	 * @param trimmingInterval
	 *            the interval to trim to
	 * @throws RuntimeException
	 *             if the intervals are nonoverlapping
	 */
	public void trim(Interval trimmingInterval)
	{
		if (min > trimmingInterval.max || max < trimmingInterval.min)
			throw new RuntimeException("Trim called with nonoverlapping interval: " + this
					+ " trimmed to " + trimmingInterval);

		if (min < trimmingInterval.min)
			min = trimmingInterval.min;

		if (max > trimmingInterval.max)
			max = trimmingInterval.max;
	}

	/**
	 * Compute the intersection of two intervals, returns null if empty
	 * 
	 * @return the overlap, or null if they are disjoint
	 */
	public static Interval intersection(Interval a, Interval b)
	{
		Interval rv = new Interval();

		rv.min = Math.max(a.min, b.min);
		rv.max = Math.min(a.max, b.max);

		if (rv.max < rv.min)
			rv = null;

		return rv;
	}

	public static Interval union(Interval a, Interval b)
	{
		Interval rv = new Interval();

		rv.min = Math.min(a.min, b.min);
		rv.max = Math.max(a.max, b.max);

		return rv;
	}

	/**
	 * Expand this interval to include the given value
	 * 
	 * @param v
	 *            the value to include
	 */
	public void expand(double v)
	{
		if (v < min)
			min = v;

		if (v > max)
			max = v;
	}

	public boolean isOpenInterval()
	{
		return min == -Double.MAX_VALUE || max == Double.MAX_VALUE;
	}

	public static Interval add(Interval i, Interval j)
	{
		Interval rv = new Interval();
		rv.min = i.min + j.min;
		rv.max = i.max + j.max;

		return rv;
	}

	/**
	 * Perform an interval evaluation of the passed-in expression
	 * 
	 * @param e
	 *            the expression to evaluate
	 * @param ranges
	 *            the ranges for any variables in e
	 * @return the resultant range
	 */
	public static Interval intervalEvaluate(Expression e, Map<String, Interval> ranges)
	{
		Interval rv = null;

		try
		{
			rv = intervalEvaluateRec(e, ranges);
		}
		catch (AutomatonExportException ex)
		{
			throw new AutomatonExportException(
					"Error performing interval evaluation on expression: " + e, ex);
		}

		return rv;
	}

	private static Interval intervalEvaluateRec(Expression e, Map<String, Interval> ranges)
	{
		Interval rv = null;

		if (e instanceof Variable)
		{
			String varName = ((Variable) e).name;

			Interval i = ranges.get(varName);

			if (i == null)
				throw new AutomatonExportException(
						"Range of variable " + varName + " is needed, but wasn't provided.");

			rv = new Interval(i);
		}
		else if (e instanceof Constant)
		{
			double d = ((Constant) e).getVal();

			rv = new Interval(d);
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			Interval left = null;
			Interval right = null;

			if (o.children.size() == 2)
			{
				left = intervalEvaluateRec(o.getLeft(), ranges);
				right = intervalEvaluateRec(o.getRight(), ranges);
			}

			switch (o.op)
			{
			case ADD:
				rv = new Interval(left.min + right.min, left.max + right.max);
				break;
			case SUBTRACT:
				rv = new Interval(left.min - right.max, left.max - right.min);
				break;
			case NEGATIVE:
			{
				rv = intervalEvaluateRec(o.children.get(0), ranges);

				double oldMin = rv.min;

				rv.min = -rv.max;
				rv.max = -oldMin;
				break;
			}
			case MULTIPLY:
			{
				double a = left.min * right.min;
				double b = left.min * right.max;
				double c = left.max * right.min;
				double d = left.max * right.max;

				double min = Math.min(Math.min(a, b), Math.min(c, d));
				double max = Math.max(Math.max(a, b), Math.max(c, d));

				rv = new Interval(min, max);

				break;
			}
			case DIVIDE:
			{
				if (right.min <= 0 && right.max >= 0)
					throw new AutomatonExportException(
							"Interval division contains zero in denominator.");

				double a = left.min * 1.0 / right.min;
				double b = left.min * 1.0 / right.max;
				double c = left.max * 1.0 / right.min;
				double d = left.max * 1.0 / right.max;

				double min = Math.min(Math.min(a, b), Math.min(c, d));
				double max = Math.max(Math.max(a, b), Math.max(c, d));

				rv = new Interval(min, max);
				break;
			}
			case LN:
			{
				Interval i = intervalEvaluateRec(o.children.get(0), ranges);

				if (i.min <= 0)
					throw new AutomatonExportException(
							"Interval evaluate of ln with min <= 0: " + i.min);

				rv = new Interval(Math.log(i.min), Math.log(i.max));
				break;
			}
			case SQRT:
			{
				Interval i = intervalEvaluateRec(o.children.get(0), ranges);

				if (i.min < 0)
					throw new AutomatonExportException(
							"Interval evaluate of sqrt with min < 0: " + i.min);

				rv = new Interval(Math.sqrt(i.max), Math.sqrt(i.min));
				break;
			}
			case POW:
				rv = intervalPow(left, right);
				break;
			case EXP:
				Interval i = intervalEvaluateRec(o.children.get(0), ranges);
				rv = intervalPow(new Interval(Math.E), i);
				break;
			case SIN:
			case COS:
			case TAN:
				// these could be done using Algorithm 2 on page 29 of Xin
				// Chen's dissertation
				// tan would need division tan(x) = sin(x) / cos(x)
				throw new AutomatonExportException(
						"Operator is not yet supported (submit a feature request if you need it): "
								+ o.op.name());

			case AND:
			case EQUAL:
			case GREATER:
			case GREATEREQUAL:
			case LESS:
			case LESSEQUAL:
			case LOC:
			case LOGICAL_NOT:
			case NOTEQUAL:
			case OR:
			default:
				throw new AutomatonExportException("Operator is not supported: " + o.op.name());

			}
		}
		else
			throw new AutomatonExportException("Unsupported Expression Type: " + e);

		return rv;
	}

	/**
	 * Compute the power function for intervals. Based on Algorithm 1 from Xin Chen's dissertation
	 * (Page 28)
	 * 
	 * @param left
	 *            the left interval
	 * @param right
	 *            the right interval (must be a nonnegative constant);
	 * @return the interval evaluation of the interval
	 */
	private static Interval intervalPow(Interval left, Interval right)
	{
		if (right.min != right.max)
			throw new AutomatonExportException("Interval value in exponent not supported.");

		double val = right.min;

		if ((val != Math.floor(val)) || Double.isInfinite(val) || val < 0)
			throw new AutomatonExportException(
					"Only nonnegative integer exponents are supproted: " + val);

		long n = (long) Math.floor(val);

		double a = left.min;
		double b = left.max;
		double c, d;

		if (n % 2 == 1)
		{
			c = Math.pow(a, n);
			d = Math.pow(b, n);
		}
		else
		{
			if (a >= 0)
			{
				c = Math.pow(a, n);
				d = Math.pow(b, n);
			}
			else if (b < 0)
			{
				c = Math.pow(b, n);
				d = Math.pow(a, n);
			}
			else
			{
				c = 0;
				d = Math.max(Math.pow(a, n), Math.pow(b, n));
			}
		}

		return new Interval(c, d);
	}

	public static Interval mult(Interval i, Interval j)
	{
		double a = i.min;
		double b = i.max;
		double c = j.min;
		double d = j.max;

		double min = Math.min(Math.min(a * c, a * d), Math.min(b * c, b * d));
		double max = Math.max(Math.max(a * c, a * d), Math.max(b * c, b * d));

		return new Interval(min, max);
	}

	public static Interval mult(Interval i, double c)
	{
		Interval rv = new Interval(i);
		rv.min *= c;
		rv.max *= c;

		if (c < 0)
		{
			double temp = rv.min;
			rv.min = rv.max;
			rv.max = temp;
		}

		return rv;
	}

	public void validate()
	{
		if (min > max)
			throw new RuntimeException("Interval min > max");
	}

	public boolean isMaxOpen()
	{
		return max == Double.MAX_VALUE;
	}

	public boolean isMinOpen()
	{
		return min == -Double.MAX_VALUE;
	}

	public Interval copy()
	{
		return new Interval(this);
	}

	@Override
	public boolean equals(Object o)
	{
		return equals(o, COMPARE_TOL);
	}

	/**
	 * Comparison with explicit tolerance for floating-point difference
	 * 
	 * @param o
	 *            the object to compare
	 * @param tol
	 *            the floating-point tolerance to consider equal
	 * @return true if intervals are the same
	 */
	public boolean equals(Object o, double tol)
	{
		boolean rv = false;

		if (o instanceof Interval)
		{
			Interval rhs = (Interval) o;

			if (Math.abs(rhs.max - max) < tol && Math.abs(rhs.min - min) < tol)
				rv = true;
		}

		return rv;
	}
}

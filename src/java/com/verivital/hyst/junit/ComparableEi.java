package com.verivital.hyst.junit;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * This is an ExpressionInterval wrapper used during unit testing. It uses a numerical sampling
 * strategy in order to compare expression intervals
 * 
 * @author Stanley Bak
 *
 */
public class ComparableEi
{
	private ExpressionInterval ei;
	private double tol = 1e-9;

	public ComparableEi(String expStr, Interval i, double tol)
	{
		this.ei = new ExpressionInterval(expStr, i);
		this.tol = tol;
	}

	public ComparableEi(ExpressionInterval ei, double tol)
	{
		this.ei = ei;
		this.tol = tol;
	}

	public boolean equals(Object other)
	{
		boolean rv = false;

		if (other instanceof ComparableEi)
		{
			ComparableEi otherCei = (ComparableEi) other;
			ExpressionInterval a = ei;
			ExpressionInterval b = otherCei.ei;

			String msg = AutomatonUtil.areExpressionsEqual(a.getExpression(), b.getExpression(),
					tol);

			rv = (msg == null);

			if (rv)
			{
				// check intervals
				Interval aI = a.getInterval();
				Interval bI = b.getInterval();

				if ((aI != null && bI == null) || (bI == null && aI != null))
					rv = false;
				else if (aI != null && bI != null)
					rv = aI.equals(bI, tol);
			}
		}

		return rv;
	}

	public String toString()
	{
		return ei.toString();
	}
}

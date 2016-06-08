/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import java.util.Arrays;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A lookup table (LUT) as part of an expression. Currently these are only allowed inside flow
 * expressions.
 * 
 * @author Stanley Bak
 *
 */
public class LutExpression extends Expression
{
	public Expression[] inputs; // length >= 1
	public MatrixExpression table;
	public double[][] breakpoints; // height = number of variables, width[i] =
									// length of dimension i of table

	/**
	 * Look up table constructor. Shallow copies of the passed-in arrays are stored
	 * 
	 * @param inputs
	 *            the inputs (usually variables)
	 * @param data
	 *            the table data
	 * @param breakpoints
	 *            the breakpoints for each variable
	 */
	public LutExpression(Expression inputs[], MatrixExpression data,
			MatrixExpression... breakpoints)
	{
		int len = inputs.length;

		if (len == 0)
			throw new AutomatonExportException("vars length must be at least 1");

		if (len != data.getNumDims())
			throw new AutomatonExportException(
					"nums vars must equal the number of dimensions in lookup table data");

		if (len != breakpoints.length)
			throw new AutomatonExportException(
					"nums vars must equal the number of breakpoint arrays");

		this.inputs = new Expression[len];

		for (int v = 0; v < len; ++v)
			this.inputs[v] = inputs[v].copy();

		this.table = new MatrixExpression(data);

		this.breakpoints = new double[len][];
		for (int d = 0; d < len; ++d)
		{
			MatrixExpression bp = breakpoints[d];

			if (bp.getNumDims() != 1)
				throw new RuntimeException("breakpoints must be 1-d arrays");

			if (bp.getDimWidth(0) < 2)
				throw new RuntimeException("breakpoints[" + d + "] must be at least of size 2");

			if (bp.getDimWidth(0) != data.getDimWidth(d))
				throw new RuntimeException("breakpoints[" + d + "] size(" + bp.getDimWidth(0)
						+ ") must be equal to width of data in table for that dimension ("
						+ data.getDimWidth(d) + ")");

			// breakpoints should be strictly increasing
			Expression first = bp.get(0);

			if (!(first instanceof Constant))
				throw new AutomatonExportException(
						"Breakpoints must be numeric constants: " + first.toDefaultString());

			double last = ((Constant) first).getVal();

			for (int bi = 0; bi < bp.getDimWidth(0); ++bi)
			{
				Expression curExp = bp.get(0);

				if (!(curExp instanceof Constant))
					throw new AutomatonExportException(
							"Breakpoints must be numeric constants: " + curExp.toDefaultString());

				double cur = ((Constant) curExp).getVal();

				if (cur < last)
					throw new AutomatonExportException(
							"Breakpoints must be strictly increasing: " + bp);

				last = cur;
			}

			double[] row = new double[bp.getDimWidth(0)];
			for (int i = 0; i < bp.getDimWidth(0); ++i)
			{
				Expression e = bp.get(i);

				if (!(e instanceof Constant))
					throw new AutomatonExportException(
							"breakpoint in LUT must be a numeric constant: " + e.toDefaultString());

				row[i] = ((Constant) e).getVal();
			}

			this.breakpoints[d] = row;
		}
	}

	private static MatrixExpression[] convertBreakPoints(LutExpression l)
	{
		MatrixExpression[] bps = new MatrixExpression[l.breakpoints.length];

		for (int i = 0; i < l.breakpoints.length; ++i)
			bps[i] = new MatrixExpression(l.breakpoints[i]);

		return bps;
	}

	/**
	 * Copy constuctore
	 * 
	 * @param lut
	 */
	public LutExpression(LutExpression l)
	{
		this(l.inputs, l.table, convertBreakPoints(l));
	}

	@Override
	public Expression copy()
	{
		return new LutExpression(this);
	}

	public String toString(ExpressionPrinter printer)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("lut([");

		boolean first = true;

		for (Expression var : inputs)
		{
			if (first)
				first = false;
			else
				sb.append(", ");

			sb.append(printer.print(var));
		}

		sb.append("], ");
		sb.append(table.toString(printer));

		for (int i = 0; i < breakpoints.length; ++i)
		{
			double[] bp = breakpoints[i];

			sb.append(", ");
			sb.append(Arrays.toString(bp));
		}

		sb.append(")");

		return sb.toString();
	}

	public String toDefaultString()
	{
		return toString(DefaultExpressionPrinter.instance);
	}
}

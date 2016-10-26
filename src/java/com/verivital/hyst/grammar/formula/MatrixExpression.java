/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;

/**
 * A matrix expression is one defined using matlab-like syntax: [1, 2; 3 4]
 * 
 * It can also be n-dimension, through the reshape function reshape([1,1,2,2,3,3,4,4],2,2,2) would
 * be a 2x2x2 matrix
 * 
 * They can be used, for example, to specify look up tables. They must be at least one dimensional,
 * and each dimension must be at least width 1
 */
public class MatrixExpression extends Expression implements Iterable<Entry<int[], Expression>>
{
	private int[] sizes; // the size of each dimension, x y z
	private Expression[] data; // the data for each cell (should be length
								// size[0] * size[1] * ...)

	// internally, the sizes and data arrays are the same order as the call to
	// reshape() (matlab's order)

	/**
	 * Copy constructor
	 * 
	 * @param other
	 */
	public MatrixExpression(MatrixExpression other)
	{
		this(other.data, other.sizes);
	}

	/**
	 * from array constructor
	 * 
	 * @param other
	 */
	public MatrixExpression(double... data)
	{
		sizes = new int[] { data.length };

		this.data = new Expression[data.length];

		for (int i = 0; i < data.length; ++i)
			this.data[i] = new Constant(data[i]);
	}

	/**
	 * from list of Expressions constructor
	 * 
	 * @param other
	 */
	public MatrixExpression(Expression... data)
	{
		sizes = new int[] { data.length };

		this.data = new Expression[data.length];

		for (int i = 0; i < data.length; ++i)
			this.data[i] = data[i];
	}

	/**
	 * Create a 2-d matrix. The order used in get() is BACKWARDS from the matrix order. This is to
	 * preserve the behavior in matlab
	 * 
	 * @param data
	 */
	public MatrixExpression(Expression[][] data)
	{
		if (data.length == 0 || data[0].length == 0)
			throw new AutomatonExportException("Matrix width must be at least 1");

		int total;

		if (data.length == 1)
		{
			// its actually a 1-d matrix
			sizes = new int[1];
			sizes[0] = data[0].length;

			total = sizes[0];
		}
		else
		{
			sizes = new int[2];
			sizes[0] = data.length;
			sizes[1] = data[0].length;

			total = sizes[0] * sizes[1];
		}

		this.data = new Expression[total];
		int index = 0;

		int numRows = data.length;
		int numCols = data[0].length;

		for (int col = 0; col < numCols; ++col)
		{
			for (int row = 0; row < numRows; ++row)
			{
				if (col == 0 && data[row].length != numCols)
					throw new AutomatonExportException(
							"Passed-in Expression[][] is not square, expected " + numCols
									+ " columns in row #" + row + ", but instead got "
									+ data[row].length);

				this.data[index++] = data[row][col].copy();
			}
		}
	}

	/**
	 * Create a new MatrixExpression from values given in the same order as matlab's reshape command
	 * 
	 * @param data
	 *            the matrix data, in the same order as matlab's reshape() command
	 * @param sizes
	 *            the sizes for each dimension
	 */
	public MatrixExpression(Expression[] data, int[] sizes)
	{
		int total = 1;

		for (int s : sizes)
		{
			if (s <= 0)
				throw new AutomatonExportException("Invalid matrix row width: " + s);

			total *= s;
		}

		if (data.length != total)
			throw new AutomatonExportException(
					"Invalid matrix data. Expected " + total + " entries, got " + data.length);

		this.sizes = new int[sizes.length];

		// copy sizes
		for (int i = 0; i < sizes.length; ++i)
			this.sizes[i] = sizes[i];

		// copy data
		this.data = new Expression[data.length];

		for (int i = 0; i < data.length; ++i)
			this.data[i] = data[i].copy();
	}

	@Override
	public Expression copy()
	{
		return new MatrixExpression(data, sizes);
	}

	public int getNumDims()
	{
		return sizes.length;
	}

	public int getDimWidth(int dimNum)
	{
		return sizes[dimNum];
	}

	/**
	 * Get an expression from this matrix
	 * 
	 * @param indices
	 *            the index for each dimension, ordered from largest offset to smallest offset
	 * @return
	 */
	public Expression get(int... indices)
	{
		if (sizes.length != indices.length)
			throw new IndexOutOfBoundsException(
					"Expected " + sizes.length + " indicies, got " + indices.length);

		for (int d = 0; d < sizes.length; ++d)
		{
			if (indices[d] < 0 || indices[d] >= sizes[d])
			{
				throw new IndexOutOfBoundsException("got " + Arrays.toString(indices)
						+ " with sizes " + Arrays.toString(sizes));
			}
		}

		int finalIndex = 0;
		int multiplier = 1;

		for (int j = 0; j < indices.length; ++j)
		{
			int index = indices[j];

			finalIndex += multiplier * index;
			multiplier *= sizes[j];
		}

		return data[finalIndex];
	}

	public void setExpressionAtIndex(int[] indices, Expression e)
	{
		if (sizes.length != indices.length)
			throw new IndexOutOfBoundsException(
					"Expected " + sizes.length + " indicies, got " + indices.length);

		for (int d = 0; d < sizes.length; ++d)
		{
			if (indices[d] < 0 || indices[d] >= sizes[d])
			{
				throw new IndexOutOfBoundsException("got " + Arrays.toString(indices)
						+ " with sizes " + Arrays.toString(sizes));
			}
		}

		int finalIndex = 0;
		int multiplier = 1;

		for (int j = 0; j < indices.length; ++j)
		{
			int index = indices[j];

			finalIndex += multiplier * index;
			multiplier *= sizes[j];
		}

		data[finalIndex] = e;
	}

	public String toString(ExpressionPrinter printer)
	{
		StringBuilder rv = new StringBuilder();

		if (sizes.length == 1)
			makeString1d(rv, printer);
		else if (sizes.length == 2)
			makeString2d(rv, printer);
		else
			makeStringReshape(rv, printer);

		return rv.toString();
	}

	/**
	 * Get the string representation of this matrix
	 */
	public String toDefaultString()
	{
		return toString(DefaultExpressionPrinter.instance);
	}

	public void makeStringReshape(StringBuilder rv, ExpressionPrinter printer)
	{
		rv.append("reshape([");
		boolean first = true;

		for (Expression e : data)
		{
			if (first)
				first = false;
			else
				rv.append(", ");

			rv.append(printer.print(e));
		}

		rv.append("]");

		for (int i = 0; i < sizes.length; ++i)
			rv.append(", " + sizes[i]);

		rv.append(")");
	}

	public void makeString2d(StringBuilder rv, ExpressionPrinter printer)
	{
		rv.append("[");
		int numRows = sizes[0];
		int numCols = sizes[1];

		for (int row = 0; row < numRows; ++row)
		{
			for (int col = 0; col < numCols; ++col)
			{
				if (col != 0)
					rv.append(", ");

				Expression e = get(row, col);
				rv.append(printer.print(e));
			}

			if (row != numRows - 1)
				rv.append(" ; ");
		}

		rv.append("]");
	}

	/**
	 * Get the table data as a 1-d array
	 * 
	 * @param rv
	 *            where to store the string
	 */
	public void makeString1d(StringBuilder rv, ExpressionPrinter printer)
	{
		rv.append("[");
		boolean first = true;

		for (Expression e : data)
		{
			if (first)
				first = false;
			else
				rv.append(", ");

			rv.append(printer.print(e));
		}

		rv.append("]");
	}

	@Override
	public Iterator<Entry<int[], Expression>> iterator()
	{
		return new MatrixEntryIterator(this);
	}

	/**
	 * An iterator for matrix expressions. Loop over every expression in the matrix.
	 * 
	 * @author Stanley Bak (11-2015)
	 *
	 */
	private class MatrixEntryIterator implements Iterator<Entry<int[], Expression>>
	{
		private MatrixExpression me;
		private int[] iterator;

		public MatrixEntryIterator(MatrixExpression me)
		{
			this.me = me;
			iterator = new int[me.getNumDims()];
		}

		@Override
		public boolean hasNext()
		{
			return iterator != null;
		}

		@Override
		public Entry<int[], Expression> next()
		{
			Entry<int[], Expression> e = new AbstractMap.SimpleEntry<int[], Expression>(iterator,
					me.get(iterator));

			iterator = incrementIterator();

			return e;
		}

		@Override
		public void remove()
		{
			throw new RuntimeException("iteartor.remove() not supported on Matrix");
		}

		/**
		 * Increment the iterator indexList (with overflowing to the next dimension if necessary)
		 * This returns null upon overflow (when done)
		 * 
		 * @return the incremented indexList
		 */
		private int[] incrementIterator()
		{
			boolean done = false;
			int[] rv = Arrays.copyOf(iterator, iterator.length);

			++rv[0];

			for (int d = 0; d < me.getNumDims(); ++d)
			{
				int size = me.getDimWidth(d);

				if (rv[d] >= size)
				{
					rv[d] -= size;

					if (d == me.getNumDims() - 1)
						done = true;
					else
						++rv[d + 1];
				}
			}

			if (done)
				rv = null;

			return rv;
		}
	}

	public static Expression fromRange(Expression startExp, Expression stepExp, Expression stopExp)
	{
		Expression start = SimplifyExpressionsPass.simplifyExpression(startExp);
		Expression step = SimplifyExpressionsPass.simplifyExpression(stepExp);
		Expression stop = SimplifyExpressionsPass.simplifyExpression(stopExp);

		if (!(start instanceof Constant))
			throw new AutomatonExportException(
					"Matrix start;step;stop expression should be constants. Got start = "
							+ startExp.toDefaultString());

		if (!(step instanceof Constant))
			throw new AutomatonExportException(
					"Matrix start;step;stop expression should be constants. Got step = "
							+ stepExp.toDefaultString());

		if (!(stop instanceof Constant))
			throw new AutomatonExportException(
					"Matrix start;step;stop expression should be constants. Got stop = "
							+ stopExp.toDefaultString());

		double min = ((Constant) start).getVal();
		double max = ((Constant) stop).getVal();
		double delta = ((Constant) step).getVal();

		if (max < min)
			throw new AutomatonExportException(
					"Matrix start;step;stop expression should have stop >= start");

		if (delta <= 0)
			throw new AutomatonExportException(
					"Matrix start;step;stop expression should have step > 0");

		ArrayList<Double> values = new ArrayList<Double>();

		double tol = 1e-9;

		if (delta < tol)
			tol = delta / 2.0;

		for (double d = min; d < max + tol; d += delta)
			values.add(d);

		double[] vals = new double[values.size()];

		for (int i = 0; i < values.size(); ++i)
			vals[i] = values.get(i);

		return new MatrixExpression(vals);
	}
}

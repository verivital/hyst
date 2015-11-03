/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A matrix expression is one defined using matlab-like syntax:
 * [1, 2; 3 4] 
 * 
 * It can also be n-dimension, through the reshape function
 * reshape([1,1,2,2,3,3,4,4],2,2,2) would be a 2x2x2 matrix
 * 
 * They can be used, for example, to specify look up tables. They must be at least one dimensional, and each
 * dimension must be at least width 1
 */
public class MatrixExpression extends Expression 
{
	private int[] sizes; // the size of each dimension, x y z
	private Expression[] data; // the data for each cell (should be length size[0] * size[1] * ...)
	
	// internally, the data array is in the same order as the call to reshape()
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public MatrixExpression(MatrixExpression other)
	{
		this(other.data, other.sizes);
	}
	
	/**
	 * from array constructor
	 * @param other
	 */
	public MatrixExpression(double ... data)
	{
		sizes = new int[]{data.length};
		
		this.data = new Expression[data.length];
		
		for (int i = 0; i < data.length; ++i)
			this.data[i] = new Constant(data[i]);
	}
	
	/**
	 * from list of Expressions constructor
	 * @param other
	 */
	public MatrixExpression(Expression ... data)
	{
		sizes = new int[]{data.length};
		
		this.data = new Expression[data.length];
		
		for (int i = 0; i < data.length; ++i)
			this.data[i] = data[i];
	}
	
	/**
	 * Create a 2-d matrix
	 * @param data
	 */
	public MatrixExpression(Expression[][] data)
	{
		if (data.length == 0 || data[0].length == 0)
			throw new AutomatonExportException("Matrix width must be at least 1");
		
		int total;
		
		if (data.length == 1)
		{
			// its acutally a 1-d matrix 
			sizes = new int[1];
			sizes[0] = data[0].length;
			
			total = sizes[0];
		}
		else
		{
			// sizes order is from last index to first index
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
					throw new AutomatonExportException("Passed-in Expression[][] is not square, expected " + numCols 
							+ " columns in row #" + row + ", but instead got " + data[row].length);
				
				this.data[index++] = data[row][col].copy();
			}
		}
	}
	
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
			throw new AutomatonExportException("Invalid matrix data. Expected " + total + " entries, got " + data.length);
		
		// copy sizes
		this.sizes = new int[sizes.length];
		
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
	 * @param indices the index for each dimension, ordered from largest offset to smallest offset
	 * @return
	 */
	public Expression get(int ... indices)
	{
		if (sizes.length != indices.length)
			throw new IndexOutOfBoundsException("Expected " + sizes.length + " indicies, got " + indices.length);
	
		int finalIndex = 0;
		int multiplier = 1;
		
		for (int j = indices.length - 1; j >= 0; --j)
		{
			int index = indices[j];
			
			finalIndex += multiplier * index;
			multiplier *= sizes[indices.length - 1-j];
		}
		
		return data[finalIndex];
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
		
		for (int s : sizes)
			rv.append(", " + s);
		
		rv.append(")");
	}
	
	public void makeString2d(StringBuilder rv, ExpressionPrinter printer)
	{
		rv.append("[");
		int numCols = sizes[1];
		int numRows = sizes[0];
		
		for (int row = 0; row < numRows; ++row)
		{
			for (int col = 0; col < numCols; ++col)
			{
				if (col != 0)
					rv.append(", ");
				
				Expression e = data[col * numRows + row];
				rv.append(printer.print(e));
			}
			
			if (row != numRows - 1)
				rv.append(" ; ");
		}
		
		rv.append("]");
	}

	/**
	 * Get the table data as a 1-d array 
	 * @param rv where to store the string
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
}

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
		
		// sizes order is from last index to first index
		sizes = new int[2];
		sizes[0] = data[0].length; // arg!
		sizes[1] = data.length;
		
		int total = sizes[0] * sizes[1];
		this.data = new Expression[total];
		int index = 0;
		
		for (int r = 0; r < data.length; ++r)
		{
			int w = data[r].length;
			
			if (w != sizes[0])
				throw new AutomatonExportException("Matrix width mismatch: row " + r + " is of length " + w 
						+ " but expected " + sizes[0]);
			
			for (int c = 0; c < w; ++c)
				this.data[index++] = data[r][c].copy();
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
	
	public int numDims()
	{
		return sizes.length;
	}
	
	public int getDimWidth(int dimNum)
	{
		return sizes[dimNum];
	}
	
	/**
	 * Get an expression from this matrix
	 * @param index the index for each dimension, ordered from largest offset to smallest offset
	 * @return
	 */
	public Expression get(int ... indicies)
	{
		if (sizes.length != indicies.length)
			throw new IndexOutOfBoundsException("Expected " + sizes.length + " indicies, got " + indicies.length);
		
		int finalIndex = 0;
		int multiplier = 1;
		
		for (int j = sizes.length-1; j >= 0 ; --j)
		{
			int index = indicies[j];
			
			finalIndex += multiplier * index;
			multiplier *= sizes[j];
		}
		
		return data[finalIndex];
	}
	
	/**
	 * Get the string representation of this matrix
	 */
	public String toString()
	{
		WORKING HERE
	}
}

/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A lookup table (LUT) as part of an expression. Currently these are only allowed inside flow expressions.
 * 
 * @author Stanley Bak
 *
 */
public class Lut extends Expression 
{
	public String[] variables;
	public MatrixExpression values; // at least 1x2, equal in size to breakpoints
	public MatrixExpression breakpoints; // at least 1x2

	/**
	 * Look up table constructor. Shallow copies of the passed-in arrays are stored
	 * @param vars
	 * @param vals
	 * @param breakpoints
	 */
	public Lut(String vars[], MatrixExpression vals, MatrixExpression breakpoints) 
	{
		int len = vars.length;
		
		if (len < 1 || len > 2)
			throw new AutomatonExportException("Only 1 or 2d lookup tables are supported");
		
		if (len == 1 && vals.length != 1)
			throw new AutomatonExportException("values must be a 1-d matrix");
		
		if (len == 1 && breakpoints.length != 1)
			throw new AutomatonExportException("breakpoints must be a 1-d matrix");
		
		if (len == 2 && vals.length < 2)
			throw new AutomatonExportException("values must be a 2-d matrix");
		
		if (len == 2 && breakpoints.length < 2)
			throw new AutomatonExportException("breakpoints must be a 2-d matrix");
		
		int h = vals.length;
		int w = vals[0].length;
		
		if (h != breakpoints.length)
			throw new AutomatonExportException("values and breakpoints matricies have different heights");
		
		if (w != breakpoints[0].length)
			throw new AutomatonExportException("values and breakpoints matricies have different widths");
		
		this.variables = vars;
		this.values = vals;
		this.breakpoints = breakpoints;
	}

	@Override
	public Expression copy() 
	{
		// deep copy
		String vars[] = new String[variables.length];
		double vals[][] = new double[values.length][values[0].length];
		double bps[][] = new double[breakpoints.length][breakpoints[0].length];
		
		for (int i = 0; i < variables.length; ++i)
			vars[i] = variables[i];
		
		for (int i = 0; i < values.length; ++i)
			for (int j = 0; j < values[0].length; ++j)
				vals[i][j] = values[i][j];
		
		for (int i = 0; i < breakpoints.length; ++i)
			for (int j = 0; j < breakpoints[0].length; ++j)
				bps[i][j] = breakpoints[i][j];
		
		return new Lut(vars, vals, bps);
	}
}

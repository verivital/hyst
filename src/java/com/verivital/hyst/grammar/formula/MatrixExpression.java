/**
 * 
 */
package com.verivital.hyst.grammar.formula;

/**
 * A matrix expression is one defined using matlab-like syntax:
 * [1, 2; 3 4] 
 * 
 * They can be used, for example, to specify look up tables.
 */
public class MatrixExpression extends Expression 
{
	public String name;

	public MatrixExpression(String name) 
	{
		this.name = name;
	}

	@Override
	public Expression copy() 
	{
		return new MatrixExpression(name);
	}
}

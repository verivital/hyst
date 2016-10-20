package com.verivital.hyst.grammar.formula;

/**
 * General parent class for expressions.
 * 
 * Expressions can be Constants, Variables, or Operations (like +, -, &&, ==, or cos)
 * 
 * To print expressions differently, you should inherit from ExpressionPrinter, override any methods
 * you want to change, and then assign Expression.expressionPrinter
 */
public abstract class Expression
{
	// this printer should be assigned as-needed by printers or passes. Use
	// DefaultExpressionPrinter.instance for a reasonable default.
	public static ExpressionPrinter expressionPrinter = null;

	// void setParent(Operation aThis) {
	// throw new UnsupportedOperationException("Not supported yet."); //To
	// change body of generated methods, choose Tools | Templates.
	// }

	// control the way expressions are printed
	enum PrintMode
	{
		INLINE, PREFIX,
	};

	public static PrintMode printMode = PrintMode.INLINE;

	private Operation mParent;

	public Expression(Operation parent)
	{
		setParent(parent);
	}

	public Expression()
	{
		setParent(null);
	}

	public Operation getParent()
	{
		return mParent;
	}

	public void setParent(Operation parent)
	{
		mParent = parent;
	}

	public abstract Expression copy();

	/**
	 * Get this expression as an operation (if it is one), or null (if it's not)
	 * 
	 * @return
	 */
	public Operation asOperation()
	{
		Operation rv = null;

		if (this instanceof Operation)
			rv = (Operation) this;

		return rv;
	}

	@Override
	public String toString()
	{
		return expressionPrinter.print(this);
	}

	/**
	 * Create an AND operation with this and the given expression. This is a short-circuited AND
	 * which will simplify if either expression is TRUE or FALSE.
	 * 
	 * @param e
	 *            the other expression
	 */
	public static Expression and(Expression i, Expression j)
	{
		Expression rv;

		if (i == null || j == null)
			throw new RuntimeException("parameter to Expression.and() was null");

		if (i == Constant.FALSE || j == Constant.FALSE)
			rv = Constant.FALSE;
		else if (i == Constant.TRUE)
			rv = j;
		else if (j == Constant.TRUE)
			rv = i;
		else
			rv = new Operation(Operator.AND, i, j);

		return rv;
	}

	public static Expression or(Expression i, Expression j)
	{
		Expression rv;

		if (i == null || j == null)
			throw new RuntimeException("parameter to Expression.or() was null");

		if (i == Constant.TRUE || j == Constant.TRUE)
			rv = Constant.TRUE;
		else if (i == Constant.FALSE)
			rv = j;
		else if (j == Constant.FALSE)
			rv = i;
		else
			rv = new Operation(Operator.OR, i, j);

		return rv;
	}

	public String toDefaultString()
	{
		return DefaultExpressionPrinter.instance.print(this);
	}
}

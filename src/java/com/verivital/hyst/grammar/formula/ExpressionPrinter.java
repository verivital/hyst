package com.verivital.hyst.grammar.formula;



public abstract class ExpressionPrinter
{
	public String print(Expression e)
	{
		String rv = null;

		if (e == null)
			rv = "null";
		else if (e instanceof Constant)
			rv = printConstant((Constant) e);
		else if (e instanceof Operation)
			rv = printOperation((Operation) e);
		else if (e instanceof Variable)
			rv = printVariable((Variable) e);
		else
			rv = printOther(e);
		
		return rv;
	}
	
	public String printOther(Expression e)
	{
		return e.toString();
	}
	
	public String printVariable(Variable v)
	{
		return v.name;
	}
	
	public String printConstant(Constant c)
	{
		String rv = null;
		
		if (c == Constant.TRUE)
			rv = printTrue();
		else if (c == Constant.FALSE)
			rv = printFalse();
		else
			rv = printConstantValue(c.getVal());

		return rv;
	}
	
	public String printConstantValue(double d)
	{
		return "" + d;
	}
	
	public String printTrue()
	{
		return "true";
	}
	
	public String printFalse()
	{
		return "false";
	}
	
	// although Operators are technically not expressions, it's better to define this here to keep the printing all in once place
	public abstract String printOperator(Operator op);
	
	/**
	 * Prefix printing for everything
	 * @param o
	 * @return
	 */
	public String printOperation(Operation o)
	{
		String childrenStr = "";
		
		for (Expression e : o.children)
			childrenStr += " " + print(e);
		
		return "(" + printOperator(o.op) + childrenStr + ")";
	}
}

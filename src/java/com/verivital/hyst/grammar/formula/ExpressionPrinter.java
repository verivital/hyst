package com.verivital.hyst.grammar.formula;

import java.util.ArrayList;



public abstract class ExpressionPrinter
{
	
	public ArrayList <String> ControlVar = new ArrayList <String>();
	public ArrayList <String> UncontrolVar = new ArrayList <String>();
	
	public String print(Expression e)
	{
		String rv = null;
		if (e == null)
			rv = "null";
		else if (e instanceof Constant)
		{
			rv = printConstant((Constant) e);
		}
		else if (e instanceof Operation)
		{
			rv = printOperation((Operation) e);
		}
		else if (e instanceof Variable)
		{
			rv = printVariable((Variable) e);
		}
		else
		{
			rv = printOther(e);
		}
		return rv;
	}

	public String printExspeedFirst(Expression e, int j, ArrayList<String>Control, ArrayList<String>Uncontrol)
	{
		String rv = null;
		ControlVar = Control;
		UncontrolVar = Uncontrol;
		//System.out.println("Inside "+j);
			if (e == null)
				rv = "null";
			else if (e instanceof Constant && Integer.parseInt(e.toString())!=0)
			{
			rv = e.toString();
			//System.out.println("constant :"+rv);
			saveCmatrix(rv,j);
			}
			else if (e instanceof Operation)
			{
		 	rv = printOperationExspeed((Operation) e,j);
			//System.out.println("operation "+rv);
			}
			else if (e instanceof Variable)
			{
				if(ControlVar.contains(e.toString()))
				{
				rv = printVariable((Variable) e);
				//System.out.println("variable "+rv);
				saveAmatrix(rv,j);
				}
				if(UncontrolVar.contains(e.toString()))
				{
					rv = printVariable((Variable) e);
					//System.out.println("unvariable "+rv);
					saveBmatrix(rv,j);
				}
			}
			else
			{
				rv = printOther(e);
				//System.out.println("other"+rv);
			}
		return rv;
		
			
	}
	
	
	
	
	public String printExspeed(Expression e, int j)
	{

			String rv = null;
			if (e == null)
				rv = "null";
			else if (e instanceof Constant)
			{
			rv = printConstant((Constant) e);
			//System.out.println("constant :"+rv);
			}
			else if (e instanceof Operation)
			{
		 	rv = printOperationExspeed((Operation) e,j);
			//System.out.println("operation "+rv);
			}
			else if (e instanceof Variable)
			{
				rv = printVariable((Variable) e);
			//	System.out.println("variable "+rv);
			}
			else
			{
				rv = printOther(e);
			//	System.out.println("other"+rv);
			}
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
		{
			//System.out.println("");
			//System.out.println("");
			childrenStr += " " + print(e);
		}
		return "(" + printOperator(o.op) + childrenStr + ")";
	}
	public String printOperationExspeed(Operation o, int j)
	{
		String childrenStr = "";
		
		for (Expression e : o.children)
		{
			//System.out.println("");
			//System.out.println("");
			childrenStr += " " + print(e);
		}
		return "(" + printOperator(o.op) + childrenStr + ")";
	}
    public void saveAmatrix(String st,int j)
	{
		System.out.println("hello A");
	}
    public void saveBmatrix(String st,int j)
	{
		System.out.println("hello B");
	}
    public void saveCmatrix(String st,int j)
	{
		System.out.println("hello C");
	}
    
}



package com.verivital.hyst.grammar.formula;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.lang.*;

import com.verivital.hyst.ir.AutomatonExportException;

public class DefaultExpressionPrinter extends ExpressionPrinter
{
	public static DefaultExpressionPrinter instance = new DefaultExpressionPrinter(); 
	
	public DecimalFormat constFormatter;
	protected Map <Operator, String> opNames = new TreeMap <Operator, String>();
	int index,i,index0,indexU, indexU0;
	String [] st;
	String [] st1;
	double[][] cons ;
	double[][] Bmatrix;
	double [] constraint;
	public DefaultExpressionPrinter()
	{
		initializeData();
	}
	public DefaultExpressionPrinter(String [] vars, String [] cos, int con, int uncon)
	{
		cons = new double[con][con];
		Bmatrix = new double [con][uncon];
		for(int k=0; k<con;k++)
			for(int j=0;j<con;j++)
				cons[k][j]=0;
		for(int k=0; k<con;k++)
			for(int j=0;j<uncon;j++)
				Bmatrix[k][j]=0;
		
		st = new String[vars.length];
		st = vars;
		st1 = new String[cos.length];
		st1 = cos;
		constraint = new double[con];
		for(int k=0; k<con;k++)
				constraint[k]=0;
		initializeData();
	}
	private void initializeData(){
		
		constFormatter = new DecimalFormat("", new DecimalFormatSymbols(Locale.ENGLISH));
		constFormatter.setGroupingUsed(false);
		//constFormatter.setMinimumFractionDigits(1);
		constFormatter.setMinimumIntegerDigits(1);
		//System.out.println("I'm in default expression printer initialize");

		for (Operator o : Operator.values())
		{
			
			switch (o)
			{
				case ADD:
					opNames.put(Operator.ADD, "+");
					break;
				case AND:
					opNames.put(Operator.AND, "&");
					break;
				case COS:
					opNames.put(Operator.COS, "cos");
					break;
				case DIVIDE:
					opNames.put(Operator.DIVIDE, "/");
					break;
				case EQUAL:
					opNames.put(Operator.EQUAL, "=");
					break;
				case EXP:
					opNames.put(Operator.EXP, "exp");
					break;
				case GREATER:
					opNames.put(Operator.GREATER, ">");
					break;
				case GREATEREQUAL:
					opNames.put(Operator.GREATEREQUAL, ">=");
					break;
				case LESS:
					opNames.put(Operator.LESS, "<");
					break;
				case LESSEQUAL:
					opNames.put(Operator.LESSEQUAL, "<=");
					break;
				case LN:
					opNames.put(Operator.LN, "ln");
					break;
				case LOC:
					opNames.put(Operator.LOC, "loc");
					break;
				case MULTIPLY:
					opNames.put(Operator.MULTIPLY, "*");
					break;
				case NEGATIVE:
					opNames.put(Operator.NEGATIVE, "-");
					break;
				case NOTEQUAL:
					opNames.put(Operator.NOTEQUAL, "!=");
					break;
				case OR:
					opNames.put(Operator.OR, "|");
					break;
				case POW:
					opNames.put(Operator.POW, "^");
					break;
				case SIN:
					opNames.put(Operator.SIN, "sin");
					break;
				case SQRT:
					opNames.put(Operator.SQRT, "sqrt");
					break;
				case SUBTRACT:
					opNames.put(Operator.SUBTRACT, "-");
					break;
				case TAN:
					opNames.put(Operator.TAN, "tan");
					break;
				case LOGICAL_NOT:
					opNames.put(Operator.LOGICAL_NOT, "!");
					break;
				default:
					throw new RuntimeException("unsupported case: " + o);
			}
		}
	}
	
	@Override
	public String printConstantValue(double d)
	{
		return constFormatter.format(d);
	}
	
	@Override
	public String printOperator(Operator op)
	{
		return opNames.get(op);
	}
	
	/**
	 * Inline printing unless unary function
	 */
	@Override
	public String printOperation(Operation o)
	{
		String rv;
		List <Expression> children = o.children;
		Operator op = o.op;
		
		if (children.size() == 0)
			{
			rv = printOperator(o.op);
			}
		else if (children.size() == 1)
		{
			
	
			Expression child = children.get(0);	
			if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT))
			{
				if (child instanceof Operation && child.asOperation().children.size() > 1)
					rv = opNames.get(o.op) + "(" + print(child) + ")";
				else
					rv = opNames.get(o.op) + "" + print(child);
				
			}
			else
				{
				rv = opNames.get(o.op) + "(" + print(child) + ")";
			    }
			
			
		}
		else if (children.size() == 2)
		{
			Expression leftExp = children.get(0);
			Operation left = leftExp.asOperation();
			
			Expression rightExp = children.get(1);
			Operation right = rightExp.asOperation();
			
			boolean needParenLeft = false;
			boolean needParenRight = false;
			
			// use parentheses if they are needed
			int myP = Operator.getPriority(op);
			if (left != null && left.children.size() > 1)
			{
				int leftP = Operator.getPriority(left.op);
				
				if (leftP < myP)
					needParenLeft = true;
			}
			
			if (right != null && right.children.size() > 1)
			{
				int rightP = Operator.getPriority(right.op);
				
				if (myP > rightP || (myP == rightP && !Operator.isCommutative(op))) // commutative
					needParenRight = true;
			}
			
			rv = "";
			
			if (needParenLeft)
			   rv += "(" + print(leftExp) + ")"; 
			// maybe not strictly necessary as the expression.toString is overriden to call this print, but was having problems with this
			else
				{
				rv += print(leftExp);
				//System.out.println("the left expression is:"+ print(leftExp));
				}
			
			rv += " " + opNames.get(o.op) + " ";
			if (needParenRight)
				rv += "(" + print(rightExp) + ")";
			else
			{
				rv += print(rightExp);
				//System.out.println("the right expression is:"+ print(rightExp));
			}
		}
		else
		{
			throw new AutomatonExportException("No default way to in-line print expression with " + children.size() 
					+ " children (" + opNames.get(o.op) + ".");
		}
		return rv;
		
	}
	

	
	public String printOperationExspeed(Operation o, int j)
	{
		
		String rv;
		List <Expression> children = o.children;
		Operator op = o.op;
		
		if(op.equals(Operator.EQUAL))
		{
			if(children.get(1) instanceof Constant)
			{
				constraint[j] = Double.parseDouble(children.get(1).toString());
			}
		}
		
		if (children.size() == 0)
			{
			rv = printOperator(o.op);
			}
		else if (children.size() == 1)
		{
			
			//System.out.println("I'm in 1 child block");
			
			Expression child = children.get(0);
		    
			
			if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT))
			{
				if (child instanceof Operation && child.asOperation().children.size() > 1)
					rv = opNames.get(o.op) + "(" + printExspeed(child,j) + ")";
				else
					rv = opNames.get(o.op) + "" + printExspeed(child,j);
			//	System.out.println("negative : "+rv);
				
			}
			else
				{
				
				rv = opNames.get(o.op) + "(" + printExspeed(child,j) + ")";
			    }
			for(int l =0 ;l<st1.length;l++)
			{
				if(child.toString().equals(st1[l]))
				{
					//System.out.println(j+" () "+l+"  "+st1[l]);
					constraint[j] = Double.parseDouble(rv);
					break;
				}
			}
			
		}
		else if (children.size() == 2)
		{
			//System.out.println(children.toString());
			
			System.out.println("vvvv"+children.toString());
			
			Expression leftExp = children.get(0);
			Operation left = leftExp.asOperation();
			
			Expression rightExp = children.get(1);
			Operation right = rightExp.asOperation();
			
		
			boolean needParenLeft = false;
			boolean needParenRight = false;
			
			rv = "";
			// use parentheses if they are needed
			
			if(children.get(1) instanceof Variable && children.get(0) instanceof Constant && o.op.equals(Operator.MULTIPLY))
			{				
				for (int k = 0; k < ControlVar.size(); k++)
				{
					if(ControlVar.get(k).equals((children.get(1).toString())))
						{
						//System.out.println("hiii");
						cons[j][k] =Double.parseDouble(children.get(0).toString());
						//System.out.println("hiii    cons["+j+"]["+k+"]"+cons[j][k]);
						   break;
						}
				}
				for (int k = 0; k < UncontrolVar.size(); k++)
				{
				if(UncontrolVar.get(k).equals((children.get(1).toString())))
					{
					//System.out.println("hiii");
						Bmatrix[j][k] =Double.parseDouble(children.get(0).toString());
						//System.out.println("hiii"+Bmatrix[j][k]);
						break;
					}
				}
			}
			
			else if(( rightExp instanceof Constant) && (opNames.get(o.op)=="+" || opNames.get(o.op)=="-"))
			{
				//System.out.println("hiii");
				if(opNames.get(o.op)=="-")
				{
				constraint[j] = -Double.parseDouble(rightExp.toString());
				}
				else
				{
				constraint[j] = Double.parseDouble(rightExp.toString());
				}
				
				rv += printExspeed(leftExp,j);
			}
			/*
			if(leftExp instanceof Variable && rightExp instanceof Variable && opNames.get(o.op)=="+")
			{
				
				for (int k = 0; k < ControlVar.size(); k++)
				{
					if(ControlVar.contains((children.get(0).toString())))
						{
						   index0 = k;
						   break;
						}
				}
				
				for (int k = 0; k < ControlVar.size(); k++)
				{
					if(ControlVar.contains((children.get(1).toString())))
						{
						   index = k;
						   break;
						}
				}
				
				
				for (int k = 0; k < UncontrolVar.size(); k++)
				{
					if(UncontrolVar.contains((children.get(0).toString())))
						{
						   indexU0 = k;
						   break;
						}
				}
				
				for (int k = 0; k < UncontrolVar.size(); k++)
				{
					if(UncontrolVar.contains((children.get(1).toString())))
						{
						   indexU = k;
						   break;
						}
				}
				
				
				
				
			   cons[j][index]= 1;
			   cons[j][index0]= 1;
			//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			}
			
			if(leftExp instanceof Variable && rightExp instanceof Variable && opNames.get(o.op)=="-")
			{
				
				for (int k = 0; k < ControlVar.size(); k++)
				{
					if(ControlVar.contains((children.get(0).toString())))
						{
						   index0 = k;
						   break;
						}
				}
				
				for (int k = 0; k < ControlVar.size(); k++)
				{
					if(ControlVar.contains((children.get(1).toString())))
						{
						   index = k;
						   break;
						}
				}
				
				
				for (int k = 0; k < UncontrolVar.size(); k++)
				{
					if(UncontrolVar.contains((children.get(0).toString())))
						{
						   index0 = k;
						   break;
						}
				}
				
				for (int k = 0; k < UncontrolVar.size(); k++)
				{
					if(UncontrolVar.contains((children.get(1).toString())))
						{
						   index = k;
						   break;
						}
				}
				cons[j][index]= 1;
				cons[j][index]= -1;
			//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			}
			*/
			else if(left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op)=="-")
				{
					for (int k = 0; k < ControlVar.size(); k++)
					{
						if(ControlVar.get(k).equals(rightExp))
							{
							//System.out.println("hiii");
								cons[j][k]= -1;
								break;
							}
					
					}
				
					for (int k = 0; k < UncontrolVar.size(); k++)
					{
						if(UncontrolVar.get(k).equals(rightExp))
						{
							//System.out.println("hiii");
							cons[j][k]= -1;
							break;
						}
					}
					
					rv += printExspeed(leftExp,j);
				//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
				}
			
			
			else
				if(left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op)=="+")
			{
					for (int k = 0; k < ControlVar.size(); k++)
					{
						if(ControlVar.get(k).equals(rightExp))
							{
								//System.out.println("hiii");
								cons[j][k]= 1;
								break;
							}
					
					}
				
					for (int k = 0; k < UncontrolVar.size(); k++)
					{
						if(UncontrolVar.get(k).equals(rightExp))
						{
							//System.out.println("hiii");
							cons[j][k]= 1;
							break;
						}
					}
					
					
					rv += printExspeed(leftExp,j);
			//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			}
				
				else
				{
					
					int myP = Operator.getPriority(op);
					if (left != null && left.children.size() > 1)
					{
						int leftP = Operator.getPriority(left.op);
						
						if (leftP < myP)
							needParenLeft = true;
					//	System.out.println(needParenLeft);
					}
					
					if (right != null && right.children.size() > 1)
					{
						int rightP = Operator.getPriority(right.op);
						
						if (myP > rightP || (myP == rightP && !Operator.isCommutative(op))) // commutative
							needParenRight = true;
						//System.out.println(needParenRight);
					}
					
					
					if (needParenLeft)
					{
						//System.out.println("Unexpected");
					   rv += "(" + printExspeed(leftExp,j) + ")"; 
					// maybe not strictly necessary as the expression.toString is overriden to call this print, but was having problems with this
					}
					else
						{
						rv += printExspeed(leftExp,j);
						//System.out.println("the left expression is:"+ print(leftExp));
						}
					
					rv += " " + opNames.get(o.op) + " ";
				//	System.out.println(opNames.get(o.op));
					if (needParenRight)
					{
						//System.out.println("Unexpected");
						rv += "(" + printExspeed(rightExp,j) + ")";
					}
					else
					{
						rv += printExspeed(rightExp,j);
						//System.out.println("the right expression is:"+ print(rightExp));
					}
					
				}
			
		}
		else
		{
			throw new AutomatonExportException("No default way to in-line print expression with " + children.size() 
					+ " children (" + opNames.get(o.op) + ".");
		}
		
		
		//System.out.println("The result is :"+ rv);
		return rv;
		
	}
	
	public double cons(int cons1, int cons2)
	{
		return cons[cons1][cons2];
			
	}
	
	public double Bmatrix(int cons1, int cons2)
	{
		return Bmatrix[cons1][cons2];
			
	}
	
	
	public double Cmatrix(int cons1)
	{
		return constraint[cons1];
			
	}
	
	public void saveAmatrix(String stx,int j)
	{
		
		for (int k = 0; k < ControlVar.size(); k++)
		{
			if(ControlVar.get(k).equals(stx.toString()))
				{
				   index = k;
				   cons[j][index]= 1;
				   //System.out.println("cons["+j+"]["+index+"]"+cons[j][index]);
				   break;
				}
		}
		
		  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
		
	}
	
	public void saveCmatrix(String stl,int j)
	{
		
				   constraint[j]= Integer.parseInt(stl);
		
		
		  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
		
	}
	
	public void saveBmatrix(String stl,int j)
	{
		for (int k = 0; k < UncontrolVar.size(); k++)
		{
			if(UncontrolVar.get(k).equals(stl.toString()))
				{
				   index = k;
				   Bmatrix[j][index]= 1;
				   break;
				}
		}
		
				   Bmatrix[j][index]= Integer.parseInt(stl);
		
		
		  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
		
	}
	
}
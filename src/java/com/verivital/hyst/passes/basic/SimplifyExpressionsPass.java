package com.verivital.hyst.passes.basic;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionModifier;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;


/**
 * This pass splits guards with disjunctions into multiple transitions.
 * 
 * For example, A -- (x == 1 || x == 2) --> B would be split into two transitions
 * from A to B, one with (x==1) and one with (x == 2)
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class SimplifyExpressionsPass extends TransformationPass
{
	public SimplifyExpressionsPass()
	{
		preconditions = new Preconditions(true); // skip all checks
	}
	
	@Override
	protected void runPass(String params)
	{
		runRec(config.root);
	}

	private void runRec(Component c)
	{
		if (c instanceof BaseComponent)
		{
			ExpressionModifier.modifyBaseComponent((BaseComponent)c, new ExpressionModifier(){
				@Override
				public Expression modifyExpression(Expression e)
				{
					return simplifyExpression(e);
				}});
		}
		else
		{
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
				runRec(ci.child);
		}
	}
	
	/**
	 * Simplify a single expression and return it
	 * Boolean expressions are simplified to Constant.FALSE or Constant.TRUE
	 */
	public static Expression simplifyExpression(Expression e)
	{
		Expression rv = simplifyExpressionRec(e);
		
		return rv;
	}
	
	private static Expression simplifyExpressionRec(Expression e)
	{
		Expression rv = e;
		
		if (e instanceof Operation)
		{
			Operation o = (Operation)e;
			Operator op = o.op;
			
			for (int i = 0; i < o.children.size(); ++i)
			{
				Expression child = o.children.get(i);
				
				o.children.set(i, simplifyExpressionRec(child));
			}

			if (op == Operator.AND && o.getLeft() instanceof Constant)
			{
				// short-circuit AND (left)
				if (o.getLeft().equals(Constant.FALSE))
					rv = Constant.FALSE;
				else
					rv = o.getRight();
			}
			else if (op == Operator.AND && o.getRight() instanceof Constant)
			{
				// short-circuit AND (right)
				if (o.getRight().equals(Constant.FALSE))
					rv = Constant.FALSE;
				else
					rv = o.getLeft();
			}
			else if (op == Operator.OR && o.getLeft() instanceof Constant)
			{
				// short-circuit OR (left)
				if (o.getLeft().equals(Constant.FALSE))
					rv = o.getRight();
				else
					rv = Constant.TRUE;
			}
			else if (op == Operator.OR && o.getRight() instanceof Constant)
			{
				// short-circuit OR (right)
				if (o.getRight().equals(Constant.FALSE))
					rv = o.getLeft();
				else
					rv = Constant.TRUE;
			}
			else if (o.children.size() == 1 && o.children.get(0) instanceof Constant)
			{
				double val = ((Constant)o.children.get(0)).getVal();
				
				switch (op) 
				{
					case SUBTRACT:
						rv = new Constant(-val);
						break;
					case COS:
						rv = new Constant(Math.cos(val));
						break;
					case EXP:
						rv = new Constant(Math.exp(val));
						break;
					case SIN:
						rv = new Constant(Math.sin(val));
						break;
					case SQRT:
						rv = new Constant(Math.sqrt(val));
						break;
					case TAN:
						rv = new Constant(Math.tan(val));
						break;
					case LN:
						rv = new Constant(Math.log(val));
						break;
					case NEGATIVE:
						rv = new Constant(-val);
						break;
					default:
						// should never come up
						throw new AutomatonExportException("Unsupported unary operation: " + op);
				}
			}
			else if (o.children.size() == 2 && o.getLeft() instanceof Constant && o.getRight() instanceof Constant)
			{
				// simplify constant comparisons / math
				
				double left = ((Constant)o.getLeft()).getVal();
				double right = ((Constant)o.getRight()).getVal();
				
				switch (op) 
				{
					case MULTIPLY :
						rv = new Constant(left * right);
						break;
					case DIVIDE :
						rv = new Constant(left / right);
						break;
					case ADD :
						rv = new Constant(left + right);
						break;
					case SUBTRACT :
						rv = new Constant(left - right);
						break;
					case POW:
						rv = new Constant(Math.pow(left, right));
						break;
					case EQUAL :
						rv = left == right ? Constant.TRUE : Constant.FALSE;
						break;
					case LESS :
						rv = left < right ? Constant.TRUE : Constant.FALSE;
						break;
					case GREATER :
						rv = left > right ? Constant.TRUE : Constant.FALSE;
						break;
					case LESSEQUAL :
						rv = left <= right ? Constant.TRUE : Constant.FALSE;
						break;
					case GREATEREQUAL :
						rv = left >= right ? Constant.TRUE : Constant.FALSE;
						break;
					case NOTEQUAL :
						rv = left != right ? Constant.TRUE : Constant.FALSE;
						break;
					default:
						throw new AutomatonExportException("Unsupported binary operation: " + op);
				}
			}
			// shortcut math operations
			else if (op == Operator.ADD && o.getRight() instanceof Constant && ((Constant)o.getRight()).getVal() == 0)
				rv = o.getLeft();
			else if (op == Operator.ADD && o.getLeft() instanceof Constant && ((Constant)o.getLeft()).getVal() == 0)
				rv = o.getRight();
			else if (op == Operator.SUBTRACT && o.getRight() instanceof Constant && ((Constant)o.getRight()).getVal() == 0)
				rv = o.getLeft();
			else if (op == Operator.MULTIPLY && o.getRight() instanceof Constant && ((Constant)o.getRight()).getVal() == 0)
				rv = new Constant(0);
			else if (op == Operator.MULTIPLY && o.getLeft() instanceof Constant && ((Constant)o.getLeft()).getVal() == 0)
				rv = new Constant(0);
			else if (op == Operator.POW && o.getRight() instanceof Constant && ((Constant)o.getRight()).getVal() == 0)
				rv = new Constant(1); // anything^0 = 1
			else if (op == Operator.POW && o.getRight() instanceof Constant && ((Constant)o.getRight()).getVal() == 1)
				rv = o.getLeft();
		}
		
		return rv;
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pass_simplify";
	}
	
	@Override
	public String getName()
	{
		return "Simplify Expressions Pass";
	}
}

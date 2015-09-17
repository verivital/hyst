package com.verivital.hyst.grammar.formula;


import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.NotNull;

import com.verivital.hyst.grammar.antlr.HystExpressionBaseVisitor;
import com.verivital.hyst.grammar.antlr.HystExpressionParser;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.AddSubContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.AndContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.LocSubExpressionContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.MatrixExpressionContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.NegativeUnaryContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.NotContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.OpContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.OrContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.PowContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.ResetSubExpressionContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.TimesDivContext;


public class ExpVisitor extends HystExpressionBaseVisitor <Expression>
{
	@Override public Expression visitResetSubOp(@NotNull HystExpressionParser.ResetSubOpContext ctx) 
	{
		Expression rv = null;
		Expression last = null;
		
		int opIndex = 0;
		for (AddSubContext a : ctx.addSub())
		{
			Expression e = visit(a);
			
			if (last != null)
			{
				Operation o = visit(ctx.op(opIndex++)).asOperation();
				
				o.children.add(last);
				o.children.add(e);
				
				if (rv == null)
					rv = o;
				else
					rv = new Operation(Operator.AND, rv, o);
			}
			
			last = e.copy();
		}
		
		return rv;
	}
	
	@Override public Expression visitResetSubEq(@NotNull HystExpressionParser.ResetSubEqContext ctx) 
	{
		Variable v = new Variable(ctx.VAR().getText());
		
		Expression rhs = visit(ctx.addSub());
		
		return new Operation(Operator.EQUAL, v, rhs);
	}
	
	@Override public Expression visitReset(@NotNull HystExpressionParser.ResetContext ctx) 
	{
		Expression rv = null;
		
		for (ResetSubExpressionContext r :  ctx.resetSubExpression())
		{
			Expression e = visit(r);
			
			if (rv == null)
				rv = e;
			else
				rv = new Operation(Operator.AND, rv, e);
		}
		
		return rv;
	}
	
	@Override public Expression visitGuard(@NotNull HystExpressionParser.GuardContext ctx) 
	{
		OrContext child = ctx.or();
		
		return visit(child);
	}
	
	@Override public Expression visitInvariant(@NotNull HystExpressionParser.InvariantContext ctx) 
	{
		OrContext child = ctx.or();
		
		return visit(child);
	}
	
	@Override public Expression visitFlow(@NotNull HystExpressionParser.FlowContext ctx) 
	{
		Expression rv = null;
		
		for (int i = 0; i < ctx.VAR().size(); ++i)
		{
			Variable v = new Variable(ctx.VAR(i).getText());
			Expression rhs = visit(ctx.addSub(i));
			Expression term = new Operation(Operator.EQUAL, v, rhs);
			
			if (rv == null)
				rv = term;
			else
				rv = new Operation(Operator.AND, rv, term);
		}
		
		return rv;
	}
	
	@Override public Expression visitLut(@NotNull HystExpressionParser.LutContext ctx)
	{
		String[] varList = ctx.varListExpression().getText().split(",");
		double[][] values = parseMatrix(ctx.matrixExpression(0).getText());
		double[][] breakpoints = parseMatrix(ctx.matrixExpression(1).getText());
		
		return new Lut(varList, values, breakpoints);
	}
	
	private double[][] parseMatrix(String s)
	{
		String[] rows = s.split(";");
		
		
		return null;
	}

	@Override public Expression visitFlowFalse(@NotNull HystExpressionParser.FlowFalseContext ctx)
	{
		return Constant.FALSE;
	}
	
	@Override public Expression visitLocSubExp(@NotNull HystExpressionParser.LocSubExpContext ctx)
	{
		return new Operation(Operator.EQUAL, new Operation(Operator.LOC, 
				new Variable(ctx.dottedVar().getText())), new Variable(ctx.VAR().getText()));
	}
	
	@Override public Expression visitLocSubBlankExp(@NotNull HystExpressionParser.LocSubBlankExpContext ctx)
	{
		return new Operation(Operator.EQUAL, new Operation(Operator.LOC, 
				new Variable("")), new Variable(ctx.VAR().getText()));
	}
	
	@Override public Expression visitLocAndExp(@NotNull HystExpressionParser.LocAndExpContext ctx)
	{
		return visit(ctx.and());
	}
	
	@Override public Expression visitLocExp(@NotNull HystExpressionParser.LocExpContext ctx)
	{
		Expression rv = null;
		
		for (LocSubExpressionContext l : ctx.locSubExpression())
		{
			Expression e = visit(l);
			
			if (rv == null)
				rv = e;
			else
				rv = new Operation(Operator.AND, rv, e);
		}
		
		return rv;
	}
	
	@Override public Expression visitLocFalse(@NotNull HystExpressionParser.LocFalseContext ctx)
	{
		return null;
	}

	@Override public Expression visitOrExpression(@NotNull HystExpressionParser.OrExpressionContext ctx) 
	{
		AndContext left = ctx.and();
		OrContext right = ctx.or();
		
		return new Operation(Operator.OR, visit(left), visit(right));
	}
	
	@Override public Expression visitAndExpression(@NotNull HystExpressionParser.AndExpressionContext ctx) 
	{
		NotContext left = ctx.not();
		AndContext right = ctx.and();
		
		return new Operation(Operator.AND, visit(left), visit(right));
	}
	
	@Override public Expression visitNotExpression(@NotNull HystExpressionParser.NotExpressionContext ctx) 
	{
		OrContext child = ctx.or();
		
		return new Operation(Operator.NEGATIVE, visit(child));
	}
	
	@Override public Expression visitBoolParentheses(@NotNull HystExpressionParser.BoolParenthesesContext ctx) 
	{
		OrContext child = ctx.or();
		
		return visit(child);
	}
	
	@Override public Expression visitEqualOp(@NotNull HystExpressionParser.EqualOpContext ctx)
	{
		return new Operation(Operator.EQUAL);
	}
	
	@Override public Expression visitLessOp(@NotNull HystExpressionParser.LessOpContext ctx)
	{
		return new Operation(Operator.LESS);
	}
	
	@Override public Expression visitLessEqualOp(@NotNull HystExpressionParser.LessEqualOpContext ctx)
	{
		return new Operation(Operator.LESSEQUAL);
	}
	
	@Override public Expression visitGreaterOp(@NotNull HystExpressionParser.GreaterOpContext ctx)
	{
		return new Operation(Operator.GREATER);
	}
	
	@Override public Expression visitGreaterEqualOp(@NotNull HystExpressionParser.GreaterEqualOpContext ctx)
	{
		return new Operation(Operator.GREATEREQUAL);
	}
	
	@Override public Expression visitNotEqualOp(@NotNull HystExpressionParser.NotEqualOpContext ctx)
	{
		return new Operation(Operator.NOTEQUAL);
	}
	
	@Override public Expression visitBoolOp(@NotNull HystExpressionParser.BoolOpContext ctx) 
	{
		Expression rv = null;
		List <Expression> childExp = new ArrayList <Expression>();
		
		for (AddSubContext child : ctx.addSub())
			childExp.add(visit(child));
		
		List <OpContext> ops = ctx.op();
		
		// for every op, generate one expression
		// a < b == c < d generates a < b & b == c & c < d
		for (int leftIndex = 0; leftIndex < childExp.size() - 1; ++leftIndex)
		{
			int rightIndex = leftIndex + 1;
			OpContext opCtx = ops.get(leftIndex);
			Expression left = childExp.get(leftIndex);
			Expression right = childExp.get(rightIndex);
			
			Operation o = (Operation)visit(opCtx);
			
			o.children.add(left);
			o.children.add(right.copy()); // always copy right
			
			if (rv == null)
				rv = o;
			else
				rv = new Operation(Operator.AND, rv, o);
		}
		
		return rv;
	}
	
	@Override public Expression visitConstTrue(@NotNull HystExpressionParser.ConstTrueContext ctx) 
	{
		return Constant.TRUE;
	}
	
	@Override public Expression visitConstFalse(@NotNull HystExpressionParser.ConstFalseContext ctx) 
	{
		return Constant.FALSE;
	}
	
	@Override public Expression visitPlus(@NotNull HystExpressionParser.PlusContext ctx) 
	{
		AddSubContext left = ctx.addSub();
		TimesDivContext right = ctx.timesDiv();
		
		return new Operation(Operator.ADD, visit(left), visit(right));
	}
	
	@Override public Expression visitMinus(@NotNull HystExpressionParser.MinusContext ctx) 
	{
		AddSubContext left = ctx.addSub();
		TimesDivContext right = ctx.timesDiv();
		
		return new Operation(Operator.SUBTRACT, visit(left), visit(right));
	}
	
	@Override public Expression visitMultiplication(@NotNull HystExpressionParser.MultiplicationContext ctx) 
	{
		TimesDivContext left = ctx.timesDiv();
		PowContext right = ctx.pow();
		
		Expression e = visit(right);
		
		return new Operation(Operator.MULTIPLY, visit(left), e);
	}
	
	@Override public Expression visitDivision(@NotNull HystExpressionParser.DivisionContext ctx) 
	{
		TimesDivContext left = ctx.timesDiv();
		PowContext right = ctx.pow();
		
		return new Operation(Operator.DIVIDE, visit(left), visit(right));
	}
	
	@Override public Expression visitPowExpression(@NotNull HystExpressionParser.PowExpressionContext ctx) 
	{
		PowContext left = ctx.pow();
		NegativeUnaryContext right = ctx.negativeUnary();
		
		return new Operation(Operator.POW, visit(left), visit(right));
	}
	
	@Override public Expression visitNegative(@NotNull HystExpressionParser.NegativeContext ctx) 
	{
		NegativeUnaryContext child = ctx.negativeUnary();		
		
		Expression rv = new Operation(Operator.NEGATIVE, visit(child));
		
		// eliminate double negatives
		while (true)
		{
			if (rv.asOperation() == null || rv.asOperation().children.size() != 1 || rv.asOperation().op != Operator.NEGATIVE)
				break;
			
			Expression childExp = rv.asOperation().children.get(0);
			Operation childOp = childExp.asOperation();
			
			if (childOp != null && childOp.children.size() == 1 && childOp.op == Operator.NEGATIVE)
				rv = childOp.children.get(0);
			else
				break;
		}
		
		// negative constants
		if (rv.asOperation() != null & rv.asOperation().children.size() == 1 && rv.asOperation().op == Operator.NEGATIVE)
		{
			if (rv.asOperation().children.get(0) instanceof Constant)
			{
				Constant c = (Constant)rv.asOperation().children.get(0);
				c.setVal(c.getVal() * -1);
				
				rv = c;
			} 
		}
		
		return rv;
	}
	
	@Override public Expression visitTanFunc(@NotNull HystExpressionParser.TanFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.TAN, visit(child));
	}
	
	@Override public Expression visitSqrtFunc(@NotNull HystExpressionParser.SqrtFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.SQRT, visit(child));
	}
	
	@Override public Expression visitSinFunc(@NotNull HystExpressionParser.SinFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.SIN, visit(child));
	}
	
	@Override public Expression visitCosFunc(@NotNull HystExpressionParser.CosFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.COS, visit(child));
	}
	
	@Override public Expression visitExpFunc(@NotNull HystExpressionParser.ExpFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.EXP, visit(child));
	}
	
	@Override public Expression visitLnFunc(@NotNull HystExpressionParser.LnFuncContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return new Operation(Operator.LN, visit(child));
	}
	
	@Override public Expression visitNumber(@NotNull HystExpressionParser.NumberContext ctx) 
	{
		return new Constant(Double.parseDouble(ctx.NUM().getText()));
	}
	
	@Override public Expression visitDottedVariable(@NotNull HystExpressionParser.DottedVariableContext ctx) 
	{
		String text = ctx.dottedVar().getText();
		
		if (text.endsWith("'"))
			text = text.substring(0, text.length() - 1);
		
		return new Variable(text);
	}
	
	@Override public Expression visitParentheses(@NotNull HystExpressionParser.ParenthesesContext ctx) 
	{
		AddSubContext child = ctx.addSub();
		
		return visit(child);
	}
	
	private class VarList extends Expression
	{
		String[] variables;
		
		public VarList(String[] vars)
		{
			this.variables = vars;
		}

		@Override
		public Expression copy() { return null; }
	}
}

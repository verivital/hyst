package com.verivital.hyst.grammar.formula;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.NotNull;

import com.verivital.hyst.grammar.antlr.HystExpressionBaseVisitor;
import com.verivital.hyst.grammar.antlr.HystExpressionParser;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.AddSubContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.AndContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.FunctionContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.NegativeUnaryContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.NotContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.OpContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.OrContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.PowContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.ResetSubExpressionContext;
import com.verivital.hyst.grammar.antlr.HystExpressionParser.TimesDivContext;
import com.verivital.hyst.ir.AutomatonExportException;

public class ExpVisitor extends HystExpressionBaseVisitor<Expression>
{
	@Override
	public Expression visitResetSubOp(@NotNull HystExpressionParser.ResetSubOpContext ctx)
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

	@Override
	public Expression visitResetSubEq(@NotNull HystExpressionParser.ResetSubEqContext ctx)
	{
		Variable v = new Variable(ctx.VAR().getText());

		Expression rhs = visit(ctx.addSub());

		return new Operation(Operator.EQUAL, v, rhs);
	}

	@Override
	public Expression visitReset(@NotNull HystExpressionParser.ResetContext ctx)
	{
		Expression rv = null;

		for (ResetSubExpressionContext r : ctx.resetSubExpression())
		{
			Expression e = visit(r);

			if (rv == null)
				rv = e;
			else
				rv = new Operation(Operator.AND, rv, e);
		}

		return rv;
	}

	@Override
	public Expression visitGuard(@NotNull HystExpressionParser.GuardContext ctx)
	{
		OrContext child = ctx.or();

		return visit(child);
	}

	@Override
	public Expression visitInvariant(@NotNull HystExpressionParser.InvariantContext ctx)
	{
		OrContext child = ctx.or();

		return visit(child);
	}

	private Expression balancedFlow(List<Expression> children)
	{
		Expression rv;

		if (children.size() == 1)
			rv = children.get(0);
		else
		{
			int middleIndex = children.size() / 2;

			rv = new Operation(Operator.AND, balancedFlow(children.subList(0, middleIndex)),
					balancedFlow(children.subList(middleIndex, children.size())));
		}

		return rv;
	}

	@Override
	public Expression visitFlow(@NotNull HystExpressionParser.FlowContext ctx)
	{
		Expression rv = null;
		List<Expression> terms = new ArrayList<Expression>();

		for (int i = 0; i < ctx.VAR().size(); ++i)
		{
			Variable v = new Variable(ctx.VAR(i).getText());
			Expression rhs = visit(ctx.addSub(i));
			Expression term = new Operation(Operator.EQUAL, v, rhs);

			terms.add(term);
		}

		Expression flow = balancedFlow(terms);

		return flow;
	}

	@Override
	public Expression visitFlowFalse(@NotNull HystExpressionParser.FlowFalseContext ctx)
	{
		return Constant.FALSE;
	}

	@Override
	public Expression visitLocExp(@NotNull HystExpressionParser.LocExpContext ctx)
	{
		Expression rv = visit(ctx.or());

		return rv;
	}

	@Override
	public Expression visitLocFalse(@NotNull HystExpressionParser.LocFalseContext ctx)
	{
		return null;
	}

	@Override
	public Expression visitOrExpression(@NotNull HystExpressionParser.OrExpressionContext ctx)
	{
		AndContext left = ctx.and();
		OrContext right = ctx.or();

		return new Operation(Operator.OR, visit(left), visit(right));
	}

	private Expression balancedAnd(List<NotContext> children)
	{
		Expression rv;

		if (children.size() == 1)
			rv = visit(children.get(0));
		else
		{
			int middleIndex = children.size() / 2;

			rv = new Operation(Operator.AND, balancedAnd(children.subList(0, middleIndex)),
					balancedAnd(children.subList(middleIndex, children.size())));
		}

		return rv;
	}

	@Override
	public Expression visitAndExpression(@NotNull HystExpressionParser.AndExpressionContext ctx)
	{
		// (not AND)* not # AndExpression

		List<NotContext> children = ctx.not();

		// construct a balanced tree of expressions
		Expression root = balancedAnd(children);

		return root;
	}

	@Override
	public Expression visitNotExpression(@NotNull HystExpressionParser.NotExpressionContext ctx)
	{
		OrContext child = ctx.or();

		return new Operation(Operator.NEGATIVE, visit(child));
	}

	@Override
	public Expression visitBoolParentheses(@NotNull HystExpressionParser.BoolParenthesesContext ctx)
	{
		OrContext child = ctx.or();

		return visit(child);
	}

	@Override
	public Expression visitEqualOp(@NotNull HystExpressionParser.EqualOpContext ctx)
	{
		return new Operation(Operator.EQUAL);
	}

	@Override
	public Expression visitLessOp(@NotNull HystExpressionParser.LessOpContext ctx)
	{
		return new Operation(Operator.LESS);
	}

	@Override
	public Expression visitLessEqualOp(@NotNull HystExpressionParser.LessEqualOpContext ctx)
	{
		return new Operation(Operator.LESSEQUAL);
	}

	@Override
	public Expression visitGreaterOp(@NotNull HystExpressionParser.GreaterOpContext ctx)
	{
		return new Operation(Operator.GREATER);
	}

	@Override
	public Expression visitGreaterEqualOp(@NotNull HystExpressionParser.GreaterEqualOpContext ctx)
	{
		return new Operation(Operator.GREATEREQUAL);
	}

	@Override
	public Expression visitNotEqualOp(@NotNull HystExpressionParser.NotEqualOpContext ctx)
	{
		return new Operation(Operator.NOTEQUAL);
	}

	@Override
	public Expression visitBoolOp(@NotNull HystExpressionParser.BoolOpContext ctx)
	{
		Expression rv = null;
		List<Expression> childExp = new ArrayList<Expression>();

		for (AddSubContext child : ctx.addSub())
			childExp.add(visit(child));

		List<OpContext> ops = ctx.op();

		// for every op, generate one expression
		// a < b == c < d generates a < b & b == c & c < d
		for (int leftIndex = 0; leftIndex < childExp.size() - 1; ++leftIndex)
		{
			int rightIndex = leftIndex + 1;
			OpContext opCtx = ops.get(leftIndex);
			Expression left = childExp.get(leftIndex);
			Expression right = childExp.get(rightIndex);

			Operation o = (Operation) visit(opCtx);

			o.children.add(left);
			o.children.add(right.copy()); // always copy right

			if (rv == null)
				rv = o;
			else
				rv = new Operation(Operator.AND, rv, o);
		}

		return rv;
	}

	@Override
	public Expression visitConstTrue(@NotNull HystExpressionParser.ConstTrueContext ctx)
	{
		return Constant.TRUE;
	}

	@Override
	public Expression visitConstFalse(@NotNull HystExpressionParser.ConstFalseContext ctx)
	{
		return Constant.FALSE;
	}

	@Override
	public Expression visitPlus(@NotNull HystExpressionParser.PlusContext ctx)
	{
		AddSubContext left = ctx.addSub();
		TimesDivContext right = ctx.timesDiv();

		return new Operation(Operator.ADD, visit(left), visit(right));
	}

	@Override
	public Expression visitMinus(@NotNull HystExpressionParser.MinusContext ctx)
	{
		AddSubContext left = ctx.addSub();
		TimesDivContext right = ctx.timesDiv();

		return new Operation(Operator.SUBTRACT, visit(left), visit(right));
	}

	@Override
	public Expression visitMultiplication(@NotNull HystExpressionParser.MultiplicationContext ctx)
	{
		TimesDivContext left = ctx.timesDiv();
		PowContext right = ctx.pow();

		Expression e = visit(right);

		return new Operation(Operator.MULTIPLY, visit(left), e);
	}

	@Override
	public Expression visitDivision(@NotNull HystExpressionParser.DivisionContext ctx)
	{
		TimesDivContext left = ctx.timesDiv();
		PowContext right = ctx.pow();

		return new Operation(Operator.DIVIDE, visit(left), visit(right));
	}

	@Override
	public Expression visitPowExpression(@NotNull HystExpressionParser.PowExpressionContext ctx)
	{
		PowContext left = ctx.pow();
		NegativeUnaryContext right = ctx.negativeUnary();

		return new Operation(Operator.POW, visit(left), visit(right));
	}

	@Override
	public Expression visitNegative(@NotNull HystExpressionParser.NegativeContext ctx)
	{
		NegativeUnaryContext child = ctx.negativeUnary();

		Expression rv = new Operation(Operator.NEGATIVE, visit(child));

		// eliminate double negatives
		while (true)
		{
			if (rv.asOperation() == null || rv.asOperation().children.size() != 1
					|| rv.asOperation().op != Operator.NEGATIVE)
				break;

			Expression childExp = rv.asOperation().children.get(0);
			Operation childOp = childExp.asOperation();

			if (childOp != null && childOp.children.size() == 1 && childOp.op == Operator.NEGATIVE)
				rv = childOp.children.get(0);
			else
				break;
		}

		// negative constants
		if (rv.asOperation() != null && rv.asOperation().children.size() == 1
				&& rv.asOperation().op == Operator.NEGATIVE)
		{
			if (rv.asOperation().children.get(0) instanceof Constant)
			{
				Constant c = (Constant) rv.asOperation().children.get(0);
				c.setVal(c.getVal() * -1);

				rv = c;
			}
		}

		return rv;
	}

	private class NameOperator
	{
		public NameOperator(String name, Operator op)
		{
			this.name = name;
			this.op = op;
		}

		public String name;
		public Operator op;
	}

	@Override
	public Expression visitFunction(@NotNull HystExpressionParser.FunctionContext ctx)
	{
		String name = ctx.VAR().getText().toLowerCase();
		List<AddSubContext> args = ctx.addSub();
		Expression rv = null;

		NameOperator[] singleParamFuncs = { new NameOperator("tan", Operator.TAN),
				new NameOperator("sqrt", Operator.SQRT), new NameOperator("sin", Operator.SIN),
				new NameOperator("cos", Operator.COS), new NameOperator("exp", Operator.EXP),
				new NameOperator("ln", Operator.LN) };

		for (NameOperator no : singleParamFuncs)
		{
			if (name.equals(no.name))
			{
				if (args.size() != 1)
					throw new AutomatonExportException(
							"Function '" + no.name + "' expects single argument.");

				rv = new Operation(no.op, visit(args.get(0)));
			}
		}

		// special case: loc can take 0 or 1 arguments
		if (name.equals("loc"))
		{
			if (args.size() == 0)
				rv = new Operation(Operator.LOC);
			else if (args.size() == 1)
				rv = new Operation(Operator.LOC, visit(args.get(0)));
			else
				throw new AutomatonExportException("Function 'loc' expects 0 or 1 arguments.");
		}

		// special case: lookup table
		if (name.equals("lut"))
			rv = processLut(ctx);

		if (name.equals("reshape"))
			rv = processReshape(ctx);

		// unsupported
		if (rv == null)
			throw new AutomatonExportException("Unknown function '" + ctx.VAR().getText() + "'");

		return rv;
	}

	private Expression processReshape(FunctionContext ctx)
	{
		List<AddSubContext> args = ctx.addSub();

		if (args.size() < 2)
			throw new AutomatonExportException(
					"Function 'reshape' expects at least 2 arguments: array, [width]+");

		MatrixExpression data = (MatrixExpression) (visit(args.get(0)));

		if (data.getNumDims() != 1)
			throw new AutomatonExportException(
					"Function 'reshape' expects fist argument to be a 1-d matrix. Instead got "
							+ data.getNumDims() + "-d data: " + data.toDefaultString());

		Expression[] expressions = new Expression[data.getDimWidth(0)];

		for (int i = 0; i < data.getDimWidth(0); ++i)
			expressions[i] = data.get(i);

		int[] vals = new int[args.size() - 1];

		for (int a = 1; a < args.size(); ++a)
		{
			Expression e = visit(args.get(a));

			if (!(e instanceof Constant))
				throw new AutomatonExportException(
						"width arguments in function 'reshape' must be integer constants");

			vals[a - 1] = (int) Math.round(Double.parseDouble(e.toDefaultString()));
		}

		return new MatrixExpression(expressions, vals);
	}

	/**
	 * Create a lookup table expression from a function context. Luts expect three arguments: 1. var
	 * list, 2. table, and 3. breakpoints
	 * 
	 * @param ctx
	 *            the function context
	 * @return the constructed expression
	 */
	private Expression processLut(FunctionContext ctx)
	{
		List<AddSubContext> args = ctx.addSub();

		if (args.size() < 3)
			throw new AutomatonExportException(
					"Function 'lut' expects at least 3 arguments: varlist, table, [breakpoints]+");

		MatrixExpression vars = (MatrixExpression) (visit(args.get(0)));

		if (vars.getNumDims() != 1)
			throw new AutomatonExportException(
					"Function 'lut' expects fist argument to be a 1-d list of variables");

		Expression[] varList = new Expression[vars.getDimWidth(0)];

		for (int v = 0; v < vars.getDimWidth(0); ++v)
			varList[v] = vars.get(v);

		MatrixExpression data = (MatrixExpression) visit(args.get(1));
		MatrixExpression[] breakPoints = new MatrixExpression[args.size() - 2];

		for (int a = 2; a < args.size(); ++a)
		{
			MatrixExpression bp = (MatrixExpression) visit(args.get(a));
			breakPoints[a - 2] = bp;
		}

		return new LutExpression(varList, data, breakPoints);
	}

	@Override
	public Expression visitMatrixRangeExp(@NotNull HystExpressionParser.MatrixRangeExpContext ctx)
	{
		return MatrixExpression.fromRange(visit(ctx.addSub(0)), visit(ctx.addSub(1)),
				visit(ctx.addSub(2)));
	}

	@Override
	public Expression visitMatrixGenerated(@NotNull HystExpressionParser.MatrixGeneratedContext ctx)
	{
		return visit(ctx.matrixRange());
	}

	@Override
	public Expression visitMatrixExplicit(@NotNull HystExpressionParser.MatrixExplicitContext ctx)
	{
		int len = ctx.matrixRow().size();

		Expression[][] rows = new Expression[len][];

		for (int i = 0; i < len; ++i)
		{
			MatrixExpression row = (MatrixExpression) visit(ctx.matrixRow(i));

			rows[i] = new Expression[row.getDimWidth(0)];

			for (int j = 0; j < row.getDimWidth(0); ++j)
				rows[i][j] = row.get(j);
		}

		return new MatrixExpression(rows);
	}

	@Override
	public Expression visitMatrixRowExp(@NotNull HystExpressionParser.MatrixRowExpContext ctx)
	{
		int len = ctx.addSub().size();
		Expression[] expressions = new Expression[len];

		for (int i = 0; i < len; ++i)
			expressions[i] = visit(ctx.addSub(i));

		return new MatrixExpression(expressions);
	}

	@Override
	public Expression visitNumber(@NotNull HystExpressionParser.NumberContext ctx)
	{
		return new Constant(Double.parseDouble(ctx.NUM().getText()));
	}

	@Override
	public Expression visitDottedVariable(@NotNull HystExpressionParser.DottedVariableContext ctx)
	{
		String text = ctx.dottedVar().getText();

		if (text.endsWith("'"))
			text = text.substring(0, text.length() - 1);

		return new Variable(text);
	}

	@Override
	public Expression visitParentheses(@NotNull HystExpressionParser.ParenthesesContext ctx)
	{
		AddSubContext child = ctx.addSub();

		return visit(child);
	}
}

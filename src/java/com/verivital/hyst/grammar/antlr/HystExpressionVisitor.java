// Generated from HystExpression.g4 by ANTLR 4.4
package com.verivital.hyst.grammar.antlr;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link HystExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface HystExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by the {@code Flow}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlow(@NotNull HystExpressionParser.FlowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrExpression}
	 * labeled alternative in {@link HystExpressionParser#or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpression(@NotNull HystExpressionParser.OrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Parentheses}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentheses(@NotNull HystExpressionParser.ParenthesesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocFalse}
	 * labeled alternative in {@link HystExpressionParser#locExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocFalse(@NotNull HystExpressionParser.LocFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToAnd}
	 * labeled alternative in {@link HystExpressionParser#or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToAnd(@NotNull HystExpressionParser.ToAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToTimesDiv}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToTimesDiv(@NotNull HystExpressionParser.ToTimesDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Matrix}
	 * labeled alternative in {@link HystExpressionParser#matrixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatrix(@NotNull HystExpressionParser.MatrixContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PowExpression}
	 * labeled alternative in {@link HystExpressionParser#pow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowExpression(@NotNull HystExpressionParser.PowExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetSubOp}
	 * labeled alternative in {@link HystExpressionParser#resetSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetSubOp(@NotNull HystExpressionParser.ResetSubOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GreaterEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterEqualOp(@NotNull HystExpressionParser.GreaterEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotEqualOp(@NotNull HystExpressionParser.NotEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocExp}
	 * labeled alternative in {@link HystExpressionParser#locExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocExp(@NotNull HystExpressionParser.LocExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToNot}
	 * labeled alternative in {@link HystExpressionParser#and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToNot(@NotNull HystExpressionParser.ToNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetSubEq}
	 * labeled alternative in {@link HystExpressionParser#resetSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetSubEq(@NotNull HystExpressionParser.ResetSubEqContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(@NotNull HystExpressionParser.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToPow}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToPow(@NotNull HystExpressionParser.ToPowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Guard}
	 * labeled alternative in {@link HystExpressionParser#guardExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuard(@NotNull HystExpressionParser.GuardContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DotVar}
	 * labeled alternative in {@link HystExpressionParser#dottedVar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotVar(@NotNull HystExpressionParser.DotVarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstTrue}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstTrue(@NotNull HystExpressionParser.ConstTrueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MatrixRowExp}
	 * labeled alternative in {@link HystExpressionParser#matrixRow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatrixRowExp(@NotNull HystExpressionParser.MatrixRowExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolParentheses}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolParentheses(@NotNull HystExpressionParser.BoolParenthesesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Multiplication}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplication(@NotNull HystExpressionParser.MultiplicationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Plus}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlus(@NotNull HystExpressionParser.PlusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Minus}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinus(@NotNull HystExpressionParser.MinusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DottedVariable}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDottedVariable(@NotNull HystExpressionParser.DottedVariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LessOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessOp(@NotNull HystExpressionParser.LessOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndExpression}
	 * labeled alternative in {@link HystExpressionParser#and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(@NotNull HystExpressionParser.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Number}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(@NotNull HystExpressionParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolOp}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolOp(@NotNull HystExpressionParser.BoolOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link HystExpressionParser#functionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(@NotNull HystExpressionParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FlowFalse}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowFalse(@NotNull HystExpressionParser.FlowFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToUnary}
	 * labeled alternative in {@link HystExpressionParser#negativeUnary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToUnary(@NotNull HystExpressionParser.ToUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FuncExp}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncExp(@NotNull HystExpressionParser.FuncExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstFalse}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstFalse(@NotNull HystExpressionParser.ConstFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LessEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessEqualOp(@NotNull HystExpressionParser.LessEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FlowBlank}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowBlank(@NotNull HystExpressionParser.FlowBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToNegativeUnary}
	 * labeled alternative in {@link HystExpressionParser#pow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToNegativeUnary(@NotNull HystExpressionParser.ToNegativeUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualOp(@NotNull HystExpressionParser.EqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MatrixExp}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatrixExp(@NotNull HystExpressionParser.MatrixExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Negative}
	 * labeled alternative in {@link HystExpressionParser#negativeUnary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegative(@NotNull HystExpressionParser.NegativeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GreaterOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterOp(@NotNull HystExpressionParser.GreaterOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetBlank}
	 * labeled alternative in {@link HystExpressionParser#resetExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetBlank(@NotNull HystExpressionParser.ResetBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToCompare}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToCompare(@NotNull HystExpressionParser.ToCompareContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Invariant}
	 * labeled alternative in {@link HystExpressionParser#invariantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariant(@NotNull HystExpressionParser.InvariantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Division}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivision(@NotNull HystExpressionParser.DivisionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GuardBlank}
	 * labeled alternative in {@link HystExpressionParser#guardExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuardBlank(@NotNull HystExpressionParser.GuardBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InvariantBlank}
	 * labeled alternative in {@link HystExpressionParser#invariantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariantBlank(@NotNull HystExpressionParser.InvariantBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Reset}
	 * labeled alternative in {@link HystExpressionParser#resetExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReset(@NotNull HystExpressionParser.ResetContext ctx);
}
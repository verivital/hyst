// Generated from java/com/verivital/hyst/grammar/antlr/HystExpression.g4 by ANTLR 4.5
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
	 * Visit a parse tree produced by the {@code VarList}
	 * labeled alternative in {@link HystExpressionParser#varListExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarList(HystExpressionParser.VarListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MatrixRow}
	 * labeled alternative in {@link HystExpressionParser#matrixRowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatrixRow(HystExpressionParser.MatrixRowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Matrix}
	 * labeled alternative in {@link HystExpressionParser#matrixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatrix(HystExpressionParser.MatrixContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Lut}
	 * labeled alternative in {@link HystExpressionParser#lutExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLut(HystExpressionParser.LutContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetSubEq}
	 * labeled alternative in {@link HystExpressionParser#resetSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetSubEq(HystExpressionParser.ResetSubEqContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetSubOp}
	 * labeled alternative in {@link HystExpressionParser#resetSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetSubOp(HystExpressionParser.ResetSubOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ResetBlank}
	 * labeled alternative in {@link HystExpressionParser#resetExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetBlank(HystExpressionParser.ResetBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Reset}
	 * labeled alternative in {@link HystExpressionParser#resetExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReset(HystExpressionParser.ResetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GuardBlank}
	 * labeled alternative in {@link HystExpressionParser#guardExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuardBlank(HystExpressionParser.GuardBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Guard}
	 * labeled alternative in {@link HystExpressionParser#guardExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuard(HystExpressionParser.GuardContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InvariantBlank}
	 * labeled alternative in {@link HystExpressionParser#invariantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariantBlank(HystExpressionParser.InvariantBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Invariant}
	 * labeled alternative in {@link HystExpressionParser#invariantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariant(HystExpressionParser.InvariantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FlowBlank}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowBlank(HystExpressionParser.FlowBlankContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Flow}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlow(HystExpressionParser.FlowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FlowFalse}
	 * labeled alternative in {@link HystExpressionParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowFalse(HystExpressionParser.FlowFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DotVar}
	 * labeled alternative in {@link HystExpressionParser#dottedVar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotVar(HystExpressionParser.DotVarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocSubExp}
	 * labeled alternative in {@link HystExpressionParser#locSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocSubExp(HystExpressionParser.LocSubExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocSubBlankExp}
	 * labeled alternative in {@link HystExpressionParser#locSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocSubBlankExp(HystExpressionParser.LocSubBlankExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocAndExp}
	 * labeled alternative in {@link HystExpressionParser#locSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocAndExp(HystExpressionParser.LocAndExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocExp}
	 * labeled alternative in {@link HystExpressionParser#locExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocExp(HystExpressionParser.LocExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LocFalse}
	 * labeled alternative in {@link HystExpressionParser#locExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocFalse(HystExpressionParser.LocFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrExpression}
	 * labeled alternative in {@link HystExpressionParser#or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpression(HystExpressionParser.OrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToAnd}
	 * labeled alternative in {@link HystExpressionParser#or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToAnd(HystExpressionParser.ToAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndExpression}
	 * labeled alternative in {@link HystExpressionParser#and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(HystExpressionParser.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToNot}
	 * labeled alternative in {@link HystExpressionParser#and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToNot(HystExpressionParser.ToNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(HystExpressionParser.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolParentheses}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolParentheses(HystExpressionParser.BoolParenthesesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToCompare}
	 * labeled alternative in {@link HystExpressionParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToCompare(HystExpressionParser.ToCompareContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualOp(HystExpressionParser.EqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LessOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessOp(HystExpressionParser.LessOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LessEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessEqualOp(HystExpressionParser.LessEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GreaterOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterOp(HystExpressionParser.GreaterOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GreaterEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterEqualOp(HystExpressionParser.GreaterEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotEqualOp}
	 * labeled alternative in {@link HystExpressionParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotEqualOp(HystExpressionParser.NotEqualOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolOp}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolOp(HystExpressionParser.BoolOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstTrue}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstTrue(HystExpressionParser.ConstTrueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstFalse}
	 * labeled alternative in {@link HystExpressionParser#compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstFalse(HystExpressionParser.ConstFalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Plus}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlus(HystExpressionParser.PlusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToTimesDiv}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToTimesDiv(HystExpressionParser.ToTimesDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Minus}
	 * labeled alternative in {@link HystExpressionParser#addSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinus(HystExpressionParser.MinusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Multiplication}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplication(HystExpressionParser.MultiplicationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Division}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivision(HystExpressionParser.DivisionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToPow}
	 * labeled alternative in {@link HystExpressionParser#timesDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToPow(HystExpressionParser.ToPowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToNegativeUnary}
	 * labeled alternative in {@link HystExpressionParser#pow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToNegativeUnary(HystExpressionParser.ToNegativeUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PowExpression}
	 * labeled alternative in {@link HystExpressionParser#pow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowExpression(HystExpressionParser.PowExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Negative}
	 * labeled alternative in {@link HystExpressionParser#negativeUnary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegative(HystExpressionParser.NegativeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ToUnary}
	 * labeled alternative in {@link HystExpressionParser#negativeUnary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToUnary(HystExpressionParser.ToUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LutFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLutFunc(HystExpressionParser.LutFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TanFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTanFunc(HystExpressionParser.TanFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SqrtFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqrtFunc(HystExpressionParser.SqrtFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SinFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSinFunc(HystExpressionParser.SinFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CosFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCosFunc(HystExpressionParser.CosFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExpFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpFunc(HystExpressionParser.ExpFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LnFunc}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLnFunc(HystExpressionParser.LnFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Number}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(HystExpressionParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DottedVariable}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDottedVariable(HystExpressionParser.DottedVariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Parentheses}
	 * labeled alternative in {@link HystExpressionParser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentheses(HystExpressionParser.ParenthesesContext ctx);
}
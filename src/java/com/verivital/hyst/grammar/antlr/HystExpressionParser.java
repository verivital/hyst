// Generated from HystExpression.g4 by ANTLR 4.4
package com.verivital.hyst.grammar.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HystExpressionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, TICK=2, TRUE=3, FALSE=4, NUM=5, VAR=6, LPAR=7, RPAR=8, LBRAC=9, 
		RBRAC=10, COMMA=11, SEMICOLON=12, PLUS=13, MINUS=14, TIMES=15, DIV=16, 
		POW=17, DOT=18, AND=19, OR=20, NOT=21, LESS=22, GREATER=23, LESSEQUAL=24, 
		GREATEREQUAL=25, NOTEQUAL=26, EQUAL=27, EQUAL_RESET=28;
	public static final String[] tokenNames = {
		"<INVALID>", "WS", "'''", "'true'", "'false'", "NUM", "VAR", "'('", "')'", 
		"'['", "']'", "','", "';'", "'+'", "'-'", "'*'", "'/'", "'^'", "'.'", 
		"AND", "OR", "'!'", "'<'", "'>'", "'<='", "'>='", "'!='", "EQUAL", "':='"
	};
	public static final int
		RULE_matrixRow = 0, RULE_matrixExpression = 1, RULE_functionExpression = 2, 
		RULE_resetSubExpression = 3, RULE_resetExpression = 4, RULE_guardExpression = 5, 
		RULE_invariantExpression = 6, RULE_flowExpression = 7, RULE_dottedVar = 8, 
		RULE_locExpression = 9, RULE_or = 10, RULE_and = 11, RULE_not = 12, RULE_op = 13, 
		RULE_compare = 14, RULE_addSub = 15, RULE_timesDiv = 16, RULE_pow = 17, 
		RULE_negativeUnary = 18, RULE_unary = 19;
	public static final String[] ruleNames = {
		"matrixRow", "matrixExpression", "functionExpression", "resetSubExpression", 
		"resetExpression", "guardExpression", "invariantExpression", "flowExpression", 
		"dottedVar", "locExpression", "or", "and", "not", "op", "compare", "addSub", 
		"timesDiv", "pow", "negativeUnary", "unary"
	};

	@Override
	public String getGrammarFileName() { return "HystExpression.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public HystExpressionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class MatrixRowContext extends ParserRuleContext {
		public MatrixRowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matrixRow; }
	 
		public MatrixRowContext() { }
		public void copyFrom(MatrixRowContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatrixRowExpContext extends MatrixRowContext {
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public List<TerminalNode> COMMA() { return getTokens(HystExpressionParser.COMMA); }
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public TerminalNode COMMA(int i) {
			return getToken(HystExpressionParser.COMMA, i);
		}
		public MatrixRowExpContext(MatrixRowContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrixRowExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatrixRowContext matrixRow() throws RecognitionException {
		MatrixRowContext _localctx = new MatrixRowContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_matrixRow);
		int _la;
		try {
			_localctx = new MatrixRowExpContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(46);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUM) | (1L << VAR) | (1L << LPAR) | (1L << LBRAC) | (1L << MINUS))) != 0)) {
				{
				{
				setState(40); addSub(0);
				setState(42);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(41); match(COMMA);
					}
				}

				}
				}
				setState(48);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MatrixExpressionContext extends ParserRuleContext {
		public MatrixExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matrixExpression; }
	 
		public MatrixExpressionContext() { }
		public void copyFrom(MatrixExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatrixContext extends MatrixExpressionContext {
		public List<TerminalNode> SEMICOLON() { return getTokens(HystExpressionParser.SEMICOLON); }
		public List<MatrixRowContext> matrixRow() {
			return getRuleContexts(MatrixRowContext.class);
		}
		public TerminalNode SEMICOLON(int i) {
			return getToken(HystExpressionParser.SEMICOLON, i);
		}
		public MatrixRowContext matrixRow(int i) {
			return getRuleContext(MatrixRowContext.class,i);
		}
		public TerminalNode LBRAC() { return getToken(HystExpressionParser.LBRAC, 0); }
		public TerminalNode RBRAC() { return getToken(HystExpressionParser.RBRAC, 0); }
		public MatrixContext(MatrixExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatrixExpressionContext matrixExpression() throws RecognitionException {
		MatrixExpressionContext _localctx = new MatrixExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_matrixExpression);
		int _la;
		try {
			_localctx = new MatrixContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(49); match(LBRAC);
			setState(50); matrixRow();
			setState(55);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(51); match(SEMICOLON);
				setState(52); matrixRow();
				}
				}
				setState(57);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(58); match(RBRAC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionExpressionContext extends ParserRuleContext {
		public FunctionExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionExpression; }
	 
		public FunctionExpressionContext() { }
		public void copyFrom(FunctionExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FunctionContext extends FunctionExpressionContext {
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public List<TerminalNode> COMMA() { return getTokens(HystExpressionParser.COMMA); }
		public TerminalNode VAR() { return getToken(HystExpressionParser.VAR, 0); }
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public TerminalNode COMMA(int i) {
			return getToken(HystExpressionParser.COMMA, i);
		}
		public FunctionContext(FunctionExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionExpressionContext functionExpression() throws RecognitionException {
		FunctionExpressionContext _localctx = new FunctionExpressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_functionExpression);
		int _la;
		try {
			_localctx = new FunctionContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(60); match(VAR);
			setState(61); match(LPAR);
			setState(70);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUM) | (1L << VAR) | (1L << LPAR) | (1L << LBRAC) | (1L << MINUS))) != 0)) {
				{
				setState(62); addSub(0);
				setState(67);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(63); match(COMMA);
					setState(64); addSub(0);
					}
					}
					setState(69);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(72); match(RPAR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResetSubExpressionContext extends ParserRuleContext {
		public ResetSubExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resetSubExpression; }
	 
		public ResetSubExpressionContext() { }
		public void copyFrom(ResetSubExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ResetSubEqContext extends ResetSubExpressionContext {
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
		public TerminalNode EQUAL_RESET() { return getToken(HystExpressionParser.EQUAL_RESET, 0); }
		public TerminalNode VAR() { return getToken(HystExpressionParser.VAR, 0); }
		public ResetSubEqContext(ResetSubExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitResetSubEq(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetSubOpContext extends ResetSubExpressionContext {
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public List<OpContext> op() {
			return getRuleContexts(OpContext.class);
		}
		public OpContext op(int i) {
			return getRuleContext(OpContext.class,i);
		}
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public ResetSubOpContext(ResetSubExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitResetSubOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResetSubExpressionContext resetSubExpression() throws RecognitionException {
		ResetSubExpressionContext _localctx = new ResetSubExpressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_resetSubExpression);
		int _la;
		try {
			setState(88);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new ResetSubEqContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(74); match(VAR);
				setState(75); match(EQUAL_RESET);
				setState(76); addSub(0);
				}
				break;
			case 2:
				_localctx = new ResetSubOpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(77); addSub(0);
				setState(78); op();
				setState(79); addSub(0);
				setState(85);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << GREATER) | (1L << LESSEQUAL) | (1L << GREATEREQUAL) | (1L << NOTEQUAL) | (1L << EQUAL))) != 0)) {
					{
					{
					setState(80); op();
					setState(81); addSub(0);
					}
					}
					setState(87);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResetExpressionContext extends ParserRuleContext {
		public ResetExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resetExpression; }
	 
		public ResetExpressionContext() { }
		public void copyFrom(ResetExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ResetBlankContext extends ResetExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public ResetBlankContext(ResetExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitResetBlank(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetContext extends ResetExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public List<TerminalNode> AND() { return getTokens(HystExpressionParser.AND); }
		public ResetSubExpressionContext resetSubExpression(int i) {
			return getRuleContext(ResetSubExpressionContext.class,i);
		}
		public TerminalNode AND(int i) {
			return getToken(HystExpressionParser.AND, i);
		}
		public List<ResetSubExpressionContext> resetSubExpression() {
			return getRuleContexts(ResetSubExpressionContext.class);
		}
		public ResetContext(ResetExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitReset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResetExpressionContext resetExpression() throws RecognitionException {
		ResetExpressionContext _localctx = new ResetExpressionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_resetExpression);
		int _la;
		try {
			setState(101);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new ResetBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(90); match(EOF);
				}
				break;
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
				_localctx = new ResetContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(91); resetSubExpression();
				setState(96);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AND) {
					{
					{
					setState(92); match(AND);
					setState(93); resetSubExpression();
					}
					}
					setState(98);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(99); match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GuardExpressionContext extends ParserRuleContext {
		public GuardExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardExpression; }
	 
		public GuardExpressionContext() { }
		public void copyFrom(GuardExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class GuardContext extends GuardExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public GuardContext(GuardExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitGuard(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GuardBlankContext extends GuardExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public GuardBlankContext(GuardExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitGuardBlank(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GuardExpressionContext guardExpression() throws RecognitionException {
		GuardExpressionContext _localctx = new GuardExpressionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_guardExpression);
		try {
			setState(107);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new GuardBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(103); match(EOF);
				}
				break;
			case TRUE:
			case FALSE:
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
			case NOT:
				_localctx = new GuardContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(104); or();
				setState(105); match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InvariantExpressionContext extends ParserRuleContext {
		public InvariantExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invariantExpression; }
	 
		public InvariantExpressionContext() { }
		public void copyFrom(InvariantExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class InvariantContext extends InvariantExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public InvariantContext(InvariantExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitInvariant(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InvariantBlankContext extends InvariantExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public InvariantBlankContext(InvariantExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitInvariantBlank(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InvariantExpressionContext invariantExpression() throws RecognitionException {
		InvariantExpressionContext _localctx = new InvariantExpressionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_invariantExpression);
		try {
			setState(113);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new InvariantBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(109); match(EOF);
				}
				break;
			case TRUE:
			case FALSE:
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
			case NOT:
				_localctx = new InvariantContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(110); or();
				setState(111); match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FlowExpressionContext extends ParserRuleContext {
		public FlowExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flowExpression; }
	 
		public FlowExpressionContext() { }
		public void copyFrom(FlowExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FlowContext extends FlowExpressionContext {
		public TerminalNode VAR(int i) {
			return getToken(HystExpressionParser.VAR, i);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public List<TerminalNode> AND() { return getTokens(HystExpressionParser.AND); }
		public TerminalNode EQUAL(int i) {
			return getToken(HystExpressionParser.EQUAL, i);
		}
		public List<TerminalNode> VAR() { return getTokens(HystExpressionParser.VAR); }
		public List<TerminalNode> TICK() { return getTokens(HystExpressionParser.TICK); }
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public TerminalNode TICK(int i) {
			return getToken(HystExpressionParser.TICK, i);
		}
		public List<TerminalNode> EQUAL() { return getTokens(HystExpressionParser.EQUAL); }
		public TerminalNode AND(int i) {
			return getToken(HystExpressionParser.AND, i);
		}
		public FlowContext(FlowExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitFlow(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FlowFalseContext extends FlowExpressionContext {
		public TerminalNode FALSE() { return getToken(HystExpressionParser.FALSE, 0); }
		public FlowFalseContext(FlowExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitFlowFalse(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FlowBlankContext extends FlowExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public FlowBlankContext(FlowExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitFlowBlank(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlowExpressionContext flowExpression() throws RecognitionException {
		FlowExpressionContext _localctx = new FlowExpressionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_flowExpression);
		int _la;
		try {
			setState(137);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new FlowBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(115); match(EOF);
				}
				break;
			case VAR:
				_localctx = new FlowContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(116); match(VAR);
				setState(118);
				_la = _input.LA(1);
				if (_la==TICK) {
					{
					setState(117); match(TICK);
					}
				}

				setState(120); match(EQUAL);
				setState(121); addSub(0);
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AND) {
					{
					{
					setState(122); match(AND);
					setState(123); match(VAR);
					setState(125);
					_la = _input.LA(1);
					if (_la==TICK) {
						{
						setState(124); match(TICK);
						}
					}

					setState(127); match(EQUAL);
					setState(128); addSub(0);
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(134); match(EOF);
				}
				break;
			case FALSE:
				_localctx = new FlowFalseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(136); match(FALSE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DottedVarContext extends ParserRuleContext {
		public DottedVarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dottedVar; }
	 
		public DottedVarContext() { }
		public void copyFrom(DottedVarContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DotVarContext extends DottedVarContext {
		public TerminalNode VAR(int i) {
			return getToken(HystExpressionParser.VAR, i);
		}
		public List<TerminalNode> DOT() { return getTokens(HystExpressionParser.DOT); }
		public List<TerminalNode> VAR() { return getTokens(HystExpressionParser.VAR); }
		public TerminalNode TICK() { return getToken(HystExpressionParser.TICK, 0); }
		public TerminalNode DOT(int i) {
			return getToken(HystExpressionParser.DOT, i);
		}
		public DotVarContext(DottedVarContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitDotVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DottedVarContext dottedVar() throws RecognitionException {
		DottedVarContext _localctx = new DottedVarContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_dottedVar);
		try {
			int _alt;
			_localctx = new DotVarContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(139); match(VAR);
			setState(144);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(140); match(DOT);
					setState(141); match(VAR);
					}
					} 
				}
				setState(146);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			setState(148);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(147); match(TICK);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LocExpressionContext extends ParserRuleContext {
		public LocExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locExpression; }
	 
		public LocExpressionContext() { }
		public void copyFrom(LocExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LocExpContext extends LocExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public LocExpContext(LocExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitLocExp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LocFalseContext extends LocExpressionContext {
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public LocFalseContext(LocExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitLocFalse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocExpressionContext locExpression() throws RecognitionException {
		LocExpressionContext _localctx = new LocExpressionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_locExpression);
		try {
			setState(154);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
			case NOT:
				_localctx = new LocExpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(150); and();
				setState(151); match(EOF);
				}
				break;
			case EOF:
				_localctx = new LocFalseContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(153); match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrContext extends ParserRuleContext {
		public OrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or; }
	 
		public OrContext() { }
		public void copyFrom(OrContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class OrExpressionContext extends OrContext {
		public TerminalNode OR() { return getToken(HystExpressionParser.OR, 0); }
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public OrExpressionContext(OrContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitOrExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ToAndContext extends OrContext {
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public ToAndContext(OrContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToAnd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrContext or() throws RecognitionException {
		OrContext _localctx = new OrContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_or);
		try {
			setState(161);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				_localctx = new OrExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(156); and();
				setState(157); match(OR);
				setState(158); or();
				}
				break;
			case 2:
				_localctx = new ToAndContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(160); and();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AndContext extends ParserRuleContext {
		public AndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and; }
	 
		public AndContext() { }
		public void copyFrom(AndContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ToNotContext extends AndContext {
		public NotContext not() {
			return getRuleContext(NotContext.class,0);
		}
		public ToNotContext(AndContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToNot(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AndExpressionContext extends AndContext {
		public TerminalNode AND() { return getToken(HystExpressionParser.AND, 0); }
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public NotContext not() {
			return getRuleContext(NotContext.class,0);
		}
		public AndExpressionContext(AndContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitAndExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AndContext and() throws RecognitionException {
		AndContext _localctx = new AndContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_and);
		try {
			setState(168);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				_localctx = new AndExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(163); not();
				setState(164); match(AND);
				setState(165); and();
				}
				break;
			case 2:
				_localctx = new ToNotContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(167); not();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotContext extends ParserRuleContext {
		public NotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_not; }
	 
		public NotContext() { }
		public void copyFrom(NotContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ToCompareContext extends NotContext {
		public CompareContext compare() {
			return getRuleContext(CompareContext.class,0);
		}
		public ToCompareContext(NotContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToCompare(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotExpressionContext extends NotContext {
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public TerminalNode NOT() { return getToken(HystExpressionParser.NOT, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public NotExpressionContext(NotContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNotExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BoolParenthesesContext extends NotContext {
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public BoolParenthesesContext(NotContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitBoolParentheses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotContext not() throws RecognitionException {
		NotContext _localctx = new NotContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_not);
		try {
			setState(180);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				_localctx = new NotExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(170); match(NOT);
				setState(171); match(LPAR);
				setState(172); or();
				setState(173); match(RPAR);
				}
				break;
			case 2:
				_localctx = new BoolParenthesesContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(175); match(LPAR);
				setState(176); or();
				setState(177); match(RPAR);
				}
				break;
			case 3:
				_localctx = new ToCompareContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(179); compare();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OpContext extends ParserRuleContext {
		public OpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op; }
	 
		public OpContext() { }
		public void copyFrom(OpContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LessOpContext extends OpContext {
		public TerminalNode LESS() { return getToken(HystExpressionParser.LESS, 0); }
		public LessOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitLessOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LessEqualOpContext extends OpContext {
		public TerminalNode LESSEQUAL() { return getToken(HystExpressionParser.LESSEQUAL, 0); }
		public LessEqualOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitLessEqualOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EqualOpContext extends OpContext {
		public TerminalNode EQUAL() { return getToken(HystExpressionParser.EQUAL, 0); }
		public EqualOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitEqualOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotEqualOpContext extends OpContext {
		public TerminalNode NOTEQUAL() { return getToken(HystExpressionParser.NOTEQUAL, 0); }
		public NotEqualOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNotEqualOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GreaterEqualOpContext extends OpContext {
		public TerminalNode GREATEREQUAL() { return getToken(HystExpressionParser.GREATEREQUAL, 0); }
		public GreaterEqualOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitGreaterEqualOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GreaterOpContext extends OpContext {
		public TerminalNode GREATER() { return getToken(HystExpressionParser.GREATER, 0); }
		public GreaterOpContext(OpContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitGreaterOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OpContext op() throws RecognitionException {
		OpContext _localctx = new OpContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_op);
		try {
			setState(188);
			switch (_input.LA(1)) {
			case EQUAL:
				_localctx = new EqualOpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(182); match(EQUAL);
				}
				break;
			case LESS:
				_localctx = new LessOpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(183); match(LESS);
				}
				break;
			case LESSEQUAL:
				_localctx = new LessEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(184); match(LESSEQUAL);
				}
				break;
			case GREATER:
				_localctx = new GreaterOpContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(185); match(GREATER);
				}
				break;
			case GREATEREQUAL:
				_localctx = new GreaterEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(186); match(GREATEREQUAL);
				}
				break;
			case NOTEQUAL:
				_localctx = new NotEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(187); match(NOTEQUAL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CompareContext extends ParserRuleContext {
		public CompareContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compare; }
	 
		public CompareContext() { }
		public void copyFrom(CompareContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class BoolOpContext extends CompareContext {
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public List<OpContext> op() {
			return getRuleContexts(OpContext.class);
		}
		public OpContext op(int i) {
			return getRuleContext(OpContext.class,i);
		}
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public BoolOpContext(CompareContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitBoolOp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ConstFalseContext extends CompareContext {
		public TerminalNode FALSE() { return getToken(HystExpressionParser.FALSE, 0); }
		public ConstFalseContext(CompareContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitConstFalse(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ConstTrueContext extends CompareContext {
		public TerminalNode TRUE() { return getToken(HystExpressionParser.TRUE, 0); }
		public ConstTrueContext(CompareContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitConstTrue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompareContext compare() throws RecognitionException {
		CompareContext _localctx = new CompareContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_compare);
		int _la;
		try {
			setState(203);
			switch (_input.LA(1)) {
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
				_localctx = new BoolOpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(190); addSub(0);
				setState(191); op();
				setState(192); addSub(0);
				setState(198);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << GREATER) | (1L << LESSEQUAL) | (1L << GREATEREQUAL) | (1L << NOTEQUAL) | (1L << EQUAL))) != 0)) {
					{
					{
					setState(193); op();
					setState(194); addSub(0);
					}
					}
					setState(200);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case TRUE:
				_localctx = new ConstTrueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(201); match(TRUE);
				}
				break;
			case FALSE:
				_localctx = new ConstFalseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(202); match(FALSE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AddSubContext extends ParserRuleContext {
		public AddSubContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_addSub; }
	 
		public AddSubContext() { }
		public void copyFrom(AddSubContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PlusContext extends AddSubContext {
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(HystExpressionParser.PLUS, 0); }
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public PlusContext(AddSubContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitPlus(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MinusContext extends AddSubContext {
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(HystExpressionParser.MINUS, 0); }
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public MinusContext(AddSubContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMinus(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ToTimesDivContext extends AddSubContext {
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public ToTimesDivContext(AddSubContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToTimesDiv(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AddSubContext addSub() throws RecognitionException {
		return addSub(0);
	}

	private AddSubContext addSub(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		AddSubContext _localctx = new AddSubContext(_ctx, _parentState);
		AddSubContext _prevctx = _localctx;
		int _startState = 30;
		enterRecursionRule(_localctx, 30, RULE_addSub, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToTimesDivContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(206); timesDiv(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(216);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(214);
					switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
					case 1:
						{
						_localctx = new PlusContext(new AddSubContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_addSub);
						setState(208);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(209); match(PLUS);
						setState(210); timesDiv(0);
						}
						break;
					case 2:
						{
						_localctx = new MinusContext(new AddSubContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_addSub);
						setState(211);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(212); match(MINUS);
						setState(213); timesDiv(0);
						}
						break;
					}
					} 
				}
				setState(218);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class TimesDivContext extends ParserRuleContext {
		public TimesDivContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_timesDiv; }
	 
		public TimesDivContext() { }
		public void copyFrom(TimesDivContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MultiplicationContext extends TimesDivContext {
		public TerminalNode TIMES() { return getToken(HystExpressionParser.TIMES, 0); }
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public MultiplicationContext(TimesDivContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMultiplication(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DivisionContext extends TimesDivContext {
		public TerminalNode DIV() { return getToken(HystExpressionParser.DIV, 0); }
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public DivisionContext(TimesDivContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitDivision(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ToPowContext extends TimesDivContext {
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public ToPowContext(TimesDivContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToPow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimesDivContext timesDiv() throws RecognitionException {
		return timesDiv(0);
	}

	private TimesDivContext timesDiv(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TimesDivContext _localctx = new TimesDivContext(_ctx, _parentState);
		TimesDivContext _prevctx = _localctx;
		int _startState = 32;
		enterRecursionRule(_localctx, 32, RULE_timesDiv, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToPowContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(220); pow(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(230);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(228);
					switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicationContext(new TimesDivContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_timesDiv);
						setState(222);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(223); match(TIMES);
						setState(224); pow(0);
						}
						break;
					case 2:
						{
						_localctx = new DivisionContext(new TimesDivContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_timesDiv);
						setState(225);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(226); match(DIV);
						setState(227); pow(0);
						}
						break;
					}
					} 
				}
				setState(232);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class PowContext extends ParserRuleContext {
		public PowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pow; }
	 
		public PowContext() { }
		public void copyFrom(PowContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ToNegativeUnaryContext extends PowContext {
		public NegativeUnaryContext negativeUnary() {
			return getRuleContext(NegativeUnaryContext.class,0);
		}
		public ToNegativeUnaryContext(PowContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToNegativeUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PowExpressionContext extends PowContext {
		public NegativeUnaryContext negativeUnary() {
			return getRuleContext(NegativeUnaryContext.class,0);
		}
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public TerminalNode POW() { return getToken(HystExpressionParser.POW, 0); }
		public PowExpressionContext(PowContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitPowExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PowContext pow() throws RecognitionException {
		return pow(0);
	}

	private PowContext pow(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PowContext _localctx = new PowContext(_ctx, _parentState);
		PowContext _prevctx = _localctx;
		int _startState = 34;
		enterRecursionRule(_localctx, 34, RULE_pow, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToNegativeUnaryContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(234); negativeUnary();
			}
			_ctx.stop = _input.LT(-1);
			setState(241);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new PowExpressionContext(new PowContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_pow);
					setState(236);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(237); match(POW);
					setState(238); negativeUnary();
					}
					} 
				}
				setState(243);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class NegativeUnaryContext extends ParserRuleContext {
		public NegativeUnaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_negativeUnary; }
	 
		public NegativeUnaryContext() { }
		public void copyFrom(NegativeUnaryContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ToUnaryContext extends NegativeUnaryContext {
		public UnaryContext unary() {
			return getRuleContext(UnaryContext.class,0);
		}
		public ToUnaryContext(NegativeUnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitToUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NegativeContext extends NegativeUnaryContext {
		public NegativeUnaryContext negativeUnary() {
			return getRuleContext(NegativeUnaryContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(HystExpressionParser.MINUS, 0); }
		public NegativeContext(NegativeUnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNegative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NegativeUnaryContext negativeUnary() throws RecognitionException {
		NegativeUnaryContext _localctx = new NegativeUnaryContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_negativeUnary);
		try {
			setState(247);
			switch (_input.LA(1)) {
			case MINUS:
				_localctx = new NegativeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(244); match(MINUS);
				setState(245); negativeUnary();
				}
				break;
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
				_localctx = new ToUnaryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(246); unary();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnaryContext extends ParserRuleContext {
		public UnaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary; }
	 
		public UnaryContext() { }
		public void copyFrom(UnaryContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DottedVariableContext extends UnaryContext {
		public DottedVarContext dottedVar() {
			return getRuleContext(DottedVarContext.class,0);
		}
		public DottedVariableContext(UnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitDottedVariable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParenthesesContext extends UnaryContext {
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public ParenthesesContext(UnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitParentheses(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NumberContext extends UnaryContext {
		public TerminalNode NUM() { return getToken(HystExpressionParser.NUM, 0); }
		public NumberContext(UnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FuncExpContext extends UnaryContext {
		public FunctionExpressionContext functionExpression() {
			return getRuleContext(FunctionExpressionContext.class,0);
		}
		public FuncExpContext(UnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitFuncExp(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MatrixExpContext extends UnaryContext {
		public MatrixExpressionContext matrixExpression() {
			return getRuleContext(MatrixExpressionContext.class,0);
		}
		public MatrixExpContext(UnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrixExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryContext unary() throws RecognitionException {
		UnaryContext _localctx = new UnaryContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_unary);
		try {
			setState(257);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				_localctx = new MatrixExpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(249); matrixExpression();
				}
				break;
			case 2:
				_localctx = new FuncExpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(250); functionExpression();
				}
				break;
			case 3:
				_localctx = new NumberContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(251); match(NUM);
				}
				break;
			case 4:
				_localctx = new DottedVariableContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(252); dottedVar();
				}
				break;
			case 5:
				_localctx = new ParenthesesContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(253); match(LPAR);
				setState(254); addSub(0);
				setState(255); match(RPAR);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 15: return addSub_sempred((AddSubContext)_localctx, predIndex);
		case 16: return timesDiv_sempred((TimesDivContext)_localctx, predIndex);
		case 17: return pow_sempred((PowContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean pow_sempred(PowContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4: return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean addSub_sempred(AddSubContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0: return precpred(_ctx, 3);
		case 1: return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean timesDiv_sempred(TimesDivContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2: return precpred(_ctx, 3);
		case 3: return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\36\u0106\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\3\2\3\2\5\2-\n\2\7\2/\n\2\f\2\16\2\62\13"+
		"\2\3\3\3\3\3\3\3\3\7\38\n\3\f\3\16\3;\13\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4"+
		"\7\4D\n\4\f\4\16\4G\13\4\5\4I\n\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\7\5V\n\5\f\5\16\5Y\13\5\5\5[\n\5\3\6\3\6\3\6\3\6\7\6a\n\6\f\6"+
		"\16\6d\13\6\3\6\3\6\5\6h\n\6\3\7\3\7\3\7\3\7\5\7n\n\7\3\b\3\b\3\b\3\b"+
		"\5\bt\n\b\3\t\3\t\3\t\5\ty\n\t\3\t\3\t\3\t\3\t\3\t\5\t\u0080\n\t\3\t\3"+
		"\t\7\t\u0084\n\t\f\t\16\t\u0087\13\t\3\t\3\t\3\t\5\t\u008c\n\t\3\n\3\n"+
		"\3\n\7\n\u0091\n\n\f\n\16\n\u0094\13\n\3\n\5\n\u0097\n\n\3\13\3\13\3\13"+
		"\3\13\5\13\u009d\n\13\3\f\3\f\3\f\3\f\3\f\5\f\u00a4\n\f\3\r\3\r\3\r\3"+
		"\r\3\r\5\r\u00ab\n\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\5\16\u00b7\n\16\3\17\3\17\3\17\3\17\3\17\3\17\5\17\u00bf\n\17\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\7\20\u00c7\n\20\f\20\16\20\u00ca\13\20\3\20\3"+
		"\20\5\20\u00ce\n\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\7\21"+
		"\u00d9\n\21\f\21\16\21\u00dc\13\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\7\22\u00e7\n\22\f\22\16\22\u00ea\13\22\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\7\23\u00f2\n\23\f\23\16\23\u00f5\13\23\3\24\3\24\3\24\5\24"+
		"\u00fa\n\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u0104\n\25\3"+
		"\25\2\5 \"$\26\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(\2\2\u011a"+
		"\2\60\3\2\2\2\4\63\3\2\2\2\6>\3\2\2\2\bZ\3\2\2\2\ng\3\2\2\2\fm\3\2\2\2"+
		"\16s\3\2\2\2\20\u008b\3\2\2\2\22\u008d\3\2\2\2\24\u009c\3\2\2\2\26\u00a3"+
		"\3\2\2\2\30\u00aa\3\2\2\2\32\u00b6\3\2\2\2\34\u00be\3\2\2\2\36\u00cd\3"+
		"\2\2\2 \u00cf\3\2\2\2\"\u00dd\3\2\2\2$\u00eb\3\2\2\2&\u00f9\3\2\2\2(\u0103"+
		"\3\2\2\2*,\5 \21\2+-\7\r\2\2,+\3\2\2\2,-\3\2\2\2-/\3\2\2\2.*\3\2\2\2/"+
		"\62\3\2\2\2\60.\3\2\2\2\60\61\3\2\2\2\61\3\3\2\2\2\62\60\3\2\2\2\63\64"+
		"\7\13\2\2\649\5\2\2\2\65\66\7\16\2\2\668\5\2\2\2\67\65\3\2\2\28;\3\2\2"+
		"\29\67\3\2\2\29:\3\2\2\2:<\3\2\2\2;9\3\2\2\2<=\7\f\2\2=\5\3\2\2\2>?\7"+
		"\b\2\2?H\7\t\2\2@E\5 \21\2AB\7\r\2\2BD\5 \21\2CA\3\2\2\2DG\3\2\2\2EC\3"+
		"\2\2\2EF\3\2\2\2FI\3\2\2\2GE\3\2\2\2H@\3\2\2\2HI\3\2\2\2IJ\3\2\2\2JK\7"+
		"\n\2\2K\7\3\2\2\2LM\7\b\2\2MN\7\36\2\2N[\5 \21\2OP\5 \21\2PQ\5\34\17\2"+
		"QW\5 \21\2RS\5\34\17\2ST\5 \21\2TV\3\2\2\2UR\3\2\2\2VY\3\2\2\2WU\3\2\2"+
		"\2WX\3\2\2\2X[\3\2\2\2YW\3\2\2\2ZL\3\2\2\2ZO\3\2\2\2[\t\3\2\2\2\\h\7\2"+
		"\2\3]b\5\b\5\2^_\7\25\2\2_a\5\b\5\2`^\3\2\2\2ad\3\2\2\2b`\3\2\2\2bc\3"+
		"\2\2\2ce\3\2\2\2db\3\2\2\2ef\7\2\2\3fh\3\2\2\2g\\\3\2\2\2g]\3\2\2\2h\13"+
		"\3\2\2\2in\7\2\2\3jk\5\26\f\2kl\7\2\2\3ln\3\2\2\2mi\3\2\2\2mj\3\2\2\2"+
		"n\r\3\2\2\2ot\7\2\2\3pq\5\26\f\2qr\7\2\2\3rt\3\2\2\2so\3\2\2\2sp\3\2\2"+
		"\2t\17\3\2\2\2u\u008c\7\2\2\3vx\7\b\2\2wy\7\4\2\2xw\3\2\2\2xy\3\2\2\2"+
		"yz\3\2\2\2z{\7\35\2\2{\u0085\5 \21\2|}\7\25\2\2}\177\7\b\2\2~\u0080\7"+
		"\4\2\2\177~\3\2\2\2\177\u0080\3\2\2\2\u0080\u0081\3\2\2\2\u0081\u0082"+
		"\7\35\2\2\u0082\u0084\5 \21\2\u0083|\3\2\2\2\u0084\u0087\3\2\2\2\u0085"+
		"\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0088\3\2\2\2\u0087\u0085\3\2"+
		"\2\2\u0088\u0089\7\2\2\3\u0089\u008c\3\2\2\2\u008a\u008c\7\6\2\2\u008b"+
		"u\3\2\2\2\u008bv\3\2\2\2\u008b\u008a\3\2\2\2\u008c\21\3\2\2\2\u008d\u0092"+
		"\7\b\2\2\u008e\u008f\7\24\2\2\u008f\u0091\7\b\2\2\u0090\u008e\3\2\2\2"+
		"\u0091\u0094\3\2\2\2\u0092\u0090\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0096"+
		"\3\2\2\2\u0094\u0092\3\2\2\2\u0095\u0097\7\4\2\2\u0096\u0095\3\2\2\2\u0096"+
		"\u0097\3\2\2\2\u0097\23\3\2\2\2\u0098\u0099\5\30\r\2\u0099\u009a\7\2\2"+
		"\3\u009a\u009d\3\2\2\2\u009b\u009d\7\2\2\3\u009c\u0098\3\2\2\2\u009c\u009b"+
		"\3\2\2\2\u009d\25\3\2\2\2\u009e\u009f\5\30\r\2\u009f\u00a0\7\26\2\2\u00a0"+
		"\u00a1\5\26\f\2\u00a1\u00a4\3\2\2\2\u00a2\u00a4\5\30\r\2\u00a3\u009e\3"+
		"\2\2\2\u00a3\u00a2\3\2\2\2\u00a4\27\3\2\2\2\u00a5\u00a6\5\32\16\2\u00a6"+
		"\u00a7\7\25\2\2\u00a7\u00a8\5\30\r\2\u00a8\u00ab\3\2\2\2\u00a9\u00ab\5"+
		"\32\16\2\u00aa\u00a5\3\2\2\2\u00aa\u00a9\3\2\2\2\u00ab\31\3\2\2\2\u00ac"+
		"\u00ad\7\27\2\2\u00ad\u00ae\7\t\2\2\u00ae\u00af\5\26\f\2\u00af\u00b0\7"+
		"\n\2\2\u00b0\u00b7\3\2\2\2\u00b1\u00b2\7\t\2\2\u00b2\u00b3\5\26\f\2\u00b3"+
		"\u00b4\7\n\2\2\u00b4\u00b7\3\2\2\2\u00b5\u00b7\5\36\20\2\u00b6\u00ac\3"+
		"\2\2\2\u00b6\u00b1\3\2\2\2\u00b6\u00b5\3\2\2\2\u00b7\33\3\2\2\2\u00b8"+
		"\u00bf\7\35\2\2\u00b9\u00bf\7\30\2\2\u00ba\u00bf\7\32\2\2\u00bb\u00bf"+
		"\7\31\2\2\u00bc\u00bf\7\33\2\2\u00bd\u00bf\7\34\2\2\u00be\u00b8\3\2\2"+
		"\2\u00be\u00b9\3\2\2\2\u00be\u00ba\3\2\2\2\u00be\u00bb\3\2\2\2\u00be\u00bc"+
		"\3\2\2\2\u00be\u00bd\3\2\2\2\u00bf\35\3\2\2\2\u00c0\u00c1\5 \21\2\u00c1"+
		"\u00c2\5\34\17\2\u00c2\u00c8\5 \21\2\u00c3\u00c4\5\34\17\2\u00c4\u00c5"+
		"\5 \21\2\u00c5\u00c7\3\2\2\2\u00c6\u00c3\3\2\2\2\u00c7\u00ca\3\2\2\2\u00c8"+
		"\u00c6\3\2\2\2\u00c8\u00c9\3\2\2\2\u00c9\u00ce\3\2\2\2\u00ca\u00c8\3\2"+
		"\2\2\u00cb\u00ce\7\5\2\2\u00cc\u00ce\7\6\2\2\u00cd\u00c0\3\2\2\2\u00cd"+
		"\u00cb\3\2\2\2\u00cd\u00cc\3\2\2\2\u00ce\37\3\2\2\2\u00cf\u00d0\b\21\1"+
		"\2\u00d0\u00d1\5\"\22\2\u00d1\u00da\3\2\2\2\u00d2\u00d3\f\5\2\2\u00d3"+
		"\u00d4\7\17\2\2\u00d4\u00d9\5\"\22\2\u00d5\u00d6\f\4\2\2\u00d6\u00d7\7"+
		"\20\2\2\u00d7\u00d9\5\"\22\2\u00d8\u00d2\3\2\2\2\u00d8\u00d5\3\2\2\2\u00d9"+
		"\u00dc\3\2\2\2\u00da\u00d8\3\2\2\2\u00da\u00db\3\2\2\2\u00db!\3\2\2\2"+
		"\u00dc\u00da\3\2\2\2\u00dd\u00de\b\22\1\2\u00de\u00df\5$\23\2\u00df\u00e8"+
		"\3\2\2\2\u00e0\u00e1\f\5\2\2\u00e1\u00e2\7\21\2\2\u00e2\u00e7\5$\23\2"+
		"\u00e3\u00e4\f\4\2\2\u00e4\u00e5\7\22\2\2\u00e5\u00e7\5$\23\2\u00e6\u00e0"+
		"\3\2\2\2\u00e6\u00e3\3\2\2\2\u00e7\u00ea\3\2\2\2\u00e8\u00e6\3\2\2\2\u00e8"+
		"\u00e9\3\2\2\2\u00e9#\3\2\2\2\u00ea\u00e8\3\2\2\2\u00eb\u00ec\b\23\1\2"+
		"\u00ec\u00ed\5&\24\2\u00ed\u00f3\3\2\2\2\u00ee\u00ef\f\4\2\2\u00ef\u00f0"+
		"\7\23\2\2\u00f0\u00f2\5&\24\2\u00f1\u00ee\3\2\2\2\u00f2\u00f5\3\2\2\2"+
		"\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4%\3\2\2\2\u00f5\u00f3\3"+
		"\2\2\2\u00f6\u00f7\7\20\2\2\u00f7\u00fa\5&\24\2\u00f8\u00fa\5(\25\2\u00f9"+
		"\u00f6\3\2\2\2\u00f9\u00f8\3\2\2\2\u00fa\'\3\2\2\2\u00fb\u0104\5\4\3\2"+
		"\u00fc\u0104\5\6\4\2\u00fd\u0104\7\7\2\2\u00fe\u0104\5\22\n\2\u00ff\u0100"+
		"\7\t\2\2\u0100\u0101\5 \21\2\u0101\u0102\7\n\2\2\u0102\u0104\3\2\2\2\u0103"+
		"\u00fb\3\2\2\2\u0103\u00fc\3\2\2\2\u0103\u00fd\3\2\2\2\u0103\u00fe\3\2"+
		"\2\2\u0103\u00ff\3\2\2\2\u0104)\3\2\2\2!,\609EHWZbgmsx\177\u0085\u008b"+
		"\u0092\u0096\u009c\u00a3\u00aa\u00b6\u00be\u00c8\u00cd\u00d8\u00da\u00e6"+
		"\u00e8\u00f3\u00f9\u0103";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
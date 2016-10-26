// Generated from HystExpression.g4 by ANTLR 4.5.3
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
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, TICK=2, TRUE=3, FALSE=4, NUM=5, VAR=6, LPAR=7, RPAR=8, LBRAC=9, 
		RBRAC=10, COMMA=11, SEMICOLON=12, COLON=13, PLUS=14, MINUS=15, TIMES=16, 
		DIV=17, POW=18, DOT=19, AND=20, OR=21, NOT=22, LESS=23, GREATER=24, LESSEQUAL=25, 
		GREATEREQUAL=26, NOTEQUAL=27, EQUAL=28, EQUAL_RESET=29;
	public static final int
		RULE_matrixRow = 0, RULE_matrixRange = 1, RULE_matrixExpression = 2, RULE_functionExpression = 3, 
		RULE_resetSubExpression = 4, RULE_resetExpression = 5, RULE_guardExpression = 6, 
		RULE_invariantExpression = 7, RULE_flowExpression = 8, RULE_dottedVar = 9, 
		RULE_locExpression = 10, RULE_or = 11, RULE_and = 12, RULE_not = 13, RULE_op = 14, 
		RULE_compare = 15, RULE_addSub = 16, RULE_timesDiv = 17, RULE_pow = 18, 
		RULE_negativeUnary = 19, RULE_unary = 20;
	public static final String[] ruleNames = {
		"matrixRow", "matrixRange", "matrixExpression", "functionExpression", 
		"resetSubExpression", "resetExpression", "guardExpression", "invariantExpression", 
		"flowExpression", "dottedVar", "locExpression", "or", "and", "not", "op", 
		"compare", "addSub", "timesDiv", "pow", "negativeUnary", "unary"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'''", "'true'", "'false'", null, null, "'('", "')'", "'['", 
		"']'", "','", "';'", "':'", "'+'", "'-'", "'*'", "'/'", "'^'", "'.'", 
		null, null, "'!'", "'<'", "'>'", "'<='", "'>='", "'!='", null, "':='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "TICK", "TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", 
		"RBRAC", "COMMA", "SEMICOLON", "COLON", "PLUS", "MINUS", "TIMES", "DIV", 
		"POW", "DOT", "AND", "OR", "NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", 
		"NOTEQUAL", "EQUAL", "EQUAL_RESET"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "HystExpression.g4"; }

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
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(HystExpressionParser.COMMA); }
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
			setState(42);
			addSub(0);
			setState(47);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(43);
				match(COMMA);
				setState(44);
				addSub(0);
				}
				}
				setState(49);
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

	public static class MatrixRangeContext extends ParserRuleContext {
		public MatrixRangeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matrixRange; }
	 
		public MatrixRangeContext() { }
		public void copyFrom(MatrixRangeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatrixRangeExpContext extends MatrixRangeContext {
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public List<TerminalNode> COLON() { return getTokens(HystExpressionParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(HystExpressionParser.COLON, i);
		}
		public MatrixRangeExpContext(MatrixRangeContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrixRangeExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatrixRangeContext matrixRange() throws RecognitionException {
		MatrixRangeContext _localctx = new MatrixRangeContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_matrixRange);
		try {
			_localctx = new MatrixRangeExpContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			addSub(0);
			setState(51);
			match(COLON);
			setState(52);
			addSub(0);
			setState(53);
			match(COLON);
			setState(54);
			addSub(0);
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
	public static class MatrixExplicitContext extends MatrixExpressionContext {
		public TerminalNode LBRAC() { return getToken(HystExpressionParser.LBRAC, 0); }
		public List<MatrixRowContext> matrixRow() {
			return getRuleContexts(MatrixRowContext.class);
		}
		public MatrixRowContext matrixRow(int i) {
			return getRuleContext(MatrixRowContext.class,i);
		}
		public TerminalNode RBRAC() { return getToken(HystExpressionParser.RBRAC, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(HystExpressionParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(HystExpressionParser.SEMICOLON, i);
		}
		public MatrixExplicitContext(MatrixExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrixExplicit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MatrixGeneratedContext extends MatrixExpressionContext {
		public TerminalNode LBRAC() { return getToken(HystExpressionParser.LBRAC, 0); }
		public MatrixRangeContext matrixRange() {
			return getRuleContext(MatrixRangeContext.class,0);
		}
		public TerminalNode RBRAC() { return getToken(HystExpressionParser.RBRAC, 0); }
		public MatrixGeneratedContext(MatrixExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMatrixGenerated(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatrixExpressionContext matrixExpression() throws RecognitionException {
		MatrixExpressionContext _localctx = new MatrixExpressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_matrixExpression);
		int _la;
		try {
			setState(71);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				_localctx = new MatrixExplicitContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(56);
				match(LBRAC);
				setState(57);
				matrixRow();
				setState(62);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SEMICOLON) {
					{
					{
					setState(58);
					match(SEMICOLON);
					setState(59);
					matrixRow();
					}
					}
					setState(64);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(65);
				match(RBRAC);
				}
				break;
			case 2:
				_localctx = new MatrixGeneratedContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(67);
				match(LBRAC);
				setState(68);
				matrixRange();
				setState(69);
				match(RBRAC);
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
		public TerminalNode VAR() { return getToken(HystExpressionParser.VAR, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(HystExpressionParser.COMMA); }
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
		enterRule(_localctx, 6, RULE_functionExpression);
		int _la;
		try {
			_localctx = new FunctionContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(VAR);
			setState(74);
			match(LPAR);
			setState(83);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUM) | (1L << VAR) | (1L << LPAR) | (1L << LBRAC) | (1L << MINUS))) != 0)) {
				{
				setState(75);
				addSub(0);
				setState(80);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(76);
					match(COMMA);
					setState(77);
					addSub(0);
					}
					}
					setState(82);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(85);
			match(RPAR);
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
		public TerminalNode VAR() { return getToken(HystExpressionParser.VAR, 0); }
		public TerminalNode EQUAL_RESET() { return getToken(HystExpressionParser.EQUAL_RESET, 0); }
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
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
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public List<OpContext> op() {
			return getRuleContexts(OpContext.class);
		}
		public OpContext op(int i) {
			return getRuleContext(OpContext.class,i);
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
		enterRule(_localctx, 8, RULE_resetSubExpression);
		int _la;
		try {
			setState(101);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new ResetSubEqContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(87);
				match(VAR);
				setState(88);
				match(EQUAL_RESET);
				setState(89);
				addSub(0);
				}
				break;
			case 2:
				_localctx = new ResetSubOpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(90);
				addSub(0);
				setState(91);
				op();
				setState(92);
				addSub(0);
				setState(98);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << GREATER) | (1L << LESSEQUAL) | (1L << GREATEREQUAL) | (1L << NOTEQUAL) | (1L << EQUAL))) != 0)) {
					{
					{
					setState(93);
					op();
					setState(94);
					addSub(0);
					}
					}
					setState(100);
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
		public List<ResetSubExpressionContext> resetSubExpression() {
			return getRuleContexts(ResetSubExpressionContext.class);
		}
		public ResetSubExpressionContext resetSubExpression(int i) {
			return getRuleContext(ResetSubExpressionContext.class,i);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public List<TerminalNode> AND() { return getTokens(HystExpressionParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(HystExpressionParser.AND, i);
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
		enterRule(_localctx, 10, RULE_resetExpression);
		int _la;
		try {
			setState(114);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new ResetBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(103);
				match(EOF);
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
				setState(104);
				resetSubExpression();
				setState(109);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AND) {
					{
					{
					setState(105);
					match(AND);
					setState(106);
					resetSubExpression();
					}
					}
					setState(111);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(112);
				match(EOF);
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
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
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
		enterRule(_localctx, 12, RULE_guardExpression);
		try {
			setState(120);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new GuardBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(116);
				match(EOF);
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
				setState(117);
				or();
				setState(118);
				match(EOF);
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
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
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
		enterRule(_localctx, 14, RULE_invariantExpression);
		try {
			setState(126);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new InvariantBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(122);
				match(EOF);
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
				setState(123);
				or();
				setState(124);
				match(EOF);
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
		public List<TerminalNode> VAR() { return getTokens(HystExpressionParser.VAR); }
		public TerminalNode VAR(int i) {
			return getToken(HystExpressionParser.VAR, i);
		}
		public List<TerminalNode> EQUAL() { return getTokens(HystExpressionParser.EQUAL); }
		public TerminalNode EQUAL(int i) {
			return getToken(HystExpressionParser.EQUAL, i);
		}
		public List<AddSubContext> addSub() {
			return getRuleContexts(AddSubContext.class);
		}
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
		public List<TerminalNode> TICK() { return getTokens(HystExpressionParser.TICK); }
		public TerminalNode TICK(int i) {
			return getToken(HystExpressionParser.TICK, i);
		}
		public List<TerminalNode> AND() { return getTokens(HystExpressionParser.AND); }
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
		enterRule(_localctx, 16, RULE_flowExpression);
		int _la;
		try {
			setState(150);
			switch (_input.LA(1)) {
			case EOF:
				_localctx = new FlowBlankContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(128);
				match(EOF);
				}
				break;
			case VAR:
				_localctx = new FlowContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(129);
				match(VAR);
				setState(131);
				_la = _input.LA(1);
				if (_la==TICK) {
					{
					setState(130);
					match(TICK);
					}
				}

				setState(133);
				match(EQUAL);
				setState(134);
				addSub(0);
				setState(144);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AND) {
					{
					{
					setState(135);
					match(AND);
					setState(136);
					match(VAR);
					setState(138);
					_la = _input.LA(1);
					if (_la==TICK) {
						{
						setState(137);
						match(TICK);
						}
					}

					setState(140);
					match(EQUAL);
					setState(141);
					addSub(0);
					}
					}
					setState(146);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(147);
				match(EOF);
				}
				break;
			case FALSE:
				_localctx = new FlowFalseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(149);
				match(FALSE);
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
		public List<TerminalNode> VAR() { return getTokens(HystExpressionParser.VAR); }
		public TerminalNode VAR(int i) {
			return getToken(HystExpressionParser.VAR, i);
		}
		public List<TerminalNode> DOT() { return getTokens(HystExpressionParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(HystExpressionParser.DOT, i);
		}
		public TerminalNode TICK() { return getToken(HystExpressionParser.TICK, 0); }
		public DotVarContext(DottedVarContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitDotVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DottedVarContext dottedVar() throws RecognitionException {
		DottedVarContext _localctx = new DottedVarContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_dottedVar);
		try {
			int _alt;
			_localctx = new DotVarContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			match(VAR);
			setState(157);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(153);
					match(DOT);
					setState(154);
					match(VAR);
					}
					} 
				}
				setState(159);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			setState(161);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(160);
				match(TICK);
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
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public TerminalNode EOF() { return getToken(HystExpressionParser.EOF, 0); }
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
		enterRule(_localctx, 20, RULE_locExpression);
		try {
			setState(167);
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
				setState(163);
				or();
				setState(164);
				match(EOF);
				}
				break;
			case EOF:
				_localctx = new LocFalseContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(166);
				match(EOF);
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
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public TerminalNode OR() { return getToken(HystExpressionParser.OR, 0); }
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
		enterRule(_localctx, 22, RULE_or);
		try {
			setState(174);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				_localctx = new OrExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(169);
				and();
				setState(170);
				match(OR);
				setState(171);
				or();
				}
				break;
			case 2:
				_localctx = new ToAndContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(173);
				and();
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
		public NotContext not() {
			return getRuleContext(NotContext.class,0);
		}
		public TerminalNode AND() { return getToken(HystExpressionParser.AND, 0); }
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
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
		enterRule(_localctx, 24, RULE_and);
		try {
			setState(181);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				_localctx = new AndExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(176);
				not();
				setState(177);
				match(AND);
				setState(178);
				and();
				}
				break;
			case 2:
				_localctx = new ToNotContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(180);
				not();
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
		public TerminalNode NOT() { return getToken(HystExpressionParser.NOT, 0); }
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public NotExpressionContext(NotContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNotExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BoolParenthesesContext extends NotContext {
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
		public BoolParenthesesContext(NotContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitBoolParentheses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotContext not() throws RecognitionException {
		NotContext _localctx = new NotContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_not);
		try {
			setState(193);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				_localctx = new NotExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(183);
				match(NOT);
				setState(184);
				match(LPAR);
				setState(185);
				or();
				setState(186);
				match(RPAR);
				}
				break;
			case 2:
				_localctx = new BoolParenthesesContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(188);
				match(LPAR);
				setState(189);
				or();
				setState(190);
				match(RPAR);
				}
				break;
			case 3:
				_localctx = new ToCompareContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(192);
				compare();
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
		enterRule(_localctx, 28, RULE_op);
		try {
			setState(201);
			switch (_input.LA(1)) {
			case EQUAL:
				_localctx = new EqualOpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(195);
				match(EQUAL);
				}
				break;
			case LESS:
				_localctx = new LessOpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(196);
				match(LESS);
				}
				break;
			case LESSEQUAL:
				_localctx = new LessEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(197);
				match(LESSEQUAL);
				}
				break;
			case GREATER:
				_localctx = new GreaterOpContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(198);
				match(GREATER);
				}
				break;
			case GREATEREQUAL:
				_localctx = new GreaterEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(199);
				match(GREATEREQUAL);
				}
				break;
			case NOTEQUAL:
				_localctx = new NotEqualOpContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(200);
				match(NOTEQUAL);
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
		public AddSubContext addSub(int i) {
			return getRuleContext(AddSubContext.class,i);
		}
		public List<OpContext> op() {
			return getRuleContexts(OpContext.class);
		}
		public OpContext op(int i) {
			return getRuleContext(OpContext.class,i);
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
		enterRule(_localctx, 30, RULE_compare);
		int _la;
		try {
			setState(213);
			switch (_input.LA(1)) {
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
			case MINUS:
				_localctx = new BoolOpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(203);
				addSub(0);
				setState(207); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(204);
					op();
					setState(205);
					addSub(0);
					}
					}
					setState(209); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << GREATER) | (1L << LESSEQUAL) | (1L << GREATEREQUAL) | (1L << NOTEQUAL) | (1L << EQUAL))) != 0) );
				}
				break;
			case TRUE:
				_localctx = new ConstTrueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(211);
				match(TRUE);
				}
				break;
			case FALSE:
				_localctx = new ConstFalseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(212);
				match(FALSE);
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
		int _startState = 32;
		enterRecursionRule(_localctx, 32, RULE_addSub, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToTimesDivContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(216);
			timesDiv(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(226);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(224);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
					case 1:
						{
						_localctx = new PlusContext(new AddSubContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_addSub);
						setState(218);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(219);
						match(PLUS);
						setState(220);
						timesDiv(0);
						}
						break;
					case 2:
						{
						_localctx = new MinusContext(new AddSubContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_addSub);
						setState(221);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(222);
						match(MINUS);
						setState(223);
						timesDiv(0);
						}
						break;
					}
					} 
				}
				setState(228);
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
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public TerminalNode TIMES() { return getToken(HystExpressionParser.TIMES, 0); }
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public MultiplicationContext(TimesDivContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitMultiplication(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DivisionContext extends TimesDivContext {
		public TimesDivContext timesDiv() {
			return getRuleContext(TimesDivContext.class,0);
		}
		public TerminalNode DIV() { return getToken(HystExpressionParser.DIV, 0); }
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
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
		int _startState = 34;
		enterRecursionRule(_localctx, 34, RULE_timesDiv, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToPowContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(230);
			pow(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(240);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(238);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicationContext(new TimesDivContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_timesDiv);
						setState(232);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(233);
						match(TIMES);
						setState(234);
						pow(0);
						}
						break;
					case 2:
						{
						_localctx = new DivisionContext(new TimesDivContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_timesDiv);
						setState(235);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(236);
						match(DIV);
						setState(237);
						pow(0);
						}
						break;
					}
					} 
				}
				setState(242);
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
		public PowContext pow() {
			return getRuleContext(PowContext.class,0);
		}
		public TerminalNode POW() { return getToken(HystExpressionParser.POW, 0); }
		public NegativeUnaryContext negativeUnary() {
			return getRuleContext(NegativeUnaryContext.class,0);
		}
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
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_pow, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ToNegativeUnaryContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(244);
			negativeUnary();
			}
			_ctx.stop = _input.LT(-1);
			setState(251);
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
					setState(246);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(247);
					match(POW);
					setState(248);
					negativeUnary();
					}
					} 
				}
				setState(253);
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
		public TerminalNode MINUS() { return getToken(HystExpressionParser.MINUS, 0); }
		public NegativeUnaryContext negativeUnary() {
			return getRuleContext(NegativeUnaryContext.class,0);
		}
		public NegativeContext(NegativeUnaryContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof HystExpressionVisitor ) return ((HystExpressionVisitor<? extends T>)visitor).visitNegative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NegativeUnaryContext negativeUnary() throws RecognitionException {
		NegativeUnaryContext _localctx = new NegativeUnaryContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_negativeUnary);
		try {
			setState(257);
			switch (_input.LA(1)) {
			case MINUS:
				_localctx = new NegativeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(254);
				match(MINUS);
				setState(255);
				negativeUnary();
				}
				break;
			case NUM:
			case VAR:
			case LPAR:
			case LBRAC:
				_localctx = new ToUnaryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(256);
				unary();
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
		public TerminalNode LPAR() { return getToken(HystExpressionParser.LPAR, 0); }
		public AddSubContext addSub() {
			return getRuleContext(AddSubContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(HystExpressionParser.RPAR, 0); }
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
		enterRule(_localctx, 40, RULE_unary);
		try {
			setState(267);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				_localctx = new MatrixExpContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
				matrixExpression();
				}
				break;
			case 2:
				_localctx = new FuncExpContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(260);
				functionExpression();
				}
				break;
			case 3:
				_localctx = new NumberContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(261);
				match(NUM);
				}
				break;
			case 4:
				_localctx = new DottedVariableContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(262);
				dottedVar();
				}
				break;
			case 5:
				_localctx = new ParenthesesContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(263);
				match(LPAR);
				setState(264);
				addSub(0);
				setState(265);
				match(RPAR);
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
		case 16:
			return addSub_sempred((AddSubContext)_localctx, predIndex);
		case 17:
			return timesDiv_sempred((TimesDivContext)_localctx, predIndex);
		case 18:
			return pow_sempred((PowContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean addSub_sempred(AddSubContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean timesDiv_sempred(TimesDivContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean pow_sempred(PowContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\37\u0110\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\3\2\3\2\3\2\7\2\60\n\2\f\2\16"+
		"\2\63\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\7\4?\n\4\f\4\16\4B"+
		"\13\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4J\n\4\3\5\3\5\3\5\3\5\3\5\7\5Q\n\5\f"+
		"\5\16\5T\13\5\5\5V\n\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6"+
		"c\n\6\f\6\16\6f\13\6\5\6h\n\6\3\7\3\7\3\7\3\7\7\7n\n\7\f\7\16\7q\13\7"+
		"\3\7\3\7\5\7u\n\7\3\b\3\b\3\b\3\b\5\b{\n\b\3\t\3\t\3\t\3\t\5\t\u0081\n"+
		"\t\3\n\3\n\3\n\5\n\u0086\n\n\3\n\3\n\3\n\3\n\3\n\5\n\u008d\n\n\3\n\3\n"+
		"\7\n\u0091\n\n\f\n\16\n\u0094\13\n\3\n\3\n\3\n\5\n\u0099\n\n\3\13\3\13"+
		"\3\13\7\13\u009e\n\13\f\13\16\13\u00a1\13\13\3\13\5\13\u00a4\n\13\3\f"+
		"\3\f\3\f\3\f\5\f\u00aa\n\f\3\r\3\r\3\r\3\r\3\r\5\r\u00b1\n\r\3\16\3\16"+
		"\3\16\3\16\3\16\5\16\u00b8\n\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\5\17\u00c4\n\17\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u00cc\n"+
		"\20\3\21\3\21\3\21\3\21\6\21\u00d2\n\21\r\21\16\21\u00d3\3\21\3\21\5\21"+
		"\u00d8\n\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\7\22\u00e3\n"+
		"\22\f\22\16\22\u00e6\13\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\7\23\u00f1\n\23\f\23\16\23\u00f4\13\23\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\7\24\u00fc\n\24\f\24\16\24\u00ff\13\24\3\25\3\25\3\25\5\25\u0104\n\25"+
		"\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u010e\n\26\3\26\2\5\"$&"+
		"\27\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*\2\2\u0123\2,\3\2\2\2"+
		"\4\64\3\2\2\2\6I\3\2\2\2\bK\3\2\2\2\ng\3\2\2\2\ft\3\2\2\2\16z\3\2\2\2"+
		"\20\u0080\3\2\2\2\22\u0098\3\2\2\2\24\u009a\3\2\2\2\26\u00a9\3\2\2\2\30"+
		"\u00b0\3\2\2\2\32\u00b7\3\2\2\2\34\u00c3\3\2\2\2\36\u00cb\3\2\2\2 \u00d7"+
		"\3\2\2\2\"\u00d9\3\2\2\2$\u00e7\3\2\2\2&\u00f5\3\2\2\2(\u0103\3\2\2\2"+
		"*\u010d\3\2\2\2,\61\5\"\22\2-.\7\r\2\2.\60\5\"\22\2/-\3\2\2\2\60\63\3"+
		"\2\2\2\61/\3\2\2\2\61\62\3\2\2\2\62\3\3\2\2\2\63\61\3\2\2\2\64\65\5\""+
		"\22\2\65\66\7\17\2\2\66\67\5\"\22\2\678\7\17\2\289\5\"\22\29\5\3\2\2\2"+
		":;\7\13\2\2;@\5\2\2\2<=\7\16\2\2=?\5\2\2\2><\3\2\2\2?B\3\2\2\2@>\3\2\2"+
		"\2@A\3\2\2\2AC\3\2\2\2B@\3\2\2\2CD\7\f\2\2DJ\3\2\2\2EF\7\13\2\2FG\5\4"+
		"\3\2GH\7\f\2\2HJ\3\2\2\2I:\3\2\2\2IE\3\2\2\2J\7\3\2\2\2KL\7\b\2\2LU\7"+
		"\t\2\2MR\5\"\22\2NO\7\r\2\2OQ\5\"\22\2PN\3\2\2\2QT\3\2\2\2RP\3\2\2\2R"+
		"S\3\2\2\2SV\3\2\2\2TR\3\2\2\2UM\3\2\2\2UV\3\2\2\2VW\3\2\2\2WX\7\n\2\2"+
		"X\t\3\2\2\2YZ\7\b\2\2Z[\7\37\2\2[h\5\"\22\2\\]\5\"\22\2]^\5\36\20\2^d"+
		"\5\"\22\2_`\5\36\20\2`a\5\"\22\2ac\3\2\2\2b_\3\2\2\2cf\3\2\2\2db\3\2\2"+
		"\2de\3\2\2\2eh\3\2\2\2fd\3\2\2\2gY\3\2\2\2g\\\3\2\2\2h\13\3\2\2\2iu\7"+
		"\2\2\3jo\5\n\6\2kl\7\26\2\2ln\5\n\6\2mk\3\2\2\2nq\3\2\2\2om\3\2\2\2op"+
		"\3\2\2\2pr\3\2\2\2qo\3\2\2\2rs\7\2\2\3su\3\2\2\2ti\3\2\2\2tj\3\2\2\2u"+
		"\r\3\2\2\2v{\7\2\2\3wx\5\30\r\2xy\7\2\2\3y{\3\2\2\2zv\3\2\2\2zw\3\2\2"+
		"\2{\17\3\2\2\2|\u0081\7\2\2\3}~\5\30\r\2~\177\7\2\2\3\177\u0081\3\2\2"+
		"\2\u0080|\3\2\2\2\u0080}\3\2\2\2\u0081\21\3\2\2\2\u0082\u0099\7\2\2\3"+
		"\u0083\u0085\7\b\2\2\u0084\u0086\7\4\2\2\u0085\u0084\3\2\2\2\u0085\u0086"+
		"\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0088\7\36\2\2\u0088\u0092\5\"\22\2"+
		"\u0089\u008a\7\26\2\2\u008a\u008c\7\b\2\2\u008b\u008d\7\4\2\2\u008c\u008b"+
		"\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f\7\36\2\2"+
		"\u008f\u0091\5\"\22\2\u0090\u0089\3\2\2\2\u0091\u0094\3\2\2\2\u0092\u0090"+
		"\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0095\3\2\2\2\u0094\u0092\3\2\2\2\u0095"+
		"\u0096\7\2\2\3\u0096\u0099\3\2\2\2\u0097\u0099\7\6\2\2\u0098\u0082\3\2"+
		"\2\2\u0098\u0083\3\2\2\2\u0098\u0097\3\2\2\2\u0099\23\3\2\2\2\u009a\u009f"+
		"\7\b\2\2\u009b\u009c\7\25\2\2\u009c\u009e\7\b\2\2\u009d\u009b\3\2\2\2"+
		"\u009e\u00a1\3\2\2\2\u009f\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a3"+
		"\3\2\2\2\u00a1\u009f\3\2\2\2\u00a2\u00a4\7\4\2\2\u00a3\u00a2\3\2\2\2\u00a3"+
		"\u00a4\3\2\2\2\u00a4\25\3\2\2\2\u00a5\u00a6\5\30\r\2\u00a6\u00a7\7\2\2"+
		"\3\u00a7\u00aa\3\2\2\2\u00a8\u00aa\7\2\2\3\u00a9\u00a5\3\2\2\2\u00a9\u00a8"+
		"\3\2\2\2\u00aa\27\3\2\2\2\u00ab\u00ac\5\32\16\2\u00ac\u00ad\7\27\2\2\u00ad"+
		"\u00ae\5\30\r\2\u00ae\u00b1\3\2\2\2\u00af\u00b1\5\32\16\2\u00b0\u00ab"+
		"\3\2\2\2\u00b0\u00af\3\2\2\2\u00b1\31\3\2\2\2\u00b2\u00b3\5\34\17\2\u00b3"+
		"\u00b4\7\26\2\2\u00b4\u00b5\5\32\16\2\u00b5\u00b8\3\2\2\2\u00b6\u00b8"+
		"\5\34\17\2\u00b7\u00b2\3\2\2\2\u00b7\u00b6\3\2\2\2\u00b8\33\3\2\2\2\u00b9"+
		"\u00ba\7\30\2\2\u00ba\u00bb\7\t\2\2\u00bb\u00bc\5\30\r\2\u00bc\u00bd\7"+
		"\n\2\2\u00bd\u00c4\3\2\2\2\u00be\u00bf\7\t\2\2\u00bf\u00c0\5\30\r\2\u00c0"+
		"\u00c1\7\n\2\2\u00c1\u00c4\3\2\2\2\u00c2\u00c4\5 \21\2\u00c3\u00b9\3\2"+
		"\2\2\u00c3\u00be\3\2\2\2\u00c3\u00c2\3\2\2\2\u00c4\35\3\2\2\2\u00c5\u00cc"+
		"\7\36\2\2\u00c6\u00cc\7\31\2\2\u00c7\u00cc\7\33\2\2\u00c8\u00cc\7\32\2"+
		"\2\u00c9\u00cc\7\34\2\2\u00ca\u00cc\7\35\2\2\u00cb\u00c5\3\2\2\2\u00cb"+
		"\u00c6\3\2\2\2\u00cb\u00c7\3\2\2\2\u00cb\u00c8\3\2\2\2\u00cb\u00c9\3\2"+
		"\2\2\u00cb\u00ca\3\2\2\2\u00cc\37\3\2\2\2\u00cd\u00d1\5\"\22\2\u00ce\u00cf"+
		"\5\36\20\2\u00cf\u00d0\5\"\22\2\u00d0\u00d2\3\2\2\2\u00d1\u00ce\3\2\2"+
		"\2\u00d2\u00d3\3\2\2\2\u00d3\u00d1\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00d8"+
		"\3\2\2\2\u00d5\u00d8\7\5\2\2\u00d6\u00d8\7\6\2\2\u00d7\u00cd\3\2\2\2\u00d7"+
		"\u00d5\3\2\2\2\u00d7\u00d6\3\2\2\2\u00d8!\3\2\2\2\u00d9\u00da\b\22\1\2"+
		"\u00da\u00db\5$\23\2\u00db\u00e4\3\2\2\2\u00dc\u00dd\f\5\2\2\u00dd\u00de"+
		"\7\20\2\2\u00de\u00e3\5$\23\2\u00df\u00e0\f\4\2\2\u00e0\u00e1\7\21\2\2"+
		"\u00e1\u00e3\5$\23\2\u00e2\u00dc\3\2\2\2\u00e2\u00df\3\2\2\2\u00e3\u00e6"+
		"\3\2\2\2\u00e4\u00e2\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5#\3\2\2\2\u00e6"+
		"\u00e4\3\2\2\2\u00e7\u00e8\b\23\1\2\u00e8\u00e9\5&\24\2\u00e9\u00f2\3"+
		"\2\2\2\u00ea\u00eb\f\5\2\2\u00eb\u00ec\7\22\2\2\u00ec\u00f1\5&\24\2\u00ed"+
		"\u00ee\f\4\2\2\u00ee\u00ef\7\23\2\2\u00ef\u00f1\5&\24\2\u00f0\u00ea\3"+
		"\2\2\2\u00f0\u00ed\3\2\2\2\u00f1\u00f4\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2"+
		"\u00f3\3\2\2\2\u00f3%\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f5\u00f6\b\24\1\2"+
		"\u00f6\u00f7\5(\25\2\u00f7\u00fd\3\2\2\2\u00f8\u00f9\f\4\2\2\u00f9\u00fa"+
		"\7\24\2\2\u00fa\u00fc\5(\25\2\u00fb\u00f8\3\2\2\2\u00fc\u00ff\3\2\2\2"+
		"\u00fd\u00fb\3\2\2\2\u00fd\u00fe\3\2\2\2\u00fe\'\3\2\2\2\u00ff\u00fd\3"+
		"\2\2\2\u0100\u0101\7\21\2\2\u0101\u0104\5(\25\2\u0102\u0104\5*\26\2\u0103"+
		"\u0100\3\2\2\2\u0103\u0102\3\2\2\2\u0104)\3\2\2\2\u0105\u010e\5\6\4\2"+
		"\u0106\u010e\5\b\5\2\u0107\u010e\7\7\2\2\u0108\u010e\5\24\13\2\u0109\u010a"+
		"\7\t\2\2\u010a\u010b\5\"\22\2\u010b\u010c\7\n\2\2\u010c\u010e\3\2\2\2"+
		"\u010d\u0105\3\2\2\2\u010d\u0106\3\2\2\2\u010d\u0107\3\2\2\2\u010d\u0108"+
		"\3\2\2\2\u010d\u0109\3\2\2\2\u010e+\3\2\2\2!\61@IRUdgotz\u0080\u0085\u008c"+
		"\u0092\u0098\u009f\u00a3\u00a9\u00b0\u00b7\u00c3\u00cb\u00d3\u00d7\u00e2"+
		"\u00e4\u00f0\u00f2\u00fd\u0103\u010d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
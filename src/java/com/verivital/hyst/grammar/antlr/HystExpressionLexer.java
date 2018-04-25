// Generated from HystExpression.g4 by ANTLR 4.5
package com.verivital.hyst.grammar.antlr;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HystExpressionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, TICK=2, TRUE=3, FALSE=4, NUM=5, VAR=6, LPAR=7, RPAR=8, LBRAC=9, 
		RBRAC=10, COMMA=11, SEMICOLON=12, COLON=13, PLUS=14, MINUS=15, TIMES=16, 
		DIV=17, POW=18, DOT=19, AND=20, OR=21, NOT=22, LESS=23, GREATER=24, LESSEQUAL=25, 
		GREATEREQUAL=26, NOTEQUAL=27, EQUAL=28, EQUAL_RESET=29;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"WS", "TICK", "TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", 
		"RBRAC", "COMMA", "SEMICOLON", "COLON", "PLUS", "MINUS", "TIMES", "DIV", 
		"POW", "DOT", "AND", "OR", "NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", 
		"NOTEQUAL", "EQUAL", "EQUAL_RESET"
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


	public HystExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "HystExpression.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\37\u00b1\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\3\2\6\2?\n\2\r"+
		"\2\16\2@\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\6\6\6S\n\6\r\6\16\6T\3\6\3\6\6\6Y\n\6\r\6\16\6Z\5\6]\n\6\3\6\3\6\6\6"+
		"a\n\6\r\6\16\6b\5\6e\n\6\3\6\3\6\5\6i\n\6\3\6\6\6l\n\6\r\6\16\6m\5\6p"+
		"\n\6\3\7\3\7\7\7t\n\7\f\7\16\7w\13\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13"+
		"\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23"+
		"\3\23\3\24\3\24\3\25\3\25\5\25\u0095\n\25\3\26\3\26\5\26\u0099\n\26\3"+
		"\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3"+
		"\34\3\35\3\35\3\35\5\35\u00ad\n\35\3\36\3\36\3\36\2\2\37\3\3\5\4\7\5\t"+
		"\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23"+
		"%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37\3\2\n\5\2\13"+
		"\f\17\17\"\"\3\2\62;\4\2GGgg\5\2--//~~\5\2C\\aac|\6\2\62;C\\aac|\3\2("+
		")\4\2))~~\u00bd\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3"+
		"\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2"+
		"\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3"+
		"\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2"+
		"\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\2"+
		"9\3\2\2\2\2;\3\2\2\2\3>\3\2\2\2\5D\3\2\2\2\7F\3\2\2\2\tK\3\2\2\2\13d\3"+
		"\2\2\2\rq\3\2\2\2\17x\3\2\2\2\21z\3\2\2\2\23|\3\2\2\2\25~\3\2\2\2\27\u0080"+
		"\3\2\2\2\31\u0082\3\2\2\2\33\u0084\3\2\2\2\35\u0086\3\2\2\2\37\u0088\3"+
		"\2\2\2!\u008a\3\2\2\2#\u008c\3\2\2\2%\u008e\3\2\2\2\'\u0090\3\2\2\2)\u0092"+
		"\3\2\2\2+\u0096\3\2\2\2-\u009a\3\2\2\2/\u009c\3\2\2\2\61\u009e\3\2\2\2"+
		"\63\u00a0\3\2\2\2\65\u00a3\3\2\2\2\67\u00a6\3\2\2\29\u00ac\3\2\2\2;\u00ae"+
		"\3\2\2\2=?\t\2\2\2>=\3\2\2\2?@\3\2\2\2@>\3\2\2\2@A\3\2\2\2AB\3\2\2\2B"+
		"C\b\2\2\2C\4\3\2\2\2DE\7)\2\2E\6\3\2\2\2FG\7v\2\2GH\7t\2\2HI\7w\2\2IJ"+
		"\7g\2\2J\b\3\2\2\2KL\7h\2\2LM\7c\2\2MN\7n\2\2NO\7u\2\2OP\7g\2\2P\n\3\2"+
		"\2\2QS\t\3\2\2RQ\3\2\2\2ST\3\2\2\2TR\3\2\2\2TU\3\2\2\2U\\\3\2\2\2VX\7"+
		"\60\2\2WY\t\3\2\2XW\3\2\2\2YZ\3\2\2\2ZX\3\2\2\2Z[\3\2\2\2[]\3\2\2\2\\"+
		"V\3\2\2\2\\]\3\2\2\2]e\3\2\2\2^`\7\60\2\2_a\t\3\2\2`_\3\2\2\2ab\3\2\2"+
		"\2b`\3\2\2\2bc\3\2\2\2ce\3\2\2\2dR\3\2\2\2d^\3\2\2\2eo\3\2\2\2fh\t\4\2"+
		"\2gi\t\5\2\2hg\3\2\2\2hi\3\2\2\2ik\3\2\2\2jl\t\3\2\2kj\3\2\2\2lm\3\2\2"+
		"\2mk\3\2\2\2mn\3\2\2\2np\3\2\2\2of\3\2\2\2op\3\2\2\2p\f\3\2\2\2qu\t\6"+
		"\2\2rt\t\7\2\2sr\3\2\2\2tw\3\2\2\2us\3\2\2\2uv\3\2\2\2v\16\3\2\2\2wu\3"+
		"\2\2\2xy\7*\2\2y\20\3\2\2\2z{\7+\2\2{\22\3\2\2\2|}\7]\2\2}\24\3\2\2\2"+
		"~\177\7_\2\2\177\26\3\2\2\2\u0080\u0081\7.\2\2\u0081\30\3\2\2\2\u0082"+
		"\u0083\7=\2\2\u0083\32\3\2\2\2\u0084\u0085\7<\2\2\u0085\34\3\2\2\2\u0086"+
		"\u0087\7-\2\2\u0087\36\3\2\2\2\u0088\u0089\7/\2\2\u0089 \3\2\2\2\u008a"+
		"\u008b\7,\2\2\u008b\"\3\2\2\2\u008c\u008d\7\61\2\2\u008d$\3\2\2\2\u008e"+
		"\u008f\7`\2\2\u008f&\3\2\2\2\u0090\u0091\7\60\2\2\u0091(\3\2\2\2\u0092"+
		"\u0094\7(\2\2\u0093\u0095\t\b\2\2\u0094\u0093\3\2\2\2\u0094\u0095\3\2"+
		"\2\2\u0095*\3\2\2\2\u0096\u0098\7~\2\2\u0097\u0099\t\t\2\2\u0098\u0097"+
		"\3\2\2\2\u0098\u0099\3\2\2\2\u0099,\3\2\2\2\u009a\u009b\7#\2\2\u009b."+
		"\3\2\2\2\u009c\u009d\7>\2\2\u009d\60\3\2\2\2\u009e\u009f\7@\2\2\u009f"+
		"\62\3\2\2\2\u00a0\u00a1\7>\2\2\u00a1\u00a2\7?\2\2\u00a2\64\3\2\2\2\u00a3"+
		"\u00a4\7@\2\2\u00a4\u00a5\7?\2\2\u00a5\66\3\2\2\2\u00a6\u00a7\7#\2\2\u00a7"+
		"\u00a8\7?\2\2\u00a88\3\2\2\2\u00a9\u00aa\7?\2\2\u00aa\u00ad\7?\2\2\u00ab"+
		"\u00ad\7?\2\2\u00ac\u00a9\3\2\2\2\u00ac\u00ab\3\2\2\2\u00ad:\3\2\2\2\u00ae"+
		"\u00af\7<\2\2\u00af\u00b0\7?\2\2\u00b0<\3\2\2\2\20\2@TZ\\bdhmou\u0094"+
		"\u0098\u00ac\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
// Generated from java/com/verivital/hyst/grammar/antlr/HystExpression.g4 by ANTLR 4.5.1
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
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, TICK=2, SIN=3, COS=4, TAN=5, EXP=6, SQRT=7, LN=8, LUT=9, LOC=10, 
		TRUE=11, FALSE=12, NUM=13, VAR=14, LPAR=15, RPAR=16, LBRAC=17, RBRAC=18, 
		COMMA=19, SEMICOLON=20, PLUS=21, MINUS=22, TIMES=23, DIV=24, POW=25, DOT=26, 
		AND=27, OR=28, NOT=29, LESS=30, GREATER=31, LESSEQUAL=32, GREATEREQUAL=33, 
		NOTEQUAL=34, EQUAL=35, EQUAL_RESET=36;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"WS", "TICK", "SIN", "COS", "TAN", "EXP", "SQRT", "LN", "LUT", "LOC", 
		"TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", "RBRAC", "COMMA", 
		"SEMICOLON", "PLUS", "MINUS", "TIMES", "DIV", "POW", "DOT", "AND", "OR", 
		"NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", "NOTEQUAL", "EQUAL", 
		"EQUAL_RESET"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'''", "'sin'", "'cos'", "'tan'", "'exp'", "'sqrt'", "'ln'", 
		"'lut'", "'loc'", "'true'", "'false'", null, null, "'('", "')'", "'['", 
		"']'", "','", "';'", "'+'", "'-'", "'*'", "'/'", "'^'", "'.'", null, null, 
		"'!'", "'<'", "'>'", "'<='", "'>='", "'!='", null, "':='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "TICK", "SIN", "COS", "TAN", "EXP", "SQRT", "LN", "LUT", "LOC", 
		"TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", "RBRAC", "COMMA", 
		"SEMICOLON", "PLUS", "MINUS", "TIMES", "DIV", "POW", "DOT", "AND", "OR", 
		"NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", "NOTEQUAL", "EQUAL", 
		"EQUAL_RESET"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2&\u00dd\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\3\2\6\2M\n\2\r\2\16\2N\3\2\3\2\3\3\3\3\3"+
		"\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b"+
		"\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3\f\3"+
		"\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\16\6\16\u0081\n\16\r\16\16\16\u0082"+
		"\3\16\3\16\6\16\u0087\n\16\r\16\16\16\u0088\5\16\u008b\n\16\3\16\3\16"+
		"\6\16\u008f\n\16\r\16\16\16\u0090\5\16\u0093\n\16\3\16\3\16\5\16\u0097"+
		"\n\16\3\16\6\16\u009a\n\16\r\16\16\16\u009b\5\16\u009e\n\16\3\17\3\17"+
		"\7\17\u00a2\n\17\f\17\16\17\u00a5\13\17\3\20\3\20\3\21\3\21\3\22\3\22"+
		"\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31"+
		"\3\32\3\32\3\33\3\33\3\34\3\34\5\34\u00c1\n\34\3\35\3\35\5\35\u00c5\n"+
		"\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3$\3$\3$\5"+
		"$\u00d9\n$\3%\3%\3%\2\2&\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f"+
		"\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63"+
		"\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&\3\2\n\5\2\13\f\17\17\"\"\3\2\62"+
		";\4\2GGgg\5\2--//~~\5\2C\\aac|\6\2\62;C\\aac|\3\2()\4\2))~~\u00e9\2\3"+
		"\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2"+
		"\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31"+
		"\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2"+
		"\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2"+
		"\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2"+
		"\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2"+
		"I\3\2\2\2\3L\3\2\2\2\5R\3\2\2\2\7T\3\2\2\2\tX\3\2\2\2\13\\\3\2\2\2\r`"+
		"\3\2\2\2\17d\3\2\2\2\21i\3\2\2\2\23l\3\2\2\2\25p\3\2\2\2\27t\3\2\2\2\31"+
		"y\3\2\2\2\33\u0092\3\2\2\2\35\u009f\3\2\2\2\37\u00a6\3\2\2\2!\u00a8\3"+
		"\2\2\2#\u00aa\3\2\2\2%\u00ac\3\2\2\2\'\u00ae\3\2\2\2)\u00b0\3\2\2\2+\u00b2"+
		"\3\2\2\2-\u00b4\3\2\2\2/\u00b6\3\2\2\2\61\u00b8\3\2\2\2\63\u00ba\3\2\2"+
		"\2\65\u00bc\3\2\2\2\67\u00be\3\2\2\29\u00c2\3\2\2\2;\u00c6\3\2\2\2=\u00c8"+
		"\3\2\2\2?\u00ca\3\2\2\2A\u00cc\3\2\2\2C\u00cf\3\2\2\2E\u00d2\3\2\2\2G"+
		"\u00d8\3\2\2\2I\u00da\3\2\2\2KM\t\2\2\2LK\3\2\2\2MN\3\2\2\2NL\3\2\2\2"+
		"NO\3\2\2\2OP\3\2\2\2PQ\b\2\2\2Q\4\3\2\2\2RS\7)\2\2S\6\3\2\2\2TU\7u\2\2"+
		"UV\7k\2\2VW\7p\2\2W\b\3\2\2\2XY\7e\2\2YZ\7q\2\2Z[\7u\2\2[\n\3\2\2\2\\"+
		"]\7v\2\2]^\7c\2\2^_\7p\2\2_\f\3\2\2\2`a\7g\2\2ab\7z\2\2bc\7r\2\2c\16\3"+
		"\2\2\2de\7u\2\2ef\7s\2\2fg\7t\2\2gh\7v\2\2h\20\3\2\2\2ij\7n\2\2jk\7p\2"+
		"\2k\22\3\2\2\2lm\7n\2\2mn\7w\2\2no\7v\2\2o\24\3\2\2\2pq\7n\2\2qr\7q\2"+
		"\2rs\7e\2\2s\26\3\2\2\2tu\7v\2\2uv\7t\2\2vw\7w\2\2wx\7g\2\2x\30\3\2\2"+
		"\2yz\7h\2\2z{\7c\2\2{|\7n\2\2|}\7u\2\2}~\7g\2\2~\32\3\2\2\2\177\u0081"+
		"\t\3\2\2\u0080\177\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0080\3\2\2\2\u0082"+
		"\u0083\3\2\2\2\u0083\u008a\3\2\2\2\u0084\u0086\7\60\2\2\u0085\u0087\t"+
		"\3\2\2\u0086\u0085\3\2\2\2\u0087\u0088\3\2\2\2\u0088\u0086\3\2\2\2\u0088"+
		"\u0089\3\2\2\2\u0089\u008b\3\2\2\2\u008a\u0084\3\2\2\2\u008a\u008b\3\2"+
		"\2\2\u008b\u0093\3\2\2\2\u008c\u008e\7\60\2\2\u008d\u008f\t\3\2\2\u008e"+
		"\u008d\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2"+
		"\2\2\u0091\u0093\3\2\2\2\u0092\u0080\3\2\2\2\u0092\u008c\3\2\2\2\u0093"+
		"\u009d\3\2\2\2\u0094\u0096\t\4\2\2\u0095\u0097\t\5\2\2\u0096\u0095\3\2"+
		"\2\2\u0096\u0097\3\2\2\2\u0097\u0099\3\2\2\2\u0098\u009a\t\3\2\2\u0099"+
		"\u0098\3\2\2\2\u009a\u009b\3\2\2\2\u009b\u0099\3\2\2\2\u009b\u009c\3\2"+
		"\2\2\u009c\u009e\3\2\2\2\u009d\u0094\3\2\2\2\u009d\u009e\3\2\2\2\u009e"+
		"\34\3\2\2\2\u009f\u00a3\t\6\2\2\u00a0\u00a2\t\7\2\2\u00a1\u00a0\3\2\2"+
		"\2\u00a2\u00a5\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\36"+
		"\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a6\u00a7\7*\2\2\u00a7 \3\2\2\2\u00a8\u00a9"+
		"\7+\2\2\u00a9\"\3\2\2\2\u00aa\u00ab\7]\2\2\u00ab$\3\2\2\2\u00ac\u00ad"+
		"\7_\2\2\u00ad&\3\2\2\2\u00ae\u00af\7.\2\2\u00af(\3\2\2\2\u00b0\u00b1\7"+
		"=\2\2\u00b1*\3\2\2\2\u00b2\u00b3\7-\2\2\u00b3,\3\2\2\2\u00b4\u00b5\7/"+
		"\2\2\u00b5.\3\2\2\2\u00b6\u00b7\7,\2\2\u00b7\60\3\2\2\2\u00b8\u00b9\7"+
		"\61\2\2\u00b9\62\3\2\2\2\u00ba\u00bb\7`\2\2\u00bb\64\3\2\2\2\u00bc\u00bd"+
		"\7\60\2\2\u00bd\66\3\2\2\2\u00be\u00c0\7(\2\2\u00bf\u00c1\t\b\2\2\u00c0"+
		"\u00bf\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c18\3\2\2\2\u00c2\u00c4\7~\2\2\u00c3"+
		"\u00c5\t\t\2\2\u00c4\u00c3\3\2\2\2\u00c4\u00c5\3\2\2\2\u00c5:\3\2\2\2"+
		"\u00c6\u00c7\7#\2\2\u00c7<\3\2\2\2\u00c8\u00c9\7>\2\2\u00c9>\3\2\2\2\u00ca"+
		"\u00cb\7@\2\2\u00cb@\3\2\2\2\u00cc\u00cd\7>\2\2\u00cd\u00ce\7?\2\2\u00ce"+
		"B\3\2\2\2\u00cf\u00d0\7@\2\2\u00d0\u00d1\7?\2\2\u00d1D\3\2\2\2\u00d2\u00d3"+
		"\7#\2\2\u00d3\u00d4\7?\2\2\u00d4F\3\2\2\2\u00d5\u00d6\7?\2\2\u00d6\u00d9"+
		"\7?\2\2\u00d7\u00d9\7?\2\2\u00d8\u00d5\3\2\2\2\u00d8\u00d7\3\2\2\2\u00d9"+
		"H\3\2\2\2\u00da\u00db\7<\2\2\u00db\u00dc\7?\2\2\u00dcJ\3\2\2\2\20\2N\u0082"+
		"\u0088\u008a\u0090\u0092\u0096\u009b\u009d\u00a3\u00c0\u00c4\u00d8\3\b"+
		"\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
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
		RBRAC=10, COMMA=11, SEMICOLON=12, PLUS=13, MINUS=14, TIMES=15, DIV=16, 
		POW=17, DOT=18, AND=19, OR=20, NOT=21, LESS=22, GREATER=23, LESSEQUAL=24, 
		GREATEREQUAL=25, NOTEQUAL=26, EQUAL=27, EQUAL_RESET=28;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"WS", "TICK", "TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", 
		"RBRAC", "COMMA", "SEMICOLON", "PLUS", "MINUS", "TIMES", "DIV", "POW", 
		"DOT", "AND", "OR", "NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", 
		"NOTEQUAL", "EQUAL", "EQUAL_RESET"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'''", "'true'", "'false'", null, null, "'('", "')'", "'['", 
		"']'", "','", "';'", "'+'", "'-'", "'*'", "'/'", "'^'", "'.'", null, null, 
		"'!'", "'<'", "'>'", "'<='", "'>='", "'!='", null, "':='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "TICK", "TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "LBRAC", 
		"RBRAC", "COMMA", "SEMICOLON", "PLUS", "MINUS", "TIMES", "DIV", "POW", 
		"DOT", "AND", "OR", "NOT", "LESS", "GREATER", "LESSEQUAL", "GREATEREQUAL", 
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\36\u00ad\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\6\2=\n\2\r\2\16\2>\3"+
		"\2\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\6\6\6Q\n"+
		"\6\r\6\16\6R\3\6\3\6\6\6W\n\6\r\6\16\6X\5\6[\n\6\3\6\3\6\6\6_\n\6\r\6"+
		"\16\6`\5\6c\n\6\3\6\3\6\5\6g\n\6\3\6\6\6j\n\6\r\6\16\6k\5\6n\n\6\3\7\3"+
		"\7\7\7r\n\7\f\7\16\7u\13\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3"+
		"\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24"+
		"\3\24\5\24\u0091\n\24\3\25\3\25\5\25\u0095\n\25\3\26\3\26\3\27\3\27\3"+
		"\30\3\30\3\31\3\31\3\31\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\34\5"+
		"\34\u00a9\n\34\3\35\3\35\3\35\2\2\36\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21"+
		"\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30"+
		"/\31\61\32\63\33\65\34\67\359\36\3\2\n\5\2\13\f\17\17\"\"\3\2\62;\4\2"+
		"GGgg\5\2--//~~\5\2C\\aac|\6\2\62;C\\aac|\3\2()\4\2))~~\u00b9\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3"+
		"\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2"+
		"\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2"+
		"\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2"+
		"\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\3<\3\2\2\2\5B\3"+
		"\2\2\2\7D\3\2\2\2\tI\3\2\2\2\13b\3\2\2\2\ro\3\2\2\2\17v\3\2\2\2\21x\3"+
		"\2\2\2\23z\3\2\2\2\25|\3\2\2\2\27~\3\2\2\2\31\u0080\3\2\2\2\33\u0082\3"+
		"\2\2\2\35\u0084\3\2\2\2\37\u0086\3\2\2\2!\u0088\3\2\2\2#\u008a\3\2\2\2"+
		"%\u008c\3\2\2\2\'\u008e\3\2\2\2)\u0092\3\2\2\2+\u0096\3\2\2\2-\u0098\3"+
		"\2\2\2/\u009a\3\2\2\2\61\u009c\3\2\2\2\63\u009f\3\2\2\2\65\u00a2\3\2\2"+
		"\2\67\u00a8\3\2\2\29\u00aa\3\2\2\2;=\t\2\2\2<;\3\2\2\2=>\3\2\2\2><\3\2"+
		"\2\2>?\3\2\2\2?@\3\2\2\2@A\b\2\2\2A\4\3\2\2\2BC\7)\2\2C\6\3\2\2\2DE\7"+
		"v\2\2EF\7t\2\2FG\7w\2\2GH\7g\2\2H\b\3\2\2\2IJ\7h\2\2JK\7c\2\2KL\7n\2\2"+
		"LM\7u\2\2MN\7g\2\2N\n\3\2\2\2OQ\t\3\2\2PO\3\2\2\2QR\3\2\2\2RP\3\2\2\2"+
		"RS\3\2\2\2SZ\3\2\2\2TV\7\60\2\2UW\t\3\2\2VU\3\2\2\2WX\3\2\2\2XV\3\2\2"+
		"\2XY\3\2\2\2Y[\3\2\2\2ZT\3\2\2\2Z[\3\2\2\2[c\3\2\2\2\\^\7\60\2\2]_\t\3"+
		"\2\2^]\3\2\2\2_`\3\2\2\2`^\3\2\2\2`a\3\2\2\2ac\3\2\2\2bP\3\2\2\2b\\\3"+
		"\2\2\2cm\3\2\2\2df\t\4\2\2eg\t\5\2\2fe\3\2\2\2fg\3\2\2\2gi\3\2\2\2hj\t"+
		"\3\2\2ih\3\2\2\2jk\3\2\2\2ki\3\2\2\2kl\3\2\2\2ln\3\2\2\2md\3\2\2\2mn\3"+
		"\2\2\2n\f\3\2\2\2os\t\6\2\2pr\t\7\2\2qp\3\2\2\2ru\3\2\2\2sq\3\2\2\2st"+
		"\3\2\2\2t\16\3\2\2\2us\3\2\2\2vw\7*\2\2w\20\3\2\2\2xy\7+\2\2y\22\3\2\2"+
		"\2z{\7]\2\2{\24\3\2\2\2|}\7_\2\2}\26\3\2\2\2~\177\7.\2\2\177\30\3\2\2"+
		"\2\u0080\u0081\7=\2\2\u0081\32\3\2\2\2\u0082\u0083\7-\2\2\u0083\34\3\2"+
		"\2\2\u0084\u0085\7/\2\2\u0085\36\3\2\2\2\u0086\u0087\7,\2\2\u0087 \3\2"+
		"\2\2\u0088\u0089\7\61\2\2\u0089\"\3\2\2\2\u008a\u008b\7`\2\2\u008b$\3"+
		"\2\2\2\u008c\u008d\7\60\2\2\u008d&\3\2\2\2\u008e\u0090\7(\2\2\u008f\u0091"+
		"\t\b\2\2\u0090\u008f\3\2\2\2\u0090\u0091\3\2\2\2\u0091(\3\2\2\2\u0092"+
		"\u0094\7~\2\2\u0093\u0095\t\t\2\2\u0094\u0093\3\2\2\2\u0094\u0095\3\2"+
		"\2\2\u0095*\3\2\2\2\u0096\u0097\7#\2\2\u0097,\3\2\2\2\u0098\u0099\7>\2"+
		"\2\u0099.\3\2\2\2\u009a\u009b\7@\2\2\u009b\60\3\2\2\2\u009c\u009d\7>\2"+
		"\2\u009d\u009e\7?\2\2\u009e\62\3\2\2\2\u009f\u00a0\7@\2\2\u00a0\u00a1"+
		"\7?\2\2\u00a1\64\3\2\2\2\u00a2\u00a3\7#\2\2\u00a3\u00a4\7?\2\2\u00a4\66"+
		"\3\2\2\2\u00a5\u00a6\7?\2\2\u00a6\u00a9\7?\2\2\u00a7\u00a9\7?\2\2\u00a8"+
		"\u00a5\3\2\2\2\u00a8\u00a7\3\2\2\2\u00a98\3\2\2\2\u00aa\u00ab\7<\2\2\u00ab"+
		"\u00ac\7?\2\2\u00ac:\3\2\2\2\20\2>RXZ`bfkms\u0090\u0094\u00a8\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
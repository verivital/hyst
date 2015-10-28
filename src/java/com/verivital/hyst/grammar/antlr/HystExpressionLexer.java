// Generated from HystExpression.g4 by ANTLR 4.4
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
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, TICK=2, TRUE=3, FALSE=4, NUM=5, VAR=6, LPAR=7, RPAR=8, COMMA=9, 
		PLUS=10, MINUS=11, TIMES=12, DIV=13, POW=14, DOT=15, AND=16, OR=17, NOT=18, 
		LESS=19, GREATER=20, LESSEQUAL=21, GREATEREQUAL=22, NOTEQUAL=23, EQUAL=24, 
		EQUAL_RESET=25;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"'\\u0000'", "'\\u0001'", "'\\u0002'", "'\\u0003'", "'\\u0004'", "'\\u0005'", 
		"'\\u0006'", "'\\u0007'", "'\b'", "'\t'", "'\n'", "'\\u000B'", "'\f'", 
		"'\r'", "'\\u000E'", "'\\u000F'", "'\\u0010'", "'\\u0011'", "'\\u0012'", 
		"'\\u0013'", "'\\u0014'", "'\\u0015'", "'\\u0016'", "'\\u0017'", "'\\u0018'", 
		"'\\u0019'"
	};
	public static final String[] ruleNames = {
		"WS", "TICK", "TRUE", "FALSE", "NUM", "VAR", "LPAR", "RPAR", "COMMA", 
		"PLUS", "MINUS", "TIMES", "DIV", "POW", "DOT", "AND", "OR", "NOT", "LESS", 
		"GREATER", "LESSEQUAL", "GREATEREQUAL", "NOTEQUAL", "EQUAL", "EQUAL_RESET"
	};


	public HystExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "HystExpression.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\33\u00a1\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\3\2\6\2\67\n\2\r\2\16\28\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3"+
		"\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\6\6\6K\n\6\r\6\16\6L\3\6\3\6\6\6Q\n\6"+
		"\r\6\16\6R\5\6U\n\6\3\6\3\6\6\6Y\n\6\r\6\16\6Z\5\6]\n\6\3\6\3\6\5\6a\n"+
		"\6\3\6\6\6d\n\6\r\6\16\6e\5\6h\n\6\3\7\3\7\7\7l\n\7\f\7\16\7o\13\7\3\b"+
		"\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20"+
		"\3\20\3\21\3\21\5\21\u0085\n\21\3\22\3\22\5\22\u0089\n\22\3\23\3\23\3"+
		"\24\3\24\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\27\3\30\3\30\3\30\3\31\3"+
		"\31\3\31\5\31\u009d\n\31\3\32\3\32\3\32\2\2\33\3\3\5\4\7\5\t\6\13\7\r"+
		"\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25"+
		")\26+\27-\30/\31\61\32\63\33\3\2\n\5\2\13\f\17\17\"\"\3\2\62;\4\2GGgg"+
		"\5\2--//~~\5\2C\\aac|\6\2\62;C\\aac|\3\2()\4\2))~~\u00ad\2\3\3\2\2\2\2"+
		"\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2"+
		"\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2"+
		"\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2"+
		"\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2"+
		"\2\63\3\2\2\2\3\66\3\2\2\2\5<\3\2\2\2\7>\3\2\2\2\tC\3\2\2\2\13\\\3\2\2"+
		"\2\ri\3\2\2\2\17p\3\2\2\2\21r\3\2\2\2\23t\3\2\2\2\25v\3\2\2\2\27x\3\2"+
		"\2\2\31z\3\2\2\2\33|\3\2\2\2\35~\3\2\2\2\37\u0080\3\2\2\2!\u0082\3\2\2"+
		"\2#\u0086\3\2\2\2%\u008a\3\2\2\2\'\u008c\3\2\2\2)\u008e\3\2\2\2+\u0090"+
		"\3\2\2\2-\u0093\3\2\2\2/\u0096\3\2\2\2\61\u009c\3\2\2\2\63\u009e\3\2\2"+
		"\2\65\67\t\2\2\2\66\65\3\2\2\2\678\3\2\2\28\66\3\2\2\289\3\2\2\29:\3\2"+
		"\2\2:;\b\2\2\2;\4\3\2\2\2<=\7)\2\2=\6\3\2\2\2>?\7v\2\2?@\7t\2\2@A\7w\2"+
		"\2AB\7g\2\2B\b\3\2\2\2CD\7h\2\2DE\7c\2\2EF\7n\2\2FG\7u\2\2GH\7g\2\2H\n"+
		"\3\2\2\2IK\t\3\2\2JI\3\2\2\2KL\3\2\2\2LJ\3\2\2\2LM\3\2\2\2MT\3\2\2\2N"+
		"P\7\60\2\2OQ\t\3\2\2PO\3\2\2\2QR\3\2\2\2RP\3\2\2\2RS\3\2\2\2SU\3\2\2\2"+
		"TN\3\2\2\2TU\3\2\2\2U]\3\2\2\2VX\7\60\2\2WY\t\3\2\2XW\3\2\2\2YZ\3\2\2"+
		"\2ZX\3\2\2\2Z[\3\2\2\2[]\3\2\2\2\\J\3\2\2\2\\V\3\2\2\2]g\3\2\2\2^`\t\4"+
		"\2\2_a\t\5\2\2`_\3\2\2\2`a\3\2\2\2ac\3\2\2\2bd\t\3\2\2cb\3\2\2\2de\3\2"+
		"\2\2ec\3\2\2\2ef\3\2\2\2fh\3\2\2\2g^\3\2\2\2gh\3\2\2\2h\f\3\2\2\2im\t"+
		"\6\2\2jl\t\7\2\2kj\3\2\2\2lo\3\2\2\2mk\3\2\2\2mn\3\2\2\2n\16\3\2\2\2o"+
		"m\3\2\2\2pq\7*\2\2q\20\3\2\2\2rs\7+\2\2s\22\3\2\2\2tu\7.\2\2u\24\3\2\2"+
		"\2vw\7-\2\2w\26\3\2\2\2xy\7/\2\2y\30\3\2\2\2z{\7,\2\2{\32\3\2\2\2|}\7"+
		"\61\2\2}\34\3\2\2\2~\177\7`\2\2\177\36\3\2\2\2\u0080\u0081\7\60\2\2\u0081"+
		" \3\2\2\2\u0082\u0084\7(\2\2\u0083\u0085\t\b\2\2\u0084\u0083\3\2\2\2\u0084"+
		"\u0085\3\2\2\2\u0085\"\3\2\2\2\u0086\u0088\7~\2\2\u0087\u0089\t\t\2\2"+
		"\u0088\u0087\3\2\2\2\u0088\u0089\3\2\2\2\u0089$\3\2\2\2\u008a\u008b\7"+
		"#\2\2\u008b&\3\2\2\2\u008c\u008d\7>\2\2\u008d(\3\2\2\2\u008e\u008f\7@"+
		"\2\2\u008f*\3\2\2\2\u0090\u0091\7>\2\2\u0091\u0092\7?\2\2\u0092,\3\2\2"+
		"\2\u0093\u0094\7@\2\2\u0094\u0095\7?\2\2\u0095.\3\2\2\2\u0096\u0097\7"+
		"#\2\2\u0097\u0098\7?\2\2\u0098\60\3\2\2\2\u0099\u009a\7?\2\2\u009a\u009d"+
		"\7?\2\2\u009b\u009d\7?\2\2\u009c\u0099\3\2\2\2\u009c\u009b\3\2\2\2\u009d"+
		"\62\3\2\2\2\u009e\u009f\7<\2\2\u009f\u00a0\7?\2\2\u00a0\64\3\2\2\2\20"+
		"\28LRTZ\\`egm\u0084\u0088\u009c\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
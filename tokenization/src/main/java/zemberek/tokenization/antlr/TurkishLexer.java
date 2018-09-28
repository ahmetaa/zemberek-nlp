// Generated from /home/ahmetaa/projects/zemberek-nlp/tokenization/src/main/resources/tokenization/TurkishLexer.g4 by ANTLR 4.7
package zemberek.tokenization.antlr;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TurkishLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		Abbreviation=1, SpaceTab=2, NewLine=3, Time=4, Date=5, PercentNumeral=6,
		Number=7, URL=8, Email=9, HashTag=10, Mention=11, MetaTag=12, Emoticon=13,
		RomanNumeral=14, AbbreviationWithDots=15, Word=16, WordAlphanumerical=17,
		WordWithSymbol=18, Punctuation=19, UnknownWord=20, Unknown=21;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"Digit", "TurkishLetters", "TurkishLettersCapital", "TurkishLettersAll",
		"AllTurkishAlphanumerical", "AllTurkishAlphanumericalUnderscore", "Apostrophe",
		"DoubleQuote", "AposAndSuffix", "SpaceTab", "NewLine", "Time", "Date",
		"PercentNumeral", "Number", "Integer", "Exp", "URLFragment", "URL", "Email",
		"HashTag", "Mention", "MetaTag", "Emoticon", "RomanNumeral", "AbbreviationWithDots",
		"Word", "WordAlphanumerical", "WordWithSymbol", "PunctuationFragment",
		"Punctuation", "UnknownWord", "Unknown"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "Abbreviation", "SpaceTab", "NewLine", "Time", "Date", "PercentNumeral",
		"Number", "URL", "Email", "HashTag", "Mention", "MetaTag", "Emoticon",
		"RomanNumeral", "AbbreviationWithDots", "Word", "WordAlphanumerical",
		"WordWithSymbol", "Punctuation", "UnknownWord", "Unknown"
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


	private static Set<String> abbreviations = Sets.newHashSet();
	private java.util.Queue<Token> queue = new java.util.LinkedList<Token>();
	private static Locale localeTr = new Locale("tr");

	static {
	    try {
	        for(String line: Resources.readLines(Resources.getResource("tokenization/abbreviations.txt"),Charsets.UTF_8)) {
	            if (line.trim().length() > 0) {
	                final String abbr = line.trim().replaceAll("\\s+",""); // erase spaces
	                if (abbr.endsWith(".")) {
	                    abbreviations.add(abbr);
	                    abbreviations.add(abbr.toLowerCase(Locale.ENGLISH));
	                    abbreviations.add(abbr.toLowerCase(localeTr));
	                }
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


	@Override
	public Token nextToken() {

	    if(!queue.isEmpty()) {
	      return queue.poll();
	    }

	    Token next = super.nextToken();

	    if(next.getType() != Word) {
	      return next;
	    }

	    Token next2 = super.nextToken();
	    if (next2.getType() == Punctuation && next2.getText().equals(".")) {
	        String abbrev = next.getText() + ".";
	        if (abbreviations!= null && abbreviations.contains(abbrev)) {
	            CommonToken commonToken = new CommonToken(Abbreviation, abbrev);
	            commonToken.setStartIndex(next.getStartIndex());
	            commonToken.setStopIndex(next2.getStopIndex());
	            commonToken.setTokenIndex(next.getTokenIndex());
	            commonToken.setCharPositionInLine(next.getCharPositionInLine());
	            commonToken.setLine(next.getLine());
	            return commonToken;
	        }
	    }
	    queue.offer(next2);
	    return next;
	}



	public TurkishLexer(CharStream input) {
		super(input);
		_interp = new CustomLexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TurkishLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\27\u0217\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3"+
		"\b\3\t\3\t\3\n\3\n\6\nX\n\n\r\n\16\nY\3\13\6\13]\n\13\r\13\16\13^\3\f"+
		"\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\rk\n\r\3\r\5\rn\n\r\3\16\5\16q"+
		"\n\16\3\16\3\16\3\16\5\16v\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\5\16\u0084\n\16\3\16\5\16\u0087\n\16\3\16\5\16\u008a"+
		"\n\16\3\16\3\16\3\16\5\16\u008f\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u009d\n\16\3\16\5\16\u00a0\n\16\5\16\u00a2"+
		"\n\16\3\17\3\17\3\17\3\20\5\20\u00a8\n\20\3\20\3\20\3\20\3\20\5\20\u00ae"+
		"\n\20\3\20\5\20\u00b1\n\20\3\20\5\20\u00b4\n\20\3\20\3\20\3\20\5\20\u00b9"+
		"\n\20\3\20\5\20\u00bc\n\20\3\20\3\20\5\20\u00c0\n\20\3\20\5\20\u00c3\n"+
		"\20\3\20\3\20\3\20\3\20\5\20\u00c9\n\20\3\20\3\20\3\20\6\20\u00ce\n\20"+
		"\r\20\16\20\u00cf\3\20\3\20\5\20\u00d4\n\20\3\20\3\20\3\20\6\20\u00d9"+
		"\n\20\r\20\16\20\u00da\3\20\3\20\5\20\u00df\n\20\3\20\3\20\5\20\u00e3"+
		"\n\20\3\20\5\20\u00e6\n\20\5\20\u00e8\n\20\3\21\6\21\u00eb\n\21\r\21\16"+
		"\21\u00ec\3\22\3\22\5\22\u00f1\n\22\3\22\3\22\3\23\6\23\u00f6\n\23\r\23"+
		"\16\23\u00f7\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\5\24\u0109\n\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u011b\n\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\6\24\u0124\n\24\r\24\16\24\u0125\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0141\n\24\3\24\3\24"+
		"\3\24\5\24\u0146\n\24\3\24\3\24\5\24\u014a\n\24\5\24\u014c\n\24\3\25\6"+
		"\25\u014f\n\25\r\25\16\25\u0150\3\25\5\25\u0154\n\25\3\25\6\25\u0157\n"+
		"\25\r\25\16\25\u0158\3\25\3\25\6\25\u015d\n\25\r\25\16\25\u015e\3\25\3"+
		"\25\6\25\u0163\n\25\r\25\16\25\u0164\6\25\u0167\n\25\r\25\16\25\u0168"+
		"\3\26\3\26\6\26\u016d\n\26\r\26\16\26\u016e\3\27\3\27\6\27\u0173\n\27"+
		"\r\27\16\27\u0174\3\30\3\30\6\30\u0179\n\30\r\30\16\30\u017a\3\30\3\30"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\5\31\u01ca\n\31\3\32\6\32\u01cd\n\32\r\32\16"+
		"\32\u01ce\3\32\5\32\u01d2\n\32\3\32\5\32\u01d5\n\32\3\33\3\33\3\33\6\33"+
		"\u01da\n\33\r\33\16\33\u01db\3\33\5\33\u01df\n\33\3\33\5\33\u01e2\n\33"+
		"\3\34\6\34\u01e5\n\34\r\34\16\34\u01e6\3\35\6\35\u01ea\n\35\r\35\16\35"+
		"\u01eb\3\36\6\36\u01ef\n\36\r\36\16\36\u01f0\3\36\5\36\u01f4\n\36\3\36"+
		"\6\36\u01f7\n\36\r\36\16\36\u01f8\3\36\5\36\u01fc\n\36\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\5\37\u020a\n\37\3 \3 \3"+
		"!\6!\u020f\n!\r!\16!\u0210\3\"\6\"\u0214\n\"\r\"\16\"\u0215\3\u0215\2"+
		"#\3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\2\25\4\27\5\31\6\33\7\35\b\37"+
		"\t!\2#\2%\2\'\n)\13+\f-\r/\16\61\17\63\20\65\21\67\229\23;\24=\2?\25A"+
		"\26C\27\3\2\35\3\2\62;\13\2c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8"+
		"\u00f8\u00fd\u00fe\u0121\u0121\u0133\u0133\u0161\u0161\13\2C\\\u00c4\u00c4"+
		"\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132"+
		"\u0160\u0160\21\2C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8"+
		"\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe"+
		"\u0120\u0121\u0132\u0133\u0160\u0161\22\2\62;C\\c|\u00c4\u00c4\u00c9\u00c9"+
		"\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0"+
		"\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\23\2\62;"+
		"C\\aac|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4"+
		"\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132"+
		"\u0133\u0160\u0161\4\2))\u201b\u201b\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e"+
		"\u201f\4\2\13\13\"\"\4\2\f\f\17\17\3\2\62\64\4\2\60\60<<\3\2\62\67\3\2"+
		"\62\65\3\2\62\63\3\2\63\63\3\29;\3\2\64\64\3\2\62\62\4\2--//\4\2..\60"+
		"\60\4\2GGgg\31\2((--/;==??AAC]__aac|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0"+
		"\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8"+
		"\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\6\2\62;C\\aac|\7\2EF"+
		"KKNOXXZZ\20\2##&(*\61<=?B]`}}\177\177\u00ab\u00ab\u00b0\u00b0\u201a\u201a"+
		"\u2028\u2028\u2122\u2122\u2124\u2124\24\2\13\f\17\17\"$&\61<=?B]`}}\177"+
		"\177\u00ab\u00ab\u00ad\u00ad\u00b0\u00b0\u00bd\u00bd\u201a\u201b\u201e"+
		"\u201f\u2028\u2028\u2122\u2122\u2124\u2124\2\u0273\2\25\3\2\2\2\2\27\3"+
		"\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2\'\3\2\2\2"+
		"\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2"+
		"\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2"+
		"\2C\3\2\2\2\3E\3\2\2\2\5G\3\2\2\2\7I\3\2\2\2\tK\3\2\2\2\13M\3\2\2\2\r"+
		"O\3\2\2\2\17Q\3\2\2\2\21S\3\2\2\2\23U\3\2\2\2\25\\\3\2\2\2\27`\3\2\2\2"+
		"\31b\3\2\2\2\33\u00a1\3\2\2\2\35\u00a3\3\2\2\2\37\u00e7\3\2\2\2!\u00ea"+
		"\3\2\2\2#\u00ee\3\2\2\2%\u00f5\3\2\2\2\'\u014b\3\2\2\2)\u014e\3\2\2\2"+
		"+\u016a\3\2\2\2-\u0170\3\2\2\2/\u0176\3\2\2\2\61\u01c9\3\2\2\2\63\u01cc"+
		"\3\2\2\2\65\u01d9\3\2\2\2\67\u01e4\3\2\2\29\u01e9\3\2\2\2;\u01ee\3\2\2"+
		"\2=\u0209\3\2\2\2?\u020b\3\2\2\2A\u020e\3\2\2\2C\u0213\3\2\2\2EF\t\2\2"+
		"\2F\4\3\2\2\2GH\t\3\2\2H\6\3\2\2\2IJ\t\4\2\2J\b\3\2\2\2KL\t\5\2\2L\n\3"+
		"\2\2\2MN\t\6\2\2N\f\3\2\2\2OP\t\7\2\2P\16\3\2\2\2QR\t\b\2\2R\20\3\2\2"+
		"\2ST\t\t\2\2T\22\3\2\2\2UW\5\17\b\2VX\5\t\5\2WV\3\2\2\2XY\3\2\2\2YW\3"+
		"\2\2\2YZ\3\2\2\2Z\24\3\2\2\2[]\t\n\2\2\\[\3\2\2\2]^\3\2\2\2^\\\3\2\2\2"+
		"^_\3\2\2\2_\26\3\2\2\2`a\t\13\2\2a\30\3\2\2\2bc\t\f\2\2cd\t\2\2\2de\t"+
		"\r\2\2ef\t\16\2\2fj\t\2\2\2gh\t\r\2\2hi\t\16\2\2ik\t\2\2\2jg\3\2\2\2j"+
		"k\3\2\2\2km\3\2\2\2ln\5\23\n\2ml\3\2\2\2mn\3\2\2\2n\32\3\2\2\2oq\t\17"+
		"\2\2po\3\2\2\2pq\3\2\2\2qr\3\2\2\2rs\t\2\2\2su\7\60\2\2tv\t\20\2\2ut\3"+
		"\2\2\2uv\3\2\2\2vw\3\2\2\2wx\t\2\2\2x\u0083\7\60\2\2yz\t\21\2\2z{\t\22"+
		"\2\2{|\t\2\2\2|\u0084\t\2\2\2}~\t\23\2\2~\177\t\24\2\2\177\u0080\t\2\2"+
		"\2\u0080\u0084\t\2\2\2\u0081\u0082\t\2\2\2\u0082\u0084\t\2\2\2\u0083y"+
		"\3\2\2\2\u0083}\3\2\2\2\u0083\u0081\3\2\2\2\u0084\u0086\3\2\2\2\u0085"+
		"\u0087\5\23\n\2\u0086\u0085\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u00a2\3"+
		"\2\2\2\u0088\u008a\t\17\2\2\u0089\u0088\3\2\2\2\u0089\u008a\3\2\2\2\u008a"+
		"\u008b\3\2\2\2\u008b\u008c\t\2\2\2\u008c\u008e\7\61\2\2\u008d\u008f\t"+
		"\20\2\2\u008e\u008d\3\2\2\2\u008e\u008f\3\2\2\2\u008f\u0090\3\2\2\2\u0090"+
		"\u0091\t\2\2\2\u0091\u009c\7\61\2\2\u0092\u0093\t\21\2\2\u0093\u0094\t"+
		"\22\2\2\u0094\u0095\t\2\2\2\u0095\u009d\t\2\2\2\u0096\u0097\t\23\2\2\u0097"+
		"\u0098\t\24\2\2\u0098\u0099\t\2\2\2\u0099\u009d\t\2\2\2\u009a\u009b\t"+
		"\2\2\2\u009b\u009d\t\2\2\2\u009c\u0092\3\2\2\2\u009c\u0096\3\2\2\2\u009c"+
		"\u009a\3\2\2\2\u009d\u009f\3\2\2\2\u009e\u00a0\5\23\n\2\u009f\u009e\3"+
		"\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a2\3\2\2\2\u00a1p\3\2\2\2\u00a1\u0089"+
		"\3\2\2\2\u00a2\34\3\2\2\2\u00a3\u00a4\7\'\2\2\u00a4\u00a5\5\37\20\2\u00a5"+
		"\36\3\2\2\2\u00a6\u00a8\t\25\2\2\u00a7\u00a6\3\2\2\2\u00a7\u00a8\3\2\2"+
		"\2\u00a8\u00a9\3\2\2\2\u00a9\u00aa\5!\21\2\u00aa\u00ab\t\26\2\2\u00ab"+
		"\u00ad\5!\21\2\u00ac\u00ae\5#\22\2\u00ad\u00ac\3\2\2\2\u00ad\u00ae\3\2"+
		"\2\2\u00ae\u00b0\3\2\2\2\u00af\u00b1\5\23\n\2\u00b0\u00af\3\2\2\2\u00b0"+
		"\u00b1\3\2\2\2\u00b1\u00e8\3\2\2\2\u00b2\u00b4\t\25\2\2\u00b3\u00b2\3"+
		"\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00b5\3\2\2\2\u00b5\u00b6\5!\21\2\u00b6"+
		"\u00b8\5#\22\2\u00b7\u00b9\5\23\n\2\u00b8\u00b7\3\2\2\2\u00b8\u00b9\3"+
		"\2\2\2\u00b9\u00e8\3\2\2\2\u00ba\u00bc\t\25\2\2\u00bb\u00ba\3\2\2\2\u00bb"+
		"\u00bc\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00bf\5!\21\2\u00be\u00c0\5\23"+
		"\n\2\u00bf\u00be\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00e8\3\2\2\2\u00c1"+
		"\u00c3\t\25\2\2\u00c2\u00c1\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c4\3"+
		"\2\2\2\u00c4\u00c5\5!\21\2\u00c5\u00c6\7\61\2\2\u00c6\u00c8\5!\21\2\u00c7"+
		"\u00c9\5\23\n\2\u00c8\u00c7\3\2\2\2\u00c8\u00c9\3\2\2\2\u00c9\u00e8\3"+
		"\2\2\2\u00ca\u00cb\5!\21\2\u00cb\u00cc\7\60\2\2\u00cc\u00ce\3\2\2\2\u00cd"+
		"\u00ca\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00cd\3\2\2\2\u00cf\u00d0\3\2"+
		"\2\2\u00d0\u00d1\3\2\2\2\u00d1\u00d3\5!\21\2\u00d2\u00d4\5\23\n\2\u00d3"+
		"\u00d2\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00e8\3\2\2\2\u00d5\u00d6\5!"+
		"\21\2\u00d6\u00d7\7.\2\2\u00d7\u00d9\3\2\2\2\u00d8\u00d5\3\2\2\2\u00d9"+
		"\u00da\3\2\2\2\u00da\u00d8\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u00dc\3\2"+
		"\2\2\u00dc\u00de\5!\21\2\u00dd\u00df\5\23\n\2\u00de\u00dd\3\2\2\2\u00de"+
		"\u00df\3\2\2\2\u00df\u00e8\3\2\2\2\u00e0\u00e2\5!\21\2\u00e1\u00e3\7\60"+
		"\2\2\u00e2\u00e1\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u00e5\3\2\2\2\u00e4"+
		"\u00e6\5\23\n\2\u00e5\u00e4\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e8\3"+
		"\2\2\2\u00e7\u00a7\3\2\2\2\u00e7\u00b3\3\2\2\2\u00e7\u00bb\3\2\2\2\u00e7"+
		"\u00c2\3\2\2\2\u00e7\u00cd\3\2\2\2\u00e7\u00d8\3\2\2\2\u00e7\u00e0\3\2"+
		"\2\2\u00e8 \3\2\2\2\u00e9\u00eb\5\3\2\2\u00ea\u00e9\3\2\2\2\u00eb\u00ec"+
		"\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\"\3\2\2\2\u00ee"+
		"\u00f0\t\27\2\2\u00ef\u00f1\t\25\2\2\u00f0\u00ef\3\2\2\2\u00f0\u00f1\3"+
		"\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f3\5!\21\2\u00f3$\3\2\2\2\u00f4\u00f6"+
		"\t\30\2\2\u00f5\u00f4\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u00f5\3\2\2\2"+
		"\u00f7\u00f8\3\2\2\2\u00f8&\3\2\2\2\u00f9\u00fa\7j\2\2\u00fa\u00fb\7v"+
		"\2\2\u00fb\u00fc\7v\2\2\u00fc\u00fd\7r\2\2\u00fd\u00fe\7<\2\2\u00fe\u00ff"+
		"\7\61\2\2\u00ff\u0109\7\61\2\2\u0100\u0101\7j\2\2\u0101\u0102\7v\2\2\u0102"+
		"\u0103\7v\2\2\u0103\u0104\7r\2\2\u0104\u0105\7u\2\2\u0105\u0106\7<\2\2"+
		"\u0106\u0107\7\61\2\2\u0107\u0109\7\61\2\2\u0108\u00f9\3\2\2\2\u0108\u0100"+
		"\3\2\2\2\u0109\u010a\3\2\2\2\u010a\u014c\5%\23\2\u010b\u010c\7j\2\2\u010c"+
		"\u010d\7v\2\2\u010d\u010e\7v\2\2\u010e\u010f\7r\2\2\u010f\u0110\7<\2\2"+
		"\u0110\u0111\7\61\2\2\u0111\u011b\7\61\2\2\u0112\u0113\7j\2\2\u0113\u0114"+
		"\7v\2\2\u0114\u0115\7v\2\2\u0115\u0116\7r\2\2\u0116\u0117\7u\2\2\u0117"+
		"\u0118\7<\2\2\u0118\u0119\7\61\2\2\u0119\u011b\7\61\2\2\u011a\u010b\3"+
		"\2\2\2\u011a\u0112\3\2\2\2\u011a\u011b\3\2\2\2\u011b\u011c\3\2\2\2\u011c"+
		"\u011d\7y\2\2\u011d\u011e\7y\2\2\u011e\u011f\7y\2\2\u011f\u0120\7\60\2"+
		"\2\u0120\u0121\3\2\2\2\u0121\u014c\5%\23\2\u0122\u0124\t\31\2\2\u0123"+
		"\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0123\3\2\2\2\u0125\u0126\3\2"+
		"\2\2\u0126\u0140\3\2\2\2\u0127\u0128\7\60\2\2\u0128\u0129\7e\2\2\u0129"+
		"\u012a\7q\2\2\u012a\u0141\7o\2\2\u012b\u012c\7\60\2\2\u012c\u012d\7q\2"+
		"\2\u012d\u012e\7t\2\2\u012e\u0141\7i\2\2\u012f\u0130\7\60\2\2\u0130\u0131"+
		"\7g\2\2\u0131\u0132\7f\2\2\u0132\u0141\7w\2\2\u0133\u0134\7\60\2\2\u0134"+
		"\u0135\7i\2\2\u0135\u0136\7q\2\2\u0136\u0141\7x\2\2\u0137\u0138\7\60\2"+
		"\2\u0138\u0139\7p\2\2\u0139\u013a\7g\2\2\u013a\u0141\7v\2\2\u013b\u013c"+
		"\7\60\2\2\u013c\u013d\7k\2\2\u013d\u013e\7p\2\2\u013e\u013f\7h\2\2\u013f"+
		"\u0141\7q\2\2\u0140\u0127\3\2\2\2\u0140\u012b\3\2\2\2\u0140\u012f\3\2"+
		"\2\2\u0140\u0133\3\2\2\2\u0140\u0137\3\2\2\2\u0140\u013b\3\2\2\2\u0141"+
		"\u0145\3\2\2\2\u0142\u0143\7\60\2\2\u0143\u0144\7v\2\2\u0144\u0146\7t"+
		"\2\2\u0145\u0142\3\2\2\2\u0145\u0146\3\2\2\2\u0146\u0149\3\2\2\2\u0147"+
		"\u0148\7\61\2\2\u0148\u014a\5%\23\2\u0149\u0147\3\2\2\2\u0149\u014a\3"+
		"\2\2\2\u014a\u014c\3\2\2\2\u014b\u0108\3\2\2\2\u014b\u011a\3\2\2\2\u014b"+
		"\u0123\3\2\2\2\u014c(\3\2\2\2\u014d\u014f\5\r\7\2\u014e\u014d\3\2\2\2"+
		"\u014f\u0150\3\2\2\2\u0150\u014e\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u0153"+
		"\3\2\2\2\u0152\u0154\7\60\2\2\u0153\u0152\3\2\2\2\u0153\u0154\3\2\2\2"+
		"\u0154\u0156\3\2\2\2\u0155\u0157\5\r\7\2\u0156\u0155\3\2\2\2\u0157\u0158"+
		"\3\2\2\2\u0158\u0156\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u015a\3\2\2\2\u015a"+
		"\u0166\7B\2\2\u015b\u015d\5\13\6\2\u015c\u015b\3\2\2\2\u015d\u015e\3\2"+
		"\2\2\u015e\u015c\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0160\3\2\2\2\u0160"+
		"\u0162\7\60\2\2\u0161\u0163\5\13\6\2\u0162\u0161\3\2\2\2\u0163\u0164\3"+
		"\2\2\2\u0164\u0162\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0167\3\2\2\2\u0166"+
		"\u015c\3\2\2\2\u0167\u0168\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2"+
		"\2\2\u0169*\3\2\2\2\u016a\u016c\7%\2\2\u016b\u016d\5\13\6\2\u016c\u016b"+
		"\3\2\2\2\u016d\u016e\3\2\2\2\u016e\u016c\3\2\2\2\u016e\u016f\3\2\2\2\u016f"+
		",\3\2\2\2\u0170\u0172\7B\2\2\u0171\u0173\5\13\6\2\u0172\u0171\3\2\2\2"+
		"\u0173\u0174\3\2\2\2\u0174\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175.\3"+
		"\2\2\2\u0176\u0178\7>\2\2\u0177\u0179\5\r\7\2\u0178\u0177\3\2\2\2\u0179"+
		"\u017a\3\2\2\2\u017a\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u017c\3\2"+
		"\2\2\u017c\u017d\7@\2\2\u017d\60\3\2\2\2\u017e\u017f\7<\2\2\u017f\u01ca"+
		"\7+\2\2\u0180\u0181\7<\2\2\u0181\u0182\7/\2\2\u0182\u01ca\7+\2\2\u0183"+
		"\u0184\7<\2\2\u0184\u0185\7/\2\2\u0185\u01ca\7_\2\2\u0186\u0187\7<\2\2"+
		"\u0187\u01ca\7F\2\2\u0188\u0189\7<\2\2\u0189\u018a\7/\2\2\u018a\u01ca"+
		"\7F\2\2\u018b\u018c\7:\2\2\u018c\u018d\7/\2\2\u018d\u01ca\7+\2\2\u018e"+
		"\u018f\7=\2\2\u018f\u01ca\7+\2\2\u0190\u0191\7=\2\2\u0191\u0192\7\u2013"+
		"\2\2\u0192\u01ca\7+\2\2\u0193\u0194\7<\2\2\u0194\u01ca\7*\2\2\u0195\u0196"+
		"\7<\2\2\u0196\u0197\7/\2\2\u0197\u01ca\7*\2\2\u0198\u0199\7<\2\2\u0199"+
		"\u019a\7)\2\2\u019a\u01ca\7*\2\2\u019b\u019c\7<\2\2\u019c\u019d\7)\2\2"+
		"\u019d\u01ca\7+\2\2\u019e\u019f\7<\2\2\u019f\u01ca\7R\2\2\u01a0\u01a1"+
		"\7<\2\2\u01a1\u01ca\7r\2\2\u01a2\u01a3\7<\2\2\u01a3\u01ca\7~\2\2\u01a4"+
		"\u01a5\7?\2\2\u01a5\u01ca\7~\2\2\u01a6\u01a7\7?\2\2\u01a7\u01ca\7+\2\2"+
		"\u01a8\u01a9\7?\2\2\u01a9\u01ca\7*\2\2\u01aa\u01ab\7<\2\2\u01ab\u01ac"+
		"\7\u2013\2\2\u01ac\u01ca\7\61\2\2\u01ad\u01ae\7<\2\2\u01ae\u01ca\7\61"+
		"\2\2\u01af\u01b0\7<\2\2\u01b0\u01b1\7`\2\2\u01b1\u01ca\7+\2\2\u01b2\u01b3"+
		"\7\u00b1\2\2\u01b3\u01b4\7^\2\2\u01b4\u01b5\7a\2\2\u01b5\u01b6\7*\2\2"+
		"\u01b6\u01b7\7\u30c6\2\2\u01b7\u01b8\7+\2\2\u01b8\u01b9\7a\2\2\u01b9\u01ba"+
		"\7\61\2\2\u01ba\u01ca\7\u00b1\2\2\u01bb\u01bc\7Q\2\2\u01bc\u01bd\7a\2"+
		"\2\u01bd\u01ca\7q\2\2\u01be\u01bf\7q\2\2\u01bf\u01c0\7a\2\2\u01c0\u01ca"+
		"\7Q\2\2\u01c1\u01c2\7Q\2\2\u01c2\u01c3\7a\2\2\u01c3\u01ca\7Q\2\2\u01c4"+
		"\u01c5\7^\2\2\u01c5\u01c6\7q\2\2\u01c6\u01ca\7\61\2\2\u01c7\u01c8\7>\2"+
		"\2\u01c8\u01ca\7\65\2\2\u01c9\u017e\3\2\2\2\u01c9\u0180\3\2\2\2\u01c9"+
		"\u0183\3\2\2\2\u01c9\u0186\3\2\2\2\u01c9\u0188\3\2\2\2\u01c9\u018b\3\2"+
		"\2\2\u01c9\u018e\3\2\2\2\u01c9\u0190\3\2\2\2\u01c9\u0193\3\2\2\2\u01c9"+
		"\u0195\3\2\2\2\u01c9\u0198\3\2\2\2\u01c9\u019b\3\2\2\2\u01c9\u019e\3\2"+
		"\2\2\u01c9\u01a0\3\2\2\2\u01c9\u01a2\3\2\2\2\u01c9\u01a4\3\2\2\2\u01c9"+
		"\u01a6\3\2\2\2\u01c9\u01a8\3\2\2\2\u01c9\u01aa\3\2\2\2\u01c9\u01ad\3\2"+
		"\2\2\u01c9\u01af\3\2\2\2\u01c9\u01b2\3\2\2\2\u01c9\u01bb\3\2\2\2\u01c9"+
		"\u01be\3\2\2\2\u01c9\u01c1\3\2\2\2\u01c9\u01c4\3\2\2\2\u01c9\u01c7\3\2"+
		"\2\2\u01ca\62\3\2\2\2\u01cb\u01cd\t\32\2\2\u01cc\u01cb\3\2\2\2\u01cd\u01ce"+
		"\3\2\2\2\u01ce\u01cc\3\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01d1\3\2\2\2\u01d0"+
		"\u01d2\7\60\2\2\u01d1\u01d0\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2\u01d4\3"+
		"\2\2\2\u01d3\u01d5\5\23\n\2\u01d4\u01d3\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5"+
		"\64\3\2\2\2\u01d6\u01d7\5\7\4\2\u01d7\u01d8\7\60\2\2\u01d8\u01da\3\2\2"+
		"\2\u01d9\u01d6\3\2\2\2\u01da\u01db\3\2\2\2\u01db\u01d9\3\2\2\2\u01db\u01dc"+
		"\3\2\2\2\u01dc\u01de\3\2\2\2\u01dd\u01df\5\7\4\2\u01de\u01dd\3\2\2\2\u01de"+
		"\u01df\3\2\2\2\u01df\u01e1\3\2\2\2\u01e0\u01e2\5\23\n\2\u01e1\u01e0\3"+
		"\2\2\2\u01e1\u01e2\3\2\2\2\u01e2\66\3\2\2\2\u01e3\u01e5\5\t\5\2\u01e4"+
		"\u01e3\3\2\2\2\u01e5\u01e6\3\2\2\2\u01e6\u01e4\3\2\2\2\u01e6\u01e7\3\2"+
		"\2\2\u01e78\3\2\2\2\u01e8\u01ea\5\13\6\2\u01e9\u01e8\3\2\2\2\u01ea\u01eb"+
		"\3\2\2\2\u01eb\u01e9\3\2\2\2\u01eb\u01ec\3\2\2\2\u01ec:\3\2\2\2\u01ed"+
		"\u01ef\5\13\6\2\u01ee\u01ed\3\2\2\2\u01ef\u01f0\3\2\2\2\u01f0\u01ee\3"+
		"\2\2\2\u01f0\u01f1\3\2\2\2\u01f1\u01f3\3\2\2\2\u01f2\u01f4\7/\2\2\u01f3"+
		"\u01f2\3\2\2\2\u01f3\u01f4\3\2\2\2\u01f4\u01f6\3\2\2\2\u01f5\u01f7\5\13"+
		"\6\2\u01f6\u01f5\3\2\2\2\u01f7\u01f8\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f8"+
		"\u01f9\3\2\2\2\u01f9\u01fb\3\2\2\2\u01fa\u01fc\5\23\n\2\u01fb\u01fa\3"+
		"\2\2\2\u01fb\u01fc\3\2\2\2\u01fc<\3\2\2\2\u01fd\u020a\5\17\b\2\u01fe\u020a"+
		"\5\21\t\2\u01ff\u0200\7\60\2\2\u0200\u0201\7\60\2\2\u0201\u020a\7\60\2"+
		"\2\u0202\u0203\7*\2\2\u0203\u0204\7#\2\2\u0204\u020a\7+\2\2\u0205\u0206"+
		"\7*\2\2\u0206\u0207\7A\2\2\u0207\u020a\7+\2\2\u0208\u020a\t\33\2\2\u0209"+
		"\u01fd\3\2\2\2\u0209\u01fe\3\2\2\2\u0209\u01ff\3\2\2\2\u0209\u0202\3\2"+
		"\2\2\u0209\u0205\3\2\2\2\u0209\u0208\3\2\2\2\u020a>\3\2\2\2\u020b\u020c"+
		"\5=\37\2\u020c@\3\2\2\2\u020d\u020f\n\34\2\2\u020e\u020d\3\2\2\2\u020f"+
		"\u0210\3\2\2\2\u0210\u020e\3\2\2\2\u0210\u0211\3\2\2\2\u0211B\3\2\2\2"+
		"\u0212\u0214\13\2\2\2\u0213\u0212\3\2\2\2\u0214\u0215\3\2\2\2\u0215\u0216"+
		"\3\2\2\2\u0215\u0213\3\2\2\2\u0216D\3\2\2\2C\2Y^jmpu\u0083\u0086\u0089"+
		"\u008e\u009c\u009f\u00a1\u00a7\u00ad\u00b0\u00b3\u00b8\u00bb\u00bf\u00c2"+
		"\u00c8\u00cf\u00d3\u00da\u00de\u00e2\u00e5\u00e7\u00ec\u00f0\u00f7\u0108"+
		"\u011a\u0125\u0140\u0145\u0149\u014b\u0150\u0153\u0158\u015e\u0164\u0168"+
		"\u016e\u0174\u017a\u01c9\u01ce\u01d1\u01d4\u01db\u01de\u01e1\u01e6\u01eb"+
		"\u01f0\u01f3\u01f8\u01fb\u0209\u0210\u0215\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
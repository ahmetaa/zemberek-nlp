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
		Number=7, URL=8, Email=9, HashTag=10, Mention=11, Emoticon=12, RomanNumeral=13, 
		AbbreviationWithDots=14, Word=15, WordAlphanumerical=16, WordWithSymbol=17, 
		Punctuation=18, UnknownWord=19, Unknown=20;
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
		"HashTag", "Mention", "Emoticon", "RomanNumeral", "AbbreviationWithDots", 
		"Word", "WordAlphanumerical", "WordWithSymbol", "PunctuationFragment", 
		"Punctuation", "UnknownWord", "Unknown"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "Abbreviation", "SpaceTab", "NewLine", "Time", "Date", "PercentNumeral", 
		"Number", "URL", "Email", "HashTag", "Mention", "Emoticon", "RomanNumeral", 
		"AbbreviationWithDots", "Word", "WordAlphanumerical", "WordWithSymbol", 
		"Punctuation", "UnknownWord", "Unknown"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\26\u01dc\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3"+
		"\t\3\n\3\n\6\nV\n\n\r\n\16\nW\3\13\6\13[\n\13\r\13\16\13\\\3\f\3\f\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\ri\n\r\3\r\5\rl\n\r\3\16\5\16o\n\16\3\16"+
		"\3\16\3\16\5\16t\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\5\16\u0082\n\16\3\16\5\16\u0085\n\16\3\16\5\16\u0088\n\16\3"+
		"\16\3\16\3\16\5\16\u008d\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\5\16\u009b\n\16\3\16\5\16\u009e\n\16\5\16\u00a0\n"+
		"\16\3\17\3\17\3\17\3\20\5\20\u00a6\n\20\3\20\3\20\3\20\3\20\5\20\u00ac"+
		"\n\20\3\20\5\20\u00af\n\20\3\20\5\20\u00b2\n\20\3\20\3\20\3\20\5\20\u00b7"+
		"\n\20\3\20\5\20\u00ba\n\20\3\20\3\20\5\20\u00be\n\20\3\20\3\20\3\20\6"+
		"\20\u00c3\n\20\r\20\16\20\u00c4\3\20\3\20\5\20\u00c9\n\20\3\20\3\20\3"+
		"\20\6\20\u00ce\n\20\r\20\16\20\u00cf\3\20\3\20\5\20\u00d4\n\20\3\20\3"+
		"\20\5\20\u00d8\n\20\3\20\5\20\u00db\n\20\5\20\u00dd\n\20\3\21\6\21\u00e0"+
		"\n\21\r\21\16\21\u00e1\3\22\3\22\5\22\u00e6\n\22\3\22\3\22\3\23\6\23\u00eb"+
		"\n\23\r\23\16\23\u00ec\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\5\24\u00fe\n\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0110\n\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0118\n\24\3\25\6\25\u011b\n\25\r"+
		"\25\16\25\u011c\3\25\5\25\u0120\n\25\3\25\6\25\u0123\n\25\r\25\16\25\u0124"+
		"\3\25\3\25\6\25\u0129\n\25\r\25\16\25\u012a\3\25\3\25\6\25\u012f\n\25"+
		"\r\25\16\25\u0130\6\25\u0133\n\25\r\25\16\25\u0134\3\26\3\26\6\26\u0139"+
		"\n\26\r\26\16\26\u013a\3\27\3\27\6\27\u013f\n\27\r\27\16\27\u0140\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\5\30\u017d\n\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\5\31\u0193\n\31\3\31"+
		"\5\31\u0196\n\31\3\31\5\31\u0199\n\31\3\32\3\32\3\32\6\32\u019e\n\32\r"+
		"\32\16\32\u019f\3\32\5\32\u01a3\n\32\3\32\5\32\u01a6\n\32\3\33\6\33\u01a9"+
		"\n\33\r\33\16\33\u01aa\3\34\6\34\u01ae\n\34\r\34\16\34\u01af\3\35\6\35"+
		"\u01b3\n\35\r\35\16\35\u01b4\3\35\5\35\u01b8\n\35\3\35\6\35\u01bb\n\35"+
		"\r\35\16\35\u01bc\3\35\5\35\u01c0\n\35\3\36\3\36\3\36\3\36\3\36\3\36\3"+
		"\36\3\36\3\36\3\36\3\36\3\36\3\36\5\36\u01cf\n\36\3\37\3\37\3 \6 \u01d4"+
		"\n \r \16 \u01d5\3!\6!\u01d9\n!\r!\16!\u01da\3\u01da\2\"\3\2\5\2\7\2\t"+
		"\2\13\2\r\2\17\2\21\2\23\2\25\4\27\5\31\6\33\7\35\b\37\t!\2#\2%\2\'\n"+
		")\13+\f-\r/\16\61\17\63\20\65\21\67\229\23;\2=\24?\25A\26\3\2\34\3\2\62"+
		";\13\2c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0121"+
		"\u0121\u0133\u0133\u0161\u0161\13\2C\\\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0"+
		"\u00d8\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132\u0160\u0160\21\2C\\c"+
		"|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4"+
		"\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133"+
		"\u0160\u0161\22\2\62;C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8"+
		"\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe"+
		"\u0120\u0121\u0132\u0133\u0160\u0161\23\2\62;C\\aac|\u00c4\u00c4\u00c9"+
		"\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0"+
		"\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\4\2"+
		"))\u201b\u201b\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e\u201f\4\2\13\13\"\""+
		"\4\2\f\f\17\17\3\2\62\64\4\2\60\60<<\3\2\62\67\3\2\62\65\3\2\62\63\3\2"+
		"\63\63\3\29;\3\2\64\64\3\2\62\62\4\2--//\4\2..\60\60\4\2GGgg\31\2((--"+
		"/;==??AAC]__aac|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd"+
		"\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120"+
		"\u0121\u0132\u0133\u0160\u0161\4\2\u201a\u201a\u2028\u2028\n\2##&(*\61"+
		"<=AB]_}}\177\177\20\2\13\f\17\17\"$&\61<=AB]_}}\177\177\u00ad\u00ad\u00bd"+
		"\u00bd\u201a\u201b\u201e\u201f\u2028\u2028\2\u022b\2\25\3\2\2\2\2\27\3"+
		"\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2\'\3\2\2\2"+
		"\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2"+
		"\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2"+
		"\3C\3\2\2\2\5E\3\2\2\2\7G\3\2\2\2\tI\3\2\2\2\13K\3\2\2\2\rM\3\2\2\2\17"+
		"O\3\2\2\2\21Q\3\2\2\2\23S\3\2\2\2\25Z\3\2\2\2\27^\3\2\2\2\31`\3\2\2\2"+
		"\33\u009f\3\2\2\2\35\u00a1\3\2\2\2\37\u00dc\3\2\2\2!\u00df\3\2\2\2#\u00e3"+
		"\3\2\2\2%\u00ea\3\2\2\2\'\u0117\3\2\2\2)\u011a\3\2\2\2+\u0136\3\2\2\2"+
		"-\u013c\3\2\2\2/\u017c\3\2\2\2\61\u0192\3\2\2\2\63\u019d\3\2\2\2\65\u01a8"+
		"\3\2\2\2\67\u01ad\3\2\2\29\u01b2\3\2\2\2;\u01ce\3\2\2\2=\u01d0\3\2\2\2"+
		"?\u01d3\3\2\2\2A\u01d8\3\2\2\2CD\t\2\2\2D\4\3\2\2\2EF\t\3\2\2F\6\3\2\2"+
		"\2GH\t\4\2\2H\b\3\2\2\2IJ\t\5\2\2J\n\3\2\2\2KL\t\6\2\2L\f\3\2\2\2MN\t"+
		"\7\2\2N\16\3\2\2\2OP\t\b\2\2P\20\3\2\2\2QR\t\t\2\2R\22\3\2\2\2SU\5\17"+
		"\b\2TV\5\t\5\2UT\3\2\2\2VW\3\2\2\2WU\3\2\2\2WX\3\2\2\2X\24\3\2\2\2Y[\t"+
		"\n\2\2ZY\3\2\2\2[\\\3\2\2\2\\Z\3\2\2\2\\]\3\2\2\2]\26\3\2\2\2^_\t\13\2"+
		"\2_\30\3\2\2\2`a\t\f\2\2ab\t\2\2\2bc\t\r\2\2cd\t\16\2\2dh\t\2\2\2ef\t"+
		"\r\2\2fg\t\16\2\2gi\t\2\2\2he\3\2\2\2hi\3\2\2\2ik\3\2\2\2jl\5\23\n\2k"+
		"j\3\2\2\2kl\3\2\2\2l\32\3\2\2\2mo\t\17\2\2nm\3\2\2\2no\3\2\2\2op\3\2\2"+
		"\2pq\t\2\2\2qs\7\60\2\2rt\t\20\2\2sr\3\2\2\2st\3\2\2\2tu\3\2\2\2uv\t\2"+
		"\2\2v\u0081\7\60\2\2wx\t\21\2\2xy\t\22\2\2yz\t\2\2\2z\u0082\t\2\2\2{|"+
		"\t\23\2\2|}\t\24\2\2}~\t\2\2\2~\u0082\t\2\2\2\177\u0080\t\2\2\2\u0080"+
		"\u0082\t\2\2\2\u0081w\3\2\2\2\u0081{\3\2\2\2\u0081\177\3\2\2\2\u0082\u0084"+
		"\3\2\2\2\u0083\u0085\5\23\n\2\u0084\u0083\3\2\2\2\u0084\u0085\3\2\2\2"+
		"\u0085\u00a0\3\2\2\2\u0086\u0088\t\17\2\2\u0087\u0086\3\2\2\2\u0087\u0088"+
		"\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u008a\t\2\2\2\u008a\u008c\7\61\2\2"+
		"\u008b\u008d\t\20\2\2\u008c\u008b\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e"+
		"\3\2\2\2\u008e\u008f\t\2\2\2\u008f\u009a\7\61\2\2\u0090\u0091\t\21\2\2"+
		"\u0091\u0092\t\22\2\2\u0092\u0093\t\2\2\2\u0093\u009b\t\2\2\2\u0094\u0095"+
		"\t\23\2\2\u0095\u0096\t\24\2\2\u0096\u0097\t\2\2\2\u0097\u009b\t\2\2\2"+
		"\u0098\u0099\t\2\2\2\u0099\u009b\t\2\2\2\u009a\u0090\3\2\2\2\u009a\u0094"+
		"\3\2\2\2\u009a\u0098\3\2\2\2\u009b\u009d\3\2\2\2\u009c\u009e\5\23\n\2"+
		"\u009d\u009c\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u00a0\3\2\2\2\u009fn\3"+
		"\2\2\2\u009f\u0087\3\2\2\2\u00a0\34\3\2\2\2\u00a1\u00a2\7\'\2\2\u00a2"+
		"\u00a3\5\37\20\2\u00a3\36\3\2\2\2\u00a4\u00a6\t\25\2\2\u00a5\u00a4\3\2"+
		"\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a8\5!\21\2\u00a8"+
		"\u00a9\t\26\2\2\u00a9\u00ab\5!\21\2\u00aa\u00ac\5#\22\2\u00ab\u00aa\3"+
		"\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad\u00af\5\23\n\2\u00ae"+
		"\u00ad\3\2\2\2\u00ae\u00af\3\2\2\2\u00af\u00dd\3\2\2\2\u00b0\u00b2\t\25"+
		"\2\2\u00b1\u00b0\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3"+
		"\u00b4\5!\21\2\u00b4\u00b6\5#\22\2\u00b5\u00b7\5\23\n\2\u00b6\u00b5\3"+
		"\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00dd\3\2\2\2\u00b8\u00ba\t\25\2\2\u00b9"+
		"\u00b8\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00bb\3\2\2\2\u00bb\u00bd\5!"+
		"\21\2\u00bc\u00be\5\23\n\2\u00bd\u00bc\3\2\2\2\u00bd\u00be\3\2\2\2\u00be"+
		"\u00dd\3\2\2\2\u00bf\u00c0\5!\21\2\u00c0\u00c1\7\60\2\2\u00c1\u00c3\3"+
		"\2\2\2\u00c2\u00bf\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\u00c2\3\2\2\2\u00c4"+
		"\u00c5\3\2\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00c8\5!\21\2\u00c7\u00c9\5\23"+
		"\n\2\u00c8\u00c7\3\2\2\2\u00c8\u00c9\3\2\2\2\u00c9\u00dd\3\2\2\2\u00ca"+
		"\u00cb\5!\21\2\u00cb\u00cc\7.\2\2\u00cc\u00ce\3\2\2\2\u00cd\u00ca\3\2"+
		"\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00cd\3\2\2\2\u00cf\u00d0\3\2\2\2\u00d0"+
		"\u00d1\3\2\2\2\u00d1\u00d3\5!\21\2\u00d2\u00d4\5\23\n\2\u00d3\u00d2\3"+
		"\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00dd\3\2\2\2\u00d5\u00d7\5!\21\2\u00d6"+
		"\u00d8\7\60\2\2\u00d7\u00d6\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00da\3"+
		"\2\2\2\u00d9\u00db\5\23\n\2\u00da\u00d9\3\2\2\2\u00da\u00db\3\2\2\2\u00db"+
		"\u00dd\3\2\2\2\u00dc\u00a5\3\2\2\2\u00dc\u00b1\3\2\2\2\u00dc\u00b9\3\2"+
		"\2\2\u00dc\u00c2\3\2\2\2\u00dc\u00cd\3\2\2\2\u00dc\u00d5\3\2\2\2\u00dd"+
		" \3\2\2\2\u00de\u00e0\5\3\2\2\u00df\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2"+
		"\u00e1\u00df\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\"\3\2\2\2\u00e3\u00e5\t"+
		"\27\2\2\u00e4\u00e6\t\25\2\2\u00e5\u00e4\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6"+
		"\u00e7\3\2\2\2\u00e7\u00e8\5!\21\2\u00e8$\3\2\2\2\u00e9\u00eb\t\30\2\2"+
		"\u00ea\u00e9\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ec\u00ed"+
		"\3\2\2\2\u00ed&\3\2\2\2\u00ee\u00ef\7j\2\2\u00ef\u00f0\7v\2\2\u00f0\u00f1"+
		"\7v\2\2\u00f1\u00f2\7r\2\2\u00f2\u00f3\7<\2\2\u00f3\u00f4\7\61\2\2\u00f4"+
		"\u00fe\7\61\2\2\u00f5\u00f6\7j\2\2\u00f6\u00f7\7v\2\2\u00f7\u00f8\7v\2"+
		"\2\u00f8\u00f9\7r\2\2\u00f9\u00fa\7u\2\2\u00fa\u00fb\7<\2\2\u00fb\u00fc"+
		"\7\61\2\2\u00fc\u00fe\7\61\2\2\u00fd\u00ee\3\2\2\2\u00fd\u00f5\3\2\2\2"+
		"\u00fe\u00ff\3\2\2\2\u00ff\u0118\5%\23\2\u0100\u0101\7j\2\2\u0101\u0102"+
		"\7v\2\2\u0102\u0103\7v\2\2\u0103\u0104\7r\2\2\u0104\u0105\7<\2\2\u0105"+
		"\u0106\7\61\2\2\u0106\u0110\7\61\2\2\u0107\u0108\7j\2\2\u0108\u0109\7"+
		"v\2\2\u0109\u010a\7v\2\2\u010a\u010b\7r\2\2\u010b\u010c\7u\2\2\u010c\u010d"+
		"\7<\2\2\u010d\u010e\7\61\2\2\u010e\u0110\7\61\2\2\u010f\u0100\3\2\2\2"+
		"\u010f\u0107\3\2\2\2\u010f\u0110\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0112"+
		"\7y\2\2\u0112\u0113\7y\2\2\u0113\u0114\7y\2\2\u0114\u0115\7\60\2\2\u0115"+
		"\u0116\3\2\2\2\u0116\u0118\5%\23\2\u0117\u00fd\3\2\2\2\u0117\u010f\3\2"+
		"\2\2\u0118(\3\2\2\2\u0119\u011b\5\r\7\2\u011a\u0119\3\2\2\2\u011b\u011c"+
		"\3\2\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011f\3\2\2\2\u011e"+
		"\u0120\7\60\2\2\u011f\u011e\3\2\2\2\u011f\u0120\3\2\2\2\u0120\u0122\3"+
		"\2\2\2\u0121\u0123\5\r\7\2\u0122\u0121\3\2\2\2\u0123\u0124\3\2\2\2\u0124"+
		"\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u0132\7B"+
		"\2\2\u0127\u0129\5\13\6\2\u0128\u0127\3\2\2\2\u0129\u012a\3\2\2\2\u012a"+
		"\u0128\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u012c\3\2\2\2\u012c\u012e\7\60"+
		"\2\2\u012d\u012f\5\13\6\2\u012e\u012d\3\2\2\2\u012f\u0130\3\2\2\2\u0130"+
		"\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131\u0133\3\2\2\2\u0132\u0128\3\2"+
		"\2\2\u0133\u0134\3\2\2\2\u0134\u0132\3\2\2\2\u0134\u0135\3\2\2\2\u0135"+
		"*\3\2\2\2\u0136\u0138\7%\2\2\u0137\u0139\5\13\6\2\u0138\u0137\3\2\2\2"+
		"\u0139\u013a\3\2\2\2\u013a\u0138\3\2\2\2\u013a\u013b\3\2\2\2\u013b,\3"+
		"\2\2\2\u013c\u013e\7B\2\2\u013d\u013f\5\13\6\2\u013e\u013d\3\2\2\2\u013f"+
		"\u0140\3\2\2\2\u0140\u013e\3\2\2\2\u0140\u0141\3\2\2\2\u0141.\3\2\2\2"+
		"\u0142\u0143\7<\2\2\u0143\u017d\7+\2\2\u0144\u0145\7<\2\2\u0145\u0146"+
		"\7/\2\2\u0146\u017d\7+\2\2\u0147\u0148\7<\2\2\u0148\u0149\7/\2\2\u0149"+
		"\u017d\7_\2\2\u014a\u014b\7<\2\2\u014b\u017d\7F\2\2\u014c\u014d\7<\2\2"+
		"\u014d\u014e\7/\2\2\u014e\u017d\7F\2\2\u014f\u0150\7:\2\2\u0150\u0151"+
		"\7/\2\2\u0151\u017d\7+\2\2\u0152\u0153\7=\2\2\u0153\u017d\7+\2\2\u0154"+
		"\u0155\7=\2\2\u0155\u0156\7\u2013\2\2\u0156\u017d\7+\2\2\u0157\u0158\7"+
		"<\2\2\u0158\u017d\7*\2\2\u0159\u015a\7<\2\2\u015a\u015b\7/\2\2\u015b\u017d"+
		"\7*\2\2\u015c\u015d\7<\2\2\u015d\u015e\7)\2\2\u015e\u017d\7*\2\2\u015f"+
		"\u0160\7<\2\2\u0160\u0161\7\u2013\2\2\u0161\u017d\7\61\2\2\u0162\u0163"+
		"\7<\2\2\u0163\u017d\7\61\2\2\u0164\u0165\7<\2\2\u0165\u0166\7`\2\2\u0166"+
		"\u017d\7+\2\2\u0167\u0168\7\u00b1\2\2\u0168\u0169\7^\2\2\u0169\u016a\7"+
		"a\2\2\u016a\u016b\7*\2\2\u016b\u016c\7\u30c6\2\2\u016c\u016d\7+\2\2\u016d"+
		"\u016e\7a\2\2\u016e\u016f\7\61\2\2\u016f\u017d\7\u00b1\2\2\u0170\u0171"+
		"\7Q\2\2\u0171\u0172\7a\2\2\u0172\u017d\7q\2\2\u0173\u0174\7q\2\2\u0174"+
		"\u0175\7a\2\2\u0175\u017d\7Q\2\2\u0176\u0177\7Q\2\2\u0177\u0178\7a\2\2"+
		"\u0178\u017d\7Q\2\2\u0179\u017a\7^\2\2\u017a\u017b\7q\2\2\u017b\u017d"+
		"\7\61\2\2\u017c\u0142\3\2\2\2\u017c\u0144\3\2\2\2\u017c\u0147\3\2\2\2"+
		"\u017c\u014a\3\2\2\2\u017c\u014c\3\2\2\2\u017c\u014f\3\2\2\2\u017c\u0152"+
		"\3\2\2\2\u017c\u0154\3\2\2\2\u017c\u0157\3\2\2\2\u017c\u0159\3\2\2\2\u017c"+
		"\u015c\3\2\2\2\u017c\u015f\3\2\2\2\u017c\u0162\3\2\2\2\u017c\u0164\3\2"+
		"\2\2\u017c\u0167\3\2\2\2\u017c\u0170\3\2\2\2\u017c\u0173\3\2\2\2\u017c"+
		"\u0176\3\2\2\2\u017c\u0179\3\2\2\2\u017d\60\3\2\2\2\u017e\u0193\7K\2\2"+
		"\u017f\u0180\7K\2\2\u0180\u0193\7K\2\2\u0181\u0182\7K\2\2\u0182\u0183"+
		"\7K\2\2\u0183\u0193\7K\2\2\u0184\u0185\7K\2\2\u0185\u0193\7X\2\2\u0186"+
		"\u0193\7X\2\2\u0187\u0188\7X\2\2\u0188\u0193\7K\2\2\u0189\u018a\7X\2\2"+
		"\u018a\u018b\7K\2\2\u018b\u0193\7K\2\2\u018c\u018d\7X\2\2\u018d\u018e"+
		"\7K\2\2\u018e\u018f\7K\2\2\u018f\u0193\7K\2\2\u0190\u0191\7K\2\2\u0191"+
		"\u0193\7Z\2\2\u0192\u017e\3\2\2\2\u0192\u017f\3\2\2\2\u0192\u0181\3\2"+
		"\2\2\u0192\u0184\3\2\2\2\u0192\u0186\3\2\2\2\u0192\u0187\3\2\2\2\u0192"+
		"\u0189\3\2\2\2\u0192\u018c\3\2\2\2\u0192\u0190\3\2\2\2\u0193\u0195\3\2"+
		"\2\2\u0194\u0196\7\60\2\2\u0195\u0194\3\2\2\2\u0195\u0196\3\2\2\2\u0196"+
		"\u0198\3\2\2\2\u0197\u0199\5\23\n\2\u0198\u0197\3\2\2\2\u0198\u0199\3"+
		"\2\2\2\u0199\62\3\2\2\2\u019a\u019b\5\7\4\2\u019b\u019c\7\60\2\2\u019c"+
		"\u019e\3\2\2\2\u019d\u019a\3\2\2\2\u019e\u019f\3\2\2\2\u019f\u019d\3\2"+
		"\2\2\u019f\u01a0\3\2\2\2\u01a0\u01a2\3\2\2\2\u01a1\u01a3\5\7\4\2\u01a2"+
		"\u01a1\3\2\2\2\u01a2\u01a3\3\2\2\2\u01a3\u01a5\3\2\2\2\u01a4\u01a6\5\23"+
		"\n\2\u01a5\u01a4\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\64\3\2\2\2\u01a7\u01a9"+
		"\5\t\5\2\u01a8\u01a7\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01a8\3\2\2\2\u01aa"+
		"\u01ab\3\2\2\2\u01ab\66\3\2\2\2\u01ac\u01ae\5\13\6\2\u01ad\u01ac\3\2\2"+
		"\2\u01ae\u01af\3\2\2\2\u01af\u01ad\3\2\2\2\u01af\u01b0\3\2\2\2\u01b08"+
		"\3\2\2\2\u01b1\u01b3\5\13\6\2\u01b2\u01b1\3\2\2\2\u01b3\u01b4\3\2\2\2"+
		"\u01b4\u01b2\3\2\2\2\u01b4\u01b5\3\2\2\2\u01b5\u01b7\3\2\2\2\u01b6\u01b8"+
		"\7/\2\2\u01b7\u01b6\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8\u01ba\3\2\2\2\u01b9"+
		"\u01bb\5\13\6\2\u01ba\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01ba\3"+
		"\2\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01bf\3\2\2\2\u01be\u01c0\5\23\n\2\u01bf"+
		"\u01be\3\2\2\2\u01bf\u01c0\3\2\2\2\u01c0:\3\2\2\2\u01c1\u01cf\5\17\b\2"+
		"\u01c2\u01cf\5\21\t\2\u01c3\u01cf\t\31\2\2\u01c4\u01c5\7\60\2\2\u01c5"+
		"\u01c6\7\60\2\2\u01c6\u01cf\7\60\2\2\u01c7\u01c8\7*\2\2\u01c8\u01c9\7"+
		"#\2\2\u01c9\u01cf\7+\2\2\u01ca\u01cb\7*\2\2\u01cb\u01cc\7A\2\2\u01cc\u01cf"+
		"\7+\2\2\u01cd\u01cf\t\32\2\2\u01ce\u01c1\3\2\2\2\u01ce\u01c2\3\2\2\2\u01ce"+
		"\u01c3\3\2\2\2\u01ce\u01c4\3\2\2\2\u01ce\u01c7\3\2\2\2\u01ce\u01ca\3\2"+
		"\2\2\u01ce\u01cd\3\2\2\2\u01cf<\3\2\2\2\u01d0\u01d1\5;\36\2\u01d1>\3\2"+
		"\2\2\u01d2\u01d4\n\33\2\2\u01d3\u01d2\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5"+
		"\u01d3\3\2\2\2\u01d5\u01d6\3\2\2\2\u01d6@\3\2\2\2\u01d7\u01d9\13\2\2\2"+
		"\u01d8\u01d7\3\2\2\2\u01d9\u01da\3\2\2\2\u01da\u01db\3\2\2\2\u01da\u01d8"+
		"\3\2\2\2\u01dbB\3\2\2\2<\2W\\hkns\u0081\u0084\u0087\u008c\u009a\u009d"+
		"\u009f\u00a5\u00ab\u00ae\u00b1\u00b6\u00b9\u00bd\u00c4\u00c8\u00cf\u00d3"+
		"\u00d7\u00da\u00dc\u00e1\u00e5\u00ec\u00fd\u010f\u0117\u011c\u011f\u0124"+
		"\u012a\u0130\u0134\u013a\u0140\u017c\u0192\u0195\u0198\u019f\u01a2\u01a5"+
		"\u01aa\u01af\u01b4\u01b7\u01bc\u01bf\u01ce\u01d5\u01da\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
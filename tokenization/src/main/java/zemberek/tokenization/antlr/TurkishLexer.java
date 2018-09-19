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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\26\u01d6\b\1\4\2"+
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
		"\n\20\3\20\5\20\u00ba\n\20\3\20\3\20\5\20\u00be\n\20\3\20\5\20\u00c1\n"+
		"\20\3\20\3\20\3\20\3\20\5\20\u00c7\n\20\3\20\3\20\3\20\6\20\u00cc\n\20"+
		"\r\20\16\20\u00cd\3\20\3\20\5\20\u00d2\n\20\3\20\3\20\3\20\6\20\u00d7"+
		"\n\20\r\20\16\20\u00d8\3\20\3\20\5\20\u00dd\n\20\3\20\3\20\5\20\u00e1"+
		"\n\20\3\20\5\20\u00e4\n\20\5\20\u00e6\n\20\3\21\6\21\u00e9\n\21\r\21\16"+
		"\21\u00ea\3\22\3\22\5\22\u00ef\n\22\3\22\3\22\3\23\6\23\u00f4\n\23\r\23"+
		"\16\23\u00f5\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\5\24\u0107\n\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0119\n\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\5\24\u0121\n\24\3\25\6\25\u0124\n\25\r\25\16\25\u0125"+
		"\3\25\5\25\u0129\n\25\3\25\6\25\u012c\n\25\r\25\16\25\u012d\3\25\3\25"+
		"\6\25\u0132\n\25\r\25\16\25\u0133\3\25\3\25\6\25\u0138\n\25\r\25\16\25"+
		"\u0139\6\25\u013c\n\25\r\25\16\25\u013d\3\26\3\26\6\26\u0142\n\26\r\26"+
		"\16\26\u0143\3\27\3\27\6\27\u0148\n\27\r\27\16\27\u0149\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\5\30\u0188\n\30\3\31\6\31\u018b\n\31\r\31\16\31\u018c\3\31\5\31"+
		"\u0190\n\31\3\31\5\31\u0193\n\31\3\32\3\32\3\32\6\32\u0198\n\32\r\32\16"+
		"\32\u0199\3\32\5\32\u019d\n\32\3\32\5\32\u01a0\n\32\3\33\6\33\u01a3\n"+
		"\33\r\33\16\33\u01a4\3\34\6\34\u01a8\n\34\r\34\16\34\u01a9\3\35\6\35\u01ad"+
		"\n\35\r\35\16\35\u01ae\3\35\5\35\u01b2\n\35\3\35\6\35\u01b5\n\35\r\35"+
		"\16\35\u01b6\3\35\5\35\u01ba\n\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3"+
		"\36\3\36\3\36\3\36\3\36\3\36\5\36\u01c9\n\36\3\37\3\37\3 \6 \u01ce\n "+
		"\r \16 \u01cf\3!\6!\u01d3\n!\r!\16!\u01d4\3\u01d4\2\"\3\2\5\2\7\2\t\2"+
		"\13\2\r\2\17\2\21\2\23\2\25\4\27\5\31\6\33\7\35\b\37\t!\2#\2%\2\'\n)\13"+
		"+\f-\r/\16\61\17\63\20\65\21\67\229\23;\2=\24?\25A\26\3\2\35\3\2\62;\13"+
		"\2c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0121"+
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
		"\u0121\u0132\u0133\u0160\u0161\7\2EFKKNOXXZZ\4\2\u201a\u201a\u2028\u2028"+
		"\n\2##&(*\61<=AB]_}}\177\177\20\2\13\f\17\17\"$&\61<=AB]_}}\177\177\u00ad"+
		"\u00ad\u00bd\u00bd\u201a\u201b\u201e\u201f\u2028\u2028\2\u0222\2\25\3"+
		"\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2"+
		"\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2"+
		"\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2=\3\2\2\2\2?\3\2"+
		"\2\2\2A\3\2\2\2\3C\3\2\2\2\5E\3\2\2\2\7G\3\2\2\2\tI\3\2\2\2\13K\3\2\2"+
		"\2\rM\3\2\2\2\17O\3\2\2\2\21Q\3\2\2\2\23S\3\2\2\2\25Z\3\2\2\2\27^\3\2"+
		"\2\2\31`\3\2\2\2\33\u009f\3\2\2\2\35\u00a1\3\2\2\2\37\u00e5\3\2\2\2!\u00e8"+
		"\3\2\2\2#\u00ec\3\2\2\2%\u00f3\3\2\2\2\'\u0120\3\2\2\2)\u0123\3\2\2\2"+
		"+\u013f\3\2\2\2-\u0145\3\2\2\2/\u0187\3\2\2\2\61\u018a\3\2\2\2\63\u0197"+
		"\3\2\2\2\65\u01a2\3\2\2\2\67\u01a7\3\2\2\29\u01ac\3\2\2\2;\u01c8\3\2\2"+
		"\2=\u01ca\3\2\2\2?\u01cd\3\2\2\2A\u01d2\3\2\2\2CD\t\2\2\2D\4\3\2\2\2E"+
		"F\t\3\2\2F\6\3\2\2\2GH\t\4\2\2H\b\3\2\2\2IJ\t\5\2\2J\n\3\2\2\2KL\t\6\2"+
		"\2L\f\3\2\2\2MN\t\7\2\2N\16\3\2\2\2OP\t\b\2\2P\20\3\2\2\2QR\t\t\2\2R\22"+
		"\3\2\2\2SU\5\17\b\2TV\5\t\5\2UT\3\2\2\2VW\3\2\2\2WU\3\2\2\2WX\3\2\2\2"+
		"X\24\3\2\2\2Y[\t\n\2\2ZY\3\2\2\2[\\\3\2\2\2\\Z\3\2\2\2\\]\3\2\2\2]\26"+
		"\3\2\2\2^_\t\13\2\2_\30\3\2\2\2`a\t\f\2\2ab\t\2\2\2bc\t\r\2\2cd\t\16\2"+
		"\2dh\t\2\2\2ef\t\r\2\2fg\t\16\2\2gi\t\2\2\2he\3\2\2\2hi\3\2\2\2ik\3\2"+
		"\2\2jl\5\23\n\2kj\3\2\2\2kl\3\2\2\2l\32\3\2\2\2mo\t\17\2\2nm\3\2\2\2n"+
		"o\3\2\2\2op\3\2\2\2pq\t\2\2\2qs\7\60\2\2rt\t\20\2\2sr\3\2\2\2st\3\2\2"+
		"\2tu\3\2\2\2uv\t\2\2\2v\u0081\7\60\2\2wx\t\21\2\2xy\t\22\2\2yz\t\2\2\2"+
		"z\u0082\t\2\2\2{|\t\23\2\2|}\t\24\2\2}~\t\2\2\2~\u0082\t\2\2\2\177\u0080"+
		"\t\2\2\2\u0080\u0082\t\2\2\2\u0081w\3\2\2\2\u0081{\3\2\2\2\u0081\177\3"+
		"\2\2\2\u0082\u0084\3\2\2\2\u0083\u0085\5\23\n\2\u0084\u0083\3\2\2\2\u0084"+
		"\u0085\3\2\2\2\u0085\u00a0\3\2\2\2\u0086\u0088\t\17\2\2\u0087\u0086\3"+
		"\2\2\2\u0087\u0088\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u008a\t\2\2\2\u008a"+
		"\u008c\7\61\2\2\u008b\u008d\t\20\2\2\u008c\u008b\3\2\2\2\u008c\u008d\3"+
		"\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f\t\2\2\2\u008f\u009a\7\61\2\2\u0090"+
		"\u0091\t\21\2\2\u0091\u0092\t\22\2\2\u0092\u0093\t\2\2\2\u0093\u009b\t"+
		"\2\2\2\u0094\u0095\t\23\2\2\u0095\u0096\t\24\2\2\u0096\u0097\t\2\2\2\u0097"+
		"\u009b\t\2\2\2\u0098\u0099\t\2\2\2\u0099\u009b\t\2\2\2\u009a\u0090\3\2"+
		"\2\2\u009a\u0094\3\2\2\2\u009a\u0098\3\2\2\2\u009b\u009d\3\2\2\2\u009c"+
		"\u009e\5\23\n\2\u009d\u009c\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u00a0\3"+
		"\2\2\2\u009fn\3\2\2\2\u009f\u0087\3\2\2\2\u00a0\34\3\2\2\2\u00a1\u00a2"+
		"\7\'\2\2\u00a2\u00a3\5\37\20\2\u00a3\36\3\2\2\2\u00a4\u00a6\t\25\2\2\u00a5"+
		"\u00a4\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a8\5!"+
		"\21\2\u00a8\u00a9\t\26\2\2\u00a9\u00ab\5!\21\2\u00aa\u00ac\5#\22\2\u00ab"+
		"\u00aa\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad\u00af\5\23"+
		"\n\2\u00ae\u00ad\3\2\2\2\u00ae\u00af\3\2\2\2\u00af\u00e6\3\2\2\2\u00b0"+
		"\u00b2\t\25\2\2\u00b1\u00b0\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b3\3"+
		"\2\2\2\u00b3\u00b4\5!\21\2\u00b4\u00b6\5#\22\2\u00b5\u00b7\5\23\n\2\u00b6"+
		"\u00b5\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00e6\3\2\2\2\u00b8\u00ba\t\25"+
		"\2\2\u00b9\u00b8\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00bb\3\2\2\2\u00bb"+
		"\u00bd\5!\21\2\u00bc\u00be\5\23\n\2\u00bd\u00bc\3\2\2\2\u00bd\u00be\3"+
		"\2\2\2\u00be\u00e6\3\2\2\2\u00bf\u00c1\t\25\2\2\u00c0\u00bf\3\2\2\2\u00c0"+
		"\u00c1\3\2\2\2\u00c1\u00c2\3\2\2\2\u00c2\u00c3\5!\21\2\u00c3\u00c4\7\61"+
		"\2\2\u00c4\u00c6\5!\21\2\u00c5\u00c7\5\23\n\2\u00c6\u00c5\3\2\2\2\u00c6"+
		"\u00c7\3\2\2\2\u00c7\u00e6\3\2\2\2\u00c8\u00c9\5!\21\2\u00c9\u00ca\7\60"+
		"\2\2\u00ca\u00cc\3\2\2\2\u00cb\u00c8\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd"+
		"\u00cb\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00d1\5!"+
		"\21\2\u00d0\u00d2\5\23\n\2\u00d1\u00d0\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2"+
		"\u00e6\3\2\2\2\u00d3\u00d4\5!\21\2\u00d4\u00d5\7.\2\2\u00d5\u00d7\3\2"+
		"\2\2\u00d6\u00d3\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00d6\3\2\2\2\u00d8"+
		"\u00d9\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00dc\5!\21\2\u00db\u00dd\5\23"+
		"\n\2\u00dc\u00db\3\2\2\2\u00dc\u00dd\3\2\2\2\u00dd\u00e6\3\2\2\2\u00de"+
		"\u00e0\5!\21\2\u00df\u00e1\7\60\2\2\u00e0\u00df\3\2\2\2\u00e0\u00e1\3"+
		"\2\2\2\u00e1\u00e3\3\2\2\2\u00e2\u00e4\5\23\n\2\u00e3\u00e2\3\2\2\2\u00e3"+
		"\u00e4\3\2\2\2\u00e4\u00e6\3\2\2\2\u00e5\u00a5\3\2\2\2\u00e5\u00b1\3\2"+
		"\2\2\u00e5\u00b9\3\2\2\2\u00e5\u00c0\3\2\2\2\u00e5\u00cb\3\2\2\2\u00e5"+
		"\u00d6\3\2\2\2\u00e5\u00de\3\2\2\2\u00e6 \3\2\2\2\u00e7\u00e9\5\3\2\2"+
		"\u00e8\u00e7\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00eb"+
		"\3\2\2\2\u00eb\"\3\2\2\2\u00ec\u00ee\t\27\2\2\u00ed\u00ef\t\25\2\2\u00ee"+
		"\u00ed\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00f1\5!"+
		"\21\2\u00f1$\3\2\2\2\u00f2\u00f4\t\30\2\2\u00f3\u00f2\3\2\2\2\u00f4\u00f5"+
		"\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f6\3\2\2\2\u00f6&\3\2\2\2\u00f7"+
		"\u00f8\7j\2\2\u00f8\u00f9\7v\2\2\u00f9\u00fa\7v\2\2\u00fa\u00fb\7r\2\2"+
		"\u00fb\u00fc\7<\2\2\u00fc\u00fd\7\61\2\2\u00fd\u0107\7\61\2\2\u00fe\u00ff"+
		"\7j\2\2\u00ff\u0100\7v\2\2\u0100\u0101\7v\2\2\u0101\u0102\7r\2\2\u0102"+
		"\u0103\7u\2\2\u0103\u0104\7<\2\2\u0104\u0105\7\61\2\2\u0105\u0107\7\61"+
		"\2\2\u0106\u00f7\3\2\2\2\u0106\u00fe\3\2\2\2\u0107\u0108\3\2\2\2\u0108"+
		"\u0121\5%\23\2\u0109\u010a\7j\2\2\u010a\u010b\7v\2\2\u010b\u010c\7v\2"+
		"\2\u010c\u010d\7r\2\2\u010d\u010e\7<\2\2\u010e\u010f\7\61\2\2\u010f\u0119"+
		"\7\61\2\2\u0110\u0111\7j\2\2\u0111\u0112\7v\2\2\u0112\u0113\7v\2\2\u0113"+
		"\u0114\7r\2\2\u0114\u0115\7u\2\2\u0115\u0116\7<\2\2\u0116\u0117\7\61\2"+
		"\2\u0117\u0119\7\61\2\2\u0118\u0109\3\2\2\2\u0118\u0110\3\2\2\2\u0118"+
		"\u0119\3\2\2\2\u0119\u011a\3\2\2\2\u011a\u011b\7y\2\2\u011b\u011c\7y\2"+
		"\2\u011c\u011d\7y\2\2\u011d\u011e\7\60\2\2\u011e\u011f\3\2\2\2\u011f\u0121"+
		"\5%\23\2\u0120\u0106\3\2\2\2\u0120\u0118\3\2\2\2\u0121(\3\2\2\2\u0122"+
		"\u0124\5\r\7\2\u0123\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0123\3\2"+
		"\2\2\u0125\u0126\3\2\2\2\u0126\u0128\3\2\2\2\u0127\u0129\7\60\2\2\u0128"+
		"\u0127\3\2\2\2\u0128\u0129\3\2\2\2\u0129\u012b\3\2\2\2\u012a\u012c\5\r"+
		"\7\2\u012b\u012a\3\2\2\2\u012c\u012d\3\2\2\2\u012d\u012b\3\2\2\2\u012d"+
		"\u012e\3\2\2\2\u012e\u012f\3\2\2\2\u012f\u013b\7B\2\2\u0130\u0132\5\13"+
		"\6\2\u0131\u0130\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0131\3\2\2\2\u0133"+
		"\u0134\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0137\7\60\2\2\u0136\u0138\5"+
		"\13\6\2\u0137\u0136\3\2\2\2\u0138\u0139\3\2\2\2\u0139\u0137\3\2\2\2\u0139"+
		"\u013a\3\2\2\2\u013a\u013c\3\2\2\2\u013b\u0131\3\2\2\2\u013c\u013d\3\2"+
		"\2\2\u013d\u013b\3\2\2\2\u013d\u013e\3\2\2\2\u013e*\3\2\2\2\u013f\u0141"+
		"\7%\2\2\u0140\u0142\5\13\6\2\u0141\u0140\3\2\2\2\u0142\u0143\3\2\2\2\u0143"+
		"\u0141\3\2\2\2\u0143\u0144\3\2\2\2\u0144,\3\2\2\2\u0145\u0147\7B\2\2\u0146"+
		"\u0148\5\13\6\2\u0147\u0146\3\2\2\2\u0148\u0149\3\2\2\2\u0149\u0147\3"+
		"\2\2\2\u0149\u014a\3\2\2\2\u014a.\3\2\2\2\u014b\u014c\7<\2\2\u014c\u0188"+
		"\7+\2\2\u014d\u014e\7<\2\2\u014e\u014f\7/\2\2\u014f\u0188\7+\2\2\u0150"+
		"\u0151\7<\2\2\u0151\u0152\7/\2\2\u0152\u0188\7_\2\2\u0153\u0154\7<\2\2"+
		"\u0154\u0188\7F\2\2\u0155\u0156\7<\2\2\u0156\u0157\7/\2\2\u0157\u0188"+
		"\7F\2\2\u0158\u0159\7:\2\2\u0159\u015a\7/\2\2\u015a\u0188\7+\2\2\u015b"+
		"\u015c\7=\2\2\u015c\u0188\7+\2\2\u015d\u015e\7=\2\2\u015e\u015f\7\u2013"+
		"\2\2\u015f\u0188\7+\2\2\u0160\u0161\7<\2\2\u0161\u0188\7*\2\2\u0162\u0163"+
		"\7<\2\2\u0163\u0164\7/\2\2\u0164\u0188\7*\2\2\u0165\u0166\7<\2\2\u0166"+
		"\u0167\7)\2\2\u0167\u0188\7*\2\2\u0168\u0169\7<\2\2\u0169\u016a\7\u2013"+
		"\2\2\u016a\u0188\7\61\2\2\u016b\u016c\7<\2\2\u016c\u0188\7\61\2\2\u016d"+
		"\u016e\7<\2\2\u016e\u016f\7`\2\2\u016f\u0188\7+\2\2\u0170\u0171\7\u00b1"+
		"\2\2\u0171\u0172\7^\2\2\u0172\u0173\7a\2\2\u0173\u0174\7*\2\2\u0174\u0175"+
		"\7\u30c6\2\2\u0175\u0176\7+\2\2\u0176\u0177\7a\2\2\u0177\u0178\7\61\2"+
		"\2\u0178\u0188\7\u00b1\2\2\u0179\u017a\7Q\2\2\u017a\u017b\7a\2\2\u017b"+
		"\u0188\7q\2\2\u017c\u017d\7q\2\2\u017d\u017e\7a\2\2\u017e\u0188\7Q\2\2"+
		"\u017f\u0180\7Q\2\2\u0180\u0181\7a\2\2\u0181\u0188\7Q\2\2\u0182\u0183"+
		"\7^\2\2\u0183\u0184\7q\2\2\u0184\u0188\7\61\2\2\u0185\u0186\7>\2\2\u0186"+
		"\u0188\7\65\2\2\u0187\u014b\3\2\2\2\u0187\u014d\3\2\2\2\u0187\u0150\3"+
		"\2\2\2\u0187\u0153\3\2\2\2\u0187\u0155\3\2\2\2\u0187\u0158\3\2\2\2\u0187"+
		"\u015b\3\2\2\2\u0187\u015d\3\2\2\2\u0187\u0160\3\2\2\2\u0187\u0162\3\2"+
		"\2\2\u0187\u0165\3\2\2\2\u0187\u0168\3\2\2\2\u0187\u016b\3\2\2\2\u0187"+
		"\u016d\3\2\2\2\u0187\u0170\3\2\2\2\u0187\u0179\3\2\2\2\u0187\u017c\3\2"+
		"\2\2\u0187\u017f\3\2\2\2\u0187\u0182\3\2\2\2\u0187\u0185\3\2\2\2\u0188"+
		"\60\3\2\2\2\u0189\u018b\t\31\2\2\u018a\u0189\3\2\2\2\u018b\u018c\3\2\2"+
		"\2\u018c\u018a\3\2\2\2\u018c\u018d\3\2\2\2\u018d\u018f\3\2\2\2\u018e\u0190"+
		"\7\60\2\2\u018f\u018e\3\2\2\2\u018f\u0190\3\2\2\2\u0190\u0192\3\2\2\2"+
		"\u0191\u0193\5\23\n\2\u0192\u0191\3\2\2\2\u0192\u0193\3\2\2\2\u0193\62"+
		"\3\2\2\2\u0194\u0195\5\7\4\2\u0195\u0196\7\60\2\2\u0196\u0198\3\2\2\2"+
		"\u0197\u0194\3\2\2\2\u0198\u0199\3\2\2\2\u0199\u0197\3\2\2\2\u0199\u019a"+
		"\3\2\2\2\u019a\u019c\3\2\2\2\u019b\u019d\5\7\4\2\u019c\u019b\3\2\2\2\u019c"+
		"\u019d\3\2\2\2\u019d\u019f\3\2\2\2\u019e\u01a0\5\23\n\2\u019f\u019e\3"+
		"\2\2\2\u019f\u01a0\3\2\2\2\u01a0\64\3\2\2\2\u01a1\u01a3\5\t\5\2\u01a2"+
		"\u01a1\3\2\2\2\u01a3\u01a4\3\2\2\2\u01a4\u01a2\3\2\2\2\u01a4\u01a5\3\2"+
		"\2\2\u01a5\66\3\2\2\2\u01a6\u01a8\5\13\6\2\u01a7\u01a6\3\2\2\2\u01a8\u01a9"+
		"\3\2\2\2\u01a9\u01a7\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa8\3\2\2\2\u01ab"+
		"\u01ad\5\13\6\2\u01ac\u01ab\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01ac\3"+
		"\2\2\2\u01ae\u01af\3\2\2\2\u01af\u01b1\3\2\2\2\u01b0\u01b2\7/\2\2\u01b1"+
		"\u01b0\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b2\u01b4\3\2\2\2\u01b3\u01b5\5\13"+
		"\6\2\u01b4\u01b3\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\u01b4\3\2\2\2\u01b6"+
		"\u01b7\3\2\2\2\u01b7\u01b9\3\2\2\2\u01b8\u01ba\5\23\n\2\u01b9\u01b8\3"+
		"\2\2\2\u01b9\u01ba\3\2\2\2\u01ba:\3\2\2\2\u01bb\u01c9\5\17\b\2\u01bc\u01c9"+
		"\5\21\t\2\u01bd\u01c9\t\32\2\2\u01be\u01bf\7\60\2\2\u01bf\u01c0\7\60\2"+
		"\2\u01c0\u01c9\7\60\2\2\u01c1\u01c2\7*\2\2\u01c2\u01c3\7#\2\2\u01c3\u01c9"+
		"\7+\2\2\u01c4\u01c5\7*\2\2\u01c5\u01c6\7A\2\2\u01c6\u01c9\7+\2\2\u01c7"+
		"\u01c9\t\33\2\2\u01c8\u01bb\3\2\2\2\u01c8\u01bc\3\2\2\2\u01c8\u01bd\3"+
		"\2\2\2\u01c8\u01be\3\2\2\2\u01c8\u01c1\3\2\2\2\u01c8\u01c4\3\2\2\2\u01c8"+
		"\u01c7\3\2\2\2\u01c9<\3\2\2\2\u01ca\u01cb\5;\36\2\u01cb>\3\2\2\2\u01cc"+
		"\u01ce\n\34\2\2\u01cd\u01cc\3\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01cd\3"+
		"\2\2\2\u01cf\u01d0\3\2\2\2\u01d0@\3\2\2\2\u01d1\u01d3\13\2\2\2\u01d2\u01d1"+
		"\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d4\u01d2\3\2\2\2\u01d5"+
		"B\3\2\2\2>\2W\\hkns\u0081\u0084\u0087\u008c\u009a\u009d\u009f\u00a5\u00ab"+
		"\u00ae\u00b1\u00b6\u00b9\u00bd\u00c0\u00c6\u00cd\u00d1\u00d8\u00dc\u00e0"+
		"\u00e3\u00e5\u00ea\u00ee\u00f5\u0106\u0118\u0120\u0125\u0128\u012d\u0133"+
		"\u0139\u013d\u0143\u0149\u0187\u018c\u018f\u0192\u0199\u019c\u019f\u01a4"+
		"\u01a9\u01ae\u01b1\u01b6\u01b9\u01c8\u01cf\u01d4\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
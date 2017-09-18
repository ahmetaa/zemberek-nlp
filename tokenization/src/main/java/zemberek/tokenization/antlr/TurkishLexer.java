// Generated from /home/aaa/projects/zemberek-nlp/tokenization/src/main/resources/tokenization/TurkishLexer.g4 by ANTLR 4.7
package zemberek.tokenization.antlr;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TurkishLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		Abbreviation=1, SpaceTab=2, NewLine=3, Time=4, Date=5, PercentNumeral=6, 
		Number=7, URL=8, Email=9, HashTag=10, Mention=11, Emoticon=12, RomanNumeral=13, 
		AbbreviationWithDots=14, Word=15, WordWithApostrophe=16, Punctuation=17, 
		UnknownWord=18, Unknown=19;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"Digit", "TurkishLetters", "TurkishLettersCapital", "TurkishLettersAll", 
		"AllTurkishAlphanumerical", "Apostrophe", "DoubleQuote", "AposAndSuffix", 
		"SpaceTab", "NewLine", "Time", "Date", "PercentNumeral", "Number", "Integer", 
		"Exp", "URLFragment", "URLFragmentWithDot", "URL", "Email", "HashTag", 
		"Mention", "Emoticon", "RomanNumeral", "AbbreviationWithDots", "Word", 
		"WordWithApostrophe", "Punctuation", "UnknownWord", "Unknown"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "Abbreviation", "SpaceTab", "NewLine", "Time", "Date", "PercentNumeral", 
		"Number", "URL", "Email", "HashTag", "Mention", "Emoticon", "RomanNumeral", 
		"AbbreviationWithDots", "Word", "WordWithApostrophe", "Punctuation", "UnknownWord", 
		"Unknown"
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
		//_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\25\u01be\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\3\2"+
		"\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\6\tP\n\t"+
		"\r\t\16\tQ\3\n\6\nU\n\n\r\n\16\nV\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3"+
		"\f\3\f\5\fc\n\f\3\f\5\ff\n\f\3\r\5\ri\n\r\3\r\3\r\3\r\5\rn\n\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r|\n\r\3\r\5\r\177\n\r\3\r"+
		"\5\r\u0082\n\r\3\r\3\r\3\r\5\r\u0087\n\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\5\r\u0095\n\r\3\r\5\r\u0098\n\r\5\r\u009a\n\r\3\16"+
		"\3\16\3\16\3\17\5\17\u00a0\n\17\3\17\3\17\3\17\3\17\5\17\u00a6\n\17\3"+
		"\17\5\17\u00a9\n\17\3\17\5\17\u00ac\n\17\3\17\3\17\3\17\5\17\u00b1\n\17"+
		"\3\17\5\17\u00b4\n\17\3\17\3\17\5\17\u00b8\n\17\3\17\3\17\3\17\6\17\u00bd"+
		"\n\17\r\17\16\17\u00be\3\17\3\17\5\17\u00c3\n\17\3\17\3\17\3\17\6\17\u00c8"+
		"\n\17\r\17\16\17\u00c9\3\17\3\17\5\17\u00ce\n\17\3\17\3\17\5\17\u00d2"+
		"\n\17\3\17\5\17\u00d5\n\17\5\17\u00d7\n\17\3\20\6\20\u00da\n\20\r\20\16"+
		"\20\u00db\3\21\3\21\5\21\u00e0\n\21\3\21\3\21\3\22\6\22\u00e5\n\22\r\22"+
		"\16\22\u00e6\3\23\3\23\6\23\u00eb\n\23\r\23\16\23\u00ec\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u00fe"+
		"\n\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\6\24\u0107\n\24\r\24\16\24\u0108"+
		"\3\25\6\25\u010c\n\25\r\25\16\25\u010d\3\25\5\25\u0111\n\25\3\25\6\25"+
		"\u0114\n\25\r\25\16\25\u0115\3\25\3\25\6\25\u011a\n\25\r\25\16\25\u011b"+
		"\3\25\3\25\6\25\u0120\n\25\r\25\16\25\u0121\6\25\u0124\n\25\r\25\16\25"+
		"\u0125\3\26\3\26\6\26\u012a\n\26\r\26\16\26\u012b\3\27\3\27\6\27\u0130"+
		"\n\27\r\27\16\27\u0131\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3"+
		"\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3"+
		"\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3"+
		"\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3"+
		"\30\3\30\3\30\3\30\3\30\3\30\3\30\5\30\u016e\n\30\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\5\31\u0184\n\31\3\31\5\31\u0187\n\31\3\31\5\31\u018a\n\31\3"+
		"\32\3\32\3\32\6\32\u018f\n\32\r\32\16\32\u0190\3\32\5\32\u0194\n\32\3"+
		"\32\5\32\u0197\n\32\3\33\6\33\u019a\n\33\r\33\16\33\u019b\3\34\6\34\u019f"+
		"\n\34\r\34\16\34\u01a0\3\34\5\34\u01a4\n\34\3\35\3\35\3\35\3\35\3\35\3"+
		"\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\5\35\u01b3\n\35\3\36\6\36\u01b6"+
		"\n\36\r\36\16\36\u01b7\3\37\6\37\u01bb\n\37\r\37\16\37\u01bc\3\u01bc\2"+
		" \3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\4\25\5\27\6\31\7\33\b\35\t\37"+
		"\2!\2#\2%\2\'\n)\13+\f-\r/\16\61\17\63\20\65\21\67\229\23;\24=\25\3\2"+
		"\34\3\2\62;\13\2c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd"+
		"\u00fe\u0121\u0121\u0133\u0133\u0161\u0161\13\2C\\\u00c4\u00c4\u00c9\u00c9"+
		"\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132\u0160\u0160"+
		"\21\2C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de"+
		"\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121"+
		"\u0132\u0133\u0160\u0161\23\2//\62;C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0"+
		"\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8"+
		"\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\4\2))\u201b\u201b"+
		"\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e\u201f\4\2\13\13\"\"\4\2\f\f\17\17"+
		"\3\2\62\64\4\2\60\60<<\3\2\62\67\3\2\62\65\3\2\62\63\3\2\63\63\3\29;\3"+
		"\2\64\64\3\2\62\62\4\2--//\4\2..\60\60\4\2GGgg\23\2\62;C\\^ac|\u00c4\u00c4"+
		"\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9"+
		"\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161"+
		"\30\2((--\61;==??AAC\\^ac|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8"+
		"\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe"+
		"\u0120\u0121\u0132\u0133\u0160\u0161\4\2\u201a\u201a\u2028\u2028\n\2#"+
		"#&(*\60<=AB]_}}\177\177\17\2\13\f\17\17\"$&\60<=AB]_}}\177\177\u00ad\u00ad"+
		"\u00bd\u00bd\u201a\u201b\u201e\u201f\2\u020b\2\23\3\2\2\2\2\25\3\2\2\2"+
		"\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\'\3\2\2\2\2)\3"+
		"\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65"+
		"\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\3?\3\2\2\2\5A\3"+
		"\2\2\2\7C\3\2\2\2\tE\3\2\2\2\13G\3\2\2\2\rI\3\2\2\2\17K\3\2\2\2\21M\3"+
		"\2\2\2\23T\3\2\2\2\25X\3\2\2\2\27Z\3\2\2\2\31\u0099\3\2\2\2\33\u009b\3"+
		"\2\2\2\35\u00d6\3\2\2\2\37\u00d9\3\2\2\2!\u00dd\3\2\2\2#\u00e4\3\2\2\2"+
		"%\u00e8\3\2\2\2\'\u00fd\3\2\2\2)\u010b\3\2\2\2+\u0127\3\2\2\2-\u012d\3"+
		"\2\2\2/\u016d\3\2\2\2\61\u0183\3\2\2\2\63\u018e\3\2\2\2\65\u0199\3\2\2"+
		"\2\67\u019e\3\2\2\29\u01b2\3\2\2\2;\u01b5\3\2\2\2=\u01ba\3\2\2\2?@\t\2"+
		"\2\2@\4\3\2\2\2AB\t\3\2\2B\6\3\2\2\2CD\t\4\2\2D\b\3\2\2\2EF\t\5\2\2F\n"+
		"\3\2\2\2GH\t\6\2\2H\f\3\2\2\2IJ\t\7\2\2J\16\3\2\2\2KL\t\b\2\2L\20\3\2"+
		"\2\2MO\5\r\7\2NP\5\t\5\2ON\3\2\2\2PQ\3\2\2\2QO\3\2\2\2QR\3\2\2\2R\22\3"+
		"\2\2\2SU\t\t\2\2TS\3\2\2\2UV\3\2\2\2VT\3\2\2\2VW\3\2\2\2W\24\3\2\2\2X"+
		"Y\t\n\2\2Y\26\3\2\2\2Z[\t\13\2\2[\\\t\2\2\2\\]\t\f\2\2]^\t\r\2\2^b\t\2"+
		"\2\2_`\t\f\2\2`a\t\r\2\2ac\t\2\2\2b_\3\2\2\2bc\3\2\2\2ce\3\2\2\2df\5\21"+
		"\t\2ed\3\2\2\2ef\3\2\2\2f\30\3\2\2\2gi\t\16\2\2hg\3\2\2\2hi\3\2\2\2ij"+
		"\3\2\2\2jk\t\2\2\2km\7\60\2\2ln\t\17\2\2ml\3\2\2\2mn\3\2\2\2no\3\2\2\2"+
		"op\t\2\2\2p{\7\60\2\2qr\t\20\2\2rs\t\21\2\2st\t\2\2\2t|\t\2\2\2uv\t\22"+
		"\2\2vw\t\23\2\2wx\t\2\2\2x|\t\2\2\2yz\t\2\2\2z|\t\2\2\2{q\3\2\2\2{u\3"+
		"\2\2\2{y\3\2\2\2|~\3\2\2\2}\177\5\21\t\2~}\3\2\2\2~\177\3\2\2\2\177\u009a"+
		"\3\2\2\2\u0080\u0082\t\16\2\2\u0081\u0080\3\2\2\2\u0081\u0082\3\2\2\2"+
		"\u0082\u0083\3\2\2\2\u0083\u0084\t\2\2\2\u0084\u0086\7\61\2\2\u0085\u0087"+
		"\t\17\2\2\u0086\u0085\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0088\3\2\2\2"+
		"\u0088\u0089\t\2\2\2\u0089\u0094\7\61\2\2\u008a\u008b\t\20\2\2\u008b\u008c"+
		"\t\21\2\2\u008c\u008d\t\2\2\2\u008d\u0095\t\2\2\2\u008e\u008f\t\22\2\2"+
		"\u008f\u0090\t\23\2\2\u0090\u0091\t\2\2\2\u0091\u0095\t\2\2\2\u0092\u0093"+
		"\t\2\2\2\u0093\u0095\t\2\2\2\u0094\u008a\3\2\2\2\u0094\u008e\3\2\2\2\u0094"+
		"\u0092\3\2\2\2\u0095\u0097\3\2\2\2\u0096\u0098\5\21\t\2\u0097\u0096\3"+
		"\2\2\2\u0097\u0098\3\2\2\2\u0098\u009a\3\2\2\2\u0099h\3\2\2\2\u0099\u0081"+
		"\3\2\2\2\u009a\32\3\2\2\2\u009b\u009c\7\'\2\2\u009c\u009d\5\35\17\2\u009d"+
		"\34\3\2\2\2\u009e\u00a0\t\24\2\2\u009f\u009e\3\2\2\2\u009f\u00a0\3\2\2"+
		"\2\u00a0\u00a1\3\2\2\2\u00a1\u00a2\5\37\20\2\u00a2\u00a3\t\25\2\2\u00a3"+
		"\u00a5\5\37\20\2\u00a4\u00a6\5!\21\2\u00a5\u00a4\3\2\2\2\u00a5\u00a6\3"+
		"\2\2\2\u00a6\u00a8\3\2\2\2\u00a7\u00a9\5\21\t\2\u00a8\u00a7\3\2\2\2\u00a8"+
		"\u00a9\3\2\2\2\u00a9\u00d7\3\2\2\2\u00aa\u00ac\t\24\2\2\u00ab\u00aa\3"+
		"\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ad\3\2\2\2\u00ad\u00ae\5\37\20\2\u00ae"+
		"\u00b0\5!\21\2\u00af\u00b1\5\21\t\2\u00b0\u00af\3\2\2\2\u00b0\u00b1\3"+
		"\2\2\2\u00b1\u00d7\3\2\2\2\u00b2\u00b4\t\24\2\2\u00b3\u00b2\3\2\2\2\u00b3"+
		"\u00b4\3\2\2\2\u00b4\u00b5\3\2\2\2\u00b5\u00b7\5\37\20\2\u00b6\u00b8\5"+
		"\21\t\2\u00b7\u00b6\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8\u00d7\3\2\2\2\u00b9"+
		"\u00ba\5\37\20\2\u00ba\u00bb\7\60\2\2\u00bb\u00bd\3\2\2\2\u00bc\u00b9"+
		"\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf"+
		"\u00c0\3\2\2\2\u00c0\u00c2\5\37\20\2\u00c1\u00c3\5\21\t\2\u00c2\u00c1"+
		"\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00d7\3\2\2\2\u00c4\u00c5\5\37\20\2"+
		"\u00c5\u00c6\7.\2\2\u00c6\u00c8\3\2\2\2\u00c7\u00c4\3\2\2\2\u00c8\u00c9"+
		"\3\2\2\2\u00c9\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb"+
		"\u00cd\5\37\20\2\u00cc\u00ce\5\21\t\2\u00cd\u00cc\3\2\2\2\u00cd\u00ce"+
		"\3\2\2\2\u00ce\u00d7\3\2\2\2\u00cf\u00d1\5\37\20\2\u00d0\u00d2\7\60\2"+
		"\2\u00d1\u00d0\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00d4\3\2\2\2\u00d3\u00d5"+
		"\5\21\t\2\u00d4\u00d3\3\2\2\2\u00d4\u00d5\3\2\2\2\u00d5\u00d7\3\2\2\2"+
		"\u00d6\u009f\3\2\2\2\u00d6\u00ab\3\2\2\2\u00d6\u00b3\3\2\2\2\u00d6\u00bc"+
		"\3\2\2\2\u00d6\u00c7\3\2\2\2\u00d6\u00cf\3\2\2\2\u00d7\36\3\2\2\2\u00d8"+
		"\u00da\5\3\2\2\u00d9\u00d8\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u00d9\3\2"+
		"\2\2\u00db\u00dc\3\2\2\2\u00dc \3\2\2\2\u00dd\u00df\t\26\2\2\u00de\u00e0"+
		"\t\24\2\2\u00df\u00de\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0\u00e1\3\2\2\2"+
		"\u00e1\u00e2\5\37\20\2\u00e2\"\3\2\2\2\u00e3\u00e5\t\27\2\2\u00e4\u00e3"+
		"\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e4\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7"+
		"$\3\2\2\2\u00e8\u00ea\7\60\2\2\u00e9\u00eb\t\30\2\2\u00ea\u00e9\3\2\2"+
		"\2\u00eb\u00ec\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed&"+
		"\3\2\2\2\u00ee\u00ef\7j\2\2\u00ef\u00f0\7v\2\2\u00f0\u00f1\7v\2\2\u00f1"+
		"\u00f2\7r\2\2\u00f2\u00f3\7<\2\2\u00f3\u00f4\7\61\2\2\u00f4\u00fe\7\61"+
		"\2\2\u00f5\u00f6\7j\2\2\u00f6\u00f7\7v\2\2\u00f7\u00f8\7v\2\2\u00f8\u00f9"+
		"\7r\2\2\u00f9\u00fa\7u\2\2\u00fa\u00fb\7<\2\2\u00fb\u00fc\7\61\2\2\u00fc"+
		"\u00fe\7\61\2\2\u00fd\u00ee\3\2\2\2\u00fd\u00f5\3\2\2\2\u00fd\u00fe\3"+
		"\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0100\7y\2\2\u0100\u0101\7y\2\2\u0101"+
		"\u0102\7y\2\2\u0102\u0103\7\60\2\2\u0103\u0104\3\2\2\2\u0104\u0106\5#"+
		"\22\2\u0105\u0107\5%\23\2\u0106\u0105\3\2\2\2\u0107\u0108\3\2\2\2\u0108"+
		"\u0106\3\2\2\2\u0108\u0109\3\2\2\2\u0109(\3\2\2\2\u010a\u010c\5\13\6\2"+
		"\u010b\u010a\3\2\2\2\u010c\u010d\3\2\2\2\u010d\u010b\3\2\2\2\u010d\u010e"+
		"\3\2\2\2\u010e\u0110\3\2\2\2\u010f\u0111\7\60\2\2\u0110\u010f\3\2\2\2"+
		"\u0110\u0111\3\2\2\2\u0111\u0113\3\2\2\2\u0112\u0114\5\13\6\2\u0113\u0112"+
		"\3\2\2\2\u0114\u0115\3\2\2\2\u0115\u0113\3\2\2\2\u0115\u0116\3\2\2\2\u0116"+
		"\u0117\3\2\2\2\u0117\u0123\7B\2\2\u0118\u011a\5\13\6\2\u0119\u0118\3\2"+
		"\2\2\u011a\u011b\3\2\2\2\u011b\u0119\3\2\2\2\u011b\u011c\3\2\2\2\u011c"+
		"\u011d\3\2\2\2\u011d\u011f\7\60\2\2\u011e\u0120\5\13\6\2\u011f\u011e\3"+
		"\2\2\2\u0120\u0121\3\2\2\2\u0121\u011f\3\2\2\2\u0121\u0122\3\2\2\2\u0122"+
		"\u0124\3\2\2\2\u0123\u0119\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0123\3\2"+
		"\2\2\u0125\u0126\3\2\2\2\u0126*\3\2\2\2\u0127\u0129\7%\2\2\u0128\u012a"+
		"\5\13\6\2\u0129\u0128\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u0129\3\2\2\2"+
		"\u012b\u012c\3\2\2\2\u012c,\3\2\2\2\u012d\u012f\7B\2\2\u012e\u0130\5\13"+
		"\6\2\u012f\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131\u012f\3\2\2\2\u0131"+
		"\u0132\3\2\2\2\u0132.\3\2\2\2\u0133\u0134\7<\2\2\u0134\u016e\7+\2\2\u0135"+
		"\u0136\7<\2\2\u0136\u0137\7/\2\2\u0137\u016e\7+\2\2\u0138\u0139\7<\2\2"+
		"\u0139\u013a\7/\2\2\u013a\u016e\7_\2\2\u013b\u013c\7<\2\2\u013c\u016e"+
		"\7F\2\2\u013d\u013e\7<\2\2\u013e\u013f\7/\2\2\u013f\u016e\7F\2\2\u0140"+
		"\u0141\7:\2\2\u0141\u0142\7/\2\2\u0142\u016e\7+\2\2\u0143\u0144\7=\2\2"+
		"\u0144\u016e\7+\2\2\u0145\u0146\7=\2\2\u0146\u0147\7\u2013\2\2\u0147\u016e"+
		"\7+\2\2\u0148\u0149\7<\2\2\u0149\u016e\7*\2\2\u014a\u014b\7<\2\2\u014b"+
		"\u014c\7/\2\2\u014c\u016e\7*\2\2\u014d\u014e\7<\2\2\u014e\u014f\7)\2\2"+
		"\u014f\u016e\7*\2\2\u0150\u0151\7<\2\2\u0151\u0152\7\u2013\2\2\u0152\u016e"+
		"\7\61\2\2\u0153\u0154\7<\2\2\u0154\u016e\7\61\2\2\u0155\u0156\7<\2\2\u0156"+
		"\u0157\7`\2\2\u0157\u016e\7+\2\2\u0158\u0159\7\u00b1\2\2\u0159\u015a\7"+
		"^\2\2\u015a\u015b\7a\2\2\u015b\u015c\7*\2\2\u015c\u015d\7\u30c6\2\2\u015d"+
		"\u015e\7+\2\2\u015e\u015f\7a\2\2\u015f\u0160\7\61\2\2\u0160\u016e\7\u00b1"+
		"\2\2\u0161\u0162\7Q\2\2\u0162\u0163\7a\2\2\u0163\u016e\7q\2\2\u0164\u0165"+
		"\7q\2\2\u0165\u0166\7a\2\2\u0166\u016e\7Q\2\2\u0167\u0168\7Q\2\2\u0168"+
		"\u0169\7a\2\2\u0169\u016e\7Q\2\2\u016a\u016b\7^\2\2\u016b\u016c\7q\2\2"+
		"\u016c\u016e\7\61\2\2\u016d\u0133\3\2\2\2\u016d\u0135\3\2\2\2\u016d\u0138"+
		"\3\2\2\2\u016d\u013b\3\2\2\2\u016d\u013d\3\2\2\2\u016d\u0140\3\2\2\2\u016d"+
		"\u0143\3\2\2\2\u016d\u0145\3\2\2\2\u016d\u0148\3\2\2\2\u016d\u014a\3\2"+
		"\2\2\u016d\u014d\3\2\2\2\u016d\u0150\3\2\2\2\u016d\u0153\3\2\2\2\u016d"+
		"\u0155\3\2\2\2\u016d\u0158\3\2\2\2\u016d\u0161\3\2\2\2\u016d\u0164\3\2"+
		"\2\2\u016d\u0167\3\2\2\2\u016d\u016a\3\2\2\2\u016e\60\3\2\2\2\u016f\u0184"+
		"\7K\2\2\u0170\u0171\7K\2\2\u0171\u0184\7K\2\2\u0172\u0173\7K\2\2\u0173"+
		"\u0174\7K\2\2\u0174\u0184\7K\2\2\u0175\u0176\7K\2\2\u0176\u0184\7X\2\2"+
		"\u0177\u0184\7X\2\2\u0178\u0179\7X\2\2\u0179\u0184\7K\2\2\u017a\u017b"+
		"\7X\2\2\u017b\u017c\7K\2\2\u017c\u0184\7K\2\2\u017d\u017e\7X\2\2\u017e"+
		"\u017f\7K\2\2\u017f\u0180\7K\2\2\u0180\u0184\7K\2\2\u0181\u0182\7K\2\2"+
		"\u0182\u0184\7Z\2\2\u0183\u016f\3\2\2\2\u0183\u0170\3\2\2\2\u0183\u0172"+
		"\3\2\2\2\u0183\u0175\3\2\2\2\u0183\u0177\3\2\2\2\u0183\u0178\3\2\2\2\u0183"+
		"\u017a\3\2\2\2\u0183\u017d\3\2\2\2\u0183\u0181\3\2\2\2\u0184\u0186\3\2"+
		"\2\2\u0185\u0187\7\60\2\2\u0186\u0185\3\2\2\2\u0186\u0187\3\2\2\2\u0187"+
		"\u0189\3\2\2\2\u0188\u018a\5\21\t\2\u0189\u0188\3\2\2\2\u0189\u018a\3"+
		"\2\2\2\u018a\62\3\2\2\2\u018b\u018c\5\7\4\2\u018c\u018d\7\60\2\2\u018d"+
		"\u018f\3\2\2\2\u018e\u018b\3\2\2\2\u018f\u0190\3\2\2\2\u0190\u018e\3\2"+
		"\2\2\u0190\u0191\3\2\2\2\u0191\u0193\3\2\2\2\u0192\u0194\5\7\4\2\u0193"+
		"\u0192\3\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2\2\2\u0195\u0197\5\21"+
		"\t\2\u0196\u0195\3\2\2\2\u0196\u0197\3\2\2\2\u0197\64\3\2\2\2\u0198\u019a"+
		"\5\t\5\2\u0199\u0198\3\2\2\2\u019a\u019b\3\2\2\2\u019b\u0199\3\2\2\2\u019b"+
		"\u019c\3\2\2\2\u019c\66\3\2\2\2\u019d\u019f\5\13\6\2\u019e\u019d\3\2\2"+
		"\2\u019f\u01a0\3\2\2\2\u01a0\u019e\3\2\2\2\u01a0\u01a1\3\2\2\2\u01a1\u01a3"+
		"\3\2\2\2\u01a2\u01a4\5\21\t\2\u01a3\u01a2\3\2\2\2\u01a3\u01a4\3\2\2\2"+
		"\u01a48\3\2\2\2\u01a5\u01b3\5\r\7\2\u01a6\u01b3\5\17\b\2\u01a7\u01b3\t"+
		"\31\2\2\u01a8\u01a9\7\60\2\2\u01a9\u01aa\7\60\2\2\u01aa\u01b3\7\60\2\2"+
		"\u01ab\u01ac\7*\2\2\u01ac\u01ad\7#\2\2\u01ad\u01b3\7+\2\2\u01ae\u01af"+
		"\7*\2\2\u01af\u01b0\7A\2\2\u01b0\u01b3\7+\2\2\u01b1\u01b3\t\32\2\2\u01b2"+
		"\u01a5\3\2\2\2\u01b2\u01a6\3\2\2\2\u01b2\u01a7\3\2\2\2\u01b2\u01a8\3\2"+
		"\2\2\u01b2\u01ab\3\2\2\2\u01b2\u01ae\3\2\2\2\u01b2\u01b1\3\2\2\2\u01b3"+
		":\3\2\2\2\u01b4\u01b6\n\33\2\2\u01b5\u01b4\3\2\2\2\u01b6\u01b7\3\2\2\2"+
		"\u01b7\u01b5\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8<\3\2\2\2\u01b9\u01bb\13"+
		"\2\2\2\u01ba\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bc"+
		"\u01ba\3\2\2\2\u01bd>\3\2\2\29\2QVbehm{~\u0081\u0086\u0094\u0097\u0099"+
		"\u009f\u00a5\u00a8\u00ab\u00b0\u00b3\u00b7\u00be\u00c2\u00c9\u00cd\u00d1"+
		"\u00d4\u00d6\u00db\u00df\u00e6\u00ec\u00fd\u0108\u010d\u0110\u0115\u011b"+
		"\u0121\u0125\u012b\u0131\u016d\u0183\u0186\u0189\u0190\u0193\u0196\u019b"+
		"\u01a0\u01a3\u01b2\u01b7\u01bc\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
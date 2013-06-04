// Generated from /home/afsina/projects/zemberek/shared/src/main/resources/tokenizer/TurkishLexer.g4 by ANTLR 4.0

package zemberek.tokenizer.antlr;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNSimulator;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TurkishLexer extends Lexer {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		Abbreviation=1, SpaceTab=2, NewLine=3, PercentNumeral=4, Number=5, RomanNumeral=6, 
		TurkishWord=7, TurkishWordWithApos=8, AllCapsWord=9, AbbreviationWithDots=10, 
		Alphanumerical=11, Punctuation=12, Unknown=13;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"<INVALID>",
		"Abbreviation", "SpaceTab", "NewLine", "PercentNumeral", "Number", "RomanNumeral", 
		"TurkishWord", "TurkishWordWithApos", "AllCapsWord", "AbbreviationWithDots", 
		"Alphanumerical", "Punctuation", "Unknown"
	};
	public static final String[] ruleNames = {
		"Digit", "TurkishLetters", "TurkishLettersCapital", "AllTurkishAlphanumerical", 
		"AposAndSuffix", "AposAndSuffixCapital", "SpaceTab", "NewLine", "PercentNumeral", 
		"Number", "Integer", "Exp", "RomanNumeral", "TurkishWord", "TurkishWordWithApos", 
		"AllCapsWord", "AbbreviationWithDots", "Alphanumerical", "Punctuation", 
		"Unknown"
	};


	private static Set<String> abbreviations = Sets.newHashSet();
	private java.util.Queue<Token> queue = new java.util.LinkedList<Token>();
	private static Locale localeTr = new Locale("tr");

	static {
	    try {
	        for(String line: Resources.readLines(Resources.getResource("tokenizer/abbreviations.txt"),Charsets.UTF_8)) {
	            final int abbrEndIndex = line.indexOf(":");
	            if (abbrEndIndex > 0) {
	                final String abbr = line.substring(0, abbrEndIndex);
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

	    if(next.getType() != TurkishWord) {
	      return next;
	    }

	    StringBuilder builder = new StringBuilder(next.getText());

	    Token next2 = super.nextToken();
	    if (next2.getType() == Punctuation && next2.getText().equals(".")) {
	      builder.append('.');
	      String abbrev = builder.toString();
	      if (abbreviations!= null && abbreviations.contains(abbrev)) {
	         return new CommonToken(Abbreviation, abbrev);
	      }
	    }
	    queue.offer(next2);
	    return next;
	}



	public TurkishLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TurkishLexer.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\2\4\17\u00f7\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b"+
		"\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20"+
		"\t\20\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\3\2\3\2\3\3\3"+
		"\3\3\4\3\4\3\5\3\5\3\6\3\6\6\6\66\n\6\r\6\16\6\67\3\7\3\7\6\7<\n\7\r\7"+
		"\16\7=\3\b\6\bA\n\b\r\b\16\bB\3\t\6\tF\n\t\r\t\16\tG\3\n\3\n\3\n\3\13"+
		"\5\13N\n\13\3\13\3\13\3\13\3\13\5\13T\n\13\3\13\5\13W\n\13\3\13\5\13Z"+
		"\n\13\3\13\3\13\3\13\5\13_\n\13\3\13\5\13b\n\13\3\13\3\13\5\13f\n\13\3"+
		"\13\3\13\3\13\6\13k\n\13\r\13\16\13l\3\13\3\13\5\13q\n\13\3\13\3\13\3"+
		"\13\6\13v\n\13\r\13\16\13w\3\13\3\13\5\13|\n\13\3\13\3\13\5\13\u0080\n"+
		"\13\3\13\5\13\u0083\n\13\5\13\u0085\n\13\3\f\6\f\u0088\n\f\r\f\16\f\u0089"+
		"\3\r\3\r\5\r\u008e\n\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u00a6"+
		"\n\16\3\16\5\16\u00a9\n\16\3\17\5\17\u00ac\n\17\3\17\6\17\u00af\n\17\r"+
		"\17\16\17\u00b0\3\20\3\20\6\20\u00b5\n\20\r\20\16\20\u00b6\3\20\3\20\3"+
		"\21\6\21\u00bc\n\21\r\21\16\21\u00bd\3\21\5\21\u00c1\n\21\3\21\5\21\u00c4"+
		"\n\21\5\21\u00c6\n\21\3\22\3\22\3\22\6\22\u00cb\n\22\r\22\16\22\u00cc"+
		"\3\22\5\22\u00d0\n\22\3\22\5\22\u00d3\n\22\3\22\5\22\u00d6\n\22\5\22\u00d8"+
		"\n\22\3\23\6\23\u00db\n\23\r\23\16\23\u00dc\3\23\5\23\u00e0\n\23\3\23"+
		"\5\23\u00e3\n\23\5\23\u00e5\n\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3"+
		"\24\3\24\3\24\5\24\u00f1\n\24\3\25\6\25\u00f4\n\25\r\25\16\25\u00f5\3"+
		"\u00f5\26\3\2\1\5\2\1\7\2\1\t\2\1\13\2\1\r\2\1\17\4\1\21\5\1\23\6\1\25"+
		"\7\1\27\2\1\31\2\1\33\b\1\35\t\1\37\n\1!\13\1#\f\1%\r\1\'\16\1)\17\1\3"+
		"\2\17\3\62;\13c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd"+
		"\u00fe\u0121\u0121\u0133\u0133\u0161\u0161\13C\\\u00c4\u00c4\u00c9\u00c9"+
		"\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132\u0160\u0160"+
		"\23//\62;C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de"+
		"\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121"+
		"\u0132\u0133\u0160\u0161\4\13\13\"\"\4\f\f\17\17\4--//\5..\60\60^^\4-"+
		"-//\4--//\4GGgg\4--//\t#$&\60<=AB]_}}\177\177\u0123\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2"+
		"\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\3+\3\2\2"+
		"\2\5-\3\2\2\2\7/\3\2\2\2\t\61\3\2\2\2\13\63\3\2\2\2\r9\3\2\2\2\17@\3\2"+
		"\2\2\21E\3\2\2\2\23I\3\2\2\2\25\u0084\3\2\2\2\27\u0087\3\2\2\2\31\u008b"+
		"\3\2\2\2\33\u00a5\3\2\2\2\35\u00ab\3\2\2\2\37\u00b2\3\2\2\2!\u00bb\3\2"+
		"\2\2#\u00ca\3\2\2\2%\u00da\3\2\2\2\'\u00f0\3\2\2\2)\u00f3\3\2\2\2+,\t"+
		"\2\2\2,\4\3\2\2\2-.\t\3\2\2.\6\3\2\2\2/\60\t\4\2\2\60\b\3\2\2\2\61\62"+
		"\t\5\2\2\62\n\3\2\2\2\63\65\7)\2\2\64\66\5\5\3\2\65\64\3\2\2\2\66\67\3"+
		"\2\2\2\67\65\3\2\2\2\678\3\2\2\28\f\3\2\2\29;\7)\2\2:<\5\7\4\2;:\3\2\2"+
		"\2<=\3\2\2\2=;\3\2\2\2=>\3\2\2\2>\16\3\2\2\2?A\t\6\2\2@?\3\2\2\2AB\3\2"+
		"\2\2B@\3\2\2\2BC\3\2\2\2C\20\3\2\2\2DF\t\7\2\2ED\3\2\2\2FG\3\2\2\2GE\3"+
		"\2\2\2GH\3\2\2\2H\22\3\2\2\2IJ\7\'\2\2JK\5\25\13\2K\24\3\2\2\2LN\t\b\2"+
		"\2ML\3\2\2\2MN\3\2\2\2NO\3\2\2\2OP\5\27\f\2PQ\t\t\2\2QS\5\27\f\2RT\5\31"+
		"\r\2SR\3\2\2\2ST\3\2\2\2TV\3\2\2\2UW\5\13\6\2VU\3\2\2\2VW\3\2\2\2W\u0085"+
		"\3\2\2\2XZ\t\n\2\2YX\3\2\2\2YZ\3\2\2\2Z[\3\2\2\2[\\\5\27\f\2\\^\5\31\r"+
		"\2]_\5\13\6\2^]\3\2\2\2^_\3\2\2\2_\u0085\3\2\2\2`b\t\13\2\2a`\3\2\2\2"+
		"ab\3\2\2\2bc\3\2\2\2ce\5\27\f\2df\5\13\6\2ed\3\2\2\2ef\3\2\2\2f\u0085"+
		"\3\2\2\2gh\5\27\f\2hi\7\60\2\2ik\3\2\2\2jg\3\2\2\2kl\3\2\2\2lj\3\2\2\2"+
		"lm\3\2\2\2mn\3\2\2\2np\5\27\f\2oq\5\13\6\2po\3\2\2\2pq\3\2\2\2q\u0085"+
		"\3\2\2\2rs\5\27\f\2st\7.\2\2tv\3\2\2\2ur\3\2\2\2vw\3\2\2\2wu\3\2\2\2w"+
		"x\3\2\2\2xy\3\2\2\2y{\5\27\f\2z|\5\13\6\2{z\3\2\2\2{|\3\2\2\2|\u0085\3"+
		"\2\2\2}\177\5\27\f\2~\u0080\7\60\2\2\177~\3\2\2\2\177\u0080\3\2\2\2\u0080"+
		"\u0082\3\2\2\2\u0081\u0083\5\13\6\2\u0082\u0081\3\2\2\2\u0082\u0083\3"+
		"\2\2\2\u0083\u0085\3\2\2\2\u0084M\3\2\2\2\u0084Y\3\2\2\2\u0084a\3\2\2"+
		"\2\u0084j\3\2\2\2\u0084u\3\2\2\2\u0084}\3\2\2\2\u0085\26\3\2\2\2\u0086"+
		"\u0088\5\3\2\2\u0087\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u0087\3\2"+
		"\2\2\u0089\u008a\3\2\2\2\u008a\30\3\2\2\2\u008b\u008d\t\f\2\2\u008c\u008e"+
		"\t\r\2\2\u008d\u008c\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f\3\2\2\2\u008f"+
		"\u0090\5\27\f\2\u0090\32\3\2\2\2\u0091\u00a6\7K\2\2\u0092\u0093\7K\2\2"+
		"\u0093\u00a6\7K\2\2\u0094\u0095\7K\2\2\u0095\u0096\7K\2\2\u0096\u00a6"+
		"\7K\2\2\u0097\u0098\7K\2\2\u0098\u00a6\7X\2\2\u0099\u00a6\7X\2\2\u009a"+
		"\u009b\7X\2\2\u009b\u00a6\7K\2\2\u009c\u009d\7X\2\2\u009d\u009e\7K\2\2"+
		"\u009e\u00a6\7K\2\2\u009f\u00a0\7X\2\2\u00a0\u00a1\7K\2\2\u00a1\u00a2"+
		"\7K\2\2\u00a2\u00a6\7K\2\2\u00a3\u00a4\7K\2\2\u00a4\u00a6\7Z\2\2\u00a5"+
		"\u0091\3\2\2\2\u00a5\u0092\3\2\2\2\u00a5\u0094\3\2\2\2\u00a5\u0097\3\2"+
		"\2\2\u00a5\u0099\3\2\2\2\u00a5\u009a\3\2\2\2\u00a5\u009c\3\2\2\2\u00a5"+
		"\u009f\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a6\u00a8\3\2\2\2\u00a7\u00a9\7\60"+
		"\2\2\u00a8\u00a7\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\34\3\2\2\2\u00aa\u00ac"+
		"\5\7\4\2\u00ab\u00aa\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad"+
		"\u00af\5\5\3\2\u00ae\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00ae\3\2"+
		"\2\2\u00b0\u00b1\3\2\2\2\u00b1\36\3\2\2\2\u00b2\u00b4\5\7\4\2\u00b3\u00b5"+
		"\5\5\3\2\u00b4\u00b3\3\2\2\2\u00b5\u00b6\3\2\2\2\u00b6\u00b4\3\2\2\2\u00b6"+
		"\u00b7\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8\u00b9\5\13\6\2\u00b9 \3\2\2\2"+
		"\u00ba\u00bc\5\7\4\2\u00bb\u00ba\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00bb"+
		"\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00c5\3\2\2\2\u00bf\u00c1\5\r\7\2\u00c0"+
		"\u00bf\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1\u00c6\3\2\2\2\u00c2\u00c4\5\13"+
		"\6\2\u00c3\u00c2\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\u00c6\3\2\2\2\u00c5"+
		"\u00c0\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6\"\3\2\2\2\u00c7\u00c8\5\7\4\2"+
		"\u00c8\u00c9\7\60\2\2\u00c9\u00cb\3\2\2\2\u00ca\u00c7\3\2\2\2\u00cb\u00cc"+
		"\3\2\2\2\u00cc\u00ca\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd\u00cf\3\2\2\2\u00ce"+
		"\u00d0\5\7\4\2\u00cf\u00ce\3\2\2\2\u00cf\u00d0\3\2\2\2\u00d0\u00d7\3\2"+
		"\2\2\u00d1\u00d3\5\r\7\2\u00d2\u00d1\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3"+
		"\u00d8\3\2\2\2\u00d4\u00d6\5\13\6\2\u00d5\u00d4\3\2\2\2\u00d5\u00d6\3"+
		"\2\2\2\u00d6\u00d8\3\2\2\2\u00d7\u00d2\3\2\2\2\u00d7\u00d5\3\2\2\2\u00d8"+
		"$\3\2\2\2\u00d9\u00db\5\t\5\2\u00da\u00d9\3\2\2\2\u00db\u00dc\3\2\2\2"+
		"\u00dc\u00da\3\2\2\2\u00dc\u00dd\3\2\2\2\u00dd\u00e4\3\2\2\2\u00de\u00e0"+
		"\5\r\7\2\u00df\u00de\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0\u00e5\3\2\2\2\u00e1"+
		"\u00e3\5\13\6\2\u00e2\u00e1\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u00e5\3"+
		"\2\2\2\u00e4\u00df\3\2\2\2\u00e4\u00e2\3\2\2\2\u00e5&\3\2\2\2\u00e6\u00e7"+
		"\7\60\2\2\u00e7\u00e8\7\60\2\2\u00e8\u00f1\7\60\2\2\u00e9\u00ea\7*\2\2"+
		"\u00ea\u00eb\7#\2\2\u00eb\u00f1\7+\2\2\u00ec\u00ed\7*\2\2\u00ed\u00ee"+
		"\7A\2\2\u00ee\u00f1\7+\2\2\u00ef\u00f1\t\16\2\2\u00f0\u00e6\3\2\2\2\u00f0"+
		"\u00e9\3\2\2\2\u00f0\u00ec\3\2\2\2\u00f0\u00ef\3\2\2\2\u00f1(\3\2\2\2"+
		"\u00f2\u00f4\13\2\2\2\u00f3\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f6"+
		"\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f6*\3\2\2\2+\2\67=BGMSVY^aelpw{\177\u0082"+
		"\u0084\u0089\u008d\u00a5\u00a8\u00ab\u00b0\u00b6\u00bd\u00c0\u00c3\u00c5"+
		"\u00cc\u00cf\u00d2\u00d5\u00d7\u00dc\u00df\u00e2\u00e4\u00f0\u00f5";
	public static final ATN _ATN =
		ATNSimulator.deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
	}
}
// Generated from /home/cemil/projects/zemberek5/tokenization/src/main/resources/tokenizer/TurkishLexer.g4 by ANTLR 4.6
package zemberek.tokenizer.antlr;

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
	static { RuntimeMetaData.checkVersion("4.6", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		Abbreviation=1, SpaceTab=2, NewLine=3, TimeHours=4, Date=5, PercentNumeral=6, 
		Number=7, URL=8, Email=9, HashTag=10, Mention=11, Emoticon=12, RomanNumeral=13, 
		AbbreviationWithDots=14, TurkishWord=15, TurkishWordWithApos=16, Punctuation=17, 
		UnknownWord=18, Unknown=19;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"Digit", "TurkishLetters", "TurkishLettersCapital", "TurkishLettersAll", 
		"AllTurkishAlphanumerical", "Apostrophe", "DoubleQuote", "AposAndSuffix", 
		"SpaceTab", "NewLine", "TimeHours", "Date", "PercentNumeral", "Number", 
		"Integer", "Exp", "URL", "Email", "HashTag", "Mention", "Emoticon", "RomanNumeral", 
		"AbbreviationWithDots", "TurkishWord", "TurkishWordWithApos", "Punctuation", 
		"UnknownWord", "Unknown"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "Abbreviation", "SpaceTab", "NewLine", "TimeHours", "Date", "PercentNumeral", 
		"Number", "URL", "Email", "HashTag", "Mention", "Emoticon", "RomanNumeral", 
		"AbbreviationWithDots", "TurkishWord", "TurkishWordWithApos", "Punctuation", 
		"UnknownWord", "Unknown"
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
	        for(String line: Resources.readLines(Resources.getResource("tokenizer/abbreviations.txt"),Charsets.UTF_8)) {
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

	    if(next.getType() != TurkishWord) {
	      return next;
	    }

	    StringBuilder builder = new StringBuilder(next.getText());

	    Token next2 = super.nextToken();
	    if (next2.getType() == Punctuation && next2.getText().equals(".")) {
	      builder.append('.');
	      String abbrev = builder.toString();
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
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\25\u01b2\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\3\2\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\6\tL\n\t\r\t\16\tM\3\n\6\nQ\n"+
		"\n\r\n\16\nR\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\5\f]\n\f\3\r\5\r`\n\r\3"+
		"\r\3\r\3\r\5\re\n\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\rq\n\r\3"+
		"\r\5\rt\n\r\3\r\5\rw\n\r\3\r\3\r\3\r\5\r|\n\r\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\5\r\u0088\n\r\3\r\5\r\u008b\n\r\5\r\u008d\n\r\3\16\3"+
		"\16\3\16\3\17\5\17\u0093\n\17\3\17\3\17\3\17\3\17\5\17\u0099\n\17\3\17"+
		"\5\17\u009c\n\17\3\17\5\17\u009f\n\17\3\17\3\17\3\17\5\17\u00a4\n\17\3"+
		"\17\5\17\u00a7\n\17\3\17\3\17\5\17\u00ab\n\17\3\17\3\17\3\17\6\17\u00b0"+
		"\n\17\r\17\16\17\u00b1\3\17\3\17\5\17\u00b6\n\17\3\17\3\17\3\17\6\17\u00bb"+
		"\n\17\r\17\16\17\u00bc\3\17\3\17\5\17\u00c1\n\17\3\17\3\17\5\17\u00c5"+
		"\n\17\3\17\5\17\u00c8\n\17\5\17\u00ca\n\17\3\20\6\20\u00cd\n\20\r\20\16"+
		"\20\u00ce\3\21\3\21\5\21\u00d3\n\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u00e6\n\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\6\22\u00ee\n\22\r\22\16\22\u00ef\3\22\3\22\6"+
		"\22\u00f4\n\22\r\22\16\22\u00f5\6\22\u00f8\n\22\r\22\16\22\u00f9\3\23"+
		"\6\23\u00fd\n\23\r\23\16\23\u00fe\3\23\5\23\u0102\n\23\3\23\6\23\u0105"+
		"\n\23\r\23\16\23\u0106\3\23\3\23\6\23\u010b\n\23\r\23\16\23\u010c\3\23"+
		"\3\23\6\23\u0111\n\23\r\23\16\23\u0112\6\23\u0115\n\23\r\23\16\23\u0116"+
		"\3\24\3\24\6\24\u011b\n\24\r\24\16\24\u011c\3\24\5\24\u0120\n\24\3\25"+
		"\3\25\6\25\u0124\n\25\r\25\16\25\u0125\3\26\3\26\3\26\3\26\3\26\3\26\3"+
		"\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3"+
		"\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3"+
		"\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3"+
		"\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u0162\n\26\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\5\27\u0178\n\27\3\27\5\27\u017b\n\27\3\27\5"+
		"\27\u017e\n\27\3\30\3\30\3\30\6\30\u0183\n\30\r\30\16\30\u0184\3\30\5"+
		"\30\u0188\n\30\3\30\5\30\u018b\n\30\3\31\6\31\u018e\n\31\r\31\16\31\u018f"+
		"\3\32\6\32\u0193\n\32\r\32\16\32\u0194\3\32\5\32\u0198\n\32\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\5\33\u01a7\n\33"+
		"\3\34\6\34\u01aa\n\34\r\34\16\34\u01ab\3\35\6\35\u01af\n\35\r\35\16\35"+
		"\u01b0\3\u01b0\2\36\3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\4\25\5\27\6"+
		"\31\7\33\b\35\t\37\2!\2#\n%\13\'\f)\r+\16-\17/\20\61\21\63\22\65\23\67"+
		"\249\25\3\2\31\3\2\62;\13\2c|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8"+
		"\u00f8\u00fd\u00fe\u0121\u0121\u0133\u0133\u0161\u0161\13\2C\\\u00c4\u00c4"+
		"\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132"+
		"\u0160\u0160\21\2C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8"+
		"\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe"+
		"\u0120\u0121\u0132\u0133\u0160\u0161\23\2//\62;C\\c|\u00c4\u00c4\u00c9"+
		"\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0"+
		"\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\4\2"+
		"))\u201b\u201b\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e\u201f\4\2\13\13\"\""+
		"\4\2\f\f\17\17\3\2\62\64\4\2\60\60<<\3\2\62\67\3\2\62\65\3\2\62\63\3\2"+
		"\63\63\3\29;\3\2\64\64\3\2\62\62\4\2--//\4\2..\60\60\4\2GGgg\n\2##&(*"+
		"\60<=AB]_}}\177\177\17\2\13\f\17\17\"$&\60<=AB]_}}\177\177\u00ad\u00ad"+
		"\u00bd\u00bd\u201b\u201b\u201e\u201f\u01ff\2\23\3\2\2\2\2\25\3\2\2\2\2"+
		"\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2#\3\2\2\2\2%\3\2\2"+
		"\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2"+
		"\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\3;\3\2\2\2\5=\3\2"+
		"\2\2\7?\3\2\2\2\tA\3\2\2\2\13C\3\2\2\2\rE\3\2\2\2\17G\3\2\2\2\21I\3\2"+
		"\2\2\23P\3\2\2\2\25T\3\2\2\2\27V\3\2\2\2\31\u008c\3\2\2\2\33\u008e\3\2"+
		"\2\2\35\u00c9\3\2\2\2\37\u00cc\3\2\2\2!\u00d0\3\2\2\2#\u00e5\3\2\2\2%"+
		"\u00fc\3\2\2\2\'\u0118\3\2\2\2)\u0121\3\2\2\2+\u0161\3\2\2\2-\u0177\3"+
		"\2\2\2/\u0182\3\2\2\2\61\u018d\3\2\2\2\63\u0192\3\2\2\2\65\u01a6\3\2\2"+
		"\2\67\u01a9\3\2\2\29\u01ae\3\2\2\2;<\t\2\2\2<\4\3\2\2\2=>\t\3\2\2>\6\3"+
		"\2\2\2?@\t\4\2\2@\b\3\2\2\2AB\t\5\2\2B\n\3\2\2\2CD\t\6\2\2D\f\3\2\2\2"+
		"EF\t\7\2\2F\16\3\2\2\2GH\t\b\2\2H\20\3\2\2\2IK\5\r\7\2JL\5\t\5\2KJ\3\2"+
		"\2\2LM\3\2\2\2MK\3\2\2\2MN\3\2\2\2N\22\3\2\2\2OQ\t\t\2\2PO\3\2\2\2QR\3"+
		"\2\2\2RP\3\2\2\2RS\3\2\2\2S\24\3\2\2\2TU\t\n\2\2U\26\3\2\2\2VW\t\13\2"+
		"\2WX\t\2\2\2XY\t\f\2\2YZ\t\r\2\2Z\\\t\2\2\2[]\5\21\t\2\\[\3\2\2\2\\]\3"+
		"\2\2\2]\30\3\2\2\2^`\t\16\2\2_^\3\2\2\2_`\3\2\2\2`a\3\2\2\2ab\t\2\2\2"+
		"bd\7\60\2\2ce\t\17\2\2dc\3\2\2\2de\3\2\2\2ef\3\2\2\2fg\t\2\2\2gp\7\60"+
		"\2\2hi\t\20\2\2ij\t\21\2\2jk\t\2\2\2kq\t\2\2\2lm\t\22\2\2mn\t\23\2\2n"+
		"o\t\2\2\2oq\t\2\2\2ph\3\2\2\2pl\3\2\2\2qs\3\2\2\2rt\5\21\t\2sr\3\2\2\2"+
		"st\3\2\2\2t\u008d\3\2\2\2uw\t\16\2\2vu\3\2\2\2vw\3\2\2\2wx\3\2\2\2xy\t"+
		"\2\2\2y{\7\61\2\2z|\t\17\2\2{z\3\2\2\2{|\3\2\2\2|}\3\2\2\2}~\t\2\2\2~"+
		"\u0087\7\61\2\2\177\u0080\t\20\2\2\u0080\u0081\t\21\2\2\u0081\u0082\t"+
		"\2\2\2\u0082\u0088\t\2\2\2\u0083\u0084\t\22\2\2\u0084\u0085\t\23\2\2\u0085"+
		"\u0086\t\2\2\2\u0086\u0088\t\2\2\2\u0087\177\3\2\2\2\u0087\u0083\3\2\2"+
		"\2\u0088\u008a\3\2\2\2\u0089\u008b\5\21\t\2\u008a\u0089\3\2\2\2\u008a"+
		"\u008b\3\2\2\2\u008b\u008d\3\2\2\2\u008c_\3\2\2\2\u008cv\3\2\2\2\u008d"+
		"\32\3\2\2\2\u008e\u008f\7\'\2\2\u008f\u0090\5\35\17\2\u0090\34\3\2\2\2"+
		"\u0091\u0093\t\24\2\2\u0092\u0091\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0094"+
		"\3\2\2\2\u0094\u0095\5\37\20\2\u0095\u0096\t\25\2\2\u0096\u0098\5\37\20"+
		"\2\u0097\u0099\5!\21\2\u0098\u0097\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u009b"+
		"\3\2\2\2\u009a\u009c\5\21\t\2\u009b\u009a\3\2\2\2\u009b\u009c\3\2\2\2"+
		"\u009c\u00ca\3\2\2\2\u009d\u009f\t\24\2\2\u009e\u009d\3\2\2\2\u009e\u009f"+
		"\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a1\5\37\20\2\u00a1\u00a3\5!\21\2"+
		"\u00a2\u00a4\5\21\t\2\u00a3\u00a2\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00ca"+
		"\3\2\2\2\u00a5\u00a7\t\24\2\2\u00a6\u00a5\3\2\2\2\u00a6\u00a7\3\2\2\2"+
		"\u00a7\u00a8\3\2\2\2\u00a8\u00aa\5\37\20\2\u00a9\u00ab\5\21\t\2\u00aa"+
		"\u00a9\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u00ca\3\2\2\2\u00ac\u00ad\5\37"+
		"\20\2\u00ad\u00ae\7\60\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ac\3\2\2\2\u00b0"+
		"\u00b1\3\2\2\2\u00b1\u00af\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b3\3\2"+
		"\2\2\u00b3\u00b5\5\37\20\2\u00b4\u00b6\5\21\t\2\u00b5\u00b4\3\2\2\2\u00b5"+
		"\u00b6\3\2\2\2\u00b6\u00ca\3\2\2\2\u00b7\u00b8\5\37\20\2\u00b8\u00b9\7"+
		".\2\2\u00b9\u00bb\3\2\2\2\u00ba\u00b7\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc"+
		"\u00ba\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00c0\5\37"+
		"\20\2\u00bf\u00c1\5\21\t\2\u00c0\u00bf\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1"+
		"\u00ca\3\2\2\2\u00c2\u00c4\5\37\20\2\u00c3\u00c5\7\60\2\2\u00c4\u00c3"+
		"\3\2\2\2\u00c4\u00c5\3\2\2\2\u00c5\u00c7\3\2\2\2\u00c6\u00c8\5\21\t\2"+
		"\u00c7\u00c6\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00ca\3\2\2\2\u00c9\u0092"+
		"\3\2\2\2\u00c9\u009e\3\2\2\2\u00c9\u00a6\3\2\2\2\u00c9\u00af\3\2\2\2\u00c9"+
		"\u00ba\3\2\2\2\u00c9\u00c2\3\2\2\2\u00ca\36\3\2\2\2\u00cb\u00cd\5\3\2"+
		"\2\u00cc\u00cb\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce\u00cc\3\2\2\2\u00ce\u00cf"+
		"\3\2\2\2\u00cf \3\2\2\2\u00d0\u00d2\t\26\2\2\u00d1\u00d3\t\24\2\2\u00d2"+
		"\u00d1\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00d5\5\37"+
		"\20\2\u00d5\"\3\2\2\2\u00d6\u00d7\7j\2\2\u00d7\u00d8\7v\2\2\u00d8\u00d9"+
		"\7v\2\2\u00d9\u00da\7r\2\2\u00da\u00db\7<\2\2\u00db\u00dc\7\61\2\2\u00dc"+
		"\u00e6\7\61\2\2\u00dd\u00de\7j\2\2\u00de\u00df\7v\2\2\u00df\u00e0\7v\2"+
		"\2\u00e0\u00e1\7r\2\2\u00e1\u00e2\7u\2\2\u00e2\u00e3\7<\2\2\u00e3\u00e4"+
		"\7\61\2\2\u00e4\u00e6\7\61\2\2\u00e5\u00d6\3\2\2\2\u00e5\u00dd\3\2\2\2"+
		"\u00e5\u00e6\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\u00e8\7y\2\2\u00e8\u00e9"+
		"\7y\2\2\u00e9\u00ea\7y\2\2\u00ea\u00eb\7\60\2\2\u00eb\u00f7\3\2\2\2\u00ec"+
		"\u00ee\5\13\6\2\u00ed\u00ec\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00ed\3"+
		"\2\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u00f3\7\60\2\2\u00f2"+
		"\u00f4\5\13\6\2\u00f3\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f3\3"+
		"\2\2\2\u00f5\u00f6\3\2\2\2\u00f6\u00f8\3\2\2\2\u00f7\u00ed\3\2\2\2\u00f8"+
		"\u00f9\3\2\2\2\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa$\3\2\2\2"+
		"\u00fb\u00fd\5\13\6\2\u00fc\u00fb\3\2\2\2\u00fd\u00fe\3\2\2\2\u00fe\u00fc"+
		"\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0101\3\2\2\2\u0100\u0102\7\60\2\2"+
		"\u0101\u0100\3\2\2\2\u0101\u0102\3\2\2\2\u0102\u0104\3\2\2\2\u0103\u0105"+
		"\5\13\6\2\u0104\u0103\3\2\2\2\u0105\u0106\3\2\2\2\u0106\u0104\3\2\2\2"+
		"\u0106\u0107\3\2\2\2\u0107\u0108\3\2\2\2\u0108\u0114\7B\2\2\u0109\u010b"+
		"\5\13\6\2\u010a\u0109\3\2\2\2\u010b\u010c\3\2\2\2\u010c\u010a\3\2\2\2"+
		"\u010c\u010d\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u0110\7\60\2\2\u010f\u0111"+
		"\5\13\6\2\u0110\u010f\3\2\2\2\u0111\u0112\3\2\2\2\u0112\u0110\3\2\2\2"+
		"\u0112\u0113\3\2\2\2\u0113\u0115\3\2\2\2\u0114\u010a\3\2\2\2\u0115\u0116"+
		"\3\2\2\2\u0116\u0114\3\2\2\2\u0116\u0117\3\2\2\2\u0117&\3\2\2\2\u0118"+
		"\u011a\7%\2\2\u0119\u011b\5\13\6\2\u011a\u0119\3\2\2\2\u011b\u011c\3\2"+
		"\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011f\3\2\2\2\u011e"+
		"\u0120\7%\2\2\u011f\u011e\3\2\2\2\u011f\u0120\3\2\2\2\u0120(\3\2\2\2\u0121"+
		"\u0123\7B\2\2\u0122\u0124\5\13\6\2\u0123\u0122\3\2\2\2\u0124\u0125\3\2"+
		"\2\2\u0125\u0123\3\2\2\2\u0125\u0126\3\2\2\2\u0126*\3\2\2\2\u0127\u0128"+
		"\7<\2\2\u0128\u0162\7+\2\2\u0129\u012a\7<\2\2\u012a\u012b\7/\2\2\u012b"+
		"\u0162\7+\2\2\u012c\u012d\7<\2\2\u012d\u012e\7/\2\2\u012e\u0162\7_\2\2"+
		"\u012f\u0130\7<\2\2\u0130\u0162\7F\2\2\u0131\u0132\7<\2\2\u0132\u0133"+
		"\7/\2\2\u0133\u0162\7F\2\2\u0134\u0135\7:\2\2\u0135\u0136\7/\2\2\u0136"+
		"\u0162\7+\2\2\u0137\u0138\7=\2\2\u0138\u0162\7+\2\2\u0139\u013a\7=\2\2"+
		"\u013a\u013b\7\u2013\2\2\u013b\u0162\7+\2\2\u013c\u013d\7<\2\2\u013d\u0162"+
		"\7*\2\2\u013e\u013f\7<\2\2\u013f\u0140\7/\2\2\u0140\u0162\7*\2\2\u0141"+
		"\u0142\7<\2\2\u0142\u0143\7)\2\2\u0143\u0162\7*\2\2\u0144\u0145\7<\2\2"+
		"\u0145\u0146\7\u2013\2\2\u0146\u0162\7\61\2\2\u0147\u0148\7<\2\2\u0148"+
		"\u0162\7\61\2\2\u0149\u014a\7<\2\2\u014a\u014b\7`\2\2\u014b\u0162\7+\2"+
		"\2\u014c\u014d\7\u00b1\2\2\u014d\u014e\7^\2\2\u014e\u014f\7a\2\2\u014f"+
		"\u0150\7*\2\2\u0150\u0151\7\u30c6\2\2\u0151\u0152\7+\2\2\u0152\u0153\7"+
		"a\2\2\u0153\u0154\7\61\2\2\u0154\u0162\7\u00b1\2\2\u0155\u0156\7Q\2\2"+
		"\u0156\u0157\7a\2\2\u0157\u0162\7q\2\2\u0158\u0159\7q\2\2\u0159\u015a"+
		"\7a\2\2\u015a\u0162\7Q\2\2\u015b\u015c\7Q\2\2\u015c\u015d\7a\2\2\u015d"+
		"\u0162\7Q\2\2\u015e\u015f\7^\2\2\u015f\u0160\7q\2\2\u0160\u0162\7\61\2"+
		"\2\u0161\u0127\3\2\2\2\u0161\u0129\3\2\2\2\u0161\u012c\3\2\2\2\u0161\u012f"+
		"\3\2\2\2\u0161\u0131\3\2\2\2\u0161\u0134\3\2\2\2\u0161\u0137\3\2\2\2\u0161"+
		"\u0139\3\2\2\2\u0161\u013c\3\2\2\2\u0161\u013e\3\2\2\2\u0161\u0141\3\2"+
		"\2\2\u0161\u0144\3\2\2\2\u0161\u0147\3\2\2\2\u0161\u0149\3\2\2\2\u0161"+
		"\u014c\3\2\2\2\u0161\u0155\3\2\2\2\u0161\u0158\3\2\2\2\u0161\u015b\3\2"+
		"\2\2\u0161\u015e\3\2\2\2\u0162,\3\2\2\2\u0163\u0178\7K\2\2\u0164\u0165"+
		"\7K\2\2\u0165\u0178\7K\2\2\u0166\u0167\7K\2\2\u0167\u0168\7K\2\2\u0168"+
		"\u0178\7K\2\2\u0169\u016a\7K\2\2\u016a\u0178\7X\2\2\u016b\u0178\7X\2\2"+
		"\u016c\u016d\7X\2\2\u016d\u0178\7K\2\2\u016e\u016f\7X\2\2\u016f\u0170"+
		"\7K\2\2\u0170\u0178\7K\2\2\u0171\u0172\7X\2\2\u0172\u0173\7K\2\2\u0173"+
		"\u0174\7K\2\2\u0174\u0178\7K\2\2\u0175\u0176\7K\2\2\u0176\u0178\7Z\2\2"+
		"\u0177\u0163\3\2\2\2\u0177\u0164\3\2\2\2\u0177\u0166\3\2\2\2\u0177\u0169"+
		"\3\2\2\2\u0177\u016b\3\2\2\2\u0177\u016c\3\2\2\2\u0177\u016e\3\2\2\2\u0177"+
		"\u0171\3\2\2\2\u0177\u0175\3\2\2\2\u0178\u017a\3\2\2\2\u0179\u017b\7\60"+
		"\2\2\u017a\u0179\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u017d\3\2\2\2\u017c"+
		"\u017e\5\21\t\2\u017d\u017c\3\2\2\2\u017d\u017e\3\2\2\2\u017e.\3\2\2\2"+
		"\u017f\u0180\5\7\4\2\u0180\u0181\7\60\2\2\u0181\u0183\3\2\2\2\u0182\u017f"+
		"\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0182\3\2\2\2\u0184\u0185\3\2\2\2\u0185"+
		"\u0187\3\2\2\2\u0186\u0188\5\7\4\2\u0187\u0186\3\2\2\2\u0187\u0188\3\2"+
		"\2\2\u0188\u018a\3\2\2\2\u0189\u018b\5\21\t\2\u018a\u0189\3\2\2\2\u018a"+
		"\u018b\3\2\2\2\u018b\60\3\2\2\2\u018c\u018e\5\t\5\2\u018d\u018c\3\2\2"+
		"\2\u018e\u018f\3\2\2\2\u018f\u018d\3\2\2\2\u018f\u0190\3\2\2\2\u0190\62"+
		"\3\2\2\2\u0191\u0193\5\13\6\2\u0192\u0191\3\2\2\2\u0193\u0194\3\2\2\2"+
		"\u0194\u0192\3\2\2\2\u0194\u0195\3\2\2\2\u0195\u0197\3\2\2\2\u0196\u0198"+
		"\5\21\t\2\u0197\u0196\3\2\2\2\u0197\u0198\3\2\2\2\u0198\64\3\2\2\2\u0199"+
		"\u01a7\5\r\7\2\u019a\u01a7\5\17\b\2\u019b\u01a7\7\u2028\2\2\u019c\u019d"+
		"\7\60\2\2\u019d\u019e\7\60\2\2\u019e\u01a7\7\60\2\2\u019f\u01a0\7*\2\2"+
		"\u01a0\u01a1\7#\2\2\u01a1\u01a7\7+\2\2\u01a2\u01a3\7*\2\2\u01a3\u01a4"+
		"\7A\2\2\u01a4\u01a7\7+\2\2\u01a5\u01a7\t\27\2\2\u01a6\u0199\3\2\2\2\u01a6"+
		"\u019a\3\2\2\2\u01a6\u019b\3\2\2\2\u01a6\u019c\3\2\2\2\u01a6\u019f\3\2"+
		"\2\2\u01a6\u01a2\3\2\2\2\u01a6\u01a5\3\2\2\2\u01a7\66\3\2\2\2\u01a8\u01aa"+
		"\n\30\2\2\u01a9\u01a8\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01a9\3\2\2\2"+
		"\u01ab\u01ac\3\2\2\2\u01ac8\3\2\2\2\u01ad\u01af\13\2\2\2\u01ae\u01ad\3"+
		"\2\2\2\u01af\u01b0\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b0\u01ae\3\2\2\2\u01b1"+
		":\3\2\2\29\2MR\\_dpsv{\u0087\u008a\u008c\u0092\u0098\u009b\u009e\u00a3"+
		"\u00a6\u00aa\u00b1\u00b5\u00bc\u00c0\u00c4\u00c7\u00c9\u00ce\u00d2\u00e5"+
		"\u00ef\u00f5\u00f9\u00fe\u0101\u0106\u010c\u0112\u0116\u011c\u011f\u0125"+
		"\u0161\u0177\u017a\u017d\u0184\u0187\u018a\u018f\u0194\u0197\u01a6\u01ab"+
		"\u01b0\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
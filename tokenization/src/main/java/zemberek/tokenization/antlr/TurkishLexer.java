// Generated from tokenization/src/main/resources/tokenization/TurkishLexer.g4 by ANTLR 4.7.2

package zemberek.tokenization.antlr;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import zemberek.core.turkish.TurkishAlphabet;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TurkishLexer extends Lexer {
  static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

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

  private static String[] makeRuleNames() {
    return new String[] {
      "Digit", "TurkishLetters", "TurkishLettersCapital", "TurkishLettersAll", 
      "AllTurkishAlphanumerical", "AllTurkishAlphanumericalUnderscore", "Apostrophe", 
      "DoubleQuote", "AposAndSuffix", "SpaceTab", "NewLine", "Hour", "MinuteSecond", 
      "Time", "Date", "PercentNumeral", "Number", "Integer", "Exp", "URLChar", 
      "URLFragment", "URL", "EmailLocalPart", "EmailDomainPart", "Email", "HashTag", 
      "Mention", "MetaTag", "Emoticon", "RomanNumeral", "AbbreviationWithDots", 
      "Word", "WordAlphanumerical", "WordWithSymbol", "PunctuationFragment", 
      "Punctuation", "UnknownWordPart", "UnknownWord", "Unknown"
    };
  }
  public static final String[] ruleNames = makeRuleNames();

  private static String[] makeLiteralNames() {
    return new String[] {
    };
  }
  private static final String[] _LITERAL_NAMES = makeLiteralNames();
  private static String[] makeSymbolicNames() {
    return new String[] {
      null, "Abbreviation", "SpaceTab", "NewLine", "Time", "Date", "PercentNumeral", 
      "Number", "URL", "Email", "HashTag", "Mention", "MetaTag", "Emoticon", 
      "RomanNumeral", "AbbreviationWithDots", "Word", "WordAlphanumerical", 
      "WordWithSymbol", "Punctuation", "UnknownWord", "Unknown"
    };
  }
  private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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


  private static final Locale LOCALE_TR = new Locale("tr");
  private static final Set<String> abbreviations = loadAbbreviations();
  private java.util.Queue<Token> queue = new java.util.LinkedList<Token>();

  private static Set<String> loadAbbreviations() {
    Set<String> set = Sets.newHashSet();
    try {
      List<String> lines = Resources.readLines(
          Resources.getResource("tokenization/abbreviations.txt"), Charsets.UTF_8);
      for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        for (String part : trimmed.split("[\\s\\u00a0]+")) {
          if (part.endsWith(".")) {
            set.add(part);
            if (!TurkishAlphabet.INSTANCE.containsVowel(part)) {
              set.add(part.toLowerCase(LOCALE_TR));
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Collections.unmodifiableSet(set);
  }

  @Override
  public Token nextToken() {
    if (!queue.isEmpty()) {
      return queue.poll();
    }

    Token next = super.nextToken();
    if (next.getType() != Word) {
      return next;
    }

    Token next2 = super.nextToken();
    if (next2.getType() == Punctuation && next2.getText().equals(".")) {
      String abbrev = next.getText() + ".";
      if (abbreviations != null && abbreviations.contains(abbrev)) {
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
    _interp = new CustomLexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
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
    "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\27\u0268\b\1\4\2"+
    "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
    "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
    "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
    "\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
    " \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\3\2\3\3\3\3"+
    "\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\6\nd\n\n\r\n"+
    "\16\ne\3\13\6\13i\n\13\r\13\16\13j\3\f\3\f\3\r\3\r\3\r\3\r\5\rs\n\r\3"+
    "\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\5\17}\n\17\3\17\5\17\u0080\n\17"+
    "\3\20\5\20\u0083\n\20\3\20\3\20\3\20\5\20\u0088\n\20\3\20\3\20\3\20\3"+
    "\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u0096\n\20\3\20\5\20"+
    "\u0099\n\20\3\20\5\20\u009c\n\20\3\20\3\20\3\20\5\20\u00a1\n\20\3\20\3"+
    "\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u00af\n\20"+
    "\3\20\5\20\u00b2\n\20\5\20\u00b4\n\20\3\21\3\21\3\21\3\22\5\22\u00ba\n"+
    "\22\3\22\3\22\3\22\3\22\5\22\u00c0\n\22\3\22\5\22\u00c3\n\22\3\22\5\22"+
    "\u00c6\n\22\3\22\3\22\3\22\5\22\u00cb\n\22\3\22\5\22\u00ce\n\22\3\22\3"+
    "\22\5\22\u00d2\n\22\3\22\5\22\u00d5\n\22\3\22\3\22\3\22\3\22\5\22\u00db"+
    "\n\22\3\22\3\22\3\22\6\22\u00e0\n\22\r\22\16\22\u00e1\3\22\3\22\5\22\u00e6"+
    "\n\22\3\22\3\22\3\22\6\22\u00eb\n\22\r\22\16\22\u00ec\3\22\3\22\5\22\u00f1"+
    "\n\22\3\22\3\22\5\22\u00f5\n\22\3\22\5\22\u00f8\n\22\5\22\u00fa\n\22\3"+
    "\23\6\23\u00fd\n\23\r\23\16\23\u00fe\3\24\3\24\5\24\u0103\n\24\3\24\3"+
    "\24\3\25\3\25\3\26\3\26\3\26\7\26\u010c\n\26\f\26\16\26\u010f\13\26\3"+
    "\26\3\26\3\26\5\26\u0114\n\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u0125\n\27\3\27\3\27\5\27\u0129"+
    "\n\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\5\27\u013a\n\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u0143"+
    "\n\27\3\27\6\27\u0146\n\27\r\27\16\27\u0147\3\27\3\27\3\27\3\27\3\27\3"+
    "\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3"+
    "\27\3\27\3\27\3\27\3\27\3\27\5\27\u0163\n\27\3\27\3\27\3\27\5\27\u0168"+
    "\n\27\3\27\3\27\5\27\u016c\n\27\3\27\5\27\u016f\n\27\5\27\u0171\n\27\3"+
    "\30\6\30\u0174\n\30\r\30\16\30\u0175\3\30\3\30\6\30\u017a\n\30\r\30\16"+
    "\30\u017b\7\30\u017e\n\30\f\30\16\30\u0181\13\30\3\31\6\31\u0184\n\31"+
    "\r\31\16\31\u0185\3\31\3\31\6\31\u018a\n\31\r\31\16\31\u018b\7\31\u018e"+
    "\n\31\f\31\16\31\u0191\13\31\3\32\3\32\3\32\3\32\3\32\6\32\u0198\n\32"+
    "\r\32\16\32\u0199\3\32\5\32\u019d\n\32\3\33\3\33\6\33\u01a1\n\33\r\33"+
    "\16\33\u01a2\3\33\5\33\u01a6\n\33\3\34\3\34\6\34\u01aa\n\34\r\34\16\34"+
    "\u01ab\3\34\5\34\u01af\n\34\3\35\3\35\6\35\u01b3\n\35\r\35\16\35\u01b4"+
    "\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
    "\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
    "\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
    "\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
    "\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
    "\3\36\3\36\3\36\3\36\3\36\3\36\3\36\5\36\u0204\n\36\3\37\6\37\u0207\n"+
    "\37\r\37\16\37\u0208\3\37\5\37\u020c\n\37\3\37\5\37\u020f\n\37\3 \3 \3"+
    " \6 \u0214\n \r \16 \u0215\3 \5 \u0219\n \3 \5 \u021c\n \3!\6!\u021f\n"+
    "!\r!\16!\u0220\3\"\6\"\u0224\n\"\r\"\16\"\u0225\3#\6#\u0229\n#\r#\16#"+
    "\u022a\3#\3#\6#\u022f\n#\r#\16#\u0230\6#\u0233\n#\r#\16#\u0234\3#\5#\u0238"+
    "\n#\3#\6#\u023b\n#\r#\16#\u023c\3#\6#\u0240\n#\r#\16#\u0241\5#\u0244\n"+
    "#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\5$\u0252\n$\3%\3%\3&\6&\u0257\n"+
    "&\r&\16&\u0258\3\'\3\'\3\'\3\'\7\'\u025f\n\'\f\'\16\'\u0262\13\'\3(\6"+
    "(\u0265\n(\r(\16(\u0266\3\u0266\2)\3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2"+
    "\23\2\25\4\27\5\31\2\33\2\35\6\37\7!\b#\t%\2\'\2)\2+\2-\n/\2\61\2\63\13"+
    "\65\f\67\r9\16;\17=\20?\21A\22C\23E\24G\2I\25K\2M\26O\27\3\2\35\3\2\62"+
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
    "))\u201b\u201b\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e\u201f\5\2\13\13\"\""+
    "\u00a2\u00a2\4\2\f\f\17\17\3\2\62\63\3\2\62\65\3\2\62\67\4\2\60\60<<\3"+
    "\2\63\63\3\29;\3\2\64\64\3\2\62\62\4\2--//\4\2..\60\60\4\2GGgg\33\2%%"+
    "\'(--//\61=??AAC]__aac|\u0080\u0080\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0"+
    "\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8"+
    "\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\6\2\62;C\\aac|\4\2--"+
    "/\60\5\2KKXXZZ\30\2##&(*\61<B]`bb}\u0080\u00a5\u00a5\u00a7\u00a7\u00a9"+
    "\u00a9\u00ab\u00ab\u00b0\u00b0\u00b2\u00b3\u00d9\u00d9\u00f9\u00f9\u2015"+
    "\u2016\u201a\u201a\u2028\u2028\u20ae\u20ae\u20bc\u20bc\u2122\u2122\u2124"+
    "\u2124\35\2\13\f\17\17\"$&\61<B]`bb}\u0080\u00a2\u00a2\u00a5\u00a5\u00a7"+
    "\u00a7\u00a9\u00a9\u00ab\u00ab\u00ad\u00ad\u00b0\u00b0\u00b2\u00b3\u00bd"+
    "\u00bd\u00d9\u00d9\u00f9\u00f9\u2015\u2016\u201a\u201b\u201e\u201f\u2028"+
    "\u2028\u20ae\u20ae\u20bc\u20bc\u2122\u2122\u2124\u2124\2\u02cc\2\25\3"+
    "\2\2\2\2\27\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2"+
    "-\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2"+
    "\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2I\3\2\2\2\2M"+
    "\3\2\2\2\2O\3\2\2\2\3Q\3\2\2\2\5S\3\2\2\2\7U\3\2\2\2\tW\3\2\2\2\13Y\3"+
    "\2\2\2\r[\3\2\2\2\17]\3\2\2\2\21_\3\2\2\2\23a\3\2\2\2\25h\3\2\2\2\27l"+
    "\3\2\2\2\31r\3\2\2\2\33t\3\2\2\2\35w\3\2\2\2\37\u00b3\3\2\2\2!\u00b5\3"+
    "\2\2\2#\u00f9\3\2\2\2%\u00fc\3\2\2\2\'\u0100\3\2\2\2)\u0106\3\2\2\2+\u0113"+
    "\3\2\2\2-\u0170\3\2\2\2/\u0173\3\2\2\2\61\u0183\3\2\2\2\63\u0192\3\2\2"+
    "\2\65\u019e\3\2\2\2\67\u01a7\3\2\2\29\u01b0\3\2\2\2;\u0203\3\2\2\2=\u0206"+
    "\3\2\2\2?\u0213\3\2\2\2A\u021e\3\2\2\2C\u0223\3\2\2\2E\u0243\3\2\2\2G"+
    "\u0251\3\2\2\2I\u0253\3\2\2\2K\u0256\3\2\2\2M\u025a\3\2\2\2O\u0264\3\2"+
    "\2\2QR\t\2\2\2R\4\3\2\2\2ST\t\3\2\2T\6\3\2\2\2UV\t\4\2\2V\b\3\2\2\2WX"+
    "\t\5\2\2X\n\3\2\2\2YZ\t\6\2\2Z\f\3\2\2\2[\\\t\7\2\2\\\16\3\2\2\2]^\t\b"+
    "\2\2^\20\3\2\2\2_`\t\t\2\2`\22\3\2\2\2ac\5\17\b\2bd\5\t\5\2cb\3\2\2\2"+
    "de\3\2\2\2ec\3\2\2\2ef\3\2\2\2f\24\3\2\2\2gi\t\n\2\2hg\3\2\2\2ij\3\2\2"+
    "\2jh\3\2\2\2jk\3\2\2\2k\26\3\2\2\2lm\t\13\2\2m\30\3\2\2\2no\t\f\2\2os"+
    "\t\2\2\2pq\7\64\2\2qs\t\r\2\2rn\3\2\2\2rp\3\2\2\2s\32\3\2\2\2tu\t\16\2"+
    "\2uv\t\2\2\2v\34\3\2\2\2wx\5\31\r\2xy\t\17\2\2y|\5\33\16\2z{\t\17\2\2"+
    "{}\5\33\16\2|z\3\2\2\2|}\3\2\2\2}\177\3\2\2\2~\u0080\5\23\n\2\177~\3\2"+
    "\2\2\177\u0080\3\2\2\2\u0080\36\3\2\2\2\u0081\u0083\t\r\2\2\u0082\u0081"+
    "\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0085\t\2\2\2\u0085"+
    "\u0087\7\60\2\2\u0086\u0088\t\f\2\2\u0087\u0086\3\2\2\2\u0087\u0088\3"+
    "\2\2\2\u0088\u0089\3\2\2\2\u0089\u008a\t\2\2\2\u008a\u0095\7\60\2\2\u008b"+
    "\u008c\t\20\2\2\u008c\u008d\t\21\2\2\u008d\u008e\t\2\2\2\u008e\u0096\t"+
    "\2\2\2\u008f\u0090\t\22\2\2\u0090\u0091\t\23\2\2\u0091\u0092\t\2\2\2\u0092"+
    "\u0096\t\2\2\2\u0093\u0094\t\2\2\2\u0094\u0096\t\2\2\2\u0095\u008b\3\2"+
    "\2\2\u0095\u008f\3\2\2\2\u0095\u0093\3\2\2\2\u0096\u0098\3\2\2\2\u0097"+
    "\u0099\5\23\n\2\u0098\u0097\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u00b4\3"+
    "\2\2\2\u009a\u009c\t\r\2\2\u009b\u009a\3\2\2\2\u009b\u009c\3\2\2\2\u009c"+
    "\u009d\3\2\2\2\u009d\u009e\t\2\2\2\u009e\u00a0\7\61\2\2\u009f\u00a1\t"+
    "\f\2\2\u00a0\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2"+
    "\u00a3\t\2\2\2\u00a3\u00ae\7\61\2\2\u00a4\u00a5\t\20\2\2\u00a5\u00a6\t"+
    "\21\2\2\u00a6\u00a7\t\2\2\2\u00a7\u00af\t\2\2\2\u00a8\u00a9\t\22\2\2\u00a9"+
    "\u00aa\t\23\2\2\u00aa\u00ab\t\2\2\2\u00ab\u00af\t\2\2\2\u00ac\u00ad\t"+
    "\2\2\2\u00ad\u00af\t\2\2\2\u00ae\u00a4\3\2\2\2\u00ae\u00a8\3\2\2\2\u00ae"+
    "\u00ac\3\2\2\2\u00af\u00b1\3\2\2\2\u00b0\u00b2\5\23\n\2\u00b1\u00b0\3"+
    "\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b4\3\2\2\2\u00b3\u0082\3\2\2\2\u00b3"+
    "\u009b\3\2\2\2\u00b4 \3\2\2\2\u00b5\u00b6\7\'\2\2\u00b6\u00b7\5#\22\2"+
    "\u00b7\"\3\2\2\2\u00b8\u00ba\t\24\2\2\u00b9\u00b8\3\2\2\2\u00b9\u00ba"+
    "\3\2\2\2\u00ba\u00bb\3\2\2\2\u00bb\u00bc\5%\23\2\u00bc\u00bd\t\25\2\2"+
    "\u00bd\u00bf\5%\23\2\u00be\u00c0\5\'\24\2\u00bf\u00be\3\2\2\2\u00bf\u00c0"+
    "\3\2\2\2\u00c0\u00c2\3\2\2\2\u00c1\u00c3\5\23\n\2\u00c2\u00c1\3\2\2\2"+
    "\u00c2\u00c3\3\2\2\2\u00c3\u00fa\3\2\2\2\u00c4\u00c6\t\24\2\2\u00c5\u00c4"+
    "\3\2\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00c7\3\2\2\2\u00c7\u00c8\5%\23\2\u00c8"+
    "\u00ca\5\'\24\2\u00c9\u00cb\5\23\n\2\u00ca\u00c9\3\2\2\2\u00ca\u00cb\3"+
    "\2\2\2\u00cb\u00fa\3\2\2\2\u00cc\u00ce\t\24\2\2\u00cd\u00cc\3\2\2\2\u00cd"+
    "\u00ce\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00d1\5%\23\2\u00d0\u00d2\5\23"+
    "\n\2\u00d1\u00d0\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00fa\3\2\2\2\u00d3"+
    "\u00d5\t\24\2\2\u00d4\u00d3\3\2\2\2\u00d4\u00d5\3\2\2\2\u00d5\u00d6\3"+
    "\2\2\2\u00d6\u00d7\5%\23\2\u00d7\u00d8\7\61\2\2\u00d8\u00da\5%\23\2\u00d9"+
    "\u00db\5\23\n\2\u00da\u00d9\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u00fa\3"+
    "\2\2\2\u00dc\u00dd\5%\23\2\u00dd\u00de\7\60\2\2\u00de\u00e0\3\2\2\2\u00df"+
    "\u00dc\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00df\3\2\2\2\u00e1\u00e2\3\2"+
    "\2\2\u00e2\u00e3\3\2\2\2\u00e3\u00e5\5%\23\2\u00e4\u00e6\5\23\n\2\u00e5"+
    "\u00e4\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00fa\3\2\2\2\u00e7\u00e8\5%"+
    "\23\2\u00e8\u00e9\7.\2\2\u00e9\u00eb\3\2\2\2\u00ea\u00e7\3\2\2\2\u00eb"+
    "\u00ec\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00ee\3\2"+
    "\2\2\u00ee\u00f0\5%\23\2\u00ef\u00f1\5\23\n\2\u00f0\u00ef\3\2\2\2\u00f0"+
    "\u00f1\3\2\2\2\u00f1\u00fa\3\2\2\2\u00f2\u00f4\5%\23\2\u00f3\u00f5\7\60"+
    "\2\2\u00f4\u00f3\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f7\3\2\2\2\u00f6"+
    "\u00f8\5\23\n\2\u00f7\u00f6\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00fa\3"+
    "\2\2\2\u00f9\u00b9\3\2\2\2\u00f9\u00c5\3\2\2\2\u00f9\u00cd\3\2\2\2\u00f9"+
    "\u00d4\3\2\2\2\u00f9\u00df\3\2\2\2\u00f9\u00ea\3\2\2\2\u00f9\u00f2\3\2"+
    "\2\2\u00fa$\3\2\2\2\u00fb\u00fd\5\3\2\2\u00fc\u00fb\3\2\2\2\u00fd\u00fe"+
    "\3\2\2\2\u00fe\u00fc\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff&\3\2\2\2\u0100"+
    "\u0102\t\26\2\2\u0101\u0103\t\24\2\2\u0102\u0101\3\2\2\2\u0102\u0103\3"+
    "\2\2\2\u0103\u0104\3\2\2\2\u0104\u0105\5%\23\2\u0105(\3\2\2\2\u0106\u0107"+
    "\t\27\2\2\u0107*\3\2\2\2\u0108\u010d\5)\25\2\u0109\u010c\5)\25\2\u010a"+
    "\u010c\7\60\2\2\u010b\u0109\3\2\2\2\u010b\u010a\3\2\2\2\u010c\u010f\3"+
    "\2\2\2\u010d\u010b\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u0110\3\2\2\2\u010f"+
    "\u010d\3\2\2\2\u0110\u0111\5)\25\2\u0111\u0114\3\2\2\2\u0112\u0114\5)"+
    "\25\2\u0113\u0108\3\2\2\2\u0113\u0112\3\2\2\2\u0114,\3\2\2\2\u0115\u0116"+
    "\7j\2\2\u0116\u0117\7v\2\2\u0117\u0118\7v\2\2\u0118\u0119\7r\2\2\u0119"+
    "\u011a\7<\2\2\u011a\u011b\7\61\2\2\u011b\u0125\7\61\2\2\u011c\u011d\7"+
    "j\2\2\u011d\u011e\7v\2\2\u011e\u011f\7v\2\2\u011f\u0120\7r\2\2\u0120\u0121"+
    "\7u\2\2\u0121\u0122\7<\2\2\u0122\u0123\7\61\2\2\u0123\u0125\7\61\2\2\u0124"+
    "\u0115\3\2\2\2\u0124\u011c\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u0128\5+"+
    "\26\2\u0127\u0129\5\23\n\2\u0128\u0127\3\2\2\2\u0128\u0129\3\2\2\2\u0129"+
    "\u0171\3\2\2\2\u012a\u012b\7j\2\2\u012b\u012c\7v\2\2\u012c\u012d\7v\2"+
    "\2\u012d\u012e\7r\2\2\u012e\u012f\7<\2\2\u012f\u0130\7\61\2\2\u0130\u013a"+
    "\7\61\2\2\u0131\u0132\7j\2\2\u0132\u0133\7v\2\2\u0133\u0134\7v\2\2\u0134"+
    "\u0135\7r\2\2\u0135\u0136\7u\2\2\u0136\u0137\7<\2\2\u0137\u0138\7\61\2"+
    "\2\u0138\u013a\7\61\2\2\u0139\u012a\3\2\2\2\u0139\u0131\3\2\2\2\u0139"+
    "\u013a\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u013c\7y\2\2\u013c\u013d\7y\2"+
    "\2\u013d\u013e\7y\2\2\u013e\u013f\7\60\2\2\u013f\u0140\3\2\2\2\u0140\u0142"+
    "\5+\26\2\u0141\u0143\5\23\n\2\u0142\u0141\3\2\2\2\u0142\u0143\3\2\2\2"+
    "\u0143\u0171\3\2\2\2\u0144\u0146\t\30\2\2\u0145\u0144\3\2\2\2\u0146\u0147"+
    "\3\2\2\2\u0147\u0145\3\2\2\2\u0147\u0148\3\2\2\2\u0148\u0162\3\2\2\2\u0149"+
    "\u014a\7\60\2\2\u014a\u014b\7e\2\2\u014b\u014c\7q\2\2\u014c\u0163\7o\2"+
    "\2\u014d\u014e\7\60\2\2\u014e\u014f\7q\2\2\u014f\u0150\7t\2\2\u0150\u0163"+
    "\7i\2\2\u0151\u0152\7\60\2\2\u0152\u0153\7g\2\2\u0153\u0154\7f\2\2\u0154"+
    "\u0163\7w\2\2\u0155\u0156\7\60\2\2\u0156\u0157\7i\2\2\u0157\u0158\7q\2"+
    "\2\u0158\u0163\7x\2\2\u0159\u015a\7\60\2\2\u015a\u015b\7p\2\2\u015b\u015c"+
    "\7g\2\2\u015c\u0163\7v\2\2\u015d\u015e\7\60\2\2\u015e\u015f\7k\2\2\u015f"+
    "\u0160\7p\2\2\u0160\u0161\7h\2\2\u0161\u0163\7q\2\2\u0162\u0149\3\2\2"+
    "\2\u0162\u014d\3\2\2\2\u0162\u0151\3\2\2\2\u0162\u0155\3\2\2\2\u0162\u0159"+
    "\3\2\2\2\u0162\u015d\3\2\2\2\u0163\u0167\3\2\2\2\u0164\u0165\7\60\2\2"+
    "\u0165\u0166\7v\2\2\u0166\u0168\7t\2\2\u0167\u0164\3\2\2\2\u0167\u0168"+
    "\3\2\2\2\u0168\u016b\3\2\2\2\u0169\u016a\7\61\2\2\u016a\u016c\5+\26\2"+
    "\u016b\u0169\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016e\3\2\2\2\u016d\u016f"+
    "\5\23\n\2\u016e\u016d\3\2\2\2\u016e\u016f\3\2\2\2\u016f\u0171\3\2\2\2"+
    "\u0170\u0124\3\2\2\2\u0170\u0139\3\2\2\2\u0170\u0145\3\2\2\2\u0171.\3"+
    "\2\2\2\u0172\u0174\5\r\7\2\u0173\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175"+
    "\u0173\3\2\2\2\u0175\u0176\3\2\2\2\u0176\u017f\3\2\2\2\u0177\u0179\t\31"+
    "\2\2\u0178\u017a\5\r\7\2\u0179\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b"+
    "\u0179\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017e\3\2\2\2\u017d\u0177\3\2"+
    "\2\2\u017e\u0181\3\2\2\2\u017f\u017d\3\2\2\2\u017f\u0180\3\2\2\2\u0180"+
    "\60\3\2\2\2\u0181\u017f\3\2\2\2\u0182\u0184\5\r\7\2\u0183\u0182\3\2\2"+
    "\2\u0184\u0185\3\2\2\2\u0185\u0183\3\2\2\2\u0185\u0186\3\2\2\2\u0186\u018f"+
    "\3\2\2\2\u0187\u0189\7/\2\2\u0188\u018a\5\r\7\2\u0189\u0188\3\2\2\2\u018a"+
    "\u018b\3\2\2\2\u018b\u0189\3\2\2\2\u018b\u018c\3\2\2\2\u018c\u018e\3\2"+
    "\2\2\u018d\u0187\3\2\2\2\u018e\u0191\3\2\2\2\u018f\u018d\3\2\2\2\u018f"+
    "\u0190\3\2\2\2\u0190\62\3\2\2\2\u0191\u018f\3\2\2\2\u0192\u0193\5/\30"+
    "\2\u0193\u0194\7B\2\2\u0194\u0197\5\61\31\2\u0195\u0196\7\60\2\2\u0196"+
    "\u0198\5\61\31\2\u0197\u0195\3\2\2\2\u0198\u0199\3\2\2\2\u0199\u0197\3"+
    "\2\2\2\u0199\u019a\3\2\2\2\u019a\u019c\3\2\2\2\u019b\u019d\5\23\n\2\u019c"+
    "\u019b\3\2\2\2\u019c\u019d\3\2\2\2\u019d\64\3\2\2\2\u019e\u01a0\7%\2\2"+
    "\u019f\u01a1\5\r\7\2\u01a0\u019f\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\u01a0"+
    "\3\2\2\2\u01a2\u01a3\3\2\2\2\u01a3\u01a5\3\2\2\2\u01a4\u01a6\5\23\n\2"+
    "\u01a5\u01a4\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\66\3\2\2\2\u01a7\u01a9"+
    "\7B\2\2\u01a8\u01aa\5\r\7\2\u01a9\u01a8\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab"+
    "\u01a9\3\2\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01ae\3\2\2\2\u01ad\u01af\5\23"+
    "\n\2\u01ae\u01ad\3\2\2\2\u01ae\u01af\3\2\2\2\u01af8\3\2\2\2\u01b0\u01b2"+
    "\7>\2\2\u01b1\u01b3\5\r\7\2\u01b2\u01b1\3\2\2\2\u01b3\u01b4\3\2\2\2\u01b4"+
    "\u01b2\3\2\2\2\u01b4\u01b5\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\u01b7\7@"+
    "\2\2\u01b7:\3\2\2\2\u01b8\u01b9\7<\2\2\u01b9\u0204\7+\2\2\u01ba\u01bb"+
    "\7<\2\2\u01bb\u01bc\7/\2\2\u01bc\u0204\7+\2\2\u01bd\u01be\7<\2\2\u01be"+
    "\u01bf\7/\2\2\u01bf\u0204\7_\2\2\u01c0\u01c1\7<\2\2\u01c1\u0204\7F\2\2"+
    "\u01c2\u01c3\7<\2\2\u01c3\u01c4\7/\2\2\u01c4\u0204\7F\2\2\u01c5\u01c6"+
    "\7:\2\2\u01c6\u01c7\7/\2\2\u01c7\u0204\7+\2\2\u01c8\u01c9\7=\2\2\u01c9"+
    "\u0204\7+\2\2\u01ca\u01cb\7=\2\2\u01cb\u01cc\7\u2013\2\2\u01cc\u0204\7"+
    "+\2\2\u01cd\u01ce\7<\2\2\u01ce\u0204\7*\2\2\u01cf\u01d0\7<\2\2\u01d0\u01d1"+
    "\7/\2\2\u01d1\u0204\7*\2\2\u01d2\u01d3\7<\2\2\u01d3\u01d4\7)\2\2\u01d4"+
    "\u0204\7*\2\2\u01d5\u01d6\7<\2\2\u01d6\u01d7\7)\2\2\u01d7\u0204\7+\2\2"+
    "\u01d8\u01d9\7<\2\2\u01d9\u0204\7R\2\2\u01da\u01db\7<\2\2\u01db\u0204"+
    "\7r\2\2\u01dc\u01dd\7<\2\2\u01dd\u0204\7~\2\2\u01de\u01df\7?\2\2\u01df"+
    "\u0204\7~\2\2\u01e0\u01e1\7?\2\2\u01e1\u0204\7+\2\2\u01e2\u01e3\7?\2\2"+
    "\u01e3\u0204\7*\2\2\u01e4\u01e5\7<\2\2\u01e5\u01e6\7\u2013\2\2\u01e6\u0204"+
    "\7\61\2\2\u01e7\u01e8\7<\2\2\u01e8\u0204\7\61\2\2\u01e9\u01ea\7<\2\2\u01ea"+
    "\u01eb\7`\2\2\u01eb\u0204\7+\2\2\u01ec\u01ed\7\u00b1\2\2\u01ed\u01ee\7"+
    "^\2\2\u01ee\u01ef\7a\2\2\u01ef\u01f0\7*\2\2\u01f0\u01f1\7\u30c6\2\2\u01f1"+
    "\u01f2\7+\2\2\u01f2\u01f3\7a\2\2\u01f3\u01f4\7\61\2\2\u01f4\u0204\7\u00b1"+
    "\2\2\u01f5\u01f6\7Q\2\2\u01f6\u01f7\7a\2\2\u01f7\u0204\7q\2\2\u01f8\u01f9"+
    "\7q\2\2\u01f9\u01fa\7a\2\2\u01fa\u0204\7Q\2\2\u01fb\u01fc\7Q\2\2\u01fc"+
    "\u01fd\7a\2\2\u01fd\u0204\7Q\2\2\u01fe\u01ff\7^\2\2\u01ff\u0200\7q\2\2"+
    "\u0200\u0204\7\61\2\2\u0201\u0202\7>\2\2\u0202\u0204\7\65\2\2\u0203\u01b8"+
    "\3\2\2\2\u0203\u01ba\3\2\2\2\u0203\u01bd\3\2\2\2\u0203\u01c0\3\2\2\2\u0203"+
    "\u01c2\3\2\2\2\u0203\u01c5\3\2\2\2\u0203\u01c8\3\2\2\2\u0203\u01ca\3\2"+
    "\2\2\u0203\u01cd\3\2\2\2\u0203\u01cf\3\2\2\2\u0203\u01d2\3\2\2\2\u0203"+
    "\u01d5\3\2\2\2\u0203\u01d8\3\2\2\2\u0203\u01da\3\2\2\2\u0203\u01dc\3\2"+
    "\2\2\u0203\u01de\3\2\2\2\u0203\u01e0\3\2\2\2\u0203\u01e2\3\2\2\2\u0203"+
    "\u01e4\3\2\2\2\u0203\u01e7\3\2\2\2\u0203\u01e9\3\2\2\2\u0203\u01ec\3\2"+
    "\2\2\u0203\u01f5\3\2\2\2\u0203\u01f8\3\2\2\2\u0203\u01fb\3\2\2\2\u0203"+
    "\u01fe\3\2\2\2\u0203\u0201\3\2\2\2\u0204<\3\2\2\2\u0205\u0207\t\32\2\2"+
    "\u0206\u0205\3\2\2\2\u0207\u0208\3\2\2\2\u0208\u0206\3\2\2\2\u0208\u0209"+
    "\3\2\2\2\u0209\u020b\3\2\2\2\u020a\u020c\7\60\2\2\u020b\u020a\3\2\2\2"+
    "\u020b\u020c\3\2\2\2\u020c\u020e\3\2\2\2\u020d\u020f\5\23\n\2\u020e\u020d"+
    "\3\2\2\2\u020e\u020f\3\2\2\2\u020f>\3\2\2\2\u0210\u0211\5\7\4\2\u0211"+
    "\u0212\7\60\2\2\u0212\u0214\3\2\2\2\u0213\u0210\3\2\2\2\u0214\u0215\3"+
    "\2\2\2\u0215\u0213\3\2\2\2\u0215\u0216\3\2\2\2\u0216\u0218\3\2\2\2\u0217"+
    "\u0219\5\7\4\2\u0218\u0217\3\2\2\2\u0218\u0219\3\2\2\2\u0219\u021b\3\2"+
    "\2\2\u021a\u021c\5\23\n\2\u021b\u021a\3\2\2\2\u021b\u021c\3\2\2\2\u021c"+
    "@\3\2\2\2\u021d\u021f\5\t\5\2\u021e\u021d\3\2\2\2\u021f\u0220\3\2\2\2"+
    "\u0220\u021e\3\2\2\2\u0220\u0221\3\2\2\2\u0221B\3\2\2\2\u0222\u0224\5"+
    "\13\6\2\u0223\u0222\3\2\2\2\u0224\u0225\3\2\2\2\u0225\u0223\3\2\2\2\u0225"+
    "\u0226\3\2\2\2\u0226D\3\2\2\2\u0227\u0229\5\13\6\2\u0228\u0227\3\2\2\2"+
    "\u0229\u022a\3\2\2\2\u022a\u0228\3\2\2\2\u022a\u022b\3\2\2\2\u022b\u0232"+
    "\3\2\2\2\u022c\u022e\7/\2\2\u022d\u022f\5\13\6\2\u022e\u022d\3\2\2\2\u022f"+
    "\u0230\3\2\2\2\u0230\u022e\3\2\2\2\u0230\u0231\3\2\2\2\u0231\u0233\3\2"+
    "\2\2\u0232\u022c\3\2\2\2\u0233\u0234\3\2\2\2\u0234\u0232\3\2\2\2\u0234"+
    "\u0235\3\2\2\2\u0235\u0237\3\2\2\2\u0236\u0238\5\23\n\2\u0237\u0236\3"+
    "\2\2\2\u0237\u0238\3\2\2\2\u0238\u0244\3\2\2\2\u0239\u023b\5\13\6\2\u023a"+
    "\u0239\3\2\2\2\u023b\u023c\3\2\2\2\u023c\u023a\3\2\2\2\u023c\u023d\3\2"+
    "\2\2\u023d\u023f\3\2\2\2\u023e\u0240\5\23\n\2\u023f\u023e\3\2\2\2\u0240"+
    "\u0241\3\2\2\2\u0241\u023f\3\2\2\2\u0241\u0242\3\2\2\2\u0242\u0244\3\2"+
    "\2\2\u0243\u0228\3\2\2\2\u0243\u023a\3\2\2\2\u0244F\3\2\2\2\u0245\u0252"+
    "\5\17\b\2\u0246\u0252\5\21\t\2\u0247\u0248\7\60\2\2\u0248\u0249\7\60\2"+
    "\2\u0249\u0252\7\60\2\2\u024a\u024b\7*\2\2\u024b\u024c\7#\2\2\u024c\u0252"+
    "\7+\2\2\u024d\u024e\7*\2\2\u024e\u024f\7A\2\2\u024f\u0252\7+\2\2\u0250"+
    "\u0252\t\33\2\2\u0251\u0245\3\2\2\2\u0251\u0246\3\2\2\2\u0251\u0247\3"+
    "\2\2\2\u0251\u024a\3\2\2\2\u0251\u024d\3\2\2\2\u0251\u0250\3\2\2\2\u0252"+
    "H\3\2\2\2\u0253\u0254\5G$\2\u0254J\3\2\2\2\u0255\u0257\n\34\2\2\u0256"+
    "\u0255\3\2\2\2\u0257\u0258\3\2\2\2\u0258\u0256\3\2\2\2\u0258\u0259\3\2"+
    "\2\2\u0259L\3\2\2\2\u025a\u0260\5K&\2\u025b\u025c\5\17\b\2\u025c\u025d"+
    "\5K&\2\u025d\u025f\3\2\2\2\u025e\u025b\3\2\2\2\u025f\u0262\3\2\2\2\u0260"+
    "\u025e\3\2\2\2\u0260\u0261\3\2\2\2\u0261N\3\2\2\2\u0262\u0260\3\2\2\2"+
    "\u0263\u0265\13\2\2\2\u0264\u0263\3\2\2\2\u0265\u0266\3\2\2\2\u0266\u0267"+
    "\3\2\2\2\u0266\u0264\3\2\2\2\u0267P\3\2\2\2Q\2ejr|\177\u0082\u0087\u0095"+
    "\u0098\u009b\u00a0\u00ae\u00b1\u00b3\u00b9\u00bf\u00c2\u00c5\u00ca\u00cd"+
    "\u00d1\u00d4\u00da\u00e1\u00e5\u00ec\u00f0\u00f4\u00f7\u00f9\u00fe\u0102"+
    "\u010b\u010d\u0113\u0124\u0128\u0139\u0142\u0147\u0162\u0167\u016b\u016e"+
    "\u0170\u0175\u017b\u017f\u0185\u018b\u018f\u0199\u019c\u01a2\u01a5\u01ab"+
    "\u01ae\u01b4\u0203\u0208\u020b\u020e\u0215\u0218\u021b\u0220\u0225\u022a"+
    "\u0230\u0234\u0237\u023c\u0241\u0243\u0251\u0258\u0260\u0266\2";
  public static final ATN _ATN =
    new ATNDeserializer().deserialize(_serializedATN.toCharArray());
  static {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
      _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
    }
  }
}
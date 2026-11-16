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
    Number=7, URL=8, Email=9, HashTag=10, Mention=11, MetaTag=12, Emoji=13, 
    Emoticon=14, RomanNumeral=15, AbbreviationWithDots=16, Word=17, WordAlphanumerical=18, 
    WordWithSymbol=19, Punctuation=20, UnknownWord=21, Unknown=22;
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
      "Mention", "MetaTag", "RegionalIndicator", "EmojiModifier", "EmojiBase", 
      "Emoji", "Emoticon", "RomanNumeral", "AbbreviationWithDots", "Word", 
      "WordAlphanumerical", "WordWithSymbol", "PunctuationFragment", "Punctuation", 
      "UnknownWordPart", "UnknownWord", "Unknown"
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
      "Number", "URL", "Email", "HashTag", "Mention", "MetaTag", "Emoji", "Emoticon", 
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
  private Token deferredToken;

  private TurkishLexer(CharStream input, boolean useCustomSimulator) {
    super(input);
    _interp = new CustomLexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
  }

  public static TurkishLexer create(CharStream input) {
    TurkishLexer lexer = new TurkishLexer(input, true);
    lexer.removeErrorListeners();
    return lexer;
  }

  @Override
  public CustomLexerATNSimulator getInterpreter() {
    if (!(_interp instanceof CustomLexerATNSimulator)) {
      _interp = new CustomLexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }
    return (CustomLexerATNSimulator) _interp;
  }

  public static boolean isAbbreviation(String wordWithDot) {
    return abbreviations.contains(wordWithDot);
  }

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
    if (deferredToken != null) {
      Token res = deferredToken;
      deferredToken = null;
      return res;
    }

    Token next = super.nextToken();
    if (next.getType() != Word || _input.LA(1) != '.') {
      return next;
    }

    Token next2 = super.nextToken();
    if (next2.getType() == Punctuation && next2.getStartIndex() == next2.getStopIndex()) {
      String abbrev = next.getText() + ".";
      if (abbreviations.contains(abbrev)) {
        CommonToken commonToken = new CommonToken(Abbreviation, abbrev);
        commonToken.setStartIndex(next.getStartIndex());
        commonToken.setStopIndex(next2.getStopIndex());
        commonToken.setTokenIndex(next.getTokenIndex());
        commonToken.setCharPositionInLine(next.getCharPositionInLine());
        commonToken.setLine(next.getLine());
        return commonToken;
      }
    }
    deferredToken = next2;
    return next;
  }


  public TurkishLexer(CharStream input) {
    super(input);
    _interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
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
    "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\30\u0288\b\1\4\2"+
    "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
    "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
    "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
    "\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
    " \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
    "+\4,\t,\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3"+
    "\t\3\n\3\n\6\nl\n\n\r\n\16\nm\3\13\6\13q\n\13\r\13\16\13r\3\f\3\f\3\r"+
    "\3\r\3\r\3\r\5\r{\n\r\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\5\17\u0085"+
    "\n\17\3\17\5\17\u0088\n\17\3\20\5\20\u008b\n\20\3\20\3\20\3\20\5\20\u0090"+
    "\n\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20"+
    "\u009e\n\20\3\20\5\20\u00a1\n\20\3\20\5\20\u00a4\n\20\3\20\3\20\3\20\5"+
    "\20\u00a9\n\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
    "\3\20\5\20\u00b7\n\20\3\20\5\20\u00ba\n\20\5\20\u00bc\n\20\3\21\3\21\3"+
    "\21\3\22\5\22\u00c2\n\22\3\22\3\22\3\22\3\22\5\22\u00c8\n\22\3\22\5\22"+
    "\u00cb\n\22\3\22\5\22\u00ce\n\22\3\22\3\22\3\22\5\22\u00d3\n\22\3\22\5"+
    "\22\u00d6\n\22\3\22\3\22\5\22\u00da\n\22\3\22\5\22\u00dd\n\22\3\22\3\22"+
    "\3\22\3\22\5\22\u00e3\n\22\3\22\3\22\3\22\6\22\u00e8\n\22\r\22\16\22\u00e9"+
    "\3\22\3\22\5\22\u00ee\n\22\3\22\3\22\3\22\6\22\u00f3\n\22\r\22\16\22\u00f4"+
    "\3\22\3\22\5\22\u00f9\n\22\3\22\3\22\5\22\u00fd\n\22\3\22\5\22\u0100\n"+
    "\22\5\22\u0102\n\22\3\23\6\23\u0105\n\23\r\23\16\23\u0106\3\24\3\24\5"+
    "\24\u010b\n\24\3\24\3\24\3\25\3\25\3\26\3\26\3\26\7\26\u0114\n\26\f\26"+
    "\16\26\u0117\13\26\3\26\3\26\3\26\5\26\u011c\n\26\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u012d\n\27"+
    "\3\27\3\27\5\27\u0131\n\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u0142\n\27\3\27\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\5\27\u014b\n\27\3\27\6\27\u014e\n\27\r\27\16\27\u014f\3\27"+
    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u016b\n\27\3\27"+
    "\3\27\3\27\5\27\u0170\n\27\3\27\3\27\5\27\u0174\n\27\3\27\5\27\u0177\n"+
    "\27\5\27\u0179\n\27\3\30\6\30\u017c\n\30\r\30\16\30\u017d\3\30\3\30\6"+
    "\30\u0182\n\30\r\30\16\30\u0183\7\30\u0186\n\30\f\30\16\30\u0189\13\30"+
    "\3\31\6\31\u018c\n\31\r\31\16\31\u018d\3\31\3\31\6\31\u0192\n\31\r\31"+
    "\16\31\u0193\7\31\u0196\n\31\f\31\16\31\u0199\13\31\3\32\3\32\3\32\3\32"+
    "\3\32\6\32\u01a0\n\32\r\32\16\32\u01a1\3\32\5\32\u01a5\n\32\3\33\3\33"+
    "\6\33\u01a9\n\33\r\33\16\33\u01aa\3\33\5\33\u01ae\n\33\3\34\3\34\6\34"+
    "\u01b2\n\34\r\34\16\34\u01b3\3\34\5\34\u01b7\n\34\3\35\3\35\6\35\u01bb"+
    "\n\35\r\35\16\35\u01bc\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \7 \u01c7\n"+
    " \f \16 \u01ca\13 \3!\3!\3!\3!\3!\3!\7!\u01d2\n!\f!\16!\u01d5\13!\5!\u01d7"+
    "\n!\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3"+
    "\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\""+
    "\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3"+
    "\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\""+
    "\3\"\3\"\3\"\3\"\3\"\3\"\5\"\u0224\n\"\3#\6#\u0227\n#\r#\16#\u0228\3#"+
    "\5#\u022c\n#\3#\5#\u022f\n#\3$\3$\3$\6$\u0234\n$\r$\16$\u0235\3$\5$\u0239"+
    "\n$\3$\5$\u023c\n$\3%\6%\u023f\n%\r%\16%\u0240\3&\6&\u0244\n&\r&\16&\u0245"+
    "\3\'\6\'\u0249\n\'\r\'\16\'\u024a\3\'\3\'\6\'\u024f\n\'\r\'\16\'\u0250"+
    "\6\'\u0253\n\'\r\'\16\'\u0254\3\'\5\'\u0258\n\'\3\'\6\'\u025b\n\'\r\'"+
    "\16\'\u025c\3\'\6\'\u0260\n\'\r\'\16\'\u0261\5\'\u0264\n\'\3(\3(\3(\3"+
    "(\3(\3(\3(\3(\3(\3(\3(\3(\5(\u0272\n(\3)\3)\3*\6*\u0277\n*\r*\16*\u0278"+
    "\3+\3+\3+\3+\7+\u027f\n+\f+\16+\u0282\13+\3,\6,\u0285\n,\r,\16,\u0286"+
    "\3\u0286\2-\3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\2\25\4\27\5\31\2\33"+
    "\2\35\6\37\7!\b#\t%\2\'\2)\2+\2-\n/\2\61\2\63\13\65\f\67\r9\16;\2=\2?"+
    "\2A\17C\20E\21G\22I\23K\24M\25O\2Q\26S\2U\27W\30\3\2\34\3\2\62;\13\2c"+
    "|\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0121\u0121"+
    "\u0133\u0133\u0161\u0161\13\2C\\\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8"+
    "\u00d8\u00dd\u00de\u0120\u0120\u0132\u0132\u0160\u0160\21\2C\\c|\u00c4"+
    "\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9"+
    "\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160"+
    "\u0161\22\2\62;C\\c|\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8\u00dd"+
    "\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe\u0120"+
    "\u0121\u0132\u0133\u0160\u0161\23\2\62;C\\aac|\u00c4\u00c4\u00c9\u00c9"+
    "\u00d0\u00d0\u00d8\u00d8\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0"+
    "\u00f8\u00f8\u00fd\u00fe\u0120\u0121\u0132\u0133\u0160\u0161\4\2))\u201b"+
    "\u201b\6\2$$\u00ad\u00ad\u00bd\u00bd\u201e\u201f\5\2\13\13\"\"\u00a2\u00a2"+
    "\4\2\f\f\17\17\3\2\62\63\3\2\62\65\3\2\62\67\4\2\60\60<<\3\2\63\63\3\2"+
    "9;\3\2\64\64\3\2\62\62\4\2--//\4\2..\60\60\4\2GGgg\33\2%%\'(--//\61=?"+
    "?AAC]__aac|\u0080\u0080\u00c4\u00c4\u00c9\u00c9\u00d0\u00d0\u00d8\u00d8"+
    "\u00dd\u00de\u00e4\u00e4\u00e9\u00e9\u00f0\u00f0\u00f8\u00f8\u00fd\u00fe"+
    "\u0120\u0121\u0132\u0133\u0160\u0161\6\2\62;C\\aac|\4\2--/\60\5\2KKXX"+
    "ZZ\30\2##&(*\61<B]`bb}\u0080\u00a5\u00a5\u00a7\u00a7\u00a9\u00a9\u00ab"+
    "\u00ab\u00b0\u00b0\u00b2\u00b3\u00d9\u00d9\u00f9\u00f9\u2015\u2016\u201a"+
    "\u201a\u2028\u2028\u20ae\u20ae\u20bc\u20bc\u2122\u2122\u2124\u2124\6\3"+
    "\2\uf1e8\3\uf201\3\4\2\ufe10\2\ufe11\2\uf3fd\3\uf401\3z\2\u231c\2\u231d"+
    "\2\u238a\2\u238a\2\u23eb\2\u23ee\2\u23f2\2\u23f2\2\u23f5\2\u23f5\2\u25ff"+
    "\2\u2600\2\u2602\2\u2602\2\u2607\2\u2607\2\u2609\2\u260f\2\u2611\2\u2612"+
    "\2\u2614\2\u2614\2\u2616\2\u2621\2\u2623\2\u2623\2\u2626\2\u2627\2\u2629"+
    "\2\u262b\2\u262d\2\u262f\2\u2632\2\u2639\2\u263b\2\u2661\2\u2663\2\u2664"+
    "\2\u2666\2\u2667\2\u2669\2\u2669\2\u266b\2\u2693\2\u2695\2\u2695\2\u2697"+
    "\2\u2697\2\u269a\2\u269a\2\u269c\2\u269c\2\u269f\2\u26a1\2\u26a3\2\u26b1"+
    "\2\u26b4\2\u26c9\2\u26cb\2\u26d0\2\u26d2\2\u26d2\2\u26d4\2\u26d4\2\u26d6"+
    "\2\u26ea\2\u26ec\2\u26f1\2\u26f4\2\u26f5\2\u26f7\2\u26f8\2\u26fc\2\u2703"+
    "\2\u2705\2\u2707\2\u270c\2\u270e\2\u2710\2\u2710\2\u2712\2\u2713\2\u2716"+
    "\2\u2716\2\u272a\2\u272a\2\u274e\2\u274e\2\u2750\2\u2750\2\u2755\2\u2757"+
    "\2\u2759\2\u2759\2\u2765\2\u2769\2\u2797\2\u2799\2\u27a3\2\u27a3\2\u27b2"+
    "\2\u27b2\2\u27c1\2\u27c1\2\u2b1d\2\u2b1e\2\u2b52\2\u2b52\2\u2b57\2\u2b57"+
    "\2\uf002\3\uf101\3\uf10f\3\uf111\3\uf131\3\uf131\3\uf16e\3\uf171\3\uf190"+
    "\3\uf190\3\uf193\3\uf19c\3\uf1af\3\uf201\3\uf203\3\uf203\3\uf205\3\uf211"+
    "\3\uf21c\3\uf21c\3\uf231\3\uf231\3\uf234\3\uf238\3\uf23a\3\uf23c\3\uf23e"+
    "\3\uf241\3\uf24b\3\uf322\3\uf324\3\uf325\3\uf32f\3\uf337\3\uf339\3\uf37e"+
    "\3\uf380\3\uf397\3\uf39a\3\uf39a\3\uf39e\3\uf39f\3\uf3a2\3\uf3cc\3\uf3d1"+
    "\3\uf3d5\3\uf3e2\3\uf3f4\3\uf3f6\3\uf3f6\3\uf3f8\3\uf3f8\3\uf3fa\3\uf440"+
    "\3\uf442\3\uf442\3\uf444\3\uf4fe\3\uf500\3\uf54a\3\uf54d\3\uf570\3\uf573"+
    "\3\uf574\3\uf57c\3\uf588\3\uf58a\3\uf58b\3\uf590\3\uf591\3\uf593\3\uf5a6"+
    "\3\uf5a8\3\uf5a9\3\uf5ab\3\uf5b2\3\uf5b5\3\uf5bd\3\uf5bf\3\uf5c3\3\uf5c7"+
    "\3\uf5d2\3\uf5d6\3\uf5dd\3\uf5e1\3\uf5e2\3\uf5e4\3\uf5e4\3\uf5e6\3\uf5e9"+
    "\3\uf5eb\3\uf5f0\3\uf5f2\3\uf5f4\3\uf5f6\3\uf5fb\3\uf5fd\3\uf651\3\uf682"+
    "\3\uf6cc\3\uf6ce\3\uf6ce\3\uf6d2\3\uf6e1\3\uf6e8\3\uf6ea\3\uf6ec\3\uf6f1"+
    "\3\uf6f3\3\uf6f4\3\uf6f6\3\uf701\3\uf776\3\uf781\3\uf7d7\3\uf801\3\uf80e"+
    "\3\uf811\3\uf84a\3\uf851\3\uf85c\3\uf861\3\uf88a\3\uf891\3\uf8b0\3\uf93c"+
    "\3\uf93e\3\uf947\3\uf949\3\uffff\3\u0095\2\13\2\f\2\17\2\17\2\"\2$\2&"+
    "\2\61\2<\2B\2]\2`\2b\2b\2}\2\u0080\2\u00a2\2\u00a2\2\u00a5\2\u00a5\2\u00a7"+
    "\2\u00a7\2\u00a9\2\u00a9\2\u00ab\2\u00ab\2\u00ad\2\u00ad\2\u00b0\2\u00b0"+
    "\2\u00b2\2\u00b3\2\u00bd\2\u00bd\2\u00d9\2\u00d9\2\u00f9\2\u00f9\2\u2015"+
    "\2\u2016\2\u201a\2\u201b\2\u201e\2\u201f\2\u2028\2\u2028\2\u20ae\2\u20ae"+
    "\2\u20bc\2\u20bc\2\u2122\2\u2122\2\u2124\2\u2124\2\u231c\2\u231d\2\u238a"+
    "\2\u238a\2\u23eb\2\u23ee\2\u23f2\2\u23f2\2\u23f5\2\u23f5\2\u25ff\2\u2600"+
    "\2\u2602\2\u2602\2\u2607\2\u2607\2\u2609\2\u260f\2\u2611\2\u2612\2\u2614"+
    "\2\u2614\2\u2616\2\u2621\2\u2623\2\u2623\2\u2626\2\u2627\2\u2629\2\u262b"+
    "\2\u262d\2\u262f\2\u2632\2\u2639\2\u263b\2\u2661\2\u2663\2\u2664\2\u2666"+
    "\2\u2667\2\u2669\2\u2669\2\u266b\2\u2693\2\u2695\2\u2695\2\u2697\2\u2697"+
    "\2\u269a\2\u269a\2\u269c\2\u269c\2\u269f\2\u26a1\2\u26a3\2\u26b1\2\u26b4"+
    "\2\u26c9\2\u26cb\2\u26d0\2\u26d2\2\u26d2\2\u26d4\2\u26d4\2\u26d6\2\u26ea"+
    "\2\u26ec\2\u26f1\2\u26f4\2\u26f5\2\u26f7\2\u26f8\2\u26fc\2\u2703\2\u2705"+
    "\2\u2707\2\u270c\2\u270e\2\u2710\2\u2710\2\u2712\2\u2713\2\u2716\2\u2716"+
    "\2\u272a\2\u272a\2\u274e\2\u274e\2\u2750\2\u2750\2\u2755\2\u2757\2\u2759"+
    "\2\u2759\2\u2765\2\u2769\2\u2797\2\u2799\2\u27a3\2\u27a3\2\u27b2\2\u27b2"+
    "\2\u27c1\2\u27c1\2\u2b1d\2\u2b1e\2\u2b52\2\u2b52\2\u2b57\2\u2b57\2\uf002"+
    "\3\uf101\3\uf10f\3\uf111\3\uf131\3\uf131\3\uf16e\3\uf171\3\uf190\3\uf190"+
    "\3\uf193\3\uf19c\3\uf1af\3\uf201\3\uf203\3\uf203\3\uf205\3\uf211\3\uf21c"+
    "\3\uf21c\3\uf231\3\uf231\3\uf234\3\uf238\3\uf23a\3\uf23c\3\uf23e\3\uf241"+
    "\3\uf24b\3\uf322\3\uf324\3\uf325\3\uf32f\3\uf337\3\uf339\3\uf37e\3\uf380"+
    "\3\uf397\3\uf39a\3\uf39a\3\uf39e\3\uf39f\3\uf3a2\3\uf3cc\3\uf3d1\3\uf3d5"+
    "\3\uf3e2\3\uf3f4\3\uf3f6\3\uf3f6\3\uf3f8\3\uf3f8\3\uf3fa\3\uf440\3\uf442"+
    "\3\uf442\3\uf444\3\uf4fe\3\uf500\3\uf54a\3\uf54d\3\uf570\3\uf573\3\uf574"+
    "\3\uf57c\3\uf588\3\uf58a\3\uf58b\3\uf590\3\uf591\3\uf593\3\uf5a6\3\uf5a8"+
    "\3\uf5a9\3\uf5ab\3\uf5b2\3\uf5b5\3\uf5bd\3\uf5bf\3\uf5c3\3\uf5c7\3\uf5d2"+
    "\3\uf5d6\3\uf5dd\3\uf5e1\3\uf5e2\3\uf5e4\3\uf5e4\3\uf5e6\3\uf5e9\3\uf5eb"+
    "\3\uf5f0\3\uf5f2\3\uf5f4\3\uf5f6\3\uf5fb\3\uf5fd\3\uf651\3\uf682\3\uf6cc"+
    "\3\uf6ce\3\uf6ce\3\uf6d2\3\uf6e1\3\uf6e8\3\uf6ea\3\uf6ec\3\uf6f1\3\uf6f3"+
    "\3\uf6f4\3\uf6f6\3\uf701\3\uf776\3\uf781\3\uf7d7\3\uf801\3\uf80e\3\uf811"+
    "\3\uf84a\3\uf851\3\uf85c\3\uf861\3\uf88a\3\uf891\3\uf8b0\3\uf93c\3\uf93e"+
    "\3\uf947\3\uf949\3\uffff\3\u02ec\2\25\3\2\2\2\2\27\3\2\2\2\2\35\3\2\2"+
    "\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2-\3\2\2\2\2\63\3\2\2\2\2\65\3\2"+
    "\2\2\2\67\3\2\2\2\29\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2"+
    "\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2Q\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\3"+
    "Y\3\2\2\2\5[\3\2\2\2\7]\3\2\2\2\t_\3\2\2\2\13a\3\2\2\2\rc\3\2\2\2\17e"+
    "\3\2\2\2\21g\3\2\2\2\23i\3\2\2\2\25p\3\2\2\2\27t\3\2\2\2\31z\3\2\2\2\33"+
    "|\3\2\2\2\35\177\3\2\2\2\37\u00bb\3\2\2\2!\u00bd\3\2\2\2#\u0101\3\2\2"+
    "\2%\u0104\3\2\2\2\'\u0108\3\2\2\2)\u010e\3\2\2\2+\u011b\3\2\2\2-\u0178"+
    "\3\2\2\2/\u017b\3\2\2\2\61\u018b\3\2\2\2\63\u019a\3\2\2\2\65\u01a6\3\2"+
    "\2\2\67\u01af\3\2\2\29\u01b8\3\2\2\2;\u01c0\3\2\2\2=\u01c2\3\2\2\2?\u01c4"+
    "\3\2\2\2A\u01d6\3\2\2\2C\u0223\3\2\2\2E\u0226\3\2\2\2G\u0233\3\2\2\2I"+
    "\u023e\3\2\2\2K\u0243\3\2\2\2M\u0263\3\2\2\2O\u0271\3\2\2\2Q\u0273\3\2"+
    "\2\2S\u0276\3\2\2\2U\u027a\3\2\2\2W\u0284\3\2\2\2YZ\t\2\2\2Z\4\3\2\2\2"+
    "[\\\t\3\2\2\\\6\3\2\2\2]^\t\4\2\2^\b\3\2\2\2_`\t\5\2\2`\n\3\2\2\2ab\t"+
    "\6\2\2b\f\3\2\2\2cd\t\7\2\2d\16\3\2\2\2ef\t\b\2\2f\20\3\2\2\2gh\t\t\2"+
    "\2h\22\3\2\2\2ik\5\17\b\2jl\5\t\5\2kj\3\2\2\2lm\3\2\2\2mk\3\2\2\2mn\3"+
    "\2\2\2n\24\3\2\2\2oq\t\n\2\2po\3\2\2\2qr\3\2\2\2rp\3\2\2\2rs\3\2\2\2s"+
    "\26\3\2\2\2tu\t\13\2\2u\30\3\2\2\2vw\t\f\2\2w{\t\2\2\2xy\7\64\2\2y{\t"+
    "\r\2\2zv\3\2\2\2zx\3\2\2\2{\32\3\2\2\2|}\t\16\2\2}~\t\2\2\2~\34\3\2\2"+
    "\2\177\u0080\5\31\r\2\u0080\u0081\t\17\2\2\u0081\u0084\5\33\16\2\u0082"+
    "\u0083\t\17\2\2\u0083\u0085\5\33\16\2\u0084\u0082\3\2\2\2\u0084\u0085"+
    "\3\2\2\2\u0085\u0087\3\2\2\2\u0086\u0088\5\23\n\2\u0087\u0086\3\2\2\2"+
    "\u0087\u0088\3\2\2\2\u0088\36\3\2\2\2\u0089\u008b\t\r\2\2\u008a\u0089"+
    "\3\2\2\2\u008a\u008b\3\2\2\2\u008b\u008c\3\2\2\2\u008c\u008d\t\2\2\2\u008d"+
    "\u008f\7\60\2\2\u008e\u0090\t\f\2\2\u008f\u008e\3\2\2\2\u008f\u0090\3"+
    "\2\2\2\u0090\u0091\3\2\2\2\u0091\u0092\t\2\2\2\u0092\u009d\7\60\2\2\u0093"+
    "\u0094\t\20\2\2\u0094\u0095\t\21\2\2\u0095\u0096\t\2\2\2\u0096\u009e\t"+
    "\2\2\2\u0097\u0098\t\22\2\2\u0098\u0099\t\23\2\2\u0099\u009a\t\2\2\2\u009a"+
    "\u009e\t\2\2\2\u009b\u009c\t\2\2\2\u009c\u009e\t\2\2\2\u009d\u0093\3\2"+
    "\2\2\u009d\u0097\3\2\2\2\u009d\u009b\3\2\2\2\u009e\u00a0\3\2\2\2\u009f"+
    "\u00a1\5\23\n\2\u00a0\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00bc\3"+
    "\2\2\2\u00a2\u00a4\t\r\2\2\u00a3\u00a2\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4"+
    "\u00a5\3\2\2\2\u00a5\u00a6\t\2\2\2\u00a6\u00a8\7\61\2\2\u00a7\u00a9\t"+
    "\f\2\2\u00a8\u00a7\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa"+
    "\u00ab\t\2\2\2\u00ab\u00b6\7\61\2\2\u00ac\u00ad\t\20\2\2\u00ad\u00ae\t"+
    "\21\2\2\u00ae\u00af\t\2\2\2\u00af\u00b7\t\2\2\2\u00b0\u00b1\t\22\2\2\u00b1"+
    "\u00b2\t\23\2\2\u00b2\u00b3\t\2\2\2\u00b3\u00b7\t\2\2\2\u00b4\u00b5\t"+
    "\2\2\2\u00b5\u00b7\t\2\2\2\u00b6\u00ac\3\2\2\2\u00b6\u00b0\3\2\2\2\u00b6"+
    "\u00b4\3\2\2\2\u00b7\u00b9\3\2\2\2\u00b8\u00ba\5\23\n\2\u00b9\u00b8\3"+
    "\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00bc\3\2\2\2\u00bb\u008a\3\2\2\2\u00bb"+
    "\u00a3\3\2\2\2\u00bc \3\2\2\2\u00bd\u00be\7\'\2\2\u00be\u00bf\5#\22\2"+
    "\u00bf\"\3\2\2\2\u00c0\u00c2\t\24\2\2\u00c1\u00c0\3\2\2\2\u00c1\u00c2"+
    "\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c4\5%\23\2\u00c4\u00c5\t\25\2\2"+
    "\u00c5\u00c7\5%\23\2\u00c6\u00c8\5\'\24\2\u00c7\u00c6\3\2\2\2\u00c7\u00c8"+
    "\3\2\2\2\u00c8\u00ca\3\2\2\2\u00c9\u00cb\5\23\n\2\u00ca\u00c9\3\2\2\2"+
    "\u00ca\u00cb\3\2\2\2\u00cb\u0102\3\2\2\2\u00cc\u00ce\t\24\2\2\u00cd\u00cc"+
    "\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00d0\5%\23\2\u00d0"+
    "\u00d2\5\'\24\2\u00d1\u00d3\5\23\n\2\u00d2\u00d1\3\2\2\2\u00d2\u00d3\3"+
    "\2\2\2\u00d3\u0102\3\2\2\2\u00d4\u00d6\t\24\2\2\u00d5\u00d4\3\2\2\2\u00d5"+
    "\u00d6\3\2\2\2\u00d6\u00d7\3\2\2\2\u00d7\u00d9\5%\23\2\u00d8\u00da\5\23"+
    "\n\2\u00d9\u00d8\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u0102\3\2\2\2\u00db"+
    "\u00dd\t\24\2\2\u00dc\u00db\3\2\2\2\u00dc\u00dd\3\2\2\2\u00dd\u00de\3"+
    "\2\2\2\u00de\u00df\5%\23\2\u00df\u00e0\7\61\2\2\u00e0\u00e2\5%\23\2\u00e1"+
    "\u00e3\5\23\n\2\u00e2\u00e1\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u0102\3"+
    "\2\2\2\u00e4\u00e5\5%\23\2\u00e5\u00e6\7\60\2\2\u00e6\u00e8\3\2\2\2\u00e7"+
    "\u00e4\3\2\2\2\u00e8\u00e9\3\2\2\2\u00e9\u00e7\3\2\2\2\u00e9\u00ea\3\2"+
    "\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ed\5%\23\2\u00ec\u00ee\5\23\n\2\u00ed"+
    "\u00ec\3\2\2\2\u00ed\u00ee\3\2\2\2\u00ee\u0102\3\2\2\2\u00ef\u00f0\5%"+
    "\23\2\u00f0\u00f1\7.\2\2\u00f1\u00f3\3\2\2\2\u00f2\u00ef\3\2\2\2\u00f3"+
    "\u00f4\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f6\3\2"+
    "\2\2\u00f6\u00f8\5%\23\2\u00f7\u00f9\5\23\n\2\u00f8\u00f7\3\2\2\2\u00f8"+
    "\u00f9\3\2\2\2\u00f9\u0102\3\2\2\2\u00fa\u00fc\5%\23\2\u00fb\u00fd\7\60"+
    "\2\2\u00fc\u00fb\3\2\2\2\u00fc\u00fd\3\2\2\2\u00fd\u00ff\3\2\2\2\u00fe"+
    "\u0100\5\23\n\2\u00ff\u00fe\3\2\2\2\u00ff\u0100\3\2\2\2\u0100\u0102\3"+
    "\2\2\2\u0101\u00c1\3\2\2\2\u0101\u00cd\3\2\2\2\u0101\u00d5\3\2\2\2\u0101"+
    "\u00dc\3\2\2\2\u0101\u00e7\3\2\2\2\u0101\u00f2\3\2\2\2\u0101\u00fa\3\2"+
    "\2\2\u0102$\3\2\2\2\u0103\u0105\5\3\2\2\u0104\u0103\3\2\2\2\u0105\u0106"+
    "\3\2\2\2\u0106\u0104\3\2\2\2\u0106\u0107\3\2\2\2\u0107&\3\2\2\2\u0108"+
    "\u010a\t\26\2\2\u0109\u010b\t\24\2\2\u010a\u0109\3\2\2\2\u010a\u010b\3"+
    "\2\2\2\u010b\u010c\3\2\2\2\u010c\u010d\5%\23\2\u010d(\3\2\2\2\u010e\u010f"+
    "\t\27\2\2\u010f*\3\2\2\2\u0110\u0115\5)\25\2\u0111\u0114\5)\25\2\u0112"+
    "\u0114\7\60\2\2\u0113\u0111\3\2\2\2\u0113\u0112\3\2\2\2\u0114\u0117\3"+
    "\2\2\2\u0115\u0113\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0118\3\2\2\2\u0117"+
    "\u0115\3\2\2\2\u0118\u0119\5)\25\2\u0119\u011c\3\2\2\2\u011a\u011c\5)"+
    "\25\2\u011b\u0110\3\2\2\2\u011b\u011a\3\2\2\2\u011c,\3\2\2\2\u011d\u011e"+
    "\7j\2\2\u011e\u011f\7v\2\2\u011f\u0120\7v\2\2\u0120\u0121\7r\2\2\u0121"+
    "\u0122\7<\2\2\u0122\u0123\7\61\2\2\u0123\u012d\7\61\2\2\u0124\u0125\7"+
    "j\2\2\u0125\u0126\7v\2\2\u0126\u0127\7v\2\2\u0127\u0128\7r\2\2\u0128\u0129"+
    "\7u\2\2\u0129\u012a\7<\2\2\u012a\u012b\7\61\2\2\u012b\u012d\7\61\2\2\u012c"+
    "\u011d\3\2\2\2\u012c\u0124\3\2\2\2\u012d\u012e\3\2\2\2\u012e\u0130\5+"+
    "\26\2\u012f\u0131\5\23\n\2\u0130\u012f\3\2\2\2\u0130\u0131\3\2\2\2\u0131"+
    "\u0179\3\2\2\2\u0132\u0133\7j\2\2\u0133\u0134\7v\2\2\u0134\u0135\7v\2"+
    "\2\u0135\u0136\7r\2\2\u0136\u0137\7<\2\2\u0137\u0138\7\61\2\2\u0138\u0142"+
    "\7\61\2\2\u0139\u013a\7j\2\2\u013a\u013b\7v\2\2\u013b\u013c\7v\2\2\u013c"+
    "\u013d\7r\2\2\u013d\u013e\7u\2\2\u013e\u013f\7<\2\2\u013f\u0140\7\61\2"+
    "\2\u0140\u0142\7\61\2\2\u0141\u0132\3\2\2\2\u0141\u0139\3\2\2\2\u0141"+
    "\u0142\3\2\2\2\u0142\u0143\3\2\2\2\u0143\u0144\7y\2\2\u0144\u0145\7y\2"+
    "\2\u0145\u0146\7y\2\2\u0146\u0147\7\60\2\2\u0147\u0148\3\2\2\2\u0148\u014a"+
    "\5+\26\2\u0149\u014b\5\23\n\2\u014a\u0149\3\2\2\2\u014a\u014b\3\2\2\2"+
    "\u014b\u0179\3\2\2\2\u014c\u014e\t\30\2\2\u014d\u014c\3\2\2\2\u014e\u014f"+
    "\3\2\2\2\u014f\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u016a\3\2\2\2\u0151"+
    "\u0152\7\60\2\2\u0152\u0153\7e\2\2\u0153\u0154\7q\2\2\u0154\u016b\7o\2"+
    "\2\u0155\u0156\7\60\2\2\u0156\u0157\7q\2\2\u0157\u0158\7t\2\2\u0158\u016b"+
    "\7i\2\2\u0159\u015a\7\60\2\2\u015a\u015b\7g\2\2\u015b\u015c\7f\2\2\u015c"+
    "\u016b\7w\2\2\u015d\u015e\7\60\2\2\u015e\u015f\7i\2\2\u015f\u0160\7q\2"+
    "\2\u0160\u016b\7x\2\2\u0161\u0162\7\60\2\2\u0162\u0163\7p\2\2\u0163\u0164"+
    "\7g\2\2\u0164\u016b\7v\2\2\u0165\u0166\7\60\2\2\u0166\u0167\7k\2\2\u0167"+
    "\u0168\7p\2\2\u0168\u0169\7h\2\2\u0169\u016b\7q\2\2\u016a\u0151\3\2\2"+
    "\2\u016a\u0155\3\2\2\2\u016a\u0159\3\2\2\2\u016a\u015d\3\2\2\2\u016a\u0161"+
    "\3\2\2\2\u016a\u0165\3\2\2\2\u016b\u016f\3\2\2\2\u016c\u016d\7\60\2\2"+
    "\u016d\u016e\7v\2\2\u016e\u0170\7t\2\2\u016f\u016c\3\2\2\2\u016f\u0170"+
    "\3\2\2\2\u0170\u0173\3\2\2\2\u0171\u0172\7\61\2\2\u0172\u0174\5+\26\2"+
    "\u0173\u0171\3\2\2\2\u0173\u0174\3\2\2\2\u0174\u0176\3\2\2\2\u0175\u0177"+
    "\5\23\n\2\u0176\u0175\3\2\2\2\u0176\u0177\3\2\2\2\u0177\u0179\3\2\2\2"+
    "\u0178\u012c\3\2\2\2\u0178\u0141\3\2\2\2\u0178\u014d\3\2\2\2\u0179.\3"+
    "\2\2\2\u017a\u017c\5\r\7\2\u017b\u017a\3\2\2\2\u017c\u017d\3\2\2\2\u017d"+
    "\u017b\3\2\2\2\u017d\u017e\3\2\2\2\u017e\u0187\3\2\2\2\u017f\u0181\t\31"+
    "\2\2\u0180\u0182\5\r\7\2\u0181\u0180\3\2\2\2\u0182\u0183\3\2\2\2\u0183"+
    "\u0181\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0186\3\2\2\2\u0185\u017f\3\2"+
    "\2\2\u0186\u0189\3\2\2\2\u0187\u0185\3\2\2\2\u0187\u0188\3\2\2\2\u0188"+
    "\60\3\2\2\2\u0189\u0187\3\2\2\2\u018a\u018c\5\r\7\2\u018b\u018a\3\2\2"+
    "\2\u018c\u018d\3\2\2\2\u018d\u018b\3\2\2\2\u018d\u018e\3\2\2\2\u018e\u0197"+
    "\3\2\2\2\u018f\u0191\7/\2\2\u0190\u0192\5\r\7\2\u0191\u0190\3\2\2\2\u0192"+
    "\u0193\3\2\2\2\u0193\u0191\3\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2"+
    "\2\2\u0195\u018f\3\2\2\2\u0196\u0199\3\2\2\2\u0197\u0195\3\2\2\2\u0197"+
    "\u0198\3\2\2\2\u0198\62\3\2\2\2\u0199\u0197\3\2\2\2\u019a\u019b\5/\30"+
    "\2\u019b\u019c\7B\2\2\u019c\u019f\5\61\31\2\u019d\u019e\7\60\2\2\u019e"+
    "\u01a0\5\61\31\2\u019f\u019d\3\2\2\2\u01a0\u01a1\3\2\2\2\u01a1\u019f\3"+
    "\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\u01a4\3\2\2\2\u01a3\u01a5\5\23\n\2\u01a4"+
    "\u01a3\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5\64\3\2\2\2\u01a6\u01a8\7%\2\2"+
    "\u01a7\u01a9\5\r\7\2\u01a8\u01a7\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01a8"+
    "\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01ad\3\2\2\2\u01ac\u01ae\5\23\n\2"+
    "\u01ad\u01ac\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\66\3\2\2\2\u01af\u01b1"+
    "\7B\2\2\u01b0\u01b2\5\r\7\2\u01b1\u01b0\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3"+
    "\u01b1\3\2\2\2\u01b3\u01b4\3\2\2\2\u01b4\u01b6\3\2\2\2\u01b5\u01b7\5\23"+
    "\n\2\u01b6\u01b5\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b78\3\2\2\2\u01b8\u01ba"+
    "\7>\2\2\u01b9\u01bb\5\r\7\2\u01ba\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc"+
    "\u01ba\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01bf\7@"+
    "\2\2\u01bf:\3\2\2\2\u01c0\u01c1\t\34\2\2\u01c1<\3\2\2\2\u01c2\u01c3\t"+
    "\35\2\2\u01c3>\3\2\2\2\u01c4\u01c8\t\36\2\2\u01c5\u01c7\5=\37\2\u01c6"+
    "\u01c5\3\2\2\2\u01c7\u01ca\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c8\u01c9\3\2"+
    "\2\2\u01c9@\3\2\2\2\u01ca\u01c8\3\2\2\2\u01cb\u01cc\5;\36\2\u01cc\u01cd"+
    "\5;\36\2\u01cd\u01d7\3\2\2\2\u01ce\u01d3\5? \2\u01cf\u01d0\7\u200f\2\2"+
    "\u01d0\u01d2\5? \2\u01d1\u01cf\3\2\2\2\u01d2\u01d5\3\2\2\2\u01d3\u01d1"+
    "\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d7\3\2\2\2\u01d5\u01d3\3\2\2\2\u01d6"+
    "\u01cb\3\2\2\2\u01d6\u01ce\3\2\2\2\u01d7B\3\2\2\2\u01d8\u01d9\7<\2\2\u01d9"+
    "\u0224\7+\2\2\u01da\u01db\7<\2\2\u01db\u01dc\7/\2\2\u01dc\u0224\7+\2\2"+
    "\u01dd\u01de\7<\2\2\u01de\u01df\7/\2\2\u01df\u0224\7_\2\2\u01e0\u01e1"+
    "\7<\2\2\u01e1\u0224\7F\2\2\u01e2\u01e3\7<\2\2\u01e3\u01e4\7/\2\2\u01e4"+
    "\u0224\7F\2\2\u01e5\u01e6\7:\2\2\u01e6\u01e7\7/\2\2\u01e7\u0224\7+\2\2"+
    "\u01e8\u01e9\7=\2\2\u01e9\u0224\7+\2\2\u01ea\u01eb\7=\2\2\u01eb\u01ec"+
    "\7\u2013\2\2\u01ec\u0224\7+\2\2\u01ed\u01ee\7<\2\2\u01ee\u0224\7*\2\2"+
    "\u01ef\u01f0\7<\2\2\u01f0\u01f1\7/\2\2\u01f1\u0224\7*\2\2\u01f2\u01f3"+
    "\7<\2\2\u01f3\u01f4\7)\2\2\u01f4\u0224\7*\2\2\u01f5\u01f6\7<\2\2\u01f6"+
    "\u01f7\7)\2\2\u01f7\u0224\7+\2\2\u01f8\u01f9\7<\2\2\u01f9\u0224\7R\2\2"+
    "\u01fa\u01fb\7<\2\2\u01fb\u0224\7r\2\2\u01fc\u01fd\7<\2\2\u01fd\u0224"+
    "\7~\2\2\u01fe\u01ff\7?\2\2\u01ff\u0224\7~\2\2\u0200\u0201\7?\2\2\u0201"+
    "\u0224\7+\2\2\u0202\u0203\7?\2\2\u0203\u0224\7*\2\2\u0204\u0205\7<\2\2"+
    "\u0205\u0206\7\u2013\2\2\u0206\u0224\7\61\2\2\u0207\u0208\7<\2\2\u0208"+
    "\u0224\7\61\2\2\u0209\u020a\7<\2\2\u020a\u020b\7`\2\2\u020b\u0224\7+\2"+
    "\2\u020c\u020d\7\u00b1\2\2\u020d\u020e\7^\2\2\u020e\u020f\7a\2\2\u020f"+
    "\u0210\7*\2\2\u0210\u0211\7\u30c6\2\2\u0211\u0212\7+\2\2\u0212\u0213\7"+
    "a\2\2\u0213\u0214\7\61\2\2\u0214\u0224\7\u00b1\2\2\u0215\u0216\7Q\2\2"+
    "\u0216\u0217\7a\2\2\u0217\u0224\7q\2\2\u0218\u0219\7q\2\2\u0219\u021a"+
    "\7a\2\2\u021a\u0224\7Q\2\2\u021b\u021c\7Q\2\2\u021c\u021d\7a\2\2\u021d"+
    "\u0224\7Q\2\2\u021e\u021f\7^\2\2\u021f\u0220\7q\2\2\u0220\u0224\7\61\2"+
    "\2\u0221\u0222\7>\2\2\u0222\u0224\7\65\2\2\u0223\u01d8\3\2\2\2\u0223\u01da"+
    "\3\2\2\2\u0223\u01dd\3\2\2\2\u0223\u01e0\3\2\2\2\u0223\u01e2\3\2\2\2\u0223"+
    "\u01e5\3\2\2\2\u0223\u01e8\3\2\2\2\u0223\u01ea\3\2\2\2\u0223\u01ed\3\2"+
    "\2\2\u0223\u01ef\3\2\2\2\u0223\u01f2\3\2\2\2\u0223\u01f5\3\2\2\2\u0223"+
    "\u01f8\3\2\2\2\u0223\u01fa\3\2\2\2\u0223\u01fc\3\2\2\2\u0223\u01fe\3\2"+
    "\2\2\u0223\u0200\3\2\2\2\u0223\u0202\3\2\2\2\u0223\u0204\3\2\2\2\u0223"+
    "\u0207\3\2\2\2\u0223\u0209\3\2\2\2\u0223\u020c\3\2\2\2\u0223\u0215\3\2"+
    "\2\2\u0223\u0218\3\2\2\2\u0223\u021b\3\2\2\2\u0223\u021e\3\2\2\2\u0223"+
    "\u0221\3\2\2\2\u0224D\3\2\2\2\u0225\u0227\t\32\2\2\u0226\u0225\3\2\2\2"+
    "\u0227\u0228\3\2\2\2\u0228\u0226\3\2\2\2\u0228\u0229\3\2\2\2\u0229\u022b"+
    "\3\2\2\2\u022a\u022c\7\60\2\2\u022b\u022a\3\2\2\2\u022b\u022c\3\2\2\2"+
    "\u022c\u022e\3\2\2\2\u022d\u022f\5\23\n\2\u022e\u022d\3\2\2\2\u022e\u022f"+
    "\3\2\2\2\u022fF\3\2\2\2\u0230\u0231\5\7\4\2\u0231\u0232\7\60\2\2\u0232"+
    "\u0234\3\2\2\2\u0233\u0230\3\2\2\2\u0234\u0235\3\2\2\2\u0235\u0233\3\2"+
    "\2\2\u0235\u0236\3\2\2\2\u0236\u0238\3\2\2\2\u0237\u0239\5\7\4\2\u0238"+
    "\u0237\3\2\2\2\u0238\u0239\3\2\2\2\u0239\u023b\3\2\2\2\u023a\u023c\5\23"+
    "\n\2\u023b\u023a\3\2\2\2\u023b\u023c\3\2\2\2\u023cH\3\2\2\2\u023d\u023f"+
    "\5\t\5\2\u023e\u023d\3\2\2\2\u023f\u0240\3\2\2\2\u0240\u023e\3\2\2\2\u0240"+
    "\u0241\3\2\2\2\u0241J\3\2\2\2\u0242\u0244\5\13\6\2\u0243\u0242\3\2\2\2"+
    "\u0244\u0245\3\2\2\2\u0245\u0243\3\2\2\2\u0245\u0246\3\2\2\2\u0246L\3"+
    "\2\2\2\u0247\u0249\5\13\6\2\u0248\u0247\3\2\2\2\u0249\u024a\3\2\2\2\u024a"+
    "\u0248\3\2\2\2\u024a\u024b\3\2\2\2\u024b\u0252\3\2\2\2\u024c\u024e\7/"+
    "\2\2\u024d\u024f\5\13\6\2\u024e\u024d\3\2\2\2\u024f\u0250\3\2\2\2\u0250"+
    "\u024e\3\2\2\2\u0250\u0251\3\2\2\2\u0251\u0253\3\2\2\2\u0252\u024c\3\2"+
    "\2\2\u0253\u0254\3\2\2\2\u0254\u0252\3\2\2\2\u0254\u0255\3\2\2\2\u0255"+
    "\u0257\3\2\2\2\u0256\u0258\5\23\n\2\u0257\u0256\3\2\2\2\u0257\u0258\3"+
    "\2\2\2\u0258\u0264\3\2\2\2\u0259\u025b\5\13\6\2\u025a\u0259\3\2\2\2\u025b"+
    "\u025c\3\2\2\2\u025c\u025a\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025f\3\2"+
    "\2\2\u025e\u0260\5\23\n\2\u025f\u025e\3\2\2\2\u0260\u0261\3\2\2\2\u0261"+
    "\u025f\3\2\2\2\u0261\u0262\3\2\2\2\u0262\u0264\3\2\2\2\u0263\u0248\3\2"+
    "\2\2\u0263\u025a\3\2\2\2\u0264N\3\2\2\2\u0265\u0272\5\17\b\2\u0266\u0272"+
    "\5\21\t\2\u0267\u0268\7\60\2\2\u0268\u0269\7\60\2\2\u0269\u0272\7\60\2"+
    "\2\u026a\u026b\7*\2\2\u026b\u026c\7#\2\2\u026c\u0272\7+\2\2\u026d\u026e"+
    "\7*\2\2\u026e\u026f\7A\2\2\u026f\u0272\7+\2\2\u0270\u0272\t\33\2\2\u0271"+
    "\u0265\3\2\2\2\u0271\u0266\3\2\2\2\u0271\u0267\3\2\2\2\u0271\u026a\3\2"+
    "\2\2\u0271\u026d\3\2\2\2\u0271\u0270\3\2\2\2\u0272P\3\2\2\2\u0273\u0274"+
    "\5O(\2\u0274R\3\2\2\2\u0275\u0277\n\37\2\2\u0276\u0275\3\2\2\2\u0277\u0278"+
    "\3\2\2\2\u0278\u0276\3\2\2\2\u0278\u0279\3\2\2\2\u0279T\3\2\2\2\u027a"+
    "\u0280\5S*\2\u027b\u027c\5\17\b\2\u027c\u027d\5S*\2\u027d\u027f\3\2\2"+
    "\2\u027e\u027b\3\2\2\2\u027f\u0282\3\2\2\2\u0280\u027e\3\2\2\2\u0280\u0281"+
    "\3\2\2\2\u0281V\3\2\2\2\u0282\u0280\3\2\2\2\u0283\u0285\13\2\2\2\u0284"+
    "\u0283\3\2\2\2\u0285\u0286\3\2\2\2\u0286\u0287\3\2\2\2\u0286\u0284\3\2"+
    "\2\2\u0287X\3\2\2\2T\2mrz\u0084\u0087\u008a\u008f\u009d\u00a0\u00a3\u00a8"+
    "\u00b6\u00b9\u00bb\u00c1\u00c7\u00ca\u00cd\u00d2\u00d5\u00d9\u00dc\u00e2"+
    "\u00e9\u00ed\u00f4\u00f8\u00fc\u00ff\u0101\u0106\u010a\u0113\u0115\u011b"+
    "\u012c\u0130\u0141\u014a\u014f\u016a\u016f\u0173\u0176\u0178\u017d\u0183"+
    "\u0187\u018d\u0193\u0197\u01a1\u01a4\u01aa\u01ad\u01b3\u01b6\u01bc\u01c8"+
    "\u01d3\u01d6\u0223\u0228\u022b\u022e\u0235\u0238\u023b\u0240\u0245\u024a"+
    "\u0250\u0254\u0257\u025c\u0261\u0263\u0271\u0278\u0280\u0286\2";
  public static final ATN _ATN =
    new ATNDeserializer().deserialize(_serializedATN.toCharArray());
  static {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
      _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
    }
  }
}
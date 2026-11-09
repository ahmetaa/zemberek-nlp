/*
 * A simple lexer grammar for Turkish texts.
 */
lexer grammar TurkishLexer;

@header {
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
}

@members {
private static final Locale LOCALE_TR = new Locale("tr");
private static final Set<String> abbreviations = loadAbbreviations();
private Token deferredToken;

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
}

tokens {Abbreviation}

options {
  language = Java;
}

fragment Digit: [0-9];

// Letters
fragment TurkishLetters
    : [a-zçğıöşüâîû];

fragment TurkishLettersCapital
    : [A-ZÇĞİÖŞÜÂÎÛ];

fragment TurkishLettersAll
    : [a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ];

fragment AllTurkishAlphanumerical
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ];

fragment AllTurkishAlphanumericalUnderscore
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ_];

fragment Apostrophe: ('\''|'’');

fragment DoubleQuote: ('"'|'”'|'“'|'»'|'«');

// 'lerin
fragment AposAndSuffix: Apostrophe TurkishLettersAll+;

SpaceTab
    : [ \t\u00a0]+;
NewLine
    : [\n\r];

fragment Hour: [01][0-9] | '2'[0-3];
fragment MinuteSecond: [0-5][0-9];

Time
    : Hour (':'|'.') MinuteSecond ((':'|'.') MinuteSecond)? AposAndSuffix? ;

Date
    : ([0-3]?[0-9] '.' [0-1]?[0-9] '.' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?)
    | ([0-3]?[0-9] '/' [0-1]?[0-9] '/' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?)
    ;

PercentNumeral
    : '%' Number;

Number
    : [+\-]? Integer [.,] Integer Exp? AposAndSuffix? // -1.35, 1.35E-9, 3,1'e
    | [+\-]? Integer Exp AposAndSuffix?               // 1e10 -3e4 1e10'dur
    | [+\-]? Integer AposAndSuffix?                   // -3, 45
    | [+\-]? Integer '/' Integer AposAndSuffix?       // -1/2
    | (Integer '.')+ Integer AposAndSuffix?           // 1.000.000
    | (Integer ',')+ Integer AposAndSuffix?           // 2,345,531
    | Integer '.'? AposAndSuffix?                     // Ordinal 2. 34.
    ;

// Not really an integer as it can have zeroes at the start, but this is ok.
fragment Integer
    : Digit+ ;

fragment Exp
    : [Ee] [+\-]? Integer ;

fragment URLChar
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ\-_/?&+;=[\]%#~:];

fragment URLFragment
    : URLChar (URLChar | '.')* URLChar
    | URLChar
    ;

URL
    : ('http://'|'https://') URLFragment AposAndSuffix?
    | ('http://'|'https://')? 'www.' URLFragment AposAndSuffix?
    | [0-9a-zA-Z_]+ ('.com'| '.org' | '.edu' | '.gov'|'.net'|'.info') ('.tr')? ('/' URLFragment)? AposAndSuffix?
    ;

fragment EmailLocalPart
    : AllTurkishAlphanumericalUnderscore+ ([.\-+] AllTurkishAlphanumericalUnderscore+)*;

fragment EmailDomainPart
    : AllTurkishAlphanumericalUnderscore+ ('-' AllTurkishAlphanumericalUnderscore+)*;

Email
    : EmailLocalPart '@' EmailDomainPart ('.' EmailDomainPart)+ AposAndSuffix?;

HashTag: '#' AllTurkishAlphanumericalUnderscore+ AposAndSuffix?;

Mention: '@' AllTurkishAlphanumericalUnderscore+ AposAndSuffix?;

MetaTag: '<' AllTurkishAlphanumericalUnderscore+ '>';

// Only a subset.
// TODO: Add more, also consider Emoji tokens.
Emoticon
    : ':)'|':-)'|':-]'|':D'|':-D'|'8-)'|';)'|';‑)'|':('|':-('|':\'('|':\')'
    |':P'|':p'|':|'|'=|'|'=)'|'=('
    |':‑/'|':/'|':^)'|'¯\\_(ツ)_/¯'|'O_o'|'o_O'|'O_O'|'\\o/'|'<3';

// Possible Roman numbers:
RomanNumeral
    : [IVX]+ '.'? AposAndSuffix? ;

// I.B.M.
AbbreviationWithDots
    : (TurkishLettersCapital '.')+ TurkishLettersCapital? AposAndSuffix?;

// Merhaba kedi
Word
    : TurkishLettersAll+;

// f16
WordAlphanumerical
    : AllTurkishAlphanumerical+;

WordWithSymbol
    : AllTurkishAlphanumerical+ ('-' AllTurkishAlphanumerical+)+ AposAndSuffix?
    | AllTurkishAlphanumerical+ AposAndSuffix+
    ;

fragment PunctuationFragment
    : Apostrophe | DoubleQuote | '...' | '(!)' | '(?)'
    | [<>‘…=.,!?%$&*+@:;®™©℠|~`°±×÷§–—₺€£¥]
    | '\\' | '-' | '/' | '(' | ')' | '[' | ']' | '{' | '}' | '^'
    ;

Punctuation
    : PunctuationFragment;

fragment UnknownWordPart
    : ~([ \t\u00a0\n\r<>‘…=.,!?%$&*+@:;®™©℠|~`°±×÷§–—₺€£¥\\\-()/[\]{}^]
    | '\'' | '’' | '"' | '”' | '“' | '»' | '«')+
    ;

UnknownWord
    : UnknownWordPart (Apostrophe UnknownWordPart)*;

// Catch all remaining as Unknown.
Unknown : .+? ;

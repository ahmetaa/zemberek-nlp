/*
 * A simple lexer grammar for Turkish texts.
 * 
 */
lexer grammar TurkishLexer;

@header {
package zemberek3.shared.tokenizer.zemberek.antlr;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Locale;
}

@members {
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

}

tokens {Abbreviation}

options {
  language = Java;
}

fragment Digit: [0-9];

// Letters
fragment TurkishLetters
    : [a-z\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00e2\u00ee\u00fb];

fragment TurkishLettersCapital
    : [A-Z\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00c2\u00ce\u00db];

fragment AllTurkishAlphanumerical
    : [0-9a-zA-Z\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00e2\u00ee\u00fb\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00c2\u00ce\u00db\-];

// 'lerin
fragment AposAndSuffix: '\'' TurkishLetters+;

// 'LERIN
fragment AposAndSuffixCapital: '\'' TurkishLettersCapital+;

SpaceTab
    : [ \t]+;
NewLine
    : [\n\r]+;

PercentNumeral
    : '%' Number;

Number
    : [+\-]? Integer [.\,] Integer Exp? AposAndSuffix? // -1.35, 1.35E-9, 3,1'e
    | [+\-]? Integer Exp AposAndSuffix?     // 1e10 -3e4 1e10'dur
    | [+\-]? Integer AposAndSuffix?         // -3, 45
    | (Integer '.')+ Integer AposAndSuffix? // 1.000.000 
    | (Integer ',')+ Integer AposAndSuffix? // 2,345,531
    | Integer '.'? AposAndSuffix?           // Ordinal 2. 34.      
    ;

// Not really an integer as it can have zeroes at the start but this is ok.
fragment Integer
    : Digit+ ;

fragment Exp
    : [Ee] [+\-]? Integer ;

// Roman numbers:
RomanNumeral
    : ('I'|'II'|'III'|'IV'|'V'|'VI'|'VII'|'VIII'|'IX') '.'?;

// Merhaba kedi
TurkishWord 
    : TurkishLettersCapital? TurkishLetters+;

// Ahmet'in
TurkishWordWithApos
    : TurkishLettersCapital TurkishLetters+ AposAndSuffix;

// Abbreviations and All Caps words, could be an abbreviation or header.
// Should be checked with dictionary.
// NATO TBMM'NE IDO'nun etc.
AllCapsWord
    : TurkishLettersCapital+ (AposAndSuffixCapital? | AposAndSuffix?);

// I.B.M.
AbbreviationWithDots 
    : (TurkishLettersCapital '.')+ TurkishLettersCapital? (AposAndSuffixCapital? | AposAndSuffix?);

// Need to match anything with a dot and post process it to check if 
// it is actually an abbreviation: prof. vs.
// WordWithDot : TurkishLetters+ '.';

// Alpha numerical words F16 H1N1 etc.
Alphanumerical
    : (AllTurkishAlphanumerical)+ (AposAndSuffixCapital? | AposAndSuffix?);

Punctuation 
    :  '...' | '(!)' | '(?)'| [.,!?%$&*+@\\:;\-\"\'\(\)\[\]\{\}];

// Catch all remaining as Unknown.
Unknown : .+? ;

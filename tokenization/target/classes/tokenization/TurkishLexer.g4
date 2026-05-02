/*
 * A simple lexer grammar for Turkish texts.
 * 
 */
lexer grammar TurkishLexer;

@header {
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
    : [ \t]+;
NewLine
    : [\n\r];

Time
    : [0-2][0-9] (':'|'.') [0-5][0-9] ((':'|'.') [0-5][0-9])? AposAndSuffix? ;

Date
    :([0-3]?[0-9] '.' [0-1]?[0-9] '.' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?)|
    ([0-3]?[0-9] '/' [0-1]?[0-9] '/' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?);

PercentNumeral
    : '%' Number;

Number
    : [+\-]? Integer [.,] Integer Exp? AposAndSuffix? // -1.35, 1.35E-9, 3,1'e
    | [+\-]? Integer Exp AposAndSuffix?     // 1e10 -3e4 1e10'dur
    | [+\-]? Integer AposAndSuffix?         // -3, 45
    | [+\-]? Integer '/' Integer AposAndSuffix?  // -1/2
    | (Integer '.')+ Integer AposAndSuffix? // 1.000.000
    | (Integer ',')+ Integer AposAndSuffix? // 2,345,531
    | Integer '.'? AposAndSuffix?           // Ordinal 2. 34.      
    ;

// Not really an integer as it can have zeroes at the start but this is ok.
fragment Integer
    : Digit+ ;

fragment Exp
    : [Ee] [+\-]? Integer ;

fragment URLFragment
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ\-_/?&+;=[\].]+;

URL :
    ('http://'|'https://') URLFragment AposAndSuffix? |
    ('http://'|'https://')? 'www.' URLFragment AposAndSuffix?|
    [0-9a-zA-Z_]+ ('.com'| '.org' | '.edu' | '.gov'|'.net'|'.info') ('.tr')? ('/'URLFragment)? AposAndSuffix?;

Email
    :AllTurkishAlphanumericalUnderscore+ '.'? AllTurkishAlphanumericalUnderscore+ '@'
    (AllTurkishAlphanumericalUnderscore+ '.' AllTurkishAlphanumericalUnderscore+)+ AposAndSuffix?;

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
    : [ILVCDMX]+ '.'? AposAndSuffix? ;

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
    : AllTurkishAlphanumerical+ '-'? AllTurkishAlphanumerical+ AposAndSuffix?;

fragment PunctuationFragment
    : Apostrophe | DoubleQuote | '...' | '(!)' | '(?)'| [>‘…=.,!?%$&*+@:;®™©℠]
          | '\\' | '-' | '/' | '(' | ')' | '[' | ']' | '{' | '}' | '^' ;

Punctuation
    : PunctuationFragment;

UnknownWord
    : ~([ \n\r\t.,!?%$&*+@:;…®™©℠=>] | '\'' | '’' | '‘' | '"' | '”' | '“' | '»' | '«'
    |'\\' | '-' |'(' | '/' | ')' | '[' | ']' | '{' | '}' | '^')+;

// Catch all remaining as Unknown.
Unknown : .+? ;


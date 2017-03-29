Turkish Tokenization and Sentence Extraction
============

## Sentence Boundary Detection

### Usage

Zemberek provides a mechanism for extracting sentences from text. This is usually the first step
 for an NLP application. TurkishSentenceExtractor class is used for this purpose. This class uses 
 a combination of simple rules and a Binary Averaged Perceptron model for finding 
 sentence boundaries. For default behavior, a singleton instance is provided:
 
    TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;

After that, three methods can be used for extracting sentences from a paragraph, a list
     of paragraphs or a String representing a document.
      For extracting sentences from a paragraph :

    String input = "Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?";     
    List<String> sentences = extractor.fromParagraph(input);

    sentences -> "Merhaba!", "Bugün 2. köprü Fsm.'de trafik vardı.", "değil mi?"
    
If input contains line breaks, fromDocument() method should be used.
fromParagraph() method will not split sentences from
 line breaks. fromDocument() method will first split input from line breaks to paragraphs
 then call fromParagraphs() internally.
 
    String input = "Merhaba\nNasılsınız?";
    List<String> sentences = extractor.fromDocument(input);        
    
    sentences -> "Merhaba", "Nasılsınız?"    
 

### Performance and speed

We compared our system with Maximum Entropy based OpenNlp 
[SentenceDetector](https://opennlp.apache.org/documentation/1.7.0/manual/opennlp.html#tools.sentdetect) for performance and speed.
 TurkishSentenceExtractor was trained with 8300 sentences.  OpenNLP model was
 trained with a very large but somewhat noisy data. We did try training it with our training data but results were worse.
 Test set contains 1916 sentences and many of them contains numbers with dot characters. 

Precision and Recall values are calculated for correct/incorrect boundaries. NIST error rate is calculated 
by dividing total amount of boundary errors to sentence count.

|  Library   | Precision| Recall   | NIST Error| Correct Sentences | Speed Sentences/s| Model Size |
|------------|----------|----------|-----------|-------------------|------------------|------------|
| Zemberek   |  0.9990  | 0.9974   |  0.3653%  |  99.37%           |  29030           |  10.1 KB   |
| OpenNLP    |  0.9871  | 0.9979   |  1.5136%  |  98.38%           |  23084           |  3.3 MB    |

Test platform: 2.3 Ghz AMD FX-8320, Ubuntu Linux 16.04 LTS

Both systems work quite well for this test set. Probably if OpenNlp system contained the rule based mechanism, it would give equivalent results. 

Open NLP model is provided by @sonerx

### Notes

- Extracting from paragraphs only splits from [.!?] characters.
- fromDocument method also splits from line breaks.
- Class will not split if a sentence ends with an abbreviation or a number,
- Narration sentences such as [Ali "topu at." dedi.] will be split.

## Tokenization

### Usage

Zemberek offers a rule based tokenizer class called TurkishTokenizer. 
This tokenizer uses a custom Antlr grammar based Lexer.
There are static instances provided for common use:
 
        TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
        TurkishTokenizer tokenizer = TurkishTokenizer.ALL;        

DEFAULT tokenizer ignores most white spaces (space, tab, line feed and carriage return). 
ALL tokenizer tokenizes everything. 
TurkishTokenizer instances are thread safe.
There are several helper methods in the tokenizer.
If detailed token information is required:

    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    List<Token> tokens = tokenizer.tokenize("Saat 12:00.");
    for (Token token : tokens) {
        System.out.println("Content = " + token.getText());
        System.out.println("Type = " + TurkishLexer.VOCABULARY.getDisplayName(token.getType()));
        System.out.println("Start = " + token.getStartIndex());
        System.out.println("Stop  = " + token.getStopIndex());
        System.out.println();
    }

This will output 

    Content = Saat
    Type = Word
    Start = 0
    Stop  = 3
    
    Content = 12:00
    Type = Time
    Start = 5
    Stop  = 9
    
    Content = .
    Type = Punctuation
    Start = 10
    Stop  = 10

Token is org.antlr.v4.runtime.Token implementation instance. User can reach several information
 such as type, start and stop character indexes.

User can customize a TurkishTokenizer. 
For example if punctuations and white spaces are needed to be ignored:
 
    TurkishTokenizer tokenizer = TurkishTokenizer
            .builder()
            .ignoreTypes(TurkishLexer.Punctuation, TurkishLexer.NewLine, TurkishLexer.SpaceTab)
            .build();
    List<Token> tokens = tokenizer.tokenize("Saat, 12:00.");
    for (Token token : tokens) {
        System.out.println("Content = " + token.getText());
        System.out.println("Type = " + TurkishLexer.VOCABULARY.getDisplayName(token.getType()));
        System.out.println();
    } 

This will output 

    Content = Saat
    Type = Word
    
    Content = 12:00
    Type = Time

If user only interested in String values of the tokens, this method can be used:

    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    List<String> tokens = tokenizer.tokenizeToStrings("Saat, 12:00.");
    tokens.forEach(System.out::println);

Code will print out:

    Saat
    ,
    12:00
    .

### Token Types

Tokenizer currently tokenizes these types:

    Abbreviation=1
    SpaceTab=2
    NewLine=3
    Time=4
    Date=5
    PercentNumeral=6
    Number=7
    URL=8
    Email=9
    HashTag=10
    Mention=11
    Emoticon=12
    RomanNumeral=13
    AbbreviationWithDots=14
    Word=15
    WordWithApostrophe=16
    Punctuation=17
    UnknownWord=18
    Unknown=19

### Speed

We tested the DEFAULT TurkishTokenizer with 100,000 lines of news sentences 
on an Intel Xeon E5-2680 @ 2.50GHz system. 
Tokenization speed is about 1,500,000 tokens per second. 

### Notes

- Tokenizer is not a very good classifier for some cases. There may be cases it splits meaningful words.
- It is suggested to use it for sentence tokenization after sentence extraction. 


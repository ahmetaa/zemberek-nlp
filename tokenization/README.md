Turkish Tokenization
============

## Sentence Boundary Detection

### Usage

Zemberek provides a mechanism for extracting sentences from a paragraph of text. This is usually the first step
 for an NLP application. TurkishSentenceExtractor class is used for this. This class uses 
 a combination of simple rules and a Binary Averaged Perceptron model for finding 
 sentence boundaries. Here is how to instantiate the class:
 
    TurkishSentenceExtractor extractor = TurkishSentenceExtractor.fromInternalModel();

After that, two methods can be used for extracting sentences from a paragraph or a list
     of paragraphs:

    String input = "Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?";     
    List<String> sentences = extractor.extract(input);

    sentences -> "Merhaba!", "Bugün 2. köprü Fsm.'de trafik vardı.", "değil mi?"

### Performance and speed

We compared our system with Maximum Entropy based OpenNlp 
[SentenceDetector] (https://opennlp.apache.org/documentation/1.7.0/manual/opennlp.html#tools.sentdetect) for performance and speed.
 TurkishSentenceExtractor was trained with 8300 sentences.  OpenNLP model was
 trained with a very large but somewhat noisy data. We did try training it with our training data but results were worse.
 Test set contains 1916 sentences and many of them contains numbers with dot characters.

Precision and Recall values are calculated for correct/incorrect boundaries. NIST error rate is calculated 
by dividing total amount of boundary errors to sentence size.

|            | Precision| Recall   | NIST Error| Correct Sentences | Speed Sentences/s| Model Size |
|------------|----------|----------|-----------|-------------------|------------------|------------|
| Zemberek   |  0.9990  | 0.9974   |  0.3653%  |  99.37%           |  29030           |  10.1 KB   |
| OpenNLP    |  0.9871  | 0.9979   |  1.5136%  |  99.38%           |  23084           |  3.3 MB    |

Test is run in a 2.3 Ghz AMD FX-8320, Ubuntu Linux 16.04 LTS

### Notes

Sentence extractor;

- only splits from [.!?] characters,
- does not split from line breaks so input should not contain it,
- will not split if a sentence ends with an abbreviation or a number,
- will split narration sentences such as [Ali "topu at." dedi.]

## Word Tokenization





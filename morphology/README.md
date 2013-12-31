Turkish Morphology and Disambiguation
============

## Morphology

### Example

    public class ParseWords {

        TurkishMorphParser parser;

        public ParseWords(TurkishMorphParser parser) {
            this.parser = parser;
        }

        public void parse(String word) {
            System.out.println("Word = " + word);
            List<MorphParse> parses = parser.parse(word);
            for (MorphParse parse : parses) {
                System.out.println(parse.formatLong());
            }
        }

        public static void main(String[] args) throws IOException {
            TurkishMorphParser parser = TurkishMorphParser.createWithDefaults();
            new ParseWords(parser).parse("kalemin");
        }
    }

    Word = kalemin
    [(kale:kale) (Noun;A3sg+P1sg:m+Gen:in)]
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Gen:in)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Gen:in)]
    [(kalem:kalem) (Noun;A3sg+P2sg:in+Nom)]

### Stemming and Lemmatization Example

    public class StemmingAndLemmatization {
        TurkishMorphParser parser;

        public StemmingAndLemmatization(TurkishMorphParser parser) {
            this.parser = parser;
        }

        public void parse(String word) {
            System.out.println("Word = " + word);

            System.out.println("Parses: ");
            List<MorphParse> parses = parser.parse(word);
            for (MorphParse parse : parses) {
                System.out.println(parse.formatLong());
                System.out.println("\tStems = " + parse.getStems());
                System.out.println("\tLemmas = " + parse.getLemmas());
            }
        }

        public static void main(String[] args) throws IOException {
            TurkishMorphParser parser = TurkishMorphParser.createWithDefaults();
            new StemmingAndLemmatization(parser).parse("kitabımızsa");
        }
    }

    Word = kitabımızsa
    Parses:
    [(kitap:kitab) (Noun;A3sg+P1pl:ımız+Nom)(Verb;Cond:sa+A3sg)]
        Stems = [kitab, kitabımızsa]
        Lemmas = [kitap, kitapımızsa]

### Known Issues

## Disambiguation

### Example

    public class DisambiguateSentences {

        TurkishSentenceParser sentenceParser;

        public DisambiguateSentences(TurkishSentenceParser sentenceParser) {
            this.sentenceParser = sentenceParser;
        }

        void parseAndDisambiguate(String sentence) {
            System.out.println("Sentence  = " + sentence);
            SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);

            System.out.println("Before disambiguation.");
            writeParseResult(sentenceParse);

            System.out.println("\nAfter disambiguation.");
            sentenceParser.disambiguate(sentenceParse);
            writeParseResult(sentenceParse);

        }

        private void writeParseResult(SentenceMorphParse sentenceParse) {
            for (SentenceMorphParse.Entry entry : sentenceParse) {
                System.out.println("Word = " + entry.input);
                for (MorphParse parse : entry.parses) {
                    System.out.println(parse.formatLong());
                }
            }
        }

        public static void main(String[] args) throws IOException {
            TurkishMorphParser morphParser = TurkishMorphParser.createWithDefaults();
            Z3MarkovModelDisambiguator disambiguator = new Z3MarkovModelDisambiguator();
            TurkishSentenceParser sentenceParser = new TurkishSentenceParser(
                    morphParser,
                    disambiguator
            );
            new DisambiguateSentences(sentenceParser)
                    .parseAndDisambiguate("Kırmızı kalemi al.");
        }
    }

    Sentence  = Kırmızı kalemi al.
    Before disambiguation.
    Word = Kırmızı
    [(kırmızı:kırmızı) (Adj)]
    [(kırmız:kırmız) (Noun;A3sg+Pnon+Acc:ı)]
    [(kırmız:kırmız) (Noun;A3sg+P3sg:ı+Nom)]
    [(kırmızı:kırmızı) (Noun;A3sg+Pnon+Nom)]
    [(Kırmızı:kırmızı) (Noun,Prop;A3sg+Pnon+Nom)]
    Word = kalemi
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Acc:i)]
    [(kale:kale) (Noun;A3sg+P1sg:m+Acc:i)]
    [(kalem:kalem) (Noun;A3sg+P3sg:i+Nom)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Acc:i)]
    Word = al
    [(al:al) (Adj)]
    [(Al:al) (Noun,Prop;A3sg+Pnon+Nom)]
    [(almak:al) (Verb;Pos+Imp+A2sg)]
    [(al:al) (Noun;A3sg+Pnon+Nom)]
    Word = .
    [(.:.) (Punc)]

    After disambiguation.
    Word = Kırmızı
    [(kırmızı:kırmızı) (Adj)]
    [(kırmız:kırmız) (Noun;A3sg+Pnon+Acc:ı)]
    [(kırmız:kırmız) (Noun;A3sg+P3sg:ı+Nom)]
    [(kırmızı:kırmızı) (Noun;A3sg+Pnon+Nom)]
    [(Kırmızı:kırmızı) (Noun,Prop;A3sg+Pnon+Nom)]
    Word = kalemi
    [(kalem:kalem) (Noun;A3sg+P3sg:i+Nom)]
    [(kale:kale) (Noun;A3sg+P1sg:m+Acc:i)]
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Acc:i)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Acc:i)]
    Word = al
    [(almak:al) (Verb;Pos+Imp+A2sg)]
    [(Al:al) (Noun,Prop;A3sg+Pnon+Nom)]
    [(al:al) (Adj)]
    [(al:al) (Noun;A3sg+Pnon+Nom)]
    Word = .
    [(.:.) (Punc)]

### Known Issues



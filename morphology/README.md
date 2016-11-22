Turkish Morphology and Disambiguation
============

## Morphology

### Example

Finds all morphological analyses of word "kalemin"

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<WordAnalysis> results = morphology.analyze("kalemin");
    results.forEach(s -> System.out.println(s.formatLong()));
    
    Output:

    [(kale:kale) (Noun;A3sg+P1sg:m+Gen:in)]
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Gen:in)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Gen:in)]
    [(kalem:kalem) (Noun;A3sg+P2sg:in+Nom)]

### Stemming and Lemmatization Example

Finds all morphological analyses, stems and lemmas of word "kitabımızsa"

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<WordAnalysis> results = morphology.analyze("kitabımızsa");
    for (WordAnalysis result : results) {
        System.out.println(result.formatLong());
        System.out.println("\tStems = " + result.getStems());
        System.out.println("\tLemmas = " + result.getLemmas());
    }
    
    Output:

    [(kitap:kitab) (Noun;A3sg+P1pl:ımız+Nom)(Verb;Cond:sa+A3sg)]
        Stems = [kitab, kitabımızsa]
        Lemmas = [kitap, kitapımızsa]

### Known Issues

## Disambiguation

### Example

    @Test
    public void testSentenceAnalysis() throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        Z3MarkovModelDisambiguator disambiguator = new Z3MarkovModelDisambiguator();
        TurkishSentenceAnalyzer analyzer = new TurkishSentenceAnalyzer(morphology, disambiguator);

        String sentence = "Kırmızı kalemi al.";
        System.out.println("Sentence  = " + sentence);
        SentenceAnalysis analysis = analyzer.analyze(sentence);

        System.out.println("Before disambiguation.");
        writeParseResult(analysis);

        System.out.println("\nAfter disambiguation.");
        analyzer.disambiguate(analysis);
        writeParseResult(analysis);
    }

    private void writeParseResult(SentenceAnalysis analysis) {
        for (SentenceAnalysis.Entry entry : analysis) {
            System.out.println("Word = " + entry.input);
            for (WordAnalysis w : entry.parses) {
                System.out.println(w.formatLong());
            }
        }
    }

    Sentence  = Kırmızı kalemi al.
    Before disambiguation.
    Word = Kırmızı
    [(kırmızı:kırmızı) (Adj)]
    [(kırmız:kırmız) (Noun;A3sg+Pnon+Acc:ı)]
    [(kırmız:kırmız) (Noun;A3sg+P3sg:ı+Nom)]
    [(Kırmız:kırmız) (Noun,Prop;A3sg+Pnon+Acc:ı)]
    [(Kırmız:kırmız) (Noun,Prop;A3sg+P3sg:ı+Nom)]
    [(kırmızı:kırmızı) (Noun;A3sg+Pnon+Nom)]
    Word = kalemi
    [(kale:kale) (Noun;A3sg+P1sg:m+Acc:i)]
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Acc:i)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Acc:i)]
    [(kalem:kalem) (Noun;A3sg+P3sg:i+Nom)]
    Word = al
    [(al:al) (Adj)]
    [(al:al) (Noun;A3sg+Pnon+Nom)]
    [(almak:al) (Verb;Pos+Imp+A2sg)]
    [(Al:al) (Noun,Prop;A3sg+Pnon+Nom)]
    Word = .
    [(.:.) (Punc)]
    
    After disambiguation.
    Word = Kırmızı
    [(kırmızı:kırmızı) (Adj)]
    [(kırmız:kırmız) (Noun;A3sg+Pnon+Acc:ı)]
    [(kırmız:kırmız) (Noun;A3sg+P3sg:ı+Nom)]
    [(Kırmız:kırmız) (Noun,Prop;A3sg+Pnon+Acc:ı)]
    [(Kırmız:kırmız) (Noun,Prop;A3sg+P3sg:ı+Nom)]
    [(kırmızı:kırmızı) (Noun;A3sg+Pnon+Nom)]
    Word = kalemi
    [(kalem:kalem) (Noun;A3sg+P3sg:i+Nom)]
    [(Kale:kale) (Noun,Prop;A3sg+P1sg:m+Acc:i)]
    [(kalem:kalem) (Noun;A3sg+Pnon+Acc:i)]
    [(kale:kale) (Noun;A3sg+P1sg:m+Acc:i)]
    Word = al
    [(almak:al) (Verb;Pos+Imp+A2sg)]
    [(al:al) (Noun;A3sg+Pnon+Nom)]
    [(al:al) (Adj)]
    [(Al:al) (Noun,Prop;A3sg+Pnon+Nom)]
    Word = .
    [(.:.) (Punc)]

### Known Issues



Turkish Morphology and Disambiguation
============

## Morphology

### Creating TurkishMorphology object

This module provides basic Turkish morphological analysis and generation. Analysis can be done in word and sentence level.
 For word level analysis and generation, TurkishMorphology class is used. By default, class is instantiated as follows:
 
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
     
After this, Turkish suffix graph is generated, internal dictionaries are loaded and they are connected to related graph 
 nodes. Because creation of this object takes time and consumes memory, 
 a single instance should be used throughout the life of an application.
  
  You can add your own dictionaries or remove existing items during creation of the TurkishMorphology class. For example, you have
  a dictionary file *my-dictionary.txt* in this form:
  
    show 
    relaks [P:Adj]
    gugıllamak
    Hepsiburada
    
Dictionary rules are explained here: // TODO //

For adding this dictionary, builder mechanism is used for instantiation:
  
    TurkishMorphology analyzer = TurkishMorphology.builder()
            .addDefaultDictionaries()
            .addTextDictionaries(new File("my-dictionary.txt"))
            .build();
  
There are other options available for building the object.
Turkish morphology class contain a built in cache, so in time analysis speed will get faster. There 
is an option to disable the cache if builder mechanism is used.  

### Single word morphological analysis

For analyzing a word, *analyze* method is used. it returns a list of *WordAnalysis* objects.
 There are several things user should be aware of.
 
 - Returning list is never empty.
 - If a word cannot be parsed, list will contain 1 item. You can identify those words by checking if getPos() method returns PrimaryPos.Unknown or 
   with isUnknown() method (after 0.11.0). Example:
   
        TurkishMorphology parser = TurkishMorphology.createWithDefaults();
        String input = "fofofo";
        List<WordAnalysis> result = parser.analyze(input);
        System.out.println("Input = " + input);
        System.out.println("Result size = " + result.size());
        WordAnalysis analysis = result.get(0);
        System.out.println("Result DictioanyItem = " + analysis.dictionaryItem);
        System.out.println("Is Unknown? " + analysis.isUnknown());
        System.out.println("Is Runtime? " + analysis.isRuntime());        
        result.forEach(s-> System.out.println("Analysis = "  + s.formatLong()));

        Input = fofofo
        Result size = 1        
        Result DictioanyItem = UNK [P:Unk, Unk]
        Is Unknown? true
        Is Runtime? false        
        Analysis = [(UNK:fofofo) (Unk,Unk;Unkown)]   
   
 - There are some words, their roots do not exist in dictionary but they are analyzed anyway. Such as numbers or 
   proper nouns that start with capital letters and contain a single quote. 
   Users can disable this behavior by calling disableUnidentifiedAnalyzer() method while building TurkishMorphology object. 
   
   Consider examples "Matsumoto'ya" and "153'ü".
   System will temporarily generate DictionaryItem objects for "Matsumoto" and "153" and try to parse the words.
   If successful, returning WordAnalysis objects will have that temporary DictionaryItem object in it.
    User can check if DictionaryItem is generated temporarily by checking isRuntime() 
    method of returning WordAnalysis objects (After 0.11.0)
   or checking if that DictionaryItem object's root Attributes  contain "RootAttribute.Runtime". 
   
        
        Output for Matsumo'ya:

        Input = Matsumoto'ya
        Result size = 1
        Result DictioanyItem = Matsumoto [P:Noun, Prop; A:Runtime]
        Is Unknown? false
        Is Runtime? true      
        Anaysis = [(Matsumoto:matsumoto) (Noun,Prop;A3sg+Pnon+Dat:ya)]
           
        For 153'ü

        Input = 153'ü
        Result size = 2
        Result DictioanyItem = 153 [P:Num, Card]
        Is Unknown? false
        Is Runtime? true     
        Analysis = [(153:153) (Num,Card)(Noun;A3sg+P3sg:ü+Nom)]
        Analysis = [(153:153) (Num,Card)(Noun;A3sg+Pnon+Acc:ü)]           

 - Even if input is lower case and without an apostrophe ['], result may contain Proper noun analyses. 
 Analyzer does not care about punctuation rules. User may decide if this is a valid analysis with post processing.
 In future versions this behavior may change or some helper functionality will be added.
   Apostrophe is only checked for unknown proper nouns. 
  
        Input = ankaradan
        Result size = 1
        Result DictioanyItem = Ankara [P:Noun, Prop]
        Is Unknown? false
        Is Runtime? false     
        Analysis = [(Ankara:ankara) (Noun,Prop;A3sg+Pnon+Abl:dan)]  

### Examples

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



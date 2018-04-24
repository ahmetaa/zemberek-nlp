Turkish Morphology
============

## Morphology

Turkish is a morphologically rich language. 
Zemberek provides morphological analysis, morphological ambiguity resolution and word generation functions. 

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

For analyzing a word, *analyze* method is used. it returns a `WordAnalysis` object. This object
contains the input and zero or more `SingleAnalysis` objects in it.
 
 - If a word cannot be parsed, `WordAnalysis` will have 0 `SingleAnalysis` item.
   
 - Even if a word's root does not exist in dictionary, it will be analyzed anyway. Such as numbers or 
   proper nouns that start with capital letters and contain a single quote. 
   Users can disable this behavior by calling `disableUnidentifiedAnalyzer()` method while building `TurkishMorphology` object. 
   
   Consider examples "Matsumoto'ya" and "153'ü".
   System will temporarily generate `DictionaryItem` objects for "Matsumoto" and "153" and try to parse the words.
   If successful, returning `WordAnalysis` will contain `SingleAnalysis` objects that temporary `DictionaryItem` object in it.
   User can check if DictionaryItem is generated temporarily by checking `isRuntime()` 
   method on returning `SingleAnaysis` objects of the `WordAnalysis` result 
   or checking if that `DictionaryItem` object's root attributes  contain `RootAttribute.Runtime`. 
   
 - Even if input is lower case and without an apostrophe ['], result may contain Proper noun analyses. 
 Analyzer does not care about punctuation rules. User may decide if this is a valid analysis with post processing.
 In future versions this behavior may change or some helper functionality will be added.
 Apostrophe is only checked for unknown proper nouns.
 

     Input = ankaradan   
     Analysis = [Ankara:Noun,Prop] ankara:Noun+A3sg+dan:Abl  

### Examples

Finds all morphological analyses of word "kalemin"

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    WordAnalysis results = morphology.analyze("kalemin");
    results.forEach(s -> System.out.println(s.formatLong()));
    
    Output:

    [kale:Noun] kale:Noun+A3sg+m:P1sg+in:Gen
    [Kale:Noun,Prop] kale:Noun+A3sg+m:P1sg+in:Gen
    [kalem:Noun] kalem:Noun+A3sg+in:Gen
    [kalem:Noun] kalem:Noun+A3sg+in:P2sg


### Stemming and Lemmatization Example

Finds all morphological analyses, stems and lemmas of word "kitabımızsa"

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    WordAnalysis result = morphology.analyze("kitabımızsa");
    for (SingleAnalysis analysis : result) {
      System.out.println(analysis.formatLong());
      System.out.println("\tStems = " + analysis.getStems());
      System.out.println("\tLemmas = " + analysis.getLemmas());
    }
    
 Output:

    [kitap:Noun] kitab:Noun+A3sg+ımız:P1pl|Zero→Verb+sa:Cond+A3sg
      Stems = [kitab, kitabımız]
      Lemmas = [kitap, kitabımız]

### Known Issues

 - Some words may not get analyzed correctly.
 - Words with circumflex letters may have problems.
 - Proper noun and Abbreviations may not be analyzed correctly

## Ambiguity Resolution

Turkish is a highly ambigious language. As shown in the example below, Word `yarın` has 9 morphological
analyses. Zemberek uses an Averaged Perceptron based mechanism to resolve ambiguity. 

### Example

Below is an example of analyzing and applying ambiguity resolution to a sentence.

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    String sentence = "Yarın kar yağacak.";
    System.out.println("Sentence  = " + sentence);
    List<WordAnalysis> analysis = morphology.analyzeSentence(sentence);

    System.out.println("Before disambiguation.");
    for (WordAnalysis entry : analysis) {
      System.out.println("Word = " + entry.getInput());
      for (SingleAnalysis single : entry) {
        System.out.println(single.formatLong());
      }
    }
    
    System.out.println("\nAfter disambiguation.");
    SentenceAnalysis after = morphology.disambiguate(sentence, analysis);
    after.bestAnalysis().forEach(s-> System.out.println(s.formatLong()));

Output:    

    Sentence  = Yarın kar yağacak.
    Before disambiguation.
    Word = Yarın
    [yarın:Adv] yarın:Adv
    [yarmak:Verb] yar:Verb+Imp+ın:A2pl
    [Yar:Noun,Prop] yar:Noun+A3sg+ın:Gen
    [Yar:Noun,Prop] yar:Noun+A3sg+ın:P2sg
    [yar:Noun] yar:Noun+A3sg+ın:Gen
    [yar:Noun] yar:Noun+A3sg+ın:P2sg
    [yarı:Noun] yarı:Noun+A3sg+n:P2sg
    [yarın:Noun,Time] yarın:Noun+A3sg
    [yarı:Adj] yarı:Adj|Zero→Noun+A3sg+n:P2sg
    Word = kar
    [karmak:Verb] kar:Verb+Imp+A2sg
    [kar:Noun] kar:Noun+A3sg
    [kâr:Noun] kar:Noun+A3sg
    Word = yağacak
    [yağmak:Verb] yağ:Verb+acak:Fut+A3sg
    [yağmak:Verb] yağ:Verb|acak:FutPart→Adj
    Word = .
    [.:Punc] .:Punc
    
    After disambiguation.
    [yarın:Noun,Time] yarın:Noun+A3sg
    [kar:Noun] kar:Noun+A3sg
    [yağmak:Verb] yağ:Verb+acak:Fut+A3sg
    [.:Punc] .:Punc


### Known Issues

As of version 0.12, ambiguity resolution mechanism is trained with automatically generated training data. 
In later versions, manually
generated data will be added to improve the accuracy. Previous language model based system is 
retired for the time being.    

## Word Generation

Zemberek offers simple word generation functionality.
Generation requires root form of a word of a Dictionary item and morphemes. Generation mechanism 
is very similar to analysis mechanism. But it passes through empty Morphemes in the 
search graph even if they are not provided in the input. For example, user does not need to provide 
A3sg morpheme as input as it's surface is empty. Generation returns an inner static 
 class `Result` instance. There may be multiple results. Form that object, generated word 
 (surface form) and analysis results of the generated word can be accessed.      

### Example

In the example below, a dictionary with a single word "armut" is used for creating TurkishMorphology 
instance. After that, possessive and case suffix combinations are used for generating inflections of that word.
You can run this example from `zemberek.examples.morphology.GenerateWords` in examples module.

    String[] number = {"A3sg", "A3pl"};
    String[] possessives = {"P1sg", "P2sg", "P3sg"};
    String[] cases = {"Dat", "Loc", "Abl"};

    TurkishMorphology morphology =
        TurkishMorphology.builder().addDictionaryLines("armut").disableCache().build();

    DictionaryItem item = morphology.getLexicon().getMatchingItems("armut").get(0);
    for (String numberM : number) {

      for (String possessiveM : possessives) {
        for (String caseM : cases) {
          List<Result> results =
              morphology.getWordGenerator().generate(item, numberM, possessiveM, caseM);
          for (Result result : results) {
            System.out.println(result.surface);
          }
        }
      }
    }

Output:

    armuduma
    armudumda
    armudumdan
    armuduna
    armudunda
    armudundan
    armuduna
    armudunda
    armudundan
    armutlarıma
    armutlarımda
    armutlarımdan
    armutlarına
    armutlarında
    armutlarından
    armutlarına
    armutlarında
    armutlarından


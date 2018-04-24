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

For analyzing a word, *analyze* method is used. it returns a `WordAnalysis` object. This object
contains the input and zero or more SingleAnalysis objects in it.
 
  - If a word cannot be parsed, `WordAnalysis` will have list will contain 0 SingleAnalysis item.
   
 - There are some words, their roots do not exist in dictionary but they are analyzed anyway. Such as numbers or 
   proper nouns that start with capital letters and contain a single quote. 
   Users can disable this behavior by calling disableUnidentifiedAnalyzer() method while building TurkishMorphology object. 
   
   Consider examples "Matsumoto'ya" and "153'ü".
   System will temporarily generate DictionaryItem objects for "Matsumoto" and "153" and try to parse the words.
   If successful, returning WordAnalysis objects will have that temporary DictionaryItem object in it.
    User can check if DictionaryItem is generated temporarily by checking isRuntime() 
    method of returning SingleAnaysis objects in WordAnalysis result 
   or checking if that DictionaryItem object's root Attributes  contain "RootAttribute.Runtime". 
   
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

## Disambiguation

### Example

urkishMorphology morphology = TurkishMorphology.createWithDefaults();

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

### Known Issues



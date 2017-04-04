Turkish Text Normalization
============

## Turkish Spell Checker

Spell checker provides methods for checking if a word is correctly written and can give suggestions for a word.
TurkishSpellChecker class is used for this.

 
Instantiation:

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

After this, several methods are available. _check(String input)_ method is used for 
 detecting if a word is spelled correctly.
 
    String[] words = {"okuyabileceğimden","okuyablirim", "Ankara", "Ankar'ada", "3'de", "3'te"};
    for (String word : words) {
        System.out.println(word + " = " + spellChecker.check(word));
    } 

Output will be

    okuyabileceğimden = true
    okuyablirim = false
    Ankara = true
    Ankar'ada = false
    3'de = false
    3'te = true

## Spelling Suggestion

Currently Zemberek provides a simple suggestion mechanism.

For getting suggestions for incorrect words suggestForWord method can be used:

    String[] words = {"okuyablirim", "tartısıyor", "Ankar'ada", "knlıca"};
    for (String word : words) {
        System.out.println(word + " = " + spellChecker.suggestForWord(word));
    } 
    
Will give these suggestions:
    
    okuyablirim = [okuyabilirim]
    tartısıyor = [tartışıyor, tartılıyor, tartınıyor]
    Ankar'ada = [Ankara'da, Ankara'ya, Ankara'dan, Ankaray'da, Ankara'ma, Antara'da, Ankara'ca, Cankara'da, Anakarada, Ankara'na, Angara'da, Ankara'mda, Ankara'nda, Ankaray'a]
    knlıca = [Kanlıca, kanlıca, In'lıca, kılıca, anlıca, Kanlı'ca, kınlıca, kalıca]

If user provides a higher order language model (bi-gram models are sufficient) and context words, ranking of the suggestions may improve. Method below is used for this. 

    public List<String> suggestForWord(
                String word,
                String leftContext,
                String rightContext,
                NgramLanguageModel lm)
    
### Limitations

 - It only may correct for 1 insertion, 1 deletion, 1 substitution and 1 transposition errors.
 - It ranks the results with an internal unigram language model.
 - There is no deasciifier.
 - It does not correct numbers, dates and times.
 - There may be junk results.
 - For shorter words, there will be a lot of suggestions (sometimes >50 ).
 - Suggestion function is not so fast (Around 500-1000 words/second).
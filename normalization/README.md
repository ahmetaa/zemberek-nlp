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
 
 ## Noisy Text Normalization (Not Yet Released)
 
 Zemberek offers noisy text normalization function. This tool can be used correcting 
 words written incorrectly or informal speech from noisy texts. Some examples:
 
    Tmm, yarin havuza giricem ve aksama kadar yaticam :)    
    ah aynen ya annemde fark etti siz evinizden çıkmayın diyo
    gercek mı bu? Yuh! Artık unutulması bile beklenmiyo   
    Hayır hayat telaşm olmasa alacam buraları gökdelen dikicem.
    
Some NLP tasks needs normalization as a pre-processing step before applying 
actual algorithms to the text. Normalization especially helps improved results in areas like: 

- Social media and forum texts
- Chat, messaging or bot applications  
- Mobil phone keyboards with no or bad spell correction.

### Usage

User needs to provide some lookup files and a language model. In this link, a usable language model 
and necessary lookup tables are provided. But for specific needs, usually custom work is necessary. 

After downloading the files, lets assume they will be in zemberek-data/normalization folder.
We initialize TurkishSentenceNormalizer as:


Then simple call normalize() method.

Results for the above sentences:

    Tmm, yarin havuza giricem ve aksama kadar yaticam :)
    tamam , yarın havuza gireceğim ve akşama kadar yatacağım :)
     
    ah aynenya annemde fark ettı siz evinizden cıkmayın diyo
    ah aynenya annemde fark etti siz evinizden çıkmasın diyor
    
    gercek mı bu? Yuh! Artık unutulması bile beklenmiyo
    gerçek mı bu ? yuh ! artık unutulması bile beklenmiyor
    
    Hayır hayat telaşm olmasa alacam buraları gökdelen dikicem.
    hayır hayat telaşı olmasa alacağım buraları gökdelen dikeceğim .

As it is seen, some words were not corrected and output is all lower case.
These problems will be fixed in upcoming releases. 

### Method

Zemberek uses several heuristics, lookup tables and language models for text normalization.
Some of the work:
- From clean and noisy corpora, vocabularies are created using morphological analysis.
- With some heuristics and language model, words that should be split to two are found.
- From corpora, correct, incorrect and possibly-incorrect sets are created.
- For pre-processing, deasciifier, split and combine heuristics are applied. 
- Using those sets and large corpora, a noisy to clean word lookup is 
  generated using a modified version of Hassan and Menezes's 2013 work 
  "Social Text Normalization using Contextual Graph Random Walks".
- Then for a sentence, for every noisy word, candidates are collected from lookup tables, 
informal and ascii-matching morphologcal analysis and spell checker. 
- Most likely correct sequence is found running Viterbi algorithm using a language model on candidate words

### Issues

Normalization function:

- may change correct words.
- may change formatting and casing or remove punctuations.
- may not work well for some cases.
  

 
 

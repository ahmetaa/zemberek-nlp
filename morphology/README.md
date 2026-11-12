Turkish Morphology
============

  * [Morphology](#morphology)
    + [Maven Usage](#maven-usage)
    + [Creating TurkishMorphology object](#creating-turkishmorphology-object)
    + [Single word morphological analysis](#single-word-morphological-analysis)
    + [Examples](#examples)
    + [Reading Analysis Output & Morpheme Abbreviations](#reading-analysis-output--morpheme-abbreviations)
    + [Stemming and Lemmatization Example](#stemming-and-lemmatization-example)
    + [Known Issues](#known-issues)
    + [Informal Turkish Words Analysis](#informal-turkish-words-analysis)
    + [Diacritics Ignored Analysis](#diacritics-ignored-analysis)
  * [Ambiguity Resolution](#ambiguity-resolution)
    + [Example](#example)
    + [Known Issues](#known-issues-1)
  * [Word Generation](#word-generation)
    + [Example](#example-1)

## Morphology

Turkish is a morphologically rich language. 
Zemberek provides morphological analysis, morphological ambiguity resolution and word generation functions.

### Maven Usage
 
    <dependency>
        <groupId>zemberek-nlp</groupId>
        <artifactId>zemberek-morphology</artifactId>
        <version>0.18.0</version>
    </dependency>
 

### Creating TurkishMorphology object

This module provides basic Turkish morphological analysis and generation. Analysis can be done in word and sentence level.
 For word level analysis and generation, TurkishMorphology class is used. By default, class is instantiated as follows:
 
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
     
After this, Turkish suffix graph is generated, internal dictionaries are loaded and they are connected to related graph 
 nodes. Because creation of this object takes time and consumes memory, 
 a single instance should be used throughout the life of an application.
  
  You can add your own dictionaries or remove existing items during creation of the TurkishMorphology class. For example, you have
  a dictionary file *my-dictionary.txt* in this form (albeit terrible words):
  
    show 
    relaks [P:Adj]
    gugıllamak
    Hepsiburada
    
Dictionary rules are explained [here](https://github.com/ahmetaa/zemberek-nlp/wiki/Text-Dictionary-Rules)

For adding this dictionary, RootLexicon builder mechanism can be used like this:
  
    RootLexicon lexicon = RootLexicon.builder()
        .addDefaultLexicon()
        .addTextDictionaries(Paths.get("my-dictionary.txt"))
        .build();

    TurkishMorphology analyzer = TurkishMorphology.builder()
        .setLexicon(lexicon)
        .build();

Or alternatively:
        
    TurkishMorphology analyzer = TurkishMorphology.create(lexicon);
  
There are other options available for building the object.
Turkish morphology class contain a built in cache, so in time analysis speed will get faster. There 
is an option to disable the cache if builder mechanism is used. For example:

    TurkishMorphology analyzer = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .disableCache()
        .build();

Generating your own dictionary can be liked this:

      RootLexicon myLexicon = RootLexicon.builder()
          .setLexicon(RootLexicon.getDefault) // start with default
          .addDictionaryLines("foo", "rar") // add two new nouns
          .addTextDictionaries(Paths.get("my-own-dictionary-file")) // add from file
          .build();

### Single word morphological analysis

For analyzing a word, *analyze* method is used. it returns a `WordAnalysis` object. This object
contains the input and zero or more `SingleAnalysis` objects in it.
 
 - If a word cannot be parsed, `WordAnalysis` will have 0 `SingleAnalysis` item.
   
 - Even if a word's root does not exist in dictionary, system will try to analyze it anyway. 
   This way numbers or proper nouns that start with capital letters and contain a single quote may get analyzed. 
   Users can disable this behavior by calling `disableUnidentifiedTokenAnalyzer()` 
   method while building `TurkishMorphology` object. 
   
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


### Reading Analysis Output & Morpheme Abbreviations

`SingleAnalysis.formatLong()` prints the dictionary root in brackets (`[lemma:POS]`) followed by `surface:Morpheme` tokens:

- `:` pairs an actual surface segment with its morpheme ID (e.g. `in:Gen`); morphemes with empty surface form omit the prefix (e.g. `A3sg`). Note that `Nom` (Nominative) and `Pnon` (No possessive) are omitted from `SingleAnalysis` output to reduce visual noise.
- `+` separates inflectional morphemes within an inflectional group (IG).
- `|` marks a derivational boundary, and `→` separates the derivational morpheme from the resulting part of speech (e.g. `kitab:Noun+A3sg+ımız:P1pl|Zero→Verb+sa:Cond+A3sg`).

#### Primary POS (Part-of-Speech) Tags

| Tag | Meaning | Example Word & Analysis |
| :--- | :--- | :--- |
| `Noun` | Noun | *kitap* (`[kitap:Noun] kitap:Noun+A3sg`) |
| `Verb` | Verb | *okudu* (`[okumak:Verb] oku:Verb+du:Past+A3sg`) |
| `Adj` | Adjective | *güzel* (`[güzel:Adj] güzel:Adj`) |
| `Adv` | Adverb | *hızlıca* (`[hızlıca:Adv] hızlıca:Adv`) |
| `Pron` | Pronoun | *ben* (`[ben:Pron,Pers] ben:Pron+A1sg`) |
| `Det` | Determiner | *bir* (`[bir:Det] bir:Det`) |
| `Num` | Numeral | *üç* (`[üç:Num,Card] üç:Num`) |
| `Postp` | Postposition | *için* (`[için:Postp,PCNom] için:Postp`) |
| `Conj` | Conjunction | *ve* (`[ve:Conj] ve:Conj`) |
| `Ques` | Question particle / clitic | *mi* (`[mi:Ques] mi:Ques+Pres+A3sg`) |
| `Interj` | Interjection | *eyvah* (`[eyvah:Interj] eyvah:Interj`) |
| `Dup` | Echoic / reduplicative element (duplicator) | *mırıl* (`[mırıl:Dup] mırıl:Dup`, in *mırıl mırıl*) |
| `Punc` | Punctuation | `.` (`[.:Punc] .:Punc`) |
| `Unk` | Unknown / out-of-vocabulary token | *xyz* (`[xyz:Unk] xyz:Unk`) |

#### Secondary POS Tags

| Tag | Meaning | Example Word & Analysis |
| :--- | :--- | :--- |
| `Prop` | Proper noun | *Ankara* (`[Ankara:Noun,Prop] ankara:Noun+A3sg`) |
| `Time` | Temporal noun | *yarın* (`[yarın:Noun,Time] yarın:Noun+A3sg`) |
| `Abbrv` | Abbreviation / acronym | *TBMM* (`[TBMM:Noun,Abbrv] tbmm:Noun+A3sg`) |
| `RegAbbrv` | Regular abbreviation ending with a period | *Prof.* (`[Prof.:Noun,RegAbbrv] prof:Noun+A3sg`) |
| `Pers` | Personal pronoun | *ben*, *biz* (`[ben:Pron,Pers] ben:Pron+A1sg`) |
| `Demons` | Demonstrative pronoun | *bu*, *şunlar* (`[bu:Pron,Demons] bu:Pron+A3sg`) |
| `Quant` | Quantificational / indefinite pronoun | *bazısı*, *hepsi* (`[hepsi:Pron,Quant] hepsi:Pron+A3pl+P3pl`) |
| `Ques` | Interrogative pronoun (secondary tag on `Pron`) | *kim*, *ne* (`[kim:Pron,Ques] kim:Pron+A3sg`) |
| `Reflex` | Reflexive pronoun | *kendi* (`[kendi:Pron,Reflex] kendi:Pron+A3sg+P3sg`) |
| `Card` | Cardinal numeral | *beş*, *15* (`[beş:Num,Card] beş:Num`) |
| `Ord` | Ordinal numeral | *beşinci*, *5.* (`[beşinci:Num,Ord] beşinci:Num`) |
| `Dist` | Distributive numeral | *beşer* (`[beşer:Num,Dist] beşer:Num`) |
| `Real` | Real / decimal number | *3,14* (`[3,14:Num,Real] 3,14:Num`) |
| `Range` | Numeric range | *10-15* (`[10-15:Num,Range] 10-15:Num`) |
| `Percent` | Percentage numeral | *%25* (`[%25:Num,Percent] %25:Num`) |
| `Ratio` | Ratio numeral | *1/4* (`[1/4:Num,Ratio] 1/4:Num`) |
| `RomanNumeral` | Roman numeral | *XIV.* (`[XIV.:Num,RomanNumeral] xiv:Num`) |
| `PCDat` | Postposition requiring a Dative complement (`-(y)A`) | *kadar* (*eve kadar* — `[kadar:Postp,PCDat]`) |
| `PCAbl` | Postposition requiring an Ablative complement (`-dAn`) | *sonra* (*evden sonra* — `[sonra:Postp,PCAbl]`) |
| `PCIns` | Postposition requiring an Instrumental complement (`-(y)lA`) | *birlikte* (*seninle birlikte* — `[birlikte:Postp,PCIns]`) |
| `PCNom` | Postposition requiring a Nominative complement | *için* (*kitap için* — `[için:Postp,PCNom]`) |
| `PCGen` | Postposition requiring a Genitive complement | *gibi* (*benim gibi* — `[gibi:Postp,PCGen]`) |
| `PCAcc` | Postposition requiring an Accusative complement (`-(y)I`) | *müteakip* (*toplantıyı müteakip* — `[müteakip:Postp,PCAcc]`) |
| `Clock` / `Date` | Time-of-day / calendar date expression | *14:30* (`Clock`), *12.05.2024* (`Date`) |
| `Email` / `Url` / `Mention` / `HashTag` / `Emoticon` | Web and social-media token subtypes | *a@b.com*, *https://...*, *@ad*, *#etiket*, *:)* |

#### Inflectional Morphemes

| Category | Tag | Meaning / Explanation | Example Word & Analysis |
| :--- | :--- | :--- | :--- |
| **Person / Number** | `A1sg` | 1st person singular agreement | *geldim* (`gel:Verb+di:Past+m:A1sg`) |
| | `A2sg` | 2nd person singular agreement | *geldin* (`gel:Verb+di:Past+n:A2sg`) |
| | `A3sg` | 3rd person singular agreement (null suffix) | *ev* (`ev:Noun+A3sg`), *geldi* (`gel:Verb+di:Past+A3sg`) |
| | `A1pl` | 1st person plural agreement | *geldik* (`gel:Verb+di:Past+k:A1pl`) |
| | `A2pl` | 2nd person plural agreement | *geldiniz* (`gel:Verb+di:Past+niz:A2pl`) |
| | `A3pl` | 3rd person plural agreement (`-lAr`) | *evler* (`ev:Noun+ler:A3pl`), *geldiler* (`gel:Verb+di:Past+ler:A3pl`) |
| **Possessive** | `Pnon` | No possessive suffix (omitted in `formatLong`) | *elma* (`elma:Noun+A3sg`) |
| | `P1sg` | 1st person singular possessive (`-(I)m`, "my") | *elmam* (`elma:Noun+A3sg+m:P1sg`) |
| | `P2sg` | 2nd person singular possessive (`-(I)n`, "your") | *elman* (`elma:Noun+A3sg+n:P2sg`) |
| | `P3sg` | 3rd person singular possessive (`-(s)I(n)`, "his/her/its") | *elması* (`elma:Noun+A3sg+sı:P3sg`) |
| | `P1pl` | 1st person plural possessive (`-(I)mIz`, "our") | *elmamız* (`elma:Noun+A3sg+mız:P1pl`) |
| | `P2pl` | 2nd person plural possessive (`-(I)nIz`, "your [pl.]") | *elmanız* (`elma:Noun+A3sg+nız:P2pl`) |
| | `P3pl` | 3rd person plural possessive (`-lArI`, "their") | *elmaları* (`elma:Noun+A3sg+ları:P3pl`) |
| **Case** | `Nom` | Nominative / unmarked case (omitted in `formatLong`) | *elma* (`elma:Noun+A3sg`) |
| | `Dat` | Dative case (`-(y)A`, "to / toward") | *elmaya* (`elma:Noun+A3sg+ya:Dat`) |
| | `Acc` | Accusative / definite object case (`-(y)I`) | *elmayı* (`elma:Noun+A3sg+yı:Acc`) |
| | `Abl` | Ablative case (`-dAn`, "from") | *elmadan* (`elma:Noun+A3sg+dan:Abl`) |
| | `Loc` | Locative case (`-dA`, "in / at / on") | *elmada* (`elma:Noun+A3sg+da:Loc`) |
| | `Ins` | Instrumental / comitative case (`-(y)lA`, "with / by") | *elmayla* (`elma:Noun+A3sg+yla:Ins`) |
| | `Gen` | Genitive case (`-(n)In`, "of") | *elmanın* (`elma:Noun+A3sg+nın:Gen`) |
| | `Equ` | Equative case (`-cA`, "according to / like") | *bence* (`ben:Pron+A1sg+ce:Equ`) |
| **Polarity & Copula** | `Neg` | Verbal negation (`-mA`) | *gelmedi* (`gel:Verb+me:Neg+di:Past+A3sg`) |
| | `Unable` | Impotential / inability (`-(y)AmA`, "cannot") | *okuyamıyorum* (`oku:Verb+yama:Unable+yor:Prog1+um:A1sg`) |
| | `Cop` | Emphatic / generalizing copula (`-dIr`) | *güzeldir* (`güzel:Adj|Zero→Verb+Pres+A3sg+dir:Cop`) |
| **Tense / Aspect / Mood** | `Pres` | Present copula tense | *öğrenciyim* (`öğrenci:Noun+A3sg|Zero→Verb+Pres+yim:A1sg`) |
| | `Past` | Definite / witnessed past tense (`-dI`) | *okudu* (`oku:Verb+du:Past+A3sg`) |
| | `Narr` | Narrative / evidential past tense (`-mIş`) | *okumuş* (`oku:Verb+muş:Narr+A3sg`) |
| | `Prog1` | Progressive / continuous tense (`-(I)yor`) | *okuyor* (`oku:Verb+yor:Prog1+A3sg`) |
| | `Prog2` | Formal progressive tense (`-mAktA`) | *okumakta* (`oku:Verb+makta:Prog2+A3sg`) |
| | `Aor` | Aorist / habitual tense (`-(I/A)r`, neg. `-z`) | *okur* (`oku:Verb+r:Aor+A3sg`) |
| | `Fut` | Future tense (`-(y)AcAk`) | *okuyacak* (`oku:Verb+yacak:Fut+A3sg`) |
| | `Cond` | Conditional mood (`-sA`, copular/secondary) | *okursa* (`oku:Verb+r:Aor+sa:Cond+A3sg`) |
| | `Desr` | Desiderative / conditional root mood (`-sA`) | *okusa* (`oku:Verb+sa:Desr+A3sg`) |
| | `Imp` | Imperative mood | *oku* (`oku:Verb+Imp+A2sg`), *gelsin* (`gel:Verb+Imp+sin:A3sg`) |
| | `Opt` | Optative / voluntative mood (`-(y)A`, `-(y)AyIm`) | *okuyayım* (`oku:Verb+ya:Opt+yım:A1sg`) |
| | `Neces` | Necessitative mood (`-mAlI`, "must / should") | *okumalı* (`oku:Verb+malı:Neces+A3sg`) |

#### Derivational Morphemes

| Category | Tag | Meaning / Explanation | Example Word & Analysis |
| :--- | :--- | :--- | :--- |
| **Nominal / Adjectival** | `Dim` | Diminutive noun (`-cIk` / `-cAğIz`) | *elmacık* (`elma:Noun+A3sg|cık:Dim→Noun+A3sg`) |
| | `Ness` | Abstract / state / place noun (`-lIk`) | *iyilik* (`iyi:Adj|lik:Ness→Noun+A3sg`) |
| | `With` | Proprietive adjective ("having / with", `-lI`) | *elmalı* (`elma:Noun+A3sg|lı:With→Adj`) |
| | `Without` | Privative adjective ("without", `-sIz`) | *elmasız* (`elma:Noun+A3sg|sız:Without→Adj`) |
| | `Related` | Relational adjective (`-sAl`) | *bilimsel* (`bilim:Noun+A3sg|sel:Related→Adj`) |
| | `JustLike` | Similative adjective ("-ish / like", `-(I)msI`, `-sI`) | *tahtamsı* (`tahta:Noun+A3sg|msı:JustLike→Adj`) |
| | `Rel` | Relative adjective / pronoun (`-ki`) | *evdeki* (`ev:Noun+A3sg+de:Loc|ki:Rel→Adj`) |
| | `Agt` | Agentive / occupational noun (`-cI`) | *kitapçı* (`kitap:Noun+A3sg|çı:Agt→Noun+A3sg`) |
| | `Become` | Inchoative verb ("become X", `-lAş`) | *taşlaştı* (`taş:Noun+A3sg|laş:Become→Verb+tı:Past+A3sg`) |
| | `Acquire` | Acquire verb ("gain X", `-lAn`) | *evlendi* (`ev:Noun+A3sg|len:Acquire→Verb+di:Past+A3sg`) |
| | `Ly` | Manner adverb from adjective (`-cA`) | *güzelce* (`güzel:Adj|ce:Ly→Adv`) |
| | `Zero` | Zero-morpheme POS conversion or copula carrier | *güzeller* (`güzel:Adj|Zero→Noun+ler:A3pl`) |
| **Verbal Voice & Modality** | `Caus` | Causative voice (`-dIr`, `-t`, `-Ir`, `-Ar`) | *okuttu* (`oku:Verb|t:Caus→Verb+tu:Past+A3sg`) |
| | `Pass` | Passive voice (`-Il`, `-In`) | *okundu* (`oku:Verb|n:Pass→Verb+du:Past+A3sg`) |
| | `Recip` | Reciprocal / cooperative voice (`-(I)ş`) | *bakıştılar* (`bak:Verb|ış:Recip→Verb+tı:Past+lar:A3pl`) |
| | `Reflex` | Reflexive voice (`-(I)n`) | *kaşındı* (`kaşın:Verb...` / implicit reflexive) |
| | `Able` | Potential / ability verb (`-(y)Abil`, "can") | *okuyabilir* (`oku:Verb|yabil:Able→Verb+ir:Aor+A3sg`) |
| **Verbal Nouns** | `Inf1` | Full infinitive (`-mAk`) | *okumak* (`oku:Verb|mak:Inf1→Noun+A3sg`) |
| | `Inf2` | Short infinitive / verbal noun (`-mA`) | *okuma* (`oku:Verb|ma:Inf2→Noun+A3sg`) |
| | `Inf3` | Manner verbal noun (`-(y)Iş`) | *okuyuş* (`oku:Verb|yuş:Inf3→Noun+A3sg`) |
| | `ActOf` | Activity / game noun (`-mAcA`) | *bulmaca* (`bul:Verb|maca:ActOf→Noun+A3sg`) |
| | `NotState` | Negative state noun (`-mAzlIk` / `-mAmAzlIk`) | *okumazlık* (`oku:Verb|mazlık:NotState→Noun+A3sg`) |
| **Participles** | `PresPart` | Present / subject participle (`-(y)An`) | *okuyan* (`oku:Verb|yan:PresPart→Adj`) |
| | `PastPart` | Past / factive participle (`-dIk`) | *okuduğum* (`oku:Verb|duğ:PastPart→Noun+A3sg+um:P1sg`) |
| | `NarrPart` | Narrative / perfect participle (`-mIş`) | *okumuş* (`oku:Verb|muş:NarrPart→Adj`) |
| | `FutPart` | Future participle (`-(y)AcAk`) | *okuyacağım* (`oku:Verb|yacağ:FutPart→Noun+A3sg+ım:P1sg`) |
| | `AorPart` | Aorist participle (`-(I/A)r` / `-mAz`) | *akarsu* / *okur* (`oku:Verb|r:AorPart→Adj`) |
| | `FeelLike` | Desiderative / worthy participle (`-(y)AsI`) | *öpülesi* (`öp:Verb|ül:Pass→Verb|esi:FeelLike→Adj`) |
| **Converbs (Adverbials)** | `While` | Temporal simultaneous converb (`-ken`, "while") | *okurken* (`oku:Verb+r:Aor|ken:While→Adv`) |
| | `When` | Temporal sequential converb (`-(y)IncA`, "when") | *okuyunca* (`oku:Verb|yunca:When→Adv`) |
| | `SinceDoingSo` | Elapsed-time converb (`-(y)AlI`, "since") | *okuyalı* (`oku:Verb|yalı:SinceDoingSo→Adv`) |
| | `AsLongAs` | Proportional / durational converb (`-dIkçA`) | *okudukça* (`oku:Verb|dukça:AsLongAs→Adv`) |
| | `ByDoingSo` | Manner converb (`-(y)ArAk`, "by doing") | *okuyarak* (`oku:Verb|yarak:ByDoingSo→Adv`) |
| | `Adamantly` | Intensive converb (`-(y)AsIyA`, "to the point of") | *ölesiye* (`öl:Verb|esiye:Adamantly→Adv`) |
| | `AfterDoingSo` | Consecutive converb (`-(y)Ip`, "having done / and") | *okuyup* (`oku:Verb|yup:AfterDoingSo→Adv`) |
| | `WithoutHavingDoneSo` | Negative manner converb (`-mAdAn` / `-mAksIzIn`) | *okumadan* (`oku:Verb|madan:WithoutHavingDoneSo→Adv`) |
| | `WithoutBeingAbleToHaveDoneSo` | Negative potential converb (`-(y)AmAdAn`) | *okuyamadan* (`oku:Verb|yamadan:WithoutBeingAbleToHaveDoneSo→Adv`) |
| | `AsIf` | Equative / similative converb (`-cAsInA`, "as if") | *uçarcasına* (`uç:Verb+ar:Aor|casına:AsIf→Adv`) |
| **Compound Aspectual** | `Hastily` | Swift / sudden action (`-(y)Iver`) | *okuyuverdi* (`oku:Verb|yuver:Hastily→Verb+di:Past+A3sg`) |
| | `EverSince` | Continuative / tradition (`-(y)Agel`) | *süregeldi* (`sür:Verb|egel:EverSince→Verb+di:Past+A3sg`) |
| | `Repeat` | Durative / persistence (`-(y)Adur`, `-(y)Agör`) | *okuyadur* (`oku:Verb|yadur:Repeat→Verb+Imp+A2sg`) |
| | `Almost` | Approximative / near-event (`-(y)Ayaz`, "almost") | *düşeyazdı* (`düş:Verb|eyaz:Almost→Verb+dı:Past+A3sg`) |
| | `Stay` | Frozen / prolonged state (`-(y)Akal`) | *bakakaldı* (`bak:Verb|akal:Stay→Verb+dı:Past+A3sg`) |
| | `Start` | Inceptive / go-ahead action (`-(y)Akoy`) | *gidikoy* / *okuyakoy* (`oku:Verb|yakoy:Start→Verb+Imp+A2sg`) |

For additional background on Oflazer-style Turkish morphological tagsets, see [Chapter 7 of Oflazer et al.](https://research.sabanciuniv.edu/id/eprint/8521/1/3021800000064.pdf).

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
 
### Informal Turkish Words Analysis

As of version 0.16.0, There is a mechanism for analyzing Turkish informal words. For example, word `okuycam`, analysis:

    [okumak:Verb] oku:Verb+yca:Fut_Informal+m:A1sg   

Informal morpheme names (like `Fut_Informal`) have `_Informal` suffix. 

For enabling informal morphological analysis, TurkishMorphology class should be initialized like this:

    TurkishMorphology morphology = TurkishMorphology.builder()        
        .setLexicon(RootLexicon.DEFAULT)
        .useInformalAnalysis()
        .build();

    morphology.analyzeAndDisambiguate("vurucam kırbacı")
        .bestAnalysis()
        .forEach(System.out::println);

Output:

    [vurmak:Verb] vur:Verb+uca:Fut_Informal+m:A1sg
    [kırbaç:Noun] kırbac:Noun+A3sg+ı:P3sg
        
Note that 
ambiguity resolution mechanism may not work well if sentence contains informal morphemes. 
There is also a simple informal to formal conversion mechanism `InformalAnalysisConverter` that
generates formal surface form of an informal word analysis. 

For example lets assume we used the TurkishMorhology instance created in the previous example:

    List<SingleAnalysis> analyses = morphology
            .analyzeAndDisambiguate("okuycam diyo")
            .bestAnalysis();

    for (SingleAnalysis a : analyses) {
      System.out.println(a.surfaceForm() + "-" + a);
    }

    System.out.println("Converting formal surface form:");

    InformalAnalysisConverter converter =
        new InformalAnalysisConverter(morphology.getWordGenerator());

    for (SingleAnalysis a : analyses) {
      System.out.println(converter.convert(a.surfaceForm(), a));
    }

Result will be:

    okuycam-[okumak:Verb] oku:Verb+yca:Fut_Informal+m:A1sg
    diyo-[demek:Verb] di:Verb+yo:Prog1_Informal+A3sg
    
    Converting formal surface form:
    
    okuyacağım-[okumak:Verb] oku:Verb+yacağ:Fut+ım:A1sg
    diyor-[demek:Verb] di:Verb+yor:Prog1+A3sg

Informal Analysis is still experimental and only some cases are covered.

### Diacritics Ignored Analysis

Morphological analysis can be configured to ignore Turkish diacritics marks as used in characters
**[ç,ğ,i,ö,ü,ş]** For that purpose ignoreDiacriticsInAnalysis() method is used. For example:

    TurkishMorphology morphology = TurkishMorphology.builder()        
        .setLexicon(RootLexicon.DEFAULT)
        .ignoreDiacriticsInAnalysis()
        .build();

    morphology.analyze("kisi").forEach(System.out::println);
    
Output will be:    

    [kış:Noun,Time] kış:Noun+A3sg+ı:Acc
    [kış:Noun,Time] kış:Noun+A3sg+ı:P3sg
    [kişi:Noun] kişi:Noun+A3sg

Note that same output will be generated for inputs "kısı, kışi, kişi, kışı" etc.    

## Ambiguity Resolution

Turkish is a highly ambiguous language. As shown in the example below, Word `yarın` has 9 morphological
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
Generation requires root form of a word or a Dictionary item and morphemes. Generation mechanism 
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
          results.forEach(s->System.out.println(s.surface));
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

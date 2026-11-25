CHANGE LOG
==========

## 0.20.0

(November 25th 2026)

Zemberek `0.20.0` features a **comprehensive Turkish lexicon audit and 2020–2026 vocabulary expansion** (with **multiple alternative pronunciations per dictionary entry**), an upgraded default **disambiguation model (`model_15k_v25.bin`)**, and **~1.4x–1.6x faster end-to-end analysis** with **~60% lower heap allocations**.

### Highlights (`0.19.0` → `0.20.0`)

- **Modernized Lexicon & Multi-Pronunciation Support**: Audited dictionary phonetic attributes across all core dictionaries, added 2020–2026 Turkish stems, contemporary entities, and acronyms, and introduced multiple alternative pronunciations per entry (`-7.8%` fewer unparsed `UNK` tokens on the benchmark corpus).
- **Higher Disambiguation Accuracy**: Upgraded the default model to `model_15k_v25.bin`, cutting ambiguous-word errors by **11.8%** (`91.68%` → **`92.67%`**) and lifting full-sentence exact match by **`+3.46%`** (`59.67%` → **`63.13%`**).
- **Faster & Lighter Pipeline**: **+37% to +56%** higher throughput with **-60% to -69%** fewer heap allocations across segmentation, analysis, and disambiguation.

| Metric | `0.19.0` | `0.20.0` | Change |
| :--- | :---: | :---: | :--- |
| **End-to-End Pipeline (Cold Cache)** *(segment → tokenize → analyze → disambiguate)* | ~40.3k tok/s · `899 MB` (`5.3 KB/tok`) | **~55.4k tok/s · `363 MB` (`2.1 KB/tok`)** | **+37% faster (`~1.37x`) · -60% allocs** (`1` → **`0` GC pauses**) |
| **End-to-End Pipeline (Warm Cache)** *(segment → tokenize → analyze → disambiguate)* | ~61.1k tok/s · `550 MB` (`3.3 KB/tok`) | **~95.1k tok/s · `214 MB` (`1.3 KB/tok`)** | **+56% faster (`~1.56x`) · -61% allocs** |
| **End-to-End Pipeline (Uncached Analysis)** | ~29.1k tok/s · `1,899 MB` (`11.3 KB/tok`) | **~44.5k tok/s · `755 MB` (`4.5 KB/tok`)** | **+53% faster (`~1.53x`) · -60% allocs** (`47 ms` → **`8 ms` GC**) |
| **Morphological Analysis Only (Uncached)** | ~48.1k tok/s · `1,497 MB` (`8.9 KB/tok`) | **~71.2k tok/s · `606 MB` (`3.6 KB/tok`)** | **+48% faster (`~1.48x`) · -60% allocs** (`2` → **`0` GC pauses**) |
| **Disambiguation Only (Warm Resolver)** | ~134.3k tok/s · `318 MB` (`1.9 KB/tok`) | **~191.1k tok/s · `98 MB` (`0.6 KB/tok`)** | **+42% faster (`~1.42x`) · -69% allocs** *(30% larger model)* |
| **Retained Live Heap (Idle / Warm Caches)** | `54.1 MB` / `122.0 MB` | **`56.2 MB` / `155.6 MB`** | `+2.1 MB` idle; `+33.6 MB` warm *(larger model & caches)* |
| **Benchmark Corpus Parse Coverage** (`172,792` tokens) | 99.09% (`1,565` UNK) | **99.16%** (`1,443` UNK) | **-7.8%** fewer `UNK` tokens |
| **Disambiguation Accuracy** *(Ambiguous / All / Exact Sent, 1,991 sents)* | 91.68% / 95.30% / 59.67% | **92.67% / 95.86% / 63.13%** | **+0.99% / +0.56% / +3.46%** (**-11.8%** fewer errors) |

> **Benchmark Setup:** Single-threaded on Intel Xeon @ 2.30 GHz (Linux, OpenJDK 21, `-Xms1g -Xmx1g`, median of 3 alternating runs) over the bundled Turkish prose corpus (`Sentence-Boundary-Train.txt` + `Sentence-Boundary-Test.txt`: 2,071 paragraphs, 10,404 sentences, 172,792 tokens). Allocations measured via `ThreadMXBean.getThreadAllocatedBytes`; GC pauses via `GarbageCollectorMXBean`; accuracy evaluated on 1,991 held-out sentences / 23,200 tokens ([#401](https://github.com/ahmetaa/zemberek-nlp/pull/401)).

### What's New & Improved

#### 1. Lexicon, Pronunciations & Phonetics
- **Lexicon Audit & 2020–2026 Vocabulary Expansion** ([#386](https://github.com/ahmetaa/zemberek-nlp/pull/386), [#390](https://github.com/ahmetaa/zemberek-nlp/pull/390), [#391](https://github.com/ahmetaa/zemberek-nlp/pull/391), [#393](https://github.com/ahmetaa/zemberek-nlp/pull/393), [#394](https://github.com/ahmetaa/zemberek-nlp/pull/394)):
  - Audited dictionary metadata and phonetic attributes (voicing, inverse harmony, vowel drop, e.g., *genç*, *federal*, *hapis*) across all core dictionaries, removing corpus ghost entries and adding `LexiconIntegrityTest`.
  - Added modern 2020–2026 common Turkish stems (e.g., *drone*, *biyoyakıt*, *biyobelirteç*, *yönetişim*, *yangınzede*, *aktivist*, *blockchain*, *chatbot*, *fintech*, *powerbank*, *sitcom*).
  - Added contemporary entities, foreign proper nouns with pronunciations, acronyms, and names (e.g., *ChatGPT*, *BioNTech*, *Covid*, *Discord*, *Brexit*, *Gökbey*, *AJet*, *Exxen*, *KOBİ*, *AİHM*, *Ar-Ge*, *ENAG*, *KKM*, *NAVTEX*).
- **Alternative Pronunciations per `DictionaryItem`** ([#391](https://github.com/ahmetaa/zemberek-nlp/pull/391), [#392](https://github.com/ahmetaa/zemberek-nlp/pull/392)):
  - Added support for comma-separated alternative pronunciations so words and acronyms with multiple Turkish readings accept both suffix harmonies (e.g., `Tdk [Pr:tedeka,tedeke]` for *TDK'ya* / *TDK'ye*, `Sgk [Pr:segeka,segeke]`, `Gb [Pr:gigabayt,gebe]`, `Mw [Pr:megavat,meve]`).
- **Phonetic & Stem Transition Fixes** ([#389](https://github.com/ahmetaa/zemberek-nlp/pull/389)):
  - Fixed English-locale stem transitions (e.g., *API'nin*, *UNICEF'in*), hyphen/dot lemmas (e.g., *Ar-Ge*, *Bosna-Hersek*), `-x`/`-og` phonetics, `.00` clock vowel harmony (e.g., *14.00'te*), and `Abbrv` `-lIk`/`P3` transitions.

#### 2. Morphological Disambiguation
- **Upgraded Default Model (`model_15k_v25.bin`)** ([#401](https://github.com/ahmetaa/zemberek-nlp/pull/401)):
  - Replaced `model_10k_argon_v24.bin` (`2.16 MB`, `270k` features) with `model_15k_v25.bin` (`2.80 MB`, `350k` features), trained on `14,935` sentences including `5,000` modern 2020–2026 sentences aligned with the updated lexicon:

  | Evaluation Set | Metric | `0.19.0` | `0.20.0` | Delta |
  | :--- | :--- | :---: | :---: | :--- |
  | Modern Turkish (1,000 sents) | Ambiguous / All Tokens / Exact Sent | 91.96% / 95.44% / 60.70% | **92.73% / 95.88% / 63.20%** | **+0.77% / +0.44% / +2.50%** |
  | General Turkish (991 sents) | Ambiguous / All Tokens / Exact Sent | 91.39% / 95.16% / 58.63% | **92.60% / 95.84% / 63.07%** | **+1.21% / +0.68% / +4.44%** |
  | **Pooled (1,991 sents)** | **Ambiguous / All Tokens / Exact Sent** | 91.68% / 95.30% / 59.67% | **92.67% / 95.86% / 63.13%** | **+0.99% / +0.56% / +3.46%** |
- **Faster, Lower-Allocation Decoding** ([#405](https://github.com/ahmetaa/zemberek-nlp/pull/405)):
  - Eliminated temporary feature-string allocations during model lookup, improved resolver L1 caching, and simplified Viterbi scoring (`+42%` speed, `-69%` allocations).

#### 3. Performance, Memory & Bug Fixes
- **Morphological Search & Stem Lookup** ([#396](https://github.com/ahmetaa/zemberek-nlp/pull/396), [#398](https://github.com/ahmetaa/zemberek-nlp/pull/398), [#406](https://github.com/ahmetaa/zemberek-nlp/pull/406), [#407](https://github.com/ahmetaa/zemberek-nlp/pull/407)):
  - Reworked `SearchPath` to an immutable parent-linked structure with offset slicing, streamlined BFS pruning in `RuleBasedAnalyzer`, unified and hardened `StemTransitionsMapBased` (thread-safe ASCII-tolerant lookups, removed unused trie impl) — **~1.48x** faster uncached analysis with **-60%** fewer allocations.
- **Quote & Apostrophe Normalization** ([#404](https://github.com/ahmetaa/zemberek-nlp/pull/404)):
  - Replaced regex `replaceAll` chains in `TextUtil` with single-pass zero-allocation scans.
- **Numerals, Pronunciations & Analysis Models** ([#395](https://github.com/ahmetaa/zemberek-nlp/pull/395), [#399](https://github.com/ahmetaa/zemberek-nlp/pull/399), [#400](https://github.com/ahmetaa/zemberek-nlp/pull/400), [#402](https://github.com/ahmetaa/zemberek-nlp/pull/402)):
  - Fixed numeral/clock/date/percentage edge cases in `UnidentifiedTokenAnalyzer`, `TurkishNumbers`, `TurkishTextToNumberConverter`, and `PronunciationGuesser`, and cleaned up immutability and allocations across `SingleAnalysis`, `WordAnalysis`, `SentenceAnalysis`, and `AnalysisFormatters`.

#### 4. Architecture & Docs
- **Modularized `TurkishMorphotactics`** ([#397](https://github.com/ahmetaa/zemberek-nlp/pull/397)): Split the 2,607-line class into focused domain modules while preserving `TurkishMorphotactics` as a backwards-compatible facade.
- **Cleanup & Docs** ([#387](https://github.com/ahmetaa/zemberek-nlp/pull/387), [#388](https://github.com/ahmetaa/zemberek-nlp/pull/388)): Simplified `StemTransitionsBase` with Java 21 switch expressions and added Web Demo Studio screenshots to READMEs.

*Assisted by Gemini and Antigravity*


## 0.19.0

(November 18th 2026)

Zemberek `0.19.0` introduces the **Span-Lattice Perceptron Sentence Normalizer**, **Lattice Deasciifier**, **bundled zero-setup normalization models**, an **interactive Web Demo Studio**, **Turkish detokenization**, and **Unicode emoji support**.

#### Normalization & Deasciification
- **Span-Lattice Perceptron Normalizer (`SpanLatticePerceptronNormalizer`)** ([#381](https://github.com/ahmetaa/zemberek-nlp/pull/381), [#383](https://github.com/ahmetaa/zemberek-nlp/pull/383)): Joint Semi-Markov perceptron normalizer over `1:1` rewrites, `1:2` clitic/compound splits, and `2:1`/`3:1` merges with contextual truecasing.
- **Lattice Deasciifier (`LatticeDeasciifier`)** ([#345](https://github.com/ahmetaa/zemberek-nlp/pull/345), [#346](https://github.com/ahmetaa/zemberek-nlp/pull/346), [#367](https://github.com/ahmetaa/zemberek-nlp/pull/367)): Context-aware Turkish diacritic and circumflex restoration (`CircumflexMode`: `NONE`, `DISAMBIGUATION_ONLY` default, `FULL`), enabled by default in `TurkishSentenceNormalizer`.
- **Zero-External-Setup Defaults** ([#383](https://github.com/ahmetaa/zemberek-nlp/pull/383)): `createWithDefaults(...)` on `SpanLatticePerceptronNormalizer`, `TurkishSentenceNormalizer`, and `LatticeDeasciifier` now loads classpath-bundled models automatically.
- **Spell Checker & Sentence Normalizer Improvements** ([#352](https://github.com/ahmetaa/zemberek-nlp/pull/352)–[#379](https://github.com/ahmetaa/zemberek-nlp/pull/379)): Added noisy-channel edit/keyboard/diacritic priors, geminate-preserving elongation normalization, glued number+unit and OOV clitic splitting (`5kg` → `5 kg`), solid TDK compound merging, guards against corrupting valid words/abbreviations/proper nouns, and lower memory footprint via compact `CharacterGraph` arrays and bounded caches.

#### Morphological Analysis & Morphotactics
- **Informal Turkish Morphotactics** ([#351](https://github.com/ahmetaa/zemberek-nlp/pull/351), [#371](https://github.com/ahmetaa/zemberek-nlp/pull/371)): Added progressive y-drop (*gelio*, *gidiorum*), informal future participle (*yapcakları*), y-less negative future (*yapmıcam*), and raised-vowel future (*gidiycem*).
- **Formal Morphotactics & Lexicon Fixes** ([#328](https://github.com/ahmetaa/zemberek-nlp/pull/328)–[#344](https://github.com/ahmetaa/zemberek-nlp/pull/344)): Enforced circumflex matching in standard analysis (`hâlâ` vs `hala`), deduplicated `SingleAnalysis` results, supported optional `LastVowelDrop`, fixed verbal/copular/pronoun/compound/kinship rules, and added proper-noun fallback for capitalized apostrophe stems.
- **Startup & Memory** ([#359](https://github.com/ahmetaa/zemberek-nlp/pull/359), [#363](https://github.com/ahmetaa/zemberek-nlp/pull/363)): Lazy-loaded the default `AmbiguityResolver` in `TurkishMorphology` and right-sized resolver caches.
- **Documentation** ([#336](https://github.com/ahmetaa/zemberek-nlp/pull/336)): Documented all POS tags and inflectional/derivational morphemes in `morphology/README.md`.

#### Tokenization, Apps, gRPC & Core
- **Turkish Detokenizer (`TurkishDetokenizer`)** ([#331](https://github.com/ahmetaa/zemberek-nlp/pull/331)): Added `TurkishTokenizer.detokenize(...)` for natural Turkish spacing around punctuation, quotes, brackets, currencies, and apostrophe suffixes (`"Soner"'e`).
- **Unicode Emoji Support** ([#380](https://github.com/ahmetaa/zemberek-nlp/pull/380)): Recognized and preserved Unicode emojis (including ZWJ sequences and flags) across tokenization, morphology, and normalization.
- **Interactive Web Demo Studio (`StartWebDemo`)** ([#382](https://github.com/ahmetaa/zemberek-nlp/pull/382), [#383](https://github.com/ahmetaa/zemberek-nlp/pull/383)): Built-in local web UI (`http://127.0.0.1:8080`) for inspecting normalization lattices, deasciification, morphology, and tokenization.
- **gRPC & Core** ([#329](https://github.com/ahmetaa/zemberek-nlp/pull/329), [#332](https://github.com/ahmetaa/zemberek-nlp/pull/332), [#378](https://github.com/ahmetaa/zemberek-nlp/pull/378)): Added `--config` / `-c` file support in `StartGrpcServer`, improved `Log` lifecycle handling, and added shared helpers in `TurkishAlphabet` and `Turkish`.

*Assisted by Gemini and Antigravity*


## 0.18.0

(November 10th 2026)

Zemberek `0.18.0` delivers **~4x faster end-to-end processing**, major **disambiguation accuracy** and **morphological coverage** gains, improved **tokenization and sentence segmentation**, and a **Java 21** baseline.

### Highlights (`0.17.1` → `0.18.0`)

| Area | `0.17.1` | `0.18.0` | Change |
| :--- | :---: | :---: | :--- |
| **End-to-End Pipeline Speed** *(segment → tokenize → analyze → disambiguate)* | ~22.0k tok/s | **~88.6k tok/s** | **~4.0x faster** |
| **Disambiguation Accuracy** *(Ambiguous / All Tokens)* | ~77.5% / ~86.8% | **~92.8% / ~95.6%** | **~68% fewer errors**; **~2.7x** exact sentences |
| **Morphological Analysis Speed** *(Cached, cold / warm)* | ~140k–146k tok/s | **~283k–395k tok/s** | **~2.0x–2.7x faster** |
| **Morphological Analysis Speed** *(Uncached)* | ~52.4k tok/s | **~73.6k tok/s** | **~1.4x faster** |
| **Sentence Segmentation / Tokenizer Speed** | ~184k sents/s / ~3.5M tok/s | **~255k sents/s / ~4.1M tok/s** | **~1.4x / ~1.2x faster** |

> **Benchmark Setup:** Single-threaded on AMD Ryzen 7 1700X (Linux, OpenJDK 25, `-Xmx8g`) over a 100k-paragraph news corpus (`3.69M` tokens, `211k` sentences); accuracy measured on 1,000 held-out sentences (`6,587` ambiguous tokens).

#### What's New & Improved
- **Morphological Disambiguation (`FastPerceptronAmbiguityResolver`)**: Replaced the legacy disambiguator with exact Viterbi decoding and a compact `2.16 MB` default model (`model_10k_argon_v24.bin`), plus `FastPerceptronAmbiguityResolverTrainer` (`analyzeAndDisambiguate`: `~22.2k` → `~90.0k tok/s`, `~4.0x` faster).
- **Morphological Analysis & Caching**: Added support for `deyip`/`yiyip`, `-mada/-yışta`+copula, derived abbreviations (`ABD'li`), multi-apostrophe words (`Kur'an'da`), and alphanumeric/domain stems (`3G'ye`, `meb.gov.tr'den`), cutting `UNK` tokens by **45%** (`97.95%` → `98.87%` coverage). Unified analysis caching into a single 50k thread-safe Caffeine cache.
- **Sentence Segmentation & Tokenization**: Retrained `sentence-boundary-model.bin` with ellipsis (`…`) support and fixed false vetoes; improved `TurkishTokenizer` handling of single-letter apostrophes (`O'na`), initials (`M. Kemal`), acronyms, URLs/emails, and currency symbols while reducing allocations.
- **Core, Build & Dependencies**: Hardened primitive collections in `zemberek-core`, moved baseline from Java 8 to **Java 21**, upgraded core dependencies (`Guava 33.7.1`, `Protobuf 4.36.2`, `Caffeine 3.2.0`, `gRPC 1.84.0`, `OpenNLP 2.5.12`), and removed the deprecated `server` module.

*Assisted by Gemini and Antigravity*


## 0.17.1

This is a minor release with bug fixes.
Running zemberek-full.jar now lists available applications correctly. 

## 0.17.0 

(April 17th 2019)

This is a small release with breaking changes.

### New features

Tokenization module now can be configured to ignore text in double quotes.

Zemberek now uses it's own Token class instead of using Antlr's Token class. This is 
a breaking change.

Behavior of two methods and add two methods in WordAnalysisSurfaceFormatter are changed. Now if apostrophe is provided, it will use it even in regular words.

Model compression for NER models is added.

Add Builder mechanism to TurkishSentenceExtractor.

TurkishTextToNumberConverter is now thread safe and stateless.

There is a NER postprocessor NEPostProcessor (Provided by Ayça Müge Sevinç).

Use shade plugin so that when using single jar library, dependent libraries will not conflict with ohter versions.

### Deprecations and breaking changes

`RootLexicon.DEFAULT` is now inaccessible. Use `RootLexicon.getDefault()` for accessing
default lexicon.

`RootLexicon.builder().addDefaultLexicon()` is renamed as `RootLexicon.builder().addDefaultLexicon()`

Zemberek now uses it's own Token class instead of using Antlr's Token class.

normalizedLemma() in DictionaryItem now removes -mek -mak suffixes and converts to lowercase.

Some method names in TurkishAlphabet is changed. Such as
asciiTolerantEquals -> equalsIgnoreDiacritics and
asciiTolerantStartsWith -> startsWithIgnoreDiacritics

### Notable Bug Fixes

A possible small memory leak caused by analysis of unidentified tokens is fixed. 

We thank everybody for their contributions.
Special thanks to Müge for her feedback on morphology and NER modules.


## 0.16.0 

(October 29th 2018)

This is a major release with breaking changes and new features.

### New features 

#### grpc module
Initial release of [**grpc**](grpc) remote procedure call module. This is an experimental module that allows fast access to some 
functions of the project from other programming languages. We provide initial Python access codes for experimentation.
Remote API is also subject to change until Version 1.0.0. Refer to the [documentation](grpc) for more information. 

#### Noisy Text Normalization

Now there is a sentence normalization functionality. Before this, [normalization](normalization) 
module only provided simple 1 distance word based spell check suggestion mechanism. Now. system offers a
best effort text normalization functionality. This may be useful for pre-processing noisy text inputs
before applying other functions.  

Candidate correct words for noisy words are collected using several heuristics, 
informal morphotactics, distance matching and lookup tables that generated with
 an offline contextual graph random walk algorithm. After that, best correct sequence is found
  with Viterbi search on candidate words using n-gram language model scores. 

Note that this is our first attempt, expect many errors. 

#### Informal Turkish Words Analysis
We introduce a mechanism for analyzing Turkish informal words. For example, word `okuycam`, analysis
may be:

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

Ambiguity resolution mechanism may not work well if sentence contains informal morphemes. 
There is also a simple informal to formal conversion mechanism `InformalAnalysisConverter` that
generates formal surface form of an informal word analysis. 

#### Diacritics Ignored Analysis

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

Same output will be generated for inputs "kısı, kışi, kişi, kışı" etc.    

#### New command line applications

There are several new command line applications.
* `GenerateWordVectors`: Generates word vectors using a text corpus. 
* `StartGrpcServer`: Starts Zemberek gRPC Server.
* `TrainNerModel`: Generates Turkish Named Entity Recognition model.
* `EvaluateNer`: Evaluates an annotated NER data set.
* `FindNamedEntities`: Finds named entities from a Turkish text file.

### Deprecations and breaking changes

* Most lexicon building methods in TurkishMorphology is now moved to RootLexicon's Builder mechanism.
We did not go through a deprecation stage for this because there were too many changes.
`RootLexicon.DEFAULT` is now contains default dictionary items. So if user wants to create a custom 
dictionary and add it to default, or remove items during instantiation, RootLexicon builder mechanism
needs to be used. Example:

      RootLexicon myLexicon = RootLexicon.builder()
          .setLexicon(RootLexicon.DEFAULT) // start with default
          .addDictionaryLines("foo", "rar") // add two new nouns
          .addTextDictionaries(Paths.get("my-own-dictionary-file")) // add from file
          .build();

* `InterpretingAnalyzer` is now `RuleBasedAnalyzer`

* `locations-tr.dict` (contains mostly village and district names) is removed from default 
binary dictionary because it was causing a lot of confusion. Users can add it manually.

* Instead of using Ability+Negative suffix couple, there is now a new morpheme called `Unable`.
 New suffix does not cause a Verb to Verb derivation. For example, for word `okuyamadım`: 

      Before: oku:Verb|ya:Abil→Verb+ma:Neg+dı:Past+m:A1sg
      After : oku:Verb+yama:Unable+dı:Past+m:A1sg
      
* Deprecated createWithTextDictionaries() method in TurkishMorphology is now removed.

### Notable Bug fixes

[#188](https://github.com/ahmetaa/zemberek-nlp/issues/188) Cannot analyze sendeki, bendeki etc

[#184](https://github.com/ahmetaa/zemberek-nlp/issues/184) Cannot analyze `abimsin` or any Noun+..+P1sg+..+Verb+..+A2sg

[#183](https://github.com/ahmetaa/zemberek-nlp/issues/183) Cannot analyze "Tübitak'a"

[#178](https://github.com/ahmetaa/zemberek-nlp/issues/178) Anlaysis fails with `herkeste, gibime, gibimize`

[#175](https://github.com/ahmetaa/zemberek-nlp/issues/175) Lemmatization may give incorrect results in Zero morpheme derivations

[#174](https://github.com/ahmetaa/zemberek-nlp/issues/174) Add ®™©℠symbols as punctuation.

[#167](https://github.com/ahmetaa/zemberek-nlp/issues/167) Redundant Adj -> Noun -> Noun conversion

[#170](https://github.com/ahmetaa/zemberek-nlp/issues/170) Justlike morpheme should not appear in some cases.

[#171](https://github.com/ahmetaa/zemberek-nlp/issues/171) Cannot analyze "kendimle"

[#172](https://github.com/ahmetaa/zemberek-nlp/issues/172) Cannot analyze "kendimde, kendimden, kendimce"

[#173](https://github.com/ahmetaa/zemberek-nlp/issues/173) "gelebilme" should not have an analysis with "Neg"

We thank everybody for their contributions.
Special thanks to Müge for finding numerous morphology bugs and @bojie for 
fixing Language Model compression problem.

## 0.15.0 

August 2nd 2018

#### New features 

Initial release of [**classification**](classification) module. Classification module is based on our Java port of [fastText](https://fasttext.cc/) project.
Refer to the [documentation](classification) for more information. There are two usage examples in 
[examples](https://github.com/ahmetaa/zemberek-nlp/tree/master/examples/src/main/java/zemberek/examples/classification) module.
There is also a Turkish [wiki page](https://github.com/ahmetaa/zemberek-nlp/wiki/Zemberek-NLP-ile-Metin-S%C4%B1n%C4%B1fland%C4%B1rma)
 on generating and evaluating classifier for Turkish news headline categories. 

There is now a single jar containing all dependencies named `zemberek-full.jar`. 

Initial release of **apps** module. All console applications will be in this module. When 
zemberek-full.jar is run directly, a catalogue of available applications are listed. They 
can be run directly bt the class name without the package name. For example:

     java -jar zemberek-full.jar TrainClassifier
     
will run the TrainFastTextClassifier application and list available parameters.

OpenNlp NER annotation style is added.

`toAscii` method is added to TurkishAlphabet

#### Bug fixes

Common foreign words with diacritic letters (like `José`) will not generate an exception warning.

Tokenizer will handle ellipsis `…` character correctly.

#### Deprecations and breaking changes

`createWithTextDictionaries()` method in TurkishMorphology and `addDefaultDictionaries()` in 
 TurkishMorphology.Builder are deprecated.
 
 `parseEntries()` in SentenceAnalysis is deprecated.
 
 Deprecated `analyzeAndResolveAmbiguity(String sentence)` in TurkishMorphology is removed.

#### Documentation
 
 [Dictionary Rules](https://github.com/ahmetaa/zemberek-nlp/wiki/Text-Dictionary-Rules) and
 [Morphemes](https://github.com/ahmetaa/zemberek-nlp/wiki/Morphemes) wiki pages are added. 

#### Experimental

There is also word vector generation functionality but it is not yet documented.

There are more grpc services available but module is not yet ready for release.       

## 0.14.0

This is a major release with improved Morphological disambiguation and initial release of NER module.

- Morphological Disambiguation is improved and now became more usable.  

- There is now a Named Entity Recognition (NER) module for Turkish. 
This is our initial implementation and no pretrained NER model is provided yet. 
Users can train their own models. Please refer to the [documentation](ner) for details. 

- Around 12 thousands lines of code is removed from project. Previous morphology and disambiguation code is gone.   

- Behavior for inputs proper nouns with apostrophe is changed. For example there will no longer be
an analysis with root `oba` for input `Obama'ydı`. However handling proper nouns with suffixes and apostrophes is 
still under development.

- After some cleaning, person names dictionary is added. 

- "…" is now considered a sentence boundary character. 

- Morphotactics improvements.

- A Grpc server module is under development. Remote server functions can be accessed with multiple client libraries (Python, C#, Javascript etc.).
Initial version will probably be available in the next release.

Breaking Changes: 
- `analysis` parameter in SentenceWordAnalysis class is now `bestAnalysis`.
- For consistency, `analyzeAndResolveAmbiguity` method in `TurkishMorphology` is changed to `analyzeAndDisambiguate`.
  Old method still works but deprecated.
- `addDefaultDictionaries()` in `TurkishNMorphology.Builder` is deprecated. Use `addDefaultBinaryDictionary` instead.
- Maven distibutions now includes sources (via Ali Ok)

## 0.13.0

This is a bug fix and small changes release. 

- A problem in ambiguity resolution is fixed. It was throwing NPE for unknown words. (Issue #157)

- Abbreviations are not allowed to have possessive suffixes.

- Morphotactics improvements.

- We started experimenting with server side modules.

## 0.12.0 

This release is the result of some major refactoring of the Morphology module.
There are many breaking changes.

Maven artifact id names have now `zemberek-` prefix.

Morphology module is re-written almost from scratch. Turkish morphotactics are now expressed in a simpler and more readable way
in the code. New analyzer handles pronouns better and probably it generates
more accurate results. But because this is a complete re-write, there might be new bugs and regressions.

Ambiguity resolution mechanism is changed. It now uses the old but popular Averaged Perceptron algorithm.
For now, it is trained with the data generated from some corpora using simple rules.
Therefore in this version disambiguation may not work so accurately. But it will improve in the upcoming releases quickly.
Nevertheless, new module is probably working better than previous releases.
Previous language model based algorithm is retired for now, but in future we may use a hybrid approach.

Default analysis representation is changed. Some examples:
    
    kitap ("book", Noun, Singular.)
    [kitap:Noun] kitap:Noun+A3sg

    kitabımda ("in my book"  Noun, Singular, First person possession, Locative)
    [kitap:Noun] kitab:Noun+A3sg+ım:P1sg+da:Loc

    dedim ("I told" Verb, past tense, first person singular)
    [demek:Verb] de:Verb+di:Past+m:P1sg

    diyerek ("By telling" Verb, derived to an adverb)
    [demek:Verb] di:Verb|yerek:ByDoingSo→Adv
    
We decided to omit displaying implicit `Pnon` and `Nom` suffixes from nouns to make it more readable.
This format is probably not final. We consider changing some morpheme names and refine the representation.

We now use Caffeine for caching analysis results. There are static and dynamic caches for speeding up the word analysis. 

Word generation mechanism is also re-written.

Dictionary serialization mechanism is written using protocol-buffers. 
Now initialization of `TurkishMorphology` class is faster.

There are Email, Url, Mention, HashTag, Emoticon, RomanNumeral, RegularAbbreviation, Abbreviation 
secondary POS information.  

We added examples module. This is like a copy of `turkis-nlp-examples` project. Users can see 
high level usage examples there.  

FAQ and front page Readme files are updated.

#### Breaking changes

Z3AbstractDisambiguator, TurkishMorphDisambiguator, Z3AbstractDisambiguator, Z3MarkovModelDisambiguator,
Z3ModelA removed from morphology modue.

TurkishSuffixes, TurkishSentenceAnalyzer, WordAnalyzer, SimpleGenerator, DynamicLexiconGraph,
DynamicSuffixProvider, SuffixData, SuffixSurfaceNode, StemNode, StemNodeGenerator,
Suffix, SuffixForm, SuffixProvider, SuffixSurfaceNodeGenerator  are removed from morphology modue.

TurkishMorphology analysis methods now return `WordAnalysis` object instead of `List<WordAnalysis>`
. `WordAnalysis` contains a `List<SingleAnalysis>` where analysis details can be reached. Methods like
`getStem()` or `getLemmas()` are moved from `WordAnalysis` to `SingleAnalysis`.

Generation is now handled by `WordGenerator` class. generation rules are changed so that if user does not
provide empty surface morphemes, system search through them anyway. Check `GenerateWords` example class.

#### Performance and memory footprint
System memory footprint is reduced. Analysis performance may be a bit slower but with cache, impact should be 
small. We will provide measurements later.   

#### Work that has not made this release

We wrote a port of Facebook's FastText library in Java. It can be used for word embeddings and 
classification tasks. However it is not yet ready for release. 

There is an experimental Named Entity Recognition module. But it is not yet ready for release.

## 0.11.1

TurkishSpellChecker now can load internal resources correctly.

## 0.11.0

#### Tokenization
We made a lot of changes in Tokenization module. Some of them are breaking changes.

Package name is changed from **zemberek.tokenizer** to **zemberek.tokenization** for consistency.

Now there is a better sentence extraction class called **TurkishSentenceExtractor**. This class
can split documents and paragraphs into sentences. It uses rules and a simple binary averaged perceptron algorithm for finding sentence boundaries.
 **LexerSentenceExtractor** is removed.

Tokenization is also improved. More token types are introduced. Including Date, Hashtag, Mention, URL, Email and Emoticon.
 However these tokens are not included as a POS type in morphological analysis yet. This will be done in upcoming versions.
 We changed the **ZemberekTokenizer** name to **TurkishTokenizer**. 
 There are some low-level breaking changes. Token TurkishWord is now Word
 and TurkishWordWithApos is WordWithApostrophe.
 
Please refer to [documentation](tokenization) for both sentence extraction and tokenization usage examples and test results.

#### Normalization
Zemberek now includes an alpha level spell-checker. This spell checker is intended for well formed documents. 
 It is not for automatic spell correction / normalization. Basically it can check if an individual word is 
  written correctly and give suggestions for a word. Suggestions are ranked against a small unigram language model for now.
  We will improve the spell checker in later versions.
  
  Plase refer to  [documentation](normalization) for usage examples.
  
#### Morphology
There are no big changes in morphology in this version. However several addition and fixes were made in dictionaries.
Including around 100 more Adverbs and several Question type fixes. Suffix morphotactics for Pronouns are still mostly broken.
 Please refer to these commits for changes: [1](https://github.com/ahmetaa/zemberek-nlp/commit/b67776054a5eec35be3f6b32c9bdb6fa83bc1d65)
  [2](https://github.com/ahmetaa/zemberek-nlp/commit/0810dedfebe6bf2af6498838af9a91fe37f059cb)
  [3](https://github.com/ahmetaa/zemberek-nlp/commit/230c5d6a32de8389438356606ca3d8b094f40553)
  [4](https://github.com/ahmetaa/zemberek-nlp/commit/231f7da2919cd9556b62ab454e19ea870b7d8fe0)
  [5](https://github.com/ahmetaa/zemberek-nlp/commit/465ce3c2b71c5f2dd57d1c6cb68bfdd9847a22fe) 

There was yet another memory leak fixed on use of HashMaps in RootLexicon. 

We have started a new morphotactics work in experiment module. But it is not yet usable.

#### Hyphenathion
 This module is removed and content is moved to **core** module for now. 
 It was not holding much weight and code requires an overhaul. Before 1.0.0 we may move it to normalization module. 

## 0.10.0

- Fixed a memory leak. Previous versions may suffer from this under heavy load. [87](https://github.com/ahmetaa/zemberek-nlp/issues/87)
- Re-introduce lang-id module. This provides a simple language identification API.
- Added normalization module. So far it only provides fast Levenshtein distance dictionary matching.
- Added an experiment module. This module will be used for experimental features.
- Dictionary fixes.
- Added city, village and district names.
- System can generate full jar containing all zemberek modules.
- Speed up Antlr based tokenizer. Now it is three times faster again. [89](https://github.com/ahmetaa/zemberek-nlp/issues/89)
- TurkishMorphology can be configured for not using cache and UnidentifiedTokenAnaysis.
- Eliminate static cache from TurkishMorphology [86](https://github.com/ahmetaa/zemberek-nlp/issues/86)
- Fix: Some inputs may cause excessive hypothesis generation during analysis [88](https://github.com/ahmetaa/zemberek-nlp/issues/88)
- Fix: Proper Nouns ending -nk or -og should not have Voicing attribute automatically.[83](https://github.com/ahmetaa/zemberek-nlp/issues/83)
- Fix: "foo \nabc" should be tokenized as "foo \n abc" [69](https://github.com/ahmetaa/zemberek-nlp/issues/83)
- There are some name changes.
  TurkishMorphology.TurkishMorphParserBuilder -> TurkishMorphology.Builder
  UnidentifiedTokenAnalyzer parse -> analyze  

## 0.9.3

- Improved morphological analysis coverage by cross checking with Oflazer-Analyzer. For this, a list of more than 7 million words are extracted from a 2 billion word corpora. Then a list of words that can be analyzed only by Oflazer-Analyzer is generated and Zemberek is fixed as much possible.  
- Breaking change: zemberek.morphology.parse package is now zemberek.morphology.analysis [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Breaking change: Several classes are renamed.  
   TurkishWordParserGenerator -> TurkishMorphology  
   SentenceMorphParse -> SentenceAnalysis  
   MorphParse -> WordAnalysis  
   WordParser -> WordAnalyzer [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Breaking change: Methods with name "parse" are renamed to "analyze". [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Custom Antlr dependency is removed. We now use latest stable Antlr version. We decided this because maintaining a patched version of Antlr was time consuming. We were using such a fork because original version was around 3 times slower. But current speed is good enough. We may remove Antlr dependency altogether in future releases because tokenization should be less strict and it should not do detailed classification. [68] (https://github.com/ahmetaa/zemberek-nlp/issues/68)
- Add Oflazer compatible secondary POS information for Postp.  [65](https://github.com/ahmetaa/zemberek-nlp/issues/65)
- Tokenization problem after capital letters after apostrophe. [64](https://github.com/ahmetaa/zemberek-nlp/issues/64)
- Cannot parse diyebil-, diyecek-, diyen-. [61](https://github.com/ahmetaa/zemberek-nlp/issues/61)
- Proper nouns should not have Voicing attribute automatically. [57](https://github.com/ahmetaa/zemberek-nlp/issues/57)
- Can parse reçelsi but not reçelimsi. [54](https://github.com/ahmetaa/zemberek-nlp/issues/54)
- Cannot parse maviceydi, yeşilcedir. [53](https://github.com/ahmetaa/zemberek-nlp/issues/53)
- Cannot parse "soyadları" [55](https://github.com/ahmetaa/zemberek-nlp/issues/55)
- Wrong start and stop indexes for abbreviation words on tokenization. [51](https://github.com/ahmetaa/zemberek-nlp/issues/51)
- Fixes in caching mechanism in TurkishMorphology.
- Added a dependency module. It does not perform parsing yet.

## 0.9.2

A lot of internal code changes. Added static and dynamic cache mechanisms for word parsing.

### Some Issues Fixed:
- Can parse [abdye ABDye] but not [abd'ye] [ABD'ye] #44
- Cannot parse words : [ cevaplandırmak çeşitlendirmek ] #42
- System can parse [ankaraya] but not [ankara'ya] #40
- Add ability to add a new Dictionary Item in run-time. #37
- resource test-lexicon-nouns.txt not found #36 (elifkus)
- Garip bir tokenization and stem problemi #30
- Cannot parse the word: yiyen #25 (volkanagun)

## 0.9.0

- First unstable public release.
- Removed language identification and spelling modules. They are different applications now.


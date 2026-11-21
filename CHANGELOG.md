CHANGE LOG
==========

## 0.19.0

(November 18th 2026)

Zemberek `0.19.0` introduces a new **Span-Lattice Perceptron Sentence Normalizer (`v2.4`)**, a **Morphological Candidate Lattice Deasciifier (`LatticeDeasciifier`)**, **bundled runtime normalization and deasciification models**, an **interactive Web Demo Studio (`StartWebDemo`)**, **detokenization and Unicode emoji support**, and a wide range of **morphological analysis, normalization, spell-checking, and gRPC server improvements**.

### Highlights at a Glance

- **Span-Lattice Perceptron Normalizer (`SpanLatticePerceptronNormalizer`)**: A new joint Semi-Markov Averaged Perceptron sentence normalizer operating over a multi-span candidate DAG (`1:1` rewrites, `1:2` clitic/compound splits, and `2:1`/`3:1` merges) with bundled models (`knowledge.bin.gz`, `perceptron.bin.gz`, `lm.2gram.slm.gz`).
- **Lattice Deasciifier (`LatticeDeasciifier`)**: A new context-aware Turkish diacritic restoration engine combining a morphological candidate lattice, character harmony model, trigram Viterbi beam search, and bidirectional SOV refinement—now enabled by default in `TurkishSentenceNormalizer`.
- **Zero-External-Setup Normalization**: `SpanLatticePerceptronNormalizer.createWithDefaults(...)`, `TurkishSentenceNormalizer.createWithDefaults(...)`, and `LatticeDeasciifier.createWithDefaults(...)` now work out of the box using classpath-bundled resources.
- **Interactive Web Demo Studio (`StartWebDemo` / `ZemberekDemoServer`)**: A built-in local web UI (`http://127.0.0.1:8080`) for side-by-side inspection of sentence normalization lattices, deasciification candidates, morphological analysis & disambiguation, and tokenization.
- **Turkish Detokenizer & Emoji Support**: Added `TurkishDetokenizer` (`TurkishTokenizer.detokenize(...)`) and full Unicode emoji tokenization/preservation across tokenization, morphology, and normalization.

### What's New & Improved

#### 1. Text Normalization & Deasciification
- **New `SpanLatticePerceptronNormalizer` (`zemberek.normalization.v2`)** ([#381](https://github.com/ahmetaa/zemberek-nlp/pull/381), [#383](https://github.com/ahmetaa/zemberek-nlp/pull/383)):
  - Scores `1:1` token rewrites, `1:2` clitic/number-unit/compound splits, and `2:1`/`3:1` open-compound/suffix merges jointly using 20 continuous language-model/channel features and 30 hashed sparse feature templates stored in a compact `QuantizedWeightTable`.
  - Includes `CompactStemEndingSpellChecker`, `NormalizationKnowledge`, `ShorthandExpander`, `SingleTokenRewriteGenerator`, `SplitMergeSpanGenerator`, and contextual truecasing.
  - Bundled runtime models (`knowledge.bin.gz`, `perceptron.bin.gz`, and `lm.2gram.slm.gz`) allow instant initialization via `SpanLatticePerceptronNormalizer.createWithDefaults(morphology)` without external model directories.
- **New `LatticeDeasciifier` (`zemberek.normalization.deasciifier.latticedeasciifier`)** ([#345](https://github.com/ahmetaa/zemberek-nlp/pull/345), [#346](https://github.com/ahmetaa/zemberek-nlp/pull/346), [#367](https://github.com/ahmetaa/zemberek-nlp/pull/367)):
  - Restores Turkish diacritics (`ç, ğ, ı, İ, ö, ş, ü` and circumflexes `â, î, û`) using a 3-stage pipeline: morphological/lexicon/character-harmony candidate generation, pass-1 trigram beam search with agglutinative stem lookahead, and pass-2 bidirectional refinement.
  - Added `CircumflexMode` (`NONE`, `DISAMBIGUATION_ONLY` default for minimal pairs like *kâr/kar* and *hâlâ/hala*, and `FULL`).
  - Enabled by default in `TurkishSentenceNormalizer` (configurable via `useLatticeDeasciifier()`, `useLegacyDeasciifier()`, or `setLatticeDeasciifier(...)`).
- **Upgraded `TurkishSentenceNormalizer` & `TurkishSpellChecker`**:
  - Decomposed `TurkishSentenceNormalizer` into focused pipeline components: `NormalizerPreprocessor`, `InformalContractionExpander`, `ElongationNormalizer`, `FormalSurfaceRealizer`, and `NgramSequenceDecoder` ([#377](https://github.com/ahmetaa/zemberek-nlp/pull/377), [#378](https://github.com/ahmetaa/zemberek-nlp/pull/378), [#379](https://github.com/ahmetaa/zemberek-nlp/pull/379)).
  - Added noisy-channel edit-operation costs, keyboard adjacency priors, and diacritic penalties to `TurkishSpellChecker` and `TurkishSentenceNormalizer` ([#354](https://github.com/ahmetaa/zemberek-nlp/pull/354)).
  - Added geminate-preserving elongation normalization (preserving valid double letters like *saat*, *hisse*, *madde* while collapsing *çoooook* → *çok*) and detached numeral suffix attachment ([#352](https://github.com/ahmetaa/zemberek-nlp/pull/352), [#374](https://github.com/ahmetaa/zemberek-nlp/pull/374)).
  - Added colloquial vowel-drop, `ne`+verb contraction, and `[A:Ext]`-masked open-compound expansion ([#355](https://github.com/ahmetaa/zemberek-nlp/pull/355), [#375](https://github.com/ahmetaa/zemberek-nlp/pull/375), [#376](https://github.com/ahmetaa/zemberek-nlp/pull/376)).
  - Added morphotactic splitting for glued number+unit tokens (`5kg` → `5 kg`, `100tl` → `100 tl`) and OOV clitics (`de/da`, `mi/mı/mu/mü`, `ki`) ([#373](https://github.com/ahmetaa/zemberek-nlp/pull/373)).
  - Fixed solid TDK compound merging (e.g., *uluslar arası* → *uluslararası*) while preventing false word fusions ([#245](https://github.com/ahmetaa/zemberek-nlp/issues/245), [#333](https://github.com/ahmetaa/zemberek-nlp/pull/333), [#374](https://github.com/ahmetaa/zemberek-nlp/pull/374)).
  - Guarded capitalized OOV words, valid Turkish words, abbreviations, and numbers from spell-check/deasciifier corruption ([#228](https://github.com/ahmetaa/zemberek-nlp/issues/228), [#233](https://github.com/ahmetaa/zemberek-nlp/issues/233), [#237](https://github.com/ahmetaa/zemberek-nlp/issues/237), [#309](https://github.com/ahmetaa/zemberek-nlp/issues/309), [#337](https://github.com/ahmetaa/zemberek-nlp/pull/337)–[#340](https://github.com/ahmetaa/zemberek-nlp/pull/340), [#347](https://github.com/ahmetaa/zemberek-nlp/pull/347)–[#349](https://github.com/ahmetaa/zemberek-nlp/pull/349), [#366](https://github.com/ahmetaa/zemberek-nlp/pull/366), [#370](https://github.com/ahmetaa/zemberek-nlp/pull/370), [#372](https://github.com/ahmetaa/zemberek-nlp/pull/372)).
  - Replaced per-node `UIntMap` in `CharacterGraph` with compact binary-searched `Node[]` arrays and bounded normalizer caches for lower memory footprint ([#360](https://github.com/ahmetaa/zemberek-nlp/pull/360), [#361](https://github.com/ahmetaa/zemberek-nlp/pull/361), [#363](https://github.com/ahmetaa/zemberek-nlp/pull/363)).

#### 2. Morphological Analysis & Morphotactics
- **Informal Turkish Morphotactics (`InformalTurkishMorphotactics`)** ([#351](https://github.com/ahmetaa/zemberek-nlp/pull/351), [#371](https://github.com/ahmetaa/zemberek-nlp/pull/371)):
  - Added support for progressive y-drop (`-Io`/`-Ior`, e.g., *gelio*, *gidiorum*), informal future participle (`FutPart_Informal`, e.g., *yapcakları*), y-less negative future (*yapmıcam*, *gelmicem*), and raised-vowel future forms (*gidiycem*, *okuycam*).
- **Formal Morphotactics & Lexicon Fixes**:
  - Enforced circumflex (`â, î, û`) matching in standard morphological analysis so circumflex inputs like `hâlâ` or `kâra` do not spuriously match non-circumflex roots (`hala`, `kar`, `kara`) while preserving full candidates when `ignoreDiacriticsInAnalysis` is enabled ([#111](https://github.com/ahmetaa/zemberek-nlp/issues/111), [#330](https://github.com/ahmetaa/zemberek-nlp/pull/330)).
  - Deduplicated identical `SingleAnalysis` results and added native support for stems with optional `LastVowelDrop` without duplicate dictionary entries ([#81](https://github.com/ahmetaa/zemberek-nlp/issues/81), [#335](https://github.com/ahmetaa/zemberek-nlp/pull/335), [#342](https://github.com/ahmetaa/zemberek-nlp/pull/342)).
  - Fixed verbal, copular, and participle morphotactic transitions: blocked spurious double `A3pl` across copula (*yazacaklardırlar*), duplicate `NarrPart` on `-mışken`, and invalid `FeelLike` transitions, while adding support for oblique plural predicates with `Past`/`Narr`/`Cond`/`While` (*evdelerdi*, *neredelermiş*) and negative aorist participle + ablative (*görmezden*, *bilmezden*) ([#328](https://github.com/ahmetaa/zemberek-nlp/pull/328)).
  - Fixed pronoun morphotactics, `P2pl` on interrogative pronouns (*neyiniz*, *kiminiz*), genitive `-ki` (`benimki`, `benimkiler`), and pronoun lexicon entries (*falan*, *falanca*, *beriki*, and removed spurious adjective entries for *o*/*şu*) ([#140](https://github.com/ahmetaa/zemberek-nlp/issues/140), [#343](https://github.com/ahmetaa/zemberek-nlp/pull/343)).
  - Fixed `CompoundP3sg` on *camii*/*sanayii*/*bayii*, `FamilyMember` attributes on kinship plurals ([#149](https://github.com/ahmetaa/zemberek-nlp/issues/149), [#344](https://github.com/ahmetaa/zemberek-nlp/pull/344)), removed the erroneous `Reciprocal` attribute from *karşılaşmak* ([#196](https://github.com/ahmetaa/zemberek-nlp/issues/196), [#334](https://github.com/ahmetaa/zemberek-nlp/pull/334)), and added proper-noun fallback analysis for capitalized apostrophe stems ([#309](https://github.com/ahmetaa/zemberek-nlp/issues/309), [#341](https://github.com/ahmetaa/zemberek-nlp/pull/341)).
- **Memory & Startup Optimizations** ([#359](https://github.com/ahmetaa/zemberek-nlp/pull/359), [#363](https://github.com/ahmetaa/zemberek-nlp/pull/363)):
  - Lazy-loaded the default `AmbiguityResolver` in `TurkishMorphology` (avoiding model load when only word analysis is used) and sized `FastPerceptronAmbiguityResolver` caches to avoid G1GC humongous allocations.
- **Documentation** ([#281](https://github.com/ahmetaa/zemberek-nlp/issues/281), [#336](https://github.com/ahmetaa/zemberek-nlp/pull/336)):
  - Documented all primary/secondary POS tags, inflectional morphemes, and derivational morphemes with examples in `morphology/README.md`.

#### 3. Tokenization & Detokenization
- **Turkish Detokenizer (`TurkishDetokenizer`)** ([#181](https://github.com/ahmetaa/zemberek-nlp/issues/181), [#331](https://github.com/ahmetaa/zemberek-nlp/pull/331)):
  - Added `TurkishDetokenizer` (`TurkishDetokenizer.DEFAULT` and `TurkishTokenizer.detokenize(...)` / `detokenizeTokens(...)`) to reconstruct natural Turkish spacing around punctuation, quotes, brackets, currencies, percentages, slashes, and apostrophe suffixes after closing quotes/brackets (e.g., `"Soner"'e`, `(2020)'de`).
- **Unicode Emoji Support** ([#380](https://github.com/ahmetaa/zemberek-nlp/pull/380)):
  - Updated `TurkishLexer` and `TurkishTokenizer` to recognize Unicode emojis (including surrogate pairs, variation selectors, keycaps, regional indicator flags, and ZWJ sequences) as `Token.Type.Emoticon`, and ensured `UnidentifiedTokenAnalyzer` and `TurkishSentenceNormalizer` preserve them cleanly.

#### 4. Applications, Web Demo Studio, gRPC & Core
- **Interactive Web Demo Studio (`StartWebDemo`, `ZemberekDemoServer`)** ([#382](https://github.com/ahmetaa/zemberek-nlp/pull/382), [#383](https://github.com/ahmetaa/zemberek-nlp/pull/383)):
  - Added `StartWebDemo` console application (`java -jar zemberek-full.jar StartWebDemo` or `./apps/run_demo.sh`) serving an interactive web interface at `http://127.0.0.1:8080` to test and inspect normalization lattices, deasciification candidates, morphological analysis/disambiguation, and tokenization.
- **gRPC Server Configuration (`StartGrpcServer`, `ZemberekGrpcConfiguration`)** ([#190](https://github.com/ahmetaa/zemberek-nlp/issues/190), [#332](https://github.com/ahmetaa/zemberek-nlp/pull/332)):
  - Added `--config` / `-c` configuration file support (`port`, `normalization.lm`, `normalization.dataRoot`, `morphology.extraDictionaries`, `morphology.ignoreDiacritics`) and fixed configuration directory validation.
- **Core Utilities (`zemberek-core`)** ([#222](https://github.com/ahmetaa/zemberek-nlp/issues/222), [#329](https://github.com/ahmetaa/zemberek-nlp/pull/329), [#378](https://github.com/ahmetaa/zemberek-nlp/pull/378)):
  - Improved `Log` shutdown hook management, handler reset behavior, and fast-path level checks.
  - Added shared character, casing, and diacritic helpers in `TurkishAlphabet` and `Turkish`.

*Assisted by Gemini and Antigravity*


## 0.18.0

(November 10th 2026)

Zemberek `0.18.0` brings major improvements in **disambiguation accuracy**, **end-to-end processing speed (~4x faster than `0.17.1`)**, **morphological coverage**, and **tokenization/sentence segmentation quality**, along with updated dependencies and Java 21 baseline support.

### Highlights at a Glance (`0.17.1` → `0.18.0`)

| Area | `0.17.1` | `0.18.0` | What Changed |
| :--- | :---: | :---: | :--- |
| **End-to-End Pipeline Speed** | ~22.0k tokens/sec | **~88.6k tokens/sec** | **~4.0x faster** full pipeline (segment → tokenize → analyze → disambiguate) |
| **Disambiguation Accuracy** *(Ambiguous Tokens)* | ~77.5% | **~92.8%** | **~68% fewer disambiguation errors**; **~2.7x more** sentences disambiguated with 100% exact match |
| **Disambiguation Speed** (`analyzeAndDisambiguate`) | ~22.2k tokens/sec | **~90.0k tokens/sec** | **~4.0x faster** via `FastPerceptronAmbiguityResolver` |
| **Morphological Analysis Speed (Cached)** | ~140k – 146k tokens/sec | **~283k – 395k tokens/sec** | **~2.0x – 2.7x faster** (cold / warm) with a unified 50k Caffeine cache (`88.9%–90.1%` hit rate) |
| **Morphological Analysis Speed (Uncached)** | ~52.4k tokens/sec | **~73.6k tokens/sec** | **~1.4x faster** uncached stem/morphotactic search and surface formatting |
| **Sentence Segmentation Speed** | ~184k sentences/sec | **~255k sentences/sec** | **~1.4x faster** with higher boundary precision |
| **Tokenizer Speed** | ~3.5M tokens/sec | **~4.1M tokens/sec** | **~1.2x faster** by skipping intermediate token allocations and fixing ANTLR EOF DFA caching |

> **Benchmark Setup:** Single-threaded (single-core), single-pass execution on an AMD Ryzen 7 1700X (x86_64 Linux), running on Eclipse Temurin OpenJDK 25.0.4.1 64-Bit Server VM (`-Xmx8g`). Throughput and coverage were measured on a 100,000-paragraph news corpus (`cnn-turk`, 24.6M chars / 25.8 MB, 211,232 sentences, 3,686,164 tokens, 229,161 unique word types) and disambiguation accuracy on a held-out 1,000-sentence / 11,582-token evaluation set (`test_slice_001`, 6,587 ambiguous tokens).

### What's New & Improved

#### 1. Morphological Disambiguation
- **New Default Ambiguity Resolver & Model (`FastPerceptronAmbiguityResolver`)**:
  - Replaced the legacy disambiguator and old model files with `FastPerceptronAmbiguityResolver` using Exact Viterbi decoding and a new compact default model (`model_10k_argon_v24.bin`, 2.16 MB).
  - Improves ambiguous-word accuracy from **~77.5% to ~92.8%** (overall token accuracy **~86.8% → ~95.6%**) while running **~4.0x faster**.
  - Added `FastPerceptronAmbiguityResolverTrainer` for training custom models.

#### 2. Morphological Analysis & Caching
- **Better Morphotactics & OOV Handling (`TurkishMorphotactics`, `UnidentifiedTokenAnalyzer`)**:
  - Added support for `deyip` / `yiyip` converb forms, verbal nouns in `-mada/-mede/-yışta` with copula (`beklemedeyiz`, `okumadayım`), derived abbreviations (`ABD'li`, `ABD'siz`), multi-apostrophe words (`Kur'an'da`, `Eto'o'nun`), hyphenated proper nouns, and alphanumeric/domain tokens (`3G'ye`, `S3'ün`, `meb.gov.tr'den`).
  - Improved token parse coverage on the 100k-paragraph corpus from **97.95% to 98.87%** (reducing unparsed `UNK` tokens by **45%**, from **2.05% to 1.13%**, or `75,437` → `41,489` tokens).
- **Simplified Cache & Faster Uncached Analysis (`AnalysisCache`, `RuleBasedAnalyzer`)**:
  - Unified the two-level static/dynamic cache into a single thread-safe Caffeine cache (default capacity increased to 50,000 entries) and optimized uncached morphotactic search and surface formatting (**~2.0x faster** cold cached analysis, **~2.7x faster** warm cached analysis, and **~1.4x faster** uncached analysis).

#### 3. Sentence Segmentation & Tokenization
- **More Accurate & Faster Sentence Extractor (`TurkishSentenceExtractor`)**:
  - Added ellipsis (`…`) boundary support, retrained `sentence-boundary-model.bin`, and fixed false non-boundary vetoes when sentences end with words that share spelling with uppercase abbreviations (e.g., *et.*, *bul.*, *kal.*, *at.*, *taş.*, *gör.*) or inflected dates/decimals (`06.01.2014'de`, `49.90'dan`).
  - Optimized boundary scanning and feature extraction (**~1.4x faster** than `0.17.1`).
- **Cleaner & Faster Tokenizer (`TurkishTokenizer`)**:
  - Fixed tokenization of single-letter stems with apostrophes (`O'na`, `A'dan`), initials (`M. Kemal`), acronyms (`CD'ye`, `DVD'de`), sentence-ending URLs/emails, and currency/punctuation symbols (`₺`, `€`, `–`, `—`, `°`).
  - Eliminated intermediate `CommonToken` and whitespace token allocations and fixed ANTLR EOF DFA caching in `CustomLexerATNSimulator`.

#### 4. Core Collections, Build & Dependencies
- **Hardened Primitive Collections (`zemberek-core`)**:
  - Fixed edge-case bugs in `UIntMap`, `IntMap`, `LongUIntMap`, `ActiveList`, `LongBitVector`, `Trie`, and hash-table tombstone/resize handling under heavy remove/insert churn.
- **Build & Dependency Updates**:
  - Updated compiler source/target baseline from Java 8 to **Java 21**.
  - Upgraded core dependencies (`Guava 33.7.1`, `Protobuf 4.36.2`, `Caffeine 3.2.0`, `gRPC 1.84.0`, `OpenNLP 2.5.12`, `JUnit 4.13.2`) to address security advisories.
  - Removed the deprecated `server` module and `progressbar` dependency for cleaner fat/shaded JAR packaging.

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


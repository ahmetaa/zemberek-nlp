# Morphological Output Formats & Cross-System Interoperability

Zemberek's native `SingleAnalysis` model preserves dictionary headwords, surface allomorphs, lexical morpheme IDs, and derivation group (`IG`) boundaries simultaneously. For comparative NLP research, treebank evaluation, WordNet/PropBank alignment, and cross-Turkic experiments, `zemberek.morphology.analysis.format` provides stateless formatters targeting major academic standards without altering Zemberek's internal morphotactics.

---

## 1. Quick Comparison Across Supported Formats

For the input word **`kitaplarımızdan`** (*"from our books"*) and **`gidiyor`** (*"he/she is going"*):

| Format / Formatter | `kitaplarımızdan` | `gidiyor` |
| :--- | :--- | :--- |
| **Zemberek `DEFAULT`** | `[kitap:Noun] kitap:Noun+lar:A3pl+ımız:P1pl+dan:Abl` | `[gitmek:Verb] gid:Verb+iyor:Prog1+A3sg` |
| **Zemberek `SURFACE_SEQUENCE`** | `kitap lar ımız dan` | `gid iyor` |
| **`CANONICAL_SURFACE_SEQUENCE`** | `kitap lar ımız dan` | `git iyor` |
| **`LEIPZIG_INTERLINEAR`** | `kitap-lar-ımız-dan	kitap-PL-POSS.1PL-ABL` | `gid-iyor	git-PROG` |
| **`OFLAZER_STRICT` (`METU_SABANCI`)** | `kitap+Noun+A3pl+P1pl+Abl` | `git+Verb+Pos+Prog1+A3sg` |
| **`OflazerFormatter.starlangUpper()`** | `kitap+NOUN+A3PL+P1PL+ABL` | `git+VERB+POS+PROG1+A3SG` |
| **`ConlluFormatter` (`UD_TURKIC_BOUN`)** | `kitap	NOUN	_	Case=Abl\|Number=Plur\|Number[psor]=Plur\|Person=3\|Person[psor]=1` | `git	VERB	_	Aspect=Prog\|Number=Sing\|Person=3\|Polarity=Pos\|Tense=Pres` |
| **`APERTIUM_TURKIC`** | `kitap<n><pl><px1pl><abl>` | `git<v><prog><p3><sg>` |
| **`UNIMORPH`** | `kitap	kitaplarımızdan	N;ABL;PL;PSS1P` | `gitmek	gidiyor	V;IND;PRS;PROG;3;SG;POS;DECL` |

---

## 2. Formatters Reference

### 2.1 Strict Oflazer Family (`OflazerFormatter` / `AnalysisFormatters.OFLAZER_STRICT`)

The legacy `AnalysisFormatters.OFLAZER_STYLE` formatter only reordered `^DB+` boundaries on top of Zemberek's raw morpheme list, leaving mutated stems (`kitab+`, `gid+`, `ar+`) and omitting zero-surface tags. `OflazerFormatter` closes those gaps across four research profiles:

- `OflazerFormatter.metuSabanci()` (`AnalysisFormatters.OFLAZER_STRICT`):
  - Restores unmutated dictionary lemmas (`kitap`, `git`, `ara`, `ağız`) and capitalized `ProperNoun` lemmas (`Ankara`).
  - Reconstructs zero-surface `+Pnon` and `+Nom` slots across all `Noun` and `Pron` inflectional groups (`kitap+Noun+A3sg+Pnon+Nom^DB+Adj+Without`).
  - Inserts affirmative `+Pos` verbal polarity, expands `+Unable` into `^DB+Verb+Able+Neg` (and `+WithoutBeingAbleToHaveDoneSo` into `^DB+Verb+Able+Neg^DB+Adv+WithoutHavingDoneSo`), and normalizes `+Pres+Cop+A3sg` order.
  - Maps `SecondaryPos` tags (`DemonsP`, `PersP`, `QuesP`, `ReflexP`, `QuantP`, `Distrib`), expands substantivized `PresPart`/`NarrPart` (`^DB+Adj+PresPart^DB+Noun+Zero`), rewrites genitive `-ki` (`^DB+Noun+Rel`), and splits compound-tense secondary TAMs (`ağla+Verb+Pos+Fut+A3sg^DB+Verb+Zero+Past+A3sg`).
- `OflazerFormatter.oflazer94()`: Uses `Adverb` POS label and classic 3-participle folding (`NarrPart -> PastPart`, `AorPart -> PresPart`).
- `OflazerFormatter.sak2008()`: Haşim Sak (2008/2011) disambiguator conventions (lowercase roots and flat compound tenses `ağla+Verb+Pos+Fut+Past+A3sg`).
- `OflazerFormatter.starlangUpper()`: Uppercase Oflazer notation (`kitap+NOUN+A3SG+PNON+LOC`) compatible with `StarlangSoftware/TurkishMorphologicalAnalysis`, **KeNet** (Turkish WordNet), Turkish PropBank, and Turkish FrameNet.

### 2.2 Universal Dependencies v2 `CoNLL-U` (`ConlluFormatter` / `AnalysisFormatters.CONLLU_TOKEN`)

Exports disambiguated `SentenceAnalysis` instances into 10-column Universal Dependencies v2 `CoNLL-U` format (`ID, FORM, LEMMA, UPOS, XPOS, FEATS, HEAD, DEPREL, DEPS, MISC`), leaving syntactic dependency columns (`HEAD, DEPREL, DEPS`) as `_`:

```java
TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
SentenceAnalysis sa = morphology.analyzeAndDisambiguate(
    "Fakülteyi bitirenler göreve başlıyorlarmış.");

String conllu = ConlluFormatter.bounDefault().formatSentence("1", sa);
```

Key behaviors aligned with `UD_Turkish-BOUN` and the **UD Turkic Harmonization** guidelines:
- **Lexical vs. Grammatical IG Walk:** Advances `LEMMA` across lexical derivations (`öğretmen -> öğretmenlik`, `gerçek -> gerçekleş`) and records `DerivedFrom=<root>` in `MISC`, while keeping `LEMMA` at the verbal stem across voice (`Caus`, `Pass`, `Reflex`, `Recip`) and non-finite projections (`Part`, `Vnoun`, `Conv`).
- **Composite Feature Accumulation:** Concatenates stacked voices in derivation order (`Voice=CauPass`, `Voice=CauCau`) and stacked moods alphabetically (`Mood=CndPot`, `Mood=DesPot`, `Mood=GenNec`, `Mood=GenPot`), mapping `-dIr` to `Mood=Gen` and `-Iver`/`-Akal` to `Aspect=Rapid`/`Aspect=Dur`.
- **Autonomous Multi-Word Token (`i-j`) Splitting:** Configurable via `MwtMode` (`COPULA_AND_KI`, `COPULA_ONLY`, `NONE`). Automatically splits overt nominal copulas (`İstanbul'daydı -> İstanbul'da + ydı`), finite compound-tense copulas (`başlıyorlarmış -> başlıyorlar + mış`), and adnominal `-ki` (`evdeki -> evde + ki`), placing `SpaceAfter=No` on the MWT range header per strict UD validation rules.
- **Pipe-Safe `MISC`:** When `includeZemberekInMisc(true)` is enabled, internal `|` derivation separators in Zemberek's native format are escaped to `^DB` so CoNLL-U `MISC` attribute delimiters are never broken.
- **`UD_TURKIC_BOUN` vs. `UD_IMST` Dialect Profiles:** `ConlluFormatter.bounDefault()` (`Dialect.UD_TURKIC_BOUN`) is the recommended default following modern UD Turkic Harmonization (`da/de -> PART/Emph`, `var/yok -> NOUN/Exist`, `SCONJ` subordinating conjunctions, `Inf1` without redundant `Number=Sing|Person=3`, `Aspect=Imp` on future). `ConlluFormatter.imstDefault()` (`Dialect.UD_IMST`) targets legacy METU-Sabancı-converted `UD_Turkish-IMST` benchmarks (`Mood=Ind` defaults, `ADJ/NAdj` substantivized adjectives, `CCONJ` conjunctions, `-ki -> ADP/Rel`, `Reflex=Yes` on `kendi`, and `Aspect=Perf|Tense=Pres` defaults on non-finite verbals).

### 2.3 Pan-Turkic & Subword Research Formatters

- **Apertium Turkic (`ApertiumFormatter` / `AnalysisFormatters.APERTIUM_TURKIC`):** Emits ordered `<tag>` sequences and `^surface/analysis1/analysis2$` lexical streams matching `apertium-tur` and the shared pan-Turkic Apertium tagset (`apertium-kaz`, `tat`, `uzb`, `kir`, `uig`, `aze`, `crh`, `bak`).
- **UniMorph (`UniMorphFormatter` / `AnalysisFormatters.UNIMORPH`):** Emits 3-column SIGMORPHON / `unimorph/tur` rows (`lemma\tsurface\tBUNDLE`) or standalone feature bundles (`UniMorphFormatter.BUNDLE_ONLY`).
- **Canonical Segmentation & Leipzig Glossing (`SegmentedSurfaceFormatter`):**
  - `AnalysisFormatters.CANONICAL_SURFACE_SEQUENCE`: Restores mutated stems while keeping surface suffix allomorphs (`kitap ımız dan`, `git iyor`, `ara ıyor`), preventing root-allomorph vocabulary splits in BPE/LLM pre-tokenization.
  - `AnalysisFormatters.LEIPZIG_INTERLINEAR`: Two-tier tab-separated Leipzig Glossing Rules output (`kitab-ımız-dan\tkitap-POSS.1PL-ABL`).

---

## 3. Documented Irreducible Differences (Lexicon & Morphotactic Scope)

The following differences reflect intentional linguistic design choices in Zemberek's lexicon or morphotactics rather than formatting omissions, and cannot (or should not) be rewritten blindly in a stateless single-word formatter:

1. **TDK Headwords vs. Aggressive Root Stripping:**
   - **Possessive compounds (`CompoundP3sg`):** Zemberek indexes TDK headwords such as `cumhurbaşkanı`, `soyadı`, `kamuoyu`, and `zeytinyağı` as lemmas, whereas Oflazer-1994 parses them from bare unpossessed stems (`cumhurbaşkan+Noun+A3sg+P3sg+Nom`).
   - **Bound spatial stems & distributives:** Zemberek uses real TDK lemmas `üzeri` and `birer`, whereas Oflazer-1994 uses abstract stems `üzer+Noun+A3sg+P3sg` and `bir+Num+Distrib`.
   - **Lexicalized TDK derivations:** Common derived words that have standalone TDK entries (`gerekli`, `önemli`, `sorumluluk`, `toplumsal`, `gazeteci`) analyze directly as root lemmas in Zemberek rather than always decomposing into `gerek+Noun+...+^DB+Adj+With`.
2. **POS Taxonomy Twins:**
   - Existential `var` / `yok` are modeled primarily as `Adj` in Zemberek (`ConlluFormatter` projects standalone `var`/`yok` to `NOUN` with `XPOS=Exist` in `UD_TURKIC_BOUN` mode and `ADJ` in `UD_IMST` mode).
   - Indefinite `bir` is `Det` in Zemberek (whereas Oflazer-derived corpora sometimes annotate `Num+Card`).
3. **Adnominal vs. Nominalized Possessed `-dIk` / `-yAcAk` Participles:**
   - In Zemberek, possessed `-dIk` (`okuduğum`) and `-yAcAk` (`okuyacağım`) always transition through `noun_S` (`oku+Verb^DB+Noun+PastPart+A3sg+P1sg`). In Oflazer, adnominal `"okuduğum kitap"` is `^DB+Adj+PastPart+P1sg` while headless `"okuduğum doğru"` is `^DB+Noun+PastPart+A3sg+P1sg+Nom`. Distinguishing the two requires syntactic lookahead to the following noun phrase.
4. **Lexical Verb Transitivity in Apertium (`<tv>` vs. `<iv>`):**
   - `apertium-tur` marks lexical verb roots as transitive (`<tv>`) or intransitive (`<iv>`). Because Zemberek's dictionary does not currently store root verb transitivity, `ApertiumFormatter` emits `<v>` without guessing `<tv>`/`<iv>`.
5. **Syntactic Dependency Columns & Zero-3sg Nominal Predicates in CoNLL-U:**
   - Columns `HEAD`, `DEPREL`, and `DEPS` require a syntactic dependency parser and are emitted as `_`. Similarly, `nullcop=3s` in `UD_Turkish-BOUN` (marking uninflected nouns acting as clause-final predicates, e.g., *"Ali doktor"*) is a clause-level syntactic property rather than a morphological one.

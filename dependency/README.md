# Turkish Dependency Parsing (`zemberek-dependency`)

`zemberek-dependency` provides pure-Java, zero-external-dependency Universal Dependencies v2 (`UD_Turkish-BOUN`) syntactic dependency parsing on top of Zemberek's morphological analyzer, disambiguator, and `ConlluFormatter`.

## Architecture (`ZDEP` v1)

```text
Input Sentence
  -> TurkishMorphology.analyzeAndDisambiguate
  -> ConlluFormatter.projectSentence (MWT splitting + LEMMA + UPOS + XPOS + FEATS)
  -> [LemmaEmb ; CharCNN ; UposEmb ; XposEmb ; MeanFeatEmb] -> Linear + LayerNorm + ReLU
  -> L-layer Bidirectional GRU
  -> Deep Biaffine Arc Scorer (Dozat & Manning 2017) -> Exact Single-Root Chu-Liu/Edmonds MST
  -> Deep Biaffine Label Scorer on winning arcs
  -> DependencyTree (Java API + 10-column CoNLL-U v2)
```

- **Non-projective exact decoding:** `ChuLiuEdmonds.decodeSingleRoot` guarantees cycle-free, single-root trees and natively supports Turkish non-projective word order.
- **Compact int8 storage:** Supports FP32 and symmetric per-row INT8 quantization (`.bin` / `.bin.gz`) with reusable `ThreadLocal` scratch buffers for low-GC multi-threaded inference.

## Usage

```java
TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
TurkishDependencyParser parser = TurkishDependencyParser.fromModelFile(
    morphology, Paths.get("/path/to/dep_boun_v1.bin.gz"));
// Once a default model is bundled on the classpath:
// TurkishDependencyParser parser = TurkishDependencyParser.createWithDefaults(morphology);

DependencyTree tree = parser.parse(
    "Prof. Dr. Fethi Bey saat 14.30'da Türkiye'ye döndü.");

for (DependencyNode node : tree.getNodes()) {
  System.out.printf("%d\t%s\t%s\t%s\t%d\t%s%n",
      node.id(), node.form(), node.lemma(), node.upos(), node.headId(), node.deprel());
}

// Direct 10-column CoNLL-U v2 output (with MWT lines & HEAD/DEPREL populated):
System.out.println(tree.toConllu());
```

## Training & Google Colab Pipeline

Reproducible PyTorch training, BERTurk/ConvBERT teacher–student distillation, INT8 `ZDEP` export, and a ready-to-run Google Colab notebook live under [`experiment/scripts/dependency/`](../experiment/scripts/dependency/):

- `model_zdep.py` — PyTorch `ZdepBiaffineParser` matching `BiaffineDependencyModel.java` + `ZDEP` v1 exporter.
- `train_student.py` — Supervised & distilled student trainer (`compact` ~3 MB, `medium` ~6 MB, `large` ~10 MB presets).
- `train_teacher_distill.py` — `BERTurk` / `ConvBERT-Turkish` Deep Biaffine teacher trainer & confidence-filtered silver CoNLL-U generator.
- `Zemberek_Turkish_Dependency_Colab.ipynb` — End-to-end Google Colab notebook (runs on free T4 GPU).

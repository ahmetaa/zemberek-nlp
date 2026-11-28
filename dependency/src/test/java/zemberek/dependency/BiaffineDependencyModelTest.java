package zemberek.dependency;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.dependency.BiaffineDependencyModel.GruDirectionWeights;
import zemberek.dependency.BiaffineDependencyModel.RawPredictions;
import zemberek.dependency.DependencyTree.EvaluationMetrics;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.format.ConlluFormatter;
import zemberek.morphology.analysis.format.ConlluSentence;

public class BiaffineDependencyModelTest {

  private static TurkishMorphology morphology;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void setUpClass() {
    morphology = TurkishMorphology.createWithDefaults();
  }

  private static float[] randomFloats(Random rnd, int len, float scale) {
    float[] arr = new float[len];
    for (int i = 0; i < len; i++) {
      arr[i] = (float) (rnd.nextGaussian() * scale);
    }
    return arr;
  }

  private static QuantizedMatrix randomMatrix(Random rnd, int rows, int cols, boolean int8) {
    float[] data = randomFloats(rnd, rows * cols, 0.2f);
    return int8
        ? QuantizedMatrix.quantizePerRowInt8(rows, cols, data)
        : QuantizedMatrix.ofFloat(rows, cols, data);
  }

  private static BiaffineDependencyModel buildTinyModel(long seed, boolean int8) {
    Random rnd = new Random(seed);
    int lemmaDim = 8;
    int charEmbDim = 4;
    int[] kernels = new int[] {2, 3};
    int charFilters = 6;
    int uposDim = 4;
    int xposDim = 4;
    int featDim = 6;
    int rawIn = lemmaDim + kernels.length * charFilters + uposDim + xposDim + featDim;
    int projDim = 12;
    int gruLayers = 2;
    int gruHidden = 10;
    int encOut = 2 * gruHidden;
    int arcDim = 8;
    int relDim = 6;

    String[] lemmas = {"<PAD>", "<ROOT>", "<UNK>", "ev", "çocuk", "i", "ki", "yarın", "kar", "yağ"};
    String[] chars = {"<PAD>", "<BOS>", "<EOS>", "<UNK>", "a", "e", "i", "ı", "k", "r", "v", "d", "y"};
    String[] upos = {"<PAD>", "<ROOT>", "<UNK>", "NOUN", "VERB", "AUX", "PART", "PUNCT", "PROPN", "ADV"};
    String[] xpos = {"<PAD>", "<ROOT>", "<UNK>", "_", "Zero", "Attr", "Punc"};
    String[] feats = {"<PAD>", "<UNK>", "Case=Nom", "Case=Loc", "Number=Sing", "Number=Plur", "Person=3", "Tense=Past"};
    String[] labels = {"root", "nsubj", "obj", "obl", "cop", "punct", "amod"};

    QuantizedMatrix lemmaEmb = randomMatrix(rnd, lemmas.length, lemmaDim, int8);
    QuantizedMatrix charEmb = randomMatrix(rnd, chars.length, charEmbDim, int8);
    QuantizedMatrix[] charConvW = new QuantizedMatrix[kernels.length];
    float[][] charConvB = new float[kernels.length][];
    for (int i = 0; i < kernels.length; i++) {
      charConvW[i] = randomMatrix(rnd, charFilters, kernels[i] * charEmbDim, int8);
      charConvB[i] = randomFloats(rnd, charFilters, 0.05f);
    }
    QuantizedMatrix uposEmb = randomMatrix(rnd, upos.length, uposDim, int8);
    QuantizedMatrix xposEmb = randomMatrix(rnd, xpos.length, xposDim, int8);
    QuantizedMatrix featEmb = randomMatrix(rnd, feats.length, featDim, int8);

    QuantizedMatrix projW = randomMatrix(rnd, projDim, rawIn, int8);
    float[] projB = randomFloats(rnd, projDim, 0.05f);
    float[] lnGamma = new float[projDim];
    java.util.Arrays.fill(lnGamma, 1.0f);
    float[] lnBeta = new float[projDim];

    GruDirectionWeights[] fwd = new GruDirectionWeights[gruLayers];
    GruDirectionWeights[] bwd = new GruDirectionWeights[gruLayers];
    for (int l = 0; l < gruLayers; l++) {
      int inD = (l == 0) ? projDim : encOut;
      fwd[l] = new GruDirectionWeights(
          randomMatrix(rnd, 3 * gruHidden, inD, int8),
          randomFloats(rnd, 3 * gruHidden, 0.05f),
          randomMatrix(rnd, 3 * gruHidden, gruHidden, int8),
          randomFloats(rnd, 3 * gruHidden, 0.05f));
      bwd[l] = new GruDirectionWeights(
          randomMatrix(rnd, 3 * gruHidden, inD, int8),
          randomFloats(rnd, 3 * gruHidden, 0.05f),
          randomMatrix(rnd, 3 * gruHidden, gruHidden, int8),
          randomFloats(rnd, 3 * gruHidden, 0.05f));
    }

    QuantizedMatrix arcDepW = randomMatrix(rnd, arcDim, encOut, int8);
    float[] arcDepB = randomFloats(rnd, arcDim, 0.05f);
    QuantizedMatrix arcHeadW = randomMatrix(rnd, arcDim, encOut, int8);
    float[] arcHeadB = randomFloats(rnd, arcDim, 0.05f);
    QuantizedMatrix arcBiaffine = randomMatrix(rnd, arcDim + 1, arcDim + 1, int8);

    QuantizedMatrix relDepW = randomMatrix(rnd, relDim, encOut, int8);
    float[] relDepB = randomFloats(rnd, relDim, 0.05f);
    QuantizedMatrix relHeadW = randomMatrix(rnd, relDim, encOut, int8);
    float[] relHeadB = randomFloats(rnd, relDim, 0.05f);
    QuantizedMatrix[] relBiaffine = new QuantizedMatrix[labels.length];
    for (int c = 0; c < labels.length; c++) {
      relBiaffine[c] = randomMatrix(rnd, relDim + 1, relDim + 1, int8);
    }

    return new BiaffineDependencyModel(
        lemmaDim, charEmbDim, kernels, charFilters,
        uposDim, xposDim, featDim, projDim, gruLayers, gruHidden, arcDim, relDim,
        lemmas, chars, upos, xpos, feats, labels,
        lemmaEmb, charEmb, charConvW, charConvB, uposEmb, xposEmb, featEmb,
        projW, projB, lnGamma, lnBeta,
        fwd, bwd,
        arcDepW, arcDepB, arcHeadW, arcHeadB, arcBiaffine,
        relDepW, relDepB, relHeadW, relHeadB, relBiaffine);
  }

  @Test
  public void binaryGzipRoundTripProducesBitIdenticalPredictions() throws IOException {
    BiaffineDependencyModel orig = buildTinyModel(77L, true);
    Path modelFile = tempFolder.getRoot().toPath().resolve("tiny.bin.gz");
    try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(modelFile)))) {
      orig.write(out);
    }

    BiaffineDependencyModel loaded = BiaffineDependencyModel.load(modelFile);
    TurkishDependencyParser p1 = new TurkishDependencyParser(morphology, orig, ConlluFormatter.bounDefault());
    TurkishDependencyParser p2 = TurkishDependencyParser.fromModelFile(morphology, modelFile);

    String sentence = "Evdeki çocuklar İstanbul'daydı.";
    DependencyTree t1 = p1.parse(sentence);
    DependencyTree t2 = p2.parse(sentence);

    Assert.assertEquals(t1.toConllu(), t2.toConllu());
    Assert.assertNotNull(t2.getRoot());
    Assert.assertEquals("root", t2.getRoot().deprel());
    Assert.assertEquals(t2.size(), t2.getSubtree(t2.getRoot().id()).size());

    // Ensure non-root tokens never receive the "root" deprel
    for (DependencyNode node : t2.getNodes()) {
      if (node.isRoot()) {
        Assert.assertEquals("root", node.deprel());
      } else {
        Assert.assertNotEquals("root", node.deprel());
      }
    }

    // Round-trip CoNLL-U output through ConlluSentence.parseAll and self-evaluate -> 100% UAS/LAS
    List<ConlluSentence> reparsed = ConlluSentence.parseAll(t2.toConllu());
    Assert.assertEquals(1, reparsed.size());
    EvaluationMetrics selfEval = DependencyTree.evaluate(reparsed, List.of(t2));
    Assert.assertEquals(100.0, selfEval.uas(), 1e-6);
    Assert.assertEquals(100.0, selfEval.las(), 1e-6);
    Assert.assertEquals(100.0, selfEval.exactMatch(), 1e-6);
  }

  @Test
  public void int8QuantizationCloselyTracksFp32Scores() {
    BiaffineDependencyModel fp32 = buildTinyModel(99L, false);
    BiaffineDependencyModel int8 = buildTinyModel(99L, true);

    ConlluSentence projected = ConlluFormatter.bounDefault()
        .projectSentence(morphology.analyzeAndDisambiguate("Yarın kar yağacak."));

    RawPredictions rFp = fp32.predictRaw(projected);
    RawPredictions rQ = int8.predictRaw(projected);

    int n = projected.size();
    double maxDiff = 0.0;
    for (int d = 1; d <= n; d++) {
      for (int h = 0; h <= n; h++) {
        if (d == h) {
          continue;
        }
        maxDiff = Math.max(maxDiff, Math.abs(rFp.arcScores[d][h] - rQ.arcScores[d][h]));
      }
    }
    Assert.assertTrue("INT8 arc scores diverged too much from FP32: " + maxDiff, maxDiff < 0.05);
  }

  @Test
  public void concurrentParsingIsThreadSafeAndDeterministic() throws Exception {
    BiaffineDependencyModel model = buildTinyModel(1234L, true);
    TurkishDependencyParser parser =
        new TurkishDependencyParser(morphology, model, ConlluFormatter.bounDefault());

    String[] sentences = {
        "Yarın kar yağacak.",
        "Evdeki çocuklar İstanbul'daydı.",
        "Fakülteyi bitirenler en uçtan göreve başlıyorlarmış."
    };
    String[] expected = new String[sentences.length];
    for (int i = 0; i < sentences.length; i++) {
      expected[i] = parser.parse(sentences[i]).toConllu();
    }

    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (int t = 0; t < 4; t++) {
        tasks.add(() -> {
          for (int iter = 0; iter < 15; iter++) {
            int idx = iter % sentences.length;
            Assert.assertEquals(expected[idx], parser.parse(sentences[idx]).toConllu());
          }
          return null;
        });
      }
      for (Future<Void> f : pool.invokeAll(tasks)) {
        f.get();
      }
    } finally {
      pool.shutdownNow();
    }
  }
}

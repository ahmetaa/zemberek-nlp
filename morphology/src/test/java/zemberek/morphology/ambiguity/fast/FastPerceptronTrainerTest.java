package zemberek.morphology.ambiguity.fast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.data.Weights;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class FastPerceptronTrainerTest {

  @Test
  public void testTrainerConvergenceOnSimpleSentence() {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    // Create a simple dataset with ambiguous sentences
    List<SentenceAnalysis> sentences = new ArrayList<>();

    String text1 = "Ali eve geldi .";
    List<WordAnalysis> waList1 = morphology.analyzeSentence(text1);
    List<SentenceWordAnalysis> swaList1 = new ArrayList<>();
    for (WordAnalysis wa : waList1) {
      swaList1.add(new SentenceWordAnalysis(wa.getAnalysisResults().get(0), wa));
    }
    sentences.add(new SentenceAnalysis(text1, swaList1));

    String text2 = "Büyük bir ev .";
    List<WordAnalysis> waList2 = morphology.analyzeSentence(text2);
    List<SentenceWordAnalysis> swaList2 = new ArrayList<>();
    for (WordAnalysis wa : waList2) {
      SingleAnalysis chosen = wa.getAnalysisResults().get(0);
      if (wa.analysisCount() > 1) {
        chosen = wa.getAnalysisResults().get(1);
      }
      swaList2.add(new SentenceWordAnalysis(chosen, wa));
    }
    sentences.add(new SentenceAnalysis(text2, swaList2));

    DataSet dataset = new DataSet(sentences);

    FastPerceptronAmbiguityResolverTrainer trainer =
        new FastPerceptronAmbiguityResolverTrainer(0.0);
    FastPerceptronAmbiguityResolver resolver = trainer.train(dataset, dataset, 3);

    Assert.assertNotNull(resolver);
    Weights weights = trainer.getAveragedWeights();
    Assert.assertTrue("Averaged weights should contain features", weights.size() > 0);

    // Evaluate
    double acc = FastPerceptronAmbiguityResolverTrainer.evaluateAccuracy(dataset, resolver);
    Assert.assertTrue("Accuracy should be positive", acc > 0.0);
  }

  @Test
  public void testModelSerialization() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    List<SentenceAnalysis> sentences = new ArrayList<>();

    String text = "Büyük bir ev .";
    List<WordAnalysis> waList = morphology.analyzeSentence(text);
    List<SentenceWordAnalysis> swaList = new ArrayList<>();
    for (WordAnalysis wa : waList) {
      SingleAnalysis chosen = wa.getAnalysisResults().get(0);
      if (wa.analysisCount() > 1) {
        chosen = wa.getAnalysisResults().get(1);
      }
      swaList.add(new SentenceWordAnalysis(chosen, wa));
    }
    sentences.add(new SentenceAnalysis(text, swaList));

    DataSet dataset = new DataSet(sentences);
    FastPerceptronAmbiguityResolverTrainer trainer = new FastPerceptronAmbiguityResolverTrainer();
    trainer.train(dataset, 2);

    Path tempDir = Files.createTempDirectory("zemberek-trainer-test");
    Path modelBin = tempDir.resolve("test-model.bin");

    try {
      trainer.saveModel(modelBin, true);
      Assert.assertTrue(Files.exists(modelBin));
      Assert.assertTrue(Files.size(modelBin) > 0);

      Path modelTxt = tempDir.resolve("test-model.txt");
      Assert.assertTrue(Files.exists(modelTxt));
      Assert.assertTrue(Files.size(modelTxt) > 0);

      // Verify model can be loaded back
      FastPerceptronAmbiguityResolver loaded = FastPerceptronAmbiguityResolver.fromModelFile(modelBin);
      Assert.assertNotNull(loaded);
    } finally {
      Files.deleteIfExists(tempDir.resolve("test-model.bin"));
      Files.deleteIfExists(tempDir.resolve("test-model.txt"));
      Files.deleteIfExists(tempDir);
    }
  }
}

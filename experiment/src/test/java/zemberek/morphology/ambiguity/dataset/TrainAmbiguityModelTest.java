package zemberek.morphology.ambiguity.dataset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver;
import zemberek.morphology.ambiguity.dataset.TrainAmbiguityModel.Config;
import zemberek.morphology.ambiguity.dataset.TrainAmbiguityModel.TrainingResult;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;

public class TrainAmbiguityModelTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testTrainAndDisambiguate() throws IOException {
    Path trainTxt = tempFolder.newFile("sample_train.txt").toPath();
    Path outputModel = tempFolder.getRoot().toPath().resolve("model-compressed.bin");

    // Sample training data formatted as Zemberek training text
    String trainingText =
        "S:Eve geldim.\n"
            + "Eve\n"
            + "[ev:Noun] ev:Noun+A3sg+e:Dat*\n"
            + "geldim\n"
            + "[gelmek:Verb] gel:Verb+di:Past+m:A1sg*\n"
            + ".\n"
            + "[.:Punc] .:Punc*\n"
            + "S:Kimse yok.\n"
            + "Kimse\n"
            + "[kimse:Noun] kimse:Noun+A3sg\n"
            + "[kimse:Pron,Quant] kimse:Pron+A3sg*\n"
            + "[kim:Pron,Ques] kim:Pron+A3sg|Zero→Verb+se:Cond+A3sg\n"
            + "yok\n"
            + "[yok:Conj] yok:Conj\n"
            + "[yok:Adj] yok:Adj*\n"
            + "[yok:Adv] yok:Adv\n"
            + "[yok:Adj] yok:Adj\n"
            + "[yok:Noun] yok:Noun+A3sg\n"
            + ".\n"
            + "[.:Punc] .:Punc*\n";

    Files.write(trainTxt, trainingText.getBytes(StandardCharsets.UTF_8));

    Config config = new Config.Builder()
        .trainPath(trainTxt)
        .outputPath(outputModel)
        .exportText(true)
        .iterationCount(5)
        .build();

    TrainingResult result = TrainAmbiguityModel.train(config);

    Assert.assertNotNull(result.resolver);
    Assert.assertTrue("Feature count must be greater than 0", result.featureCount > 0);
    Assert.assertTrue("Output binary model must exist", Files.exists(outputModel));
    Assert.assertTrue("Output text model must exist", Files.exists(config.getTextWeightsPath()));

    // Load the saved binary model using Zemberek's public factory
    PerceptronAmbiguityResolver loadedResolver =
        PerceptronAmbiguityResolver.fromModelFile(outputModel);
    Assert.assertNotNull("Loaded resolver must not be null", loadedResolver);

    // Verify disambiguation using the trained model
    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(loadedResolver)
        .build();

    SentenceAnalysis sentenceAnalysis = morphology.analyzeAndDisambiguate("Kimse yok.");
    Assert.assertEquals(3, sentenceAnalysis.size());

    // Check that 'Kimse' was disambiguated to Pron,Quant
    WordAnalysis kimseWord = sentenceAnalysis.getWordAnalyses().get(0).getWordAnalysis();
    Assert.assertEquals("Kimse", kimseWord.getInput());
    Assert.assertEquals("Pron", sentenceAnalysis.bestAnalysis().get(0).getDictionaryItem().primaryPos.shortForm);
  }

  @Test
  public void testTrainFromJsonl() throws IOException {
    Path jsonlFile = tempFolder.newFile("sample_train.jsonl").toPath();
    Path outputModel = tempFolder.getRoot().toPath().resolve("model-jsonl.bin");

    String jsonlContent =
        "{\"sentence_id\":\"s_1\",\"text\":\"Eve geldim.\",\"tokens\":["
            + "{\"index\":0,\"surface\":\"Eve\",\"is_ambiguous\":false,\"candidates\":[{\"id\":0,\"zemberek_key\":\"[ev:Noun] ev:Noun+A3sg+e:Dat\"}]},"
            + "{\"index\":1,\"surface\":\"geldim\",\"is_ambiguous\":false,\"candidates\":[{\"id\":0,\"zemberek_key\":\"[gelmek:Verb] gel:Verb+di:Past+m:A1sg\"}]},"
            + "{\"index\":2,\"surface\":\".\",\"is_ambiguous\":false,\"candidates\":[{\"id\":0,\"zemberek_key\":\"[.:Punc] .:Punc\"}]}"
            + "]}\n"
            + "{\"sentence_id\":\"s_2\",\"text\":\"Kimse yok.\",\"tokens\":["
            + "{\"index\":0,\"surface\":\"Kimse\",\"is_ambiguous\":true,\"candidates\":["
            + "{\"id\":0,\"zemberek_key\":\"[kimse:Noun] kimse:Noun+A3sg\"},"
            + "{\"id\":1,\"zemberek_key\":\"[kimse:Pron,Quant] kimse:Pron+A3sg\"},"
            + "{\"id\":2,\"zemberek_key\":\"[kim:Pron,Ques] kim:Pron+A3sg|Zero→Verb+se:Cond+A3sg\"}"
            + "],\"selected_candidate_id\":1},"
            + "{\"index\":1,\"surface\":\"yok\",\"is_ambiguous\":true,\"candidates\":["
            + "{\"id\":0,\"zemberek_key\":\"[yok:Conj] yok:Conj\"},"
            + "{\"id\":1,\"zemberek_key\":\"[yok:Adj] yok:Adj\"},"
            + "{\"id\":2,\"zemberek_key\":\"[yok:Adv] yok:Adv\"},"
            + "{\"id\":3,\"zemberek_key\":\"[yok:Adj] yok:Adj\"},"
            + "{\"id\":4,\"zemberek_key\":\"[yok:Noun] yok:Noun+A3sg\"}"
            + "],\"selected_candidate_id\":1},"
            + "{\"index\":2,\"surface\":\".\",\"is_ambiguous\":false,\"candidates\":[{\"id\":0,\"zemberek_key\":\"[.:Punc] .:Punc\"}]}"
            + "]}\n";

    Files.write(jsonlFile, jsonlContent.getBytes(StandardCharsets.UTF_8));

    Config config = new Config.Builder()
        .trainPath(jsonlFile)
        .outputPath(outputModel)
        .iterationCount(5)
        .build();

    TrainingResult result = TrainAmbiguityModel.train(config);

    Assert.assertNotNull(result.resolver);
    Assert.assertTrue("Feature count must be greater than 0", result.featureCount > 0);
    Assert.assertTrue("Output binary model must exist", Files.exists(outputModel));

    PerceptronAmbiguityResolver loadedResolver =
        PerceptronAmbiguityResolver.fromModelFile(outputModel);
    Assert.assertNotNull(loadedResolver);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(loadedResolver)
        .build();

    SentenceAnalysis sentenceAnalysis = morphology.analyzeAndDisambiguate("Kimse yok.");
    Assert.assertEquals("Pron", sentenceAnalysis.bestAnalysis().get(0).getDictionaryItem().primaryPos.shortForm);
    Assert.assertEquals("Adj", sentenceAnalysis.bestAnalysis().get(1).getDictionaryItem().primaryPos.shortForm);
  }
}

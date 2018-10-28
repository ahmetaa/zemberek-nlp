package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import zemberek.core.data.Weights;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;
import zemberek.morphology.lexicon.RootLexicon;

public class PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path root = Paths.get("/media/ahmetaa/depo/ambiguity");

    List<Path> paths = Lists.newArrayList(
        Paths.get("data/gold/gold1.txt"),
        root.resolve("www.aljazeera.com.tr-rule-result.txt"),
        root.resolve("wowturkey.com-rule-result.txt"),
        root.resolve("open-subtitles-tr-2018-rule-result.txt"),
        root.resolve("sak.train"),
        root.resolve("www.haberturk.com-rule-result.txt"),
        root.resolve("www.cnnturk.com-rule-result.txt"));

    Path dev = root.resolve("sak.dev");
    Path model = Paths.get("morphology/src/main/resources/tr/ambiguity/model");
    Path modelCompressed = Paths.get("morphology/src/main/resources/tr/ambiguity/model-compressed");

    TurkishMorphology morphology = TurkishMorphology.create(
        RootLexicon.builder().addTextDictionaryResources(
            "tr/master-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict",
            "tr/proper-from-corpus.dict",
            "tr/abbreviations.dict",
            "tr/person-names.dict"
        ).build());

    DataSet trainingSet = new DataSet();
    for (Path path : paths) {
      trainingSet.add(DataSet.load(path, morphology));
    }
    DataSet devSet = DataSet.load(dev, morphology);

    PerceptronAmbiguityResolver resolver =
        new PerceptronAmbiguityResolverTrainer(morphology).train(trainingSet, devSet, 7);
    Weights modelTrained = (Weights) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    PerceptronAmbiguityResolver resolverRead =
        PerceptronAmbiguityResolver.fromModelFile(model);
    Path test = root.resolve("sak.test");
    ((Weights) resolverRead.getModel()).compress().serialize(modelCompressed);

    PerceptronAmbiguityResolverTrainer.test(test, morphology, resolverRead);

    System.out.println("Load compressed model and test");

    PerceptronAmbiguityResolver comp =
        PerceptronAmbiguityResolver.fromModelFile(modelCompressed);
    PerceptronAmbiguityResolverTrainer.test(test, morphology, comp);
  }
}

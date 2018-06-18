package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.Model;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;

public class PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    List<Path> paths = Lists.newArrayList(
        Paths.get("data/gold/gold1.txt"),
        Paths.get("data/ambiguity/www.aljazeera.com.tr-rule-result.txt"),
        Paths.get("data/ambiguity/wowturkey.com-rule-result.txt"),
        Paths.get("data/ambiguity/open-subtitles-rule-result.txt"),
    Paths.get("data/ambiguity/www.cnnturk.com-rule-result.txt"));

    Path dev = Paths.get("data/ambiguity/www.haberturk.com-rule-result.txt");
    Path model = Paths.get("morphology/src/main/resources/tr/ambiguity/model");
    Path modelCompressed = Paths.get("morphology/src/main/resources/tr/ambiguity/model-compressed");

    //TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishMorphology morphology = TurkishMorphology.builder().addTextDictionaryResources(
        "tr/master-dictionary.dict",
        "tr/non-tdk.dict",
        "tr/proper.dict",
        "tr/proper-from-corpus.dict",
        "tr/abbreviations.dict",
        "tr/locations-tr.dict"
    ).build();

    DataSet trainingSet = new DataSet();
    for (Path path : paths) {
      trainingSet.add(DataSet.load(path, morphology));
    }
    DataSet devSet = DataSet.load(dev, morphology);

    PerceptronAmbiguityResolver resolver =
        new PerceptronAmbiguityResolverTrainer(morphology).train(trainingSet, devSet, 5);
    Model modelTrained = (Model) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    PerceptronAmbiguityResolver resolverRead =
        PerceptronAmbiguityResolver.fromModelFile(model);
    Path test = Paths.get("data/ambiguity/www.haberturk.com-rule-result.txt");
    ((Model) resolverRead.getModel()).compress().serialize(modelCompressed);

    PerceptronAmbiguityResolverTrainer.test(test, morphology, resolverRead);

    System.out.println("Load compressed model and test");

    PerceptronAmbiguityResolver comp =
        PerceptronAmbiguityResolver.fromModelFile(modelCompressed);
    PerceptronAmbiguityResolverTrainer.test(test, morphology, comp);
  }
}

package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.Model;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolverTrainer.DataSet;

public class PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path train1 = Paths.get("data/ambiguity/www.aljazeera.com.tr-rule-result.txt");
    Path train2 = Paths.get("data/ambiguity/wowturkey.com-rule-result.txt");
    Path train3 = Paths.get("data/ambiguity/open-subtitles-rule-result.txt");

    Path dev = Paths.get("data/ambiguity/test");
    Path model = Paths.get("morphology/src/main/resources/tr/ambiguity/model");
    Path modelCompressed = Paths.get("morphology/src/main/resources/tr/ambiguity/model-compressed");

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    DataSet set1 = DataSet.load(train1, morphology);
    DataSet set2 = DataSet.load(train2, morphology);
    DataSet set3 = DataSet.load(train3, morphology);
    set1.add(set2);
    set1.add(set3);
    DataSet devSet = DataSet.load(dev, morphology);

    PerceptronAmbiguityResolver resolver =
        new PerceptronAmbiguityResolverTrainer(morphology).train(set1, devSet, 4);
    Model modelTrained = (Model) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    PerceptronAmbiguityResolver resolverRead =
        PerceptronAmbiguityResolver.fromModelFile(model);
    Path test = Paths.get("data/ambiguity/test");
    ((Model) resolverRead.getModel()).compress().serialize(modelCompressed);

    PerceptronAmbiguityResolverTrainer.test(test, morphology, resolverRead);

    System.out.println("Load compressed model and test");

    PerceptronAmbiguityResolver comp =
        PerceptronAmbiguityResolver.fromModelFile(modelCompressed);
    PerceptronAmbiguityResolverTrainer.test(test, morphology, comp);
  }
}

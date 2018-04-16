package zemberek.morphology.ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology.ambiguity.PerceptronAmbiguityResolver.Model;
import zemberek.morphology.TurkishMorphology;

public class PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path train = Paths.get("data/ambiguity/train");
    Path dev = Paths.get("data/ambiguity/open-subtitles-test");
    Path model = Paths.get("morphology/src/main/resources/tr/ambiguity/model");
    Path modelCompressed = Paths.get("morphology/src/main/resources/tr/ambiguity/model-compressed");

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    PerceptronAmbiguityResolver resolver =
        new PerceptronAmbiguityResolverTrainer(morphology).train(train, dev, 7);
    Model modelTrained = (Model) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    PerceptronAmbiguityResolver resolverRead =
        PerceptronAmbiguityResolver.fromModelFile(model);
    Path test = Paths.get("data/ambiguity/open-subtitles-test");
    ((Model) resolverRead.getModel()).compress().serialize(modelCompressed);

    resolverRead.test(test, morphology);

    System.out.println("Load compressed model and test");

    PerceptronAmbiguityResolver comp =
        PerceptronAmbiguityResolver.fromModelFile(modelCompressed);
    comp.test(test, morphology);
  }

}

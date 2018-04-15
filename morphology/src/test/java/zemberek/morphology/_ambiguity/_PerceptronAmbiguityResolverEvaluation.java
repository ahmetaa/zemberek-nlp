package zemberek.morphology._ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.Model;
import zemberek.morphology._analyzer._TurkishMorphology;

public class _PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path train = Paths.get("data/ambiguity/train");
    Path dev = Paths.get("data/ambiguity/open-subtitles-test");
    Path model = Paths.get("morphology/src/main/resources/tr/ambiguity/model");
    Path modelCompressed = Paths.get("morphology/src/main/resources/tr/ambiguity/model-compressed");

    _TurkishMorphology morphology = _TurkishMorphology.createWithDefaults();

    _PerceptronAmbiguityResolver resolver =
        new _PerceptronAmbiguityResolverTrainer(morphology).train(train, dev, 7);
    Model modelTrained = (Model) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    _PerceptronAmbiguityResolver resolverRead =
        _PerceptronAmbiguityResolver.fromModelFile(model);
    Path test = Paths.get("data/ambiguity/open-subtitles-test");
    ((Model) resolverRead.getModel()).compress().serialize(modelCompressed);

    resolverRead.test(test, morphology);

    System.out.println("Load compressed model and test");

    _PerceptronAmbiguityResolver comp =
        _PerceptronAmbiguityResolver.fromModelFile(modelCompressed);
    comp.test(test, morphology);
  }

}

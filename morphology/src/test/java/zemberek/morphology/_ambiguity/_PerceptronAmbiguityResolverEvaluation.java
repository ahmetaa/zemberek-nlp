package zemberek.morphology._ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology._ambiguity._PerceptronAmbiguityResolver.Model;
import zemberek.morphology._analyzer._TurkishMorphology;

public class _PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path train = Paths.get("data/ambiguity/aljazeera-train");
    Path dev = Paths.get("data/ambiguity/open-subtitles-test");
    Path model = Paths.get("data/ambiguity/model");
    Path modelCompressed = Paths.get("data/ambiguity/model-compressed");
    _TurkishMorphology analyzer = _TurkishMorphology.createWithDefaults();

    _PerceptronAmbiguityResolver resolver =
        new _PerceptronAmbiguityResolverTrainer(analyzer).train(train, dev, 7);
    Model modelTrained = (Model) resolver.getModel();
    modelTrained.pruneNearZeroWeights();
    modelTrained.saveAsText(model);

    System.out.println("Load model and test");

    _PerceptronAmbiguityResolver resolverRead =
        _PerceptronAmbiguityResolver.fromModelFile(model, analyzer);
    Path test = Paths.get("data/ambiguity/open-subtitles-test");
    resolverRead.test(test);
    ((Model) resolverRead.getModel()).compress().serialize(modelCompressed);

    System.out.println("Load compressed model and test");

    _PerceptronAmbiguityResolver comp =
        _PerceptronAmbiguityResolver.fromCompressedModelFile(modelCompressed, analyzer);
    comp.test(test);


  }

}
